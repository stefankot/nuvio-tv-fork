package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.covers.TvCovers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Przegląd 27.11 (okładki z AIOMetadata): dopasowanie tytułu z przewodnika do polskiej nazwy z wtyczki. */
class TvCoversNameTest {
    private fun matches(epg: String, aioName: String): Boolean {
        val key = TvCovers.norm(TvCovers.searchTitle(epg)!!)
        return key in TvCovers.nameKeys(aioName)
    }

    @Test fun polishSeriesNameWithOriginalPart() {
        assertTrue(matches("Hell's Kitchen - Piekielna Kuchnia (12)", "Hell's Kitchen. Piekielna kuchnia"))
    }

    @Test fun exactName() {
        assertTrue(matches("Bez śladu 6: Zakręt (14)", "Bez śladu"))
    }

    @Test fun differentTitleDoesNotMatch() {
        assertFalse(matches("Kropka nad i", "Kropka"))
        assertFalse(matches("Fakty", "Fakty po faktach"))
    }
}
