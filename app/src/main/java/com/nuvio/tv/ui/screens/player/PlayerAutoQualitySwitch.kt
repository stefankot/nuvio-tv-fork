package com.nuvio.tv.ui.screens.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.nuvio.tv.core.autoquality.Cause
import com.nuvio.tv.core.autoquality.Plan
import com.nuvio.tv.core.autoquality.echo.startAutoQualityHotSwap
import com.nuvio.tv.core.player.recovery.SmartRecoveryFlags
import com.nuvio.tv.core.player.recovery.echo.SourceSwitchSession
import com.nuvio.tv.core.player.recovery.echo.beginSourceSwitch
import com.nuvio.tv.core.player.recovery.echo.isSourceSwitching
import com.nuvio.tv.core.player.recovery.echo.sourceSwitchOrigin
import com.nuvio.tv.core.player.recovery.echo.switchKey
import com.nuvio.tv.core.streams.smart.SmartCandidate
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * forge M4b, route 4B (contract C): what the player does with the brain's plan. A change goes through the copied Echo
 * session (K6): position = now (10 s back when the timeline changed), the ladder gives every next pick, a pick without
 * sound or that does not start makes way for the next, none left = back to what played. Renewing the address resolves
 * the same file again and resumes without a notice. Off: live, channels, cloud, torrent without debrid, a source picked
 * by hand. Route 4A (Echo's hot swap) is not here yet: with HOT_SWAP off a PLANNED_4A down runs as 4B, an upgrade is dropped.
 */
internal fun PlayerRuntimeController.forgeExecutePlan(plan: Plan): Boolean {
    if (!forgeAutoQualityApplies()) {
        Log.i(TAG_AQ, "plan=${plan.javaClass.simpleName} ignored (route off for this playback)")
        return false
    }
    return when (com.nuvio.tv.core.autoquality.routeFor(plan)) {
        com.nuvio.tv.core.autoquality.AutoQualityRoute.NONE -> { Log.i(TAG_AQ, "plan=${plan.javaClass.simpleName}"); false }
        com.nuvio.tv.core.autoquality.AutoQualityRoute.RENEW -> { forgeRenewAddress((plan as Plan.RenewAddress).reason); true }
        com.nuvio.tv.core.autoquality.AutoQualityRoute.DROP -> { Log.i(TAG_AQ, "plan=4A up dropped (no hot swap)"); false }
        // 4A: Echo's hot swap (copy in core/autoquality/echo); when it cannot take the change, the usual 4B.
        com.nuvio.tv.core.autoquality.AutoQualityRoute.HOT_SWAP -> {
            val p = plan as Plan.Switch
            val pick = p.ladder.firstNotNullOfOrNull { it.asStream() }
            val target = p.target
            if (pick != null && target != null &&
                PlayerAutoQualityHost(this).startAutoQualityHotSwap(target, null, pick, p.isUpgrade)
            ) true
            else forgeImmediateSwitch(p.ladder.mapNotNull { it.asStream() }, p.cause, p.timelineChanged, p.target?.toString())
        }
        com.nuvio.tv.core.autoquality.AutoQualityRoute.IMMEDIATE -> {
            val p = plan as Plan.Switch
            forgeImmediateSwitch(p.ladder.mapNotNull { it.asStream() }, p.cause, p.timelineChanged, p.target?.toString())
        }
    }
}

private const val TAG_AQ = "AutoQuality"
private const val TIMELINE_CHANGED_BACK_MS = 10_000L

private fun Any.asStream(): Stream? = (this as? SmartCandidate<*>)?.item as? Stream ?: this as? Stream

internal fun PlayerRuntimeController.forgeAutoQualityApplies(): Boolean {
    if (contentType in listOf("tv", "channel", "cloud")) return false
    if (smartTrySession?.pinned == true) return false
    if (smartTrySession == null) return false // the automatic mode is not running this title
    if (_uiState.value.isTorrentStream) return false // P2P without debrid: no switching under it
    return true
}

/** Route 4B: a change now, through Echo's session. */
internal fun PlayerRuntimeController.forgeImmediateSwitch(ladder: List<Stream>, cause: Cause?, timelineChanged: Boolean, label: String?, notice: Boolean = true): Boolean {
    if (!SmartRecoveryFlags.ECHO_SWITCH_SESSION_ENABLED) {
        val first = ladder.firstOrNull() ?: return false
        val at = currentPlaybackPositionMs()?.let { if (timelineChanged) (it - TIMELINE_CHANGED_BACK_MS).coerceAtLeast(0) else it }
        forgeSelectedStream = first
        switchToSourceStream(first, resumeSavedProgress = false, startPositionMs = at)
        return true
    }
    val host = echoSwitch
    if (host.isSourceSwitching()) { Log.i(TAG_AQ, "4B skipped: a switch is running"); return false }
    val first = ladder.firstOrNull() ?: return false
    val (original, resumeAt, duration) = host.sourceSwitchOrigin()
    val at = resumeAt?.let { if (timelineChanged) (it - TIMELINE_CHANGED_BACK_MS).coerceAtLeast(0) else it }
    val session = SourceSwitchSession(
        label = label ?: "Inne źródło",
        isAudio = false,
        original = original,
        resumeAtMs = at,
        durationMs = duration,
        next = { tried -> ladder.firstOrNull { it.switchKey() !in tried && it.switchKey() !in SmartSilentKeys.all() } },
        onAttempt = { pick -> smartTryCurrent(pick) },
        requireAudio = true,
    )
    Log.i(TAG_AQ, "4B cause=${cause ?: "-"} ladder=${ladder.size} at=${at ?: -1} timelineChanged=$timelineChanged")
    // forge fix 14.11 (JMGO B1): the file a rescue leaves is put aside for the session, so a preset change right after
    // (Zbalansowany) no longer picks the same file again and the switching goes round in circles
    val left = forgeSelectedStream ?: smartTrySession?.current
    if (left != null) when (cause) {
        Cause.LINK, Cause.SERVER -> com.nuvio.tv.core.player.recovery.SmartBadSources.mark(left, com.nuvio.tv.core.player.recovery.BadReason.STALL)
        Cause.DECODER -> com.nuvio.tv.core.player.recovery.SmartBadSources.mark(left, com.nuvio.tv.core.player.recovery.BadReason.DECODER)
        else -> Unit
    }
    // The viewer's own choice (a quality button) already said „Szukam źródła: …” (mockup 05); a rescue says why.
    if (notice) host.notice("Zmieniam źródło", forgeCauseText(cause))
    host.beginSourceSwitch(session, first)
    return true
}

private fun forgeCauseText(cause: Cause?): String = when (cause) {
    Cause.SERVER -> "serwer przestał odpowiadać"
    Cause.LINK -> "łącze nie nadąża"
    Cause.DECODER -> "telewizor nie radzi sobie z tym plikiem"
    Cause.ADDRESS_EXPIRED, null -> "na lepiej działające"
}

/** The address expired: the same file, resolved again, from the same spot; no notice, not a change. */
internal fun PlayerRuntimeController.forgeRenewAddress(reason: String) {
    val stream = forgeSelectedStream ?: smartTrySession?.current ?: return
    val at = currentPlaybackPositionMs()
    Log.i(TAG_AQ, "renew reason=$reason at=${at ?: -1}")
    scope.launch {
        runCatching { directDebridResolver.invalidate(stream, currentSeason, currentEpisode) }
        val resolved = resolveDirectDebridStreamIfNeeded(stream, currentSeason, currentEpisode) ?: return@launch
        if (resolved.getStreamUrl().isNullOrBlank()) return@launch
        switchToSourceStream(resolved, resumeSavedProgress = false, startPositionMs = at)
    }
}

/**
 * Debug entry (contract „Czego tu nie robisz”: decisions for the emulator): `adb shell am broadcast -a
 * com.nuvio.tv.forge.AQ --es plan 4b|renew`. Registered once, debug builds only; acts on the newest player.
 */
internal object ForgeAutoQualityDebug {
    private var current: WeakReference<PlayerRuntimeController>? = null
    private var registered = false

    fun attach(player: PlayerRuntimeController) {
        current = WeakReference(player)
        if (registered || !(com.nuvio.tv.BuildConfig.DEBUG || player.context.packageName.endsWith(".debug"))) return // forge: the preview build (app id ending ".debug") is not debuggable
        registered = true
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val p = current?.get() ?: return
                val which = intent.getStringExtra("plan") ?: "4b"
                p.scope.launch {
                    when (which) {
                        "renew" -> p.forgeExecutePlan(Plan.RenewAddress("debug"))
                        else -> {
                            val s = p.smartTrySession ?: run { Log.i(TAG_AQ, "debug: no automatic session"); return@launch }
                            val ladder = mutableListOf<Stream>()
                            val skip = s.skipped.toMutableSet()
                            while (ladder.size < 6) { val n = s.plan.next(skip) ?: break; ladder += n; skip += skipKeys(n) }
                            p.forgeExecutePlan(Plan.Switch(Plan.Way.IMMEDIATE_4B, null, null, false, ladder, false))
                        }
                    }
                }
            }
        }
        ContextCompat.registerReceiver(player.context.applicationContext, receiver, IntentFilter("com.nuvio.tv.forge.AQ"), ContextCompat.RECEIVER_EXPORTED)
    }
}

