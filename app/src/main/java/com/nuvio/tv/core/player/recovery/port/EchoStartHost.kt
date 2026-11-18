package com.nuvio.tv.core.player.recovery.port

import kotlinx.coroutines.CoroutineScope

/**
 * forge M3c: the one door between the copied Echo startup code (`core/player/recovery/echo`) and Kicia. The copy
 * never imports our recovery layer; [com.nuvio.tv.ui.screens.player.PlayerAutoSourceRecovery] implements this.
 */
interface EchoStartHost {
    val scope: CoroutineScope
    /** The address the player is starting now (already resolved; never a debrid file still to resolve). */
    fun currentUrl(): String?
    fun currentHeaders(): Map<String, String>
    /** The player has shown a frame, or is playing (an engine that reports no frames). */
    fun hasStarted(): Boolean
    /** The probe finished: [delivers] whether the address sent real video, in [ms]. */
    fun probed(delivers: Boolean, ms: Long)
    /** The running attempt cannot start; move on now instead of waiting for its deadline. */
    fun failAttempt(reason: String)
}
