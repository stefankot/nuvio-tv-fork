package com.nuvio.tv.ui.screens.player

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.Player
import com.nuvio.tv.core.autoquality.AutoQualityBrain
import com.nuvio.tv.core.autoquality.AutoQualitySession
import com.nuvio.tv.core.autoquality.PlaybackFacts
import com.nuvio.tv.core.autoquality.Plan
import com.nuvio.tv.core.autoquality.Signal
import com.nuvio.tv.core.autoquality.StreamCandidateKeys
import com.nuvio.tv.core.autoquality.echo.AutoQualitySample
import com.nuvio.tv.core.player.recovery.echo.isSourceSwitching
import com.nuvio.tv.core.streams.smart.ReleaseParser
import com.nuvio.tv.core.streams.smart.SmartPolicyHolder
import com.nuvio.tv.core.streams.smart.SmartStreamRanking
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * forge M4c (contract A–D, G): the monitor of the automatic mode during the film. Every second a sample (the part of
 * Echo's PlayerRuntimeControllerAutoQuality kept: readAutoQualitySample, currentPlaybackBitrateBps, canJudge) goes to
 * the M4a brain; its plan runs through route 4B (M4b). A planned change is announced first ("Za chwilę 720p · OK
 * anuluje"); OK cancels it and the brain stays quiet 5 min. Player errors and dropped frames are signals. After the
 * first frame of a new source a [SourceChanged] event goes out for the subtitles thread (M5).
 */
private const val TAG = "AutoQuality"
private const val TICK_MS = 1_000L
/** Echo AUTO_END_GUARD_MS: no change this close to the end. */
private const val END_GUARD_MS = 3 * 60_000L
/** Echo autoQualityConfirmSeconds default: the announcement before a planned change. */
private const val ANNOUNCE_MS = 5_000L
/** Dropped frames: the window and the share (> 15 %) with a full buffer. */
private const val DROP_WINDOW_MS = 10_000L
private const val DROP_SHARE = 0.15
private const val DROP_FULL_BUFFER_MS = 20_000L

/** M5 (N5): what changed, without addresses. [way]: "4A", "4B", "renew", "back". */
data class SourceChanged(
    val oldKey: String?, val newKey: String?, val positionMs: Long, val way: String, val timelineChanged: Boolean,
)

/** forge M4c: the monitor's state for the player in front; one brain per title or episode. */
internal object ForgeAutoQuality {
    private val _changes = MutableSharedFlow<SourceChanged>(extraBufferCapacity = 8)
    /** M5: subscribed by the subtitles thread; sent after the new source's first frame. */
    val changes: SharedFlow<SourceChanged> = _changes

    /** The announced change while its countdown runs (OK cancels). */
    val pending = mutableStateOf<String?>(null)

    internal var brain: AutoQualityBrain<Stream>? = null
    internal var brainKey: String? = null
    internal var job: Job? = null
    internal var pendingJob: Job? = null
    internal var pendingChange: SourceChanged? = null
    internal var dropMark: Pair<Long, Pair<Int, Int>>? = null

    fun emitAfterFirstFrame() {
        val c = pendingChange ?: return
        pendingChange = null
        _changes.tryEmit(c)
        Log.i(TAG, "source-changed way=${c.way} old=${c.oldKey?.take(8)} new=${c.newKey?.take(8)} at=${c.positionMs}")
    }
}

/** Starts (or keeps) the monitor for the playing title; called when the automatic mode's plan is known. */
internal fun PlayerRuntimeController.startForgeAutoQualityMonitor() {
    val key = "${smartTrySession?.plan?.videoId}|$currentVideoId"
    if (ForgeAutoQuality.brainKey != key) {
        ForgeAutoQuality.brain = AutoQualityBrain(StreamCandidateKeys)
        ForgeAutoQuality.brainKey = key
    }
    ForgeAutoQuality.job?.cancel()
    ForgeAutoQuality.job = scope.launch {
        while (isActive) {
            delay(TICK_MS)
            runCatching { forgeAutoQualityTick() }.onFailure { Log.w(TAG, "tick failed: ${it.javaClass.simpleName}") }
        }
    }
}

internal fun stopForgeAutoQualityMonitor() {
    ForgeAutoQuality.job?.cancel(); ForgeAutoQuality.job = null
    forgeCancelAnnouncedChange(byViewer = false)
}

/** Echo autoQualityCanJudge, with Kicia's state (no intro state; the brain adds its own quiet times). */
private fun PlayerRuntimeController.forgeAutoQualityCanJudge(): Boolean {
    if (!forgeAutoQualityApplies()) return false
    val state = _uiState.value
    if (echoSwitch.isSourceSwitching() || !hasRenderedFirstFrame || state.error != null) return false
    if (state.showLoadingOverlay || state.playbackEnded) return false
    val timeline = _playbackTimeline.value
    if (timeline.duration > 0L && timeline.duration - timeline.currentPosition < END_GUARD_MS) return false
    return ForgeAutoQuality.pending.value == null
}

/** Echo readAutoQualitySample: ExoPlayer, or mpv's own position, wait for data and demuxer cache (owner 13.11: mpv too). */
private fun PlayerRuntimeController.forgeReadSample(now: Long): AutoQualitySample? {
    if (isUsingMpvEngine()) {
        val view = mpvView ?: return null
        return AutoQualitySample(
            nowMs = now,
            positionMs = view.currentPositionMs(),
            wantsToPlay = !userPausedManually,
            stalled = view.isPausedForCacheNow(),
            bufferedAheadMs = (view.demuxerCacheDurationSec() * 1000.0).toLong().coerceAtLeast(0L),
            throughputBps = com.nuvio.tv.core.streams.smart.SmartLinkMeter.estimateBps(now),
        )
    }
    val player = _exoPlayer ?: return null
    return AutoQualitySample(
        nowMs = now,
        positionMs = player.currentPosition,
        wantsToPlay = player.playWhenReady && !userPausedManually,
        stalled = player.playbackState == Player.STATE_BUFFERING,
        bufferedAheadMs = player.totalBufferedDuration.coerceAtLeast(0L),
        throughputBps = com.nuvio.tv.core.streams.smart.SmartLinkMeter.estimateBps(now),
    )
}

/** Echo currentPlaybackBitrateBps: the video bitrate, or the size over the length. */
private fun PlayerRuntimeController.forgeCurrentBitrateBps(): Long? =
    currentVideoBitrate?.takeIf { it > 0 }?.toLong()
        ?: com.nuvio.tv.core.streams.smart.SmartPolicy.averageBitrateBps(currentVideoSize, _playbackTimeline.value.duration / 1000)

internal fun PlayerRuntimeController.forgePlaybackFacts(): PlaybackFacts<Stream>? {
    val session = smartTrySession ?: return null
    val current = forgeSelectedStream ?: session.current ?: return null
    val cand = SmartStreamRanking.candidate(current)
    val timeline = _playbackTimeline.value
    return PlaybackFacts(
        current = cand,
        currentInfo = ReleaseParser.parse(cand.text, cand.sizeBytes),
        candidates = session.plan.streams.filter { it !== current }.map { SmartStreamRanking.candidate(it) },
        policy = SmartPolicyHolder.current,
        expected = session.plan.smartExpected,
        listeningLanguage = PlayerAutoQualityHost(this).listeningLanguage(),
        durationMs = timeline.duration.takeIf { it > 0 },
        remainingMs = (timeline.duration - timeline.currentPosition).takeIf { timeline.duration > 0 },
        startupBps = SmartPolicyHolder.current.linkBps,
        currentBitrateBps = forgeCurrentBitrateBps(),
        pinned = session.pinned,
    )
}

private fun PlayerRuntimeController.forgeAutoQualityTick() {
    if (!forgeAutoQualityCanJudge()) return
    val brain = ForgeAutoQuality.brain ?: return
    val now = AutoQualitySession.nowMs()
    val facts = forgePlaybackFacts() ?: return
    forgeDroppedFramesSignal(now)?.let { signal ->
        forgeRunPlan(brain.onSignal(signal, now, facts))
        return
    }
    val sample = forgeReadSample(now) ?: return
    forgeRunPlan(brain.onSample(sample, facts))
}

/** Dropped frames over [DROP_WINDOW_MS] with a full buffer → a signal (Exo DecoderCounters; mpv: its drop counters). */
private fun PlayerRuntimeController.forgeDroppedFramesSignal(now: Long): Signal? {
    val dropped: Int
    val rendered: Int
    val bufferedMs: Long
    if (isUsingMpvEngine()) {
        val view = mpvView ?: return null
        val (d, shown) = view.forgeFrameCounts() ?: return null
        dropped = d.toInt()
        rendered = (shown - d).coerceAtLeast(0L).toInt()
        bufferedMs = (view.demuxerCacheDurationSec() * 1000.0).toLong()
    } else {
        val player = _exoPlayer ?: return null
        val counters = player.videoDecoderCounters ?: return null
        counters.ensureUpdated()
        dropped = counters.droppedBufferCount
        rendered = counters.renderedOutputBufferCount
        bufferedMs = player.totalBufferedDuration
    }
    val mark = ForgeAutoQuality.dropMark
    if (mark == null || now - mark.first >= DROP_WINDOW_MS) {
        ForgeAutoQuality.dropMark = now to (dropped to rendered)
        if (mark == null) return null
        val d = dropped - mark.second.first
        val r = rendered - mark.second.second
        val total = d + r
        if (total <= 0 || bufferedMs < DROP_FULL_BUFFER_MS) return null
        val share = d.toDouble() / total
        if (share > DROP_SHARE) return Signal.DroppedFrames(now, share)
    }
    return null
}

/** A plan from the brain: planned changes are announced first, the rest runs now. */
private fun PlayerRuntimeController.forgeRunPlan(plan: Plan) {
    if (plan == Plan.Stay) return
    Log.i(TAG, "plan=${plan.javaClass.simpleName}")
    val switch = plan as? Plan.Switch
    if (switch == null || switch.way == Plan.Way.IMMEDIATE_4B) {
        forgeRemember(plan)
        forgeExecutePlan(plan)
        return
    }
    val label = switch.target?.toString()?.let { forgeLevelLabel(it) } ?: "inne źródło"
    val text = "Za chwilę $label" + (if (switch.isUpgrade) "" else " — wolne łącze") + " · OK anuluje"
    ForgeAutoQuality.pending.value = text
    com.nuvio.tv.skin.kicia.components.ForgeToast.show(text, top = true)
    ForgeAutoQuality.pendingJob?.cancel()
    ForgeAutoQuality.pendingJob = scope.launch {
        // Pause (contract B): a change waiting for its point runs at once when the viewer pauses.
        val until = SystemClock.elapsedRealtime() + ANNOUNCE_MS
        while (SystemClock.elapsedRealtime() < until && !userPausedManually) delay(100)
        if (ForgeAutoQuality.pending.value == null) return@launch
        ForgeAutoQuality.pending.value = null
        forgeRemember(plan)
        if (forgeExecutePlan(plan)) ForgeAutoQuality.brain?.onSwitched(AutoQualitySession.nowMs(), switch, null)
    }
}

private fun forgeLevelLabel(level: String): String = when {
    level.contains("2160") || level.contains("UHD", true) || level.contains("4K", true) -> "4K"
    level.contains("1080") -> "1080p"
    level.contains("720") -> "720p"
    level.contains("480") || level.contains("SD", true) -> "SD"
    else -> level
}

/** Remembers the change for the subtitles event, sent after the new source's first frame. */
private fun PlayerRuntimeController.forgeRemember(plan: Plan) {
    val old = (forgeSelectedStream ?: smartTrySession?.current)?.let { com.nuvio.tv.core.player.recovery.badSourceKey(it) }
    val pos = currentPlaybackPositionMs() ?: 0L
    ForgeAutoQuality.pendingChange = when (plan) {
        is Plan.RenewAddress -> SourceChanged(old, old, pos, "renew", false)
        is Plan.Switch -> SourceChanged(old, null, pos, if (plan.way == Plan.Way.PLANNED_4A) "4A" else "4B", plan.timelineChanged)
        else -> null
    }
}

/** OK during the announcement (PlayerScreen hook): the change is dropped; [byViewer] = 5 min of quiet. */
internal fun forgeCancelAnnouncedChange(byViewer: Boolean): Boolean {
    if (ForgeAutoQuality.pending.value == null) return false
    ForgeAutoQuality.pending.value = null
    ForgeAutoQuality.pendingJob?.cancel()
    ForgeAutoQuality.pendingJob = null
    if (byViewer) {
        ForgeAutoQuality.brain?.onCancelled(AutoQualitySession.nowMs())
        com.nuvio.tv.skin.kicia.components.ForgeToast.show("Zostaje bieżące źródło", top = true)
        Log.i(TAG, "announced change cancelled by the viewer")
    }
    return true
}

/** contract C: a player error during the film as a signal; true when the monitor took it over (no error window). */
internal fun PlayerRuntimeController.forgeAutoQualityOnError(httpStatus: Int?, io: Boolean, errorCode: Int): Boolean {
    if (!forgeAutoQualityApplies() || !hasRenderedFirstFrame || echoSwitch.isSourceSwitching()) return false
    val brain = ForgeAutoQuality.brain ?: return false
    val facts = forgePlaybackFacts() ?: return false
    val now = AutoQualitySession.nowMs()
    val inherent = com.nuvio.tv.core.player.recovery.isFileInherentPlaybackError(errorCode)
    val plan = brain.onSignal(Signal.PlayerError(now, httpStatus, io, inherent), now, facts)
    if (plan == Plan.Stay || plan == Plan.NoAlternative) return false
    forgeRemember(plan)
    return forgeExecutePlan(plan)
}

private var forgeSwallowOkUp = false

/** PlayerScreen hook: OK / Enter down while a change is announced cancels it; its KEY_UP is swallowed as well. */
internal fun forgeOkCancelsAnnouncedChange(event: android.view.KeyEvent): Boolean {
    val ok = event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
        event.keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
    if (!ok) return false
    if (event.action == android.view.KeyEvent.ACTION_UP && forgeSwallowOkUp) { forgeSwallowOkUp = false; return true }
    if (event.action == android.view.KeyEvent.ACTION_DOWN && forgeCancelAnnouncedChange(byViewer = true)) { forgeSwallowOkUp = true; return true }
    return false
}
