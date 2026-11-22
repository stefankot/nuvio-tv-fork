package com.nuvio.app.features.player

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D
import edu.emory.mathcs.utils.ConcurrencyUtils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal data class DubbingFeatureSeries(
    val startTimeMs: Long,
    val hopMs: Int,
    val channels: List<FloatArray>,
) {
    val size: Int get() = channels.firstOrNull()?.size ?: 0
}

internal data class DubbingPcmSeries(
    val startTimeMs: Long,
    val sampleRate: Int,
    val samples: FloatArray,
)

internal data class DubbingCorrelationPeak(
    val offsetMs: Long,
    val score: Float,
    val peakToSidelobeRatio: Float,
    val peakToSecondRatio: Float,
)

internal data class DubbingCorrelationConsensus(
    val firstSlice: DubbingCorrelationPeak,
    val secondSlice: DubbingCorrelationPeak,
)

/**
 * A weak local window may not independently prove an offset, but it can safely corroborate the
 * non-zero offset already in use. Keep this deliberately narrower than the normal search gate:
 * it must land within 250 ms of the current value and still separate its best peak clearly from
 * the runner-up. Callers must preserve the current offset rather than applying the weak candidate.
 */
internal fun DubbingCorrelationPeak.corroboratesCurrentOffset(currentOffsetMs: Long): Boolean =
    currentOffsetMs != 0L &&
        abs(offsetMs - currentOffsetMs) <= MAX_CORROBORATION_DRIFT_MS &&
        peakToSidelobeRatio >= MIN_CORROBORATION_PSR &&
        peakToSecondRatio >= MIN_CORROBORATION_RATIO

/**
 * Zero is normally excluded from saved-offset corroboration because agreeing with a default value
 * proves little. A near-zero peak with unusually strong absolute score and PSR is different: it
 * establishes that the two releases are already aligned, while keeping weak or distant candidates
 * on the normal ambiguity path.
 */
internal fun DubbingCorrelationPeak.corroboratesStrongZeroOffset(currentOffsetMs: Long): Boolean =
    currentOffsetMs == 0L &&
        abs(offsetMs) <= MAX_STRONG_ZERO_DRIFT_MS &&
        score >= MIN_STRONG_ZERO_SCORE &&
        peakToSidelobeRatio >= MIN_STRONG_ZERO_PSR &&
        peakToSecondRatio >= MIN_STRONG_ZERO_RATIO

private const val MAX_CORROBORATION_DRIFT_MS = 250L
private const val MIN_CORROBORATION_PSR = 4.5f
private const val MIN_CORROBORATION_RATIO = 1.20f
private const val MAX_STRONG_ZERO_DRIFT_MS = 250L
private const val MIN_STRONG_ZERO_SCORE = 0.10f
private const val MIN_STRONG_ZERO_PSR = 8.0f
private const val MIN_STRONG_ZERO_RATIO = 1.05f

/**
 * A borderline first window may justify one short, genuinely independent read. It still needs a
 * visible peak before we spend more network/decoder time, and the second window must independently
 * recover the same absolute B-minus-A offset. This is deliberately separate from saved-offset
 * corroboration: a new offset is accepted only from two disjoint temporal observations.
 */
internal fun DubbingCorrelationPeak.canRequestIndependentWindow(): Boolean =
    score >= MIN_INDEPENDENT_WINDOW_SCORE &&
        peakToSidelobeRatio >= MIN_INDEPENDENT_WINDOW_PSR &&
        peakToSecondRatio >= MIN_PRIMARY_INDEPENDENT_WINDOW_RATIO

internal fun DubbingCorrelationPeak.isCorroboratedByIndependentWindow(
    confirmation: DubbingCorrelationPeak,
): Boolean =
    canRequestIndependentWindow() &&
        confirmation.score >= MIN_INDEPENDENT_WINDOW_SCORE &&
        confirmation.peakToSidelobeRatio >= MIN_INDEPENDENT_WINDOW_PSR &&
        confirmation.peakToSecondRatio >= MIN_CONFIRMATION_INDEPENDENT_WINDOW_RATIO &&
        abs(offsetMs - confirmation.offsetMs) <= MAX_INDEPENDENT_WINDOW_DRIFT_MS

