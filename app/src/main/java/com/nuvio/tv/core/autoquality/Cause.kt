package com.nuvio.tv.core.autoquality

/** forge M4a (project point 2): why playback is in trouble; each cause has its own reaction. */
enum class Cause { ADDRESS_EXPIRED, SERVER, LINK, DECODER }

/** What the monitor (M4c) or the player reported. */
sealed interface Signal {
    /** Echo thresholds: a stall ≥ 5 s, 2 stalls ≥ 0.4 s in 90 s, buffer < 5 s with the link < 0.9 × bitrate. */
    data class Urgent(val atMs: Long) : Signal
    /** E4: link < 0.9 × bitrate for 20 s, buffer down by 2 s and < 60 s. */
    data class Early(val atMs: Long) : Signal
    /** > 15% of frames dropped for 10 s with a full buffer. */
    data class DroppedFrames(val atMs: Long, val share: Double) : Signal
    /**
     * [fileInherent]: the device cannot play this file (decoder or container error, see
     * `core/player/recovery/isFileInherentPlaybackError`); M4c sets it. Such an error is the file's, not the server's.
     */
    data class PlayerError(val atMs: Long, val httpStatus: Int?, val io: Boolean, val fileInherent: Boolean = false) : Signal
}

/** [startupBps]: the link measured at start (M2b); [lastServerSwitchAtMs]: the last change made for a server. */
data class LinkFacts(val startupBps: Long?, val currentBitrateBps: Long?, val lastServerSwitchAtMs: Long?)

/** Headroom the startup measurement must give the playing file to blame the server rather than the link. */
const val SERVER_HEADROOM = 1.3
/** A new signal this soon after a change of server means the link, not the server. */
const val AFTER_SERVER_SWITCH_MS = 60_000L

private val ADDRESS_STATUSES = setOf(401, 403, 410)

fun diagnose(signal: Signal, link: LinkFacts, nowMs: Long): Cause = when (signal) {
    is Signal.PlayerError -> when {
        (signal.httpStatus ?: 0) in ADDRESS_STATUSES -> Cause.ADDRESS_EXPIRED
        // odbiór M4a (13.11): a decoder/container error must not block a healthy server
        signal.fileInherent -> Cause.DECODER
        // 404, 5xx, a connection or I/O error; any other player error is put on the server too (see the report)
        else -> Cause.SERVER
    }
    is Signal.DroppedFrames -> Cause.DECODER
    is Signal.Urgent, is Signal.Early -> slowCause(link, nowMs)
}

private fun slowCause(link: LinkFacts, nowMs: Long): Cause {
    val lastSwitch = link.lastServerSwitchAtMs
    if (lastSwitch != null && nowMs - lastSwitch < AFTER_SERVER_SWITCH_MS) return Cause.LINK
    val startup = link.startupBps ?: return Cause.LINK
    val bitrate = link.currentBitrateBps ?: return Cause.LINK
    return if (startup >= SERVER_HEADROOM * bitrate) Cause.SERVER else Cause.LINK
}
