package com.nuvio.tv.skin.niunia.livetv

import com.nuvio.tv.fork.tv.country.TvCountry
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 23.11 etap 1c (właściciel: „to przecież można policzyć raz i wgrywać w tej postaci już potem”): kolejność i kategorie listy
 * kanałów Niuni zapisane na dysku — ta sama lista bez liczenia, zmieniona lista: ostatnia kolejność od razu.
 */
class NiuniaLiveOrderDiskTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun ch(i: Int) = LiveTvChannel(id = "s/$i", name = "Kanał $i", streamUrl = "http://x.example/live/$i.ts", guideKey = "g$i", sourceId = "s")
    private val mode = TvCountry.Mode(prefer = true, only = false, country = "pl")
    private val cats = listOf(NiuniaLiveFilter.Category("sport", "Sport"), NiuniaLiveFilter.Category("news", "Informacje"))

    private fun prepared(input: List<LiveTvChannel>) =
        NiuniaLiveOrder.Prepared(input, emptySet(), input.reversed(), cats, emptyMap(), sorted = true, mode = mode)

    private fun file() = File(tmp.newFolder(), "live_order_v1.bin.gz")

    @Test fun `ta sama lista - gotowe bez liczenia`() {
        val input = (0 until 5_000).map(::ch)
        val f = file()
        NiuniaLiveOrder.saveTo(f, prepared(input))
        val back = NiuniaLiveOrder.readFrom(f, input, emptySet(), mode)
        assertNotNull(back)
        assertTrue(back!!.sorted)
        assertEquals(input.reversed().map { it.streamUrl }, back.shown.map { it.streamUrl })
        assertEquals(listOf("sport", "news"), back.categories.map { it.key })
        assertEquals("Informacje", back.categories[1].name)
        assertEquals(1, back.numbers[input.last().streamUrl])
    }

    @Test fun `zmieniona lista - ostatnia kolejnosc, nowe na koncu, do przeliczenia`() {
        val input = (0 until 5_000).map(::ch)
        val f = file()
        NiuniaLiveOrder.saveTo(f, prepared(input))
        val changed = input.filter { it.streamUrl != input[4_999].streamUrl } + ch(9_999)
        val back = NiuniaLiveOrder.readFrom(f, changed, emptySet(), mode)!!
        assertFalse(back.sorted)
        assertEquals(changed.size, back.shown.size)
        assertEquals(input[4_998].streamUrl, back.shown.first().streamUrl)
        assertEquals(ch(9_999).streamUrl, back.shown.last().streamUrl)
        assertEquals(changed.size, back.shown.map { it.streamUrl }.toSet().size)
    }

    @Test fun `inny tryb kraju albo ukryte grupy - brak zapisu`() {
        val input = (0 until 5_000).map(::ch)
        val f = file()
        NiuniaLiveOrder.saveTo(f, prepared(input))
        assertNull(NiuniaLiveOrder.readFrom(f, input, emptySet(), mode.copy(country = "de")))
        assertNull(NiuniaLiveOrder.readFrom(f, input, setOf("Filmy"), mode))
    }

    @Test fun `mala lista i nieposortowany wynik nie sa zapisywane`() {
        val f = file()
        NiuniaLiveOrder.saveTo(f, prepared((0 until 100).map(::ch)))
        assertFalse(f.exists())
        val big = (0 until 5_000).map(::ch)
        NiuniaLiveOrder.saveTo(f, NiuniaLiveOrder.Prepared(big, emptySet(), big, emptyList(), emptyMap(), sorted = false, mode = mode))
        assertFalse(f.exists())
    }

    @Test fun `uszkodzony plik - brak wyniku zamiast bledu`() {
        val f = file()
        f.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        assertNull(NiuniaLiveOrder.readFrom(f, (0 until 5_000).map(::ch), emptySet(), mode))
    }

    /** Obciążenie: lista właściciela (59 323 kanały) — zapis i odczyt, poprawność kolejności. */
    @Test fun `obciazenie - 60 tys kanalow`() {
        val input = (0 until 60_000).map(::ch)
        val f = file()
        val t0 = System.currentTimeMillis()
        NiuniaLiveOrder.saveTo(f, prepared(input))
        val t1 = System.currentTimeMillis()
        val back = NiuniaLiveOrder.readFrom(f, input, emptySet(), mode)!!
        val t2 = System.currentTimeMillis()
        assertTrue(back.sorted)
        assertEquals(60_000, back.shown.size)
        assertEquals(input.last().streamUrl, back.shown.first().streamUrl)
        assertTrue("zapis ${t1 - t0} ms", t1 - t0 < 10_000)
        assertTrue("odczyt ${t2 - t1} ms", t2 - t1 < 10_000)
    }
}
