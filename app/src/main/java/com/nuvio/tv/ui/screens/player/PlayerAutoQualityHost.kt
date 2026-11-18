package com.nuvio.tv.ui.screens.player

import android.os.Build
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.TrackSelector
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.nuvio.tv.core.autoquality.AutoQualitySession
import com.nuvio.tv.core.autoquality.echo.StreamQuality
import com.nuvio.tv.core.autoquality.port.HotSwapHost
import com.nuvio.tv.core.autoquality.port.HotSwapSettings
import com.nuvio.tv.core.autoquality.port.Keyframes
import com.nuvio.tv.core.autoquality.port.StreamPlayback
import com.nuvio.tv.core.player.FrameRateUtils
import com.nuvio.tv.core.player.recovery.echo.SourceSwitchSession
import com.nuvio.tv.core.player.recovery.echo.beginSourceSwitch
import com.nuvio.tv.core.player.recovery.echo.isSourceSwitching
import com.nuvio.tv.core.streams.smart.SmartPolicyHolder
import com.nuvio.tv.data.local.Dv7HandlingMode
import com.nuvio.tv.data.local.FrameRateMatchingMode
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * forge M4b (contract D): Kicia's side of the hot-swap port (route 4A) and of [com.nuvio.tv.core.autoquality.port.AutoQualityHost].
 * Short adapters only; what Kicia lacks is written here briefly or is a no-op until measured.
 */
internal class PlayerAutoQualityHost(private val p: PlayerRuntimeController) : HotSwapHost {
    override val scope: CoroutineScope get() = p.scope
    override fun playerSettings() = HotSwapSettings()
    override fun isUsingMpv() = p.isUsingMpvEngine()
    override fun dvConversionActive() = p.isExperimentalDv7ToDv81ActiveForCurrentPlayback
    override fun introActive() = false

    /** The language of the audio track being listened to, or null. */
    override fun listeningLanguage(): String? {
        val s = p._uiState.value
        return s.audioTracks.getOrNull(s.selectedAudioTrackIndex)?.language?.takeIf { it.isNotBlank() }
    }

    override fun hasPlayableAudio(): Boolean = p.echoSwitch.hasPlayableAudio()

    /** The language to keep on the next source (Kicia picks the preferred language on every source anyway). */
    override fun keepLanguageOnNextSource(language: String?) { keptLanguage = language }
    override fun keepContentOnReset(on: Boolean) { p.exoPlayerView?.setKeepContentOnPlayerReset(on) }

    override fun applySelectedStream(pick: Stream, playback: StreamPlayback) = p.applySelectedStreamState(pick, playback.url, playback.headers)
    override fun persistForReuse(pick: Stream) {
        val url = pick.getStreamUrl() ?: return
        p.persistSelectedStreamForReuse(pick, url, pick.behaviorHints?.proxyHeaders?.request.orEmpty())
    }

    override fun notice(title: String, text: String) = p.echoSwitch.notice(title, text)

    override fun usualSwitch(pick: Stream, positionMs: Long?, silent: Boolean) {
        p.forgeImmediateSwitch(listOf(pick), cause = null, timelineChanged = false, label = null)
    }

    override fun backToOriginal(original: Stream?, positionMs: Long?) {
        val o = original ?: return
        p.forgeSelectedStream = o
        p.switchToSourceStream(o, resumeSavedProgress = false, startPositionMs = positionMs)
    }

    override fun preloadLimitBytes(): Long =
        if (SmartPolicyHolder.current.lowRam || Build.SUPPORTED_64_BIT_ABIS.isEmpty()) 8L * 1024 * 1024 else 24L * 1024 * 1024

    // ---- HotSwapHost ----
    override fun exoPlayer(): ExoPlayer? = p._exoPlayer
    override fun trackSelector(): TrackSelector? = p.trackSelector
    override fun allocator(): Allocator? = p._loadControl?.allocator
    override fun bandwidthMeter(): BandwidthMeter = DefaultBandwidthMeter.getSingletonInstance(p.context)

    /** Echo's beginHotSwapLoad source: the playing source's addon subtitles and audio delay carry over. */
    override fun buildMediaSource(playback: StreamPlayback): MediaSource {
        val attached = p._uiState.value.addonSubtitles.filter { p.addonSubtitleKey(it) in p.attachedAddonSubtitleKeys }
        return p.mediaSourceFactory.createMediaSource(
            context = p.context,
            url = playback.url,
            headers = playback.headers,
            subtitleConfigurations = attached.distinctBy { "${it.id}|${it.url}" }.map { p.toSubtitleConfiguration(it) },
            subtitleRoutes = p.subtitleRoutes(attached),
            filename = playback.filename,
            responseHeaders = playback.responseHeaders.orEmpty(),
            mimeTypeOverride = playback.mimeType,
            audioDelayUsProvider = p.audioDelayUs::get,
            mediaMetadata = p.buildMediaSessionMetadata(),
            cacheKey = playback.cacheKey,
        )
    }

