package com.nuvio.tv.skin.kicia.player

/**
 * Remote-controlled seek bar logic for every player (movies, YouTube, live TV time-shift).
 * LAYOUT_SPEC §2.1. Pure Kotlin so the rules are tested on the JVM; the UI is [ForgeScrubBar].
 *
 * - tap left/right: +-10 s at once
 * - hold: a step every 200 ms; the step grows with how long the key is held
 *   (0-1 s: 10 s, 1-2.5 s: 30 s, 2.5-5 s: 60 s, after that 120 s)
 * - Up: preview mode - a marker detaches from the playing position; Left/Right move the marker;
 *   OK confirms, Back cancels
 * - on live TV the bar ends at the live edge: it can't be scrubbed past it
 */
object ForgeScrubRates {
    const val TickMs = 200L

    fun stepMs(heldMs: Long): Long = when {
        heldMs < 1_000L -> 10_000L
        heldMs < 2_500L -> 30_000L
        heldMs < 5_000L -> 60_000L
        else -> 120_000L
    }
}

data class ForgeScrubState(
    val positionMs: Long,
    val durationMs: Long,
    /** Furthest point the bar can reach. Equals the duration for a film; the live edge for TV time-shift. */
    val edgeMs: Long = durationMs,
    /** Preview marker, or null when the bar is just following playback. */
    val markerMs: Long? = null,
) {
    val inPreview: Boolean get() = markerMs != null
}

/**
 * Turns key presses into seeks. Call [arrowDown] when Left/Right goes down, [tick] every
 * [ForgeScrubRates.TickMs] while it is held, and [arrowUp] when it is released.
 */
class ForgeScrubDriver(
    initial: ForgeScrubState,
    private val onSeek: (Long) -> Unit,
    private val onChange: (ForgeScrubState) -> Unit = {},
) {
    var state: ForgeScrubState = initial
        private set

    private var holdForward = true
    private var holdStartMs = -1L

    val isHolding: Boolean get() = holdStartMs >= 0

    /** Playback moved on (or the media changed): follow it unless the user is previewing. */
    fun updatePlayback(positionMs: Long, durationMs: Long = state.durationMs, edgeMs: Long = state.edgeMs) {
        publish(state.copy(positionMs = positionMs, durationMs = durationMs, edgeMs = edgeMs))
    }

    fun arrowDown(forward: Boolean, timeMs: Long) {
        if (isHolding && holdForward == forward) return // auto-repeat from the remote: ticks do the work
        holdForward = forward
        holdStartMs = timeMs
        step(0L)
    }

    fun tick(timeMs: Long) {
        if (!isHolding) return
        step(timeMs - holdStartMs)
    }

    fun arrowUp() {
        holdStartMs = -1L
    }

    /** The Up key. Returns true when it entered preview mode (the key is used up). */
    fun up(): Boolean {
        if (state.inPreview) return false
        publish(state.copy(markerMs = state.positionMs))
        return true
    }

    /** OK: confirm the preview. Returns false when there was no preview (caller pauses/resumes instead). */
    fun ok(): Boolean {
        val marker = state.markerMs ?: return false
        publish(state.copy(positionMs = marker, markerMs = null))
        onSeek(marker)
        return true
    }

    /** Back: cancel the preview. Returns false when there was none (caller hides the UI instead). */
    fun back(): Boolean {
        if (!state.inPreview) return false
        publish(state.copy(markerMs = null))
        return true
    }

    private fun step(heldMs: Long) {
        val delta = ForgeScrubRates.stepMs(heldMs) * if (holdForward) 1 else -1
        val marker = state.markerMs
        if (marker != null) {
            publish(state.copy(markerMs = clamp(marker + delta)))
        } else {
            val target = clamp(state.positionMs + delta)
            publish(state.copy(positionMs = target))
            onSeek(target)
        }
    }

    private fun clamp(value: Long) = value.coerceIn(0L, state.edgeMs.coerceAtLeast(0L))

    private fun publish(next: ForgeScrubState) {
        state = next
        onChange(next)
    }
}

/** "1:03:08" / "4:56" - hours only when needed. */
fun formatScrubTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Time left, with a real minus sign, e.g. "−1:42:51". */
fun formatScrubRemaining(positionMs: Long, durationMs: Long): String =
    "−" + formatScrubTime((durationMs - positionMs).coerceAtLeast(0L))
