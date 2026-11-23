package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.plus.EpgProgram
import com.nuvio.tv.fork.tv.plus.GuideDbBridge
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvGuideRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 23.11 (właściciel: „zrób stress testing i testy jednostkowe”): przewodnik w bazie (`GuideDbBridge`, silnik NuvioPlusIPTV) —
 * kanał listy dostaje kanał przewodnika tymi samymi regułami co Reshaped (`LiveTvEpg`/`publish` w `startEpg`).
 */
class GuideDbLinksTest {
    private fun ch(key: String, name: String, tvgId: String? = null, source: String = "s1", logo: String? = "http://logo/$key.png") =
        LiveTvChannel(id = "$source/$key", name = name, streamUrl = "http://x.example/live/$key.ts", tvgId = tvgId, logoUrl = logo, sourceId = source, guideKey = key)

    private fun listing(url: String, index: Int, vararg channels: Triple<String, List<String>, String?>) =
        GuideDbBridge.GuideListing(url, index, "guide_$index", channels.toList())

    private fun pick(channels: List<LiveTvChannel>, listings: List<GuideDbBridge.GuideListing>, sourceLinks: Map<String, List<String>> = emptyMap(), added: Map<String, String> = emptyMap()) =
        GuideDbBridge.chooseFeeds(listings, listings.size, LiveTvGuideRequest.from(channels), channels.associate { it.guideKey to it.sourceId }, sourceLinks, added)

    @Test fun `id wygrywa z nazwa w jednym pliku`() {
        val chans = listOf(ch("k1", "TVN", tvgId = "tvn.pl"))
        val (best, _) = pick(chans, listOf(listing("http://a.example/epg.xml", 0,
            Triple("other.id", listOf("TVN"), null), Triple("tvn.pl", listOf("TVN Polska"), null))))
        assertEquals("tvn.pl", best["k1"]!!.xid)
        assertFalse(best["k1"]!!.byName)
    }

    @Test fun `dopasowanie po nazwie gdy brak id`() {
        val chans = listOf(ch("k1", "Polsat News"))
        val (best, _) = pick(chans, listOf(listing("http://a.example/epg.xml", 0, Triple("x1", listOf("Polsat News"), null))))
        assertEquals("x1", best["k1"]!!.xid)
        assertTrue(best["k1"]!!.byName)
    }

    @Test fun `przewodnik z id wygrywa z innym przewodnikiem z sama nazwa`() {
        val chans = listOf(ch("k1", "Eurosport 1", tvgId = "eurosport1.pl"))
        val (best, _) = pick(chans, listOf(
            listing("http://first.example/a.xml", 0, Triple("es1", listOf("Eurosport 1"), null)),
            listing("http://second.example/b.xml", 1, Triple("eurosport1.pl", listOf("Eurosport"), null)),
        ))
        assertEquals("guide_1", best["k1"]!!.src)
    }

    @Test fun `przewodnik dodany przez uzytkownika wygrywa`() {
        val chans = listOf(ch("k1", "TVP 1", tvgId = "tvp1"))
        val (best, _) = pick(chans, listOf(
            listing("http://first.example/a.xml", 0, Triple("tvp1", listOf("TVP 1"), null)),
            listing("http://mine.example/my.xml", 1, Triple("tvp1", listOf("TVP 1"), null)),
        ), added = mapOf("s1" to "http://mine.example/my.xml"))
        assertEquals("guide_1", best["k1"]!!.src)
    }

    @Test fun `kolejnosc przewodnikow zrodla`() {
        val chans = listOf(ch("k1", "TVN 24", tvgId = "tvn24"))
        val listings = listOf(
            listing("http://a.example/a.xml", 0, Triple("tvn24", listOf("TVN 24"), null)),
            listing("http://b.example/b.xml", 1, Triple("tvn24", listOf("TVN 24"), null)),
        )
        assertEquals("guide_1", pick(chans, listings, sourceLinks = mapOf("s1" to listOf("http://b.example/b.xml", "http://a.example/a.xml"))).first["k1"]!!.src)
        assertEquals("guide_0", pick(chans, listings).first["k1"]!!.src)
    }

