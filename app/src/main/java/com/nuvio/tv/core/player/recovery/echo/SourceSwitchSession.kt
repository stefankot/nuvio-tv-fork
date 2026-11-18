package com.nuvio.tv.core.player.recovery.echo // KICIA: package (Echo: com.nuvio.tv.ui.screens.player)

// Copied from Echo e952419 app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerSourceSwitch.kt
// (forge M4b, K6). Every change is marked KICIA; see docs/agent/auto-odtwarzanie/ECHO-PORT.md.

import android.os.SystemClock
import android.util.Log
import androidx.media3.exoplayer.SeekParameters
import com.nuvio.tv.core.player.recovery.port.EchoSwitchHost // KICIA: the host instead of PlayerRuntimeController
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield

// KICIA: Echo's R / BetaFeatures / NewLookNotifications / ProxyHeaders / StreamBehaviorHints / Player / update imports
// are gone: the host gives the timeout, the notice and the source built from the playing link.

private const val TAG = "PlayerRuntimeController" // KICIA: Echo used PlayerRuntimeController.TAG

/** KICIA: Echo kept these as PlayerRuntimeController fields (sourceSwitch, sourceSwitchJob, …Guard…, …Watch…). */
class EchoSwitchSlots {
    internal var session: SourceSwitchSession? = null
    internal var job: Job? = null
    internal var guardJob: Job? = null
    internal var watchJob: Job? = null
}

/**
 * What the controls show while a picked quality or audio loads: the spot it left, held still. [coverVideo]
 * once the old player is gone: until then its paused picture is still on screen.
 */
data class SourceSwitchUi(val positionMs: Long, val durationMs: Long, val coverVideo: Boolean = false)

/**
 * A quality or audio picked in the player (or Auto's change), from the click until the new source
 * plays. The player returns to its controls and runs a streak along the progress bar; the picture
 * holds the frame it left (taken from the stream by the seek-thumbnail decoder, never read back from
 * the screen) or, without one, the title's dimmed backdrop. The old source is released before the new
 * one loads, so only one player decoder is ever in use. A source that fails or doesn't start within the
 * timeout ([BetaPerformanceSettings.sourceSwitchTimeoutSeconds]) makes way for the next with the same
 * quality or audio ([next]); once none is left, playback goes back to [original] with a "Source failed
 * to play" notification (none when [silent]).
 */
internal class SourceSwitchSession(
    /** For the notification when nothing works: "1080p", "French". */
    val label: String,
    val isAudio: Boolean,
    val original: Stream?,
    val resumeAtMs: Long?,
    val durationMs: Long,
    /** The best source not in the keys given (see [switchKey]), or null. */
    val next: (Set<String>) -> Stream?,
    /** Runs before each pick plays (not on the way back to [original]). */
    val onAttempt: (Stream) -> Unit = {},
    /** Auto's own change: no notification when nothing plays. */
    val silent: Boolean = false,
    /** Every pick failed and playback is going back to [original]. */
    val onGiveUp: () -> Unit = {},
    /** A pick (not the way back) is playing. */
    val onPlaying: () -> Unit = {},
    /** A pick that starts without sound (no audio track, or none the device can play) counts as failed. */
    val requireAudio: Boolean = false,
    /**
     * Race matching sources: the sources to race for the first pick (the menu's own choice, see
     * AutoPlayRace.switchField); the first to send video from the resume spot plays. Empty: [next] decides alone.
     */
    val raceField: List<Stream> = emptyList()
) {
    val tried = mutableSetOf<String>()
    /** Sources that failed the race: later picks pass them over while others are left. */
    var raceLost: Set<String> = emptySet()
    var attempts = 0
    var returning = false
    /** Playing (not paused by the viewer) when the switch began: Back before the first attempt resumes it. */
    var wasPlaying = false
    /** What was playing when the current attempt started; the attempt counts once that changes. */
    var attemptFromUrl: String? = null
    /** The pick being tried ([switchKey]). */
    var attemptKey: String? = null
}

/** Picks tried for one switch before giving up. */
private const val MAX_SWITCH_ATTEMPTS = 6

/** A pick ready this long with no sound to play is taken as silent (the track list can lag readiness a little). */
private const val SILENT_GRACE_MS = 2_000L

/** How long the held frame may take to come from the thumbnail decoder before the backdrop is used. */
private const val FRAME_GRAB_TIMEOUT_MS = 1_200L

