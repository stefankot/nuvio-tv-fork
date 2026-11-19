package com.nuvio.tv.fork.tvdb

import android.content.Context
import android.util.Log
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.fork.tv.TvMeta
import com.nuvio.tv.fork.tv.TvPerson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer
import java.util.concurrent.TimeUnit
import com.nuvio.tv.fork.logged

/**
 * W8 (właściciel 19.11: „integracja z TVDB, szczególnie dane o serialach w IPTV”): TheTVDB v4 jako drugie źródło
 * opisu programu z przewodnika, gdy TMDB nic nie znalazło (polskie seriale, dokumenty). Tylko dla otwartej karty
 * programu (bez zapytań przy przewijaniu siatki), wynik w pamięci i w pliku na 7 dni (nieznalezione: 1 dzień).
 * Ścisłe dopasowanie jak w [com.nuvio.tv.fork.tv.TvTmdb]: tytuł z przewodnika = nazwa serialu lub jej tłumaczenie/alias.
 * Pusty klucz `TVDB_API_KEY` (local.properties) = moduł nic nie robi.
 */
object Tvdb {
    private const val TAG = "ForkTvdb"
    private const val BASE = "https://api4.thetvdb.com/v4"
    private const val PREFS = "fork_tvdb"
    private const val TOKEN_DAYS = 25L
    private const val FOUND_TTL_MS = 7L * 86_400_000
    private const val MISS_TTL_MS = 86_400_000L
    private const val MAX_ROWS = 1500
    private const val EP_TTL_MS = 3L * 86_400_000
    private const val ART = "https://artworks.thetvdb.com"
    private val SEASON_SUFFIX = Regex("^(.*\\S)\\s+(\\d{1,2})$")

    val hasKey: Boolean get() = BuildConfig.TVDB_API_KEY.isNotBlank()

    /** Ostatni stan dla ustawień: zalogowano / brak klucza / błąd. */
    @Volatile var status: String = if (BuildConfig.TVDB_API_KEY.isBlank()) "brak klucza" else "jeszcze nie używane"
        private set

    private val http by lazy { OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build() }
    private val lock = Mutex()
    private var mem: JSONObject? = null

