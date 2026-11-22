package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.index.TvTextIndex
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.skin.kicia.livetv.forgeTvChannelHits
import com.nuvio.tv.skin.kicia.livetv.forgeTvSearch
import com.nuvio.tv.skin.kicia.livetv.forgeWordStarts
import com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 22.11 (właściciel): indeks szukania TV w tle i przyrostowo; JMGO build171: „tvn” przy 252 827 kanałach ~110 s. */
class TvTextIndexTest {
    private fun ch(name: String, key: String = name) = LiveTvChannel(id = name, name = name, streamUrl = "http://x/$name", tvgId = key, guideKey = key)
    private fun p(title: String, start: Long, len: Long = 30 * 60_000L) = LiveTvProgramme(title, start, start + len)

    @Before fun clean() = TvTextIndex.clearForTests()

    @Test fun `norm jak dawniej - bez znakow diakrytycznych, l zamiast ł`() {
        assertEquals("zazolc gesla jazn", TvTextIndex.norm("Zażółć gęślą jaźń"))
        assertEquals("tvn 24", TvTextIndex.norm("TVN 24"))
    }

    @Test fun `klucz kanalu bez kraju, nawiasow i jakosci - bez wyrazen regularnych`() {
        assertEquals("tvn", TvTextIndex.channelKey("pl: tvn hd"))
        assertEquals("tvn", TvTextIndex.channelKey("tvn (pa)"))
        assertEquals("polsat sport", TvTextIndex.channelKey("[pl] | polsat sport fhd"))
        assertEquals("tvp1", TvTextIndex.channelKey("tvp1.pl"))
        assertEquals("eleven sports 3", TvTextIndex.channelKey("eleven sports 3 hd"))
        assertEquals("cnn", TvTextIndex.channelKey("cnn 50 fps"))
        assertEquals("hd", TvTextIndex.channelKey("hd"))
        assertEquals("tvn (pa)", TvTextIndex.channelKey("tvn (pa) hd", keepBrackets = true))
    }

    @Test fun `przyrostowo - niezmienione kanaly bez pracy, zmiana i znikniecie kanalu aktualizuja spis`() {
        val a = listOf(p("Fakty", 0), p("Kropka nad i", 1))
        val b = listOf(p("Wiadomości", 0))
        TvTextIndex.update(listOf(ch("TVN"), ch("TVP1")), mapOf("tvn" to a, "tvp1" to b))
        assertEquals(setOf("tvn"), TvTextIndex.titles("fakty").single().second)
        assertEquals(1, TvTextIndex.titles("wiadomosci").size)
        // Ten sam obiekt listy dla tvn, nowa lista dla tvp1.
        TvTextIndex.update(listOf(ch("TVN"), ch("TVP1")), mapOf("tvn" to a, "tvp1" to listOf(p("Teleexpress", 0))))
        assertTrue("stary tytuł znika", TvTextIndex.titles("wiadomosci").isEmpty())
        assertEquals(setOf("tvp1"), TvTextIndex.titles("teleexpress").single().second)
        // tvn znika z przewodnika.
        TvTextIndex.update(listOf(ch("TVP1")), mapOf("tvp1" to listOf(p("Teleexpress", 0))))
        assertTrue(TvTextIndex.titles("fakty").isEmpty())
        assertTrue(TvTextIndex.ready())
    }

    @Test fun `ten sam tytul na dwoch kanalach - jeden wpis z dwoma kluczami`() {
        TvTextIndex.update(emptyList(), mapOf("a" to listOf(p("News", 0)), "b" to listOf(p("News ", 5))))
        assertEquals(setOf("a", "b"), TvTextIndex.titles("news").single().second)
        assertTrue(TvTextIndex.titles("").isEmpty())
    }

    @Test fun `kanaly - pelne trafienie przed poczatkiem nazwy i przed zawierajacymi, jakosci raz, inne kraje osobno`() {
        val list = listOf(ch("Marketing TVNK"), ch("TVN (PA)"), ch("PL: TVN HD"), ch("TVN 24"), ch("PL: TVN FHD"), ch("Dzień Dobry TVN"))
        val hits = forgeTvChannelHits("tvn", list, emptyMap(), 30).map { it.channel.name }
        assertEquals(listOf("TVN (PA)", "PL: TVN HD", "TVN 24", "Marketing TVNK", "Dzień Dobry TVN"), hits)
        assertEquals(2, forgeTvChannelHits("tvn", list, emptyMap(), 2).size)
        assertEquals("numer kanału", "PL: TVN HD", forgeTvChannelHits("3", list, emptyMap(), 4).first().channel.name)
    }

    @Test fun `szukanie programow z indeksu - jak bez indeksu`() {
        val now = 10 * 3_600_000L
        val chans = listOf(ch("TVN", "tvn"), ch("TVP1", "tvp1"))
        val sched = mapOf(chans[0].guideKey to listOf(p("Fakty", now - 60_000), p("Fakty po południu", now + 3_600_000)), chans[1].guideKey to listOf(p("Wiadomości", now)))
        val before = forgeTvSearch("fakty", ForgeEpgCategories.PROG_ALL, chans, emptyMap(), now, schedule = { sched[it].orEmpty() }, maxProgrammes = 50)
        TvTextIndex.update(chans, sched)
        val after = forgeTvSearch("fakty", ForgeEpgCategories.PROG_ALL, chans, emptyMap(), now, schedule = { sched[it].orEmpty() }, maxProgrammes = 50)
        assertEquals(before.programmes.map { it.programme.title }, after.programmes.map { it.programme.title })
        assertEquals(listOf("Fakty", "Fakty po południu"), after.programmes.map { it.programme.title })
        assertFalse(after.programmes.any { it.programme.title == "Wiadomości" })
    }

    @Test fun `ćwierć miliona kanałów - zapytanie po indeksie szybko`() {
        val chans = (0 until 250_000).map { ch("Kanał $it HD", "k$it") }
        val sched = (0 until 30_000).associate { chans[it].guideKey to listOf(p("Program ${it % 5000}", 0), p("News", 1)) }
        TvTextIndex.update(chans, sched)
        val t0 = System.nanoTime()
        val r = forgeTvSearch("program 42", ForgeEpgCategories.PROG_ALL, chans, emptyMap(), 0, schedule = { sched[it].orEmpty() }, maxChannels = 30, maxProgrammes = 50)
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertTrue("trwało $ms ms", ms < 3_000)
        assertTrue(r.programmes.isNotEmpty())
    }

    @Test fun `poczatek slowa bez dzielenia tekstu - jak dawniej`() {
        assertTrue(forgeWordStarts("dzien dobry tvn", "tvn"))
        assertTrue(forgeWordStarts("tvn 24", "tvn"))
        assertTrue(forgeWordStarts("pl:tvn", "tvn"))
        assertTrue(forgeWordStarts("film „tvn", "tvn"))
        assertFalse(forgeWordStarts("ctvn akd", "tvn"))
        assertTrue(forgeWordStarts("cokolwiek", ""))
    }

    @Test fun `jedna litera - tylko poczatek nazwy albo slowa`() {
        val list = listOf(ch("Kanał Sport"), ch("Teletoon"), ch("Ale Kino"))
        assertEquals(listOf("Teletoon"), forgeTvChannelHits("t", list, emptyMap(), 30).map { it.channel.name })
    }
}
