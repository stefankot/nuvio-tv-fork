package com.nuvio.tv.skin.niunia

import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories
import com.nuvio.tv.skin.niunia.livetv.NiuniaLiveFilter
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
}
