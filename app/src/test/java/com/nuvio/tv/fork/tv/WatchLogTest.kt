package com.nuvio.tv.fork.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchLogTest {
    private val min = 60_000L
    private val dur = 60 * min
    private fun rec() = WatchRecord("k", "serial", "e1", "ch", 0L, dur)

    @Test fun `przeskok na koniec nie zalicza`() {
        val r = WatchLog.tick(rec(), 59 * min, 1L)
        assertTrue(r.state != WatchState.WATCHED)
    }

    @Test fun `90 procent z przewijaniem reklam zalicza`() {
        var r = rec()
        // 0-29 min, reklamy pominięte (30-44), potem 45-58 min: 44 minuty z 60 (73%), pozycja 58 > 54
        (0..29).forEach { r = WatchLog.tick(r, it * min, 1L) }
        (45..58).forEach { r = WatchLog.tick(r, it * min, 1L) }
        assertEquals(WatchState.WATCHED, r.state)
    }

    @Test fun `na zywo od polowy do konca zalicza przy 50 procentach`() {
        var r = rec()
        (30..59).forEach { r = WatchLog.tick(r, it * min, 1L) }
        assertEquals(WatchState.WATCHED, r.state)
    }

    @Test fun `w trakcie po dwoch minutach`() {
        var r = rec()
        r = WatchLog.tick(r, 0, 1L); assertEquals(WatchState.UNWATCHED, r.state)
        r = WatchLog.tick(r, min, 1L); assertEquals(WatchState.IN_PROGRESS, r.state)
    }

    @Test fun `reczne oznaczanie i wszystkie wczesniejsze`() {
        val a = rec().copy(key = "a", startMs = 0); val b = rec().copy(key = "b", startMs = 10 * dur)
        val out = WatchLog.markAllEarlier(listOf(a, b), b, 5L)
        assertEquals(WatchState.WATCHED, out[0].state); assertEquals(WatchState.UNWATCHED, out[1].state)
        assertEquals(WatchState.UNWATCHED, WatchLog.markUnwatched(out[0]).state)
    }

    @Test fun `wznowienie z archiwum albo z powtorki`() {
        val now = 20 * dur
        val inArchive = rec().copy(startMs = 10 * dur, positionMs = 5 * min)
        assertEquals(WatchLog.Resume.Archive("ch", 10 * dur, 5 * min), WatchLog.resume(inArchive, emptyList(), mapOf("ch" to 7), now))
        val rep = LogEntry("serial", "ch2", now + dur, dur, "e1", "Serial")
        val old = rec().copy(startMs = 0)
        assertEquals(WatchLog.Resume.Repeat(rep), WatchLog.resume(old, listOf(rep), mapOf("ch" to 0), now))
    }
}
