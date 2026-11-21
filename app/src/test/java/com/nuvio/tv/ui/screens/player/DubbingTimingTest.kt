package com.nuvio.tv.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Test

class DubbingTimingTest {
    @Test
    fun `positive offset advances source B`() {
        assertEquals(12_500L, DubbingTiming.sourcePositionMs(10_000L, 2_500L))
    }

    @Test
    fun `negative source position is clamped`() {
        assertEquals(0L, DubbingTiming.sourcePositionMs(1_000L, -2_000L))
    }

    @Test
    fun `offset is clamped to thirty minutes`() {
        assertEquals(DubbingTiming.MAX_OFFSET_MS, DubbingTiming.clampOffset(Long.MAX_VALUE))
        assertEquals(DubbingTiming.MIN_OFFSET_MS, DubbingTiming.clampOffset(Long.MIN_VALUE))
    }

    @Test
    fun `device delay and dubbing offset remain independent`() {
        assertEquals(-1.75, DubbingTiming.effectiveMpvAudioDelaySeconds(250, 2_000L), 0.0001)
    }

    @Test
    fun `media source maps B timestamps onto A clock`() {
        assertEquals(12_500_000L, DubbingTiming.sourcePositionUs(10_000_000L, 2_500L))
        assertEquals(10_000_000L, DubbingTiming.presentationPositionUs(12_500_000L, 2_500L))
        assertEquals(0L, DubbingTiming.sourcePositionUs(1_000_000L, -2_500L))
        assertEquals(2_500_000L, DubbingTiming.presentationPositionUs(0L, -2_500L))
    }

    @Test
    fun `negative offset keeps requested seek inside initial silent gap`() {
        assertEquals(0L, DubbingTiming.resolvedSeekPositionUs(0L, 0L, -12_930L))
        assertEquals(5_000_000L, DubbingTiming.resolvedSeekPositionUs(5_000_000L, 0L, -12_930L))
        assertEquals(12_930_000L, DubbingTiming.resolvedSeekPositionUs(12_930_000L, 0L, -12_930L))
        assertEquals(20_000_000L, DubbingTiming.resolvedSeekPositionUs(20_000_000L, 7_070_000L, -12_930L))
    }
}
