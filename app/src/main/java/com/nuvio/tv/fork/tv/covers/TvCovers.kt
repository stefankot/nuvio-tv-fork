package com.nuvio.tv.fork.tv.covers

import android.content.Context
import android.util.Log
import com.nuvio.tv.domain.model.supportsExtra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
 *
 * 27.11 (właściciel, przegląd): dane z wtyczki AIOMetadata (skonsolidowane z wielu serwisów) — katalog wyszukiwania wtyczki
 * (najpierw seriale, potem filmy), ta sama ścisła zgodność tytułu. Bez zainstalowanej wtyczki — awaryjnie TMDB jak dawniej.
 * Nowa pamięć `files/fork_tv_covers_aio.json`; stary plik TMDB zostaje obok nieruszony.
 *
 * 27.11 (właściciel: „zrób to dopytywanie o szczegóły w aplikacji”): po znalezieniu tytułu w wyszukiwaniu wtyczki
 * aplikacja pyta wtyczkę o szczegóły tytułu (`meta`) — tam obrazy idą według „Art Providers” z panelu wtyczki
 * (łączenie serwisów), a w wynikach wyszukiwania tylko z jednej wyszukiwarki. Kadr poziomy ze szczegółów, inaczej obraz
 * z wyszukiwania jak dotąd. Wyłącznik: klucz ForkFeatures [META_KEY]. Nowa pamięć `fork_tv_covers_aio3.json` (aio2 zostaje).
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

    @dagger.hilt.EntryPoint @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface Deps { fun addons(): com.nuvio.tv.domain.repository.AddonRepository }

    private const val FILE = "fork_tv_covers_aio3.json" // aio3: obraz ze szczegółów wtyczki (aio2 i starsze zostają)
    /** Wyłącznik dopytywania wtyczki o szczegóły tytułu (wyłączony = obraz z samego wyszukiwania, jak w aio2). */
    const val META_KEY = "tv_covers_aio_meta"

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
            val url = sem.withPermit { withContext(Dispatchers.IO) { fetchAio(c, q, norm(q), tvOnly = key.endsWith("|tv")) } }
            if (url != null) { mem[key] = Entry(url, System.currentTimeMillis()); save(c) }
            url?.ifEmpty { null }
        }
    }

    private fun fresh(key: String): Entry? {
        val e = mem[key] ?: return null
        val ttl = if (e.url.isEmpty()) MISS_TTL_MS else HIT_TTL_MS
        return e.takeIf { System.currentTimeMillis() - it.at < ttl }
    }

    /** Wtyczka AIOMetadata z profilu (włączona) albo null. */
    private suspend fun aioAddon(c: Context): com.nuvio.tv.domain.model.Addon? = try {
        dagger.hilt.android.EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).addons()
            .getInstalledAddons().first()
            .firstOrNull { a -> a.isActive && listOf(a.name, a.id, a.displayName).any { it.contains("aiometadata", ignoreCase = true) } }
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Log.w(TAG, "lista wtyczek: ${e.javaClass.simpleName}: ${e.message}"); null
    }

    /** Katalog wyszukiwania AIOMetadata: seriale (programy TV), potem filmy (gdy program ≥ [MOVIE_MIN] min). */
    private suspend fun fetchAio(c: Context, q: String, key: String, tvOnly: Boolean): String? {
        val aio = aioAddon(c)
        if (aio == null) {
            if (!aioMissingLogged) { aioMissingLogged = true; Log.w(TAG, "brak włączonej wtyczki AIOMetadata — okładki awaryjnie z TMDB") }
            return fetch(q, key, tvOnly)
        }
        val catalogs = aio.catalogs.filter { cat ->
            cat.supportsExtra("search") && cat.extra.none { it.isRequired && !it.name.equals("search", true) } &&
                (cat.apiType == "series" || (cat.apiType == "movie" && !tvOnly))
        }.sortedBy { if (it.apiType == "series") 0 else 1 }.distinctBy { it.apiType }
        if (catalogs.isEmpty()) { Log.w(TAG, "AIOMetadata bez katalogu wyszukiwania — okładki awaryjnie z TMDB"); return fetch(q, key, tvOnly) }
        val details = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, META_KEY, true)
        var failed = false
        for (cat in catalogs) {
            when (val r = searchAio(aio.baseUrl, cat.apiType, cat.id, q, key, details)) {
                null -> failed = true
                "" -> Unit
                else -> {
                    hits++
                    Log.i(TAG, "„$q”: okładka ${cat.apiType} z AIOMetadata — razem trafień $hits, brak $misses, błędów $errors")
                    return r
                }
            }
        }
        if (failed) return null // błąd sieci: bez pamiętania „brak”
        misses++
        Log.i(TAG, "„$q”: AIOMetadata — brak zgodnego tytułu (${catalogs.joinToString("+") { it.apiType }}) — razem trafień $hits, brak $misses, błędów $errors")
        return ""
    }

    @Volatile private var aioMissingLogged = false

    /** Jedno zapytanie do katalogu: "" = brak zgodnego tytułu, null = błąd. Adres wtyczki nie trafia do logów (konfiguracja w ścieżce). */
    private fun searchAio(baseUrl: String, type: String, catalogId: String, q: String, key: String, details: Boolean): String? {
        val base = baseUrl.trimEnd('/')
        val qs = base.indexOf('?')
        val path = if (qs >= 0) base.substring(0, qs).trimEnd('/') else base
        val query = if (qs >= 0) base.substring(qs) else ""
        val u = java.net.URL("$path/catalog/$type/${enc(catalogId)}/search=${enc(q)}.json$query")
        val conn = u.openConnection() as java.net.HttpURLConnection
        return try {
            conn.connectTimeout = 5_000; conn.readTimeout = 8_000
            if (conn.responseCode != 200) { errors++; Log.w(TAG, "„$q”: AIOMetadata $type HTTP ${conn.responseCode}"); return null }
            val metas = JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).optJSONArray("metas") ?: JSONArray()
            for (i in 0 until metas.length()) {
                val m = metas.optJSONObject(i) ?: continue
                if (key !in nameKeys(m.optString("name"))) continue
                val img = listOf("background", "poster").map { m.optString(it) }.firstOrNull { it.startsWith("http") }
                val id = m.optString("id")
                val wide = if (details && id.isNotBlank()) detailBackground(path, query, m.optString("type").ifBlank { type }, id, q) else null
                if (details) Log.i(TAG, "„$q”: " + when {
                    wide != null -> "kadr ze szczegółów wtyczki (${host(wide)})"
                    img != null -> "szczegóły bez kadru — obraz z wyszukiwania (${host(img)})"
                    else -> "brak obrazu w szczegółach i w wyszukiwaniu"
                })
                return wide ?: img ?: continue
            }
            ""
        } catch (e: Exception) {
            errors++; Log.w(TAG, "„$q”: AIOMetadata $type: ${e.javaClass.simpleName}"); null
        } finally { conn.disconnect() }
    }

    private fun enc(v: String) = java.net.URLEncoder.encode(v, "UTF-8").replace("+", "%20")
    private fun host(url: String) = url.substringAfter("://").substringBefore('/')

    /**
     * Szczegóły tytułu z wtyczki (`meta/<typ>/<id>.json`): obrazy według „Art Providers” z panelu wtyczki.
     * Zwraca poziomy kadr (`background`) albo null (brak kadru albo błąd — wtedy zostaje obraz z wyszukiwania).
     * Adres wtyczki nie trafia do logów (konfiguracja w ścieżce).
     */
    private fun detailBackground(path: String, query: String, type: String, id: String, q: String): String? {
        val conn = java.net.URL("$path/meta/${enc(type)}/${enc(id)}.json$query").openConnection() as java.net.HttpURLConnection
        return try {
            conn.connectTimeout = 5_000; conn.readTimeout = 10_000
            if (conn.responseCode != 200) { Log.w(TAG, "„$q”: szczegóły AIOMetadata $type HTTP ${conn.responseCode}"); return null }
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).optJSONObject("meta")
                ?.optString("background")?.takeIf { it.startsWith("http") }
        } catch (e: Exception) {
            Log.w(TAG, "„$q”: szczegóły AIOMetadata $type: ${e.javaClass.simpleName}: ${e.message?.replace(path, "<wtyczka>")}"); null
        } finally { conn.disconnect() }
    }

    /**
     * Test JMGO prz07: AIOMetadata podaje nazwę po polsku bez oryginalnej („Hell's Kitchen. Piekielna kuchnia”), więc
     * „Hell's Kitchen” trafiało tylko w film o tej nazwie. Klucze nazwy = cała nazwa i jej części przed/po „.”, „:”, „–”, „-”, „|”.
     */
    internal fun nameKeys(name: String): Set<String> =
        (listOf(name) + name.split(Regex("""\.\s|\s[-–—:|]\s|:\s"""))).map { norm(it) }.filter { it.length >= 3 }.toSet()

    /** Awaryjnie (bez AIOMetadata): "" = TMDB nie zna tego tytułu (pamiętane); null = błąd sieci (bez pamiętania, następnym razem znów). */
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
            if (url.isEmpty()) misses++ else hits++
            Log.i(TAG, "„$q”: ${if (url.isEmpty()) "brak zgodnego tytułu (${results.length()} wyników)" else "okładka ${best?.optString("media_type")}"} — razem trafień $hits, brak $misses, błędów $errors")
            url
        } catch (e: Exception) {
            errors++; Log.w(TAG, "„$q”: ${e.javaClass.simpleName}: ${e.message?.replace(apiKey, "…")}"); null
        } finally { conn.disconnect() }
    }

    @Synchronized private fun ensureLoaded(c: Context) {
        if (file != null) return
        val f = File(c.filesDir, FILE); file = f
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
        val f = file ?: File(c.filesDir, FILE)
        try {
            val o = JSONObject()
            mem.forEach { (k, e) -> o.put(k, JSONArray().put(e.url).put(e.at)) }
            val tmp = File(f.parentFile, f.name + ".tmp"); tmp.writeText(o.toString()); tmp.renameTo(f)
        } catch (e: Exception) { Log.w(TAG, "zapis pamięci okładek: ${e.javaClass.simpleName}: ${e.message}") }
    }
}
