package com.nuvio.tv.fork.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProgramTypesTest {
    private val h = 3_600_000L
    private val now = 100 * 24 * h
    private fun a(i: Int, ep: String, dur: Long = h, start: Long = now - i * 24 * h) = LogEntry("p", "ch", start, dur, ep, "P")

    @Test fun `serial codzienny z numerami`() {
        assertEquals(ProgramType.SERIES, ProgramTypes.classify((1..5).map { a(it, "e$it", 30 * 60_000L) }, now))
    }
    @Test fun `wiadomosci to pasmo`() {
        assertEquals(ProgramType.BAND, ProgramTypes.classify((1..6).map { a(it, "d:same", 30 * 60_000L) } , now))
        assertFalse(ProgramTypes.hasUnwatchedCounter(ProgramType.BAND))
    }
    @Test fun `film`() {
        assertEquals(ProgramType.FILM, ProgramTypes.classify(listOf(a(0, "", 120 * 60_000L)), now))
        assertEquals(ProgramType.FILM, ProgramTypes.classify(listOf(a(0, "d:x", 40 * 60_000L)), now, tmdbMovie = true))
    }
    @Test fun `jednorazowy`() {
        assertEquals(ProgramType.ONE_OFF, ProgramTypes.classify(listOf(a(0, "", 45 * 60_000L)), now))
    }
}
