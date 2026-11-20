package com.nuvio.tv.fork.subs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSubtitleFormatsTest {
    @Test fun `srt bez zmian`() {
        val srt = "1\n00:00:01,000 --> 00:00:02,000\nCześć\n"
        assertEquals(srt, TextSubtitleFormats.toSrt(srt))
    }

    @Test fun `mpl2 na srt z kursywa i nowa linia`() {
        val out = TextSubtitleFormats.toSrt("[10][25]Ala|/ma kota\n[30][40]Druga\n[50][]Trzecia\n")
        assertTrue(out, out.startsWith("1\n00:00:01,000 --> 00:00:02,500\nAla\n<i>ma kota</i>\n\n2\n00:00:03,000 --> 00:00:04,000\nDruga"))
        // brak końca → do 4 s, nie dalej niż następna
        assertTrue(out, out.contains("3\n00:00:05,000 --> 00:00:09,000\nTrzecia"))
    }

    @Test fun `microdvd z liczba klatek w pierwszej linii`() {
        val out = TextSubtitleFormats.toSrt("{1}{1}25\n{25}{50}{y:i}Kursywa\n{75}{100}Zwykły\n{125}{150}Trzeci\n")
        assertTrue(out, out.startsWith("1\n00:00:01,000 --> 00:00:02,000\n<i>Kursywa</i>"))
        assertTrue(out, out.contains("00:00:03,000 --> 00:00:04,000\nZwykły"))
    }

    @Test fun `nieznany format zostaje`() {
        assertEquals("zwykły tekst", TextSubtitleFormats.toSrt("zwykły tekst"))
    }
}
