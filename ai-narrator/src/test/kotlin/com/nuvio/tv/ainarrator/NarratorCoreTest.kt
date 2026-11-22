package com.nuvio.tv.ainarrator

import org.junit.Assert.*
import org.junit.Test

class NarratorCoreTest {
    @Test fun cueBoundariesAndSeek() {
        val lines = listOf(
            NarratorLine(1000, 2500, "Dzień dobry."),
            NarratorLine(3000, 4500, "Dokąd idziemy?")
        )
        assertNull(NarratorPlanner.current(lines, 999))
        assertEquals("Dzień dobry.", NarratorPlanner.current(lines, 1000)?.text)
        assertNull(NarratorPlanner.current(lines, 2500))
        assertEquals("Dokąd idziemy?", NarratorPlanner.current(lines, 3200)?.text)
        assertFalse(NarratorPlanner.stillAudible(lines[0], 2450))
    }

    @Test fun onlySpeechAndDeterministicCache() {
        assertFalse(NarratorText.eligible("[muzyka]"))
        assertTrue(NarratorText.eligible("Nie!"))
        val a = NarratorText.fingerprint(NarratorModel.FLASH_LITE, "Kore", "Tak.", NarratorText.style("Tak.", null))
        assertEquals(a, NarratorText.fingerprint(NarratorModel.FLASH_LITE, "Kore", "Tak.", NarratorText.style("Tak.", null)))
        assertNotEquals(a, NarratorText.fingerprint(NarratorModel.FLASH, "Kore", "Tak.", NarratorText.style("Tak.", null)))
    }

    @Test fun styleIsRestrainedAndPricesRollOver() {
        assertTrue(NarratorText.style("Naprawdę?", null).contains("Pytanie tylko minimalnie podnieś")) // styl 19.11: klasyczny lektor
        assertFalse(NarratorText.style("Tak.", null).contains("Pytanie"))
        val old = NarratorPrices.rate(NarratorModel.FLASH_LITE, 2026, 12)
        val newRate = NarratorPrices.rate(NarratorModel.FLASH_LITE, 2027, 1)
        assertTrue(newRate.second > old.second)
    }

    @Test fun prefetchDoesNotIncludeExpiredSegments() {
        val lines = listOf(NarratorLine(0, 200, "Przed."), NarratorLine(600, 900, "Teraz."))
        assertEquals(listOf("Teraz."), NarratorPlanner.upcoming(lines, 500, 500).map { it.text })
    }
}
