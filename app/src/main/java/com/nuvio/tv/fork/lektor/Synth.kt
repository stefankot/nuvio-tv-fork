package com.nuvio.tv.fork.lektor

import java.io.File

/** Wynik syntezy: PCM 16-bit mono i jego długość. */
class Pcm(val samples: ShortArray, val sampleRate: Int) { val durationMs: Long get() = samples.size * 1000L / sampleRate }

/** Silnik mowy lektora. Brak polskiego głosu → [Unavailable] (moduł proponuje napisy). */
interface Lektor {
    class Unavailable(val reason: String) : Exception(reason)
    suspend fun synthesize(text: String, rate: Float): Pcm
    /** Polskie głosy urządzenia (nazwy). */
    fun voices(): List<String>
}

/** Okno syntezy naprzód i pamięć na dysku (B09-4). */
object SynthWindow {
    const val AHEAD_MS = 10 * 60_000L // fork 19.11 (właściciel: „z dużym wyprzedzeniem”): było 3 min

    /** Kwestie do zsyntezowania od pozycji [positionMs]: te w oknie [AHEAD_MS], jeszcze niezrobione. */
    fun pending(lines: List<Line>, positionMs: Long, done: Set<Long>): List<Line> =
        lines.filter { it.startMs >= positionMs - 500 && it.startMs <= positionMs + AHEAD_MS && it.startMs !in done }.sortedBy { it.startMs }

    /** Przewinięcie: okno przesuwa się; zwraca ile kwestii wypadło poza nowe okno. */
    fun dropOutside(done: Set<Long>, positionMs: Long): Set<Long> = done.filter { it >= positionMs - 500 && it <= positionMs + AHEAD_MS }.toSet()

    /** Kwestia niegotowa na czas jest pomijana (napis zostaje widoczny). */
    fun shouldSkip(ready: Boolean, lateByMs: Long) = !ready || lateByMs > 500
}

/** Pamięć na dysku na tytuł+plik napisów; limit [limitBytes] (200 MB), najstarsze wypadają. */
class SynthCache(private val dir: File, private val limitBytes: Long = 200L * 1024 * 1024) {
    fun file(titleKey: String, lineStartMs: Long) = File(dir, "${titleKey.hashCode().toUInt().toString(16)}_$lineStartMs.pcm")

    fun put(titleKey: String, lineStartMs: Long, bytes: ByteArray) {
        dir.mkdirs(); file(titleKey, lineStartMs).writeBytes(bytes); trim()
    }

    fun get(titleKey: String, lineStartMs: Long): ByteArray? = file(titleKey, lineStartMs).takeIf { it.isFile }?.readBytes()?.also { file(titleKey, lineStartMs).setLastModified(System.currentTimeMillis()) }

    fun sizeBytes() = dir.listFiles()?.sumOf { it.length() } ?: 0L

    fun trim() {
        var total = sizeBytes()
        val files = dir.listFiles()?.sortedBy { it.lastModified() }.orEmpty()
        for (f in files) { if (total <= limitBytes) break; total -= f.length(); f.delete() }
    }

    fun clear() { dir.listFiles()?.forEach { it.delete() } }
}