/**
 * forge M6 (N13): a quality level picked in the player for this session: the policy follows it (not the profile's
 * setting), the source is ranked again and, when another file comes first, switched to from the same position
 * (route 4B through Echo's session).
 */
internal fun PlayerRuntimeController.forgeSessionPreset(preset: com.nuvio.tv.core.streams.smart.SmartPreset) {
    // forge N13/N15: this play only — the stored (profile) policy stays; cleared when the player closes
    com.nuvio.tv.core.streams.smart.SmartPolicyHolder.playPreset = preset
    val session = smartTrySession ?: return
    if (!forgeAutoQualityApplies()) return
    val policy = com.nuvio.tv.core.streams.smart.SmartPolicyHolder.current
    val ranked = com.nuvio.tv.core.streams.smart.SmartStreamRanking.rank(session.plan.streams, policy, session.plan.smartExpected)
    val current = forgeSelectedStream ?: session.current
    val first = ranked.firstOrNull() ?: return
    if (current != null && first.switchKey() == current.switchKey()) {
        Log.i(TAG_AQ, "session preset=$preset: the same file stays")
        return
    }
    Log.i(TAG_AQ, "session preset=$preset: another file")
    forgeImmediateSwitch(ranked.filter { current == null || it.switchKey() != current.switchKey() }.take(6), null, false, null, notice = false)
}

