package com.nuvio.tv.fork.tv

import com.nuvio.tv.reshaped.livetv.LiveTvGuideWindow
import org.junit.Assert.assertEquals
import org.junit.Test

class GuideWindowTest {
    private val day = 24 * 3_600_000L
    @Test fun `zwykle urzadzenie ma pelny zakres`() {
        assertEquals(7 * day, LiveTvGuideWindow.Regular.pastMs)
        assertEquals(8 * day, LiveTvGuideWindow.Regular.aheadMs)
    }
    @Test fun `low memory zachowuje zakres Reshaped`() {
        assertEquals(2 * 3_600_000L, LiveTvGuideWindow.LowMemory.pastMs)
        assertEquals(8 * 3_600_000L, LiveTvGuideWindow.LowMemory.aheadMs)
    }
}
