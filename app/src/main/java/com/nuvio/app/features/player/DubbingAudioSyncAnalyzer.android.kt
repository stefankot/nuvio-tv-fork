package com.nuvio.app.features.player

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.Format
import androidx.media3.datasource.DataSource
import androidx.media3.decoder.ffmpeg.DubbingFfmpegPcmDecoder
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext

internal data class DubbingAnalysisSource(
    val url: String,
    val requestHeaders: Map<String, String>,
    val audioTrackOrdinal: Int,
    val audioTrackHint: DubbingAudioTrackHint? = null,
    val sourceLabel: String = "selecionada",
    val expectedDurationMs: Long? = null,
    val dataSourceFactory: (() -> DataSource)? = null,
    val dataSourceRequestHeaders: Map<String, String>? = null,
)

internal data class DubbingAudioTrackHint(
    val language: String? = null,
    val mimeType: String? = null,
    val channelCount: Int? = null,
)

internal data class DubbingAudioTrackCandidate(
    val extractorIndex: Int,
    val audioOrdinal: Int,
    val language: String?,
    val mimeType: String?,
    val channelCount: Int?,
)

/**
 * Optional platform extractor used before the Media3/MediaExtractor fallback chain.
 *
 * TV injects a headless libmpv implementation; Mobile deliberately keeps the default `null`
 * until that platform is migrated and validated independently.
 */
internal fun interface DubbingFastFeatureExtractor {
    suspend fun extract(
        source: DubbingAnalysisSource,
        startMs: Long,
        durationMs: Long,
        cancellation: DubbingAnalysisCancellationToken,
        onProgress: (Float) -> Unit,
    ): DubbingFeatureSeries
}

private const val LANGUAGE_MATCH_SCORE = 100

internal fun resolveDubbingAudioTrack(
    candidates: List<DubbingAudioTrackCandidate>,
    requestedOrdinal: Int,
    hint: DubbingAudioTrackHint?,
): DubbingAudioTrackCandidate? {
    if (candidates.isEmpty()) return null
    val requested = candidates.firstOrNull { it.audioOrdinal == requestedOrdinal }
    if (hint != null) {
        val ranked = candidates.map { candidate -> candidate to candidate.matchScore(hint) }
        val bestScore = ranked.maxOf { it.second }
        val best = ranked.filter { it.second == bestScore }
        val requestedScore = requested?.matchScore(hint) ?: Int.MIN_VALUE
        // Language is the only metadata consistently shared by ExoPlayer/libmpv and
        // MediaExtractor. Prefer its unique match even when either player enumerated
        // unsupported/adaptive tracks differently.
        if (bestScore >= LANGUAGE_MATCH_SCORE && best.size == 1 && bestScore > requestedScore) {
            return best.single().first
        }
        if (requested == null && bestScore > 0 && best.size == 1) return best.single().first
    }
    return requested ?: candidates.singleOrNull()
}

private fun DubbingAudioTrackCandidate.matchScore(hint: DubbingAudioTrackHint): Int {
    var score = 0
    val expectedLanguage = normalizeDubbingLanguage(hint.language)
    val actualLanguage = normalizeDubbingLanguage(language)
    if (expectedLanguage != null && actualLanguage == expectedLanguage) score += LANGUAGE_MATCH_SCORE
    if (hint.mimeType != null && mimeType != null && hint.mimeType.equals(mimeType, ignoreCase = true)) score += 20
    if (hint.channelCount != null && channelCount == hint.channelCount) score += 10
    return score
}

private fun normalizeDubbingLanguage(value: String?): String? {
    val language = value?.trim()?.lowercase()?.substringBefore('-')?.substringBefore('_')
        ?.takeIf { it.isNotBlank() } ?: return null
    return when (language) {
        "por" -> "pt"
        "eng" -> "en"
        "spa" -> "es"
        "fra", "fre" -> "fr"
        "deu", "ger" -> "de"
        "ita" -> "it"
        "jpn" -> "ja"
        "kor" -> "ko"
        "zho", "chi" -> "zh"
        "rus" -> "ru"
        "ara" -> "ar"
        "hin" -> "hi"
        else -> language
    }
}

