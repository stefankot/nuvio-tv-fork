package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.most.enrichProgrammes
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnrichProgrammesTest {
    private val min = 60_000L
    @Test fun `emisja bez obrazu dostaje obraz z drugiej listy`() {
        val a = listOf(LiveTvProgramme("Serial", 100 * min, 160 * min, description = "d", episode = "E1"))
        val b = listOf(LiveTvProgramme("serial", 105 * min, 160 * min, image = "http://x/y.jpg"))
        assertEquals("http://x/y.jpg", enrichProgrammes(a, listOf(b)).single().image)
    }
    @Test fun `start poza 10 minutami albo inny tytul nie laczy`() {
        val a = listOf(LiveTvProgramme("Serial", 100 * min, 160 * min, description = "d", episode = "E1"))
        assertNull(enrichProgrammes(a, listOf(listOf(LiveTvProgramme("Serial", 120 * min, 160 * min, image = "i")))).single().image)
        assertNull(enrichProgrammes(a, listOf(listOf(LiveTvProgramme("Inny", 100 * min, 160 * min, image = "i")))).single().image)
    }
}
