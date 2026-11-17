package com.nuvio.tv.fork.discover

import com.nuvio.tv.fork.tv.Followed
import com.nuvio.tv.fork.tv.LogEntry
import com.nuvio.tv.fork.tv.WatchRecord
import com.nuvio.tv.fork.tv.WatchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DiscoverTest {
    private val genres = GenreBuckets(File("src/main/assets/fork/genres.json").readText())

    @Test fun `40 kategorii trafia do koszykow`() {
        val cases = listOf(
            "film fabularny" to "Film", "Thriller" to "Film", "komedia romantyczna" to "Film", "Horror" to "Film", "science fiction" to "Film", "western" to "Film", "movie" to "Film", "dramat" to "Film",
            "serial obyczajowy" to "Serial", "telenowela" to "Serial", "sitcom" to "Serial", "Series" to "Serial", "miniserial" to "Serial", "serial kryminalny" to "Serial", "soap opera" to "Serial", "serial komediowy" to "Serial",
            "piłka nożna" to "Sport", "Football" to "Sport", "tenis" to "Sport", "Formula 1" to "Sport", "siatkówka" to "Sport", "skoki narciarskie" to "Sport", "mecz" to "Sport", "boks" to "Sport",
            "dla dzieci" to "Dla dzieci", "bajka" to "Dla dzieci", "Kids" to "Dla dzieci", "program dla dzieci" to "Dla dzieci",
            "dokument" to "Dokument", "Documentary" to "Dokument", "przyroda" to "Dokument", "reportaż" to "Dokument", "podróże" to "Dokument",
            "talk-show" to "Rozrywka", "reality show" to "Rozrywka", "teleturniej" to "Rozrywka", "Wiadomości" to "Informacje", "News" to "Informacje", "koncert" to "Muzyka", "Music" to "Muzyka", "teledysk" to "Muzyka",
        )
        val bad = cases.filter { (c, b) -> genres.bucketOf(listOf(c)) != b }
        assertTrue("źle: $bad (z ${cases.size})", cases.size >= 40 && bad.isEmpty())
        assertEquals("Sport", genres.bucketOf(emptyList(), "PL | Sport HD")); assertNull(genres.bucketOf(listOf("nieznane"), "Sport"))
        assertEquals(8, GenreBuckets.ALL.size)
    }

    private val h = 3_600_000L
    private fun e(key: String, guide: String, start: Long, dur: Long, ep: String = "") = LogEntry(key, guide, start, dur, ep, key.replaceFirstChar { it.uppercase() })

    @Test fun `wiersze scalanie bez pasm i znikanie pustych`() {
        val now = 100 * 24 * h + 21 * h
        val log = (1..5).map { e("tasiemiec", "ch1", now - it * 24 * h, h / 2, "e$it") } + // serial
            (1..6).map { e("wiadomosci", "ch1", now - it * 24 * h, h / 2, "d:same") } + // pasmo
            listOf(e("film", "ch2", now - 2 * h, 2 * h)) // film w archiwum
        val rec = listOf(WatchRecord("tasiemiec|e1", "tasiemiec", "e1", "ch1", now - 24 * h, h / 2, state = WatchState.WATCHED))
        val input = DiscoverInput(log, rec, listOf(Followed("tasiemiec", "Tasiemiec", true, 0)), setOf("ch1"), mapOf("ch1" to 7, "ch2" to 7), { if (it.programKey == "film") "Film" else null }, nowMs = now)
        val rows = DiscoverRows.build(input).associateBy { it.id }
        assertEquals(listOf("tasiemiec"), rows.getValue("series").cards.map { it.programKey }) // pasmo wiadomości nie trafia do Seriale, odcinki scalone w jedną kartę
        assertEquals("4", rows.getValue("followed").cards.single().badge) // 5 w archiwum minus 1 obejrzany
        assertEquals(listOf("film"), rows.getValue("films").cards.map { it.programKey })
        assertTrue("continue" !in rows && "genre_Sport" !in rows) // puste znikają
        assertEquals(listOf("film"), rows.getValue("genre_Film").cards.map { it.programKey })
    }

    @Test fun `ukrywanie kanalow i kolejnosc rzedow`() {
        val now = 50 * 24 * h
        val log = listOf(e("film", "hidden", now - 2 * h, 2 * h), e("film2", "ok", now - 3 * h, 2 * h))
        val input = DiscoverInput(log, emptyList(), emptyList(), emptySet(), mapOf("hidden" to 7, "ok" to 7), { null }, hiddenChannels = setOf("hidden"), nowMs = now)
        assertEquals(listOf("film2"), DiscoverRows.build(input).first { it.id == "films" }.cards.map { it.programKey })
        val rows = listOf(DiscoverRow("a", "A", listOf()), DiscoverRow("b", "B", listOf()), DiscoverRow("c", "C", listOf()))
        assertEquals(listOf("c", "a"), RowPrefs.apply(rows, listOf("c", "a", "b"), setOf("b")).map { it.id })
        assertTrue(DiscoverRows.build(input).all { it.cards.size <= DiscoverRows.MAX_CARDS })
    }

    @Test fun `szukanie 300 kanalow i 5000 emisji ponizej 50 ms`() {
        val channels = (1..300).map { SearchChannel("c$it", "Kanał Testowy $it", "G") } + SearchChannel("pol", "Polsat Sport 2")
        val now = 1_000_000_000L
        val airings = (1..5000).map { SearchAiring("p${it % 700}", "Program numer ${it % 700} wieczorny", "c${it % 300}", now + (it - 2500) * 600_000L, now + (it - 2500) * 600_000L + 3_000_000L) }
        val idx = TvSearchIndex(channels, airings)
        idx.search("pol", now); val t0 = System.nanoTime(); val r = idx.search("polsat spor", now); val ms = (System.nanoTime() - t0) / 1e6
        assertEquals("pol", r.channels.single().key); assertTrue("czas $ms", ms < 50.0)
        val prog = idx.search("progr 12", now)
        assertTrue(prog.nowAndSoon.size <= 1 || prog.nowAndSoon.map { it.programKey }.distinct().size == prog.nowAndSoon.size) // emisje jednego programu scalone
        assertTrue(idx.search("p", now).channels.isEmpty()) // od 2 liter
        assertTrue(idx.search("kanal testowy", now).channels.size >= 30 || idx.search("kanal testowy", now).channels.size == 30)
        assertEquals(listOf("b", "a"), RecentSearches.add(listOf("a"), " b "))
    }

    @Test fun `kolejka okladek limit 40 i brak podczas odtwarzania`() {
        val q = CoverFetchQueue(); val keys = (1..100).map { "k$it" }
        assertTrue(q.next(keys, playing = true).isEmpty())
        var total = 0; while (true) { val b = q.next(keys, playing = false); if (b.isEmpty()) break; total += b.size }
        assertEquals(40, total); q.startRefresh(); assertFalse(q.next(keys, false).any { it in keys.take(40) })
    }
}
