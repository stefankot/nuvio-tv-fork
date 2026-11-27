package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.dupes.ChannelDupes
import com.nuvio.tv.fork.tv.dupes.ChannelDupes.Copy
import com.nuvio.tv.fork.tv.dupes.DupeMemory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Właściciel 22.11: TVN HD/UHD/4K od 5 dostawców = jeden kanał, start najlepszej jakości (4K), pamięć wyniku 4 h. */
class ChannelDupesTest {
    private val providers = listOf("Strong8K", "Trex", "Dream 4K", "DINO IPTV", "Neo 4K Pro")
    private fun key(n: String, list: String? = null) = ChannelDupes.groupKey(n, providers, list)

    @Test fun `znaki w indeksie gornym jak zwykle litery`() {
        assertEquals("vip raw", ChannelDupes.plain("ⱽᴵᴾ ᴿᴬᵂ"))
        assertEquals("zazolc", ChannelDupes.plain("Zażółć"))
    }

    @Test fun `jakosci i dostawcy tego samego kanalu w jednej grupie`() {
        val k = key("PL: TVN HD")
        assertEquals("pl|tvn", k)
        for (n in listOf("PL: TVN FHD", "PL: TVN 4K", "PL: TVN UHD ᴿᴬᵂ", "Strong8K · PL: TVN HD", "Trex · PL | TVN HEVC", "[PL] TVN HD", "TVN (PL) FHD", "PL - TVN 50 FPS"))
            assertEquals(n, k, key(n))
    }

    @Test fun `kraj zostaje w kluczu, lista bez kraju dostaje kraj listy`() {
        assertNotEquals(key("PL: TVN HD"), key("PA: TVN HD"))
        assertEquals(key("PL: TVN HD"), key("TVN", list = "PL"))
        assertNotEquals("inne kanały", key("PL: TVN"), key("PL: TVN 24"))
        assertEquals("pl|tvp 1", key("TVP 1", list = "PL"))
        assertEquals("pl|tvn 24", key("PL: TVN 24 HD"))
    }

    @Test fun `ocena jakosci - 4K najwyzej, kopia zapasowa nizej`() {
        val q = listOf("TVN 4K", "TVN ᵁᴴᴰ", "TVN FHD", "TVN HD", "TVN", "TVN SD").map { ChannelDupes.quality(it) }
        assertEquals(listOf(40, 40, 30, 20, 15, 10), q)
        assertTrue(ChannelDupes.quality("TVN HD HEVC") > ChannelDupes.quality("TVN HD"))
        assertTrue(ChannelDupes.quality("TVN FHD backup") < ChannelDupes.quality("TVN FHD"))
        assertEquals(40, ChannelDupes.quality("TVN", "http://x/live/u/p/tvn_2160p.ts"))
    }

    @Test fun `kolejnosc kopii - jakosc, potem pamiec 4 h`() {
        val hd = Copy("PL: TVN HD", "http://a/hd", "a", 0)
        val uhd = Copy("PL: TVN 4K", "http://b/4k", "b", 1)
        val fhd = Copy("PL: TVN FHD", "http://c/fhd", "c", 2)
        val mem = DupeMemory()
        val now = 1_000_000_000L
        assertEquals(listOf(uhd, fhd, hd), ChannelDupes.order(listOf(hd, uhd, fhd), mem, now))
        mem.onFailure(uhd.url, now)
        assertEquals("4K nie ruszył → na koniec", listOf(fhd, hd, uhd), ChannelDupes.order(listOf(hd, uhd, fhd), mem, now + 1000))
        mem.onSuccess(hd.url, 800, now + 2000)
        assertEquals("ostatnia wygrana pierwsza", hd, ChannelDupes.order(listOf(hd, uhd, fhd), mem, now + 3000).first())
        assertEquals("po 4 h znów jakość", uhd, ChannelDupes.order(listOf(hd, uhd, fhd), mem, now + DupeMemory.VALID_MS + 5000).first())
    }

    @Test fun `szybsza kopia przy tej samej jakosci`() {
        val a = Copy("PL: TVN FHD", "http://a/1", "a", 0)
        val b = Copy("PL: TVN FHD", "http://b/1", "b", 1)
        val mem = DupeMemory()
        mem.onSuccess(a.url, 2500, 1); mem.onSuccess(b.url, 400, 2)
        // obie „wygrały” — o kolejności decyduje czas do pierwszych danych
        assertEquals(b, ChannelDupes.order(listOf(a, b), mem, 1000).first())
    }

    @Test fun `pamiec - zapis i odczyt, stare wpisy wypadaja, bez adresow w tekscie`() {
        val mem = DupeMemory()
        mem.onSuccess("http://srv/live/login/haslo/1.ts", 700, 5)
        mem.onFailure("http://srv/live/login/haslo/2.ts", 10)
        val text = mem.encode(100)
        assertFalse("bez loginu i hasła", text.contains("haslo") || text.contains("login"))
        val back = DupeMemory.decode(text)
        assertTrue(back.recentWin("http://srv/live/login/haslo/1.ts", 100))
        assertTrue(back.recentFailure("http://srv/live/login/haslo/2.ts", 100))
        assertEquals(0, DupeMemory.decode(mem.encode(DupeMemory.VALID_MS + 100)).size())
        assertEquals(0, DupeMemory.decode("zepsute\tdane").size())
    }

    @Test fun `grupy w kolejnosci pierwszego wystapienia`() {
        val names = listOf("PL: TVN HD", "PL: Polsat", "Strong8K · PL: TVN 4K", "PA: TVN")
        val g = ChannelDupes.groups(names) { key(it) }
        assertEquals(listOf(listOf("PL: TVN HD", "Strong8K · PL: TVN 4K"), listOf("PL: Polsat"), listOf("PA: TVN")), g)
    }

    @Test fun `ćwierć miliona nazw - klucze szybko`() {
        val names = (0 until 250_000).map { "Strong8K · PL: Kanał ${it % 60_000} FHD" }
        val t0 = System.nanoTime()
        val groups = ChannelDupes.groups(names) { key(it) }
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertEquals(60_000, groups.size)
        assertTrue("trwało $ms ms", ms < 8_000)
    }

    @org.junit.Test
    fun lqCopyIsSameChannelAndWorseQuality() {
        val a = ChannelDupes.groupKey("AR: EL Fadjer Drama DZ")
        val b = ChannelDupes.groupKey("AR: EL Fadjer Drama DZ LQ")
        org.junit.Assert.assertEquals(a, b)
        org.junit.Assert.assertTrue(ChannelDupes.quality("EL Fadjer Drama DZ") > ChannelDupes.quality("EL Fadjer Drama DZ LQ"))
    }
}
