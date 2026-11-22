package com.nuvio.tv.fork.tv.index

import android.content.Context
import java.io.File

/**
 * Wszystkie trwałe pamięci indeksów TV w jednym miejscu (`filesDir/fork_tv_index`). [attach] przy starcie modułu TV
 * wczytuje je w tle jeden raz; liczenia w tle czekają na wczytanie ([awaitLoaded]), żeby nie liczyć od nowa tego, co
 * jest w pliku. Wersja w nazwie pliku = sposób liczenia; zmiana wersji (np. reguł kategorii) = jedno przeliczenie.
 */
object TvIndexStore {
    /** nazwa kanału → tekst do szukania (TvTextIndex.norm) */
    val names = DiskMemo("names", "1")
    /** nazwa kanału → klucz kanału (bez kraju/nawiasów/jakości) */
    val keys = DiskMemo("keys", "1")
    /** tytuł programu → tekst do szukania */
    val titles = DiskMemo("titles", "1")
    /** znormalizowany tytuł → klucze przewodnika (oddzielone \u0001) */
    val titleKeys = DiskMemo("titlekeys", "1")
    /** klucz przewodnika → skrót listy programów (porównanie po restarcie) */
    val fingerprints = DiskMemo("fingerprints", "1")
    /** nazwa kanału → kraj z nazwy ("" = brak) */
    val country = DiskMemo("country", "1", poolValues = true)
    /** nazwa kanału → kategorie (Kicia ForgeEpgCategories.channelPlaces); wersja = skrót reguł, ustawiana przez [placesMemo] */
    @Volatile private var places: DiskMemo? = null

    @Volatile private var dir: File? = null
    private val all get() = listOfNotNull(names, keys, titles, titleKeys, fingerprints, country, places)

    fun attach(c: Context) {
        if (dir != null) return
        val d = File(c.applicationContext.filesDir, "fork_tv_index")
        dir = d
        Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            all.forEach { it.load(d) }
        }, "fork-tv-index-load").apply { isDaemon = true }.start()
    }

    /** Pamięć kategorii kanałów dla danej wersji reguł (pierwsze wywołanie ją tworzy i — po [attach] — wczytuje). */
    @Synchronized fun placesMemo(rulesVersion: String): DiskMemo {
        places?.takeIf { it.version == rulesVersion }?.let { return it }
        val m = DiskMemo("places", rulesVersion, poolValues = true)
        places = m
        dir?.let { d -> Thread({ m.load(d) }, "fork-tv-places-load").apply { isDaemon = true }.start() }
        return m
    }

    /** Czeka na wczytanie wszystkich pamięci (tylko z wątków tła). Bez [attach] (testy) — od razu. */
    fun awaitLoaded(timeoutMs: Long = 15_000): Boolean = dir == null || all.all { it.awaitLoaded(timeoutMs) }

    /** Testy JVM: pamięci w podanym katalogu. */
    internal fun attachDirForTests(d: File) {
        dir = d
        all.forEach { it.load(d) }
    }
}
