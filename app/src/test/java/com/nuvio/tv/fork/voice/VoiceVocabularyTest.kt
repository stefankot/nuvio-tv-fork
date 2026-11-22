package com.nuvio.tv.fork.voice

import org.junit.Assert.assertEquals
import org.junit.Test

/** 22.11: słownik komend zamiast initial_prompt (sherpa-onnx 1.13.8 nie ma tego pola dla Whispera). */
class VoiceVocabularyTest {
    @Test fun `drobne bledy poprawione do slow komend`() {
        assertEquals("włącz Netflix", VoiceVocabulary.snap("włącz netflik"))
        assertEquals("otwórz YouTube", VoiceVocabulary.snap("otwórz youtub"))
        assertEquals("ciszej", VoiceVocabulary.snap("ciszej"))
        assertEquals("następny odcinek", VoiceVocabulary.snap("następny odcinek"))
        assertEquals("następny odcinek.", VoiceVocabulary.snap("nastepny odcinek."))
        assertEquals("głośniej", VoiceVocabulary.snap("glośniej"))
    }

    @Test fun `obce slowa i krotkie bez zmian`() {
        assertEquals("Muminki", VoiceVocabulary.snap("Muminki"))
        assertEquals("tvn 24", VoiceVocabulary.snap("tvn 24"))
        assertEquals(1, VoiceVocabulary.distance("kot", "kit"))
    }
}
