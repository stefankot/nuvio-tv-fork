package com.nuvio.tv.fork.asr

import android.util.Log
import com.nuvio.tv.ui.screens.player.audiosync.asr.AsrModelChoice
import com.nuvio.tv.ui.screens.player.audiosync.asr.AsrModelFiles
import java.io.File

/**
 * Fork 26.11 (test VOD na JMGO, `uxr17`): „The Last of Us” S1 O1 w 4K — dopasowanie polskich napisów do mowy wczytywało
 * model Whisper base, `malloc(200 MB)` w 32-bitowym procesie zawiódł (`std::bad_alloc` w sherpa-onnx, wyjątek C++ bez
 * obsługi → SIGABRT) i cała aplikacja padła w trakcie filmu. Odtwarzanie 4K zajmuje dużą część przestrzeni adresów procesu.
 *
 * Przed wczytaniem modelu sprawdzamy największy wolny ciągły obszar adresów (`/proc/self/maps`). Gdy wybrany model się
 * nie zmieści: mniejszy pobrany model (Whisper tiny, potem angielski zipformer). Gdy żaden — wyjątek Kotlin, który
 * sterownik dopasowania łapie („speech recognizer unavailable”) i zostaje przy samym wykrywaniu mowy. Procesy 64-bitowe
 * i nieczytelne `/proc/self/maps`: bez zmian.
 */
object AsrMemoryGuard {
    private const val TAG = "ForkAsr"
    private const val MB = 1024L * 1024L

    /** Ile ciągłej pamięci trzeba na wczytanie modelu z [dir]: 2 × największy plik modelu (base: plik 131 MB, malloc 200 MB). */
    internal fun need(dir: File): Long {
        val biggest = dir.listFiles()?.filter { it.name.endsWith(".onnx") || it.name.endsWith(".ort") }?.maxOfOrNull { it.length() } ?: 0L
        return maxOf(2 * biggest, 64 * MB)
    }

    /**
     * Największa przerwa między obszarami z `/proc/<pid>/maps` (posortowane adresy). Liczy tylko do stosu (`[stack]`):
     * obszar tuż pod stosem jest zarezerwowany na jego wzrost, a powyżej jest jądro (`[vectors]`).
     */
    internal fun largestGap(lines: Sequence<String>): Long? {
        var prevEnd = -1L
        var best = 0L
        var any = false
        for (line in lines) {
            val dash = line.indexOf('-')
            val space = line.indexOf(' ')
            if (dash <= 0 || space <= dash) continue
            val start = line.substring(0, dash).toLongOrNull(16) ?: continue
            val end = line.substring(dash + 1, space).toLongOrNull(16) ?: continue
            if (line.endsWith("[stack]") || line.endsWith("[vectors]") || start >= 0xFFFF0000L) break
            if (prevEnd >= 0 && start > prevEnd) best = maxOf(best, start - prevEnd)
            prevEnd = maxOf(prevEnd, end)
            any = true
        }
        return if (any) best else null
    }

    /** Mniejsze modele, które są już pobrane (od największego), gdy [requested] się nie zmieści. */
    private fun smaller(requested: File): List<File> {
        val parent = requested.parentFile ?: return emptyList()
        val chosen = AsrModelChoice.entries.firstOrNull { it.id == requested.name } ?: return emptyList() // angielski: najmniejszy
        val whispers = AsrModelChoice.entries
            .filter { it.whisperSize != null && it.sizeMb < chosen.sizeMb }
            .sortedByDescending { it.sizeMb }
            .map { File(parent, it.id) to it.files }
        val english = parent.listFiles()?.filter { it.isDirectory }?.map { it to AsrModelChoice.ENGLISH.files }.orEmpty()
        return (whispers + english).filter { (dir, files) -> AsrModelFiles.isComplete(dir, files) }.map { it.first }
    }

    /** Katalog modelu do wczytania: [requested], gdy się zmieści, inaczej mniejszy; bez miejsca na żaden — wyjątek. */
    fun pick(requested: File): File {
        if (android.os.Process.is64Bit()) return requested
        val gap = try {
            File("/proc/self/maps").useLines { largestGap(it) }
        } catch (e: Exception) {
            Log.w(TAG, "model mowy: nie odczytam mapy pamięci: ${e.javaClass.simpleName}: ${e.message}")
            null
        } ?: return requested
        for (dir in listOf(requested) + smaller(requested)) {
            val n = need(dir)
            if (gap >= n) {
                if (dir == requested) Log.i(TAG, "model mowy ${dir.name}: wolne ${gap / MB} MB ciągłej pamięci, potrzeba ${n / MB} MB — wczytuję")
                else Log.w(TAG, "model mowy ${requested.name} się nie zmieści (wolne ${gap / MB} MB, potrzeba ${need(requested) / MB} MB) — wczytuję mniejszy ${dir.name}")
                return dir
            }
        }
        Log.w(TAG, "model mowy ${requested.name}: wolne tylko ${gap / MB} MB ciągłej pamięci, potrzeba ${need(requested) / MB} MB — bez rozpoznawania mowy (zostaje wykrywanie mowy)")
        throw IllegalStateException("za mało pamięci na model mowy (wolne ${gap / MB} MB)")
    }
}
