package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.index.DiskMemo
import com.nuvio.tv.fork.tv.ram.TvSnapshot
import com.nuvio.tv.reshaped.livetv.LiveTvCatchup
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** 23.11 (TvRam): telewizja w RAM tylko na jej ekranie — próbka startowa i zwalnianie pamięci indeksów. */
class TvSnapshotTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun ch(i: Int) = LiveTvChannel(
        id = "s/$i", name = "Kanał $i", streamUrl = "http://x.example/live/$i.ts", tvgId = "k$i", group = "PL",
        headers = mapOf("User-Agent" to "a"), sourceId = "s", hideKey = i.toLong(), guideKey = "g$i",
        catchup = if (i == 1) LiveTvCatchup(LiveTvCatchup.Kind.Xtream, 3) else null,
    )

    @Test fun `probka zapisana i wczytana po restarcie, tylko najblizsze programy`() {
        val f = File(tmp.newFolder(), "snapshot_v1.bin.gz")
        TvSnapshot.loadFrom(f)
        assertNull(TvSnapshot.data)
        val all = (1..500).map(::ch)
        val now = System.currentTimeMillis()
        val sched = mapOf(
            "g1" to listOf(
                LiveTvProgramme("Stary", now - 5 * 3_600_000, now - 4 * 3_600_000),
                LiveTvProgramme("Teraz", now - 600_000, now + 600_000, description = "opis nie trafia do próbki", episode = "S1E2"),
                LiveTvProgramme("Jutro", now + 30 * 3_600_000, now + 31 * 3_600_000),
            ),
        )
        TvSnapshot.save(all.take(300), all, setOf(all[399].streamUrl), listOf("g450")) { sched[it].orEmpty() }
        TvSnapshot.loadFrom(f)
        val d = TvSnapshot.data!!
        // 23.11 etap 1b: limit 400 (pierwsza strona + kategorie + ulubione) — tu 300 z ekranu + ulubiony + „Dla Ciebie”.
        assertEquals(minOf(TvSnapshot.MAX_CHANNELS, 302), d.channels.size)
        assertEquals("pierwsza strona ekranu na początku", "Kanał 1", d.channels.first().name)
        assertTrue("ulubiony spoza pierwszej strony", d.channels.any { it.name == "Kanał 400" })
        assertTrue("kanał z „Dla Ciebie”", d.channels.any { it.name == "Kanał 450" })
        assertEquals(listOf("Teraz"), d.programmes["g1"]!!.map { it.title })
        assertEquals("S1E2", d.programmes["g1"]!!.single().episode)
        assertEquals("Teraz", d.currentProgrammes(now)["g1"]?.title)
        assertEquals(ch(1), d.channels.first())
        assertEquals(setOf(all[399].streamUrl), d.favoriteUrls)
    }

    /** JMGO 23.11 (pomiar 2): próbka z programem dla 152 kanałów nadpisana zaraz po starcie próbką „przewodnik dla 0”. */
    @Test fun `probka nie nadpisuje sie pusta, gdy przewodnik jeszcze sie wczytuje`() {
        val f = File(tmp.newFolder(), "snapshot_v2.bin.gz")
        TvSnapshot.loadFrom(f)
        val all = (1..200).map(::ch)
        val now = System.currentTimeMillis()
        val full = all.associate { it.guideKey to listOf(LiveTvProgramme("Program ${it.guideKey}", now - 600_000, now + 600_000)) }
        TvSnapshot.save(all.take(100), all, emptySet(), emptyList()) { full[it].orEmpty() }
        assertEquals(100, TvSnapshot.data!!.programmes.size)
        // Inna pierwsza strona (bez blokady „co 10 min”), ale przewodnik pusty — zapis pominięty, w pamięci i na dysku stara próbka.
        TvSnapshot.save(all.drop(100), all, emptySet(), emptyList()) { emptyList() }
        assertEquals(100, TvSnapshot.data!!.programmes.size)
        TvSnapshot.loadFrom(f)
        assertEquals(100, TvSnapshot.data!!.programmes.size)
    }

    @Test fun `pamiec indeksu zwolniona i wczytana znowu z dysku`() {
        val dir = tmp.newFolder()
        val m = DiskMemo("names", "1").apply { load(dir) }
        m.put("TVN", "tvn")
        m.unload()
        assertEquals(0, m.map.size)
        m.load(dir)
        assertEquals("niezapisana zmiana zapisana przy zwolnieniu", "tvn", m["TVN"])
    }
}
