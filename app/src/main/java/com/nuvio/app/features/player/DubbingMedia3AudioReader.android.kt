package com.nuvio.app.features.player

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.DiscardingTrackOutput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import androidx.media3.extractor.TrackOutput
import java.io.Closeable
import java.io.EOFException
import java.util.ArrayDeque
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

internal data class DubbingEncodedAudioSample(
    val timeUs: Long,
    val data: ByteArray,
    val flags: Int,
)

internal fun shouldDiscardDubbingSample(sampleTimeUs: Long, startUs: Long): Boolean =
    sampleTimeUs < startUs - 250_000L

internal fun approximateDubbingBytePosition(
    targetTimeUs: Long,
    durationUs: Long,
    streamLength: Long,
): Long? {
    if (targetTimeUs < 0L || durationUs <= 0L || streamLength <= 0L) return null
    val fraction = (targetTimeUs.toDouble() / durationUs).coerceIn(0.0, 1.0)
    return (streamLength * fraction).toLong().coerceIn(0L, streamLength - 1L)
}

internal fun extrapolateDubbingBytePosition(
    targetTimeUs: Long,
    firstPosition: Long,
    firstTimeUs: Long,
    secondPosition: Long,
    secondTimeUs: Long,
    minimumPosition: Long,
    maximumPosition: Long,
): Long? {
    val timeSpan = secondTimeUs - firstTimeUs
    val byteSpan = secondPosition - firstPosition
    if (timeSpan <= 0L || byteSpan <= 0L || targetTimeUs < secondTimeUs || maximumPosition < minimumPosition) {
        return null
    }
    val projected = secondPosition.toDouble() +
        (targetTimeUs - secondTimeUs).toDouble() * byteSpan.toDouble() / timeSpan.toDouble()
    if (!projected.isFinite()) return null
    return projected.toLong().coerceIn(minimumPosition, maximumPosition)
}

internal fun findMatroskaClusterMarker(data: ByteArray, length: Int = data.size): Int {
    val limit = length.coerceIn(0, data.size)
    var matched = 0
    for (index in 0 until limit) {
        val byte = data[index]
        matched = when {
            byte == MATROSKA_CLUSTER_ID[matched] -> matched + 1
            byte == MATROSKA_CLUSTER_ID[0] -> 1
            else -> 0
        }
        if (matched == MATROSKA_CLUSTER_ID.size) return index - MATROSKA_CLUSTER_ID.lastIndex
    }
    return -1
}

private val MATROSKA_CLUSTER_ID = byteArrayOf(0x1f, 0x43, 0xb6.toByte(), 0x75)
private val MATROSKA_EBML_ID = byteArrayOf(0x1a, 0x45, 0xdf.toByte(), 0xa3.toByte())

internal fun requiresDubbingPlatformSeek(
    startUs: Long,
    seekPointPosition: Long,
    seekPointTimeUs: Long,
): Boolean = startUs >= 60_000_000L && seekPointPosition == 0L && seekPointTimeUs == 0L

internal fun canAcceptApproximateDubbingForwardSeek(
    firstSampleTimeUs: Long,
    targetTimeUs: Long,
    endTimeUs: Long,
): Boolean {
    val overshootUs = firstSampleTimeUs - targetTimeUs
    return overshootUs in 1..6_000_000L && endTimeUs - firstSampleTimeUs >= 15_000_000L
}

/**
 * Connects coroutine cancellation to the blocking Media3 DataSource read.
 *
 * Extractor.read() is synchronous, so cancelling only the coroutine does not
 * release a blocked HTTP read. Closing the currently bound source makes the
 * read return immediately and lets the analyzer reach its finally blocks.
 */
internal class DubbingAnalysisCancellationToken {
    private val cancelled = AtomicBoolean(false)
    private val boundSource = AtomicReference<DataSource?>(null)

    fun bind(source: DataSource) {
        boundSource.set(source)
        if (cancelled.get() && boundSource.compareAndSet(source, null)) {
            runCatching { source.close() }
        }
        throwIfCancelled()
    }

    fun unbind(source: DataSource) {
        boundSource.compareAndSet(source, null)
    }