    fun enabled(c: Context) = hasKey && com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, "module_" + TvdbModule.id, true)

    /** Dane serialu o tytule z przewodnika; null = TVDB go nie zna (albo moduł wyłączony / brak klucza / błąd). */
    suspend fun lookup(c: Context, title: String, year: Int? = null): TvMeta? {
        if (!enabled(c)) return null
        val wanted = norm(title)
        if (wanted.length < 3) return null
        val key = wanted + "|" + (year ?: 0)
        return withContext(Dispatchers.IO) {
            lock.withLock {
                seriesLocked(c, title, wanted, year, key)?.second
            }
        }
    }

    /** Pod [lock]: (id serialu w TVDB, dane) z pamięci albo z TVDB; null = nie znaleziono / błąd. */
    private fun seriesLocked(c: Context, title: String, wanted: String, year: Int?, key: String): Pair<Long, TvMeta>? {
        val cache = cache(c)
        cache.optJSONObject(key)?.let { row ->
            val ttl = if (row.has("m")) FOUND_TTL_MS else MISS_TTL_MS
            if (System.currentTimeMillis() - row.optLong("t") < ttl) {
                val m = row.optJSONObject("m") ?: return null
                return row.optLong("id") to fromJson(m)
            }
        }
        val found = runCatching {
            fetch(c, title, wanted, year)
                // „Klangor 2”, „Castle 3”: polski przewodnik dopisuje numer sezonu do tytułu
                ?: SEASON_SUFFIX.find(title.trim())?.let { m -> val base = m.groupValues[1]; fetch(c, base, norm(base), null) }
        }.onFailure { status = "błąd: ${it.message?.take(60)}"; Log.w(TAG, "lookup '$title': ${it.message}") }
            .getOrElse { return null } // błąd sieci nie zapisuje „nie znaleziono”
        cache.put(key, JSONObject().put("t", System.currentTimeMillis()).apply { found?.let { put("id", it.first); put("m", toJson(it.second)) } })
        save(c, cache)
        return found
    }

    /** Odcinek z TVDB dla programu z przewodnika: [label] z EPG (S3E168, 3x168, E12, xmltv_ns „2.167.0/1”) albo numer w tytule. */
    data class Episode(val name: String?, val overview: String?, val image: String?)

    suspend fun episode(c: Context, title: String, label: String?, year: Int? = null): Episode? {
        if (!enabled(c)) return null
        val wanted = norm(title)
        if (wanted.length < 3) return null
        val (season0, number) = parseLabel(label) ?: return null
        val season = season0 ?: SEASON_SUFFIX.find(title.trim())?.groupValues?.get(2)?.toIntOrNull()
        return withContext(Dispatchers.IO) {
            lock.withLock {
                val (id, _) = seriesLocked(c, title, wanted, year, wanted + "|" + (year ?: 0)) ?: return@withLock null
                val eps = episodesLocked(c, id) ?: return@withLock null
                val hit = if (season != null) eps.firstOrNull { it.optInt("s") == season && it.optInt("e") == number }
                else eps.filter { it.optInt("s") > 0 && it.optInt("e") == number }.singleOrNull() ?: eps.firstOrNull { it.optInt("a") == number }
                hit?.let { Episode(it.optString("n").takeIf { n -> n.isNotBlank() }, it.optString("o").takeIf { o -> o.isNotBlank() }, it.optString("i").takeIf { i -> i.startsWith("http") }) }
            }
        }
    }

    /** Obraz na kartę półki, gdy przewodnik go nie ma: zdjęcie odcinka, inaczej tło albo plakat serialu. */
    suspend fun cardImage(c: Context, title: String, label: String?): String? {
        if (!enabled(c)) return null
        episode(c, title, label)?.image?.let { return it }
        val m = lookup(c, title) ?: SEASON_SUFFIX.find(title.trim())?.let { lookup(c, it.groupValues[1]) }
        return m?.backdropUrl ?: m?.posterUrl
    }

    private fun episodesLocked(c: Context, id: Long): List<JSONObject>? {
        val f = File(c.applicationContext.filesDir, "tvdb/ep_$id.json")
        runCatching { JSONObject(f.readText()) }.logged("ForkTvdb", "Tvdb.kt:117").getOrNull()?.let { o ->
            if (System.currentTimeMillis() - o.optLong("t") < EP_TTL_MS) return o.optJSONArray("e")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }
        }
        val out = JSONArray()
        runCatching {
            var page = 0
            while (page < 10) {
                val d = get(c, "$BASE/series/$id/episodes/default/pol?page=$page")
                val eps = d.optJSONObject("data")?.optJSONArray("episodes") ?: break
                for (i in 0 until eps.length()) {
                    val e = eps.optJSONObject(i) ?: continue
                    val img = e.optString("image").takeIf { it.isNotBlank() && it != "null" }?.let { if (it.startsWith("http")) it else ART + it }
                    out.put(JSONObject().put("s", e.optInt("seasonNumber")).put("e", e.optInt("number")).put("a", e.optInt("absoluteNumber"))
                        .put("n", e.optString("name").takeIf { it != "null" }).put("o", e.optString("overview").takeIf { it != "null" }?.take(600)).put("i", img))
                }
                if (d.optJSONObject("links")?.optString("next").let { it.isNullOrBlank() || it == "null" }) break
                page++
            }
        }.onFailure { Log.w(TAG, "episodes $id: ${it.message}"); return null }
        runCatching { f.parentFile?.mkdirs(); f.writeText(JSONObject().put("t", System.currentTimeMillis()).put("e", out).toString()) }.logged("ForkTvdb", "Tvdb.kt:136")
        return (0 until out.length()).mapNotNull { out.optJSONObject(it) }
    }

    /** (sezon albo null, numer odcinka) z etykiety EPG. */
    internal fun parseLabel(raw: String?): Pair<Int?, Int>? {
        val t = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        Regex("(?i)s\\s*(\\d+)\\s*e\\s*(\\d+)").find(t)?.let { return it.groupValues[1].toInt() to it.groupValues[2].toInt() }
        Regex("^(\\d+)\\s*x\\s*(\\d+)").find(t)?.let { return it.groupValues[1].toInt() to it.groupValues[2].toInt() }
        Regex("(?i)^e\\s*(\\d+)").find(t)?.let { return null to it.groupValues[1].toInt() }
        Regex("^(\\d*)\\s*\\.\\s*(\\d+)").find(t)?.let { m -> // xmltv_ns: liczone od zera
            return m.groupValues[1].toIntOrNull()?.plus(1) to (m.groupValues[2].toInt() + 1)
        }
        return t.takeIf { it.all(Char::isDigit) }?.let { null to it.toInt() }
    }

    /** Ustawienia: logowanie na próbę, wynik do [status]. */
    suspend fun check(c: Context): String = withContext(Dispatchers.IO) {
        if (!hasKey) return@withContext "TVDB: brak klucza w buildzie".also { status = "brak klucza" }
        runCatching { token(c, force = true); "TVDB: zalogowano" }.logged("ForkTvdb", "Tvdb.kt:155").getOrElse { "TVDB: błąd logowania (${it.message?.take(60)})" }
            .also { status = it.removePrefix("TVDB: ") }
    }

    // ---- TheTVDB ----

    private fun fetch(c: Context, title: String, wanted: String, year: Int?): Pair<Long, TvMeta>? {
        val search = get(c, "$BASE/search?query=" + android.net.Uri.encode(title.trim()) + "&type=series&limit=20")
        val hits = search.optJSONArray("data") ?: JSONArray()
        val exact = (0 until hits.length()).mapNotNull { hits.optJSONObject(it) }.filter { h -> names(h).any { norm(it) == wanted } }
        val pick = exact.firstOrNull { year != null && h(it, "year")?.toIntOrNull() == year } ?: exact.firstOrNull() ?: return null
        val id = (h(pick, "tvdb_id") ?: h(pick, "id")?.removePrefix("series-"))?.toLongOrNull() ?: return null
        val d = get(c, "$BASE/series/$id/extended?meta=translations").optJSONObject("data") ?: return null
        val tr = d.optJSONObject("translations")
        val plName = tr?.optJSONArray("nameTranslations")?.let { pickLang(it, "name") }
        val plOverview = tr?.optJSONArray("overviewTranslations")?.let { pickLang(it, "overview") }
        val artworks = d.optJSONArray("artworks") ?: JSONArray()
        val backdrop = (0 until artworks.length()).mapNotNull { artworks.optJSONObject(it) }
            .filter { it.optInt("type") == 3 }.maxByOrNull { it.optDouble("score", 0.0) }?.optString("image")?.takeIf { it.isNotBlank() }
        val genres = d.optJSONArray("genres")?.let { g -> (0 until g.length()).mapNotNull { g.optJSONObject(it)?.optString("name")?.takeIf(String::isNotBlank) } }.orEmpty()
        val chars = d.optJSONArray("characters") ?: JSONArray()
        val people = (0 until chars.length()).mapNotNull { chars.optJSONObject(it) }
        val cast = people.filter { it.optInt("type") == 3 || it.optString("peopleType").equals("Actor", true) }
            .sortedBy { it.optInt("sort", 999) }.take(12)
            .mapNotNull { p -> p.optString("personName").takeIf { it.isNotBlank() }?.let { n -> TvPerson(null, n, p.optString("name").takeIf { it.isNotBlank() }, (p.optString("personImgURL").ifBlank { p.optString("image") }).takeIf { it.startsWith("http") }) } }
        val directors = people.filter { it.optInt("type") == 1 || it.optString("peopleType").equals("Director", true) }.take(3)
            .mapNotNull { p -> p.optString("personName").takeIf { it.isNotBlank() }?.let { TvPerson(null, it, "Reżyseria", null) } }
        status = "zalogowano"
        return id to TvMeta(
            tmdbId = 0, isTv = true,
            title = plName ?: d.optString("name").ifBlank { title },
            overview = (plOverview ?: d.optString("overview")).takeIf { it.isNotBlank() && it != "null" },
            posterUrl = d.optString("image").takeIf { it.startsWith("http") } ?: h(pick, "image_url")?.takeIf { it.startsWith("http") },
            backdropUrl = backdrop,
            genres = genres, rating = null, votes = null,
            year = d.optString("year").toIntOrNull() ?: d.optString("firstAired").take(4).toIntOrNull(),
            cast = cast, directors = directors,
        )
    }

    private fun get(c: Context, url: String, retried: Boolean = false): JSONObject {
        val req = Request.Builder().url(url).header("Authorization", "Bearer " + token(c, force = false)).header("Accept-Language", "pol").build()
        http.newCall(req).execute().use { r ->
            if (r.code == 401 && !retried) { token(c, force = true); return get(c, url, true) }
            if (r.code == 404) return JSONObject()
            if (!r.isSuccessful) throw IllegalStateException("HTTP ${r.code}")
            return JSONObject(r.body?.string().orEmpty())
        }
    }

    private fun token(c: Context, force: Boolean): String {
        val p = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val kept = p.getString("token", null)
        if (!force && kept != null && p.getLong("token_until", 0) > System.currentTimeMillis()) return kept
        val body = JSONObject().put("apikey", BuildConfig.TVDB_API_KEY).toString().toRequestBody("application/json".toMediaType())
        http.newCall(Request.Builder().url("$BASE/login").post(body).build()).execute().use { r ->
            if (!r.isSuccessful) throw IllegalStateException("login HTTP ${r.code}")
            val t = JSONObject(r.body?.string().orEmpty()).optJSONObject("data")?.optString("token")?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("login bez tokenu")
            p.edit().putString("token", t).putLong("token_until", System.currentTimeMillis() + TOKEN_DAYS * 86_400_000).apply()
            return t
        }
    }

    // ---- pomocnicze ----

    private fun h(o: JSONObject, k: String): String? = o.optString(k).takeIf { it.isNotBlank() && it != "null" }

    /** Nazwa, tłumaczenia i aliasy wyniku wyszukiwania. */
    private fun names(o: JSONObject): List<String> = buildList {
        h(o, "name")?.let(::add); h(o, "extended_title")?.let(::add)
        o.optJSONObject("translations")?.let { t -> t.keys().forEach { k -> h(t, k)?.let(::add) } }
        o.optJSONArray("aliases")?.let { a -> for (i in 0 until a.length()) a.optString(i).takeIf { it.isNotBlank() }?.let(::add) }
    }

    private fun pickLang(arr: JSONArray, field: String): String? {
        val rows = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return rows.firstOrNull { it.optString("language") == "pol" }?.optString(field)?.takeIf { it.isNotBlank() }
    }

    private fun norm(s: String): String =
        Normalizer.normalize(s.lowercase(java.util.Locale("pl")).replace('ł', 'l'), Normalizer.Form.NFD)
            .replace(MARKS, "").replace(NON_ALNUM, " ").trim()
    private val MARKS = Regex("\\p{M}+")
    private val NON_ALNUM = Regex("[^a-z0-9]+")

    private fun file(c: Context) = File(c.applicationContext.filesDir, "tvdb/meta.json")

    private fun cache(c: Context): JSONObject = mem ?: runCatching { JSONObject(file(c).readText()) }.logged("ForkTvdb", "Tvdb.kt:243").getOrDefault(JSONObject()).also { mem = it }

    private fun save(c: Context, o: JSONObject) {
        if (o.length() > MAX_ROWS) {
            val old = o.keys().asSequence().toList().sortedBy { o.optJSONObject(it)?.optLong("t") ?: 0 }
            old.take(o.length() - MAX_ROWS).forEach(o::remove)
        }
        runCatching { file(c).also { it.parentFile?.mkdirs() }.writeText(o.toString()) }.logged("ForkTvdb", "Tvdb.kt:250")
    }

    private fun toJson(m: TvMeta) = JSONObject().put("title", m.title).put("overview", m.overview).put("poster", m.posterUrl)
        .put("backdrop", m.backdropUrl).put("year", m.year).put("genres", JSONArray(m.genres))
        .put("cast", JSONArray(m.cast.map { JSONObject().put("n", it.name).put("r", it.role).put("p", it.photoUrl) }))
        .put("dir", JSONArray(m.directors.map { it.name }))

    private fun fromJson(o: JSONObject): TvMeta {
        fun s(k: String) = o.optString(k).takeIf { it.isNotBlank() && it != "null" }
        val g = o.optJSONArray("genres") ?: JSONArray(); val cs = o.optJSONArray("cast") ?: JSONArray(); val ds = o.optJSONArray("dir") ?: JSONArray()
        return TvMeta(
            tmdbId = 0, isTv = true, title = s("title").orEmpty(), overview = s("overview"), posterUrl = s("poster"), backdropUrl = s("backdrop"),
            genres = (0 until g.length()).map { g.optString(it) }, rating = null, votes = null, year = o.optInt("year").takeIf { it > 0 },
            cast = (0 until cs.length()).mapNotNull { cs.optJSONObject(it) }.map { p -> TvPerson(null, p.optString("n"), p.optString("r").takeIf { it.isNotBlank() && it != "null" }, p.optString("p").takeIf { it.startsWith("http") }) },
            directors = (0 until ds.length()).map { TvPerson(null, ds.optString(it), "Reżyseria", null) },
        )
    }
}
