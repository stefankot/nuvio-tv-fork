package com.nuvio.tv.fork.echo

import com.nuvio.tv.core.streams.AutoQualitySample
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStarvationTest {
    private val guard = PlaybackStarvation()

    private fun sample(now: Long, stalled: Boolean = true, buffer: Long = 200L) =
        AutoQualitySample(now, 1_000L, wantsToPlay = true, stalled, buffer, 5_000_000L)

    @Test
    fun shortStallsAndLowBufferWhilePlayingDoNotAuthorizeDowngrade() {
        (0L..4_000L step 1_000L).forEach { guard.observe(sample(it)) }
        guard.observe(sample(5_000L, stalled = false, buffer = 400L))
        (6_000L..11_000L step 1_000L).forEach { guard.observe(sample(it)) }
        assertFalse(guard.permitsDowngrade(sample(11_000L)))
    }

    @Test
    fun audioPipelineResetClearsPreviousNetworkEvidence() {
        (0L..7_000L step 1_000L).forEach { guard.observe(sample(it)) }
        guard.localTransition(7_500L, 12_000L)
        (8_000L..24_000L step 1_000L).forEach { guard.observe(sample(it)) }
        assertFalse(guard.permitsDowngrade(sample(24_000L)))
        assertFalse(guard.isLocalTransition(24_000L))
    }

    @Test
    fun continuingNetworkStarvationCanDowngradeButRecoveredPlaybackCancelsTheDecision() {
        (0L..8_000L step 1_000L).forEach { guard.observe(sample(it)) }
        assertTrue(guard.permitsDowngrade(sample(8_500L)))
        assertFalse(guard.permitsDowngrade(sample(8_500L, stalled = false, buffer = 400L)))
    }

    @Test
    fun missingSamplesCannotAuthorizeAStaleDowngrade() {
        guard.observe(sample(0))
        guard.observe(sample(9_000))
        assertFalse(guard.permitsDowngrade(sample(9_000)))
    }
}
