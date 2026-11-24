package com.nuvio.tv.skin.niunia.search

import android.content.Context
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

private const val TAG = "ForkNiunia"

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface NiuniaSearchFixEntryPoint {
    fun tmdbApi(): com.nuvio.tv.data.remote.api.TmdbApi
    fun tmdbService(): com.nuvio.tv.core.tmdb.TmdbService
}

/**
 * Tytuły, które skórka już widziała (rzędy Głównej, „Oglądaj dalej”, biblioteka, wyniki i podpowiedzi Szukaj).
 * Słownik dla poprawiania literówek (Z-19, krok 3); tylko w pamięci procesu, najwyżej [MAX] tytułów.
 */
object NiuniaKnownTitles {
    private const val MAX = 6000
    private val titles = java.util.Collections.synchronizedSet(LinkedHashSet<String>())

    fun add(names: Iterable<String?>) {
        var n = 0
        for (raw in names) {
            val t = raw?.trim()?.takeIf { it.length >= 2 } ?: continue
            if (titles.add(t)) n++
        }
        if (n > 0) synchronized(titles) { while (titles.size > MAX) titles.remove(titles.first()) }
    }

    fun all(): List<String> = synchronized(titles) { titles.toList() }
}

/** Krok poprawki: numer (1–5 jak w Z-19) i zapytanie do wysłania. */
internal data class NiuniaFixStep(val step: Int, val query: String)

/**
 * Z-19 (P-19): poprawianie zapytania, gdy dodatki zwróciły 0 wyników. Kolejno: (1) bez polskich znaków i wielkości liter,
 * (2+3) słowa poprawione do najbliższych słów ze znanych tytułów (sąsiednie klawisze klawiatury ekranowej 6 × N i QWERTY
 * kosztują 0,5, inne zmiany 1; odległość ≤ 2), (4) TMDB `search/multi` (tytuł pierwszego trafienia), (5) bez ostatniego słowa.
 */
internal object NiuniaSearchFix {
    private val near: Map<Char, Set<Char>> = run {
        val m = HashMap<Char, MutableSet<Char>>()
        fun link(a: Char, b: Char) { m.getOrPut(a) { mutableSetOf() }.add(b); m.getOrPut(b) { mutableSetOf() }.add(a) }
        fun grid(rows: List<String>, stagger: Boolean) {
            for (r in rows.indices) for (c in rows[r].indices) {
                val ch = rows[r][c]
                rows[r].getOrNull(c + 1)?.let { link(ch, it) }
                val below = rows.getOrNull(r + 1) ?: continue
                for (cc in if (stagger) listOf(c - 1, c) else listOf(c)) below.getOrNull(cc)?.let { link(ch, it) }
            }
        }
        grid(listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"), stagger = true)
        grid("abcdefghijklmnopqrstuvwxyz".chunked(6), stagger = false) // klawiatura ekranowa Niuni (6 w rzędzie)
        m
    }

    fun fold(s: String): String = Normalizer.normalize(s.lowercase().replace('ł', 'l'), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9 ]+"), " ").replace(Regex("\\s+"), " ").trim()

    private fun cost(a: Char, b: Char): Double = when { a == b -> 0.0; near[a]?.contains(b) == true -> 0.5; else -> 1.0 }

    /** Odległość edycji z tańszą zamianą sąsiednich klawiszy i przestawieniem dwóch liter (0,5). */
    fun distance(a: String, b: String): Double {
        val d = Array(a.length + 1) { DoubleArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i.toDouble()
        for (j in 0..b.length) d[0][j] = j.toDouble()
        for (i in 1..a.length) for (j in 1..b.length) {
            var v = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost(a[i - 1], b[j - 1]))
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = minOf(v, d[i - 2][j - 2] + 0.5)
            d[i][j] = v
        }
        return d[a.length][b.length]
    }

    private fun limit(len: Int): Double = minOf(2.0, maxOf(1.0, len / 2.0))

    /** Kroki 1–3 i 5 (bez sieci). [tried] = zapytania już wysłane (bez powtórek). */
    fun localSteps(query: String, titles: List<String>): List<NiuniaFixStep> {
        val out = ArrayList<NiuniaFixStep>()
        val q = query.trim()
        val folded = fold(q)
        if (folded.isNotBlank() && folded != q) out += NiuniaFixStep(1, folded)
        val foldedTitles = titles.map { it to fold(it) }.filter { it.second.isNotBlank() }
        // Cały tytuł blisko zapytania (≤ 2) — wygrywa najbliższy, przy remisie krótszy.
        foldedTitles.map { (t, f) -> Triple(t, f, distance(folded, f)) }
            .filter { it.third <= limit(folded.length) }
            .minWithOrNull(compareBy<Triple<String, String, Double>> { it.third }.thenBy { it.second.length })
            ?.let { out += NiuniaFixStep(3, it.first) }
        // Słowo po słowie do słownika słów znanych tytułów (sąsiednie klawisze = krok 2, reszta = krok 3).
        val vocab = foldedTitles.flatMap { it.second.split(' ') }.filter { it.length >= 2 }.toHashSet()
        if (vocab.isNotEmpty()) {
            var neighbourOnly = true
            var changed = false
            val words = folded.split(' ').filter { it.isNotBlank() }.map { w ->
                if (w in vocab || w.length < 3) w else {
                    val best = vocab.asSequence().filter { kotlin.math.abs(it.length - w.length) <= 2 }
                        .map { it to distance(w, it) }.minByOrNull { it.second }
                    if (best != null && best.second <= limit(w.length)) {
                        changed = true
                        val v = best.first
                        if (v.length != w.length || !w.indices.all { w[it] == v[it] || near[w[it]]?.contains(v[it]) == true }) neighbourOnly = false
                        best.first
                    } else w
                }
            }
            if (changed) out += NiuniaFixStep(if (neighbourOnly) 2 else 3, words.joinToString(" "))
        }
        val parts = folded.split(' ').filter { it.isNotBlank() }
        if (parts.size >= 2) out += NiuniaFixStep(5, parts.dropLast(1).joinToString(" "))
        return out.distinctBy { fold(it.query) }.filter { fold(it.query) != folded || it.step == 1 }
    }

    /** Krok 4: tytuł pierwszego filmu/serialu z TMDB `search/multi`; null = brak trafień, brak klucza albo błąd (zalogowany). */
    suspend fun tmdbTitle(context: Context, query: String): String? = withContext(Dispatchers.IO) {
        try {
            val ep = EntryPointAccessors.fromApplication(context.applicationContext, NiuniaSearchFixEntryPoint::class.java)
            val key = ep.tmdbService().apiKey()
            if (key.isBlank()) { Log.w(TAG, "Search: TMDB bez klucza — krok 4 pominięty"); return@withContext null }
            val res = ep.tmdbApi().searchMulti(query, key, "pl-PL")
            if (!res.isSuccessful) { Log.w(TAG, "Search: TMDB search/multi HTTP ${res.code()}"); return@withContext null }
            res.body()?.results.orEmpty().firstOrNull { it.mediaType == "movie" || it.mediaType == "tv" }?.let { it.title ?: it.name }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
            Log.w(TAG, "Search: TMDB krok 4: ${e.javaClass.simpleName}: ${e.message}"); null
        }
    }
}
