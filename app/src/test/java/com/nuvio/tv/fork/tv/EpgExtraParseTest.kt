package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.most.ForkTvHooks
import com.nuvio.tv.fork.tv.most.ProgrammeExtra
import com.nuvio.tv.reshaped.livetv.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.time.Instant

class EpgExtraParseTest {
    private val now = Instant.parse("2026-10-02T08:15:00Z").toEpochMilli()
    private val hour = 3_600_000L
    @After fun reset() { ForkTvHooks.sink = null }

    @Test fun `czyta szczegoly emisji i numer odcinka`() {
        val ch = LiveTvChannel(id = "a/1", name = "News", streamUrl = "u", tvgId = "shared", sourceId = "a", guideKey = liveTvGuideKey("shared", "News", "a"))
        var got: ProgrammeExtra? = null
        ForkTvHooks.sink = { _, _, e -> got = e }
        val xml = """<tv><channel id="shared"><display-name>News</display-name></channel>
          <programme channel="shared" start="20261002080000 +0000" stop="20261002090000 +0000"><title>Serial</title>
          <sub-title>Pilot</sub-title><desc>Opis</desc><date>2019</date><country>PL</country><category>Dramat</category>
          <episode-num>S1E2</episode-num><rating><value>12</value></rating><credits><actor>Jan Kowalski</actor></credits></programme></tv>"""
        val guide = readXmlTvGuide(xml.byteInputStream(), LiveTvGuideRequest.from(listOf(ch)), now, LiveTvGuideWindow(2 * hour, 4, 4 * hour, 8, detailsMs = 4 * hour, maxDescription = 300, detailsPerChannel = 4, detailsBudgetChars = 1_000_000), ::KXmlParser)
        assertEquals("S1E2", guide.schedule.getValue(ch.guideKey).single().episode)
        assertEquals(ProgrammeExtra("Pilot", "S1E2", "12", 2019, "PL", listOf("Dramat"), listOf("actor" to "Jan Kowalski")), got)
    }
}
