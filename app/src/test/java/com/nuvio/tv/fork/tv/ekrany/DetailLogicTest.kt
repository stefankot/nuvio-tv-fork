package com.nuvio.tv.fork.tv.ekrany

import com.nuvio.tv.fork.tv.GuideProgramme
import com.nuvio.tv.fork.tv.LogEntry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset

class DetailLogicTest {
    private val h = 3_600_000L
    private fun p(title: String, start: Long) = GuideProgramme(title, start, start + h)

    @After fun tearDown() = PendingPlay.clear()

    @Test fun `inne emisje to ten sam tytul a dalej to nastepne`() {
        val now = 100 * h; val shown = p("Wiadomości", now)
        val a = airingsOf(listOf(p("Wiadomości", now - 24 * h), shown, p("Film", now + h), p("wiadomości ", now + 24 * h)), shown, now)
        assertEquals(listOf(now - 24 * h, now + 24 * h), a.others.map { it.startMs })
        assertEquals(listOf(now + h, now + 24 * h), a.next.map { it.startMs })
    }

    @Test fun `etykiety odcinkow`() {
        assertEquals("Sezon 3 · Odcinek 168", episodeLabel("S3E168")); assertEquals("Odcinek 6009", episodeLabel("E6009"))
        assertEquals("Sezon 3 · Odcinek 168", episodeLabel("3x168")); assertEquals("Sezon 3 · Odcinek 168", episodeLabel("2.167.0/1"))
        assertNull(episodeLabel(" ")); assertNull(episodeLabel(null))
    }

    @Test fun `grupowanie w sezony i lista wedlug daty bez sezonow`() {
        fun e(ep: String, start: Long) = LogEntry("p", "c", start, h, ep, "P")
        val g = groupBySeason(listOf(e("s02e01", 5), e("s01e02", 3), e("s01e01", 1), e("s01e01", 9)))
        assertEquals(listOf(1, 2), g.keys.toList()); assertEquals(listOf("s01e01", "s01e02"), g[1]!!.map { it.episodeId })
        val flat = groupBySeason(listOf(e("e5", 9), e("e4", 2)))
        assertEquals(listOf(null), flat.keys.toList()); assertEquals(listOf("e4", "e5"), flat[null]!!.map { it.episodeId })
    }

    @Test fun `dni i dostepnosc`() {
        val now = 1_790_000_000_000L
        assertEquals("dziś", dayLabel(now, now, ZoneOffset.UTC)); assertEquals("jutro", dayLabel(now + 86_400_000, now, ZoneOffset.UTC))
        assertEquals(Availability.IN_ARCHIVE, availability(now - 2 * 86_400_000L, now - 86_400_000L, now, 7))
        assertEquals(Availability.GONE, availability(now - 9 * 86_400_000L, now - 8 * 86_400_000L, now, 7))
        assertEquals(Availability.FUTURE, availability(now + h, now + 2 * h, now, 0))
    }

    @Test fun `mini guide przyciski i postep`() {
        assertEquals(listOf(MiniSlot.INFO, MiniSlot.RESTART, MiniSlot.FAV, MiniSlot.NEXT), MiniGuideLogic.buttons(0, true, true))
        assertEquals(listOf(MiniSlot.INFO, MiniSlot.REMIND, MiniSlot.FAV), MiniGuideLogic.buttons(2, true, false))
        assertEquals(0.5f, MiniGuideLogic.progress(0, 1000, 500), 0.001f); assertEquals("zostało 8 min", MiniGuideLogic.left(7 * 60_000L + 1, 0))
        assertEquals("21:30", MiniGuideLogic.time(21L * h + 30 * 60_000, java.util.TimeZone.getTimeZone("UTC")))
    }

    private val channels = mapOf("http://a" to "A", "http://b" to "B")
    @Test fun `swieza prosba uruchamia kanal raz a stara nigdy`() {
        PendingPlay.ask("http://b", 1_000); assertEquals("B", PendingPlay.take(channels, 2_000)); assertNull(PendingPlay.take(channels, 2_100))
        PendingPlay.ask("http://a", 0); assertNull(PendingPlay.take(channels, PendingPlay.ValidMs + 1)); assertNull(PendingPlay.request.value)
        PendingPlay.ask("http://b", 0); assertNull(PendingPlay.take(emptyMap<String, String>(), 100)); assertEquals("B", PendingPlay.take(channels, 500))
    }
}
