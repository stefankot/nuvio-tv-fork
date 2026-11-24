package com.nuvio.tv.skin.niunia.awards

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Z-13 (24.11, właściciel: „wybierz sam źródło danych”): nagrody i nominacje z Wikidata. TMDB nie ma nagród, OMDb daje
 * tylko jedno zdanie bez kategorii (i wymaga klucza), strony IMDb nie wolno pobierać automatem. Wikidata: bezpłatne, bez
 * klucza, tytuł po identyfikatorze IMDb (P345), nagrody „award received” (P166) i „nominated for” (P1411) z rokiem (P585).
 *
 * Gala z nazwy nagrody (rejestr [NiuniaAwardBodies]; spoza rejestru — nazwa z danych, poziom 4), kategoria z polskiej
 * nazwy nagrody (inaczej angielskiej) bez nazwy gali. Wynik zapisany na dysku na 7 dni (po restarcie bez zapytania);
 * zapytanie tylko dla tytułów z identyfikatorem IMDb. W zapytaniu nie ma danych użytkownika (tylko id tytułu).
 */
class NiuniaWikidataAwards(private val context: Context) : NiuniaAwardsSource {
    override val name = "Wikidata"

    override suspend fun awards(id: String, type: String, imdbId: String?): List<NiuniaAward> {
        val imdb = (imdbId ?: id).takeIf { IMDB.matches(it) } ?: run {
            Log.i(AWARDS_TAG, "NAwards: $id bez identyfikatora IMDb — Wikidata pominięte")
            return emptyList()
        }
        return withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, "niunia-awards/$imdb.json")
            if (file.exists() && System.currentTimeMillis() - file.lastModified() < VALID_MS) {
                try { return@withContext decode(JSONArray(file.readText())).also { Log.i(AWARDS_TAG, "NAwards: $imdb z dysku (${it.size})") } }
                catch (e: Exception) { Log.w(AWARDS_TAG, "NAwards: zapis $imdb uszkodzony: ${e.javaClass.simpleName}: ${e.message}") }
            }
            // Test JMGO tv2c: pierwsze zapytanie przekroczyło 12 s (SocketTimeoutException), z Maca 1,7 s — dłuższy limit i jedna ponowna próba.
            val rows = try { query(imdb) } catch (e: java.net.SocketTimeoutException) {
                Log.w(AWARDS_TAG, "NAwards: Wikidata $imdb: ${e.javaClass.simpleName}: ${e.message} — ponawiam raz"); query(imdb)
            }
            val list = parse(rows)
            try {
                file.parentFile?.mkdirs()
                file.writeText(encode(list).toString())
            } catch (e: Exception) { Log.w(AWARDS_TAG, "NAwards: zapis $imdb na dysk: ${e.javaClass.simpleName}: ${e.message}") }
            list
        }
    }

    private fun query(imdb: String): JSONArray {
        val sparql = """
            SELECT ?award ?pl ?en ?time ?kind WHERE {
              ?t wdt:P345 "$imdb" .
              { ?t p:P166 ?st . ?st ps:P166 ?award . BIND("won" AS ?kind) }
              UNION
              { ?t p:P1411 ?st . ?st ps:P1411 ?award . BIND("nom" AS ?kind) }
              OPTIONAL { ?st pq:P585 ?time . }
              OPTIONAL { ?award rdfs:label ?pl . FILTER(LANG(?pl) = "pl") }
              OPTIONAL { ?award rdfs:label ?en . FILTER(LANG(?en) = "en") }
            } LIMIT 400
        """.trimIndent()
        val url = URL("https://query.wikidata.org/sparql?format=json&query=" + URLEncoder.encode(sparql, "UTF-8"))
        val c = url.openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 25_000
        c.setRequestProperty("Accept", "application/sparql-results+json")
        c.setRequestProperty("User-Agent", "NuvioTvFork/1.0 (Android TV app; awards lookup)")
        try {
            val code = c.responseCode
            if (code != 200) throw java.io.IOException("Wikidata HTTP $code")
            val text = c.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(text).getJSONObject("results").getJSONArray("bindings")
        } finally { c.disconnect() }
    }

    private fun parse(rows: JSONArray): List<NiuniaAward> {
        // Klucz: nagroda + rok; wygrana wypiera nominację tej samej nagrody w tym samym roku (Wikidata często ma obie).
        val out = LinkedHashMap<String, NiuniaAward>()
        for (i in 0 until rows.length()) {
            val r = rows.getJSONObject(i)
            fun v(k: String) = r.optJSONObject(k)?.optString("value")?.takeIf { it.isNotBlank() }
            val award = v("award") ?: continue
            val en = v("en") ?: continue
            val pl = v("pl")
            val year = v("time")?.take(4)?.toIntOrNull()
            val won = v("kind") == "won"
            val body = bodyOf(en)
            val award2 = NiuniaAward(
                body = body.key, year = year, category = category(pl ?: en, body),
                result = if (won) NiuniaAwardResult.Won else NiuniaAwardResult.Nominated,
                name = if (body.tier == 4) body.name else null, short = if (body.tier == 4) body.short else null,
            )
            val key = award.substringAfterLast('/') + "|" + (year ?: "")
            val old = out[key]
            if (old == null || (won && old.result != NiuniaAwardResult.Won)) out[key] = award2
        }
        val list = out.values.toList()
        Log.i(AWARDS_TAG, "NAwards: Wikidata ${rows.length()} wierszy → ${list.count { it.result == NiuniaAwardResult.Won }} nagród, ${list.count { it.result == NiuniaAwardResult.Nominated }} nominacji")
        return list
    }

    private fun encode(list: List<NiuniaAward>) = JSONArray().apply {
        list.forEach { a ->
            put(JSONObject().put("b", a.body).put("y", a.year ?: JSONObject.NULL).put("c", a.category).put("r", a.result.name)
                .put("n", a.name ?: JSONObject.NULL).put("s", a.short ?: JSONObject.NULL))
        }
    }

    private fun decode(a: JSONArray): List<NiuniaAward> = (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        NiuniaAward(
            body = o.getString("b"), year = if (o.isNull("y")) null else o.getInt("y"), category = o.getString("c"),
            result = NiuniaAwardResult.valueOf(o.getString("r")),
            name = if (o.isNull("n")) null else o.getString("n"), short = if (o.isNull("s")) null else o.getString("s"),
        )
    }

    companion object {
        private val IMDB = Regex("tt\\d{5,10}")
        private const val VALID_MS = 7L * 24 * 3600 * 1000

        /** Angielska nazwa nagrody → gala z rejestru (kolejność ważna: najpierw nazwy dłuższe i pewniejsze). */
        private val BODY_WORDS: List<Pair<String, String>> = listOf(
            "academy award" to "oscar", "oscar" to "oscar",
            "palme d'or" to "cannes", "cannes" to "cannes",
            "golden bear" to "berlinale", "silver bear" to "berlinale", "berlin" to "berlinale",
            "golden lion" to "venice", "volpi cup" to "venice", "venice" to "venice",
            "sundance" to "sundance",
            "emmy" to "emmy",
            "golden globe" to "globe",
            "bafta" to "bafta", "british academy" to "bafta",
            "saturn award" to "saturn",
            "magritte" to "magritte",
            "japan academy" to "jafp",
            "baeksang" to "baeksang",
            "british independent film award" to "bifa",
            "visual effects society" to "ves",
            "art directors guild" to "adg",
            "screen actors guild" to "sag",
            "writers guild of america" to "wga",
            "critics' choice" to "cca", "critics choice" to "cca",
            "crunchyroll" to "crunchyroll",
            "grammy" to "grammy",
        )

        internal fun bodyOf(en: String): NiuniaAwardBody {
            val low = en.lowercase()
            BODY_WORDS.firstOrNull { low.contains(it.first) }?.let { return NiuniaAwardBodies.of(it.second) }
            // Spoza rejestru: nazwa gali = nazwa nagrody bez „for …” („Annie Award for Best Feature” → „Annie Award”).
            val gala = en.substringBefore(" for ").substringBefore(" – ").trim()
            return NiuniaAwardBodies.of("wd:" + gala.lowercase().filter { it.isLetterOrDigit() }.take(24), gala, gala.take(16))
        }

        /** Kategoria bez nazwy gali: „Oscar za najlepszy film” → „Najlepszy film”, „Academy Award for Best Picture” → „Best Picture”. */
        internal fun category(label: String, body: NiuniaAwardBody): String {
            var t = label
            for (sep in listOf(" za ", " for ", " dla ", " – ", ": ")) {
                val i = t.indexOf(sep)
                if (i in 1..48) { t = t.substring(i + sep.length); break }
            }
            if (t == label && t.startsWith(body.name, ignoreCase = true)) t = t.substring(body.name.length).trim()
            return t.trim().replaceFirstChar { it.titlecase() }.ifEmpty { label }
        }
    }
}
