package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.guide.GuidePriority
import com.nuvio.tv.reshaped.livetv.LiveTvCatchup
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy 22.11: archiwum IPTV (ile godzin zostało), przypomnienia, kolejność przewodników. */
class LiveTvLogicTest {
    private val hour = 3_600_000L
    private val day = 24 * hour
    private fun ch(days: Int?, source: String = "src", url: String = "http://host.example/live/1.ts") = LiveTvChannel(
        id = "1", name = "Kanał", streamUrl = url, sourceId = source,
        catchup = days?.let { LiveTvCatchup(LiveTvCatchup.Kind.Xtream, it) },
    )

    @Test fun `archiwum - godziny do końca z deklarowanej długości`() {
        val now = 100 * day
        val p = LiveTvProgramme("Film", now - 2 * day, now - 2 * day + hour)
        // 7 dni od startu programu minus 2 dni, które minęły = 5 dni = 120 h
        assertEquals(120, CatchupDepth.hoursLeft(ch(7, source = "decl-only"), p, now))
    }

    @Test fun `archiwum - brak archiwum, trwający program, przeterminowany`() {
        val now = 100 * day
        assertNull(CatchupDepth.hoursLeft(ch(null, source = "none"), LiveTvProgramme("x", now - day, now - day + hour), now))
        assertNull("program jeszcze trwa", CatchupDepth.hoursLeft(ch(7, source = "live"), LiveTvProgramme("x", now - hour, now + hour), now))
        assertNull("starszy niż archiwum", CatchupDepth.hoursLeft(ch(1, source = "old"), LiveTvProgramme("x", now - 3 * day, now - 3 * day + hour), now))
        assertNull("zero dni", CatchupDepth.hoursLeft(ch(0, source = "zero"), LiveTvProgramme("x", now - day, now - day + hour), now))
    }

    @Test fun `archiwum - klucz źródła z adresu, gdy brak id listy`() {
        assertEquals("host.example", CatchupDepth.sourceKey(ch(7, source = "")))
        assertEquals("lista-1", CatchupDepth.sourceKey(ch(7, source = "lista-1")))
    }

    @Test fun `przypomnienia - zapis i odczyt bez utraty pól`() {
        val list = listOf(
            Reminder("http://a/1.ts", 1_000L, "Wiadomości", "TVP Info"),
            Reminder("http://a/2.ts", 2_000L, "Tytuł\tz tabem\ni nową linią", "Kanał\t2"),
        )
        val back = Reminders.decode(Reminders.encode(list))
        assertEquals(2, back.size)
        assertEquals(list[0], back[0])
        assertEquals("Tytuł z tabem i nową linią", back[1].title)
        assertEquals("Kanał 2", back[1].channelName)
        assertEquals("http://a/2.ts|2000", back[1].key)
    }

    @Test fun `przypomnienia - uszkodzone wiersze pomijane`() {
        assertTrue(Reminders.decode("").isEmpty())
        assertTrue(Reminders.decode("tylko\ttrzy\tpola").isEmpty())
        assertTrue(Reminders.decode("u\tnie-liczba\tt\tk").isEmpty())
        assertEquals(1, Reminders.decode("u\t5\tt\tk\nzły").size)
    }

    @Test fun `przewodniki - epg ovh przed tv team, inne bez rangi`() {
        assertEquals(0, GuidePriority.rank("https://epg.ovh/pl.xml"))
        assertEquals(0, GuidePriority.rank("https://www.epg.ovh/pl.xml.gz"))
        assertEquals(1, GuidePriority.rank("http://epg.team/tvteam.xml"))
        assertEquals(1, GuidePriority.rank("http://x.tv.team/epg"))
        assertNull(GuidePriority.rank("https://iptv-org.github.io/epg.xml"))
        assertNull(GuidePriority.rank("nie adres"))
        assertNull("podobna nazwa to nie ten host", GuidePriority.rank("https://notepg.ovh.example.com/a"))
    }

    @Test fun `przewodniki - ranga dla repozytorium`() {
        assertEquals(-100, GuidePriority.repoRank("https://epg.ovh/a", nameMatched = false, nameRank = 5))
        assertEquals(-99, GuidePriority.repoRank("https://epg.team/a", nameMatched = false, nameRank = 5))
        assertEquals(5 - 100 + 1, GuidePriority.repoRank("https://epg.team/a", nameMatched = true, nameRank = 5))
        assertNull(GuidePriority.repoRank("https://inny.pl/a", nameMatched = true, nameRank = 5))
        // po identyfikatorze (bez dopasowania nazwy) zawsze przed dopasowaniem po nazwie z tego samego przewodnika
        assertTrue(GuidePriority.repoRank("https://epg.ovh/a", false, 0)!! < GuidePriority.repoRank("https://epg.ovh/a", true, 1)!!)
    }

    @Test fun `archiwum - pierwszy kanał z archiwum każdego źródła, kolejność listy`() {
        val list = listOf(
            ch(null, "a", "http://a/1"), ch(7, "a", "http://a/2"), ch(3, "b", "http://b/1"), ch(14, "a", "http://a/3"), ch(null, "c", "http://c/1"),
        )
        val firsts = CatchupDepth.firstWithArchive(list)
        assertEquals(listOf("a", "b"), firsts.keys.toList())
        assertEquals("http://a/2", firsts.getValue("a").streamUrl)
        assertEquals("http://b/1", firsts.getValue("b").streamUrl)
    }
}
