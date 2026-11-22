package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.guide.EpgSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test fun `grupa po slowach bez wzgledu na wielkosc liter i separatory`() {
        val ch = listOf(null to "polska-Sport", null to "pl|VOD", null to "PL: Filmy", null to "Inne")
        assertEquals("PL", EpgSources.detect(ch, sources, minShare = 0.75)?.country)
    }
    @Test fun `slowo musi byc cale - PLUS to nie PL`() {
        val ch = (1..10).map { null to "PLUS Filmy" } + (1..10).map { "x$it.plus" to "Polskaa" }
        assertNull(EpgSources.detect(ch, sources))
    }
    @Test fun `remis - pierwsze zrodlo z listy`() {
        val two = EpgSources.parse(
            """{"sources":[{"country":"A","epg":"a","groupHints":["tv"]},{"country":"B","epg":"b","groupHints":["tv"]}]}""",
        )
        assertEquals("A", EpgSources.detect(listOf(null to "TV 1", null to "tv"), two)?.country)
    }
    @Test fun `puste zrodla albo kanaly daja null`() {
        assertNull(EpgSources.detect(emptyList(), sources))
        assertNull(EpgSources.detect(listOf(null to "PL"), emptyList()))
    }
    /** JMGO 22.11: 252 827 kanałów — dawniej jedna kompilacja wyrażenia na kanał i podpowiedź (kilkanaście minut na TV). */
    @Test fun `ćwierć miliona kanałów szybko i z tym samym wynikiem`() {
        val ch = (0 until 250_000).map { i -> (if (i % 3 == 0) "k$i.pl" else null) to "PL | Grupa ${i % 400}" }
        val t0 = System.nanoTime()
        assertEquals("PL", EpgSources.detect(ch, sources)?.country)
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertTrue("trwało $ms ms", ms < 5_000)
    }

    /** JMGO 22.11: polska lista hls.gd (326) obok list FlixNest (252 tys., głównie zagraniczne) — przewodnik dla polskiej. */
    @Test fun `kazda lista osobno - mala polska obok duzej zagranicznej`() {
        val polish = (1..326).map { "tvp$it.pl" to "Polska" }
        val foreign = (1..50_000).map { "ch$it.us" to "USA | News" }
        assertNull("razem — poniżej progu", EpgSources.detect(polish + foreign, sources))
        assertEquals(listOf("PL"), EpgSources.detectPerList(listOf(polish, foreign), sources).map { it.country })
    }
    @Test fun `dwie polskie listy - jeden przewodnik, zadna - brak`() {
        val a = (1..10).map { null to "PL | Sport" }
        val b = (1..10).map { "x$it.pl" to "Info" }
        assertEquals(1, EpgSources.detectPerList(listOf(a, b), sources).size)
        assertTrue(EpgSources.detectPerList(listOf((1..10).map { null to "UK" }), sources).isEmpty())
        assertTrue(EpgSources.detectPerList(emptyList(), sources).isEmpty())
    }
}
