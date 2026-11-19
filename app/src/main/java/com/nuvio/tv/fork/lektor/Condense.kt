package com.nuvio.tv.fork.lektor

import android.content.Context
import android.util.Log
import com.nuvio.tv.fork.ai.subtitles.AiKeyStore
import com.nuvio.tv.fork.ai.subtitles.AiSettings
import com.nuvio.tv.fork.ai.subtitles.GeminiProvider
import com.nuvio.tv.fork.ai.subtitles.ModelRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Fork 19.11 (właściciel: „czyta tylko niektóre, żeby zaraz potem przerwać, gdy jest dużo tekstu”): kwestia czytana
 * dłużej niż czas do następnej była ucinana przez następną. Jak prawdziwy polski lektor — zbyt długie kwestie
 * skracamy (streszczamy) PRZED syntezą: jedno zapytanie do modelu tekstowego Gemini (ten sam klucz i model co AI
 * napisy, limit 10 000/dzień) na do [BATCH] kwestii. Długość mowy szacowana z rzeczywistych nagrań ([learn]).
 * Bez klucza albo przy błędzie: tekst bez zmian (wtedy zostaje przyspieszenie / cięcie na granicy zdania w [Tempo]).
 */
object Condense {
    private const val TAG = "ForkLektor"
    private const val BATCH = 30
    /** Skracamy, gdy szacowana mowa przekracza czas (fork 20.11, właściciel: „ma skrócić, a nie mówić szybciej” — bez zapasu na przyspieszenie). */
    private const val TRIGGER = 1.0f
    @Volatile private var msPerChar = 65.0
    private val short = ConcurrentHashMap<Long, String>()   // start kwestii → skrócony tekst
    private val tried = ConcurrentHashMap.newKeySet<Long>()  // próbowane (także nieudane) — bez ponawiania
    @Volatile private var failStreak = 0

    fun reset() { short.clear(); tried.clear(); failStreak = 0; one.clear() }
    private val one = ConcurrentHashMap<String, String>() // tekst|maks → skrót (jedna kwestia po nagraniu)

    /** Uczy się tempa mowy z gotowego nagrania (tempo 1,0). */
    fun learn(text: String, durationMs: Long, rate: Float = 1f) {
        if (text.length < 15 || durationMs <= 0) return
        val v = durationMs * rate / text.length
        if (v in 30.0..200.0) msPerChar = msPerChar * 0.9 + v * 0.1
    }

    fun estimateMs(text: String): Long = (text.length * msPerChar).toLong()
    fun msPerCharNow(): Double = msPerChar

    private fun tooLong(l: Line) = estimateMs(l.text) > l.availableMs * TRIGGER

    private fun maxChars(l: Line) = (l.availableMs * 0.95 / msPerChar).toInt().coerceAtLeast(8)

    /** Kwestie z tekstem skróconym tam, gdzie trzeba (raz na kwestię). */
    suspend fun apply(app: Context, lines: List<Line>): List<Line> {
        val todo = lines.filter { it.startMs !in tried && !short.containsKey(it.startMs) && tooLong(it) }.take(BATCH)
        if (todo.isNotEmpty() && failStreak < 3) runCatching { condense(app, todo) }.onFailure { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            failStreak++
            Log.w(TAG, "skracanie kwestii: ${e.javaClass.simpleName}: ${e.message?.take(160)}")
        }
        return lines.map { l -> short[l.startMs]?.let { l.copy(text = it) } ?: l }
    }

    private suspend fun condense(app: Context, todo: List<Line>) {
        todo.forEach { tried += it.startMs }
        val t0 = System.currentTimeMillis()
        val got = ask(app, todo.map { it.text to maxChars(it) }) ?: return
        var n = 0
        got.forEach { (i, t) -> val l = todo.getOrNull(i) ?: return@forEach; if (t.length < l.text.length) { short[l.startMs] = t; n++ } }
        Log.i(TAG, "skrócono $n/${todo.size} zbyt długich kwestii w ${System.currentTimeMillis() - t0} ms (tempo mowy ${msPerChar.toInt()} ms/znak)")
    }

    /**
     * Fork 20.11 (właściciel: „gdy kwestia jest za długa, powinien ją skrócić, a nie mówić szybciej”): jedna kwestia, której
     * gotowe nagranie nie mieści się w czasie — skrót do [maxChars] znaków. null = bez klucza / błąd / nie krócej.
     */
    suspend fun shortenOne(app: Context, text: String, maxChars: Int): String? {
        val k = "$maxChars|$text"
        one[k]?.let { return it }
        if (failStreak >= 3) return null
        val r = runCatching { ask(app, listOf(text to maxChars)) }.onFailure { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            failStreak++; Log.w(TAG, "skracanie kwestii (po nagraniu): ${e.javaClass.simpleName}: ${e.message?.take(160)}")
        }.getOrNull()?.get(0)?.takeIf { it.length < text.length } ?: return null
        one[k] = r
        return r
    }

    /** Zapytanie do modelu tekstowego: (tekst, maks. znaków) → numer → skrót. null = brak klucza. */
    private suspend fun ask(app: Context, items: List<Pair<String, Int>>): Map<Int, String>? {
        val key = AiKeyStore.read(app)?.takeIf { it.isNotBlank() } ?: run { Log.i(TAG, "skracanie kwestii: brak klucza Gemini — bez skracania"); failStreak = 3; return null }
        val provider = GeminiProvider(key, AiSettings.model(app), thinkingLevel = "low")
        val list = items.mapIndexed { i, (t, max) -> "$i\t$max\t$t" }.joinToString("\n")
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { putJsonObject("items") {
                put("type", "array")
                putJsonObject("items") {
                    put("type", "object")
                    putJsonObject("properties") { putJsonObject("i") { put("type", "integer") }; putJsonObject("t") { put("type", "string") } }
                    putJsonArray("required") { add(kotlinx.serialization.json.JsonPrimitive("i")); add(kotlinx.serialization.json.JsonPrimitive("t")) }
                }
            } }
            putJsonArray("required") { add(kotlinx.serialization.json.JsonPrimitive("items")) }
        }
        val res = provider.complete(LektorDump.text(app, "skrot", provider, ModelRequest(
            system = SYSTEM,
            context = "Kwestie (numer TAB maks. znaków TAB tekst):\n$list",
            task = "Zwróć JSON {\"items\":[{\"i\":numer,\"t\":\"skrócony tekst\"}]} dla każdej kwestii.",
            schema = schema,
            maxOutputTokens = 4_000,
        )))
        val out = HashMap<Int, String>()
        Json.parseToJsonElement(res.text).jsonObject["items"]?.jsonArray.orEmpty().forEach { el ->
            val o = el as? JsonObject ?: return@forEach
            val i = o["i"]?.jsonPrimitive?.int ?: return@forEach
            val t = o["t"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (t.isNotEmpty() && i in items.indices) out[i] = t
        }
        failStreak = 0
        return out
    }

    const val SYSTEM = "Jesteś polskim lektorem filmowym. Lektor nie czyta napisów słowo w słowo, tylko je zwięźle streszcza, " +
        "tak żeby zdążyć przed następną kwestią. Skróć każdą kwestię do podanej maksymalnej liczby znaków, zachowując sens, " +
        "ton i najważniejsze informacje (imiona, liczby, pytania). Nie dodawaj niczego od siebie, bez komentarzy i cudzysłowów."
}
