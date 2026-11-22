package com.nuvio.tv.fork.lektor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class BandDuckTest {
    private fun rms(hz: Double, frac: Float, depth: Float = -6f, night: Float = 0f, ch: Int = 2, channel: Int = 0): Double {
        val b = BandDuck().apply { configure(48_000, ch); setDepth(depth, night) }
        var sum = 0.0; var n = 0
        for (i in 0 until 48_000) {
            val x = sin(2 * PI * hz * i / 48_000).toFloat() * 0.5f
            var y = 0f
            for (c in 0 until ch) { val v = b.process(x, c, frac); if (c == channel) y = v }
            if (i > 4_800) { sum += y * y; n++ }
        }
        return sqrt(sum / n)
    }

    @Test fun `bez lektora dzwiek bez zmian`() {
        val b = BandDuck().apply { configure(48_000, 2); setDepth(-6f, 4f) }
        for (i in 0 until 2_000) {
            val x = sin(i * 0.37).toFloat() * 0.8f
            assertEquals(x, b.process(x, 0, 0f), 1e-5f)
        }
    }

    @Test fun `mowa sciszona mocniej niz bas i gora`() {
        val ref = 0.5 / sqrt(2.0)
        val voice = rms(1_000.0, 1f) / ref
        val bass = rms(60.0, 1f) / ref
        val treble = rms(10_000.0, 1f) / ref
        assertTrue("mowa $voice < bas $bass", voice < bass - 0.15)
        assertTrue("mowa $voice < góra $treble", voice < treble - 0.15)
        assertTrue("bas $bass, góra $treble", bass > 0.65 && treble > 0.65)
    }

    @Test fun `tryb nocny sciszaja mowe dodatkowo, kanal centralny 5_1`() {
        assertTrue(rms(1_000.0, 1f, night = BandDuck.nightExtraDb("medium")) < rms(1_000.0, 1f) * 0.8)
        val b = BandDuck().apply { configure(48_000, 6); setDepth(-6f, 0f) }
        assertEquals(0.5f * b.midFloor, b.process(0.5f, 2, 1f), 1e-6f)
        assertEquals(0.5f * b.sideFloor, b.process(0.5f, 3, 1f), 1e-6f)
        assertEquals(0f, BandDuck.nightExtraDb("off"), 0f)
        assertTrue(abs(BandDuck.dbToLinear(-6f) - 0.501f) < 0.01f)
    }
}
