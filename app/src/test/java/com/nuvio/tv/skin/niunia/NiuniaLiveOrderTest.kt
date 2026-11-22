package com.nuvio.tv.skin.niunia

import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories
import com.nuvio.tv.skin.niunia.livetv.NiuniaLiveFilter
import com.nuvio.tv.skin.niunia.livetv.NiuniaListMaps
import com.nuvio.tv.skin.niunia.livetv.NiuniaLiveOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** JMGO 22.11 (ANR przy 59 349 kanałach): kolejność, kategorie i numery kanałów Live TV Niuni liczone poza wątkiem UI. */
class NiuniaLiveOrderTest {
    private fun ch(name: String) = LiveTvChannel(id = name, name = name, streamUrl = "http://x/$name")
    private val cats = { _: List<LiveTvChannel> -> listOf(NiuniaLiveFilter.Category(ForgeEpgCategories.PREFIX + "sport", "Sport")) }

    @Test fun `polskie przed zagranicznymi i numery wg nowej kolejnosci`() {
        val list = listOf(ch("CNN International"), ch("TVP 1"), ch("Polsat"))
        val p = NiuniaLiveOrder.prepare(list, emptySet(), cats)
        assertEquals(listOf("TVP 1", "Polsat", "CNN International"), p.shown.map { it.name })
        assertEquals(1, p.numbers["http://x/TVP 1"])
        assertEquals(3, p.numbers["http://x/CNN International"])
        assertEquals(1, p.categories.size)
        assertTrue(p.sorted)
    }

    @Test fun `zapamietany wynik tylko dla tej samej listy i tych samych ukrytych grup`() {
        val list = listOf(ch("TVP 1"), ch("TVN"))
        val p = NiuniaLiveOrder.prepare(list, setOf("XXX"), cats)
        assertSame(p, NiuniaLiveOrder.cached(list, setOf("XXX")))
        assertNull("inna ukryta grupa", NiuniaLiveOrder.cached(list, emptySet()))
        assertNull("nowy obiekt listy z repozytorium = przeliczenie", NiuniaLiveOrder.cached(ArrayList(list), setOf("XXX")))
    }

    @Test fun `zanim sie policzy - kolejnosc listy bez kategorii, nie zapamietane`() {
        val list = listOf(ch("CNN International"), ch("TVP 1"))
        val u = NiuniaLiveOrder.unsorted(list, emptySet())
        assertEquals(listOf("CNN International", "TVP 1"), u.shown.map { it.name })
        assertEquals(2, u.numbers["http://x/TVP 1"])
        assertTrue(u.categories.isEmpty())
        assertFalse(u.sorted)
        assertTrue(u.isFor(list, emptySet()))
        assertNull(NiuniaLiveOrder.cached(list, emptySet()))
    }

    @Test fun `powtorzony adres dostaje pierwszy numer`() {
        val list = listOf(ch("TVP 1"), ch("TVP 1"), ch("Polsat"))
        val p = NiuniaLiveOrder.prepare(list, emptySet(), cats)
        assertEquals(1, p.numbers["http://x/TVP 1"])
        assertNotNull(p.numbers["http://x/Polsat"])
    }

    /** norm() bez wyrażenia regularnego: te same znaki co dawne `\p{Mn}`. */
    @Test fun `norm usuwa znaki diakrytyczne jak dawniej`() {
        assertEquals("zazolc gesla jazn", ForgeEpgCategories.norm("Zażółć gęślą jaźń"))
        assertEquals("cesky rozhlas", ForgeEpgCategories.norm("Český rozhlas"))
        assertEquals("tvp 1 hd", ForgeEpgCategories.norm("TVP 1 HD"))
    }

    @Test fun `duza lista przed policzeniem - bez numerow (0 = puste pole)`() {
        val big = (0..NiuniaLiveOrder.SYNC_LIMIT).map { ch("K$it") }
        assertTrue(NiuniaLiveOrder.unsorted(big, emptySet()).numbers.isEmpty())
    }

    /** Build171: mapy kafelków liczone raz na listę (ten sam obiekt), nie przy każdym odświeżeniu przewodnika. */
    @Test fun `mapy kanalow raz na liste - pierwszy po przewodniku, ostatni po adresie`() {
        val a = LiveTvChannel(id = "a", name = "A", streamUrl = "http://x/1", tvgId = "g")
        val b = LiveTvChannel(id = "b", name = "A", streamUrl = "http://x/1", tvgId = "g")
        val list = listOf(a, b)
        val g = NiuniaListMaps.byGuide(list)
        assertSame(g, NiuniaListMaps.byGuide(list))
        assertEquals(1, g.size); assertEquals("a", g.values.first().id)
        assertEquals("b", NiuniaListMaps.byUrl(list)["http://x/1"]?.id)
        assertFalse("nowa lista = nowa mapa", g === NiuniaListMaps.byGuide(ArrayList(list)))
    }
}
