package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.names.ChannelNames
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelNamesTest {
    private fun c(s: String) = ChannelNames.clean(s)

    @Test fun `kraj i jakosc znikaja`() {
        assertEquals("Polsat Cafe", c("PL: Polsat Cafe HD"))
        assertEquals("TVP 1", c("PL: TVP 1 HD"))
        assertEquals("Eleven Sports 1", c("PL: Eleven Sports 1 4K"))
        assertEquals("Canal+ Sport 5", c("PL: Canal+ Sport 5 HD"))
        assertEquals("Novelas+", c("PL: Novelas+ HD"))
        assertEquals("AXN Black", c("PL: AXN Black"))
        assertEquals("TVP 1", c("[PL] TVP 1 FHD"))
        assertEquals("Active Family", c("Active Family HD (1080i)"))
        assertEquals("Nickelodeon", c("Nickelodeon (PL)"))
        assertEquals("TVN 24", c("PL | TVN 24 UHD 50fps"))
    }

    @Test fun `nazwa dostawcy przed krajem znika`() {
        assertEquals("TVN 7", c("Strong8K: PL| TVN 7 FHD"))
        assertEquals("Polsat News", c("Strong8K PL: Polsat News HD"))
        assertEquals("TVN Style", c("Strong8K · PL: TVN STYLE ᴴᴰ (NA)")) // przegląd 27.11 (12): normalne litery // JMGO iptvdb2: separator „·”
    }

    @Test fun `sama nazwa zostaje`() {
        assertEquals("HBO Max", c("HBO Max"))
        assertEquals("TVN", c("TVN"))
        assertEquals("HD", c("HD"))
        assertEquals("Kino Polska", c("Kino Polska"))
        assertEquals("BBC Earth", c("BBC Earth"))
        assertEquals("TVNHD", c("TVNHD"))
        assertEquals("Polsat 2", c("Polsat 2"))
    }
}
