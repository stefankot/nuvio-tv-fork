package com.nuvio.tv.fork.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EpisodeLogTest {
    private val h = 3_600_000L
    private fun e(i: Int) = LogEntry("serial", "ch", i * h, h, "e$i", "Serial")

    @Test fun `zapis i odczyt`() {
        val f = File.createTempFile("log", ".tsv")
        EpisodeLog.write(f, (1..5).map(::e))
        assertEquals((1..5).map(::e), EpisodeLog.read(f))
    }

    @Test fun `limit wyrzuca najstarsze`() {
        val merged = EpisodeLog.merge((1..10).map(::e), (11..20).map(::e), limit = 12)
        assertEquals(12, merged.size); assertEquals(9L * h, merged.first().startMs)
    }

    @Test fun `uszkodzony plik daje pusta liste a nie wyjatek`() {
        val f = File.createTempFile("log", ".tsv"); f.writeBytes(byteArrayOf(0, 1, -5, 10, 9, 9))
        assertTrue(EpisodeLog.read(f).isEmpty())
        assertTrue(EpisodeLog.read(File("/nie/ma/takiego")).isEmpty())
    }

    @Test fun `tylko kanaly z archiwum i okno 7 dni wstecz 8 naprzod`() {
        val now = 100 * 24 * h
        val kept = mapOf("a" to listOf(GuideProgramme("Serial S01E02", now - 8 * 24 * h, now - 8 * 24 * h + h), GuideProgramme("Serial S01E03", now - h, now)), "b" to listOf(GuideProgramme("X", now, now + h)))
        val out = EpisodeLog.fromGuide(kept, mapOf("a" to 7), now)
        assertEquals(listOf("s01e03"), out.map { it.episodeId })
    }
}
