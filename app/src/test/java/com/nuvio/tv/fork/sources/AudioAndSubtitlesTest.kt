package com.nuvio.tv.fork.sources

import com.nuvio.tv.core.player.SubtitleCharsetDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class AudioAndSubtitlesTest {
    @Test fun `decyzje po sprawdzeniu sciezek`() {
        assertEquals(AudioCheck.Decision.KEEP, AudioCheck.decide(true, listOf("eng", "pl"), 0, 3))
        assertEquals(AudioCheck.Decision.TRY_NEXT, AudioCheck.decide(true, listOf("eng"), 1, 3))
        assertEquals(AudioCheck.Decision.FALLBACK_WITH_SUBS, AudioCheck.decide(true, listOf("eng"), 3, 3))
        assertEquals(AudioCheck.Decision.KEEP, AudioCheck.decide(false, listOf("eng"), 0, 3))
        assertTrue(AudioCheck.hasPolish(listOf("pl-PL"))); assertFalse(AudioCheck.hasPolish(listOf(null, "en")))
    }

    @Test fun `kolejka napisow i podpowiedz po trzech uzyciach`() {
        val q = SubtitleQueue(listOf("a", "b", "c"))
        assertEquals("a", q.current); assertEquals("b", q.next()); assertEquals("c", q.next()); assertNull(q.suggestion(false))
        assertEquals("a", q.next()); assertEquals(SubtitleQueue.Suggestion.SUBTITLES_OFF, q.suggestion(false)); assertEquals(SubtitleQueue.Suggestion.LEKTOR, q.suggestion(true))
        assertNull(SubtitleQueue(emptyList()).next())
    }

    @Test fun `trzy rozmiary napisow`() {
        assertEquals(listOf(80, 120, 170), SubtitleSizeChoice.entries.map { it.sizePercent })
        assertEquals(SubtitleSizeChoice.LARGE, SubtitleSizeChoice.fromSize(160)); assertEquals(SubtitleSizeChoice.MEDIUM, SubtitleSizeChoice.fromSize(115))
    }

    private val polish = "1\n00:00:01,000 --> 00:00:03,000\nZażółć gęślą jaźń. Dzień dobry, mówię po polsku: święto łódź żółw.\n"
    @Test fun `polskie znaki w windows-1250 i iso-8859-2`() {
        for (cs in listOf("windows-1250", "ISO-8859-2")) {
            val bytes = polish.toByteArray(Charset.forName(cs))
            assertEquals(cs, polish, SubtitleCharsetDetector.decode(bytes, languageHint = "pl"))
        }
    }
}
