package com.nuvio.tv.skin.niunia.livetv

import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IPTV #82 (właściciel 26.11): wynik szukania „Gry małżeńskie” prowadził do emisji, której jeszcze nie nadano — strona
 * programu ma pokazać najpierw to, co da się obejrzeć (na żywo, archiwum od najnowszej), potem przyszłe emisje.
 */
class NiuniaProgrammeAiringsTest {
    private val h = 3_600_000L
    private val now = 100 * h
    private fun ch(name: String, key: String, archive: Boolean) =
        LiveTvChannel(id = name, name = name, streamUrl = "http://x/$name", guideKey = key, sourceId = if (archive) "arch" else "plain")
    private fun p(start: Long) = LiveTvProgramme("Gry małżeńskie", start, start + h)
    private fun same(c: LiveTvChannel) = c.name.substringBefore(" HD").substringBefore(" FHD")
    private val playable: (LiveTvChannel, LiveTvProgramme) -> Boolean = { c, _ -> c.sourceId == "arch" }

    @Test fun watchableFirstThenSoon() {
        val tvn7 = ch("TVN 7 HD", "k1", archive = true)
        val got = NiuniaProgrammeAirings.pick(mapOf("k1" to listOf(p(now - 30 * h), p(now - 5 * h), p(now + 20 * h), p(now - h / 2))), listOf(tvn7), now, ::same, playable)
        assertEquals(listOf(NiuniaAiringKind.LIVE, NiuniaAiringKind.ARCHIVE, NiuniaAiringKind.ARCHIVE, NiuniaAiringKind.SOON), got.map { it.kind })
        assertEquals(now - 5 * h, got[1].programme.startEpochMs) // najnowsze archiwum przed starszym
        assertEquals(now - 30 * h, got[2].programme.startEpochMs)
    }

    @Test fun pastWithoutArchiveIsDropped() {
        val plain = ch("Polsat", "k2", archive = false)
        val got = NiuniaProgrammeAirings.pick(mapOf("k2" to listOf(p(now - 5 * h), p(now + 2 * h))), listOf(plain), now, ::same, playable)
        assertEquals(listOf(NiuniaAiringKind.SOON), got.map { it.kind })
    }

    @Test fun copiesOfOneChannelShownOnceArchiveFromCopyWithArchive() {
        val hd = ch("TVN 7 HD", "k1", archive = false)
        val fhd = ch("TVN 7 FHD", "k3", archive = true)
        val other = ch("Inny", "k9", archive = true)
        val got = NiuniaProgrammeAirings.pick(mapOf("k1" to listOf(p(now - 5 * h), p(now + 3 * h)), "k3" to listOf(p(now - 5 * h), p(now + 3 * h))),
            listOf(hd, other, fhd), now, ::same, playable)
        assertEquals(2, got.size)
        assertEquals("TVN 7 FHD", got[0].channel.name) // archiwum tylko na kopii z archiwum
        assertEquals("TVN 7 HD", got[1].channel.name) // przyszła emisja na pierwszej kopii z listy
    }

    @Test fun keysOutsideListIgnored() {
        assertTrue(NiuniaProgrammeAirings.pick(mapOf("zz" to listOf(p(now))), listOf(ch("A", "k1", true)), now, ::same, playable).isEmpty())
    }

    /** JMGO uxr17: „Gry małżeńskie (196)” — strona pokazuje serial bez numeru, numer odcinka w wierszu emisji. */
    @Test fun showNameAndEpisode() {
        assertEquals("Gry małżeńskie", NiuniaProgrammeAirings.showName("Gry małżeńskie (196)"))
        assertEquals("196", NiuniaProgrammeAirings.episodeOf(LiveTvProgramme("Gry małżeńskie (196)", 0, 1)))
        assertEquals("Fakty", NiuniaProgrammeAirings.showName("Fakty"))
        assertEquals("S2E5", NiuniaProgrammeAirings.episodeOf(LiveTvProgramme("Fakty", 0, 1, episode = "S2E5")))
    }
}
