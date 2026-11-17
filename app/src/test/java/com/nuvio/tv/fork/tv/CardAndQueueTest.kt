package com.nuvio.tv.fork.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CardAndQueueTest {
    @Test fun `okladka najpierw z EPG potem TMDB tylko przy zgodnosci`() {
        assertEquals("epg", chooseCover("epg", "tmdb", true))
        assertEquals("tmdb", chooseCover(null, "tmdb", true))
        assertNull(chooseCover(null, "tmdb", false))
    }

    @Test fun `cache trafienie i wygasanie braku`() {
        var now = 1_000L
        val f = File.createTempFile("cards", ".json")
        val c = ProgramCardCache(f) { now }
        c.put(ProgramCard("a", "A", coverUrl = "x")); c.putMissing("b")
        assertTrue(c.get("a") is ProgramCardCache.Result.Hit)
        assertTrue(c.get("b") is ProgramCardCache.Result.KnownMissing)
        now += ProgramCardCache.MISS_TTL_MS + 1
        assertTrue(c.get("b") is ProgramCardCache.Result.Unknown)
        assertTrue(ProgramCardCache(f) { now }.get("a") is ProgramCardCache.Result.Hit) // z dysku
    }

    private fun i(k: String, b: Badge) = QueueItem(k, k, "ch", 0, b)

    @Test fun `kolejnosc bez duplikatow i limit 12`() {
        val q = ForYouQueue.build(QueueInput(listOf(i("a", Badge.RESUME)), listOf(i("a", Badge.NEW_EPISODE), i("b", Badge.NEW_EPISODE)), listOf(i("c", Badge.LIVE)), (1..20).map { i("u$it", Badge.LIVE) }))
        assertEquals(listOf("a", "b", "c"), q.take(3).map { it.programKey }); assertEquals(Badge.RESUME, q[0].badge); assertEquals(12, q.size)
    }
    @Test fun `nigdy pusta gdy cos jest na antenie`() {
        assertEquals(1, ForYouQueue.build(QueueInput(emptyList(), emptyList(), emptyList(), emptyList(), listOf(i("x", Badge.LIVE)))).size)
    }
}
