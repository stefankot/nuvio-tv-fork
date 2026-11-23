package com.nuvio.tv.fork.tv.index

import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Trwała pamięć „klucz → wartość” na dysku (właściciel 22.11: „nie chcę, żeby to wszystko liczyło się w tle po każdym
 * starcie — może się liczyć tylko przy większej zmianie”). Indeksy TV (teksty nazw i tytułów, kategorie kanałów, kraj
 * z nazwy, spis tytułów) po starcie aplikacji są tylko WCZYTYWANE z pliku; liczone są wyłącznie nowe wpisy.
 *
 * Plik `<dir>/<name>.tsv.gz` (wiersz: klucz TAB wartość; TAB/nowa linia w tekście zamienione na spacje). Zapis po cichu,
 * [SAVE_DELAY_MS] po ostatniej zmianie i tylko gdy coś się zmieniło; przez plik tymczasowy (przerwany zapis nie psuje
 * poprzedniego). [version] w nazwie pliku: zmiana sposobu liczenia (np. reguł kategorii) = nowy plik = jedno przeliczenie.
 */
class DiskMemo(val name: String, val version: String = "1", private val poolValues: Boolean = false) {
    /** Wspólne obiekty powtarzających się wartości (kraj, kategorie: kilkaset różnych na setki tysięcy wpisów). */
    private val pool = if (poolValues) ConcurrentHashMap<String, String>() else null

    /** Ta sama wartość = ten sam obiekt (dla pamięci z [poolValues]). */
    fun pooled(v: String): String = pool?.putIfAbsent(v, v) ?: v

    val map = ConcurrentHashMap<String, String>()
    @Volatile var dirty = false
        private set
    @Volatile private var loadedLatch = CountDownLatch(1)
    @Volatile private var dir: File? = null

    val fileName: String get() = "${name}_v$version.tsv.gz"

    /** Wczytanie (w tle, raz); stare wersje pliku tej pamięci są usuwane. */
    @Synchronized fun load(dir: File) {
        if (this.dir != null) return
        this.dir = dir
        val t0 = System.nanoTime()
        try {
            dir.mkdirs()
            dir.listFiles()?.forEach { f -> if (f.name.startsWith(name + "_v") && f.name != fileName) { f.delete(); Log.i(TAG, "pamięć $name: usunięta stara wersja ${f.name}") } }
            val f = File(dir, fileName)
            if (f.isFile) {
                GZIPInputStream(f.inputStream().buffered(1 shl 16)).bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val tab = line.indexOf('\t')
                        if (tab > 0) map.putIfAbsent(line.substring(0, tab), pooled(line.substring(tab + 1)))
                    }
                }
            }
            Log.i(TAG, "pamięć $name: wczytane ${map.size} wpisów w ${(System.nanoTime() - t0) / 1_000_000} ms")
        } catch (e: Exception) {
            Log.w(TAG, "pamięć $name: odczyt: ${e.javaClass.simpleName}: ${e.message} — liczone od nowa")
            map.clear()
        } finally {
            loadedLatch.countDown()
        }
    }

    /** Czeka (najwyżej [timeoutMs]) na wczytanie — żeby liczenie w tle nie liczyło od nowa tego, co jest w pliku. */
    fun awaitLoaded(timeoutMs: Long = 15_000): Boolean = dir == null || loadedLatch.await(timeoutMs, TimeUnit.MILLISECONDS)

    operator fun get(key: String): String? = map[key]

    fun put(key: String, value: String) {
        if (map.put(key, pooled(value)) != value) { dirty = true; Saver.schedule(this) }
    }

    fun remove(key: String) {
        if (map.remove(key) != null) { dirty = true; Saver.schedule(this) }
    }

    fun clear() { if (map.isNotEmpty()) { map.clear(); dirty = true; Saver.schedule(this) } }

    /**
     * Usuwa wpisy spoza [keep] (kanały, których nie ma już na żadnej liście), ale tylko przy większej zmianie: zbędnych
     * co najmniej [minStale] i co najmniej 1/5 pamięci — chwilowy brak jednej listy nie kasuje policzonych wpisów.
     * Zwraca liczbę usuniętych (0 = bez zmian).
     */
    fun retainOnly(keep: Set<String>, minStale: Int = 5_000): Int {
        if (dir != null && loadedLatch.count > 0L) return 0 // jeszcze się wczytuje
        var stale = 0
        for (k in map.keys) if (k !in keep) stale++
        if (stale < minStale || stale * 5 < map.size) return 0
        val it = map.keys.iterator()
        while (it.hasNext()) if (it.next() !in keep) it.remove()
        dirty = true
        Saver.schedule(this)
        Log.i(TAG, "pamięć $name: usunięte $stale wpisów kanałów, których już nie ma (zostaje ${map.size})")
        return stale
    }

    /**
     * 23.11 (TvRam): wyjście z pamięci po opuszczeniu telewizji — zmiany zapisane, mapa pusta, plik zostaje; następne
     * [load] wczytuje go od nowa. Tylko z wątku tła.
     */
    @Synchronized fun unload() {
        if (dir == null) return
        if (dirty) saveNow()
        map.clear()
        pool?.clear()
        dir = null
        loadedLatch = CountDownLatch(1)
    }

    /** Zapis teraz (wątek zapisu albo testy). */
    @Synchronized fun saveNow(): Boolean {
        val d = dir ?: return false
        if (!dirty) return false
        dirty = false
        val t0 = System.nanoTime()
        val tmp = File(d, "$fileName.tmp")
        return try {
            GZIPOutputStream(tmp.outputStream().buffered(1 shl 16)).bufferedWriter().use { w ->
                for ((k, v) in map) {
                    w.write(clean(k)); w.write('\t'.code); w.write(clean(v)); w.write('\n'.code)
                }
            }
            if (!tmp.renameTo(File(d, fileName))) throw java.io.IOException("nie udało się zapisać $fileName")
            Log.i(TAG, "pamięć $name: zapisane ${map.size} wpisów w ${(System.nanoTime() - t0) / 1_000_000} ms")
            true
        } catch (e: Exception) {
            dirty = true
            tmp.delete()
            Log.w(TAG, "pamięć $name: zapis: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }

    private fun clean(s: String) = if ('\t' in s || '\n' in s || '\r' in s) s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ') else s

    private object Saver {
        private val pending = ConcurrentHashMap.newKeySet<DiskMemo>()
        private val exec = Executors.newSingleThreadScheduledExecutor { r ->
            Thread({ android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND); r.run() }, "fork-tv-memo").apply { isDaemon = true }
        }
        @Volatile private var scheduled = false

        fun schedule(m: DiskMemo) {
            if (m.dir == null) return
            pending += m
            if (scheduled) return
            scheduled = true
            exec.schedule({
                scheduled = false
                val list = pending.toList(); pending.clear()
                list.forEach { it.saveNow() }
            }, SAVE_DELAY_MS, TimeUnit.MILLISECONDS)
        }
    }

    companion object {
        private const val TAG = "ForkTvIndex"
        const val SAVE_DELAY_MS = 20_000L
    }
}