/** A pick given extra time (see watchSourceSwitch) fails once no data has come in for this long. */
private const val SWITCH_LOAD_STALL_MS = 3_000L
/** However the attempts go, the loading screen never outlasts this (beyond the attempts' own timeouts). */
private const val SWITCH_GUARD_SLACK_MS = 20_000L

/** The landing spot may be up to 20 s before the old position and 5 s after it (the nearest keyframe there). */
private const val SWITCH_EARLY_MARGIN_MS = 20_000L
private const val SWITCH_LATE_MARGIN_MS = 5_000L
internal val SOURCE_SWITCH_SEEK_PARAMETERS = SeekParameters(SWITCH_EARLY_MARGIN_MS * 1_000L, SWITCH_LATE_MARGIN_MS * 1_000L)

/** Identifies a source among the tried ones: its link, or for a debrid file not yet resolved, its listing. */
internal fun Stream.switchKey(): String =
    getStreamUrl()?.takeIf { it.isNotBlank() } ?: listOf(addonName, name, title, infoHash).joinToString("|")

internal fun EchoSwitchHost.isSourceSwitching(): Boolean = switchSlots.session != null // KICIA: receiver, slots

/** Where playback is now, the way back if every pick fails (an earlier pick still loading keeps its own). */
internal fun EchoSwitchHost.sourceSwitchOrigin(): Triple<Stream?, Long?, Long> { // KICIA: receiver
    switchSlots.session?.let { return Triple(it.original, it.resumeAtMs, it.durationMs) } // KICIA: slots
    // The listed source if it's there; otherwise the playing link itself, so a failed switch can always
    // go back (the old player is gone by then).
    val original = listedCurrentSource() ?: currentPlaybackAsStream() // KICIA: host builds both
    val position = currentPlaybackPositionMs()?.takeIf { hasRenderedFirstFrame() && it > 0L } // KICIA: host
    val duration = durationMs().takeIf { hasRenderedFirstFrame() && it > 0L } ?: 0L // KICIA: host
    return Triple(original, position, duration)
}

// KICIA: currentPlaybackAsStream() moved to the host (it reads a dozen controller fields).

/** Starts [session] with [first]: back to the controls, the frame held, the streak running. */
internal fun EchoSwitchHost.beginSourceSwitch(session: SourceSwitchSession, first: Stream) { // KICIA: receiver
    switchSlots.job?.cancel() // KICIA: slots
    val previous = switchSlots.session // KICIA: slots
    previous?.let { session.tried += it.tried }
    session.wasPlaying = previous?.wasPlaying ?: isPlaying() // KICIA: host
    // The old source stops where it is while the new one loads.
    if (previous == null) setPlaybackPaused(true)
    switchSlots.session = session // KICIA: slots
    currentStreamUrl().takeIf { it.isNotBlank() }?.let { session.tried += it } // KICIA: host
    session.original?.let { session.tried += it.switchKey() }
    cancelControlsTimer() // KICIA: hideControlsJob?.cancel()
    // KICIA: Echo's uiState copy (sourceSwitch, showControls, overlays off) → host
    showSwitchUi(SourceSwitchUi(session.resumeAtMs ?: 0L, session.durationMs), openControls = true)
    switchSlots.job = scope.launch { // KICIA: slots
        yield()
        // The race runs while the frame is taken, so it adds little to the wait.
        val field = session.raceField.filter { it.switchKey() !in session.tried }
        val race = if (field.size > 1) {
            val fraction = session.resumeAtMs?.takeIf { session.durationMs > 0L }?.let { it.toDouble() / session.durationMs }
            async { AutoPlayRace.pickForSwitch(field, fraction) }
        } else {
            null
        }
        // KICIA: no held frame (Kicia has no seek-thumbnail decoder here); the title's backdrop stays. Echo grabbed
        // it within FRAME_GRAB_TIMEOUT_MS; the constant stays for the guard's arithmetic.
        val pick = race?.await()?.let { result ->
            session.raceLost = result.failed.mapTo(mutableSetOf()) { it.switchKey() }
            result.winner
        } ?: first
        if (switchSlots.session === session) attemptSourceSwitch(session, pick) // KICIA: slots
    }
    guardSourceSwitch(session)
}

/**
 * The last line of defence: whatever happens to the attempts, the loading screen ends and the player
 * is left to its own loading and error handling, so a switch can never hold the player hostage.
 */
