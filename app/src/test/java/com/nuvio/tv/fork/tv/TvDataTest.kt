package com.nuvio.tv.fork.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class TvDataTest {
    private val min = 60_000L
    private fun dir() = Files.createTempDirectory("tvdata").toFile()
    private val prog = GuideProgramme("Serial S01E01", 0, 60 * min)

    @Test fun `wylaczony modul nie zapisuje niczego`() {
        val d = dir(); val t = TvData(d, 1, enabled = false)
        t.onGuide(mapOf("ch" to listOf(prog)), mapOf("ch" to 7), 30 * min)
        t.onPlay("ch", prog, 30 * min); t.onPosition("ch", 30 * min, 30 * min)
        assertTrue(d.listFiles().orEmpty().isEmpty())
    }

    @Test fun `ogladanie do konca zapisuje stan a po ponownym wczytaniu jest ten sam`() {
        val d = dir(); val t = TvData(d, 1)
        t.onGuide(mapOf("ch" to listOf(prog)), mapOf("ch" to 7), 30 * min)
        t.onPlay("ch", prog, 0)
        (0..59).forEach { t.onPosition("ch", it * min, it * min) }
        assertEquals(WatchState.WATCHED, t.records().single().state)
        assertEquals(WatchState.WATCHED, TvData(d, 1).records().single().state)
        t.clearHistory(); assertTrue(TvData(d, 1).records().isEmpty())
    }

    @Test fun `uszkodzone pliki nie wywracaja`() {
        val d = dir(); File(d, "watch_1.json").writeText("{{{"); File(d, "episode_log_1.tsv").writeBytes(byteArrayOf(1, 2, 3))
        assertTrue(TvData(d, 1).records().isEmpty())
    }
}
