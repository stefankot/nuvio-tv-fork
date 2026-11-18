package com.nuvio.tv.ui.screens.player

import android.util.Log
import android.widget.Toast
import com.nuvio.tv.R
import com.nuvio.tv.core.player.recovery.AutoSourceRecoveryPlan
import com.nuvio.tv.core.player.recovery.BadReason
import com.nuvio.tv.core.player.recovery.SmartBadSources
import com.nuvio.tv.core.player.recovery.SmartRecoveryFlags
import com.nuvio.tv.core.player.recovery.echo.SourceSwitchSession
import com.nuvio.tv.core.player.recovery.echo.beginSourceSwitch
import com.nuvio.tv.core.player.recovery.echo.cancelSourceSwitch
import com.nuvio.tv.core.player.recovery.echo.sourceSwitchOrigin
import com.nuvio.tv.core.player.recovery.echo.switchKey
import com.nuvio.tv.core.player.recovery.badSourceKey
import com.nuvio.tv.core.player.recovery.fileIdentityKey
import com.nuvio.tv.core.player.recovery.fileNameKey
import com.nuvio.tv.core.player.recovery.recoverySourceKey
import com.nuvio.tv.data.local.StreamAutoPlayMode
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * forge M3b (N3, last point): "Spróbuj innego źródła" in the player's "Więcej" sheet. The automatic mode's ranking
 * gives the next source, started from the playing position. It is the viewer's choice: it never uses up the
 * startup's attempts, and the skipped source is put aside for 30 min ([BadReason.USER_SKIP]).
 */
internal class SmartTrySession(val plan: AutoSourceRecoveryPlan, var current: Stream?) {
    /** Sources and files left this session ([recoverySourceKey] and [fileIdentityKey]): never offered again. */
    val skipped = mutableSetOf<String>()
    /** The viewer picked a source in the sources panel: the automatic mode no longer speaks for it. */
    var pinned = false
}

/** Keys that keep a skipped source (and the same file from another addon) out of the next pick. */
internal fun skipKeys(stream: Stream): Set<String> = buildSet {
    add(recoverySourceKey(stream))
    stream.getStreamUrl()?.let { add("url:$it") }
    fileIdentityKey(stream)?.let { add(it) }
    // forge (check 13.11): the same file name with no hash or size from another addon is the same film — this title only
    fileNameKey(stream)?.let { add(it) }
}

/**
 * What the viewer is leaving: the source the automatic mode knows it started ([sessionCurrent]) and the one the player
 * actually switched to ([selected], null until a switch). Both are skipped, so the next pick is never the same file.
 */
internal fun skipKeysForPlaying(sessionCurrent: Stream?, selected: Stream?): Set<String> =
    listOfNotNull(sessionCurrent, selected).flatMapTo(mutableSetOf()) { skipKeys(it) }

/** The plan the automatic mode started this title with (null in other modes); kept for the player's menu. */
internal fun PlayerRuntimeController.rememberSmartTryPlan(plan: AutoSourceRecoveryPlan) {
    smartTrySession = if (plan.mode == StreamAutoPlayMode.SMART) SmartTrySession(plan, plan.initialSource) else null
    if (smartTrySession != null) ForgeAutoQualityDebug.attach(this) // forge M4b: adb entry for route 4B (debug builds)
    if (smartTrySession != null) startForgeAutoQualityMonitor() else stopForgeAutoQualityMonitor() // forge M4c: the monitor
    if (smartTrySession != null) startForgeBufferTuning() else stopForgeBufferTuning() // forge M4c F (N11)
    refreshTryAnotherSource()
}

/** The startup recovery moved on to [stream]: the menu's "current" follows it. */
internal fun PlayerRuntimeController.smartTryCurrent(stream: Stream) {
    smartTrySession?.current = stream
}

/** A source picked by hand in the panel: the row disappears for the rest of this title. */
internal fun PlayerRuntimeController.smartTryPinned() {
    echoSwitch.cancelSourceSwitch() // forge M4b (K6): the viewer's own pick ends a running switch
    smartTrySession?.pinned = true
    refreshTryAnotherSource()
}