private const val MIN_INDEPENDENT_WINDOW_SCORE = 0.04f
private const val MIN_INDEPENDENT_WINDOW_PSR = 4.0f
private const val MIN_PRIMARY_INDEPENDENT_WINDOW_RATIO = 1.10f
private const val MIN_CONFIRMATION_INDEPENDENT_WINDOW_RATIO = 1.05f
private const val MAX_INDEPENDENT_WINDOW_DRIFT_MS = 200L

/**
 * Streaming feature extractor. The first three channels describe the complete mix and the last
 * three describe a dialogue-reduced mix. A cheap IIR filter bank is deliberate: decoding/network
 * dominate the operation and this keeps a 60 minute extended search bounded and allocation-free.
 */
internal class DubbingAudioFeatureCollector(
    private val targetSampleRate: Int = TARGET_SAMPLE_RATE,
    private val hopSamples: Int = FEATURE_HOP_SAMPLES,
) {
    private val filters = Array(FEATURE_CHANNEL_COUNT) { index ->
        OnePoleLowPass(targetSampleRate, BAND_CUTOFFS_HZ[index % BAND_COUNT])
    }
    private val energy = DoubleArray(FEATURE_CHANNEL_COUNT)
    private val previousLogEnergy = FloatArray(FEATURE_CHANNEL_COUNT)
    private val output = Array(FEATURE_CHANNEL_COUNT) { FloatList() }
    private var samplesInHop = 0
    private var completedHops = 0
    private var firstSampleTimeUs: Long? = null

    fun add(sampleTimeUs: Long, general: Float, effects: Float) {
        if (firstSampleTimeUs == null) firstSampleTimeUs = sampleTimeUs
        accumulateBands(general, 0)
        accumulateBands(effects, BAND_COUNT)
        samplesInHop++
        if (samplesInHop < hopSamples) return

        for (channel in 0 until FEATURE_CHANNEL_COUNT) {
            val logEnergy = ln(1e-8 + energy[channel] / hopSamples).toFloat()
            // A signed onset/decay envelope is substantially less sensitive to different dialogue
            // than raw PCM, while retaining shared music cuts and sound effects.
            if (completedHops >= FEATURE_WARMUP_HOPS) {
                output[channel].add(logEnergy - previousLogEnergy[channel])
            }
            previousLogEnergy[channel] = logEnergy
            energy[channel] = 0.0
        }
        completedHops++
        samplesInHop = 0
    }

    fun build(): DubbingFeatureSeries {
        val firstUs = firstSampleTimeUs ?: 0L
        return DubbingFeatureSeries(
            startTimeMs = (
                firstUs + (FEATURE_WARMUP_HOPS + 1L) * hopSamples * 1_000_000L / targetSampleRate
            ) / 1_000L,
            hopMs = hopSamples * 1_000 / targetSampleRate,
            channels = output.map(FloatList::toArray),
        )
    }

    private fun accumulateBands(sample: Float, outputOffset: Int) {
        val low0 = filters[outputOffset].process(sample)
        val low1 = filters[outputOffset + 1].process(sample)
        val low2 = filters[outputOffset + 2].process(sample)
        val band0 = low0.toDouble()
        val band1 = (low1 - low0).toDouble()
        val band2 = (low2 - low1).toDouble()
        energy[outputOffset] += band0 * band0
        energy[outputOffset + 1] += band1 * band1
        energy[outputOffset + 2] += band2 * band2
    }

    private class OnePoleLowPass(sampleRate: Int, cutoffHz: Int) {
        private val alpha = (1.0 - exp(-2.0 * PI * cutoffHz / sampleRate)).toFloat()
        private var state = 0f
        fun process(value: Float): Float {
            state += alpha * (value - state)
            return state
        }
    }

    private class FloatList(initialCapacity: Int = 4_096) {
        private var values = FloatArray(initialCapacity)
        private var size = 0
        fun add(value: Float) {
            if (size == values.size) values = values.copyOf(values.size * 2)
            values[size++] = value
        }
        fun toArray(): FloatArray = values.copyOf(size)
    }

    companion object {
        const val TARGET_SAMPLE_RATE = 8_000
        const val FEATURE_HOP_SAMPLES = 80
        private const val FEATURE_WARMUP_HOPS = 50
        private const val BAND_COUNT = 3
        private const val FEATURE_CHANNEL_COUNT = BAND_COUNT * 2
        private val BAND_CUTOFFS_HZ = intArrayOf(500, 1_500, 3_500)
    }
}

