package com.nuvio.tv.ainarrator

import org.junit.Assert.*
import org.junit.Test

class NarratorPreparationTest {
    private val complete = (0 until 30).map { i ->
        NarratorLine(i * 10_000L, (i + 1) * 10_000L, "Linia numer " + i)
    }

    @Test fun rejectsIncompleteSubtitleTimeline() {
        assertNull(NarratorPreparationPlan.allFilmCues(emptyList(), 300_000L))
        assertNull(NarratorPreparationPlan.allFilmCues(complete.take(12), 300_000L))
        assertNull(NarratorPreparationPlan.allFilmCues(complete, 0L))
        assertEquals(30, NarratorPreparationPlan.allFilmCues(complete, 300_000L)?.size)
    }

    @Test fun halvesAreDisjointAndCoverAllCues() {
        val all = NarratorPreparationPlan.allFilmCues(complete, 300_000L)!!
        val first = NarratorPreparationPlan.select(all, 300_000L, PreparationHalf.FIRST)
        val second = NarratorPreparationPlan.select(all, 300_000L, PreparationHalf.SECOND)
        assertEquals(15, first.size)
        assertEquals(15, second.size)
        assertEquals(all, first + second)
    }

    @Test fun stableCacheKeysAllowResumeAcrossFilmsAndSessions() {
        val original = NarratorLine(0L, 2_000L, "To samo zdanie.")
        val rerun = NarratorLine(200_000L, 204_000L, "To samo zdanie.")
        assertEquals(NarratorPreparationPlan.key(original), NarratorPreparationPlan.key(rerun))
        assertNotEquals(NarratorPreparationPlan.key(original),
            NarratorPreparationPlan.key(NarratorLine(0L, 1_000L, "Inny tekst.")))
    }

    @Test fun fifteenMinuteBudgetIsAStopPointNotACompletionGuarantee() {
        assertFalse(NarratorPreparationPlan.shouldPause(899_999L))
        assertTrue(NarratorPreparationPlan.shouldPause(900_000L))
        assertEquals(50, PreparationStatus(PreparationPhase.WORKING, 10, 20).percentage)
    }

    @Test fun twoGbMayRunOnlyDuringPausedOfflineStageWithSufficientHeadroom() {
        val mib = 1024L * 1024L
        val sample = ZonosMemorySnapshot(2_000L * mib, 1_350L * mib, 100L * mib,
            systemLowMemory = false, applicationPssBytes = 100L * mib)
        assertFalse(ZonosMemoryPolicy.evaluate(sample, starting = true, offline = false).allowed)
        assertTrue(ZonosMemoryPolicy.evaluate(sample, starting = true, offline = true).allowed)
        assertFalse(ZonosMemoryPolicy.evaluate(sample.copy(availableBytes = 600L * mib),
            starting = true, offline = true).allowed)
        assertFalse(ZonosMemoryPolicy.evaluate(sample.copy(systemLowMemory = true),
            starting = true, offline = true).allowed)
    }

    @Test fun threeAndFourGbNeedLiveHeadroom() {
        val mib = 1024L * 1024L
        val sample3 = ZonosMemorySnapshot(3_000L * mib, 1_600L * mib,
            100L * mib, false, 200L * mib)
        assertTrue(ZonosMemoryPolicy.evaluate(sample3, starting = true, offline = true).allowed)
        val sample4 = sample3.copy(totalBytes = 4_000L * mib, availableBytes = 1_650L * mib)
        assertFalse(ZonosMemoryPolicy.evaluate(sample4, starting = true, offline = true).allowed)
    }
}
