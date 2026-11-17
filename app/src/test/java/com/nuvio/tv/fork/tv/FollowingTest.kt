package com.nuvio.tv.fork.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowingTest {
    private val day = 24 * 3_600_000L
    private fun w(ep: String) = WatchRecord("s|$ep", "s", ep, "ch", 0, 1, state = WatchState.WATCHED)

    @Test fun `automatyczne po dwoch odcinkach i cofniecie nie wraca`() {
        val auto = Following.autoFollow(emptyList(), listOf(w("e1"), w("e2")), mapOf("s" to "Serial"), setOf("s"), 1L, true)
        assertEquals(1, auto.size); assertTrue(auto[0].auto)
        val off = Following.unfollow(auto, "s")
        assertTrue(Following.active(off).isEmpty())
        val again = Following.autoFollow(off, listOf(w("e1"), w("e2")), mapOf("s" to "Serial"), setOf("s"), 2L, true)
        assertTrue(Following.active(again).isEmpty())
    }
    @Test fun `jeden odcinek i wylaczone nie obserwuje`() {
        assertTrue(Following.autoFollow(emptyList(), listOf(w("e1")), emptyMap(), setOf("s"), 1L, true).isEmpty())
        assertTrue(Following.autoFollow(emptyList(), listOf(w("e1"), w("e2")), emptyMap(), setOf("s"), 1L, false).isEmpty())
    }
    @Test fun `twoje kanaly prog 3 w 14 dni i wygaszanie`() {
        val now = 100 * day
        val s = listOf(ViewSession("a", now - day, 30), ViewSession("a", now - 2 * day, 30), ViewSession("a", now - 3 * day, 30),
            ViewSession("b", now - day, 300), ViewSession("b", now - 20 * day, 300), ViewSession("b", now - 21 * day, 300))
        assertEquals(listOf("a"), ChannelStats.yourChannels(s, now))
        assertEquals(1.0, ChannelStats.weight(10 * day), 0.0); assertEquals(0.5, ChannelStats.weight(30 * day), 0.0); assertEquals(0.0, ChannelStats.weight(70 * day), 0.0)
        assertTrue(ChannelStats.prune(s, now + 50 * day).size < s.size)
    }
}