private fun EchoSwitchHost.guardSourceSwitch(session: SourceSwitchSession) { // KICIA: receiver
    switchSlots.guardJob?.cancel() // KICIA: slots
    val perAttemptMs = switchTimeoutMs() // KICIA: host (Echo: BetaFeatures.settings(context))
    switchSlots.guardJob = scope.launch { // KICIA: slots
        // Each attempt may take twice its timeout while data keeps arriving (see watchSourceSwitch).
        delay(perAttemptMs * 2 * (MAX_SWITCH_ATTEMPTS + 1) + FRAME_GRAB_TIMEOUT_MS + SWITCH_GUARD_SLACK_MS)
        if (switchSlots.session === session) { // KICIA: slots
            Log.w(TAG, "SOURCE_SWITCH: guard ended a switch that ran too long")
            switchSlots.job?.cancel() // KICIA: slots
            endSourceSwitch(resumeControlsTimer = false)
        }
    }
}

private fun EchoSwitchHost.attemptSourceSwitch(session: SourceSwitchSession, target: Stream) { // KICIA: receiver
    session.attempts++
    session.tried += target.switchKey()
    session.attemptFromUrl = currentStreamUrl() // KICIA: host
    session.attemptKey = target.switchKey()
    if (session.returning) {
        // The original plays as it was, not on the language that was being tried.
        clearPendingAudioSwitch() // KICIA: host (Echo: pendingAudioLanguageSwitch = null; audioSwitchCheck = null)
    } else {
        session.onAttempt(target)
    }
    Log.i(
        TAG,
        "SOURCE_SWITCH: ${if (session.returning) "back to" else "trying"} addon=${target.addonName} " +
            "attempt=${session.attempts} label=${session.label}"
    )
    try {
        trySource(target, session.resumeAtMs) // KICIA: host (Echo: switchKeepingPosition(target, resumeAtMs, startIntro = false))
    } catch (e: Exception) {
        // Never leave the loading screen up over a player that didn't start: hand back to the usual flow.
        Log.e(TAG, "SOURCE_SWITCH: switch threw; ending it", e)
        endSourceSwitch(resumeControlsTimer = false)
        return
    }
    // The controls opened when the switch began; whether they stay up from here is the viewer's call.
    showSwitchUi(SourceSwitchUi(session.resumeAtMs ?: 0L, session.durationMs, coverVideo = true), openControls = false) // KICIA: host
    watchSourceSwitch(session)
}

/**
 * Waits for the attempt to play; past the timeout it counts as failed. One still downloading then (a big file opened
 * mid-film on a slow server) gets as long again, for as long as data keeps arriving.
 */
private fun EchoSwitchHost.watchSourceSwitch(session: SourceSwitchSession) { // KICIA: receiver
    switchSlots.watchJob?.cancel() // KICIA: slots
    switchSlots.watchJob = scope.launch { // KICIA: slots
        val timeoutMs = switchTimeoutMs() // KICIA: host
        var silentSince: Long? = null
        // True: playing. False: playing without sound. Null: not yet.
        fun check(): Boolean? {
            if (sourceSwitchAttemptPlaying(session)) {
                if (!session.requireAudio || session.returning || hasPlayableAudio()) return true
                val now = SystemClock.elapsedRealtime()
                val since = silentSince ?: now.also { silentSince = it }
                if (now - since >= SILENT_GRACE_MS) return false
            } else {
                silentSince = null
            }
            return null
        }
        var outcome: Boolean? = withTimeoutOrNull(timeoutMs) {
            var result: Boolean? = null
            while (result == null) {
                result = check()
                if (result == null) delay(100)
            }
            result
        }
        if (outcome == null && !session.returning && switchSlots.session === session && switchPickLoading(session)) { // KICIA: slots
            Log.i(TAG, "SOURCE_SWITCH: still loading after ${timeoutMs / 1000}s; waiting while data arrives")
            outcome = withTimeoutOrNull(timeoutMs) {
                var mark = exoLoadMark() // KICIA: host (Echo: switchPickLoadMark())
                var movedAt = SystemClock.elapsedRealtime()
                var result: Boolean? = null
                while (result == null) {
                    result = check()
                    if (result != null) break
                    val now = SystemClock.elapsedRealtime()
                    val current = exoLoadMark() // KICIA: host
                    if (current != mark) {
                        mark = current
                        movedAt = now
                    } else if (now - movedAt >= SWITCH_LOAD_STALL_MS) {
                        break
                    }
                    delay(100)
                }
                result
            }
        }
        if (switchSlots.session !== session) return@launch // KICIA: slots
        if (outcome == false) {
            Log.w(TAG, "SOURCE_SWITCH: no sound (attempt=${session.attempts}); next")
            session.attemptKey?.let { markSilent(it) } // KICIA: host (Echo: silentSourceKeys += it)
            moveOnFromSourceSwitch(session)
            return@launch
        }
        val started = outcome == true
        if (started) {
            Log.i(TAG, "SOURCE_SWITCH: playing (attempt=${session.attempts})")
            if (!session.returning) session.onPlaying()
            endSourceSwitch(resumeControlsTimer = true)
        } else {
            Log.w(TAG, "SOURCE_SWITCH: no playback in time")
            if (session.returning) {
                // Still loading the original: let it carry on as a normal load.
                endSourceSwitch(resumeControlsTimer = false)
            } else {
                moveOnFromSourceSwitch(session)
            }
        }
    }
}