internal object DubbingAudioSyncDsp {
    init {
        // JTransforms 2.4 otherwise uses every advertised core. Two workers are the best compromise
        // for a Fire TV while the main player still owns its video/audio decoder threads.
        ConcurrencyUtils.setNumberOfThreads(2)
    }

    fun correlateFeatures(reference: DubbingFeatureSeries, search: DubbingFeatureSeries): DubbingCorrelationPeak? {
        if (reference.hopMs != search.hopMs || reference.size < 100 || search.size < reference.size) return null
        val candidateCount = search.size - reference.size + 1
        val combined = FloatArray(candidateCount)
        var totalWeight = 0f

        val channelCount = min(reference.channels.size, search.channels.size)
        for (channel in 0 until channelCount) {
            val ref = reference.channels[channel]
            val candidate = search.channels[channel]
            val baseWeight = if (channel >= 3) 0.7f else 0.3f
            val scores = normalizedLinearCorrelation(ref, candidate) ?: continue
            for (index in scores.indices) combined[index] += scores[index] * baseWeight
            totalWeight += baseWeight
        }
        if (totalWeight <= 0f) return null
        for (index in combined.indices) combined[index] /= totalWeight

        val metrics = peakMetrics(combined, guardSamples = max(5, 1_000 / reference.hopMs)) ?: return null
        val matchedTimeMs = search.startTimeMs + metrics.index.toLong() * search.hopMs
        return DubbingCorrelationPeak(
            offsetMs = matchedTimeMs - reference.startTimeMs,
            score = metrics.value,
            peakToSidelobeRatio = metrics.psr,
            peakToSecondRatio = metrics.ratio,
        )
    }

    /**
     * Corroborates a borderline full-window peak with two non-overlapping portions of the PCM
     * already decoded. Agreement is the strong signal here: no network access or second decode is
     * performed, and a repeated/ambiguous soundtrack normally sends the two slices to different
     * candidates.
     */
    fun correlateIndependentSlices(
        reference: DubbingFeatureSeries,
        search: DubbingFeatureSeries,
        fullWindowPeak: DubbingCorrelationPeak,
    ): DubbingCorrelationConsensus? {
        if (fullWindowPeak.score < MIN_CONSENSUS_FULL_SCORE ||
            fullWindowPeak.peakToSidelobeRatio < MIN_CONSENSUS_FULL_PSR ||
            fullWindowPeak.peakToSecondRatio < MIN_CONSENSUS_FULL_RATIO ||
            reference.size < MIN_CONSENSUS_REFERENCE_FRAMES
        ) return null

        val sliceSize = reference.size * 2 / 5
        val firstReference = reference.slice(start = 0, length = sliceSize)
        val secondReference = reference.slice(start = reference.size - sliceSize, length = sliceSize)
        val firstPeak = correlateFeatures(firstReference, search) ?: return null
        val secondPeak = correlateFeatures(secondReference, search) ?: return null
        if (firstPeak.isAtSearchBoundary(firstReference, search) ||
            secondPeak.isAtSearchBoundary(secondReference, search) ||
            !firstPeak.isUsableConsensusSlice() ||
            !secondPeak.isUsableConsensusSlice() ||
            abs(firstPeak.offsetMs - secondPeak.offsetMs) > MAX_CONSENSUS_DRIFT_MS ||
            abs(firstPeak.offsetMs - fullWindowPeak.offsetMs) > MAX_CONSENSUS_DRIFT_MS ||
            abs(secondPeak.offsetMs - fullWindowPeak.offsetMs) > MAX_CONSENSUS_DRIFT_MS
        ) return null
        return DubbingCorrelationConsensus(firstPeak, secondPeak)
    }

