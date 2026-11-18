package com.nuvio.tv.fork.lektor

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LektorEngineTest {
    @After fun reset() = LektorEngine.clear()

    private fun cue(s: Long, e: Long, t: String) = SubtitleCue(s, e, t)

    @Test fun `ściszanie tylko w kwestii i głos tylko gdy gotowy`() {
        LektorEngine.load(listOf(cue(10_000, 12_000, "Cześć wszystkim")))
        LektorEngine.enabled = true
        assertEquals(1f, LektorEngine.gainAt(5_000), 0.001f)
        assertTrue(LektorEngine.gainAt(11_000) < 0.4f)
        assertEquals(0f, LektorEngine.voiceAt(11_000), 0f) // głos niegotowy: pominięty
        LektorEngine.voice[10_000] = Pcm(ShortArray(48_000) { 16_384 }, 24_000)
        assertEquals(0.5f, LektorEngine.voiceAt(11_000), 0.001f)
        assertEquals(1f, LektorEngine.gainAt(20_000), 0.001f)
    }

    @Test fun `procesor miksuje głos do wszystkich kanałów i nie rusza ciszy poza kwestią`() {
        LektorEngine.load(listOf(cue(0, 2_000, "Test")))
        LektorEngine.voice[0] = Pcm(ShortArray(24_000) { 8_192 }, 24_000)
        LektorEngine.enabled = true
        LektorEngine.updatePosition(0, false)
        val p = LektorAudioProcessor()
        p.configure(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        p.flush(); p.flush()
        assertTrue(p.isActive)
        val inp = ByteBuffer.allocateDirect(4 * 100).order(ByteOrder.nativeOrder()) // 100 klatek stereo, cisza
        p.queueInput(inp.also { it.limit(it.capacity()) })
        val out = p.output.order(ByteOrder.nativeOrder())
        val l = out.short; val r = out.short
        assertTrue("lewy $l", l > 5_000); assertEquals(l, r)
    }

    @Test fun `wyłączony lektor nie aktywuje procesora`() {
        val p = LektorAudioProcessor()
        p.configure(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        assertTrue(!p.isActive)
    }
}
