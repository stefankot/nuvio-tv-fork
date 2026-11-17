package com.nuvio.tv.fork.picks

import com.nuvio.tv.fork.ai.VectorMath
import com.nuvio.tv.fork.voice.Text
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.ln

@Serializable
data class Mood(
    val id: String,
    val title: String,
    val genres: Map<String, Double> = emptyMap(),
    val minRating: Double = 0.0,
    val votesMin: Int = 0,
    val votesMax: Int = Int.MAX_VALUE,
    val minRuntime: Int = 0,
    val maxRuntime: Int = Int.MAX_VALUE,
    val blockKeywords: List<String> = emptyList(),
    val keywordsBonus: List<String> = emptyList(),
    /** −1 = kara za popularność (perełki), 0 = bez wpływu. */
    val popularityBias: Double = 0.0,
    val vectorQuery: String = "",
)

@Serializable private data class MoodFile(val moods: List<Mood> = emptyList())

object Moods {
    fun parse(text: String): List<Mood> = runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<MoodFile>(text).moods }.getOrDefault(emptyList())
}

/** Punktacja regułowa (B10-3): podobieństwo do profilu × waga nastroju. Zwraca null, gdy tytuł odpada twardo. */
object RuleScoring {
    private fun n(s: String) = Text.norm(s)

    fun score(t: PickTitle, profile: TasteProfile, mood: Mood): Double? {
        if (t.tmdbId in profile.never || t.tmdbId in profile.watched) return null
        if (t.rating < mood.minRating || t.runtimeMin !in mood.minRuntime..mood.maxRuntime) return null
        if (t.votes !in mood.votesMin..mood.votesMax && mood.votesMin > 0) return null
        val kws = t.keywords.map(::n)
        if (mood.blockKeywords.any { b -> kws.any { it.contains(n(b)) } }) return null

        val taste = tasteSimilarity(t, profile)
        val moodWeight = moodWeight(t, mood, profile)
        if (moodWeight <= 0.0) return null
        return (0.5 + taste) * moodWeight
    }

    private fun tasteSimilarity(t: PickTitle, p: TasteProfile): Double {
        val g = t.genres.sumOf { p.genreWeights[it] ?: 0.0 }
        val k = t.keywords.sumOf { p.keywordWeights[it] ?: 0.0 } * 0.3
        val d = (p.decadeWeights[t.year / 10 * 10] ?: 0.0) * 0.2
        val raw = g + k + d
        return raw / (abs(raw) + 3.0) // wygładzenie do (-1, 1)
    }

    private fun moodWeight(t: PickTitle, m: Mood, p: TasteProfile): Double {
        var w = if (m.genres.isEmpty()) 1.0 else 0.2 + t.genres.sumOf { m.genres[n(it)] ?: m.genres[it] ?: 0.0 }
        val kws = t.keywords.map(::n)
        if (m.keywordsBonus.any { b -> kws.any { it.contains(n(b)) } }) w *= 1.6
        // Popularność ~ log głosów; suwak „znane ↔ nieodkryte” przesuwa limit we wszystkich rzędach.
        val pop = ln(t.votes.coerceAtLeast(1).toDouble()) / ln(300_000.0)
        val undiscovered = p.sliders.undiscovered
        w *= if (m.popularityBias < 0) (1.0 - pop).coerceAtLeast(0.05) * (0.6 + undiscovered) else 1.0 + (0.5 - undiscovered) * pop
        // Suwaki: lekkie ↔ wymagające (ocena i czas), nowe ↔ klasyka.
        w *= 1.0 + (p.sliders.demanding - 0.5) * (if (t.genres.any { it in HEAVY }) 0.6 else -0.6)
        val age = (2026 - t.year).coerceAtLeast(0) / 60.0
        w *= 1.0 + (p.sliders.classic - 0.5) * (age - 0.5)
        return w.coerceAtLeast(0.0)
    }

    private val HEAVY = setOf("dramat", "dokument", "historyczny", "wojenny")

    fun rank(candidates: List<PickTitle>, profile: TasteProfile, mood: Mood): List<Pair<PickTitle, Double>> =
        candidates.mapNotNull { t -> score(t, profile, mood)?.let { t to it } }.sortedByDescending { it.second }
}

/** Punktacja na wektorach (B10-3b): arytmetyka na gotowej paczce, bez uruchamiania modelu. */
object VectorScoring {
    /** Wektor gustu = średnia wektorów obejrzanych do końca i polubionych − 0,5 × średnia porzuconych. */
    fun tasteVector(feedback: List<Feedback>, vectors: Map<Int, FloatArray>): FloatArray? {
        val pos = feedback.filter { it.outcome == Outcome.COMPLETED || it.outcome == Outcome.THUMB_UP }.mapNotNull { vectors[it.titleId] }
        val neg = feedback.filter { it.outcome == Outcome.ABANDONED_EARLY || it.outcome == Outcome.THUMB_DOWN }.mapNotNull { vectors[it.titleId] }
        if (pos.isEmpty()) return null
        val dim = pos.first().size
        val out = FloatArray(dim)
        pos.forEach { v -> for (i in 0 until dim) out[i] += v[i] / pos.size }
        if (neg.isNotEmpty()) neg.forEach { v -> for (i in 0 until dim) out[i] -= 0.5f * v[i] / neg.size }
        return VectorMath.truncateNormalize(out, dim)
    }

    /** Punkt = podobieństwo do gustu × podobieństwo do nastroju × waga cech z reguł (twarde odsiewy jak w [RuleScoring]). */
    fun rank(candidates: List<PickTitle>, taste: FloatArray, moodVec: FloatArray, profile: TasteProfile, mood: Mood): List<Pair<PickTitle, Double>> =
        candidates.mapNotNull { t ->
            val v = t.vector ?: return@mapNotNull null
            RuleScoring.score(t, profile, mood) ?: return@mapNotNull null
            val st = (VectorMath.cosine(taste, v) + 1f) / 2f
            val sm = (VectorMath.cosine(moodVec, v) + 1f) / 2f
            val feature = if (mood.popularityBias < 0) (1.0 - ln(t.votes.coerceAtLeast(1).toDouble()) / ln(300_000.0)).coerceAtLeast(0.05) else 1.0
            t to (st * sm * feature)
        }.sortedByDescending { it.second }

    /** Różnorodność: nie więcej niż [maxSimilar] tytułów o wzajemnym podobieństwie > [threshold] w jednym rzędzie. */
    fun diversify(ranked: List<Pair<PickTitle, Double>>, size: Int, threshold: Float = 0.9f, maxSimilar: Int = 3): List<PickTitle> {
        val out = ArrayList<PickTitle>()
        for ((t, _) in ranked) {
            if (out.size >= size) break
            val v = t.vector
            val similar = if (v == null) 0 else out.count { o -> o.vector?.let { VectorMath.cosine(v, it) > threshold } == true }
            if (similar < maxSimilar - 1) out += t
        }
        return out
    }
}
