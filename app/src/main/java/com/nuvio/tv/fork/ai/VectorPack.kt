package com.nuvio.tv.fork.ai

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/** Cechy pozycji w paczce. */
data class ItemFeatures(val year: Int, val genreMask: Int, val rating10: Int, val votes: Int, val runtimeMin: Int)

/** Pozycja do zapisu: identyfikator TMDB, typ (0 film, 1 serial), wektor jednostkowy, cechy. */
data class PackItem(val tmdbId: Int, val type: Int, val vector: FloatArray, val features: ItemFeatures)

class PackMismatch(message: String) : Exception(message)

/**
 * Plik binarny: nagłówek (magic, nazwa modelu, wersja, wymiar, liczba), tablica id+typ, wektory 8-bitowe, tablica cech.
 * Odczyt przez mapowanie pliku w pamięć. Paczka z innym modelem niż zainstalowany jest odrzucana.
 */
object VectorPack {
    private const val MAGIC = 0x4E565043 // "NVPC"
    private const val FEATURE_BYTES = 2 + 4 + 1 + 4 + 2

    fun write(file: File, model: String, version: Int, dim: Int, items: List<PackItem>) {
        val name = model.toByteArray(Charsets.UTF_8)
        val headerSize = 4 + 2 + name.size + 4 + 4 + 4
        val total = headerSize + items.size * 5 + items.size * dim + items.size * FEATURE_BYTES
        val buf = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(MAGIC).putShort(name.size.toShort()).put(name).putInt(version).putInt(dim).putInt(items.size)
        items.forEach { buf.putInt(it.tmdbId).put(it.type.toByte()) }
        items.forEach { it.vector.forEachIndexed { i, x -> if (i < dim) buf.put(VectorMath.quantByte(x)) } }
        items.forEach { f ->
            val x = f.features
            buf.putShort(x.year.toShort()).putInt(x.genreMask).put(x.rating10.toByte()).putInt(x.votes).putShort(x.runtimeMin.toShort())
        }
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp"); tmp.writeBytes(buf.array())
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }

    class Reader internal constructor(private val buf: ByteBuffer, val model: String, val version: Int, val dim: Int, val count: Int, private val idsAt: Int) {
        private val vecAt = idsAt + count * 5
        private val featAt = vecAt + count * dim
        fun tmdbId(i: Int) = buf.getInt(idsAt + i * 5)
        fun type(i: Int) = buf.get(idsAt + i * 5 + 4).toInt()
        fun vector(i: Int): ByteArray = ByteArray(dim).also { out -> for (j in 0 until dim) out[j] = buf.get(vecAt + i * dim + j) }
        fun features(i: Int): ItemFeatures {
            val p = featAt + i * FEATURE_BYTES
            return ItemFeatures(buf.getShort(p).toInt(), buf.getInt(p + 2), buf.get(p + 6).toInt(), buf.getInt(p + 7), buf.getShort(p + 11).toInt())
        }

        /** Kopia wszystkich wektorów w płaskiej tablicy (do przeszukania liniowego). */
        fun allVectors(): ByteArray = ByteArray(count * dim).also { out -> for (k in out.indices) out[k] = buf.get(vecAt + k) }
    }

    /** Otwiera plik przez mapowanie w pamięć; [expectedModel] i [expectedDim] muszą się zgadzać. */
    fun open(file: File, expectedModel: String, expectedDim: Int): Reader {
        val mapped = java.io.RandomAccessFile(file, "r").use { raf -> raf.channel.map(FileChannel.MapMode.READ_ONLY, 0, raf.length()) }
        val buf = mapped.order(ByteOrder.LITTLE_ENDIAN)
        if (buf.capacity() < 18 || buf.getInt(0) != MAGIC) throw PackMismatch("To nie jest paczka wektorów")
        val n = buf.getShort(4).toInt()
        val model = String(ByteArray(n) { buf.get(6 + it) }, Charsets.UTF_8)
        var p = 6 + n
        val version = buf.getInt(p); val dim = buf.getInt(p + 4); val count = buf.getInt(p + 8); p += 12
        if (model != expectedModel) throw PackMismatch("Paczka liczona innym modelem ($model)")
        if (dim != expectedDim) throw PackMismatch("Inny wymiar wektorów ($dim)")
        return Reader(buf, model, version, dim, count, p)
    }
}
