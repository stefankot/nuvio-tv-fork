package com.nuvio.tv.fork.yt

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy 22.11: panel dźwięku YouTube — jeden wiersz na język, myślniki w kodach języka. */
class YouTubeAudioSlimTest {
    private val mpd = """
        <MPD><Period>
          <AdaptationSet mimeType="video/mp4"><Representation id="v1" bandwidth="5000000"/></AdaptationSet>
          <AdaptationSet mimeType="audio/webm" lang="en‐US"><Representation id="en-opus-hi" bandwidth="160000"/></AdaptationSet>
          <AdaptationSet mimeType="audio/mp4" lang="en-US"><Representation id="en-aac-lo" bandwidth="48000"/><Representation id="en-aac-hi" bandwidth="128000"/></AdaptationSet>
          <AdaptationSet mimeType="audio/mp4" lang="pl"><Representation id="pl-aac" bandwidth="128000"/></AdaptationSet>
        </Period></MPD>
    """.trimIndent().toByteArray()

    @Test fun `myślniki Unicode w kodzie języka`() {
        assertEquals("en-US (original)", YouTubeAudioSlim.normalize("en‐US (original)"))
        assertEquals("pt-BR", YouTubeAudioSlim.normalize("pt‑BR"))
        assertEquals("pl", YouTubeAudioSlim.normalize("pl"))
    }

    @Test fun `jeden zestaw na język, AAC przed Opus, najlepsza jakość`() {
        val out = String(YouTubeAudioSlim.prepare(mpd))
        assertTrue(out.contains("en-aac-hi"))
        assertFalse(out.contains("en-aac-lo"))
        assertFalse("Opus tego samego języka usunięty", out.contains("en-opus-hi"))
        assertTrue(out.contains("pl-aac"))
        assertTrue("obraz bez zmian", out.contains("v1"))
    }

    @Test fun `zły albo prosty manifest wraca bez zmian`() {
        val bad = "nie xml".toByteArray()
        assertArrayEquals(bad, YouTubeAudioSlim.prepare(bad))
        val one = "<MPD><Period><AdaptationSet mimeType=\"audio/mp4\" lang=\"en\"><Representation id=\"a\" bandwidth=\"1\"/><Representation id=\"b\" bandwidth=\"2\"/></AdaptationSet></Period></MPD>".toByteArray()
        assertArrayEquals("jeden zestaw — bez zmian", one, YouTubeAudioSlim.prepare(one))
    }
}
