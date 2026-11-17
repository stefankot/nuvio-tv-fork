package com.nuvio.tv.fork.sources

import com.nuvio.tv.domain.model.AddonStreams
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBadge
import com.nuvio.tv.domain.model.StreamBehaviorHints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ReadyPickTest {
    private fun stream(
        url: String? = "https://x/a.mkv", infoHash: String? = null, quality: String? = null, value: Int = -1,
        size: Long? = null, badges: List<String> = emptyList(),
    ) = Stream(
        name = null, title = null, description = null, url = url, ytId = null, infoHash = infoHash, fileIdx = null, externalUrl = null,
        behaviorHints = size?.let { StreamBehaviorHints(null, null, null, null, videoSize = it) }, addonName = "A", addonLogo = null,
        quality = quality, qualityValue = value, badges = badges.map { StreamBadge(it) },
    )

    private fun groups(vararg s: Stream) = listOf(AddonStreams("A", null, s.toList()))

    @Test
    fun `nothing found gives nothing`() {
        assertNull(ReadyPick.info(emptyList()))
        assertNull(ReadyPick.info(groups()))
    }

    @Test
    fun `a stream that plays now beats a better torrent`() {
        val direct = stream(quality = "1080p", value = 1080)
        val torrent = stream(url = null, infoHash = "abc", quality = "4K", value = 2160)
        val info = ReadyPick.info(groups(torrent, direct))!!
        assertEquals("1080p", info.quality)
        assertTrue(info.ready)
    }

    @Test
    fun `among playable streams quality wins, then size`() {
        val a = stream(quality = "1080p", value = 1080, size = 9_000_000_000)
        val b = stream(quality = "4K", value = 2160, size = 2_000_000_000)
        val c = stream(quality = "4K", value = 2160, size = 5_000_000_000, badges = listOf("HDR10", "Atmos", "REMUX", "DV", "extra"))
        val info = ReadyPick.info(groups(a, b, c))!!
        assertEquals("4K", info.quality)
        assertEquals(5_000_000_000, info.sizeBytes)
        assertEquals(listOf("HDR10", "Atmos", "REMUX", "DV"), info.tags)
    }

    @Test
    fun `a torrent only result is found but not ready`() {
        val info = ReadyPick.info(groups(stream(url = null, infoHash = "abc", quality = "720p", value = 720)))!!
        assertFalse(info.ready)
    }

    @Test
    fun `size text`() {
        assertEquals("70.8 GB", ReadyPick.sizeText(76_000_000_000, Locale.US))
        assertEquals("850 MB", ReadyPick.sizeText(891_289_600, Locale.US))
    }
}
