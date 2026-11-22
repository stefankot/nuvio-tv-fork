package com.nuvio.tv.fork.lektor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy 22.11: skala głośności lektora (paczka „oczywiste” pkt 8) i miękki ogranicznik miksera. */
class LektorVolumeTest {
    @Test fun `100 procent to dawne 30 procent amplitudy`() {
        assertEquals(0.30f, LektorModule.voiceGainFor(100), 0.0001f)
    }

    @Test fun `skala rośnie i ma granice 10-300`() {
        val values = listOf(10, 30, 50, 70, 100, 130, 160, 200, 300).map(LektorModule::voiceGainFor)
        assertEquals(values.sorted(), values)
        assertEquals(LektorModule.voiceGainFor(10), LektorModule.voiceGainFor(0), 0f)
        assertEquals(LektorModule.voiceGainFor(300), LektorModule.voiceGainFor(1000), 0f)
    }

    @Test fun `dwa razy więcej procent to ok 10 dB głośniej`() {
        val db = 20 * kotlin.math.log10(LektorModule.voiceGainFor(200) / LektorModule.voiceGainFor(100))
        assertEquals(10.0f, db, 0.1f)
    }

    @Test fun `miękki ogranicznik - liniowo do 0,9, potem najwyżej 1`() {
        assertEquals(0.5f, softClip(0.5f), 0.0001f)
        assertEquals(-0.85f, softClip(-0.85f), 0.0001f)
        listOf(0.95f, 1.2f, 3f, 50f).forEach { x ->
            val y = softClip(x)
            assertTrue("$x → $y", y in 0.9f..1.0f)
            assertEquals(-y, softClip(-x), 0.0001f)
        }
        assertTrue(softClip(1.5f) > softClip(1.1f))
    }

    @Test fun `opisy ustawień lektora`() {
        assertEquals("aktorski (wyraźne emocje)", LektorPrefs.styleLabel("acting"))
        assertEquals("klasyczny polski lektor", LektorPrefs.styleLabel("classic"))
        assertEquals("cały film przed startem", LektorPrefs.scriptLabel("whole"))
        assertEquals("porcjami po 10 min", LektorPrefs.scriptLabel("chunks"))
    }
}