/** Audio-only Android implementation shared by the Mobile and TV patch sets. */
internal class DubbingAudioSyncAnalyzer(
    private val fastFeatureExtractor: DubbingFastFeatureExtractor? = null,
) {
    // The TV fast path derives its dialogue-reduced stereo signal as L-R. Keep Media3
    // fallbacks in the same feature domain when one of the two sources cannot use libmpv.
    // Mobile has no fast extractor and deliberately retains its established L+R behavior.
    private val useStereoDifferenceForEffects = fastFeatureExtractor != null
    private val analysisMutex = Mutex()
    private val activeCancellation = AtomicReference<DubbingAnalysisCancellationToken?>(null)

    fun cancelActiveAnalysis() {
        activeCancellation.get()?.cancel()
    }

    suspend fun analyze(
        sourceA: DubbingAnalysisSource,
        sourceB: DubbingAnalysisSource,
        request: DubbingSyncRequest,
        onProgress: (DubbingSyncProgress) -> Unit,
    ): DubbingSyncResult = withContext(Dispatchers.IO) {
        analysisMutex.withLock {
            coroutineContext.ensureActive()
            val cancellation = DubbingAnalysisCancellationToken()
            check(activeCancellation.compareAndSet(null, cancellation)) { "Já existe uma análise de dublagem ativa." }
            try {
                runCatching {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                    analyzeInternal(sourceA, sourceB, request, onProgress, cancellation)
                }.getOrElse { error ->
                    coroutineContext.ensureActive()
                    DubbingSyncResult.Failure(error.toUserMessage())
                }
            } finally {
                activeCancellation.compareAndSet(cancellation, null)
            }
        }
    }

    private suspend fun analyzeInternal(
        sourceA: DubbingAnalysisSource,
        sourceB: DubbingAnalysisSource,
        request: DubbingSyncRequest,
        onProgress: (DubbingSyncProgress) -> Unit,
        cancellation: DubbingAnalysisCancellationToken,
    ): DubbingSyncResult {
        val anchorMs = request.anchorPositionMs.coerceAtLeast(0L)
        val windowMs = request.windowDurationMs.coerceIn(MIN_WINDOW_MS, MAX_WINDOW_MS)
        val analysisStartedMs = SystemClock.elapsedRealtime()
        val maximumRadiusMs = if (request.mode == DubbingSyncMode.EXTENDED) {
            EXTENDED_RADIUS_MS
        } else {
            QUICK_RADIUS_MS
        }
        var radiusMs = if (request.mode == DubbingSyncMode.QUICK) {
            QUICK_INITIAL_RADIUS_MS
        } else {
            maximumRadiusMs
        }
        // Keep the whole candidate range after B=0. Near the beginning this moves A's
        // single window forward just enough to represent negative offsets without asking
        // either extractor to read a time that cannot exist.
        val predictedBAtAnchorMs = anchorMs + request.currentOffsetMs
        val startGuardMs = (maximumRadiusMs - predictedBAtAnchorMs).coerceAtLeast(0L)
        val referenceStartMs = anchorMs + startGuardMs
        val predictedBStartMs = referenceStartMs + request.currentOffsetMs
        val searchStartMs = (predictedBStartMs - radiusMs).coerceAtLeast(0L)
        val searchDurationMs = windowMs + radiusMs * 2
        Log.i(
            TAG,
            "Análise iniciada: janela=${windowMs}ms, raioInicial=${radiusMs}ms, " +
                "raioMáximo=${maximumRadiusMs}ms, " +
                "âncora=${anchorMs}ms, deltaInicial=${request.currentOffsetMs}ms.",
        )

        onProgress(DubbingSyncProgress(0.03f, "Lendo trecho original de ${windowMs / 1_000}s…"))
        val reference = decodeFeatures(sourceA, referenceStartMs, windowMs, cancellation) { fraction ->
            onProgress(DubbingSyncProgress(0.03f + fraction * 0.27f, "Lendo trecho original de ${windowMs / 1_000}s…"))
        }
        if (reference.size < MIN_FEATURE_FRAMES) {
            return DubbingSyncResult.NoMatch("O trecho original não contém áudio suficiente.", false)
        }

        onProgress(DubbingSyncProgress(0.30f, "Comparando música e efeitos em B…"))
        var search = decodeFeatures(sourceB, searchStartMs, searchDurationMs, cancellation) { fraction ->
            val share = if (radiusMs < maximumRadiusMs) 0.35f else 0.60f
            onProgress(DubbingSyncProgress(0.30f + fraction * share, "Comparando música e efeitos em B…"))
        }
        onProgress(DubbingSyncProgress(0.92f, "Calculando correlação…"))
        val correlationStartedMs = SystemClock.elapsedRealtime()
        var candidatePeak = DubbingAudioSyncDsp.correlateFeatures(reference, search)
        if (radiusMs < maximumRadiusMs &&
            (candidatePeak == null || candidatePeak.isAtSearchBoundary(reference, search))
        ) {
            radiusMs = maximumRadiusMs
            val expandedSearchStartMs = (predictedBStartMs - radiusMs).coerceAtLeast(0L)
            val expandedSearchDurationMs = windowMs + radiusMs * 2
            Log.i(
                TAG,
                "Busca inicial de B sem pico interno; expandindo automaticamente para ±${radiusMs}ms.",
            )
            onProgress(DubbingSyncProgress(0.65f, "Ampliando a busca em B…"))
            search = decodeFeatures(
                sourceB,
                expandedSearchStartMs,
                expandedSearchDurationMs,
                cancellation,
            ) { fraction ->
                onProgress(DubbingSyncProgress(0.65f + fraction * 0.25f, "Ampliando a busca em B…"))
            }
            onProgress(DubbingSyncProgress(0.92f, "Calculando correlação ampliada…"))
            candidatePeak = DubbingAudioSyncDsp.correlateFeatures(reference, search)
        }
        val peak = candidatePeak ?: return DubbingSyncResult.NoMatch(
                message = "Não houve sinal compartilhado suficiente para sincronizar automaticamente.",
                canTryExtended = request.mode == DubbingSyncMode.QUICK,
            )
        Log.i(
            TAG,
            "Pico candidato: offset=${peak.offsetMs}ms, score=${peak.score}, " +
                "PSR=${peak.peakToSidelobeRatio}, razão=${peak.peakToSecondRatio}.",
        )
        if (peak.isAtSearchBoundary(reference, search)) {
            return DubbingSyncResult.NoMatch(
                message = "O melhor pico ficou no limite da busca e não é seguro aplicá-lo. Tente a busca ampliada ou outro trecho.",
                canTryExtended = request.mode == DubbingSyncMode.QUICK,
            )
        }
        val isIndependentlyHighConfidence =
            peak.peakToSidelobeRatio >= HIGH_PSR && peak.peakToSecondRatio >= HIGH_RATIO
        val corroboratesCurrentOffset = !isIndependentlyHighConfidence &&
            (peak.corroboratesCurrentOffset(request.currentOffsetMs) ||
                peak.corroboratesStrongZeroOffset(request.currentOffsetMs))
        val independentSliceConsensus = if (
            fastFeatureExtractor != null && !isIndependentlyHighConfidence && !corroboratesCurrentOffset
        ) {
            DubbingAudioSyncDsp.correlateIndependentSlices(reference, search, peak)
        } else {
            null
        }
        independentSliceConsensus?.let { consensus ->
            Log.i(
                TAG,
                "Consenso independente: primeiro=${consensus.firstSlice.offsetMs}ms " +
                    "(PSR=${consensus.firstSlice.peakToSidelobeRatio}, " +
                    "razão=${consensus.firstSlice.peakToSecondRatio}), " +
                    "segundo=${consensus.secondSlice.offsetMs}ms " +
                    "(PSR=${consensus.secondSlice.peakToSidelobeRatio}, " +
                "razão=${consensus.secondSlice.peakToSecondRatio}).",
            )
        }
        val independentWindowPeak = if (
            fastFeatureExtractor != null &&
            !isIndependentlyHighConfidence &&
            !corroboratesCurrentOffset &&
            independentSliceConsensus == null &&
            peak.canRequestIndependentWindow()
        ) {
            correlateIndependentWindow(
                sourceA = sourceA,
                sourceB = sourceB,
                referenceStartMs = referenceStartMs,
                firstWindowDurationMs = windowMs,
                firstPeak = peak,
                cancellation = cancellation,
                onProgress = onProgress,
            )
        } else {
            null
        }
        val corroboratesIndependentWindow = independentWindowPeak?.let {
            peak.isCorroboratedByIndependentWindow(it)
        } == true
        if (!corroboratesCurrentOffset && independentSliceConsensus == null &&
            !corroboratesIndependentWindow &&
            (peak.peakToSidelobeRatio < MIN_COARSE_PSR || peak.peakToSecondRatio < MIN_COARSE_RATIO)
        ) {
            return DubbingSyncResult.NoMatch(
                message = "A correlação encontrou picos ambíguos. Tente outro trecho ou ajuste manualmente.",
                canTryExtended = request.mode == DubbingSyncMode.QUICK,
            )
        }
        val confidence = when {
            isIndependentlyHighConfidence -> DubbingSyncConfidence.HIGH
            corroboratesCurrentOffset -> DubbingSyncConfidence.HIGH
            independentSliceConsensus != null -> DubbingSyncConfidence.HIGH
            corroboratesIndependentWindow -> DubbingSyncConfidence.HIGH
            peak.peakToSidelobeRatio >= MEDIUM_PSR && peak.peakToSecondRatio >= MEDIUM_RATIO ->
                DubbingSyncConfidence.MEDIUM
            else -> null
        } ?: return DubbingSyncResult.NoMatch(
            message = "O trecho atual não produziu um pico confiável. Tente um trecho maior ou a busca ampliada.",
            canTryExtended = request.mode == DubbingSyncMode.QUICK,
        )

        val resolvedOffsetMs = if (corroboratesCurrentOffset) request.currentOffsetMs else peak.offsetMs
        val anchorAgreementMs = kotlin.math.abs(peak.offsetMs - request.currentOffsetMs)
        onProgress(DubbingSyncProgress(1f, "Sincronização concluída"))
        Log.i(
            TAG,
            "Análise concluída: offset=${resolvedOffsetMs}ms, candidato=${peak.offsetMs}ms, " +
                "confirmaAtual=$corroboratesCurrentOffset, consensoFatias=${independentSliceConsensus != null}, " +
                "consensoTrecho=${corroboratesIndependentWindow}, correlação=" +
                "${SystemClock.elapsedRealtime() - correlationStartedMs}ms, total=" +
                "${SystemClock.elapsedRealtime() - analysisStartedMs}ms.",
        )
        return DubbingSyncResult.Match(
            offsetMs = DubbingTiming.clampOffset(resolvedOffsetMs),
            confidence = confidence,
            peakToSidelobeRatio = peak.peakToSidelobeRatio,
            anchorAgreementMs = anchorAgreementMs,
        )
    }

    private suspend fun correlateIndependentWindow(
        sourceA: DubbingAnalysisSource,
        sourceB: DubbingAnalysisSource,
        referenceStartMs: Long,
        firstWindowDurationMs: Long,
        firstPeak: DubbingCorrelationPeak,
        cancellation: DubbingAnalysisCancellationToken,
        onProgress: (DubbingSyncProgress) -> Unit,
    ): DubbingCorrelationPeak? {
        val confirmationDurationMs = INDEPENDENT_WINDOW_DURATION_MS.coerceAtMost(firstWindowDurationMs)
        val forwardStartMs = referenceStartMs + firstWindowDurationMs + INDEPENDENT_WINDOW_GAP_MS
        val forwardFits = sourceA.expectedDurationMs?.let {
            forwardStartMs + confirmationDurationMs <= it
        } != false
        val backwardStartMs = referenceStartMs - INDEPENDENT_WINDOW_GAP_MS - confirmationDurationMs
        val confirmationReferenceStartMs = when {
            forwardFits -> forwardStartMs
            backwardStartMs >= 0L -> backwardStartMs
            else -> return null
        }
        Log.i(
            TAG,
            "Pico limítrofe; confirmando em trecho independente a partir de " +
                "${confirmationReferenceStartMs}ms.",
        )

        onProgress(DubbingSyncProgress(0.93f, "Confirmando em outro trecho de A…"))
        val confirmationReference = decodeFeatures(
            sourceA,
            confirmationReferenceStartMs,
            confirmationDurationMs,
            cancellation,
        ) { fraction ->
            onProgress(DubbingSyncProgress(0.93f + fraction * 0.025f, "Confirmando em outro trecho de A…"))
        }
        if (confirmationReference.size < MIN_FEATURE_FRAMES) return null

        val predictedBStartMs = confirmationReferenceStartMs + firstPeak.offsetMs
        val confirmationSearchStartMs =
            (predictedBStartMs - INDEPENDENT_WINDOW_RADIUS_MS).coerceAtLeast(0L)
        val leftRadiusMs = predictedBStartMs - confirmationSearchStartMs
        val confirmationSearchDurationMs =
            confirmationDurationMs + leftRadiusMs + INDEPENDENT_WINDOW_RADIUS_MS
        onProgress(DubbingSyncProgress(0.955f, "Confirmando o mesmo ponto em B…"))
        val confirmationSearch = decodeFeatures(
            sourceB,
            confirmationSearchStartMs,
            confirmationSearchDurationMs,
            cancellation,
        ) { fraction ->
            onProgress(DubbingSyncProgress(0.955f + fraction * 0.035f, "Confirmando o mesmo ponto em B…"))
        }
        val confirmationPeak = DubbingAudioSyncDsp.correlateFeatures(
            confirmationReference,
            confirmationSearch,
        ) ?: return null
        val atBoundary = confirmationPeak.isAtSearchBoundary(confirmationReference, confirmationSearch)
        Log.i(
            TAG,
            "Pico do trecho independente: offset=${confirmationPeak.offsetMs}ms, " +
                "score=${confirmationPeak.score}, PSR=${confirmationPeak.peakToSidelobeRatio}, " +
                "razão=${confirmationPeak.peakToSecondRatio}, limite=$atBoundary, " +
                "concorda=${firstPeak.isCorroboratedByIndependentWindow(confirmationPeak)}.",
        )
        return confirmationPeak.takeUnless { atBoundary }
    }

    private suspend fun decodeFeatures(
        source: DubbingAnalysisSource,
        startMs: Long,
        durationMs: Long,
        cancellation: DubbingAnalysisCancellationToken,
        onProgress: (Float) -> Unit = {},
    ): DubbingFeatureSeries {
        val startedMs = SystemClock.elapsedRealtime()
        // A TV source backed by a DataSourceFactory already shares the player's HTTP/cache
        // path and is substantially cheaper through Media3. The libmpv fast path is reserved
        // for relay-backed sources such as B, where it can seek and decode without competing
        // with the main player's upstream connection.
        val eligibleFastExtractor = fastFeatureExtractor?.takeIf { source.dataSourceFactory == null }
        if (fastFeatureExtractor != null && eligibleFastExtractor == null) {
            Log.i(TAG, "${source.sourceLabel} usará Media3 integrado; libmpv auxiliar não é necessário.")
        }
        eligibleFastExtractor?.let { extractor ->
            try {
                return extractor.extract(source, startMs, durationMs, cancellation, onProgress).also { features ->
                    Log.i(
                        TAG,
                        "Trecho ${source.sourceLabel} concluído pelo extrator rápido em " +
                            "${SystemClock.elapsedRealtime() - startedMs}ms " +
                            "(${durationMs}ms solicitados, ${features.size} frames).",
                    )
                }
            } catch (error: Throwable) {
                coroutineContext.ensureActive()
                cancellation.throwIfCancelled()
                Log.w(
                    TAG,
                    "Extrator rápido falhou para ${source.sourceLabel}; usando fallback Android seguro.",
                    error,
                )
                onProgress(0f)
            }
        }
        val collector = FeatureSink()
        decode(source, startMs, durationMs, collector, cancellation, onProgress)
        return collector.features.build().also { features ->
            Log.i(
                TAG,
                "Trecho ${source.sourceLabel} concluído em ${SystemClock.elapsedRealtime() - startedMs}ms " +
                    "(${durationMs}ms solicitados, ${features.size} frames).",
            )
        }
    }

    private suspend fun decode(
        source: DubbingAnalysisSource,
        startMs: Long,
        durationMs: Long,
        sink: ResampledSink,
        cancellation: DubbingAnalysisCancellationToken,
        onProgress: (Float) -> Unit,
    ) {
        require(durationMs > 0L)
        val media3Sink = CountingResampledSink(sink)
        try {
            decodeWithMedia3(
                source,
                startMs,
                durationMs,
                media3Sink,
                cancellation,
                onProgress,
                allowLinearScan = false,
            )
        } catch (media3Error: Throwable) {
            coroutineContext.ensureActive()
            if (media3Sink.outputCount > 0L) throw media3Error
            if (media3Error is DubbingLinearScanRequiredException) {
                Log.i(TAG, "A fonte ${source.sourceLabel} não oferece seek Media3; tentando o seek nativo antes da varredura linear.")
                try {
                    decodeWithPlatformExtractor(source, startMs, durationMs, sink, onProgress)
                    return
                } catch (platformError: Throwable) {
                    coroutineContext.ensureActive()
                    Log.w(
                        TAG,
                        "O seek nativo falhou para ${source.sourceLabel}; retomando Media3 com varredura linear limitada.",
                        platformError,
                    )
                    decodeWithMedia3(
                        source,
                        startMs,
                        durationMs,
                        media3Sink,
                        cancellation,
                        onProgress,
                        allowLinearScan = true,
                    )
                    return
                }
            }
            Log.w(TAG, "Media3 falhou antes de produzir PCM para ${source.sourceLabel}; tentando extrator Android.", media3Error)
            try {
                decodeWithPlatformExtractor(source, startMs, durationMs, sink, onProgress)
            } catch (platformError: Throwable) {
                coroutineContext.ensureActive()
                val media3Message = media3Error.message?.takeIf { it.isNotBlank() } ?: media3Error.javaClass.simpleName
                val platformMessage = platformError.message?.takeIf { it.isNotBlank() } ?: platformError.javaClass.simpleName
                error("Falha ao extrair ${source.sourceLabel}. Media3: $media3Message; Android: $platformMessage")
            }
        }
    }

    private suspend fun decodeWithMedia3(
        source: DubbingAnalysisSource,
        startMs: Long,
        durationMs: Long,
        sink: ResampledSink,
        cancellation: DubbingAnalysisCancellationToken,
        onProgress: (Float) -> Unit,
        allowLinearScan: Boolean,
    ) {
        val startUs = startMs.coerceAtLeast(0L) * 1_000L
        val endUs = startUs + durationMs * 1_000L
        DubbingMedia3AudioReader(
            source = source,
            startUs = startUs,
            endUs = endUs,
            cancellationToken = cancellation,
            onLinearScanProgress = { fraction ->
                onProgress(fraction.coerceIn(0f, 1f) * LINEAR_SCAN_PROGRESS_SHARE)
            },
        ).use { reader ->
            if (reader.requiresLinearScan && !allowLinearScan) {
                throw DubbingLinearScanRequiredException()
            }
            val decodeProgress: (Float) -> Unit = if (reader.requiresLinearScan || reader.usedApproximateSeek) {
                { fraction ->
                    onProgress(
                        LINEAR_SCAN_PROGRESS_SHARE +
                            fraction.coerceIn(0f, 1f) * (1f - LINEAR_SCAN_PROGRESS_SHARE)
                    )
                }
            } else {
                onProgress
            }
            val mime = reader.format.sampleMimeType
                ?.takeIf { it.startsWith("audio/") }
                ?: error("O demux Media3 não informou o codec de ${source.sourceLabel}.")
            if (FfmpegLibrary.isAvailable() && FfmpegLibrary.supportsFormat(mime)) {
                Log.i(TAG, "Decodificando ${source.sourceLabel} via Media3 + FFmpeg ($mime).")
                decodeMedia3WithFfmpeg(reader, startUs, endUs, sink, decodeProgress)
            } else {
                Log.i(TAG, "Decodificando ${source.sourceLabel} via Media3 + MediaCodec ($mime).")
                decodeMedia3WithMediaCodec(reader, startUs, endUs, sink, decodeProgress)
            }
        }
    }

    private suspend fun decodeMedia3WithFfmpeg(
        reader: DubbingMedia3AudioReader,
        startUs: Long,
        endUs: Long,
        sink: ResampledSink,
        onProgress: (Float) -> Unit,
    ) {
        DubbingFfmpegPcmDecoder(reader.format).use { decoder ->
            val resampler = StreamingDownmixerResampler(
                sink,
                startUs,
                endUs,
                useStereoDifferenceForEffects,
            )
            var sample: DubbingEncodedAudioSample? = reader.nextSample()
            var eosQueued = false
            var outputEnded = false
            var lastProgress = -1f
            while (!outputEnded) {
                coroutineContext.ensureActive()
                var madeProgress = false
                if (!eosQueued && decoder.tryQueue(sample?.data, sample?.timeUs ?: endUs)) {
                    madeProgress = true
                    if (sample == null) {
                        eosQueued = true
                    } else {
                        sample = reader.nextSample()
                    }
                }
                while (true) {
                    val state = decoder.drain { data, timeUs, sampleRate, channels, encoding ->
                        resampler.consume(data, data.position(), data.remaining(), timeUs, sampleRate, channels, encoding)
                        val progress = ((timeUs - startUs).toFloat() / (endUs - startUs)).coerceIn(0f, 1f)
                        if (progress - lastProgress >= 0.02f) {
                            lastProgress = progress
                            onProgress(progress)
                        }
                    }
                    if (state == DubbingFfmpegPcmDecoder.DrainState.NO_OUTPUT) break
                    madeProgress = true
                    if (state == DubbingFfmpegPcmDecoder.DrainState.END_OF_STREAM) {
                        outputEnded = true
                        break
                    }
                }
                if (!madeProgress) yield()
            }
        }
        onProgress(1f)
    }

    private suspend fun decodeMedia3WithMediaCodec(
        reader: DubbingMedia3AudioReader,
        startUs: Long,
        endUs: Long,
        sink: ResampledSink,
        onProgress: (Float) -> Unit,
    ) {
        val inputFormat = reader.format.toAndroidMediaFormat()
        val decoderName = MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(inputFormat)
            ?: error("Nenhum decoder está disponível para ${reader.format.sampleMimeType}.")
        var codec: MediaCodec? = null
        try {
            codec = MediaCodec.createByCodecName(decoderName)
            codec.configure(inputFormat, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            val resampler = StreamingDownmixerResampler(
                sink,
                startUs,
                endUs,
                useStereoDifferenceForEffects,
            )
            var sample: DubbingEncodedAudioSample? = reader.nextSample()
            var inputEnded = false
            var outputEnded = false
            var outputFormat: MediaFormat? = null
            var lastProgress = -1f
            while (!outputEnded) {
                coroutineContext.ensureActive()
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val input = codec.getInputBuffer(inputIndex) ?: error("Decoder sem buffer de entrada.")
                        val current = sample
                        if (current == null) {
                            codec.queueInputBuffer(inputIndex, 0, 0, endUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            input.clear()
                            input.put(current.data)
                            codec.queueInputBuffer(inputIndex, 0, current.data.size, current.timeUs, 0)
                            sample = reader.nextSample()
                        }
                    }
                }
                when (val outputIndex = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outputFormat = codec.outputFormat
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        val buffer = codec.getOutputBuffer(outputIndex)
                        val format = outputFormat ?: codec.outputFormat
                        if (buffer != null && info.size > 0) resampler.consume(buffer, info, format)
                        codec.releaseOutputBuffer(outputIndex, false)
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        val progress = ((info.presentationTimeUs - startUs).toFloat() / (endUs - startUs)).coerceIn(0f, 1f)
                        if (progress - lastProgress >= 0.02f) {
                            lastProgress = progress
                            onProgress(progress)
                        }
                    }
                }
            }
            onProgress(1f)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
        }
    }

    private suspend fun decodeWithPlatformExtractor(
        source: DubbingAnalysisSource,
        startMs: Long,
        durationMs: Long,
        sink: ResampledSink,
        onProgress: (Float) -> Unit,
    ) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(source.url, source.requestHeaders)
            val trackIndex = extractor.resolveAudioTrack(source)
                ?: error("A faixa de áudio ${source.sourceLabel} não pôde ser localizada nesta fonte.")
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?.takeIf { it.startsWith("audio/") }
                ?: error("Formato de áudio inválido.")
            extractor.selectTrack(trackIndex)
            val startUs = startMs.coerceAtLeast(0L) * 1_000L
            val endUs = startUs + durationMs * 1_000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var outputFormat: MediaFormat? = null
            val resampler = StreamingDownmixerResampler(
                sink,
                startUs,
                endUs,
                useStereoDifferenceForEffects,
            )
            var lastProgress = -1f

            while (!outputEnded) {
                coroutineContext.ensureActive()
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val input = codec.getInputBuffer(inputIndex) ?: error("Decoder sem buffer de entrada.")
                        val size = extractor.readSampleData(input, 0)
                        if (size < 0 || extractor.sampleTime > endUs + DECODER_PREROLL_US) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, extractor.sampleFlags)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outputFormat = codec.outputFormat
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        val buffer = codec.getOutputBuffer(outputIndex)
                        val format = outputFormat ?: codec.outputFormat
                        if (buffer != null && info.size > 0 && info.presentationTimeUs < endUs + DECODER_PREROLL_US) {
                            resampler.consume(buffer, info, format)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 || info.presentationTimeUs >= endUs
                        val progress = ((info.presentationTimeUs - startUs).toFloat() / (endUs - startUs)).coerceIn(0f, 1f)
                        if (progress - lastProgress >= 0.02f) {
                            lastProgress = progress
                            onProgress(progress)
                        }
                    }
                }
            }
            onProgress(1f)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun MediaExtractor.resolveAudioTrack(source: DubbingAnalysisSource): Int? {
        val candidates = mutableListOf<DubbingAudioTrackCandidate>()
        var audioOrdinal = 0
        for (index in 0 until trackCount) {
            val format = getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (!mime.startsWith("audio/")) continue
            candidates += DubbingAudioTrackCandidate(
                extractorIndex = index,
                audioOrdinal = audioOrdinal,
                language = format.getString(MediaFormat.KEY_LANGUAGE),
                mimeType = mime,
                channelCount = format.dubbingOptionalInt(MediaFormat.KEY_CHANNEL_COUNT),
            )
            audioOrdinal++
        }
        return resolveDubbingAudioTrack(candidates, source.audioTrackOrdinal, source.audioTrackHint)?.extractorIndex
    }

    private interface ResampledSink {
        fun add(timeUs: Long, general: Float, effects: Float)
    }

    private class CountingResampledSink(private val delegate: ResampledSink) : ResampledSink {
        var outputCount: Long = 0L
            private set

        override fun add(timeUs: Long, general: Float, effects: Float) {
            delegate.add(timeUs, general, effects)
            outputCount++
        }
    }

    private class FeatureSink : ResampledSink {
        val features = DubbingAudioFeatureCollector()
        override fun add(timeUs: Long, general: Float, effects: Float) = features.add(timeUs, general, effects)
    }

    private class StreamingDownmixerResampler(
        private val sink: ResampledSink,
        private val startUs: Long,
        private val endUs: Long,
        private val useStereoDifferenceForEffects: Boolean,
    ) {
        private var nextOutputUs = startUs
        private var configuredSampleRate = 0
        private val generalFilters = Array(3) { LowPass() }
        private val effectsFilters = Array(3) { LowPass() }

        fun consume(buffer: ByteBuffer, info: MediaCodec.BufferInfo, format: MediaFormat) {
            val sampleRate = format.dubbingIntValue(MediaFormat.KEY_SAMPLE_RATE, 48_000)
            val channels = format.dubbingIntValue(MediaFormat.KEY_CHANNEL_COUNT, 2).coerceAtLeast(1)
            val encoding = format.dubbingIntValue(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            consume(buffer, info.offset, info.size, info.presentationTimeUs, sampleRate, channels, encoding)
        }

        fun consume(
            buffer: ByteBuffer,
            offset: Int,
            size: Int,
            presentationTimeUs: Long,
            sampleRate: Int,
            channelCount: Int,
            encoding: Int,
        ) {
            val channels = channelCount.coerceAtLeast(1)
            val bytesPerSample = when (encoding) {
                AudioFormat.ENCODING_PCM_FLOAT, AudioFormat.ENCODING_PCM_32BIT -> 4
                AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
                AudioFormat.ENCODING_PCM_8BIT -> 1
                else -> 2
            }
            if (sampleRate != configuredSampleRate) {
                configuredSampleRate = sampleRate
                generalFilters.forEach { it.configure(sampleRate, LOW_PASS_HZ) }
                effectsFilters.forEach { it.configure(sampleRate, LOW_PASS_HZ) }
            }
            val frameBytes = bytesPerSample * channels
            if (frameBytes <= 0) return
            val frames = size / frameBytes
            val data = buffer.duplicate().order(ByteOrder.nativeOrder())
            data.position(offset)
            data.limit(offset + frames * frameBytes)
            val channelValues = FloatArray(channels)
            for (frame in 0 until frames) {
                for (channel in 0 until channels) channelValues[channel] = data.readPcm(encoding)
                val frameTimeUs = presentationTimeUs + frame * 1_000_000L / sampleRate
                var general: Float
                var effects: Float
                when (channelValues.size) {
                    1 -> {
                        general = channelValues[0]
                        effects = channelValues[0]
                    }
                    2 -> {
                        val mono = (channelValues[0] + channelValues[1]) * 0.5f
                        general = mono
                        effects = if (useStereoDifferenceForEffects) {
                            (channelValues[0] - channelValues[1]) * 0.5f
                        } else {
                            mono
                        }
                    }
                    else -> {
                        var generalWeight = 0f
                        var effectsWeight = 0f
                        general = 0f
                        effects = 0f
                        for (index in channelValues.indices) {
                            val value = channelValues[index]
                            val isLfe = index == 3
                            val isCenter = index == 2
                            if (!isLfe) {
                                val weight = if (isCenter) 0.35f else 1f
                                general += value * weight
                                generalWeight += weight
                            }
                            if (!isLfe && !isCenter) {
                                effects += value
                                effectsWeight += 1f
                            }
                        }
                        general /= max(1f, generalWeight)
                        effects /= max(1f, effectsWeight)
                    }
                }
                generalFilters.forEach { general = it.process(general) }
                effectsFilters.forEach { effects = it.process(effects) }
                while (nextOutputUs <= frameTimeUs && nextOutputUs < endUs) {
                    if (nextOutputUs >= startUs) sink.add(nextOutputUs, general, effects)
                    nextOutputUs += 1_000_000L / DubbingAudioFeatureCollector.TARGET_SAMPLE_RATE
                }
            }
        }

        private fun ByteBuffer.readPcm(encoding: Int): Float = when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> float.coerceIn(-1f, 1f)
            AudioFormat.ENCODING_PCM_32BIT -> int / 2_147_483_648f
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
                val b0 = get().toInt() and 0xff
                val b1 = get().toInt() and 0xff
                val b2 = get().toInt()
                ((b2 shl 16) or (b1 shl 8) or b0) / 8_388_608f
            }
            AudioFormat.ENCODING_PCM_8BIT -> ((get().toInt() and 0xff) - 128) / 128f
            else -> short / 32_768f
        }
    }

    private class LowPass {
        private var alpha = 1f
        private var state = 0f
        fun configure(sampleRate: Int, cutoffHz: Int) {
            alpha = (1.0 - exp(-2.0 * PI * cutoffHz / sampleRate)).toFloat()
            state = 0f
        }
        fun process(value: Float): Float {
            state += alpha * (value - state)
            return state
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is java.util.concurrent.CancellationException -> throw this
        else -> message?.takeIf { it.isNotBlank() } ?: "Não foi possível analisar as faixas de áudio."
    }

    companion object {
        private const val TAG = "DubbingSync"
        private const val CODEC_TIMEOUT_US = 10_000L
        private const val DECODER_PREROLL_US = 250_000L
        private const val LINEAR_SCAN_PROGRESS_SHARE = 0.20f
        private const val LOW_PASS_HZ = 3_600
        private const val EXTENDED_RADIUS_MS = 120_000L
        private const val MIN_WINDOW_MS = 15_000L
        private const val MAX_WINDOW_MS = 120_000L
        private const val MIN_FEATURE_FRAMES = 1_000
        private const val INDEPENDENT_WINDOW_DURATION_MS = 15_000L
        private const val INDEPENDENT_WINDOW_GAP_MS = 2_000L
        private const val INDEPENDENT_WINDOW_RADIUS_MS = 5_000L
        private const val MIN_COARSE_PSR = 5f
        private const val MIN_COARSE_RATIO = 1.05f
        private const val HIGH_PSR = 7f
        private const val MEDIUM_PSR = 5f
        private const val HIGH_RATIO = 1.15f
        private const val MEDIUM_RATIO = 1.08f
    }
}

private const val QUICK_RADIUS_MS = 30_000L
private const val QUICK_INITIAL_RADIUS_MS = 15_000L

private class DubbingLinearScanRequiredException : Exception()

private fun MediaFormat.dubbingIntValue(key: String, fallback: Int): Int =
    if (containsKey(key)) runCatching { getInteger(key) }.getOrDefault(fallback) else fallback

private fun MediaFormat.dubbingOptionalInt(key: String): Int? =
    if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null

private fun Format.toAndroidMediaFormat(): MediaFormat {
    val mime = sampleMimeType ?: error("Formato de áudio sem MIME.")
    return MediaFormat.createAudioFormat(
        mime,
        sampleRate.takeIf { it > 0 } ?: 48_000,
        channelCount.takeIf { it > 0 } ?: 2,
    ).also { mediaFormat ->
        if (maxInputSize > 0) mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize)
        if (bitrate > 0) mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        initializationData.forEachIndexed { index, bytes ->
            mediaFormat.setByteBuffer("csd-$index", ByteBuffer.wrap(bytes))
        }
    }
}
