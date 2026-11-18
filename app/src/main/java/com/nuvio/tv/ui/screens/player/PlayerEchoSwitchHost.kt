package com.nuvio.tv.ui.screens.player

import androidx.media3.common.Player
import com.nuvio.tv.core.player.recovery.echo.EchoSwitchSlots
import com.nuvio.tv.core.player.recovery.echo.SourceSwitchUi
import com.nuvio.tv.core.player.recovery.port.EchoSwitchHost
import com.nuvio.tv.domain.model.ProxyHeaders
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBehaviorHints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update

/**
 * forge M4b (K6): Kicia's side of the copied Echo source-switch session. Short adapters only; what Kicia lacks
 * (held frame, Echo's audio-language switch) is a no-op, as the contract says.
 */
internal class PlayerEchoSwitchHost(private val p: PlayerRuntimeController) : EchoSwitchHost {
    override val scope: CoroutineScope get() = p.scope
    override val switchSlots = EchoSwitchSlots()

    override fun isPlaying() = p._uiState.value.isPlaying
    override fun setPlaybackPaused(paused: Boolean) = p.setPlaybackPaused(paused)
    override fun currentStreamUrl(): String = p.currentStreamUrl
    override fun currentPlaybackPositionMs(): Long? = p.currentPlaybackPositionMs()
    override fun hasRenderedFirstFrame() = p.hasRenderedFirstFrame
    override fun durationMs() = p._playbackTimeline.value.duration

    /** The source the automatic mode or the viewer started (Kicia keeps it; Echo matched the panel's list). */
    override fun listedCurrentSource(): Stream? = p.forgeSelectedStream ?: p.smartTrySession?.current

    override fun currentPlaybackAsStream(): Stream? {
        val url = p.currentStreamUrl.takeIf { it.isNotBlank() } ?: return null
        if (p._uiState.value.isTorrentStream) return null
        return Stream(
            name = p._uiState.value.currentStreamName,
            title = null,
            description = p.currentStreamDescription,
            url = url,
            ytId = null,
            infoHash = null,
            fileIdx = null,
            externalUrl = null,
            behaviorHints = StreamBehaviorHints(
                notWebReady = null,
                bingeGroup = p.currentStreamBingeGroup,
                countryWhitelist = null,
                proxyHeaders = ProxyHeaders(request = p.currentHeaders.takeIf { it.isNotEmpty() }, response = null),
                videoSize = p.currentVideoSize,
                filename = p.currentFilename,
            ),
            addonName = p.currentAddonName ?: p._uiState.value.currentStreamAddonName.orEmpty(),
            addonLogo = p.currentAddonLogo,
        )
    }

    /** No streak or held frame in Kicia yet: the controls open and the player shows it is loading. */
    override fun showSwitchUi(ui: SourceSwitchUi?, openControls: Boolean) {
        if (ui == null) return
        p._uiState.update { it.copy(showControls = openControls || it.showControls, isBuffering = true) }
    }

    override fun cancelControlsTimer() { p.hideControlsJob?.cancel() }
    override fun scheduleHideControls() = p.scheduleHideControls()

    override fun trySource(stream: Stream, startPositionMs: Long?) {
        p.forgeSelectedStream = stream
        p.switchToSourceStream(stream, resumeSavedProgress = false, startPositionMs = startPositionMs)
    }

    override fun clearPendingAudioSwitch() = Unit
    override fun audioCheckPending() = false

    override fun isUsingMpvEngine() = p.isUsingMpvEngine()
    override fun exoLoading(): Boolean? = p._exoPlayer?.isLoading
    override fun exoLoadMark(): Pair<Long, Int>? = p._exoPlayer?.let { it.bufferedPosition to it.playbackState }
    override fun exoReady(): Boolean? = p._exoPlayer?.let { it.playbackState == Player.STATE_READY }
    override fun isBuffering() = p._uiState.value.isBuffering
    override fun pendingSeekPosition(): Long? = p._uiState.value.pendingSeekPosition
    override fun clearPendingSeek() { p._uiState.update { it.copy(pendingSeekPosition = null) } }
    override fun hasError() = p._uiState.value.error != null
    override fun clearError() { p._uiState.update { it.copy(error = null) } }

    override fun hasPlayableAudio(): Boolean {
        val state = p._uiState.value
        return state.audioTracks.isNotEmpty() && state.selectedAudioTrackIndex >= 0
    }

    override fun markSilent(key: String) { SmartSilentKeys.add(key) }

    /** Echo's default (BetaPerformanceSettings.sourceSwitchTimeoutSeconds = 8). */
    override fun switchTimeoutMs() = 8_000L

    override fun notice(title: String, text: String) {
        com.nuvio.tv.skin.kicia.components.ForgeToast.show("$title — $text", top = true)
    }
}

/** Sources that started without sound during a switch: later picks pass them over (Echo: silentSourceKeys). */
internal object SmartSilentKeys {
    private val keys = java.util.Collections.synchronizedSet(HashSet<String>())
    fun add(key: String) { keys += key }
    fun all(): Set<String> = keys.toSet()
}
