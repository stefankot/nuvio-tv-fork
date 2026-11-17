package com.nuvio.tv.fork.picks

import com.nuvio.tv.fork.ai.VectorMath
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Random

class PicksTest {
    private val moods = Moods.parse(File("src/main/assets/fork/moods.json").readText()).associateBy { it.id }

    private fun t(id: Int, genres: List<String>, rating: Double = 7.0, votes: Int = 2000, runtime: Int = 100, kws: List<String> = emptyList(), year: Int = 2010, vec: FloatArray? = null) =
        PickTitle(id, "T$id", genres, kws, year, rating, votes, runtime, vector = vec)

    private val titles = mapOf(1 to t(1, listOf("komedia", "familijny")), 2 to t(2, listOf("kryminal", "thriller")), 3 to t(3, listOf("dramat")), 4 to t(4, listOf("komedia")))

    @Test fun `dwie historie daja rozne wagi a porzucenie obniza wage gatunku`() {
        val a = TasteBuilder.build(listOf(Feedback(1, Outcome.COMPLETED), Feedback(4, Outcome.THUMB_UP)), titles)
        val b = TasteBuilder.build(listOf(Feedback(2, Outcome.COMPLETED)), titles)
        assertNotEquals(a.genreWeights, b.genreWeights); assertTrue(a.genreWeights.getValue("komedia") > 0 && "komedia" !in b.genreWeights)
        val c = TasteBuilder.build(listOf(Feedback(1, Outcome.COMPLETED), Feedback(4, Outcome.ABANDONED_EARLY)), titles)
        assertTrue(c.genreWeights.getValue("komedia") < a.genreWeights.getValue("komedia"))
        assertEquals(Outcome.ABANDONED_EARLY, TasteBuilder.outcomeOf(10, 0.1)); assertEquals(Outcome.COMPLETED, TasteBuilder.outcomeOf(100, 0.95)); assertEquals(null, TasteBuilder.outcomeOf(45, 0.5))
    }

    private val pool = (1..120).map { i ->
        when (i % 4) {
            0 -> t(1000 + i, listOf("kryminal", "thriller"), 7.0, 80_000, 105, year = 2015) // łatwe kryminały, popularne
            1 -> t(1000 + i, listOf("dramat"), 7.6, 800, 110, listOf("kino niezalezne", "festiwal"), 2008) // kino niezależne
            2 -> t(1000 + i, listOf("komedia"), 6.5, 200_000, 95, year = 2018) // masowe
            else -> t(1000 + i, listOf("dramat", "dokument"), 7.4, 1200, 130, listOf("festiwal"), 2005)
        }
    }

    @Test fun `film masowy nie trafia do perelek a profile maja rozlaczne dziesiatki`() {
        val easy = TasteBuilder.build(listOf(Feedback(2, Outcome.COMPLETED), Feedback(2, Outcome.THUMB_UP)), titles)
        val indie = TasteProfile(genreWeights = mapOf("dramat" to 5.0, "dokument" to 3.0), keywordWeights = mapOf("kino niezalezne" to 4.0, "festiwal" to 3.0), sliders = Sliders(undiscovered = 0.9f))
        val gems = RuleScoring.rank(pool, indie, moods.getValue("gems")).map { it.first }
        assertTrue(gems.none { it.votes > 5000 || it.votes < 150 })
        val tension = moods.getValue("tension")
        val topEasy = RuleScoring.rank(pool, easy, tension).take(10).map { it.first.tmdbId }.toSet()
        val topIndie = RuleScoring.rank(pool, indie, moods.getValue("demanding")).take(10).map { it.first.tmdbId }.toSet()
        assertTrue(topEasy.isNotEmpty() && topIndie.isNotEmpty()); assertTrue((topEasy intersect topIndie).isEmpty())
        // lekkie: bez ciężkich słów kluczowych i do 110 min
        assertTrue(RuleScoring.rank(pool + t(5000, listOf("komedia"), kws = listOf("przemoc")), easy, moods.getValue("light")).none { it.first.tmdbId == 5000 || it.first.runtimeMin > 110 })
        assertTrue(RuleScoring.rank(pool, easy, moods.getValue("ninety")).all { it.first.runtimeMin in 75..100 })
    }

    @Test fun `wektory: rozlaczne dziesiatki, perelki bez mas i roznorodnosc`() {
        val r = Random(7)
        fun unit(base: Int, noise: Float) = VectorMath.truncateNormalize(FloatArray(128) { i -> (if (i / 32 == base) 1f else 0f) + noise * r.nextGaussian().toFloat() })
        val vp = pool.mapIndexed { i, p -> p.copy(vector = unit(if (p.genres.contains("kryminal")) 0 else if (p.genres == listOf("dramat")) 1 else if (p.genres.contains("komedia")) 2 else 3, 0.05f)) }
        val vectors = vp.associate { it.tmdbId to it.vector!! }
        val easyTaste = VectorScoring.tasteVector(listOf(Feedback(1004, Outcome.COMPLETED), Feedback(1008, Outcome.THUMB_UP), Feedback(1002, Outcome.ABANDONED_EARLY)), vectors)!!
        val indieTaste = VectorScoring.tasteVector(listOf(Feedback(1001, Outcome.COMPLETED), Feedback(1005, Outcome.THUMB_UP)), vectors)!!
        val profile = TasteProfile()
        val q = unit(0, 0f)
        val a = VectorScoring.rank(vp, easyTaste, q, profile, moods.getValue("tension")).take(10).map { it.first.tmdbId }.toSet()
        val b = VectorScoring.rank(vp, indieTaste, unit(1, 0f), profile, moods.getValue("demanding")).take(10).map { it.first.tmdbId }.toSet()
        assertTrue(a.isNotEmpty() && b.isNotEmpty() && (a intersect b).isEmpty())
        val gems = VectorScoring.rank(vp, indieTaste, unit(1, 0f), profile, moods.getValue("gems"))
        assertTrue(gems.none { it.first.votes > 5000 })
        val ranked = VectorScoring.rank(vp, indieTaste, unit(1, 0f), profile, moods.getValue("demanding"))
        val diverse = VectorScoring.diversify(ranked, 10, threshold = 0.9f, maxSimilar = 3)
        for (x in diverse) assertTrue(diverse.count { y -> x !== y && VectorMath.cosine(x.vector!!, y.vector!!) > 0.9f } <= 2)
    }

