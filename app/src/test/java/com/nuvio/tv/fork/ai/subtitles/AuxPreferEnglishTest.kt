package com.nuvio.tv.fork.ai.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 22.11: film spoza Europy — pomocnicze rosyjski i angielski, angielski przy podobnym dopasowaniu. */
class AuxPreferEnglishTest {
    private fun c(id: String, lang: String, score: Int, ok: Boolean = true) = AuxiliarySubtitleCandidate(id, lang, ok, score)

    @Test fun `europejskie i spoza Europy`() {
        assertTrue(AuxiliaryLanguagePolicy.isEuropean("fre"))
        assertTrue(AuxiliaryLanguagePolicy.isEuropean("en"))
        assertTrue(AuxiliaryLanguagePolicy.isEuropean("Polish"))
        assertFalse(AuxiliaryLanguagePolicy.isEuropean("ja"))
        assertFalse(AuxiliaryLanguagePolicy.isEuropean("ko"))
        assertFalse(AuxiliaryLanguagePolicy.isEuropean("hi"))
    }

    @Test fun `angielski wygrywa przy podobnym dopasowaniu`() {
        val list = listOf(c("de", "de", 90), c("en", "en", 80), c("ru", "ru", 70), c("uk", "uk", 95))
        val sel = AuxiliaryLanguagePolicy.chooseForPolish("ja", list, preferredWestern = "en")
        assertEquals("en", sel.western?.sourceId)
        assertEquals("ru", sel.slavic?.sourceId)
        // bez preferencji (film europejski) — jak dotąd najlepsze dopasowanie
        assertEquals("de", AuxiliaryLanguagePolicy.chooseForPolish("fr", list).western?.sourceId)
    }

    @Test fun `angielski duzo gorszy albo niezweryfikowany nie wygrywa`() {
        assertEquals("de", AuxiliaryLanguagePolicy.chooseForPolish("ja", listOf(c("de", "de", 95), c("en", "en", 70)), preferredWestern = "en").western?.sourceId)
        assertEquals("de", AuxiliaryLanguagePolicy.chooseForPolish("ja", listOf(c("de", "de", 60), c("en", "en", 99, ok = false)), preferredWestern = "en").western?.sourceId)
    }
}
