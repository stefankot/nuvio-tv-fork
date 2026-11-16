package com.nuvio.tv.ui.screens.player

import android.view.KeyEvent
import com.nuvio.tv.data.local.RemoteButtonBinding
import com.nuvio.tv.data.local.RemoteButtonJob
import com.nuvio.tv.data.local.RemoteButtonMap

/**
 * Arc adapter: runs the remote-button jobs (press / hold) in the player, including the Live TV player
 * (PlayerScreen's key hook sits above both). Jobs the fork's player has no event for do nothing.
 */
class RemotePlayerHook(longPressMs: Long) {
    private val hold = RemotePressTracker(longPressMs)
    private var claimedKey = KeyEvent.KEYCODE_UNKNOWN

    fun onKey(
        e: KeyEvent,
        map: RemoteButtonMap,
        controlsHidden: Boolean,
        ready: Boolean,
        send: (PlayerEvent) -> Unit,
    ): Boolean {
        val firstDown = e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0
        if (claimedKey == e.keyCode && !firstDown) {
            if (e.action == KeyEvent.ACTION_UP) claimedKey = KeyEvent.KEYCODE_UNKNOWN
            return true
        }
        if (firstDown) { claimedKey = KeyEvent.KEYCODE_UNKNOWN; hold.forget(e.keyCode) }
        else if (hold.isTracking(e.keyCode)) {
            val jobs = hold.jobs
            when (hold.onKey(e)) {
                RemotePressTracker.Outcome.HOLD -> jobs?.hold?.let { if (run(it, e, send)) { claimedKey = e.keyCode; hold.reset() } }
                RemotePressTracker.Outcome.PRESS -> jobs?.press?.let { run(it, e, send) }
                else -> Unit
            }
            return true
        }
        if (ready && firstDown) {
            map.takeOver(e.keyCode, controlsHidden = controlsHidden)?.let { jobs ->
                if (jobs.hold != null) { hold.start(e.keyCode, e.eventTime, jobs); return true }
                jobs.press?.let { if (run(it, e, send)) { claimedKey = e.keyCode; return true } }
            }
        }
        return false
    }

    private fun run(b: RemoteButtonBinding, e: KeyEvent, send: (PlayerEvent) -> Unit): Boolean {
        when (b.job) {
            RemoteButtonJob.NOTHING, RemoteButtonJob.LEAVE -> Unit
            RemoteButtonJob.PLAY_PAUSE -> send(PlayerEvent.OnPlayPause)
            RemoteButtonJob.SEEK_FORWARD -> send(PlayerEvent.OnSeekBy(10_000L))
            RemoteButtonJob.SEEK_BACK -> send(PlayerEvent.OnSeekBy(-10_000L))
            RemoteButtonJob.SKIP_FORWARD -> send(PlayerEvent.OnSeekBy(b.skipMs))
            RemoteButtonJob.SKIP_BACK -> send(PlayerEvent.OnSeekBy(-b.skipMs))
            RemoteButtonJob.RESTART -> send(PlayerEvent.OnSeekTo(0L))
            RemoteButtonJob.SOURCES -> send(PlayerEvent.OnShowSourcesPanel)
            RemoteButtonJob.SPEED -> send(PlayerEvent.OnShowSpeedDialog)
            RemoteButtonJob.EPISODES -> send(PlayerEvent.OnShowEpisodesPanel)
            RemoteButtonJob.CHAPTERS_LIST -> send(PlayerEvent.OnShowChaptersPanel)
            RemoteButtonJob.AUDIO -> send(PlayerEvent.OnShowAudioOverlay)
            RemoteButtonJob.SUBTITLES -> send(PlayerEvent.OnShowSubtitleOverlay)
            RemoteButtonJob.ASPECT -> send(PlayerEvent.OnToggleAspectRatio)
            RemoteButtonJob.NEXT_EPISODE -> send(PlayerEvent.OnPlayNextEpisode)
            RemoteButtonJob.SKIP_INTRO_OR_NEXT -> send(PlayerEvent.OnSkipIntro)
            RemoteButtonJob.INFO -> send(PlayerEvent.OnShowStreamInfo)
            RemoteButtonJob.STATS -> send(PlayerEvent.OnTogglePlayerStatsHud)
            RemoteButtonJob.WATCH_PARTY -> send(PlayerEvent.OnShowPartyPanel)
            else -> return false
        }
        return true
    }
}