    @Test fun `odsiew kandydatow na atrapie API i cache 24 h`() = runBlocking {
        var calls = 0
        val api = object : CandidateApi {
            override suspend fun discover(genres: List<String>, keywords: List<String>): List<PickTitle> { calls++; return (1..500).map { t(it, listOf("komedia")) } }
            override suspend fun similarTo(tmdbId: Int): List<PickTitle> = listOf(t(tmdbId + 10_000, listOf("komedia")))
        }
        val profile = TasteProfile(genreWeights = mapOf("komedia" to 3.0), never = setOf(2), watched = setOf(3))
        var now = 0L; val cache = CandidateCache { now }
        val first = Candidates.fetch(api, profile, listOf(1, 2), cache)
        assertTrue(first.size <= Candidates.MAX && first.none { it.tmdbId in setOf(2, 3) })
        Candidates.fetch(api, profile, listOf(1), cache); assertEquals(1, calls)
        now += CandidateCache.TTL_MS + 1; Candidates.fetch(api, profile, listOf(1), cache); assertEquals(2, calls)
        assertTrue(Candidates.filter(listOf(t(9, listOf("x")).copy(certification = 18)), profile, ageLimit = 12).isEmpty())
    }

    @Test fun `powody bez modelu`() {
        val gem = t(1, listOf("dramat"), 7.8, 900)
        assertEquals("mało znany, ocena 7,8", Reasons.reason(gem, moods.getValue("gems"), emptyList()))
        val watched = t(2, listOf("kryminal", "thriller"), 7.0, 90_000, kws = listOf("policja")).copy(title = "Fargo")
        assertEquals("bo obejrzałeś Fargo", Reasons.reason(t(3, listOf("kryminal", "thriller"), 6.8, 90_000, kws = listOf("policja")), moods.getValue("tension"), listOf(watched)))
    }

    @Test fun `filtr po polsku kolejnosc i limit sprawdzen`() = runBlocking {
        val f = PolishFilter(dailyLimit = 5); val list = (1..10).map { t(it, listOf("x")) }
        val out = f.reorder(list) { it.tmdbId % 2 == 0 }
        assertEquals(5, f.checks); assertEquals(listOf(2, 4, 6, 7, 8, 9, 10, 1, 3, 5), out.map { it.tmdbId })
        val again = f.reorder(list) { error("nie powinno pytać (pamięć 3 dni / limit)") }
        assertEquals(out.map { it.tmdbId }, again.map { it.tmdbId })
    }

    @Test fun `ankieta zapisuje sie do profilu`() {
        val s = Survey(listOf(1, 2, 3, 4), Sliders(0.9f, 0.2f, 0.7f), never = setOf(77))
        val p = TasteBuilder.build(emptyList(), titles, s)
        assertEquals(0.9f, p.sliders.undiscovered); assertTrue(77 in p.never); assertTrue(p.genreWeights.getValue("komedia") > 0)
        assertTrue(runCatching { Survey(listOf(1, 2, 3, 4, 5, 6), Sliders()) }.isFailure)
    }

    @Test fun `ranking przez model: odrzuca tytuly spoza listy i wraca do regul przy bledzie`() = runBlocking {
        val ranked = (1..80).map { t(it, listOf("x")) }; val mood = moods.getValue("light")
        val llm = object : RankLlm { override suspend fun rank(mood: String, tasteDescription: String, candidates: List<Pair<Int, String>>) = listOf(5 to "bo lubisz X", 999 to "spoza listy", 3 to "Y") }
        val out = LlmRanker(llm).rank("p", mood, ranked, "opis")
        assertEquals(listOf(5, 3), out.map { it.title.tmdbId }); assertEquals("bo lubisz X", out[0].why)
        val broken = object : RankLlm { override suspend fun rank(mood: String, tasteDescription: String, candidates: List<Pair<Int, String>>): List<Pair<Int, String>> = error("sieć") }
        val fb = LlmRanker(broken).rank("p", mood, ranked, "opis"); assertEquals(20, fb.size); assertTrue(fb.all { it.why == null })
        assertEquals(20, LlmRanker(null).rank("p", mood, ranked, "opis").size)
        assertFalse(LlmRanker(llm).describe(TasteProfile(), emptyList(), emptyList()).isEmpty())
    }
}
