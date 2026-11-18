package com.nuvio.tv.fork.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationParserTest {
    @Test fun `minuty slowami i cyframi`() {
        assertEquals(60_000L, DurationParser.millis("minutę"))
        assertEquals(300_000L, DurationParser.millis("pięć minut"))
        assertEquals(600_000L, DurationParser.millis("10 minut"))
        assertEquals(120_000L, DurationParser.millis("dwie minuty"))
    }

    @Test fun `sekundy godziny i pol`() {
        assertEquals(30_000L, DurationParser.millis("30 sekund"))
        assertEquals(3_600_000L, DurationParser.millis("godzinę"))
        assertEquals(1_800_000L, DurationParser.millis("pół godziny"))
        assertEquals(30_000L, DurationParser.millis("pół"))
    }

    @Test fun `puste`() { assertNull(DurationParser.millis("")) }
}