    fun cancel() {
        cancelled.set(true)
        boundSource.getAndSet(null)?.let { source -> runCatching { source.close() } }
    }

    fun throwIfCancelled() {
        if (cancelled.get()) throw CancellationException("Análise de dublagem cancelada.")
    }
}

/** Reusable encoded-sample accumulator; retains only Media3's requested tail. */
internal class DubbingSampleAccumulator(initialCapacity: Int = 64 * 1024) {
    private var bytes = ByteArray(initialCapacity.coerceAtLeast(1))
    private var size = 0

    fun append(source: ByteArray, sourceOffset: Int, length: Int) {
        if (length <= 0) return
        ensureCapacity(size + length)
        source.copyInto(bytes, size, sourceOffset, sourceOffset + length)
        size += length
    }

    fun append(source: ParsableByteArray, length: Int) {
        if (length <= 0) return
        ensureCapacity(size + length)
        source.readBytes(bytes, size, length)
        size += length
    }

    fun finishSample(sampleSize: Int, offset: Int): ByteArray? {
        val start = size - offset - sampleSize
        val sample = if (start >= 0 && sampleSize > 0) {
            ByteArray(sampleSize).also { bytes.copyInto(it, 0, start, start + sampleSize) }
        } else {
            null
        }
        if (offset in 1..size) {
            bytes.copyInto(bytes, 0, size - offset, size)
            size = offset
        } else {
            size = 0
        }
        return sample
    }

    fun reset() {
        size = 0
    }

    internal fun capacityForTest(): Int = bytes.size

    private fun ensureCapacity(required: Int) {
        if (required <= bytes.size) return
        var capacity = bytes.size
        while (capacity < required) {
            val grown = capacity + (capacity shr 1)
            capacity = if (grown > capacity) grown else required
        }
        bytes = bytes.copyOf(capacity)
    }
}

/**
 * Demuxes audio with the same Media3 extractors used by ExoPlayer.
 *
 * Android's platform MediaExtractor is vendor-dependent. In particular, the
 * BlueStacks Android 11 image filters otherwise playable AC-3/DTS tracks out of
 * Matroska/TS containers. Media3 keeps demux independent from the platform and
 * exposes the same track ordering/metadata observed by the Nuvio player.
 */
