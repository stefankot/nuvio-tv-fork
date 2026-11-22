package com.nuvio.tv.skin.kicia.livetv

import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 22.11: kanał w kilku kategoriach, polskie przed zagranicznymi. */
class EpgChannelOrderTest {
    private fun ch(name: String) = LiveTvChannel(id = name, name = name, streamUrl = "http://x/$name")

    @Test fun `CNN w informacjach i w zagranicznych`() {
        val p = ForgeEpgCategories.channelPlaces(ch("CNN International")).map { it.branch }
        assertTrue(p.toString(), "informacje" in p)
        assertTrue(p.toString(), "zagraniczne" in p)
        assertTrue(ForgeEpgCategories.isForeign(ch("CNN International")))
        assertFalse(ForgeEpgCategories.isForeign(ch("TVP 1")))
    }

    @Test fun `polskie najpierw, kolejnosc w grupach bez zmian`() {
        val list = listOf(ch("CNN International"), ch("TVP 1"), ch("RTL"), ch("Polsat"), ch("TVN 24"))
        assertEquals(listOf("TVP 1", "Polsat", "TVN 24", "CNN International", "RTL"), ForgeEpgCategories.polishFirst(list).map { it.name })
    }

    @Test fun `filtr kategorii widzi kanal w obu galeziach`() {
        val list = listOf(ch("CNN International"), ch("TVP 1"))
        val foreign = ForgeEpgCategories.filter(list, ForgeEpgCategories.PREFIX + "zagraniczne", emptySet(), "")!!.map { it.name }
        val news = ForgeEpgCategories.filter(list, ForgeEpgCategories.PREFIX + "informacje", emptySet(), "")!!.map { it.name }
        assertTrue(foreign.contains("CNN International"))
        assertTrue(news.contains("CNN International"))
    }

    /** JMGO 22.11: reguły kategorii z jednym Matcherem na wątek — ten sam wynik w innym wątku i przy powtórzeniu. */
    @Test fun `kategorie takie same w innym watku i przy powtorzeniu`() {
        val names = listOf("CNN International", "TVP 1", "Polsat Sport", "Eleven Sports 3 HD", "Cartoon Network", "Discovery Channel", "BBC World News")
        val here = names.map { ForgeEpgCategories.channelPlaces(ch(it)) }
        var there: List<Any>? = null
        // Wielkie litery: inny klucz pamięci (liczone od nowa, w nowym wątku), ta sama nazwa po norm().
        val t = Thread { there = names.map { ForgeEpgCategories.channelPlaces(ch(it.uppercase())) } }
        t.start(); t.join()
        assertEquals(here, there)
        assertEquals(here, names.map { ForgeEpgCategories.channelPlaces(ch(it)) })
    }
}
