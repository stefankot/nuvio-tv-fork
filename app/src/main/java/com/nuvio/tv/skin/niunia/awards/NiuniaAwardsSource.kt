package com.nuvio.tv.skin.niunia.awards

import android.util.Log
import kotlinx.coroutines.CancellationException

/**
 * Z-13: skąd brać nagrody tytułu. Aplikacja nie ma dziś źródła nagród z galą, kategorią i wynikiem (TMDB ich nie ma,
 * `Meta.awards` z dodatków to tylko zdanie w stylu OMDb, bez kategorii). Wybór źródła należy do właściciela
 * (plan/BLOKADY.md, „Z-13 źródło nagród”) — do tego czasu [NoAwardsSource]: pasek się nie rysuje, lista pokazuje stan pusty.
 */
interface NiuniaAwardsSource {
    /** Nazwa do logów. */
    val name: String

    /** Nagrody tytułu ([id] = id Nuvio, np. „tt0050083” albo „tmdb:123”). Pusta lista = brak nagród. */
    suspend fun awards(id: String, type: String, imdbId: String?): List<NiuniaAward>
}

/** Brak źródła: zawsze pusta lista (nic nie pobiera, nic nie kosztuje). */
object NoAwardsSource : NiuniaAwardsSource {
    override val name = "brak źródła"
    override suspend fun awards(id: String, type: String, imdbId: String?): List<NiuniaAward> = emptyList()
}

/** Nagrody dla ekranów Niuni: jedno źródło, pamięć ostatnich tytułów (pasek, lista i plakietki plakatów czytają to samo). */
object NiuniaAwardsRepo {
    @Volatile var source: NiuniaAwardsSource = NoAwardsSource

    private const val MAX = 200
    private val cache = object : LinkedHashMap<String, List<NiuniaAward>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<NiuniaAward>>?) = size > MAX
    }

    /** Nagrody tytułu; błąd źródła = pusta lista + log (typ i komunikat). */
    suspend fun load(id: String, type: String, imdbId: String?): List<NiuniaAward> {
        synchronized(cache) { cache[id] }?.let { return it }
        val src = source
        val list = try {
            src.awards(id, type, imdbId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(AWARDS_TAG, "NAwards: źródło „${src.name}” dla $id: ${e.javaClass.simpleName}: ${e.message}")
            return emptyList()
        }
        Log.i(AWARDS_TAG, "NAwards: $id ($type): ${list.size} pozycji ze źródła „${src.name}”")
        synchronized(cache) { cache[id] = list }
        return list
    }

    /** Nagrody już wczytane (plakietki plakatów nie pobierają same); null = jeszcze nie wiadomo. */
    fun cached(id: String): List<NiuniaAward>? = synchronized(cache) { cache[id] }
}
