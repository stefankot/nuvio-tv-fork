package com.nuvio.tv.fork.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentlyViewedTest {
    private fun show(key: String, at: Long = 1L, seen: List<String> = emptyList()) = RecentShow(
        key = key, title = key.replaceFirstChar { it.uppercase() }, channelName = "TVP 1", channelUrl = "https://x/$key",
        guideKey = "g", lastViewedMs = at, seenEpisodes = seen,
    )

    @Test fun newestFirstAndOneEntryPerShow() {
        var list = emptyList<RecentShow>()
        list = RecentlyViewed.record(list, show("a", 1))
        list = RecentlyViewed.record(list, show("b", 2))
        list = RecentlyViewed.record(list, show("a", 3))
        assertEquals(listOf("a", "b"), list.map { it.key })
        assertEquals(3L, list.first().lastViewedMs)
    }

    @Test fun keepsFifteenAtMost() {
        var list = emptyList<RecentShow>()
        for (i in 1..20) list = RecentlyViewed.record(list, show("s$i", i.toLong()))
        assertEquals(15, list.size)
        assertEquals("s20", list.first().key)
        assertEquals("s6", list.last().key)
    }

    @Test fun seenEpisodesAreMergedNotLost() {
        var list = RecentlyViewed.record(emptyList(), show("a", 1, listOf("e01")))
        list = RecentlyViewed.record(list, show("a", 2, listOf("e02")))
        assertEquals(listOf("e01", "e02"), list.single().seenEpisodes)
        list = RecentlyViewed.record(list, show("a", 3, listOf("e02")))
        assertEquals(listOf("e01", "e02"), list.single().seenEpisodes)
    }

    @Test fun removeTakesOneShowOut() {
        val list = listOf(show("a"), show("b"))
        assertEquals(listOf("b"), RecentlyViewed.remove(list, "a").map { it.key })
    }

    @Test fun savesAndReadsBack() {
        val list = listOf(show("a", 5, listOf("e01")), show("b", 4).copy(imageUrl = "https://img/b.jpg"))
        assertEquals(list, RecentlyViewed.decode(RecentlyViewed.encode(list)))
    }

    @Test fun damagedOrMissingTextGivesAnEmptyList() {
        assertTrue(RecentlyViewed.decode(null).isEmpty())
        assertTrue(RecentlyViewed.decode("").isEmpty())
        assertTrue(RecentlyViewed.decode("{not json").isEmpty())
    }
}
