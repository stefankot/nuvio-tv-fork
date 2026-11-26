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
        assertEquals(2, tier("pl_always", "Film 2023 1080p WEB-DL PL Lektor"))
        assertEquals(0, tier("pl_always", "Film.2023.2160p.UHD.DUBBING.PL"))
        assertEquals(3, tier("pl_always", "Film 720p BluRay Dubbing"))
        assertEquals(4, tier("pl_always", "Film 480p POLISH"))
        assertEquals(11, tier("pl_always", "Film 1080p WEB-DL English"))
        assertEquals(2, tier("pl_always", "Film 1080p MULTI PL"))
        assertEquals(2, tier("pl_always", "Film 1080p PL"))
    }

    @Test fun `PL subs nie jest polskim dzwiekiem`() {
        assertEquals(7, tier("pl_always", "Film 1080p WEB-DL PL subs"))
        assertEquals(7, tier("pl_always", "Film 1080p WEB napisy PL"))
        assertEquals(7, tier("pl_always", "Film 1080p NF WEB-DL ENG + PL Subtitles"))
    }

    @Test fun `cam i ts odpadaja we wszystkich zestawach`() {
        listOf("Film 1080p CAM PL", "Film HDCAM", "Film.TS.2023", "Film 1080p TELESYNC Lektor").forEach { n ->
            presets.keys.forEach { id -> assertNull("$id $n", tier(id, n)) }
        }
    }

    @Test fun `po polsku przy dobrej jakosci`() {
        assertEquals(6, tier("pl_if_good", "Film 1080p WEB-DL Lektor PL"))
        assertEquals(0, tier("pl_if_good", "Film 4K HDR Dubbing PL"))
        assertEquals(9, tier("pl_if_good", "Film 720p Lektor PL")) // 22.11 „Jakość obrazu ważniejsza”: najpierw rozdzielczość, w niej polski dźwięk
        assertEquals(4, tier("pl_if_good", "Film 2160p English"))
        assertEquals(8, tier("pl_if_good", "Film 1080p English"))
        assertEquals(7, tier("pl_if_good", "Film 1080p PL subs"))
        assertEquals(11, tier("pl_if_good", "Film 720p English"))
    }

    @Test fun `oryginal z napisami to sama jakosc`() {
        assertEquals(0, tier("original_subs", "Film 2160p PL Lektor")); assertEquals(2, tier("original_subs", "Film 1080p English"))
        assertEquals(3, tier("original_subs", "Film 720p")); assertEquals(4, tier("original_subs", "Film SD"))
    }

    @Test fun `rozpoznawanie wlasnych regul`() {
        val list = presets.values.toList()
        assertEquals("pl_always", SourcePresets.matching(presets.getValue("pl_always").rules, list))
        assertEquals(SourcePresets.CUSTOM, SourcePresets.matching(listOf("1080p"), list))
        assertNull(SourcePresets.matching(emptyList(), list))
        assertNotNull(presets["original_subs"])
    }

    // Właściciel 22.11: reguły dla natywnego Regex Nuvio — tekst źródła to nazwa dodatku, nazwa, opis w kilku liniach, plik i adres.
    private val tcPl = "TorrentClaw (PL, 720p) ⚡️ INSTANT 4K · Web-dl ⭐ 🔵 75/100 · TB\n✅ TrueSpec Verified\n🎞️ hevc · DV+HDR10 · 10-bit\n🔊 ac3 · 5.1 · 🇵🇱🇺🇸\n💬 🇵🇱🇺🇸\n💾 17.1 GB\n🏷️ K83   [DEVIL-TORRENTS.PL] Inside.Out.2.2024.MULTi.2160p.WEB-DL   https://torrentclaw.com/x/ts/sub"
    private val tcSubsOnly = "TorrentClaw (PL, 720p) ⚡️ INSTANT 4K · Bluray 🟢 94/100 · TB\n✅ TrueSpec Verified\n🔊 truehd · 7.1 · 🇺🇸🇪🇸🇫🇷\n💬 🇵🇱🇸🇦🇧🇬🇨🇿 +24\n💾 13.2 GB   Inside Out 2 (2024) (2160p BluRay) [QxR]   https://torrentclaw.com/x"

    @Test fun `flagi dzwieku i napisow TorrentClaw`() {
        assertEquals(0, tier("pl_always", tcPl))
        assertEquals(5, tier("pl_always", tcSubsOnly))
        assertEquals(2, tier("pl_if_good", tcSubsOnly))
        assertEquals(0, tier("pl_if_good", tcPl))
        assertEquals(11, tier("pl_always", "X 1080p\n🔊 aac · 2.0 · 🇯🇵🇱🇹\n💬 🇬🇧 https://x")) // Japonia + Litwa to nie 🇵🇱
        assertEquals(2, tier("pl_always", "X 1080p\n🗣️ 🇬🇧🇵🇱 https://x"))
        assertEquals(7, tier("pl_always", "X 1080p Film.2024.PL.1080p\n🔊 ac3 · 🇺🇸\n💬 🇵🇱 https://x")) // flagi dźwięku mają pierwszeństwo przed nazwą
    }

    @Test fun `opis w kilku liniach`() {
        assertEquals(0, tier("pl_always", "Torrentio 4k DV Film.2024.PL.2160p.WEB-DL\n👤 12 💾 20 GB ⚙️ X https://x"))
        assertEquals(2, tier("pl_always", "Torrentio 1080p Film.2024.1080p.Lektor.PL\n👤 3 https://x"))
        assertEquals(7, tier("pl_always", "Torrentio 1080p Film.2024.1080p.PLSUB\n👤 3 https://x"))
    }

    @Test fun `nazwa dodatku domena i adres nie udaja polskiego`() {
        assertEquals(11, tier("pl_always", "TorrentClaw (PL, 720p) ⚡️ INSTANT 1080p · Web-dl\n🔊 eac3 · 5.1\n💾 3 GB Film.2024.1080p.WEB-DL.ENG https://torrentclaw.com/x"))
        assertEquals(11, tier("pl_always", "Addon 1080p [DEVIL-TORRENTS.PL] Film.2024.ENG.1080p https://x"))
        assertEquals(2, tier("pl_always", "Addon Film [PL] 1080p https://x"))
        assertEquals(11, tier("pl_always", "Addon Film.2024.1080p.ENG https://cdn/x/video.ts")) // „ts” w adresie to nie TELESYNC
    }

    // 22.11 (gałąź regex-presets-1122): „hd” nie łapie „DTS-HD”, źródła do ściągania (torrent bez debrid, ⬇️/⏳) za gotowymi.
    private val hash = " 7714a4a5c3d2e1f0a9b8c7d6e5f4a3b2c1d0e9f8"
    @Test fun `DTS-HD to nie 720p, do sciagania na koncu`() {
        assertEquals(11, tier("pl_always", "X 1080p DTS-HD MA English"))
        assertEquals(14, tier("pl_if_good", "X DTS-HD MA English"))
        assertEquals(14, tier("pl_always", "TorrentClaw (PL, 720p) 🌐 4K · Bluray ⭐\n🔊 ac3 · 🇵🇱" + hash))
        assertEquals(16, tier("pl_always", "TorrentClaw (PL, 720p) 🌐 4K · Bluray\n🔊 ac3 · 🇺🇸" + hash))
        assertEquals(15, tier("pl_if_good", "TorrentClaw (PL, 720p) ⬇️ Download 4K · Bluray\n🔊 ac3 · 🇵🇱 https://x"))
        assertEquals(5, tier("original_subs", "TorrentClaw 🌐 1080p" + hash))
    }

    // 26.11 (test VOD na JMGO): 4K Remux 72–77 Mb/s doczytywał się 10× na minutę, 4K BluRay 35 Mb/s grał płynnie —
    // w obrębie 4K najpierw plik bez REMUX; „Click to verify & play” (Flix-Streams SmartPlay: „4K”, a gra 1080p) na koniec.
    @Test fun `4K bez REMUX przed REMUX, SmartPlay na koncu`() {
        assertEquals(0, tier("original_subs", "SELection 4K 〈Bluray〉 Oppenheimer (2023) HEVC DV · HDR10 23.3 GB"))
        assertEquals(1, tier("original_subs", "SELection 4K 〈Remux〉 Oppenheimer.2023.2160p.UHD.BDRemux.DV.HEVC"))
        assertEquals(1, tier("original_subs", "Torrentio 4k Film.2024.2160p.BluRay.REMUX.HEVC https://x"))
        assertEquals(2, tier("original_subs", "SELection 1080P 〈Remux〉 Film.2024.1080p.BluRay.REMUX"))
        assertEquals(5, tier("original_subs", "Flix-Streams ⚡ SmartPlay — Auto Verified\n4K • Click to verify & play https://x"))
        assertEquals(1, tier("pl_always", "Film.2023.2160p.UHD.BDRemux.DUBBING.PL"))
        assertEquals(5, tier("pl_if_good", "Film 2160p REMUX English"))
        assertEquals(16, tier("pl_if_good", "Flix-Streams ⚡ SmartPlay\n4K • Click to verify & play https://x"))
    }
}