    /** GCC-PHAT refinement. Returns null rather than trusting an ambiguous waveform peak. */
    fun refineWithGccPhat(reference: DubbingPcmSeries, search: DubbingPcmSeries): DubbingCorrelationPeak? {
        if (reference.sampleRate != search.sampleRate || reference.samples.size < 1_024 || search.samples.size < reference.samples.size) {
            return null
        }
        val fftSize = nextPowerOfTwo(reference.samples.size + search.samples.size - 1)
        val refSpectrum = FloatArray(fftSize * 2)
        val searchSpectrum = FloatArray(fftSize * 2)
        val referenceMean = reference.samples.average().toFloat()
        val searchMean = search.samples.average().toFloat()

        for (index in reference.samples.indices) {
            val window = (0.5 - 0.5 * cos(2.0 * PI * index / max(1, reference.samples.lastIndex))).toFloat()
            refSpectrum[index * 2] = (reference.samples[index] - referenceMean) * window
        }
        for (index in search.samples.indices) searchSpectrum[index * 2] = search.samples[index] - searchMean

        val fft = FloatFFT_1D(fftSize)
        fft.complexForward(refSpectrum)
        fft.complexForward(searchSpectrum)
        for (index in 0 until fftSize) {
            val offset = index * 2
            val ar = refSpectrum[offset]
            val ai = refSpectrum[offset + 1]
            val br = searchSpectrum[offset]
            val bi = searchSpectrum[offset + 1]
            val real = br * ar + bi * ai
            val imaginary = bi * ar - br * ai
            val magnitude = sqrt(real * real + imaginary * imaginary).coerceAtLeast(1e-12f)
            searchSpectrum[offset] = real / magnitude
            searchSpectrum[offset + 1] = imaginary / magnitude
        }
        fft.complexInverse(searchSpectrum, true)

        val validCount = search.samples.size - reference.samples.size + 1
        val scores = FloatArray(validCount) { index -> searchSpectrum[index * 2] }
        val metrics = peakMetrics(scores, guardSamples = max(32, reference.sampleRate / 20)) ?: return null
        val matchedTimeMs = search.startTimeMs + metrics.index * 1_000L / search.sampleRate
        return DubbingCorrelationPeak(
            offsetMs = matchedTimeMs - reference.startTimeMs,
            score = metrics.value,
            peakToSidelobeRatio = metrics.psr,
            peakToSecondRatio = metrics.ratio,
        )
    }

    private fun normalizedLinearCorrelation(referenceInput: FloatArray, searchInput: FloatArray): FloatArray? {
        val reference = referenceInput.copyOf()
        val mean = reference.average().toFloat()
        var referenceEnergy = 0.0
        for (index in reference.indices) {
            reference[index] -= mean
            referenceEnergy += reference[index] * reference[index].toDouble()
        }
        if (referenceEnergy < 1e-8) return null

        val fftSize = nextPowerOfTwo(reference.size + searchInput.size - 1)
        val a = FloatArray(fftSize * 2)
        val b = FloatArray(fftSize * 2)
        for (index in searchInput.indices) a[index * 2] = searchInput[index]
        for (index in reference.indices) b[index * 2] = reference[reference.lastIndex - index]
        val fft = FloatFFT_1D(fftSize)
        fft.complexForward(a)
        fft.complexForward(b)
        for (index in 0 until fftSize) {
            val offset = index * 2
            val ar = a[offset]
            val ai = a[offset + 1]
            val br = b[offset]
            val bi = b[offset + 1]
            a[offset] = ar * br - ai * bi
            a[offset + 1] = ar * bi + ai * br
        }
        fft.complexInverse(a, true)

        val prefix = DoubleArray(searchInput.size + 1)
        val prefixSquares = DoubleArray(searchInput.size + 1)
        for (index in searchInput.indices) {
            val value = searchInput[index].toDouble()
            prefix[index + 1] = prefix[index] + value
            prefixSquares[index + 1] = prefixSquares[index] + value * value
        }
        val count = searchInput.size - reference.size + 1
        return FloatArray(count) { start ->
            val sum = prefix[start + reference.size] - prefix[start]
            val sumSquares = prefixSquares[start + reference.size] - prefixSquares[start]
            val varianceEnergy = max(0.0, sumSquares - sum * sum / reference.size)
            val denominator = sqrt(referenceEnergy * varianceEnergy)
            if (denominator <= 1e-10) 0f else (a[(start + reference.size - 1) * 2] / denominator).toFloat()
        }
    }