/** The attempt's player is downloading (ExoPlayer; mpv doesn't say). */
private fun EchoSwitchHost.switchPickLoading(session: SourceSwitchSession): Boolean { // KICIA: receiver
    if (isUsingMpvEngine() || currentStreamUrl() == session.attemptFromUrl || hasError()) return false // KICIA: host
    return exoLoading() == true // KICIA: host
}

// KICIA: switchPickLoadMark() is host.exoLoadMark().

/**
 * The new source is ready at the old spot (within the margins), with any audio check passed. Ready, not
 * playing: a viewer who pauses while it loads mustn't have the source counted as failed.
 */
private fun EchoSwitchHost.sourceSwitchAttemptPlaying(session: SourceSwitchSession): Boolean { // KICIA: receiver
    if (currentStreamUrl() == session.attemptFromUrl || !hasRenderedFirstFrame()) return false // KICIA: host
    if (pendingSeekPosition() != null || hasError()) return false // KICIA: host
    val ready = if (isUsingMpvEngine()) !isBuffering() else exoReady() == true // KICIA: host
    if (!ready) return false
    if (audioCheckPending()) return false // KICIA: host
    val resumeAt = session.resumeAtMs ?: return true
    val position = currentPlaybackPositionMs() ?: return false
    return position in (resumeAt - SWITCH_EARLY_MARGIN_MS - 1_000L)..(resumeAt + SWITCH_LATE_MARGIN_MS + 1_000L)
}

// KICIA: hasPlayableAudio() is a host member (Kicia had none).

/** The next pick with the same quality or audio; once none is left, back to the original. */
private fun EchoSwitchHost.moveOnFromSourceSwitch(session: SourceSwitchSession) { // KICIA: receiver
    val next = if (session.attempts < MAX_SWITCH_ATTEMPTS) {
        session.next(session.tried + session.raceLost) ?: session.next(session.tried)
    } else {
        null
    }
    if (next != null) {
        attemptSourceSwitch(session, next)
        return
    }
    val original = session.original
    Log.w(TAG, "SOURCE_SWITCH: nothing left for ${session.label}; back=${original?.addonName}")
    session.onGiveUp()
    if (!session.silent) {
        // KICIA: NewLookNotifications.post(PLAYER, notif_source_failed_*) → host.notice with Kicia's Polish texts
        notice(
            "Źródło nie działa",
            when {
                original != null -> "Wracam do poprzedniego źródła"
                session.isAudio -> "Brak działającego źródła z dźwiękiem: ${session.label}"
                else -> "Brak działającego źródła: ${session.label}"
            }
        )
    }
    if (original == null) {
        endSourceSwitch(resumeControlsTimer = false)
        return
    }
    session.returning = true
    attemptSourceSwitch(session, original)
}

/**
 * The playing source failed while a switch was loading it. True when the switch has taken over (the
 * next pick, or the way back); false lets the usual error handling run.
 */
