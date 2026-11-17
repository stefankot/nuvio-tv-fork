package com.nuvio.tv.fork.picks

/** Źródło kandydatów (TMDB); w testach atrapa. */
interface CandidateApi {
    suspend fun discover(genres: List<String>, keywords: List<String>): List<PickTitle>
    suspend fun similarTo(tmdbId: Int): List<PickTitle>
}

class CandidateCache(private val clock: () -> Long = System::currentTimeMillis) {
    private var at = 0L
    private var cached: List<PickTitle>? = null
    fun get(): List<PickTitle>? = cached?.takeIf { clock() - at < TTL_MS }
    fun put(list: List<PickTitle>) { cached = list; at = clock() }
    companion object { const val TTL_MS = 24 * 3_600_000L }
}

/** Kandydaci (B10-2): discover po gatunkach i słowach kluczowych profilu + „podobne” do 10 najlepiej ocenionych. Do 400, odsiew. */
object Candidates {
    const val MAX = 400

    suspend fun fetch(api: CandidateApi, profile: TasteProfile, topRated: List<Int>, cache: CandidateCache, ageLimit: Int = 99): List<PickTitle> {
        cache.get()?.let { return filter(it, profile, ageLimit) }
        val genres = profile.genreWeights.filter { it.value > 0 }.entries.sortedByDescending { it.value }.take(5).map { it.key }
        val kws = profile.keywordWeights.filter { it.value > 0 }.entries.sortedByDescending { it.value }.take(8).map { it.key }
        val found = ArrayList<PickTitle>()
        found += api.discover(genres, kws)
        topRated.take(10).forEach { found += api.similarTo(it) }
        val unique = found.distinctBy { it.tmdbId }.take(MAX * 2)
        cache.put(unique)
        return filter(unique, profile, ageLimit)
    }

    /** Odsiew: już obejrzane, lista „nigdy”, wiek profilu (certyfikat), limit 400. */
    fun filter(list: List<PickTitle>, profile: TasteProfile, ageLimit: Int): List<PickTitle> =
        list.filter { it.tmdbId !in profile.watched && it.tmdbId !in profile.never && (it.certification <= ageLimit) }.take(MAX)
}

/** Powód pod kartą, bez modelu (B10-4): „bo obejrzałeś X” albo „mało znany, ocena 7,8”. */
object Reasons {
    fun reason(t: PickTitle, mood: Mood, watchedTitles: List<PickTitle>): String {
        val shared = watchedTitles.map { w -> w to w.genres.intersect(t.genres.toSet()).size + w.keywords.intersect(t.keywords.toSet()).size }.filter { it.second >= 2 }.maxByOrNull { it.second }
        return when {
            mood.id == "gems" || (t.votes < 5000 && t.rating >= 7.0) -> "mało znany, ocena " + "%.1f".format(java.util.Locale("pl"), t.rating)
            shared != null -> "bo obejrzałeś " + shared.first.title
            t.rating >= 7.5 -> "wysoko oceniany: " + "%.1f".format(java.util.Locale("pl"), t.rating)
            else -> mood.title.lowercase()
        }
    }
}

/** Filtr „zagra po polsku” (B10-5): tytuły bez polskiego źródła na koniec; najwyżej 60 sprawdzeń na dobę, wynik pamiętany 3 dni. */
class PolishFilter(private val clock: () -> Long = System::currentTimeMillis, private val dailyLimit: Int = 60) {
    private val known = HashMap<Int, Pair<Boolean, Long>>()
    private var dayStart = 0L
    private var checksToday = 0

    /** [check] sprawdza dodatki (kosztuje 1 sprawdzenie). Zwraca ranking z niegrającymi po polsku na końcu. */
    suspend fun reorder(ranked: List<PickTitle>, firstN: Int = 30, check: suspend (PickTitle) -> Boolean): List<PickTitle> {
        val now = clock()
        if (now - dayStart > 24 * 3_600_000L) { dayStart = now; checksToday = 0 }
        val head = ranked.take(firstN)
        val ok = HashMap<Int, Boolean>()
        for (t in head) {
            val k = known[t.tmdbId]?.takeIf { now - it.second < 3 * 24 * 3_600_000L }
            when {
                k != null -> ok[t.tmdbId] = k.first
                checksToday < dailyLimit -> { checksToday++; val r = check(t); known[t.tmdbId] = r to now; ok[t.tmdbId] = r }
                else -> ok[t.tmdbId] = true // limit wyczerpany: nie karzemy
            }
        }
        return head.filter { ok[it.tmdbId] != false } + ranked.drop(firstN) + head.filter { ok[it.tmdbId] == false }
    }
    val checks: Int get() = checksToday
}
