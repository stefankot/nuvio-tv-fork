package com.nuvio.tv.fork.lektor

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Fork 19.11: mikser gra kwestie do końca, raz, i nie ucisza bieżącej przy napisie bez głosu; cięcie scen po tekście. */
class LektorMixerTest {
    @After fun reset() = LektorEngine.clear()

    private fun proc(): LektorAudioProcessor = LektorAudioProcessor().also {
        it.configure(AudioProcessor.AudioFormat(1_000, 1, C.ENCODING_PCM_16BIT)); it.flush()
    }

    /** [ms] ms ciszy przez procesor (1 kHz mono = 1 próbka/ms); zwraca próbki wyjścia. */
    private fun run(p: LektorAudioProcessor, ms: Int): ShortArray {
        val inp = ByteBuffer.allocateDirect(2 * ms).order(ByteOrder.nativeOrder())
        p.queueInput(inp)
        val out = p.output.order(ByteOrder.nativeOrder())
        return ShortArray(ms) { out.short }
    }

    @Test fun `kwestia gra do końca mimo napisu bez głosu`() {
        LektorEngine.load(listOf(SubtitleCue(0, 1_000, "Pierwsza kwestia."), SubtitleCue(1_000, 2_000, "Druga kwestia.")))
        LektorEngine.voice[0] = Pcm(ShortArray(1_800) { 8_000 }, 1_000) // dłuższa niż czas do drugiej, druga bez głosu
        LektorEngine.enabled = true; LektorEngine.updatePosition(0, false)
        val out = run(proc(), 2_500)
        assertTrue("głos w 1,5 s", out[1_500] > 4_000)
        assertTrue("koniec po 1,8 s", out[2_000].toInt() == 0)
    }

    @Test fun `następna czeka na koniec poprzedniej i każda gra raz`() {
        LektorEngine.load(listOf(SubtitleCue(0, 1_000, "Pierwsza."), SubtitleCue(1_000, 2_000, "Druga.")))
        LektorEngine.voice[0] = Pcm(ShortArray(1_300) { 8_000 }, 1_000)
        LektorEngine.voice[1_000] = Pcm(ShortArray(500) { 4_000 }, 1_000)
        LektorEngine.enabled = true; LektorEngine.updatePosition(0, false)
        val out = run(proc(), 3_000)
        assertTrue("pierwsza nieucięta w 1,2 s", out[1_200] > 6_000)
        assertTrue("druga po pierwszej", out[1_500] in 2_000..6_000)
        assertEquals("bez powtórki", 0, out[2_500].toInt())
    }

    @Test fun `cięcie sceny trafia w granice kwestii, nie w pauzę w zdaniu`() {
        val rate = 1_000
        fun speech(ms: Int) = ShortArray(ms) { if (it % 2 == 0) 9_000 else -9_000 }
        fun pause(ms: Int) = ShortArray(ms)
        // kwestia 1: 2 s mowy, pauza w zdaniu 0,5 s, 2 s mowy; granica 1,0 s; kwestia 2: 1 s mowy
        val s = speech(2_000) + pause(500) + speech(2_000) + pause(1_000) + speech(1_000)
        val parts = SilenceSplit.split(Pcm(s, rate), 2, listOf("a".repeat(80), "b".repeat(20)))
        assertNotNull(parts)
        assertTrue("pierwsza ${parts!![0].durationMs}", parts[0].durationMs in 4_400L..4_900L)
        // długości niezgodne z tekstem → odrzucone
        assertNull(SilenceSplit.split(Pcm(speech(500) + pause(1_000) + speech(5_000), rate), 2, listOf("a".repeat(80), "b".repeat(20))))
    }
}