    private data class PeakMetrics(val index: Int, val value: Float, val psr: Float, val ratio: Float)

    private fun peakMetrics(values: FloatArray, guardSamples: Int): PeakMetrics? {
        if (values.isEmpty()) return null
        var bestIndex = 0
        for (index in 1 until values.size) if (values[index] > values[bestIndex]) bestIndex = index
        val best = values[bestIndex]
        var second = Float.NEGATIVE_INFINITY
        var sum = 0.0
        var sumSquares = 0.0
        var count = 0
        for (index in values.indices) {
            if (abs(index - bestIndex) <= guardSamples) continue
            val value = values[index]
            if (value > second) second = value
            sum += value
            sumSquares += value * value.toDouble()
            count++
        }
        if (count == 0) return null
        val sidelobeMean = sum / count
        val variance = max(1e-12, sumSquares / count - sidelobeMean * sidelobeMean)
        val psr = ((best - sidelobeMean) / sqrt(variance)).toFloat()
        val ratio = if (second <= 1e-6f) Float.POSITIVE_INFINITY else best / second
        return PeakMetrics(bestIndex, best, psr, ratio)
    }

    private fun nextPowerOfTwo(value: Int): Int {
        var result = 1
        while (result < value && result > 0) result = result shl 1
        require(result > 0) { "FFT input is too large" }
        return result
    }

    private fun DubbingFeatureSeries.slice(start: Int, length: Int): DubbingFeatureSeries =
        DubbingFeatureSeries(
            startTimeMs = startTimeMs + start.toLong() * hopMs,
            hopMs = hopMs,
            channels = channels.map { it.copyOfRange(start, start + length) },
        )

    private fun DubbingCorrelationPeak.isUsableConsensusSlice(): Boolean =
        score >= MIN_CONSENSUS_SLICE_SCORE &&
            peakToSidelobeRatio >= MIN_CONSENSUS_SLICE_PSR &&
            peakToSecondRatio >= MIN_CONSENSUS_SLICE_RATIO

    private const val MIN_CONSENSUS_REFERENCE_FRAMES = 1_500
    private const val MIN_CONSENSUS_FULL_SCORE = 0.04f
    private const val MIN_CONSENSUS_FULL_PSR = 4.0f
    private const val MIN_CONSENSUS_FULL_RATIO = 1.12f
    private const val MIN_CONSENSUS_SLICE_SCORE = 0.025f
    private const val MIN_CONSENSUS_SLICE_PSR = 2.75f
    private const val MIN_CONSENSUS_SLICE_RATIO = 1.03f
    private const val MAX_CONSENSUS_DRIFT_MS = 200L
}

internal fun DubbingCorrelationPeak.isAtSearchBoundary(
    reference: DubbingFeatureSeries,
    search: DubbingFeatureSeries,
    guardMs: Long = 500L,
): Boolean {
    if (reference.hopMs != search.hopMs || search.size < reference.size) return true
    val minimumOffsetMs = search.startTimeMs - reference.startTimeMs
    val maximumOffsetMs = minimumOffsetMs + (search.size - reference.size).toLong() * search.hopMs
    return offsetMs <= minimumOffsetMs + guardMs || offsetMs >= maximumOffsetMs - guardMs
}
