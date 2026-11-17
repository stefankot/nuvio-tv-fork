package com.nuvio.tv.fork.picks

import kotlinx.serialization.Serializable
import kotlin.math.ln

/** Tytuł z faktami (z TMDB, z pamięci podręcznej); [vector] z paczki B06b (8-bitowy zdekodowany do float), jeśli jest. */
data class PickTitle(
    val tmdbId: Int,
    val title: String,
    val genres: List<String>,
    val keywords: List<String> = emptyList(),
    val year: Int = 2000,
    val rating: Double = 6.5,
    val votes: Int = 1000,
    val runtimeMin: Int = 100,
    val certification: Int = 0,
    val vector: FloatArray? = null,
)

enum class Outcome { COMPLETED, ABANDONED_EARLY, THUMB_UP, THUMB_DOWN }
data class Feedback(val titleId: Int, val outcome: Outcome, val watches: Int = 1)

/** Trzy suwaki 0..1: znane ↔ nieodkryte, lekkie ↔ wymagające, nowe ↔ klasyka. */
@Serializable
data class Sliders(val undiscovered: Float = 0.5f, val demanding: Float = 0.5f, val classic: Float = 0.5f)

@Serializable
data class TasteProfile(
    val genreWeights: Map<String, Double> = emptyMap(),
    val keywordWeights: Map<String, Double> = emptyMap(),
    val decadeWeights: Map<Int, Double> = emptyMap(),
    val sliders: Sliders = Sliders(),
    val never: Set<Int> = emptySet(),
    val watched: Set<Int> = emptySet(),
)

/** Profil gustu (B10-1): czysta logika, osobno dla profilu. Obejrzane do końca = plus, porzucone wcześnie = minus. */
object TasteBuilder {
    const val ABANDON_MINUTES = 15

    fun weight(f: Feedback): Double = when (f.outcome) {
        Outcome.COMPLETED -> 1.0 * (1 + ln(f.watches.coerceAtLeast(1).toDouble()))
        Outcome.THUMB_UP -> 1.5
        Outcome.ABANDONED_EARLY -> -0.8
        Outcome.THUMB_DOWN -> -1.5
    }

    /** Klasyfikacja obejrzenia: porzucone przed 15 minutami = minus; obejrzane do końca (>= 90%) = plus; reszta bez znaczenia. */
    fun outcomeOf(watchedMinutes: Int, fractionWatched: Double): Outcome? = when {
        fractionWatched >= 0.9 -> Outcome.COMPLETED
        watchedMinutes < ABANDON_MINUTES -> Outcome.ABANDONED_EARLY
        else -> null
    }

    fun build(feedback: List<Feedback>, titles: Map<Int, PickTitle>, survey: Survey? = null, previous: TasteProfile = TasteProfile()): TasteProfile {
        val genres = HashMap<String, Double>(); val kws = HashMap<String, Double>(); val decades = HashMap<Int, Double>()
        for (f in feedback) {
            val t = titles[f.titleId] ?: continue
            val w = weight(f)
            t.genres.forEach { genres.merge(it, w, Double::plus) }
            t.keywords.forEach { kws.merge(it, w, Double::plus) }
            decades.merge(t.year / 10 * 10, w, Double::plus)
        }
        survey?.chosen?.mapNotNull { titles[it] }?.forEach { t ->
            t.genres.forEach { genres.merge(it, 1.5, Double::plus) }; t.keywords.forEach { kws.merge(it, 1.0, Double::plus) }
        }
        return TasteProfile(
            genreWeights = genres, keywordWeights = kws, decadeWeights = decades,
            sliders = survey?.sliders ?: previous.sliders,
            never = previous.never + (survey?.never.orEmpty()),
            watched = previous.watched + feedback.map { it.titleId },
        )
    }
}

/** Ankieta (B10-6): 5 plakatów z 20, trzy suwaki, lista „nigdy”. */
data class Survey(val chosen: List<Int>, val sliders: Sliders, val never: Set<Int> = emptySet()) {
    init { require(chosen.size <= 5) { "najwyżej 5 plakatów" } }
}
