package com.nuvio.tv.fork.tv

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentChannelsTest {
    private fun ch(n: Int, at: Long = n.toLong()) = RecentChannel(streamUrl = "u$n", name = "C$n", lastViewedMs = at)

    @Test
    fun `newest first and each channel once`() {
        var list = emptyList<RecentChannel>()
        list = RecentChannels.record(list, ch(1))
        list = RecentChannels.record(list, ch(2))
        list = RecentChannels.record(list, ch(1, at = 9))
        assertEquals(listOf("u1", "u2"), list.map { it.streamUrl })
        assertEquals(9L, list.first().lastViewedMs)
    }

    @Test
    fun `keeps at most fifteen`() {
        var list = emptyList<RecentChannel>()
        for (n in 1..20) list = RecentChannels.record(list, ch(n))
        assertEquals(RecentChannels.MAX, list.size)
        assertEquals("u20", list.first().streamUrl)
        assertEquals("u6", list.last().streamUrl)
    }

    @Test
    fun `remove drops one channel`() {
        val list = listOf(ch(1), ch(2), ch(3))
        assertEquals(listOf("u1", "u3"), RecentChannels.remove(list, "u2").map { it.streamUrl })
    }

    @Test
    fun `encode and decode round trip, damaged text gives an empty list`() {
        val list = listOf(ch(1), ch(2))
        assertEquals(list, RecentChannels.decode(RecentChannels.encode(list)))
        assertEquals(emptyList<RecentChannel>(), RecentChannels.decode("{not json"))
        assertEquals(emptyList<RecentChannel>(), RecentChannels.decode(null))
    }
}
