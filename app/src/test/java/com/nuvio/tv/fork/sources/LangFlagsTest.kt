package com.nuvio.tv.fork.sources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Fork 22.11: flagi języków w opisie (TorrentClaw) i opisy w kilku liniach w regułach zestawów. */
class LangFlagsTest {
    private val presets = SourcePresets.parse(File("src/main/assets/fork/source_presets.json").readText()).associateBy { it.id }
    private fun tier(id: String, raw: String) = SourcePresets.tierOf(presets.getValue(id), LangFlags.presetText(raw))

    // Prawdziwe opisy TorrentClaw (22.11, „W głowie się nie mieści 2”), ścieżki potwierdzone ffprobe.
    private val plAudio = "⚡️ INSTANT 4K · Web-dl ⭐ 🔵 75/100 · TB\n✅ TrueSpec Verified\n🎞️ hevc · DV+HDR10 · 10-bit\n🔊 ac3 · 5.1 · 🇵🇱🇺🇸\n💬 🇵🇱🇺🇸\n💾 17.1 GB\n🏷️ K83"
    private val plSubsOnly = "⚡️ INSTANT 4K · Bluray 🟢 94/100 · TB\n✅ TrueSpec Verified\n🎞️ x265 · DV · 10-bit\n🔊 truehd · 7.1 · 🇺🇸🇪🇸🇫🇷\n💬 🇵🇱🇸🇦🇧🇬🇨🇿 +24\n💾 13.2 GB"

    @Test fun `polski dzwiek z flag a nie z napisow`() {
        assertTrue(LangFlags.polishAudio(plAudio))
        assertTrue(LangFlags.polishSubs(plAudio))
        assertFalse(LangFlags.polishAudio(plSubsOnly))
        assertTrue(LangFlags.polishSubs(plSubsOnly))
    }

    @Test fun `flagi liczone parami`() {
        assertFalse(LangFlags.polishAudio("X 1080p\n🔊 aac · 2.0 · 🇯🇵🇱🇹\n💬 🇬🇧"))
        assertTrue(LangFlags.polishAudio("X 1080p\n🔊 aac · 2.0 · 🇺🇸🇵🇱\n💬 🇬🇧"))
    }

    @Test fun `szczebel zestawu z flag`() {
        assertEquals(0, tier("pl_always", plAudio))
        assertEquals(0, tier("pl_if_good", plAudio))
        assertEquals(4, tier("pl_always", plSubsOnly))
        assertEquals(2, tier("pl_if_good", plSubsOnly))
    }

    @Test fun `opis w kilku liniach trafia w szczebel`() {
        assertNull(SourcePresets.tierOf(presets.getValue("pl_always"), "Torrentio\n4k DV Film.2024.PL.2160p.WEB-DL\n👤 12 💾 20 GB"))
        assertEquals(0, tier("pl_always", "Torrentio\n4k DV Film.2024.PL.2160p.WEB-DL\n👤 12 💾 20 GB"))
        assertEquals(1, tier("pl_always", "Torrentio\n1080p Film.2024.1080p.Lektor.PL\n👤 3"))
        assertEquals(5, tier("pl_always", "Torrentio\n1080p Film.2024.1080p.PLSUB\n👤 3"))
    }
}
