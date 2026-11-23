package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.index.DiskMemo
import com.nuvio.tv.fork.tv.index.TvTextIndex
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Właściciel 22.11: indeksy liczone tylko przy większej zmianie — po restarcie tylko odczyt z dysku. */
class DiskMemoTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun `zapis i odczyt po restarcie, tabulatory w tekscie nie psuja pliku`() {
        val dir = tmp.newFolder()
        val a = DiskMemo("names", "1").apply { load(dir) }
        a.put("PL: TVN HD", "pl: tvn hd")
        a.put("zły\tklucz", "wartość\nz nową linią")
        assertTrue(a.saveNow())
        assertFalse("bez zmian — bez zapisu", a.saveNow())
        val b = DiskMemo("names", "1").apply { load(dir) }
        assertEquals("pl: tvn hd", b["PL: TVN HD"])
        assertEquals("wartość z nową linią", b["zły klucz"])
        assertTrue(b.awaitLoaded(100))
    }

    @Test fun `nowa wersja sposobu liczenia - stary plik usuniety, liczone od nowa`() {
        val dir = tmp.newFolder()
        DiskMemo("places", "aaa").apply { load(dir); put("TVN", "ogolne/"); saveNow() }
        val nowa = DiskMemo("places", "bbb").apply { load(dir) }
        assertEquals(null, nowa["TVN"])
        assertFalse(File(dir, "places_vaaa.tsv.gz").exists())
    }

    @Test fun `powtarzajace sie wartosci jako jeden obiekt`() {
        val dir = tmp.newFolder()
        val m = DiskMemo("country", "1", poolValues = true).apply { load(dir) }
        m.put("A", String(charArrayOf('p', 'l'))); m.put("B", String(charArrayOf('p', 'l')))
        assertSame(m["A"], m["B"])
    }

    @Test fun `zbedne nazwy usuwane tylko przy wiekszej zmianie`() {
        val dir = tmp.newFolder()
        val m = DiskMemo("names", "1").apply { load(dir) }
        repeat(10_000) { m.put("k$it", "v") }
        val keep = (0 until 9_000).mapTo(HashSet()) { "k$it" }
        assertEquals("1000 zbędnych z 10000 — mała zmiana, bez usuwania", 0, m.retainOnly(keep))
        val few = (0 until 2_000).mapTo(HashSet()) { "k$it" }
        assertEquals(8_000, m.retainOnly(few))
        assertEquals(2_000, m.map.size)
        assertTrue(m.saveNow())
        assertEquals(2_000, DiskMemo("names", "1").apply { load(dir) }.map.size)
    }

    @Test fun `zepsuty plik - pusta pamiec zamiast bledu`() {
        val dir = tmp.newFolder()
        File(dir, "names_v1.tsv.gz").writeText("to nie gzip")
        val m = DiskMemo("names", "1").apply { load(dir) }
        assertEquals(0, m.map.size)
    }

    private fun p(title: String, start: Long) = LiveTvProgramme(title, start, start + 1_800_000)

    @Test fun `indeks po restarcie - niezmienione kanaly bez liczenia, spis tytulow z dysku`() {
        TvTextIndex.clearForTests()
        val ch = listOf(LiveTvChannel(id = "1", name = "TVN", streamUrl = "u", guideKey = "tvn"))
        TvTextIndex.update(ch, mapOf("tvn" to listOf(p("Fakty", 0), p("Uwaga!", 1))))
        assertEquals(1, TvTextIndex.titles("fakty").size)
        TvTextIndex.forgetMemoryForTests()
        assertTrue("po restarcie przed odczytem pusto", TvTextIndex.titles("fakty").isEmpty())
        TvTextIndex.restoreTitles()
        assertEquals("spis z dysku", setOf("tvn"), TvTextIndex.titles("fakty").single().second)
        // Ta sama treść w nowych obiektach (odczyt przewodnika po restarcie) — skrót ten sam.
        val again = listOf(p("Fakty", 0), p("Uwaga!", 1))
        assertEquals(TvTextIndex.fingerprint(again), TvTextIndex.fingerprint(listOf(p("Fakty", 0), p("Uwaga!", 1))))
        assertNotEquals(TvTextIndex.fingerprint(again), TvTextIndex.fingerprint(listOf(p("Fakty", 0), p("Uwaga!", 2))))
        TvTextIndex.update(ch, mapOf("tvn" to again))
        assertTrue(TvTextIndex.ready())
        assertEquals(setOf("tvn"), TvTextIndex.titles("uwaga").single().second)
    }
}
