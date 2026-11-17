package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.guide.EpgSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpgSourcesTest {
    private val sources = EpgSources.parse(
        """{"sources":[{"country":"PL","epg":"https://x/pl.gz","idSuffixes":[".pl"],"groupHints":["PL","Polska"]}]}""",
    )

    @Test fun `rozpoznaje Polske po koncowce tvg-id i grupie`() {
        val ch = (1..10).map { "tvp$it.pl" to "Info" } + (1..10).map { null to "PL | Filmy" }
        assertEquals("PL", EpgSources.detect(ch, sources)?.country)
    }
    @Test fun `inny kraj nie dostaje przewodnika`() {
        val ch = (1..20).map { "bbc$it.uk" to "UK Sport" }
        assertNull(EpgSources.detect(ch, sources))
    }
    @Test fun `zepsuty json daje pusta liste`() { assertEquals(0, EpgSources.parse("{").size) }
}
