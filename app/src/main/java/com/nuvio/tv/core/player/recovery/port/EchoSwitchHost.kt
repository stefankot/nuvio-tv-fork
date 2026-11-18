package com.nuvio.tv.core.player.recovery.port

import com.nuvio.tv.core.player.recovery.echo.EchoSwitchSlots
import com.nuvio.tv.core.player.recovery.echo.SourceSwitchUi
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.CoroutineScope

/**
 * forge M4b (K6): the door between the copied Echo source-switch session (`echo/SourceSwitchSession.kt`) and Kicia's
 * player. Every controller field the Echo file used is a member here; [com.nuvio.tv.ui.screens.player.PlayerEchoSwitchHost]
 * implements it on PlayerRuntimeController. The copy never imports the player package.
 */
interface EchoSwitchHost {
    val scope: CoroutineScope
    /** The session and its jobs (Echo kept them as controller fields). */
    val switchSlots: EchoSwitchSlots

    // what is playing
    fun isPlaying(): Boolean
    fun setPlaybackPaused(paused: Boolean)
    fun currentStreamUrl(): String
    fun currentPlaybackPositionMs(): Long?
    fun hasRenderedFirstFrame(): Boolean
    /** The timeline's duration (0 when unknown). */
    fun durationMs(): Long
    /** The playing source as listed in the sources panel, or null. */
    fun listedCurrentSource(): Stream?
    /** The playing link as a source to switch back to; null for a torrent (its local link dies with it). */
    fun currentPlaybackAsStream(): Stream?

    // the controls while a pick loads
    fun showSwitchUi(ui: SourceSwitchUi?, openControls: Boolean)
    fun cancelControlsTimer()
    fun scheduleHideControls()

    // one attempt
    /** Plays [stream] from [startPositionMs] (Echo: switchKeepingPosition). */
    fun trySource(stream: Stream, startPositionMs: Long?)
    /** Echo's pending audio-language switch and its check; Kicia has none (no-op / false). */
    fun clearPendingAudioSwitch()
    fun audioCheckPending(): Boolean

    // the engine
    fun isUsingMpvEngine(): Boolean
    /** ExoPlayer.isLoading; null without ExoPlayer. */
    fun exoLoading(): Boolean?
    /** ExoPlayer bufferedPosition to playbackState; null without ExoPlayer. */
    fun exoLoadMark(): Pair<Long, Int>?
    /** ExoPlayer is STATE_READY; null without ExoPlayer. */
    fun exoReady(): Boolean?
    fun isBuffering(): Boolean
    fun pendingSeekPosition(): Long?
    fun clearPendingSeek()
    fun hasError(): Boolean
    fun clearError()
    /** What's playing has an audio track the device plays (one is listed and selected). */
    fun hasPlayableAudio(): Boolean
    fun markSilent(key: String)

    // settings and messages
    /** One attempt's time before it counts as failed (Echo: BetaPerformanceSettings.sourceSwitchTimeoutSeconds). */
    fun switchTimeoutMs(): Long
    fun notice(title: String, text: String)
}
