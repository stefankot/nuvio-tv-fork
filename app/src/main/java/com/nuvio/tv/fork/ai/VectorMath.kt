package com.nuvio.tv.fork.ai

import kotlin.math.sqrt

/** Model osadzeń: zwraca wektory dla tekstów; gdy niedostępny, rzuca [EmbedderUnavailable] (moduły wracają do reguł). */
interface Embedder {
    suspend fun embed(texts: List<String>): List<FloatArray>
}

class EmbedderUnavailable(message: String = "Model osadzeń niedostępny") : Exception(message)

/** Wektory 8-bitowe: wartości w [-127, 127] ze wspólną skalą 1/127 (wektor jednostkowy). */
class QuantizedVector(val data: ByteArray)

object VectorMath {
    const val DIM = 128

    /** Obcina do [dim] pierwszych wymiarów i normalizuje do długości 1 (matryoszka). */
    fun truncateNormalize(v: FloatArray, dim: Int = DIM): FloatArray {
        val n = minOf(dim, v.size)
        val out = v.copyOf(n)
        var s = 0.0
        for (x in out) s += x * x
        val norm = sqrt(s).toFloat()
        if (norm > 0f) for (i in out.indices) out[i] /= norm
        return out
    }

    fun quantByte(x: Float): Byte = Math.round(x * 127f).coerceIn(-127, 127).toByte()

    fun quantize(unit: FloatArray): QuantizedVector = QuantizedVector(ByteArray(unit.size) { quantByte(unit[it]) })

    fun dequantize(q: QuantizedVector): FloatArray = FloatArray(q.data.size) { q.data[it] / 127f }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0.0; var na = 0.0; var nb = 0.0
        for (i in 0 until minOf(a.size, b.size)) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
        return if (na == 0.0 || nb == 0.0) 0f else (dot / (sqrt(na) * sqrt(nb))).toFloat()
    }

    /** Iloczyn skalarny zapytania (float, jednostkowego) z wektorem 8-bitowym zapisanym w tablicy [all] od [offset]; skala 1/127. */
    fun dotQuantized(query: FloatArray, all: ByteArray, offset: Int): Float {
        var s = 0f
        for (i in query.indices) s += query[i] * all[offset + i]
        return s / 127f
    }

    data class Hit(val index: Int, val score: Float)

    /** Liniowe wyszukiwanie [k] najbliższych w płaskiej tablicy [count] wektorów po [dim] bajtów. */
    fun nearest(query: FloatArray, all: ByteArray, count: Int, dim: Int = query.size, k: Int = 10): List<Hit> {
        val heap = java.util.PriorityQueue<Hit>(k + 1, compareBy { it.score })
        for (i in 0 until count) {
            val sc = dotQuantized(query, all, i * dim)
            if (heap.size < k) heap.add(Hit(i, sc)) else if (sc > heap.peek().score) { heap.poll(); heap.add(Hit(i, sc)) }
        }
        return heap.sortedByDescending { it.score }
    }
}
