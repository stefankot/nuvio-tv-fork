package com.nuvio.tv.fork.sources

import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamDebridCacheState
import com.nuvio.tv.domain.model.StreamDebridCacheStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy 22.11: obcy dubbing na koniec, gotowe przed niegotowymi, AVI/WMV w mpv. */
class SourceTiersTest {
    private fun s(name: String? = null, title: String? = null, url: String? = "https://x/a.mkv", infoHash: String? = null,
                  cache: StreamDebridCacheState? = null) = Stream(
        name = name, title = title, description = null, url = url, ytId = null, infoHash = infoHash, fileIdx = null, externalUrl = null,
        behaviorHints = null, addonName = "A", addonLogo = null,
        debridCacheStatus = cache?.let { StreamDebridCacheStatus(providerId = "tb", providerName = "TorBox", state = it) },
    )

    @After fun reset() { ForeignDub.setTitle(null); MpvContainers.on = true }

    @Test fun `obcy dubbing idzie na koniec`() {
        assertEquals(1, ForeignDub.tier(s(title = "Serial S01E01 [AC3 5.1 Castellano]")))
        assertEquals(1, ForeignDub.tier(s(title = "Film 2023 TRUEFRENCH 1080p")))
        assertEquals(0, ForeignDub.tier(s(title = "Film 2023 1080p WEB-DL")))
    }

    @Test fun `obcy dubbing z angielskim, MULTI albo polskim zostaje`() {
        assertEquals(0, ForeignDub.tier(s(title = "Film [Castellano + English]")))
        assertEquals(0, ForeignDub.tier(s(title = "Film MULTI French 2160p")))
        assertEquals(0, ForeignDub.tier(s(title = "Film German PL lektor")))
    }

    @Test fun `słowo z tytułu filmu to nie dubbing`() {
        ForeignDub.setTitle("The French Dispatch")
        assertEquals(0, ForeignDub.tier(s(title = "The.French.Dispatch.2021.1080p")))
        ForeignDub.setTitle("Inny film")
        assertEquals(1, ForeignDub.tier(s(title = "The.French.Dispatch.2021.1080p")))
    }

    @Test fun `niegotowe źródła debrid i torrent bez debrid`() {
        assertEquals(1, SourceReady.tier(s(url = null, infoHash = "abc")))
        assertEquals(1, SourceReady.tier(s(cache = StreamDebridCacheState.NOT_CACHED)))
        assertEquals(0, SourceReady.tier(s(name = "[TB download]", cache = StreamDebridCacheState.CACHED)))
        assertEquals(1, SourceReady.tier(s(name = "Comet 1080p [TB⬇️]")))
        assertEquals(1, SourceReady.tier(s(name = "Torrentio [RD download]")))
        assertEquals(1, SourceReady.tier(s(title = "Film ⏳ caching")))
        assertEquals(0, SourceReady.tier(s(name = "Torrentio [RD+]", title = "Film 1080p")))
    }

    @Test fun `AVI, WMV i ASF grają w mpv`() {
        assertTrue(MpvContainers.needsMpv("https://h/f.avi", null))
        assertTrue(MpvContainers.needsMpv("https://h/f.WMV?token=1", null))
        assertTrue(MpvContainers.needsMpv("https://h/stream", "Film.2001.DVDRip.avi "))
        assertTrue(MpvContainers.needsMpv("https://h/stream", null, "video/x-ms-wmv"))
        assertFalse(MpvContainers.needsMpv("https://h/f.mkv", "f.mkv"))
        assertFalse(MpvContainers.needsMpv("https://h/f.avi.mkv", null))
        assertFalse("nazwa .avi tylko w zapytaniu", MpvContainers.needsMpv("https://h/f.mp4?name=x.avi", null))
    }

    @Test fun `wyłączony przełącznik - zawsze Media3`() {
        MpvContainers.on = false
        assertFalse(MpvContainers.needsMpv("https://h/f.avi", "f.avi", "video/x-msvideo"))
    }
}