@OptIn(UnstableApi::class)
internal class DubbingMedia3AudioReader(
    private val source: DubbingAnalysisSource,
    private val startUs: Long,
    private val endUs: Long,
    private val cancellationToken: DubbingAnalysisCancellationToken? = null,
    private val onLinearScanProgress: (Float) -> Unit = {},
) : Closeable {
    private val uri = Uri.parse(source.url)
    private val positionHolder = PositionHolder()
    private val extractorOutput = AudioExtractorOutput()
    private var dataSource: DataSource = createDataSource()
    private var extractorInput: ExtractorInput
    private var streamLength = C.LENGTH_UNSET.toLong()
    private val extractor: Extractor
    private var ended = false
    private val selectedOutput: AudioTrackOutput
    private var lastLinearScanProgress = -1f
    private var linearScanTargetByte = C.LENGTH_UNSET.toLong()
    private var probingApproximateSeek = false
    private var isMatroskaContainer = false

    var requiresLinearScan: Boolean = false
        private set

    var usedApproximateSeek: Boolean = false
        private set

    val format: Format

    init {
        extractorInput = openInput(0L)
        isMatroskaContainer = sniffMatroskaContainer(extractorInput)
        extractor = selectExtractor(extractorInput)
        extractor.init(extractorOutput)
        while (!extractorOutput.tracksEnded || extractorOutput.audioOutputs.any { it.format == null }) {
            if (!readExtractorStep()) break
        }
        val candidates = extractorOutput.audioOutputs.mapIndexedNotNull { ordinal, output ->
            output.format?.let { format ->
                DubbingAudioTrackCandidate(
                    extractorIndex = ordinal,
                    audioOrdinal = ordinal,
                    language = format.language,
                    mimeType = format.sampleMimeType,
                    channelCount = format.channelCount.takeIf { it > 0 },
                )
            }
        }
        Log.i(
            TAG,
            "Media3 encontrou ${candidates.size} faixa(s) em ${source.sourceLabel}: " +
                candidates.joinToString { "#${it.audioOrdinal}:${it.language ?: "und"}/${it.mimeType ?: "?"}" },
        )
        val selected = resolveDubbingAudioTrack(candidates, source.audioTrackOrdinal, source.audioTrackHint)
            ?: error("A faixa de áudio ${source.sourceLabel} não pôde ser localizada nesta fonte.")
        selectedOutput = extractorOutput.audioOutputs[selected.extractorIndex]
        selectedOutput.selected = true
        format = selectedOutput.format ?: error("A faixa ${source.sourceLabel} não informou seu formato de áudio.")
        Log.i(TAG, "Media3 selecionou #${selected.audioOrdinal} (${selected.language ?: "und"}/${selected.mimeType}) em ${source.sourceLabel}.")
        seekToWindow()
    }

    fun nextSample(): DubbingEncodedAudioSample? {
        while (true) {
            cancellationToken?.throwIfCancelled()
            selectedOutput.samples.pollFirst()?.let { sample ->
                if (sample.timeUs > endUs + DECODER_PREROLL_US) {
                    ended = true
                    return null
                }
                if (shouldDiscardDubbingSample(sample.timeUs, startUs)) {
                    reportLinearScanProgress(sample.timeUs)
                    continue
                }
                return sample
            }
            if (ended || !readExtractorStep()) return null
        }
    }

    private fun seekToWindow() {
        val seekMap = extractorOutput.seekMap
        val durationUs = effectiveDurationUs(seekMap)
        val seekPoint = seekMap
            ?.takeIf { it.isSeekable }
            ?.getSeekPoints(startUs)
            ?.first
            ?: SeekPoint.START
        requiresLinearScan = requiresDubbingPlatformSeek(
            startUs = startUs,
            seekPointPosition = seekPoint.position,
            seekPointTimeUs = seekPoint.timeUs,
        )
        if (requiresLinearScan) {
            Log.i(
                TAG,
                "Fonte ${source.sourceLabel} sem índice: matroska=$isMatroskaContainer, " +
                    "duração=${durationUs / 1_000}ms, bytes=$streamLength.",
            )
        }
        if (requiresLinearScan && tryApproximateMatroskaSeek(durationUs)) {
            requiresLinearScan = false
            usedApproximateSeek = true
            Log.i(TAG, "Seek Matroska aproximado encontrou a janela de ${source.sourceLabel} sem varrer desde 00:00.")
            return
        }
        linearScanTargetByte = approximateDubbingBytePosition(
            targetTimeUs = startUs,
            durationUs = durationUs,
            streamLength = streamLength,
        ) ?: C.LENGTH_UNSET.toLong()
        extractorOutput.audioOutputs.forEach(AudioTrackOutput::resetForSeek)
        extractor.seek(seekPoint.position, startUs)
        reopen(seekPoint.position)
        ended = false
    }

    private fun effectiveDurationUs(seekMap: SeekMap?): Long {
        val extractorDurationUs = seekMap?.durationUs ?: C.TIME_UNSET
        if (extractorDurationUs > 0L && extractorDurationUs != C.TIME_UNSET) return extractorDurationUs
        val expectedMs = source.expectedDurationMs?.takeIf { it > 0L } ?: return C.TIME_UNSET
        return expectedMs.coerceAtMost(Long.MAX_VALUE / 1_000L) * 1_000L
    }

    private fun sniffMatroskaContainer(input: ExtractorInput): Boolean {
        val header = ByteArray(MATROSKA_EBML_ID.size)
        return try {
            input.peekFully(header, 0, header.size, true) && header.contentEquals(MATROSKA_EBML_ID)
        } finally {
            input.resetPeekPosition()
        }
    }

    private fun tryApproximateMatroskaSeek(durationUs: Long): Boolean {
        if (!isMatroskaContainer) return false
        val targetUs = (startUs - APPROXIMATE_SEEK_BACKOFF_US).coerceAtLeast(0L)
        if (targetUs == 0L || durationUs <= targetUs || streamLength <= 0L) return false

        var lowPosition = 0L
        var lowTimeUs = 0L
        var highPosition = streamLength - 1L
        var highTimeUs = durationUs
        var bestPosition: Long? = null
        var previousLowPosition: Long? = null
        var previousLowTimeUs: Long? = null
        var observedUpperBound = false
        probingApproximateSeek = true
        try {
            repeat(APPROXIMATE_SEEK_MAX_PROBES) { probeIndex ->
                cancellationToken?.throwIfCancelled()
                val localEstimate = if (!observedUpperBound && previousLowPosition != null && previousLowTimeUs != null) {
                    extrapolateDubbingBytePosition(
                        targetTimeUs = targetUs,
                        firstPosition = previousLowPosition,
                        firstTimeUs = previousLowTimeUs,
                        secondPosition = lowPosition,
                        secondTimeUs = lowTimeUs,
                        minimumPosition = lowPosition + 1L,
                        maximumPosition = highPosition - 1L,
                    )
                } else {
                    null
                }
                val estimated = localEstimate ?: interpolatePosition(
                        targetTimeUs = targetUs,
                        lowPosition = lowPosition,
                        lowTimeUs = lowTimeUs,
                        highPosition = highPosition,
                        highTimeUs = highTimeUs,
                    ) ?: return@repeat
                val clusterPosition = findClusterAtOrAfter(estimated) ?: return@repeat
                prepareExtractorAt(clusterPosition, targetUs)
                val firstTimeUs = readFirstSelectedSampleTime() ?: return@repeat
                emitLinearScanProgress((probeIndex + 1f) / (APPROXIMATE_SEEK_MAX_PROBES + 1f))
                Log.i(
                    TAG,
                    "Seek Matroska ${source.sourceLabel}: tentativa ${probeIndex + 1}, " +
                        "byte=$clusterPosition, primeiro=${firstTimeUs / 1_000}ms, alvo=${targetUs / 1_000}ms.",
                )
                val forwardOvershootUs = firstTimeUs - targetUs
                if (canAcceptApproximateDubbingForwardSeek(firstTimeUs, targetUs, endUs)) {
                    Log.i(
                        TAG,
                        "Seek Matroska ${source.sourceLabel}: aceitando cluster " +
                            "${forwardOvershootUs / 1_000}ms após o alvo; a janela restante " +
                            "mantém sinal suficiente.",
                    )
                    return true
                }
                if (firstTimeUs <= targetUs) {
                    bestPosition = clusterPosition
                    if (lowPosition > 0L && lowTimeUs > 0L) {
                        previousLowPosition = lowPosition
                        previousLowTimeUs = lowTimeUs
                    }
                    lowPosition = clusterPosition
                    lowTimeUs = firstTimeUs.coerceAtLeast(0L)
                    if (abs(targetUs - firstTimeUs) <= APPROXIMATE_SEEK_TOLERANCE_US) {
                        return true
                    }
                } else {
                    highPosition = clusterPosition
                    highTimeUs = firstTimeUs
                    observedUpperBound = true
                }
            }
            val fallbackPosition = bestPosition ?: return false
            prepareExtractorAt(fallbackPosition, targetUs)
            return true
        } catch (error: Throwable) {
            cancellationToken?.throwIfCancelled()
            Log.w(TAG, "Seek Matroska aproximado falhou para ${source.sourceLabel}; mantendo fallback linear.", error)
            return false
        } finally {
            probingApproximateSeek = false
        }
    }

    private fun interpolatePosition(
        targetTimeUs: Long,
        lowPosition: Long,
        lowTimeUs: Long,
        highPosition: Long,
        highTimeUs: Long,
    ): Long? {
        val timeSpan = highTimeUs - lowTimeUs
        val byteSpan = highPosition - lowPosition
        if (timeSpan <= 0L || byteSpan <= 0L) return null
        val fraction = ((targetTimeUs - lowTimeUs).toDouble() / timeSpan).coerceIn(0.0, 1.0)
        return (lowPosition + byteSpan * fraction).toLong().coerceIn(lowPosition, highPosition)
    }

    private fun findClusterAtOrAfter(estimatedPosition: Long): Long? {
        val scanStart = (estimatedPosition - APPROXIMATE_SEEK_SCAN_BEHIND_BYTES).coerceAtLeast(0L)
        val scanEnd = (estimatedPosition + APPROXIMATE_SEEK_SCAN_AHEAD_BYTES)
            .coerceAtMost(streamLength)
        reopen(scanStart)
        val buffer = ByteArray(APPROXIMATE_SEEK_SCAN_BUFFER_BYTES)
        var absolutePosition = scanStart
        var matched = 0
        while (absolutePosition < scanEnd) {
            cancellationToken?.throwIfCancelled()
            val wanted = minOf(buffer.size.toLong(), scanEnd - absolutePosition).toInt()
            val read = extractorInput.read(buffer, 0, wanted)
            if (read <= 0) return null
            for (index in 0 until read) {
                val byte = buffer[index]
                matched = when {
                    byte == MATROSKA_CLUSTER_ID[matched] -> matched + 1
                    byte == MATROSKA_CLUSTER_ID[0] -> 1
                    else -> 0
                }
                if (matched == MATROSKA_CLUSTER_ID.size) {
                    return absolutePosition + index - MATROSKA_CLUSTER_ID.lastIndex
                }
            }
            absolutePosition += read
        }
        return null
    }

    private fun prepareExtractorAt(position: Long, targetTimeUs: Long) {
        extractorOutput.audioOutputs.forEach(AudioTrackOutput::resetForSeek)
        extractor.seek(position, targetTimeUs)
        reopen(position)
        ended = false
    }

    private fun readFirstSelectedSampleTime(): Long? {
        repeat(APPROXIMATE_SEEK_SAMPLE_PROBE_STEPS) {
            selectedOutput.samples.peekFirst()?.let { return it.timeUs }
            if (!readExtractorStep()) return null
        }
        return selectedOutput.samples.peekFirst()?.timeUs
    }

    private fun reportLinearScanProgress(sampleTimeUs: Long) {
        if (!requiresLinearScan || startUs <= 0L) return
        val fraction = (sampleTimeUs.coerceAtLeast(0L).toFloat() / startUs).coerceIn(0f, 1f)
        emitLinearScanProgress(fraction)
    }

    private fun readExtractorStep(): Boolean {
        cancellationToken?.throwIfCancelled()
        val result = when (extractor.read(extractorInput, positionHolder)) {
            Extractor.RESULT_CONTINUE -> true
            Extractor.RESULT_SEEK -> {
                reopen(positionHolder.position)
                true
            }
            else -> {
                ended = true
                false
            }
        }
        reportLinearScanByteProgress()
        return result
    }

    private fun reportLinearScanByteProgress() {
        if (!requiresLinearScan || probingApproximateSeek || linearScanTargetByte <= 0L) return
        val fraction = (extractorInput.position.toDouble() / linearScanTargetByte)
            .toFloat()
            .coerceIn(0f, 1f)
        emitLinearScanProgress(fraction)
    }

    private fun emitLinearScanProgress(fraction: Float) {
        if (fraction - lastLinearScanProgress < LINEAR_SCAN_PROGRESS_STEP) return
        lastLinearScanProgress = fraction
        onLinearScanProgress(fraction)
    }

    private fun selectExtractor(input: ExtractorInput): Extractor {
        val extractors = DefaultExtractorsFactory().createExtractors(uri, emptyMap())
        for (candidate in extractors) {
            val accepted = try {
                candidate.sniff(input)
            } catch (_: EOFException) {
                false
            } finally {
                input.resetPeekPosition()
            }
            if (accepted) return candidate
        }
        extractors.forEach(Extractor::release)
        error("O formato da fonte ${source.sourceLabel} não é reconhecido pelo demux Media3.")
    }

    private fun reopen(position: Long) {
        cancellationToken?.unbind(dataSource)
        runCatching { dataSource.close() }
        cancellationToken?.throwIfCancelled()
        dataSource = createDataSource()
        extractorInput = openInput(position)
    }

    private fun openInput(position: Long): ExtractorInput {
        cancellationToken?.bind(dataSource)
        try {
            val length = dataSource.open(
                DataSpec.Builder()
                    .setUri(uri)
                    .setPosition(position)
                    .setHttpRequestHeaders(source.dataSourceRequestHeaders ?: source.requestHeaders)
                    .build()
            )
            streamLength = if (length == C.LENGTH_UNSET.toLong()) C.LENGTH_UNSET.toLong() else position + length
            return DefaultExtractorInput(dataSource, position, streamLength)
        } catch (error: Throwable) {
            cancellationToken?.unbind(dataSource)
            runCatching { dataSource.close() }
            throw error
        }
    }

    private fun createDataSource(): DataSource = source.dataSourceFactory?.invoke() ?: when (uri.scheme?.lowercase()) {
        null, "", "file" -> FileDataSource()
        else -> DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(source.requestHeaders)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .createDataSource()
    }

    override fun close() {
        runCatching { extractor.release() }
        cancellationToken?.unbind(dataSource)
        runCatching { dataSource.close() }
    }

    private class AudioExtractorOutput : ExtractorOutput {
        val audioOutputs = mutableListOf<AudioTrackOutput>()
        private val audioOutputsById = mutableMapOf<Int, AudioTrackOutput>()
        var tracksEnded = false
        var seekMap: SeekMap? = null

        override fun track(id: Int, type: Int): TrackOutput = if (type == C.TRACK_TYPE_AUDIO) {
            audioOutputsById.getOrPut(id) { AudioTrackOutput().also(audioOutputs::add) }
        } else {
            DiscardingTrackOutput()
        }

        override fun endTracks() {
            tracksEnded = true
        }

        override fun seekMap(seekMap: SeekMap) {
            this.seekMap = seekMap
        }
    }

    private class AudioTrackOutput : TrackOutput {
        var format: Format? = null
        var selected: Boolean = false
        val samples = ArrayDeque<DubbingEncodedAudioSample>()
        private val pending = DubbingSampleAccumulator(READ_CHUNK_BYTES)
        private val transferBuffer = ByteArray(READ_CHUNK_BYTES)

        override fun format(format: Format) {
            this.format = format
        }

        override fun sampleData(
            input: DataReader,
            length: Int,
            allowEndOfInput: Boolean,
            sampleDataPart: Int,
        ): Int {
            var remaining = length
            var total = 0
            while (remaining > 0) {
                val read = input.read(transferBuffer, 0, minOf(remaining, transferBuffer.size))
                if (read < 0) {
                    if (allowEndOfInput && total == 0) return C.RESULT_END_OF_INPUT
                    throw EOFException()
                }
                if (selected && sampleDataPart == TrackOutput.SAMPLE_DATA_PART_MAIN) {
                    pending.append(transferBuffer, 0, read)
                }
                remaining -= read
                total += read
            }
            return total
        }

        override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) {
            if (selected && sampleDataPart == TrackOutput.SAMPLE_DATA_PART_MAIN) {
                pending.append(data, length)
            } else {
                data.skipBytes(length)
            }
        }

        override fun sampleMetadata(
            timeUs: Long,
            flags: Int,
            size: Int,
            offset: Int,
            cryptoData: TrackOutput.CryptoData?,
        ) {
            val sample = if (selected) pending.finishSample(size, offset) else null
            if (sample != null && cryptoData == null) {
                samples.addLast(
                    DubbingEncodedAudioSample(
                        timeUs = timeUs,
                        data = sample,
                        flags = flags,
                    )
                )
            }
        }

        fun resetForSeek() {
            samples.clear()
            pending.reset()
        }
    }

    companion object {
        private const val TAG = "DubbingSync"
        private const val READ_CHUNK_BYTES = 64 * 1024
        private const val DECODER_PREROLL_US = 250_000L
        private const val LINEAR_SCAN_PROGRESS_STEP = 0.01f
        private const val APPROXIMATE_SEEK_BACKOFF_US = 1_000_000L
        private const val APPROXIMATE_SEEK_TOLERANCE_US = 15_000_000L
        private const val APPROXIMATE_SEEK_MAX_PROBES = 4
        private const val APPROXIMATE_SEEK_SAMPLE_PROBE_STEPS = 512
        private const val APPROXIMATE_SEEK_SCAN_BEHIND_BYTES = 1L * 1024L * 1024L
        private const val APPROXIMATE_SEEK_SCAN_AHEAD_BYTES = 32L * 1024L * 1024L
        private const val APPROXIMATE_SEEK_SCAN_BUFFER_BYTES = 64 * 1024
    }
}
