package com.nuvio.tv.skin.niunia

import com.nuvio.tv.skin.niunia.components.NiuniaHold
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** UX JMGO 24.11 (F9): przy przytrzymaniu kosztowna praca po fokusie rusza raz, po zatrzymaniu. */
class NiuniaHoldTest {
    @Test fun holdThreeSecondsFiresOnce() {
        val moves = (0 until 60).map { it * 50L } // 3 s powtórzeń co 50 ms
        assertEquals(listOf(59), NiuniaHold.fired(moves))
    }

    @Test fun slowStepsFireEach() {
        assertEquals(listOf(0, 1, 2), NiuniaHold.fired(listOf(0L, 400L, 900L)))
    }

    @Test fun pauseInsideHoldFiresAtPause() {
        assertEquals(listOf(1, 3), NiuniaHold.fired(listOf(0L, 100L, 600L, 650L)))
    }

    @Test fun repeatFlag() {
        assertFalse(NiuniaHold.isRepeat(0))
        assertTrue(NiuniaHold.isRepeat(1))
    }

    @Test fun empty() = assertEquals(emptyList<Int>(), NiuniaHold.fired(emptyList()))
}
