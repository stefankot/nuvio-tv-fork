package com.nuvio.tv.fork.ai.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Testy 22.11: czytanie napisów wbudowanych w MKV (EBML) dla Napisów AI i lektora. */
class MkvSubtitleTextTest {
    private fun b(vararg x: Int) = ByteArray(x.size) { x[it].toByte() }

    @Test fun `identyfikator EBML 1-4 bajty`() {
        assertEquals(0xA3L to 1, MkvSubtitleText.readId(b(0xA3), 0))
        assertEquals(0x4286L to 2, MkvSubtitleText.readId(b(0x42, 0x86), 0))
        assertEquals(0x2AD7B1L to 3, MkvSubtitleText.readId(b(0x2A, 0xD7, 0xB1), 0))
        assertEquals(0x1A45DFA3L to 4, MkvSubtitleText.readId(b(0x1A, 0x45, 0xDF, 0xA3), 0))
        assertNull("ucięty", MkvSubtitleText.readId(b(0x1A, 0x45), 0))
        assertNull("bez znacznika długości", MkvSubtitleText.readId(b(0x05), 0))
        assertNull("poza tablicą", MkvSubtitleText.readId(b(0xA3), 1))
    }

    @Test fun `rozmiar EBML i rozmiar nieznany`() {
        assertEquals(5L to 1, MkvSubtitleText.readSize(b(0x85), 0))
        assertEquals(0x123L to 2, MkvSubtitleText.readSize(b(0x41, 0x23), 0))
        assertEquals(-1L to 1, MkvSubtitleText.readSize(b(0xFF), 0))
        assertEquals(-1L to 8, MkvSubtitleText.readSize(b(0x01, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF), 0))
        assertEquals(0xFFL to 2, MkvSubtitleText.readSize(b(0x40, 0xFF), 0))
        assertNull(MkvSubtitleText.readSize(b(0x00), 0))
        assertNull(MkvSubtitleText.readSize(b(0x41), 0))
    }

    @Test fun `liczba bez znaku`() {
        assertEquals(0x010203L, MkvSubtitleText.readUInt(b(1, 2, 3), 0, 3))
        assertEquals(0L, MkvSubtitleText.readUInt(b(1), 0, 0))
        assertNull(MkvSubtitleText.readUInt(b(1, 2), 1, 2))
        assertNull(MkvSubtitleText.readUInt(ByteArray(9), 0, 9))
    }

    @Test fun `tekst ASS bez stylów, SRT bez znaczników`() {
        val ass = "12,0,Default,,0,0,0,,{\\i1}Cześć{\\i0}\\Nświecie,\\hhej".toByteArray()
        assertEquals("Cześć\nświecie, hej", MkvSubtitleText.cleanText("S_TEXT/ASS", ass))
        assertEquals("Ala ma kota", MkvSubtitleText.cleanText("S_TEXT/UTF8", "<i>Ala</i> ma <font color=\"red\">kota</font>\u0000".toByteArray()))
    }

    @Test fun `język ścieżki - kody 3-literowe na 2-literowe`() {
        assertEquals("pl", MkvSubtitleText.normLang("pol"))
        assertEquals("de", MkvSubtitleText.normLang("ger"))
        assertEquals("fr", MkvSubtitleText.normLang("fre"))
        assertEquals("en", MkvSubtitleText.normLang("en-US"))
        assertNull(MkvSubtitleText.normLang("und"))
        assertNull(MkvSubtitleText.normLang(" "))
        assertNull(MkvSubtitleText.normLang(null))
    }
}
