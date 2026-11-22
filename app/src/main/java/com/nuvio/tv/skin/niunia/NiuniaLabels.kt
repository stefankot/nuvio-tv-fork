package com.nuvio.tv.skin.niunia

import android.content.Context
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.MetaPreview
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Etykiety plakatów i linia „gatunek · ocena” wg reguł z SKIN.md, tylko z danych, które aplikacja już ma.
 * Ranking: rząd, którego nazwa wskazuje na popularne / na czasie → „#n Dziś”.
 * Nowość: data premiery ≤ 45 dni temu. Klasyka: rok < 2000. W pozostałych przypadkach brak etykiety.
 */
object NiuniaLabels {
    private val rankingRow = Regex("trend|popul|top|na czasie|popularne", RegexOption.IGNORE_CASE)

    fun isRankingRow(catalogName: String) = rankingRow.containsMatchIn(catalogName)

    fun label(context: Context, item: MetaPreview, indexInRow: Int, rankingRow: Boolean): String? {
        if (rankingRow && indexInRow < 10) return context.getString(R.string.niunia_label_rank, indexInRow + 1)
        val released = item.released?.take(10)?.let {
            try { LocalDate.parse(it) } catch (e: Exception) { android.util.Log.w("ForkNiunia", "data premiery „$it”: ${e.javaClass.simpleName}: ${e.message}"); null }
        }
        if (released != null) {
            val days = ChronoUnit.DAYS.between(released, LocalDate.now())
            if (days in 0..45) return context.getString(R.string.niunia_label_new)
        }
        val year = (item.releaseInfo ?: item.released)?.take(4)?.toIntOrNull()
        if (year != null && year < 2000) return context.getString(R.string.niunia_label_classic)
        return null
    }

    /**
     * Krótki gatunek (SKIN.md „Poster footer meta”, najwyżej ~10 znaków), po polsku przez tłumaczenia gatunków Nuvio:
     * Science Fiction / Sci-Fi & Fantasy → Sci-Fi, Action & Adventure → Akcja, War & Politics → Wojenny, Documentary → Dokument,
     * TV Movie → Film TV, w pozostałych część przed „ & ”.
     */
    fun shortGenre(context: Context, raw: String): String {
        val key = raw.trim().lowercase().replace("-", " ")
        return when {
            key == "science fiction" || key.startsWith("sci fi") -> "Sci-Fi"
            key == "documentary" -> context.getString(R.string.niunia_genre_docu)
            key == "tv movie" -> context.getString(R.string.niunia_genre_tv_movie)
            else -> {
                val base = raw.substringBefore(" & ").trim()
                val pl = com.nuvio.tv.ui.util.localizedGenreLabel(context, base)
                if (pl.length <= 12) pl else pl.substringBefore(' ')
            }
        }
    }

    /** „Gatunek · ★ 7.4” — ocena to średnia z dostępnych serwisów; w aplikacji jest dziś tylko IMDb. */
    fun meta(item: MetaPreview): String? {
        val genre = item.genres.firstOrNull()
        val rating = item.imdbRating?.takeIf { it > 0f }?.let { "★ %.1f".format(java.util.Locale.US, it) }
        return listOfNotNull(genre, rating).joinToString(" · ").ifBlank { null }
    }
}
