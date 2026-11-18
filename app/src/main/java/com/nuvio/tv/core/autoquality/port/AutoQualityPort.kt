package com.nuvio.tv.core.autoquality.port

import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.CoroutineScope

/**
 * forge M4b (contract A): the door for the copy of Echo's hot swap (route 4A, `echo/AutoQualityHotSwap.kt`, next step).
 * Prepared now so the copy only swaps its receiver; nothing calls it while AutoQualityFlags.HOT_SWAP_ENABLED is false.
 */

/** A source's playback address, as Echo's StreamPlayback (no addresses outside the player). */
data class StreamPlayback(
    val url: String,
    val headers: Map<String, String>,
    val filename: String? = null,
    val responseHeaders: Map<String, String>? = null,
    val mimeType: String? = null,
    val cacheKey: String? = null,
)

/** Echo's FileKeyframes: where the keyframes are. In M4b always null (swap at the point, no alignment; contract A). */
fun interface Keyframes {
    data class Summary(val positionsMs: List<Long>)
    fun read(url: String, headers: Map<String, String>, userAgent: String?, durationMs: Long): Summary?

    companion object { val NONE = Keyframes { _, _, _, _ -> null } }
}

/** Echo's BetaFeatures for the hot swap: the announcement's length (Echo's default). */
data class HotSwapSettings(val confirmSeconds: Int = 5)

/** Everything Echo's hot swap uses from its controller (contract A list); implemented on Kicia's player in the next step. */
interface AutoQualityHost {
    val scope: CoroutineScope
    fun playerSettings(): HotSwapSettings
    fun isUsingMpv(): Boolean
    fun dvConversionActive(): Boolean
    fun introActive(): Boolean
    fun listeningLanguage(): String?
    fun hasPlayableAudio(): Boolean
    fun keepLanguageOnNextSource(language: String?)
    fun keepContentOnReset(on: Boolean)
    fun applySelectedStream(pick: Stream, playback: StreamPlayback)
    fun persistForReuse(pick: Stream)
    fun notice(title: String, text: String)
    /** Route 4B with Echo's session (K6) when the hot swap cannot be done. */
    fun usualSwitch(pick: Stream, positionMs: Long?, silent: Boolean)
    fun backToOriginal(original: Stream?, positionMs: Long?)
    /** 8 MiB on low-RAM or 32-bit devices instead of Echo's 24 MiB (contract B; to confirm by measurement in M6). */
    fun preloadLimitBytes(): Long
}

/**
 * forge M4b: what the copied hot swap (`echo/AutoQualityHotSwap.kt`) needs beyond [AutoQualityHost] — the ExoPlayer
 * parts, Kicia's media source factory and the announcement. Implemented by PlayerAutoQualityHost.
 */
interface HotSwapHost : AutoQualityHost {
    /** The playing ExoPlayer, or null (mpv / none). */
    fun exoPlayer(): androidx.media3.exoplayer.ExoPlayer?
    fun trackSelector(): androidx.media3.exoplayer.trackselection.TrackSelector?
    fun allocator(): androidx.media3.exoplayer.upstream.Allocator?
    fun bandwidthMeter(): androidx.media3.exoplayer.upstream.BandwidthMeter
    /** Kicia's PlayerMediaSourceFactory for [playback], with the playing source's addon subtitles and audio delay. */
    fun buildMediaSource(playback: StreamPlayback): androidx.media3.exoplayer.source.MediaSource
    fun unlockStartupPrefetch()
    fun streamPlayback(pick: Stream): StreamPlayback?
    fun origin(): Stream?
    fun durationMs(): Long
    /** The automatic mode may still change the source (Echo: uiState.autoQualityActive). */
    fun autoQualityActive(): Boolean
    fun sourceSwitching(): Boolean
    fun hasError(): Boolean
    fun clearError()
    /** Echo: applySelectedStreamState + persist + retry flags reset + uiState after the swap. */
    fun afterSwap(pick: Stream, playback: StreamPlayback, isDowngrade: Boolean)
    /** Echo's countdown notice: shown [ms], OK cancels (Kicia: the M4c announcement). */
    fun announce(text: String, ms: Long)
    fun cancelAnnounced(byViewer: Boolean)
    fun blockLevel(level: com.nuvio.tv.core.autoquality.echo.StreamQuality)
    fun markHotSwapFailed(key: String)
    fun markSilent(key: String)
    /** The way back through Echo's switch session (K6), quiet and returning. */
    fun backThroughSession(original: Stream, resumeAtMs: Long, durationMs: Long, label: String)
    fun context(): android.content.Context
    fun httpClient(): okhttp3.OkHttpClient
    fun userAgent(): String
    // pipeline checks (Echo read them from its settings and helpers)
    suspend fun dolbyVisionConvertedPerFile(): Boolean
    suspend fun frameRateMatchingOff(): Boolean
    suspend fun resolutionMatchingOn(): Boolean
    fun playingFrameRate(): Float
    suspend fun newFrameRate(playback: StreamPlayback): Float?
    fun keyframes(): Keyframes
}
