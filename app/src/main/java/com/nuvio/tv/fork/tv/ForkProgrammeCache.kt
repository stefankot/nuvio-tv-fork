package com.nuvio.tv.skin.kicia.livetv

import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nuvio.tv.fork.logged

/**
 * Fork 19.11 (właściciel: „programy w kategoriach ładują się bardzo długo, jakby nie były cachowane”).
 * Kicia liczyła drzewo programów i listę kategorii przy każdym wejściu od nowa (przegląd całego EPG, ~100 tys. emisji),
 * drzewo w wątku głównym. Tu: pamięć wyników (ważna do zmiany przewodnika, kategorii albo 5-minutowego okna) i liczenie
 * zawczasu w tle (drzewo + „Wszystkie programy” + każda gałąź) po wejściu w Live TV. Kod Kicii bez zmian logiki.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal object ForkProgrammeCache {
    private val worker = Dispatchers.Default.limitedParallelism(1) // jeden wątek: rozgrzewka nie dławi przewijania

    private fun sig(channels: List<LiveTvChannel>, guideVersion: Int) =
        "${System.identityHashCode(channels)}:${channels.size}|$guideVersion|${ForgeEpgCategories.programmeVersion()}|${LiveTvClock.nowEpochMs() / 300_000}"

    @Volatile private var treeSig = ""
    @Volatile private var tree: List<ForgeEpgCategories.Node> = emptyList()

    /** Drzewo programów: z pamięci albo policzone teraz (zapamiętane). */
    fun treeNow(channels: List<LiveTvChannel>, guideVersion: Int): List<ForgeEpgCategories.Node> {
        val s = sig(channels, guideVersion)
        if (treeSig == s) return tree
        val t = ForgeEpgCategories.programmeTree(channels)
        tree = t; treeSig = s
        return t
    }

    fun treeOrNull(channels: List<LiveTvChannel>, guideVersion: Int): List<ForgeEpgCategories.Node>? =
        if (treeSig == sig(channels, guideVersion)) tree else null

    suspend fun tree(channels: List<LiveTvChannel>, guideVersion: Int) =
        treeOrNull(channels, guideVersion) ?: withContext(worker) { treeNow(channels, guideVersion) }

    private val entries = object : LinkedHashMap<String, Entries>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entries>?) = size > 40
    }

    private fun entryKey(channels: List<LiveTvChannel>, guideVersion: Int, key: String, sort: ForgeProgrammeSort, phrase: String) =
        sig(channels, guideVersion) + "|" + key + "|" + sort.name + "|" + phrase +
            (if (key == com.nuvio.tv.skin.kicia.livetv.FORGE_PROG_FAVOURITES) "|" + com.nuvio.tv.fork.tv.TvStore.favoriteShows.value.hashCode() else "") // zmiana ulubionych = nowa lista

    fun entriesOrNull(channels: List<LiveTvChannel>, guideVersion: Int, key: String, sort: ForgeProgrammeSort, phrase: String): Entries? =
        synchronized(entries) { entries[entryKey(channels, guideVersion, key, sort, phrase)] }

    suspend fun entries(channels: List<LiveTvChannel>, guideVersion: Int, key: String, sort: ForgeProgrammeSort, phrase: String): Entries {
        val k = entryKey(channels, guideVersion, key, sort, phrase)
        synchronized(entries) { entries[k] }?.let { return it }
        val e = withContext(Dispatchers.Default) {
            runCatching { buildEntries(channels, key, LiveTvClock.nowEpochMs(), sort, phrase) }.logged("ForkTv", "ForkProgrammeCache.kt:58").getOrDefault(Entries(emptyList(), 0))
        }
        synchronized(entries) { entries[k] = e }
        return e
    }

    private var warm: Job? = null

    /** Po wejściu w Live TV i po każdej zmianie przewodnika: drzewo i listy gałęzi liczone zawczasu, po kolei, w tle. */
    fun prewarm(scope: CoroutineScope, channels: List<LiveTvChannel>, guideVersion: Int) {
        if (channels.isEmpty()) return
        warm?.cancel()
        warm = scope.launch(worker) {
            val t = treeNow(channels, guideVersion)
            val sort = ForgeProgrammeView.sort
            (listOf(ForgeEpgCategories.PROG_ALL, com.nuvio.tv.skin.kicia.livetv.FORGE_PROG_FAVOURITES) + t.map { it.key }).forEach { key -> // 19.11: Ulubione też zawczasu
                val k = entryKey(channels, guideVersion, key, sort, "")
                if (synchronized(entries) { entries.containsKey(k) }) return@forEach
                val e = runCatching { buildEntries(channels, key, LiveTvClock.nowEpochMs(), sort, "") }.logged("ForkTv", "ForkProgrammeCache.kt:76").getOrNull() ?: return@forEach
                synchronized(entries) { entries[k] = e }
            }
        }
    }
}
