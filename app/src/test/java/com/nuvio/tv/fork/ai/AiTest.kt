package com.nuvio.tv.fork.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Random
import kotlin.math.abs
import kotlin.math.sqrt

class AiTest {
    private fun rnd(r: Random, n: Int = 768) = FloatArray(n) { r.nextGaussian().toFloat() }

    @Test fun `normalizacja i obciecie do 128`() {
        val v = VectorMath.truncateNormalize(rnd(Random(1)))
        assertEquals(128, v.size); assertEquals(1.0, sqrt(v.sumOf { (it * it).toDouble() }), 1e-4)
    }

    @Test fun `kwantyzacja zachowuje podobienstwo w granicach 0,02`() {
        val r = Random(2)
        repeat(50) {
            val a = VectorMath.truncateNormalize(rnd(r)); val b = VectorMath.truncateNormalize(rnd(r))
            val before = VectorMath.cosine(a, b)
            val after = VectorMath.cosine(VectorMath.dequantize(VectorMath.quantize(a)), VectorMath.dequantize(VectorMath.quantize(b)))
            assertTrue("$before vs $after", abs(before - after) < 0.02f)
        }
    }

    @Test fun `przeszukanie 50000 wektorow ponizej 50 ms`() {
        val r = Random(3); val n = 50_000
        val all = ByteArray(n * 128) { (r.nextInt(255) - 127).toByte() }
        val q = VectorMath.truncateNormalize(rnd(r))
        VectorMath.nearest(q, all, n, 128, 10) // rozgrzewka JIT
        val t0 = System.nanoTime(); val hits = VectorMath.nearest(q, all, n, 128, 10); val ms = (System.nanoTime() - t0) / 1e6
        assertEquals(10, hits.size); assertTrue("czas $ms ms", ms < 50.0)
        assertTrue(hits.zipWithNext().all { it.first.score >= it.second.score })
    }

    @Test fun `paczka zapis odczyt i odrzucenie niezgodnej`() {
        val r = Random(4); val f = File(Files.createTempDirectory("pack").toFile(), "p.bin")
        val items = (1..20).map { PackItem(100 + it, it % 2, VectorMath.truncateNormalize(rnd(r)), ItemFeatures(1990 + it, 1 shl (it % 10), 70 + it, 1000 * it, 90 + it)) }
        VectorPack.write(f, "embeddinggemma-300m-q", 3, 128, items)
        val rd = VectorPack.open(f, "embeddinggemma-300m-q", 128)
        assertEquals(20, rd.count); assertEquals(105, rd.tmdbId(4)); assertEquals(1, rd.type(0)); assertEquals(items[7].features, rd.features(7))
        assertEquals(VectorMath.quantByte(items[3].vector[5]), rd.vector(3)[5])
        val bad = runCatching { VectorPack.open(f, "inny-model", 128) }.exceptionOrNull()
        assertTrue(bad is PackMismatch); assertTrue(runCatching { VectorPack.open(f, "embeddinggemma-300m-q", 64) }.exceptionOrNull() is PackMismatch)
        val junk = File(f.parentFile, "junk.bin").also { it.writeBytes(ByteArray(40)) }
        assertTrue(runCatching { VectorPack.open(junk, "x", 1) }.exceptionOrNull() is PackMismatch)
    }

    @Test fun `zarzadca cyklu zycia`() {
        var now = 0L; var playing = false; var free = 1000; var loads = 0; var released = 0
        val m = ModelLifecycle(clock = { now }, playing = { playing }, freeMemoryMb = { free }, loader = { loads++; "model" }, onRelease = { released++ })
        assertEquals("model", m.acquire()); assertEquals("model", m.acquire()); assertEquals(1, loads)
        now += 30_000; m.tick(); assertTrue(m.loaded)
        now += 31_000; m.tick(); assertFalse(m.loaded); assertEquals(1, released)
        playing = true; assertTrue(runCatching { m.acquire() }.exceptionOrNull() is ModelLifecycle.Unavailable)
        playing = false; free = 300; assertTrue(runCatching { m.acquire() }.exceptionOrNull() is ModelLifecycle.Unavailable)
        free = 900; m.acquire(); playing = true; m.tick(); assertFalse(m.loaded)
    }

    @Test fun `wybor modelu wg pamieci`() {
        assertEquals(EmbedModel.E5_SMALL, ModelPicker.forSpeech(2000)); assertEquals(EmbedModel.GEMMA, ModelPicker.forSpeech(4000)); assertEquals(EmbedModel.GEMMA, ModelPicker.forMovies())
    }
}
