package com.nuvio.tv.fork.sources

import com.nuvio.tv.domain.model.Stream
import org.junit.Assert.assertEquals
import org.junit.Test

/** 26.11 (V10): SELection przez aiostreams.elfhosted.com nie jest „wolnym pośrednikiem”; MediaFusion nadal tak. */
class SlowProxy4kTest {
    private fun s(title: String, url: String, addon: String) = Stream(
        name = "4K", title = title, description = null, url = url, ytId = null, infoHash = null, fileIdx = null, externalUrl = null,
        behaviorHints = null, addonName = addon, addonLogo = null,
    )

    private val rank: (Stream) -> List<Int> = { listOf(0, 0, 0) }

    @Test fun `SELection przez aiostreams elfhosted zostaje w kolejności reguł`() {
        val a = s("The.Last.of.Us.S01E01.2160p.BluRay.DV 17 GB 28 Mbps", "https://aiostreams.elfhosted.com/playback/a", "SELection")
        val b = s("The.Last.of.Us.S01E01.2160p.BluRay.DV 11 GB 18 Mbps", "https://aiostreams.elfhosted.com/playback/b", "SELection")
        val direct = s("The.Last.of.Us.S01E01.2160p.WEB-DL", "https://flixnest.app/x.mkv", "Flix-Streams")
        val list = listOf(a, b, direct)
        assertEquals(list, SlowProxy4k.demote(list, rank))
    }

    @Test fun `pośrednik MediaFusion 4K idzie za bezpośrednie 4K`() {
        val slow = s("Film.2023.2160p.BluRay", "https://mediafusion.elfhosted.com/playback/a", "MediaFusion | ElfHosted")
        val direct = s("Film.2023.2160p.WEB-DL", "https://flixnest.app/x.mkv", "Flix-Streams")
        assertEquals(listOf(direct, slow), SlowProxy4k.demote(listOf(slow, direct), rank))
    }
}
