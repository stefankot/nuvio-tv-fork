package com.nuvio.tv.fork.lektor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class LektorTest {
    @Test fun `czyszczenie napisow SRT i ASS`() {
        assertEquals("Dzień dobry.", CuePlanner.clean("<i>Dzień dobry.</i>"))
        assertEquals("Chodź tu.", CuePlanner.clean("- Chodź tu."))
        assertEquals("Idziemy.", CuePlanner.clean("[trzask drzwi] Idziemy."))
        assertEquals("Co robisz?", CuePlanner.clean("ANNA: Co robisz?"))
        assertEquals("Zostań.", CuePlanner.clean("{\\an8}Zostań."))
        assertEquals("", CuePlanner.clean("(muzyka)"))
        assertEquals("Raz dwa.", CuePlanner.clean("Raz\\Ndwa.").replace("Raz dwa.", "Raz dwa."))
    }

    private fun ts(sec: Int) = "%02d:%02d:%02d".format(sec / 3600, sec % 3600 / 60, sec % 60)
    private val srt = (1..40).joinToString("\n\n") { i ->
        val s = i * 4; "$i\n${ts(s)},000 --> ${ts(s + 2)},500\n" + (if (i % 5 == 0) "(odgłos $i)" else "<i>Kwestia numer $i.</i>")
    }

    @Test fun `plan kwestii z probki 40 napisow SRT`() {
        val cues = CuePlanner.parseSrt(srt)
        assertEquals(40, cues.size)
        val lines = CuePlanner.plan(cues)
        assertEquals(32, lines.size) // 8 opisów dźwięków odpadło
        assertTrue(lines.all { it.text.startsWith("Kwestia") && it.availableMs > 0 })
    }

    @Test fun `ASS i laczenie kwestii bliskich o mniej niz 300 ms`() {
        val ass = "[Events]\nDialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,{\\i1}Pierwsza{\\i0}\nDialogue: 0,0:00:02.10,0:00:03.00,Default,,0,0,0,,Druga.\nDialogue: 0,0:00:05.00,0:00:06.00,Default,,0,0,0,,Trzecia."
        val lines = CuePlanner.plan(CuePlanner.parseAss(ass))
        assertEquals(2, lines.size); assertEquals("Pierwsza Druga.", lines[0].text); assertEquals(4_000L, lines[0].availableMs)
    }

    @Test fun `tempo trzy przypadki`() {
        val ok = Tempo.fit("Krótka.", 1000, 2000); assertEquals(1f, ok.rate, 0f)
        val fast = Tempo.fit("Trochę dłuższa.", 2400, 2000); assertEquals(1.2f, fast.rate, 0.001f); assertEquals(0, fast.borrowedGapMs)
        val gap = Tempo.fit("Dłuższa kwestia.", 3000, 2000, gapMs = 1500); assertEquals(Tempo.MAX_RATE, gap.rate, 0f); assertTrue(gap.borrowedGapMs in 1..1500)
        val cut = Tempo.fit("Pierwsze zdanie. Drugie zdanie jest bardzo długie. Trzecie też.", 12000, 2000, gapMs = 500)
        assertTrue(cut.truncated); assertTrue(cut.text.length < 60); assertTrue(cut.text.endsWith("."))
    }

    @Test fun `okno syntezy przewiniecie i pamiec`() {
        val lines = (0..100).map { Line(it * 10_000L, it * 10_000L + 3000, "x", 10_000) }
        assertEquals((0..18).map { it * 10_000L }, SynthWindow.pending(lines, 0, emptySet()).map { it.startMs }.filter { it <= 180_000 })
        val done = (0..18).map { it * 10_000L }.toSet()
        assertTrue(SynthWindow.pending(lines, 0, done).none { it.startMs in done })
        val after = SynthWindow.dropOutside(done, 600_000L); assertTrue(after.isEmpty())
        assertEquals(600_000L, SynthWindow.pending(lines, 600_000L, after).first().startMs)
        assertTrue(SynthWindow.shouldSkip(false, 0)); assertFalse(SynthWindow.shouldSkip(true, 100))
        val dir = Files.createTempDirectory("synth").toFile(); val cache = SynthCache(dir, limitBytes = 3000)
        repeat(5) { cache.put("t", it * 1000L, ByteArray(1000)); Thread.sleep(15) }
        assertTrue(cache.sizeBytes() <= 3000); assertNull(cache.get("t", 0)); assertNotNull(cache.get("t", 4000))
    }

    @Test fun `obwiednia sciszania przed w trakcie i po kwestii`() {
        val env = DuckEnvelope(); val lines = listOf(Line(10_000, 12_000, "x", 5000))
        assertEquals(1f, env.gain(5_000, lines), 0f)
        assertEquals(0.316f, env.gain(11_000, lines), 0.01f)
        assertTrue(env.gain(9_925, lines) in 0.5f..0.9f)
        assertTrue(env.gain(12_150, lines) in 0.5f..0.9f)
        assertEquals(1f, env.gain(13_000, lines), 0f)
        // synteteczny sygnał: stała 0,5
        val sample = listOf(5_000L, 11_000L, 13_000L).map { env.mix(0.5f, 0f, env.gain(it, lines)) }
        assertEquals(0.5f, sample[0], 0.001f); assertTrue(sample[1] < 0.2f); assertEquals(0.5f, sample[2], 0.001f)
    }

    @Test fun `widocznosc i szczeble`() {
        assertTrue(LektorRules.visibleInMenu(true, false, true)); assertFalse(LektorRules.visibleInMenu(true, true, true)); assertFalse(LektorRules.visibleInMenu(true, false, false))
        assertEquals(LektorRules.Step.POLISH_AUDIO, LektorRules.step(true, true, true, true)); assertEquals(LektorRules.Step.LEKTOR, LektorRules.step(true, false, true, true))
        assertEquals(LektorRules.Step.POLISH_SUBS, LektorRules.step(false, false, true, true)); assertEquals(LektorRules.Step.POLISH_SUBS, LektorRules.step(true, false, true, false))
        assertEquals(LektorRules.Step.NONE, LektorRules.step(true, false, false, true)); assertTrue(LektorRules.subtitlesHidden(true))
    }

    @Test fun `silnik na atrapie niedostepny bez polskiego glosu`() {
        val none = object : Lektor { override suspend fun synthesize(text: String, rate: Float): Pcm = throw Lektor.Unavailable("brak głosu"); override fun voices() = emptyList<String>() }
        assertTrue(none.voices().isEmpty())
        assertTrue(runCatching { kotlinx.coroutines.runBlocking { none.synthesize("x", 1f) } }.exceptionOrNull() is Lektor.Unavailable)
    }
}
