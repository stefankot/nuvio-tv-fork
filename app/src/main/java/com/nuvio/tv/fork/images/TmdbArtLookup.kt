package com.nuvio.tv.fork.images

import android.content.Context
import android.util.Log
import com.nuvio.tv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Plakat / tło z TMDB po IMDb id (22.11, właściciel: „nie z kaptain.stream, tylko TMDB w pełnej, najwyższej
 * rozdzielczości — plakaty, backdropy, wszystko”). Metahub (Cinemeta) daje najwyżej 780 px plakatu, TMDB `original`
 * ok. 2000 px. Wynik `find` trzymany w pamięci i w `fork_tmdb_art` (raz na tytuł), brak = pusty wpis (bez ponawiania).
 */
object TmdbArtLookup {
    private const val TAG = "ForkImages"
    private const val PREFS = "fork_tmdb_art"
    private val mem = ConcurrentHashMap<String, String>() // "tt…|poster" → ścieżka TMDB albo ""

    /** Ścieżka TMDB (np. `/abc.jpg`) dla [imdb] i rodzaju „poster”/„background” albo null. */
    suspend fun path(context: Context, imdb: String, kind: String): String? {
        val key = "$imdb|$kind"
        mem[key]?.let { return it.ifEmpty { null } }
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.getString(key, null)?.let { mem[key] = it; return it.ifEmpty { null } }
        if (BuildConfig.TMDB_API_KEY.isBlank()) return null
        val found = withContext(Dispatchers.IO) {
            try {
                val c = URL("https://api.themoviedb.org/3/find/$imdb?external_source=imdb_id&api_key=${BuildConfig.TMDB_API_KEY}")
                    .openConnection() as HttpURLConnection
                c.connectTimeout = 4000; c.readTimeout = 5000
                try {
                    if (c.responseCode != 200) { Log.w(TAG, "TMDB find $imdb: HTTP ${c.responseCode}"); return@withContext null }
                    val j = JSONObject(c.inputStream.bufferedReader().readText())
                    val hit = listOf("movie_results", "tv_results").firstNotNullOfOrNull { j.optJSONArray(it)?.optJSONObject(0) }
                    mapOf(
                        "poster" to (hit?.optString("poster_path").orEmpty().takeIf { it.startsWith("/") } ?: ""),
                        "background" to (hit?.optString("backdrop_path").orEmpty().takeIf { it.startsWith("/") } ?: ""),
                    )
                } finally { c.disconnect() }
            } catch (e: Exception) {
                Log.w(TAG, "TMDB find $imdb: ${e.javaClass.simpleName}: ${e.message?.replace(BuildConfig.TMDB_API_KEY, "…")}"); null
            }
        } ?: return null // błąd sieci: bez zapisu, spróbuje następnym razem
        val e = sp.edit()
        for ((k, v) in found) { mem["$imdb|$k"] = v; e.putString("$imdb|$k", v) }
        e.apply()
        return found[kind]?.ifEmpty { null }
    }
}
