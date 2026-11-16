package com.nuvio.tv.core.player

// KE adapter: the fork's TrailerPlayerPool has no screensaver suppression hook; no-op.
@Suppress("UNUSED_PARAMETER")
fun TrailerPlayerPool.setScreensaverSuppressed(suppressed: Boolean) = Unit
