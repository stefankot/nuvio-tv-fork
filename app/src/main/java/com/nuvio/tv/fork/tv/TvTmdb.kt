package com.nuvio.tv.fork.tv

import com.nuvio.tv.core.tmdb.TmdbService
import com.nuvio.tv.data.remote.api.TmdbApi
import com.nuvio.tv.data.remote.api.TmdbCastMember
import com.nuvio.tv.data.remote.api.TmdbSearchMultiItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import com.nuvio.tv.fork.logged

/** A person from TMDB: photo only when TMDB has one (no photo = initials in the UI). */
data class TvPerson(val id: Int?, val name: String, val role: String?, val photoUrl: String?)

/** What TMDB adds to a Live TV programme (spec IPTV §1C/§1E). Every field may be missing; missing = the UI hides the section. */
data class TvMeta(
    val tmdbId: Int,
    val isTv: Boolean,
    val title: String,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val genres: List<String>,
    val rating: Double?,
    val votes: Int?,
    val year: Int?,
    val cast: List<TvPerson>,
    val directors: List<TvPerson>,
) {
    /** "TMDB 7,4/10 (238 głosów)" - always with the source and the vote count, only when both were returned. */
    val ratingText: String? get() =
        if (rating != null && rating > 0.0 && votes != null && votes > 0) "TMDB %.1f/10 (%d głosów)".format(java.util.Locale("pl"), rating, votes) else null
}

/**
 * Fills the gaps of the guide (poster, cast, photos, rating, overview) from TMDB, which the app already uses.
 * Uses only the TMDB calls Nuvio has (plus the one search call added to TmdbApi) and the same key.
 * Strict matching: the guide's title must equal the TMDB title (ignoring case and accents), otherwise nothing is shown,
 * so news bulletins and generic titles never get a made-up poster or cast.
 */
@Singleton
class TvTmdb @Inject constructor(
    private val api: TmdbApi,
    private val tmdb: TmdbService,
) {
    private val cache = ConcurrentHashMap<String, Optional>()
    private class Optional(val value: TvMeta?)

    /** [year] is the production year from the guide when it has one (it breaks ties between remakes). */
    suspend fun lookup(title: String, year: Int? = null): TvMeta? {
        val wanted = norm(title)
        if (wanted.length < 3) return null
        val key = wanted + "|" + (year ?: 0)
        cache[key]?.let { return it.value }
        val found = runCatching { fetch(title, wanted, year) }.logged("ForkTv", "TvTmdb.kt:57").getOrNull()
        cache[key] = Optional(found)
        return found
    }

    private suspend fun fetch(title: String, wanted: String, year: Int?): TvMeta? {
        val apiKey = tmdb.apiKey()
        val results = api.searchMulti(title.trim(), apiKey, LANG).body()?.results.orEmpty()
            .filter { it.mediaType == "tv" || it.mediaType == "movie" }
        val exact = results.filter { r -> listOf(r.title, r.name, r.originalTitle, r.originalName).any { it != null && norm(it) == wanted } }
        val pick = (exact.filter { year != null && yearOf(it) == year }.maxByOrNull { it.popularity ?: 0.0 }
            ?: exact.maxByOrNull { it.popularity ?: 0.0 }) ?: return null
        val tv = pick.mediaType == "tv"
        return coroutineScope {
            val details = async { if (tv) api.getTvDetails(pick.id, apiKey, LANG).body() else api.getMovieDetails(pick.id, apiKey, LANG).body() }
            val credits = async { if (tv) api.getTvCredits(pick.id, apiKey, LANG).body() else api.getMovieCredits(pick.id, apiKey, LANG).body() }
            val d = details.await() ?: return@coroutineScope null
            val c = credits.await()
            TvMeta(
                tmdbId = d.id,
                isTv = tv,
                title = d.title ?: d.name ?: title,
                overview = d.overview?.takeIf { it.isNotBlank() },
                posterUrl = d.posterPath?.let { "$IMG/w500$it" },
                backdropUrl = d.backdropPath?.let { "$IMG/w1280$it" },
                genres = d.genres.orEmpty().map { it.name },
                rating = d.voteAverage,
                votes = d.voteCount,
                year = (d.releaseDate ?: d.firstAirDate)?.take(4)?.toIntOrNull(),
                cast = c?.cast.orEmpty().filter { !it.name.isNullOrBlank() }.take(12).map { person(it) },
                directors = if (tv) d.createdBy.orEmpty().mapNotNull { it.name?.let { n -> TvPerson(null, n, "Twórca", null) } }
                else c?.crew.orEmpty().filter { it.job == "Director" && !it.name.isNullOrBlank() }.map { TvPerson(it.id, it.name!!, "Reżyseria", it.profilePath?.let { p -> "$IMG/w185$p" }) },
            )
        }
    }

    private fun person(m: TmdbCastMember) = TvPerson(m.id, m.name.orEmpty(), m.character?.takeIf { it.isNotBlank() }, m.profilePath?.let { "$IMG/w185$it" })

    private fun yearOf(r: TmdbSearchMultiItem): Int? = (r.releaseDate ?: r.firstAirDate)?.take(4)?.toIntOrNull()

    /** Lower case, no accents, only letters and digits: "M jak miłość" == "m jak milosc". */
    private fun norm(s: String): String =
        Normalizer.normalize(s.lowercase(java.util.Locale("pl")).replace('ł', 'l'), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9]+"), " ").trim()

    private companion object {
        const val LANG = "pl-PL"
        const val IMG = "https://image.tmdb.org/t/p"
    }
}