internal fun PlayerRuntimeController.refreshTryAnotherSource() {
    val session = smartTrySession
    val available = session != null && !session.pinned && session.plan.videoId == currentVideoId &&
        contentType !in listOf("tv", "channel", "cloud")
    if (_uiState.value.canTryAnotherSource != available) _uiState.update { it.copy(canTryAnotherSource = available) }
}

internal fun PlayerRuntimeController.tryAnotherSource() {
    val session = smartTrySession
    // forge (check 13.11): every press leaves a line; a press that cannot act says why and hides the button
    if (session == null || session.pinned || session.plan.videoId != currentVideoId) {
        Log.i("SmartRecovery", "reason=USER_SKIP ignored session=${session != null} pinned=${session?.pinned} " +
            "video=${session?.plan?.videoId == currentVideoId}")
        refreshTryAnotherSource()
        return
    }
    // The viewer's choice: the startup recovery (if still running) stops, and nothing here counts against it.
    autoSourceRecovery?.cancel()
    val position = currentPlaybackPositionMs()
    val selected = forgeSelectedStream
    val playing = session.current ?: selected
    playing?.let { SmartBadSources.mark(it, BadReason.USER_SKIP) }
    session.skipped += skipKeysForPlaying(session.current, selected)
    Log.i("SmartRecovery", "reason=USER_SKIP key=${playing?.let { badSourceKey(it).take(8) } ?: "-"} " +
        "skipped=${session.skipped.size} pos=${position ?: -1}")
    scope.launch {
        val next = session.plan.next(session.skipped)
        if (smartTrySession !== session) return@launch
        if (next == null) {
            Log.i("SmartRecovery", "reason=USER_SKIP next=brak")
            // makieta 7.7 B3: the current source stays (no panel, no flicker); a Komunikat at the top says why
            com.nuvio.tv.skin.kicia.components.ForgeToast.show("Nie ma więcej źródeł", top = true,
                note = "Wszystkie źródła tego tytułu są oznaczone jako złe. Przytrzymaj „Źródło i jakość”, żeby wybrać z listy.")
            return@launch
        }
        Log.i("SmartRecovery", "reason=USER_SKIP next=tak key=${badSourceKey(next).take(8)}")
        if (SmartRecoveryFlags.ECHO_SWITCH_SESSION_ENABLED) { // forge M4b (K6): Echo's session — up to 6 sources, then back
            beginEchoTrySwitch(session, next)
            return@launch
        }
        session.current = next
        switchToSourceStream(next, startPositionMs = position)
    }
}

/**
 * forge M4b (K6): „Inne źródło” on the copied Echo session. The ranking gives every next pick (skipping what was tried);
 * a pick without sound or that does not start in time makes way for the next; none left = back to what was playing.
 */
private suspend fun PlayerRuntimeController.beginEchoTrySwitch(session: SmartTrySession, first: com.nuvio.tv.domain.model.Stream) {
    val host = echoSwitch
    val (original, resumeAt, duration) = host.sourceSwitchOrigin()
    // Echo's `next` is not suspending: the ranking's next picks (up to Echo's 6 attempts) are taken now.
    val ladder = mutableListOf(first)
    val skip = (session.skipped + skipKeys(first)).toMutableSet()
    while (ladder.size < 6) { val n = session.plan.next(skip) ?: break; ladder += n; skip += skipKeys(n) }
    val echo = SourceSwitchSession(
        label = "Inne źródło",
        isAudio = false,
        original = original,
        resumeAtMs = resumeAt,
        durationMs = duration,
        next = { tried -> ladder.firstOrNull { it.switchKey() !in tried && it.switchKey() !in SmartSilentKeys.all() } },
        onAttempt = { pick ->
            session.current = pick
            session.skipped += skipKeys(pick)
            smartTryCurrent(pick)
        },
        requireAudio = true,
    )
    host.beginSourceSwitch(echo, first)
}
