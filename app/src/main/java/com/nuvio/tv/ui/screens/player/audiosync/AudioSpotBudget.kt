package com.nuvio.tv.ui.screens.player.audiosync

/** One byte budget for a playback source, including cancelled or rebuilt sampling sessions. */
internal class AudioSpotBudget {
    private var readBytes = 0L
    private var active = false

    /** Null means another sampler is still finishing; zero means there are no bytes left. */
    @Synchronized
    fun begin(limit: Long): Long? {
        if (active) return null
        val remaining = (limit - readBytes).coerceAtLeast(0L)
        active = remaining > 0
        return remaining
    }

    @Synchronized
    fun finish(bytes: Long) {
        readBytes += bytes
        active = false
    }
}