    override fun unlockStartupPrefetch() = p.mediaSourceFactory.unlockStartupPrefetch()

    override fun streamPlayback(pick: Stream): StreamPlayback? {
        val url = pick.getStreamUrl()?.takeIf { it.isNotBlank() } ?: return null
        val request = PlayerMediaSourceFactory.normalizePlaybackRequest(url, PlayerMediaSourceFactory.sanitizeHeaders(pick.behaviorHints?.proxyHeaders?.request))
        return StreamPlayback(request.url, request.headers, filename = pick.behaviorHints?.filename,
            responseHeaders = pick.behaviorHints?.proxyHeaders?.response)
    }

    override fun origin(): Stream? = p.echoSwitch.listedCurrentSource() ?: p.echoSwitch.currentPlaybackAsStream()
    override fun durationMs(): Long = p._playbackTimeline.value.duration
    override fun autoQualityActive(): Boolean = p.forgeAutoQualityApplies()
    override fun sourceSwitching(): Boolean = p.echoSwitch.isSourceSwitching()
    override fun hasError(): Boolean = p._uiState.value.error != null
    override fun clearError() { p._uiState.update { it.copy(error = null) } }

    override fun afterSwap(pick: Stream, playback: StreamPlayback, isDowngrade: Boolean) {
        p.applySelectedStreamState(pick, playback.url, playback.headers)
        p.persistSelectedStreamForReuse(pick, p.currentStreamUrl, p.currentHeaders)
        p.resetErrorRetryState()
        p.smartTryCurrent(pick)
        p._uiState.update {
            it.copy(error = null, currentStreamName = pick.name ?: pick.addonName, currentStreamAddonName = pick.addonName)
        }
    }

    override fun announce(text: String, ms: Long) {
        ForgeAutoQuality.pending.value = text
        com.nuvio.tv.skin.kicia.components.ForgeToast.show(text, top = true)
    }

    override fun cancelAnnounced(byViewer: Boolean) { forgeCancelAnnouncedChange(byViewer) }

    override fun blockLevel(level: StreamQuality) {
        ForgeAutoQuality.brain?.onLevelFailed(level, AutoQualitySession.nowMs())
    }

    override fun markHotSwapFailed(key: String) { SmartSilentKeys.add(key) }
    override fun markSilent(key: String) { SmartSilentKeys.add(key) }

    override fun backThroughSession(original: Stream, resumeAtMs: Long, durationMs: Long, label: String) {
        val session = SourceSwitchSession(label = label, isAudio = false, original = original, resumeAtMs = resumeAtMs,
            durationMs = durationMs, next = { null }, silent = true)
        session.returning = true
        p.echoSwitch.beginSourceSwitch(session, original)
    }

    override fun context(): android.content.Context = p.context
    override fun httpClient(): okhttp3.OkHttpClient = PlayerPlaybackNetworking.playbackHttpClient
    override fun userAgent(): String = PlayerMediaSourceFactory.DEFAULT_USER_AGENT

    override suspend fun dolbyVisionConvertedPerFile(): Boolean =
        p.playerSettingsDataStore.playerSettings.first().dv7HandlingMode == Dv7HandlingMode.DV81_LIBDOVI
    override suspend fun frameRateMatchingOff(): Boolean =
        p.playerSettingsDataStore.playerSettings.first().frameRateMatchingMode == FrameRateMatchingMode.OFF
    override suspend fun resolutionMatchingOn(): Boolean = p.playerSettingsDataStore.playerSettings.first().resolutionMatchingEnabled
    override fun playingFrameRate(): Float = p._uiState.value.detectedFrameRate

    override suspend fun newFrameRate(playback: StreamPlayback): Float? {
        FrameRateUtils.getCachedFrameRate(playback.url, playback.headers, playback.filename)?.let { return it.snapped }
        return withContext(Dispatchers.IO) {
            FrameRateUtils.detectFrameRateWithOkHttpProbe(
                context = p.context, sourceUrl = playback.url, headers = FrameRateUtils.streamHeadersForAfrProbe(playback.headers),
                mimeType = playback.mimeType, filename = playback.filename,
            )?.also { FrameRateUtils.cacheFrameRate(playback.url, playback.headers, it, playback.filename) }?.snapped
        }
    }

    override fun keyframes(): Keyframes = ForgeHotSwapKeyframes // forge (owner 13.11): 4A swaps on a keyframe

    companion object {
        /** The language kept for the next source (read by the next step that wires language carry-over). */
        @Volatile var keptLanguage: String? = null
    }
}