    @Test fun `logo z przewodnika tylko dla kanalu bez logo`() {
        val chans = listOf(ch("k1", "A", tvgId = "a", logo = null), ch("k2", "B", tvgId = "b"))
        val (_, icons) = pick(chans, listOf(listing("http://a.example/a.xml", 0,
            Triple("a", listOf("A"), "http://icon/a.png"), Triple("b", listOf("B"), "http://icon/b.png"))))
        assertEquals("http://icon/a.png", icons["k1"])
        assertNull(icons["k2"])
    }

    @Test fun `kanal bez dopasowania nie ma przewodnika`() {
        val chans = listOf(ch("k1", "Zupełnie Inny"))
        val (best, _) = pick(chans, listOf(listing("http://a.example/a.xml", 0, Triple("x", listOf("Coś"), null))))
        assertNull(best["k1"])
    }

    @Test fun `wanted - id albo nazwa listy`() {
        val req = LiveTvGuideRequest.from(listOf(ch("k1", "Polsat", tvgId = "polsat.pl")))
        assertTrue(GuideDbBridge.wanted("POLSAT.PL", emptyList(), req))
        assertTrue(GuideDbBridge.wanted("zzz", listOf("Polsat"), req))
        assertFalse(GuideDbBridge.wanted("zzz", listOf("Inny"), req))
    }

    @Test fun `convert - duplikaty dziela jedna liste i teksty`() {
        val shared = listOf(EpgProgram(1_000, 2_000, "Fakty", icon = "http://i/1.jpg"), EpgProgram(2_000, 3_000, "Sport"))
        val out = GuideDbBridge.convert(mapOf("hd" to shared, "sd" to shared, "inny" to listOf(EpgProgram(1_000, 2_000, String(charArrayOf('F', 'a', 'k', 't', 'y'))))))
        assertSame(out["hd"], out["sd"])
        assertEquals("http://i/1.jpg", out["hd"]!![0].image)
        assertSame(out["hd"]!![0].title, out["inny"]!![0].title)
    }

    @Test fun `zaslepki programu`() {
        assertTrue(GuideDbBridge.isPlaceholder("No information"))
        assertFalse(GuideDbBridge.isPlaceholder("Fakty"))
    }

    /** Obciążenie: lista jak u właściciela (59 tys. kanałów) i 5 przewodników (30 tys. kanałów w największym). */
    @Test fun `obciazenie - 60 tys kanalow i 5 przewodnikow`() {
        val chans = (0 until 60_000).map { i -> ch("k$i", "Kanał $i HD", tvgId = if (i % 2 == 0) "id$i" else null, source = if (i < 30_000) "s1" else "s2") }
        val big = (0 until 30_000).map { i -> Triple(if (i % 2 == 0) "id$i" else "x$i", listOf("Kanał $i HD"), null as String?) }.toTypedArray()
        val listings = listOf(
            listing("http://flix.example/xmltv.php", 0, *big),
            listing("http://flix.example/m3u-epg.xml", 1, *big),
            listing("http://epg.ovh/pl.xml", 2, *(0 until 400).map { i -> Triple("id${i * 2}", listOf("Kanał ${i * 2} HD"), null as String?) }.toTypedArray()),
            listing("http://small.example/a.xml", 3, Triple("z", listOf("Z"), null)),
            listing("http://small.example/b.xml", 4, Triple("y", listOf("Y"), null)),
        )
        val t0 = System.currentTimeMillis()
        val (best, _) = pick(chans, listings)
        val ms = System.currentTimeMillis() - t0
        assertEquals(30_000, best.size)
        // epg.ovh jest pierwszy na liście właściciela (GuidePriority) — wygrywa dla swoich 400 kanałów.
        assertEquals("guide_2", best["k0"]!!.src)
        assertEquals("guide_0", best["k1"]!!.src)
        assertTrue("wybór trwał $ms ms", ms < 20_000)
    }
}
