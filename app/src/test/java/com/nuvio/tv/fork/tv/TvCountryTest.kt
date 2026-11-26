package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.country.TvCountry
import com.nuvio.tv.fork.tv.country.TvCountry.Mode
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Właściciel 22.11: preferowany kraj IPTV (najpierw), konieczny kraj (tylko); domyślnie najpierw Polska, bez wyłączności. */
class TvCountryTest {
    private val providers = listOf("Strong8K", "Trex")
    private fun ch(name: String, src: String = "flix") = LiveTvChannel(id = name, name = name, streamUrl = "http://x/$src/$name", sourceId = src)
    private val lists = mapOf("hlsgd" to "pl")
    private val list = listOf(
        ch("Strong8K · PA: TVN HD"), ch("TVN", "hlsgd"), ch("Strong8K · DSTV: TVN AFRICA"), ch("Strong8K · PL: TVN 24 HD"),
        ch("UK: BBC One"), ch("[PL] Polsat"), ch("Kanał bez kraju"),
    )
    private fun names(l: List<LiveTvChannel>) = l.map { it.name }

    @Test fun `kraj z nazwy, po zdjeciu dostawcy, inaczej kraj listy`() {
        assertEquals("pa", TvCountry.countryOf(list[0], providers) { lists[it] })
        assertEquals("pl", TvCountry.countryOf(list[1], providers) { lists[it] })
        assertEquals("pl", TvCountry.countryOf(list[3], providers) { lists[it] })
        assertEquals("pl", TvCountry.countryOf(list[5], providers) { lists[it] })
        assertNull(TvCountry.countryOf(list[6], providers) { lists[it] })
    }

    @Test fun `preferowany - polskie na poczatek, reszta w kolejnosci listy`() {
        val r = TvCountry.arrange(list, Mode(prefer = true, only = false, country = "pl"), providers, { lists[it] })
        assertEquals(listOf("TVN", "Strong8K · PL: TVN 24 HD", "[PL] Polsat", "Strong8K · PA: TVN HD", "Strong8K · DSTV: TVN AFRICA", "UK: BBC One", "Kanał bez kraju"), names(r))
    }

    @Test fun `konieczny - tylko polskie`() {
        val r = TvCountry.arrange(list, Mode(prefer = true, only = true, country = "pl"), providers, { lists[it] })
        assertEquals(listOf("TVN", "Strong8K · PL: TVN 24 HD", "[PL] Polsat"), names(r))
    }

    @Test fun `konieczny bez trafien - cala lista zamiast pustego ekranu`() {
        var told = false
        val r = TvCountry.arrange(list, Mode(prefer = false, only = true, country = "cz"), providers, { lists[it] }) { told = true }
        assertEquals(list, r)
        assertTrue(told)
    }

    @Test fun `wylaczone - lista bez zmian`() {
        assertEquals(list, TvCountry.arrange(list, Mode(prefer = false, only = false, country = "pl"), providers, { lists[it] }))
    }

    /** IPTV #81 (JMGO 26.11): nazwa źródła inna niż przedrostek („Strong8K” ≠ nazwa źródła) — kraj i tak z nazwy; grupa listy. */
    @Test fun `kraj za przedrostkiem dostawcy innym niz nazwa zrodla`() {
        val none = emptyList<String>()
        assertEquals("pl", TvCountry.computeCountry("Strong8K · PL: TVN CZAS NA ŚLUB ᴴᴰ ◉", none))
        assertEquals("pl", TvCountry.computeCountry("Strong8K: PL| TVN 7 FHD", none))
        assertEquals("de", TvCountry.computeCountry("Strong8K · DE: Das Erste HD", none))
        assertEquals("pl", TvCountry.computeCountry("|PL| TVP 1", none))
        assertEquals("pl", TvCountry.computeCountry("PL: Polsat Cafe HD", none))
        assertEquals("pl", TvCountry.computeCountry("Strong8K · PL VIP: CANAL+ SPORT 2 ᴿᴬᵂ", none))
        assertEquals("pl", TvCountry.computeCountry("PL VIP: TVN 24", none))
        assertEquals("", TvCountry.computeCountry("Strong8K · 24/7: RUNNING WILD WITH BEAR GRYLLS", none))
        assertEquals("", TvCountry.computeCountry("Strong8K · DSTV: TVN AFRICA", none))
        assertEquals("", TvCountry.computeCountry("Kanał bez kraju", none))
        assertEquals("uk", TvCountry.countryOf(LiveTvChannel(id = "a", name = "BBC One", streamUrl = "http://x/a", group = "UK | ENTERTAINMENT"), none) { null })
        assertNull(TvCountry.countryOf(LiveTvChannel(id = "b", name = "BBC Two", streamUrl = "http://x/b", group = "Rozrywka"), none) { null })
    }

    @Test fun `domyslnie Polska - nazwa i lista krajow`() {
        assertEquals("Polska", TvCountry.nameOf("pl"))
        assertEquals("pl", TvCountry.CHOICES.first().first)
    }
}
