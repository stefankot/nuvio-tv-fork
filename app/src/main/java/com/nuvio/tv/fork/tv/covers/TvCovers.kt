package com.nuvio.tv.fork.tv.covers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

/**
 * Przegląd ekranów 27.11, ekran 10 pkt 3 (właściciel: „wszędzie powinny być okładki programu, nie logo stacji w MediaCards”):
 * okładka programu Live TV z TMDB, gdy przewodnik nie ma obrazka. Jedno zapytanie `search/multi` na tytuł (kadr poziomy
 * `backdrop_path`, inaczej plakat), ścisła zgodność tytułu (jak `TvTmdb`): wiadomości i ogólne tytuły nie dostają cudzej okładki.
 * Wynik (także „brak”) w pamięci i na dysku (`files/fork_tv_covers.json`), najwyżej 2 zapytania naraz, to samo zapytanie raz.
 * Wyłącznik: klucz ForkFeatures `tv_tmdb_covers` (wyłączony = karta jak dawniej: obrazek z przewodnika albo logo).
 */
object TvCovers {
    private const val TAG = "ForkTvCovers"
    const val KEY = "tv_tmdb_covers"
    private const val HIT_TTL_MS = 30L * 24 * 3600_000
    private const val MISS_TTL_MS = 3L * 24 * 3600_000
    private const val IMG = "https://image.tmdb.org/t/p/w780"

    private class Entry(val url: String, val at: Long)
    private val mem = ConcurrentHashMap<String, Entry>()
    private val sem = Semaphore(2)
    private val locks = ConcurrentHashMap<String, kotlinx.coroutines.sync.Mutex>()
    @Volatile private var file: File? = null
    @Volatile private var dirty = 0
    private var hits = 0; private var misses = 0; private var errors = 0

