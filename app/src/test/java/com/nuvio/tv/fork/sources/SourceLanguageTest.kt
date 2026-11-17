package com.nuvio.tv.fork.sources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceLanguageTest {
    @Test fun `wykrywa jezyk`() {
        assertEquals("lektor PL", SourceLanguage.detect("Film 1080p WEB-DL Lektor PL"))
        assertEquals("dubbing PL", SourceLanguage.detect("Film 720p Dubbing"))
        assertEquals("PL", SourceLanguage.detect("Film 1080p MULTI PL"))
        assertNull(SourceLanguage.detect("Film 1080p PL subs")); assertNull(SourceLanguage.detect("Film 1080p English"))
    }
    @Test fun `linia gotowe`() {
        assertEquals("Gotowe · 1080p · lektor PL", SourceLanguage.readyLine(ReadyInfo("1080p", emptyList(), null, true), "Film Lektor PL"))
        assertEquals("Gotowe · 4K", SourceLanguage.readyLine(ReadyInfo("4K", emptyList(), null, true), "Film English"))
    }
}
