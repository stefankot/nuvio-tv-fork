package com.nuvio.tv.fork.sources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class SourcePresetsTest {
    private val presets = SourcePresets.parse(File("src/main/assets/fork/source_presets.json").readText()).associateBy { it.id }
    private fun tier(id: String, name: String) = SourcePresets.tierOf(presets.getValue(id), name)

    @Test fun `trzy zestawy po maks 20 regul`() {
        assertEquals(3, presets.size)
        presets.values.forEach { assertEquals(true, it.rules.size in 1..20) }
    }

    @Test fun `zawsze po polsku`() {
        assertEquals(1, tier("pl_always", "Film 2023 1080p WEB-DL PL Lektor"))
        assertEquals(0, tier("pl_always", "Film.2023.2160p.UHD.DUBBING.PL"))
        assertEquals(2, tier("pl_always", "Film 720p BluRay Dubbing"))
        assertEquals(3, tier("pl_always", "Film 480p POLISH"))
        assertEquals(5, tier("pl_always", "Film 1080p WEB-DL English"))
        assertEquals(1, tier("pl_always", "Film 1080p MULTI PL"))
        assertEquals(1, tier("pl_always", "Film 1080p PL"))
    }

    @Test fun `PL subs nie jest polskim dzwiekiem`() {
        assertEquals(5, tier("pl_always", "Film 1080p WEB-DL PL subs"))
        assertEquals(5, tier("pl_always", "Film 1080p WEB napisy PL"))
        assertEquals(5, tier("pl_always", "Film 1080p NF WEB-DL ENG + PL Subtitles"))
    }

    @Test fun `cam i ts odpadaja we wszystkich zestawach`() {
        listOf("Film 1080p CAM PL", "Film HDCAM", "Film.TS.2023", "Film 1080p TELESYNC Lektor").forEach { n ->
            presets.keys.forEach { id -> assertNull("$id $n", tier(id, n)) }
        }
    }

    @Test fun `po polsku przy dobrej jakosci`() {
        assertEquals(1, tier("pl_if_good", "Film 1080p WEB-DL Lektor PL"))
        assertEquals(0, tier("pl_if_good", "Film 4K HDR Dubbing PL"))
        assertEquals(4, tier("pl_if_good", "Film 720p Lektor PL")) // 720p z lektorem nie jest „dobrą jakością”: trafia do zwykłego 720p
        assertEquals(2, tier("pl_if_good", "Film 2160p English"))
        assertEquals(3, tier("pl_if_good", "Film 1080p English"))
        assertEquals(4, tier("pl_if_good", "Film 720p English"))
    }

    @Test fun `oryginal z napisami to sama jakosc`() {
        assertEquals(0, tier("original_subs", "Film 2160p PL Lektor")); assertEquals(1, tier("original_subs", "Film 1080p English"))
        assertEquals(2, tier("original_subs", "Film 720p")); assertEquals(3, tier("original_subs", "Film SD"))
    }

    @Test fun `rozpoznawanie wlasnych regul`() {
        val list = presets.values.toList()
        assertEquals("pl_always", SourcePresets.matching(presets.getValue("pl_always").rules, list))
        assertEquals(SourcePresets.CUSTOM, SourcePresets.matching(listOf("1080p"), list))
        assertNull(SourcePresets.matching(emptyList(), list))
        assertNotNull(presets["original_subs"])
    }
}