    fun enabled(c: Context): Boolean = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, KEY, true)

    /** Tytuł z przewodnika bez numerów odcinka i sezonu: „Bez śladu 6: Zakręt (14)” → „bez śladu”. */
    fun searchTitle(raw: String): String? {
        var t = raw.trim()
        t = t.replace(Regex("""\([^)]*\)"""), " ")
        t = t.split(Regex("""\s[-–—:|]\s|:\s""")).first()
        t = t.replace(Regex("""(?i)\b(odc\.?|odcinek|sezon|s\d+\s?e\d+|cz\.)\s*\d*.*$"""), " ")
        t = t.replace(Regex("""\s+\d{1,3}\s*$"""), " ")
        t = t.replace(Regex("""\s+"""), " ").trim()
        return t.takeIf { it.length >= 3 }
    }

    fun norm(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("""\p{M}+"""), "")
            .replace('ł', 'l').replace(Regex("""[^a-z0-9]+"""), " ").trim()

    /**
     * Test JMGO prz04: „Kropka nad i” (program TVN24, ~40 min) dostał plakat FILMU o tym tytule — program krótszy niż
     * [MOVIE_MIN] minut szuka tylko seriali/programów TV (osobny wpis w pamięci: klucz z „|tv”).
     */
    private const val MOVIE_MIN = 70
    private fun keyOf(q: String, minutes: Int?): String = norm(q) + if (minutes != null && minutes < MOVIE_MIN) "|tv" else ""

    /** Okładka z pamięci (bez sieci) — do pierwszego rysowania karty. */
    fun cached(title: String, minutes: Int? = null): String? = searchTitle(title)?.let { mem[keyOf(it, minutes)]?.url?.ifEmpty { null } }

    suspend fun cover(c: Context, title: String, minutes: Int? = null): String? {
        if (!enabled(c)) return null
        val q = searchTitle(title) ?: return null
        val key = keyOf(q, minutes)
        if (key.length < 3) return null
        ensureLoaded(c)
        fresh(key)?.let { return it.url.ifEmpty { null } }
        val lock = locks.getOrPut(key) { kotlinx.coroutines.sync.Mutex() }
        return lock.withLock {
            fresh(key)?.let { return@withLock it.url.ifEmpty { null } }
            val url = sem.withPermit { withContext(Dispatchers.IO) { fetch(q, norm(q), tvOnly = key.endsWith("|tv")) } }
            if (url != null) { mem[key] = Entry(url, System.currentTimeMillis()); if (url.isEmpty()) misses++ else hits++; save(c) }
            url?.ifEmpty { null }
        }
    }

    private fun fresh(key: String): Entry? {
        val e = mem[key] ?: return null
        val ttl = if (e.url.isEmpty()) MISS_TTL_MS else HIT_TTL_MS
        return e.takeIf { System.currentTimeMillis() - it.at < ttl }
    }

    /** "" = TMDB nie zna tego tytułu (pamiętane); null = błąd sieci (bez pamiętania, następnym razem znów). */
    private fun fetch(q: String, key: String, tvOnly: Boolean): String? {
        val apiKey = com.nuvio.tv.BuildConfig.TMDB_API_KEY
        if (apiKey.isBlank()) { Log.w(TAG, "brak klucza TMDB — okładki wyłączone"); return "" }
        val u = java.net.URL("https://api.themoviedb.org/3/search/multi?language=pl-PL&include_adult=false&query=" +
            java.net.URLEncoder.encode(q, "UTF-8") + "&api_key=" + apiKey)
        val conn = u.openConnection() as java.net.HttpURLConnection
        return try {
            conn.connectTimeout = 5_000; conn.readTimeout = 6_000
            if (conn.responseCode != 200) { errors++; Log.w(TAG, "„$q”: HTTP ${conn.responseCode}"); return null }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val results = JSONObject(body).optJSONArray("results") ?: JSONArray()
            var best: JSONObject? = null; var bestPop = -1.0
            for (i in 0 until results.length()) {
                val r = results.optJSONObject(i) ?: continue
                val type = r.optString("media_type")
                if (type != "tv" && (type != "movie" || tvOnly)) continue
                val names = listOf("title", "name", "original_title", "original_name").map { r.optString(it) }.filter { it.isNotBlank() }
                if (names.none { norm(it) == key }) continue
                val path = r.optString("backdrop_path").takeIf { it.startsWith("/") } ?: r.optString("poster_path").takeIf { it.startsWith("/") } ?: continue
                val pop = r.optDouble("popularity", 0.0)
                if (pop > bestPop) { best = r.put("_path", path); bestPop = pop }
            }
            val url = best?.optString("_path")?.let { IMG + it } ?: ""
            Log.i(TAG, "„$q”: ${if (url.isEmpty()) "brak zgodnego tytułu (${results.length()} wyników)" else "okładka ${best?.optString("media_type")}"} — razem trafień $hits, brak $misses, błędów $errors")
            url
        } catch (e: Exception) {
            errors++; Log.w(TAG, "„$q”: ${e.javaClass.simpleName}: ${e.message?.replace(apiKey, "…")}"); null
        } finally { conn.disconnect() }
    }

    @Synchronized private fun ensureLoaded(c: Context) {
        if (file != null) return
        val f = File(c.filesDir, "fork_tv_covers.json"); file = f
        if (!f.exists()) return
        try {
            val o = JSONObject(f.readText())
            o.keys().forEach { k -> o.optJSONArray(k)?.let { a -> mem[k] = Entry(a.optString(0), a.optLong(1)) } }
            Log.i(TAG, "wczytane z dysku: ${mem.size} tytułów (okładek ${mem.values.count { it.url.isNotEmpty() }})")
        } catch (e: Exception) { Log.w(TAG, "odczyt pamięci okładek: ${e.javaClass.simpleName}: ${e.message}") }
    }

    /** Zapis co 10 nowych tytułów (i przy pierwszym) — plik jest mały, zapis w wątku zapytania (IO). */
    @Synchronized private fun save(c: Context) {
        dirty++
        if (dirty != 1 && dirty % 10 != 0) return
        val f = file ?: File(c.filesDir, "fork_tv_covers.json")
        try {
            val o = JSONObject()
            mem.forEach { (k, e) -> o.put(k, JSONArray().put(e.url).put(e.at)) }
            val tmp = File(f.parentFile, f.name + ".tmp"); tmp.writeText(o.toString()); tmp.renameTo(f)
        } catch (e: Exception) { Log.w(TAG, "zapis pamięci okładek: ${e.javaClass.simpleName}: ${e.message}") }
    }
}