internal fun EchoSwitchHost.handleSourceSwitchFailure(detailedError: String): Boolean { // KICIA: receiver
    // KICIA: no hot swap yet (M4b part A): Echo's handleAutoQualityHotSwapFailure() check is not here.
    val session = switchSlots.session ?: return false // KICIA: slots
    // Before the first attempt the old (paused) player is still the one playing: its errors are its own.
    if (session.attempts == 0 || currentStreamUrl() == session.attemptFromUrl) return false // KICIA: host
    switchSlots.watchJob?.cancel() // KICIA: slots
    if (session.returning) {
        endSourceSwitch(resumeControlsTimer = false)
        return false
    }
    Log.w(TAG, "SOURCE_SWITCH: failed (${detailedError.lineSequence().firstOrNull().orEmpty()})")
    clearError() // KICIA: host (Echo: _uiState.update { it.copy(error = null) })
    switchSlots.job = scope.launch { // KICIA: slots
        // Out of the player's own error callback before the player is released.
        yield()
        if (switchSlots.session === session) moveOnFromSourceSwitch(session) // KICIA: slots
    }
    return true
}

/**
 * At a pick's first ready: one that can't seek would play from the start, not where the switch resumes, and only
 * fail once the switch timed out. True when it's passed over now and the next pick starts.
 */
internal fun EchoSwitchHost.rejectUnseekableSwitchPick(seekable: Boolean): Boolean { // KICIA: receiver
    val session = switchSlots.session ?: return false // KICIA: slots
    if (seekable || session.returning || currentStreamUrl() == session.attemptFromUrl) return false // KICIA: host
    val position = pendingSeekPosition() ?: return false // KICIA: host
    // Near the start, playing from 0:00 is within the margins anyway.
    if (position <= SWITCH_EARLY_MARGIN_MS) return false
    Log.w(TAG, "SOURCE_SWITCH: can't seek, so can't resume at ${position / 1000}s; next")
    clearPendingSeek() // KICIA: host
    return rejectSourceSwitchAttempt()
}

/** A pick that turned out to be the wrong video (see [verifyAudioSwitchDuration]): on to the next. */
internal fun EchoSwitchHost.rejectSourceSwitchAttempt(): Boolean { // KICIA: receiver
    val session = switchSlots.session ?: return false // KICIA: slots
    if (session.returning) return false
    switchSlots.watchJob?.cancel() // KICIA: slots
    switchSlots.job = scope.launch { // KICIA: slots
        yield()
        if (switchSlots.session === session) moveOnFromSourceSwitch(session) // KICIA: slots
    }
    return true
}

/**
 * Back while a pick loads: quietly back to what was playing. False when there's nothing to go back to
 * (or it's already on its way back), so Back does what it usually does.
 */
internal fun EchoSwitchHost.abortSourceSwitch(): Boolean { // KICIA: receiver
    val session = switchSlots.session ?: return false // KICIA: slots
    if (session.returning) return false
    Log.i(TAG, "SOURCE_SWITCH: cancelled with Back (attempt=${session.attempts})")
    switchSlots.watchJob?.cancel() // KICIA: slots
    switchSlots.job?.cancel() // KICIA: slots
    if (session.attempts == 0) {
        // Not started yet: the old source was only paused.
        endSourceSwitch(resumeControlsTimer = true)
        if (session.wasPlaying) setPlaybackPaused(false)
        return true
    }
    val original = session.original ?: return false
    session.returning = true
    switchSlots.job = scope.launch { // KICIA: slots
        yield()
        if (switchSlots.session === session) attemptSourceSwitch(session, original) // KICIA: slots
    }
    return true
}

/** Ends the switch: the frame and streak go, the controls hide again as usual. */
internal fun EchoSwitchHost.endSourceSwitch(resumeControlsTimer: Boolean) { // KICIA: receiver
    if (switchSlots.session == null) return // KICIA: slots; no held frame to check
    switchSlots.session = null // KICIA: slots
    switchSlots.watchJob?.cancel() // KICIA: slots
    switchSlots.watchJob = null
    switchSlots.guardJob?.cancel()
    switchSlots.guardJob = null
    showSwitchUi(null, openControls = false) // KICIA: host (Echo: _switchFrame = null; uiState sourceSwitch = null)
    if (resumeControlsTimer) scheduleHideControls()
}

/** Something else took over (another source or episode picked, the player closed). */
internal fun EchoSwitchHost.cancelSourceSwitch() { // KICIA: receiver
    if (switchSlots.session == null) return // KICIA: slots
    switchSlots.job?.cancel() // KICIA: slots
    switchSlots.job = null
    endSourceSwitch(resumeControlsTimer = false)
}

/** KICIA: plain-function entry for callers outside the receiver style (PlayerViewModel's Back). */
internal fun abortSourceSwitchFrom(host: EchoSwitchHost): Boolean = host.abortSourceSwitch()
