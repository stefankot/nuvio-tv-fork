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
                val cache = cache(c)
                cache.optJSONObject(key)?.let { row ->
                    val ttl = if (row.has("m")) FOUND_TTL_MS else MISS_TTL_MS
                    if (System.currentTimeMillis() - row.optLong("t") < ttl) return@withLock row.optJSONObject("m")?.let(::fromJson)
                }
                val found = runCatching { fetch(c, title, wanted, year) }
                    .onFailure { status = "błąd: ${it.message?.take(60)}"; Log.w(TAG, "lookup '$title': ${it.message}") }
                    .getOrElse { return@withLock null } // błąd sieci nie zapisuje „nie znaleziono”
                cache.put(key, JSONObject().put("t", System.currentTimeMillis()).apply { found?.let { put("m", toJson(it)) } })
                save(c, cache)
                found
            }
        }
    }

    /** Ustawienia: logowanie na próbę, wynik do [status]. */
    suspend fun check(c: Context): String = withContext(Dispatchers.IO) {
        if (!hasKey) return@withContext "TVDB: brak klucza w buildzie".also { status = "brak klucza" }
        runCatching { token(c, force = true); "TVDB: zalogowano" }.getOrElse { "TVDB: błąd logowania (${it.message?.take(60)})" }
            .also { status = it.removePrefix("TVDB: ") }
    }

    // ---- TheTVDB ----

    private fun fetch(c: Context, title: String, wanted: String, year: Int?): TvMeta? {
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
        return TvMeta(
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

    private fun cache(c: Context): JSONObject = mem ?: runCatching { JSONObject(file(c).readText()) }.getOrDefault(JSONObject()).also { mem = it }

    private fun save(c: Context, o: JSONObject) {
        if (o.length() > MAX_ROWS) {
            val old = o.keys().asSequence().toList().sortedBy { o.optJSONObject(it)?.optLong("t") ?: 0 }
            old.take(o.length() - MAX_ROWS).forEach(o::remove)
        }
        runCatching { file(c).also { it.parentFile?.mkdirs() }.writeText(o.toString()) }
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
