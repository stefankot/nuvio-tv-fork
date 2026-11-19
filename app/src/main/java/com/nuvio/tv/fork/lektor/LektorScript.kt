package com.nuvio.tv.fork.lektor

import android.content.Context
import android.util.Log
import com.nuvio.tv.fork.ai.subtitles.AiKeyStore
import com.nuvio.tv.fork.ai.subtitles.AiSettings
import com.nuvio.tv.fork.ai.subtitles.GeminiProvider
import com.nuvio.tv.fork.ai.subtitles.ModelPricing
import com.nuvio.tv.fork.ai.subtitles.ModelRequest
import com.nuvio.tv.fork.sources.PlayerBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Fork 19.11 (właściciel: „Gemini najpierw rozpisze w dłuższych fragmentach: opis filmu, scen, bohaterów, kto co mówi”;
 * strategia `plan/LEKTOR_GEMINI_STRATEGIA.md`, etap 1): scenariusz lektora z modelu tekstowego Gemini (klucz i model
 * AI napisów). W KAŻDYM zapytaniu pełny kontekst: tytuł, rok, typ, opis, obsada z postaciami i cały tekst napisów
 * z czasami; Gemini rozpisuje wskazany zakres kwestii: scena (opis), kto mówi, emocja, a gdy kwestia nie mieści się
 * w czasie — skrócony tekst lektorski. Tryby ([LektorPrefs.scriptMode]): porcjami po 10 min albo cały film od razu.
 * Wynik na dysku (ponowne oglądanie bez kosztu). Gdy scenariusz zawiedzie — kwestie idą zwykłym stylem.
 * W logach bez tekstu dialogów.
 */
object LektorScript {
    private const val TAG = "ForkLektor"
    private const val WINDOW_MS = 10 * 60_000L
    private const val FIRST_BATCH = 40
    private const val BATCH = 150

    data class Entry(val scene: Int, val speaker: String, val emotion: String, val text: String?)

    private val entries = ConcurrentHashMap<Long, Entry>()
    private val scenes = ConcurrentHashMap<Int, String>()
    private val failed = ConcurrentHashMap.newKeySet<Long>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var job: Job? = null
    @Volatile private var key = ""
    @Volatile private var file: File? = null
    @Volatile private var failStreak = 0
    @Volatile private var batches = 0

    fun reset() { job?.cancel(); job = null; entries.clear(); scenes.clear(); failed.clear(); key = ""; file = null; failStreak = 0; batches = 0 }

    /** Kwestia gotowa do nagrania (ma scenariusz albo scenariusz dla niej zawiódł). */
    fun ready(startMs: Long) = entries.containsKey(startMs) || failed.contains(startMs)
    fun get(startMs: Long): Entry? = entries[startMs]
    fun progress(total: Int) = "${entries.size + failed.size}/$total"

    /** Polecenie stylu dla kwestii: styl lektora + opis sceny + kto mówi + emocja. */
    fun styleFor(c: Context, startMs: Long): String {
        val e = entries[startMs] ?: return LektorPrefs.baseStyle(c)
        val scene = scenes[e.scene]?.let { " Scena: $it." }.orEmpty()
        val who = e.speaker.takeIf { it.isNotBlank() && it != "?" }?.let { " Mówi: $it." }.orEmpty()
        val emo = e.emotion.takeIf { it.isNotBlank() }?.let { " Nastrój i sposób podania: $it." }.orEmpty()
        return LektorPrefs.baseStyle(c) + scene + who + emo
    }

    /** Uruchamia przygotowanie następnej porcji (w tle), jeśli trzeba. Bez klucza — wszystko „gotowe” zwykłym stylem. */
    fun ensure(app: Context, lines: List<Line>, posMs: Long) {
        if (job?.isActive == true || lines.isEmpty()) return
        val apiKey = AiKeyStore.read(app)
        if (apiKey.isNullOrBlank() || failStreak >= 3) { lines.forEach { if (!entries.containsKey(it.startMs)) failed += it.startMs }; return }
        val titleKey = titleKey(lines)
        if (titleKey != key) { reset(); key = titleKey; file = File(File(app.filesDir, "lektor-script"), "${titleKey.hashCode().toUInt().toString(16)}.json"); load() }
        val whole = LektorPrefs.scriptMode(app) == "whole"
        val todo = lines.filter { !ready(it.startMs) && (whole || it.startMs in (posMs - 2_000)..(posMs + WINDOW_MS)) }
            .sortedBy { kotlin.math.abs(it.startMs - posMs).let { d -> if (it.startMs >= posMs - 2_000) d else d + 10_000_000 } } // najpierw od pozycji w przód
        if (todo.isEmpty()) return
        val first = todo.first().startMs
        val batch = lines.filter { it.startMs >= first && !ready(it.startMs) }.sortedBy { it.startMs }.take(if (batches == 0) FIRST_BATCH else BATCH)
        if (batch.isEmpty()) return
        job = scope.launch {
            val t0 = System.currentTimeMillis()
            runCatching { request(app, apiKey, lines, batch) }
                .onSuccess { n ->
                    failStreak = 0; batches++
                    batch.forEach { if (!entries.containsKey(it.startMs)) failed += it.startMs } // pominięte przez model: zwykły styl
                    Log.i(TAG, "scenariusz: $n/${batch.size} kwestii rozpisanych w ${System.currentTimeMillis() - t0} ms (razem ${progress(lines.size)}, scen ${scenes.size})")
                    save()
                }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    failStreak++
                    Log.w(TAG, "scenariusz nieudany ($failStreak): ${e.javaClass.simpleName}: ${e.message?.take(160)}")
                    batch.forEach { failed += it.startMs }
                    if (failStreak == 1) LektorController.say(app, "Lektor: scenariusz Gemini nieudany — czytam bez opisu scen (${e.message?.take(60) ?: "błąd"})")
                }
        }
    }

    private fun titleKey(lines: List<Line>): String {
        val s = PlayerBridge.current()
        return "${s?.contentName.orEmpty()}|${s?.title.orEmpty()}|${s?.releaseYear.orEmpty()}|${lines.size}|${lines.sumOf { it.startMs % 100_000 }}"
    }

    private fun filmContext(): String {
        val s = PlayerBridge.current()
        val sb = StringBuilder()
        s?.contentName?.takeIf { it.isNotBlank() }?.let { sb.append("Serial: ").append(it).append('\n') }
        sb.append("Tytuł: ").append(s?.title.orEmpty()).append('\n')
        s?.releaseYear?.let { sb.append("Rok: ").append(it).append('\n') }
        s?.contentType?.let { sb.append("Typ: ").append(it).append('\n') }
        s?.description?.takeIf { it.isNotBlank() }?.let { sb.append("Opis: ").append(it.take(3_000)).append('\n') }
        val cast = s?.castMembers.orEmpty().take(30).joinToString("\n") { m -> "- ${m.name}" + (m.character?.takeIf { it.isNotBlank() }?.let { " jako $it" } ?: "") }
        if (cast.isNotBlank()) sb.append("Obsada (aktor jako postać):\n").append(cast).append('\n')
        return sb.toString()
    }

    private fun mmss(ms: Long) = "%d:%02d".format(java.util.Locale.ROOT, ms / 60_000, ms / 1000 % 60)

    private suspend fun request(app: Context, apiKey: String, all: List<Line>, batch: List<Line>): Int {
        val model = AiSettings.model(app)
        val provider = GeminiProvider(apiKey, model, thinkingLevel = "low")
        val idx = all.withIndex().associate { it.value.startMs to it.index }
        val subtitles = all.withIndex().joinToString("\n") { (i, l) -> "$i [${mmss(l.startMs)}] ${l.text}" }
        val wanted = batch.joinToString("\n") { l ->
            val max = (l.availableMs / Condense.msPerCharNow()).toInt().coerceAtLeast(8)
            "${idx[l.startMs]}\tmaks $max znaków"
        }
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("scenes") { put("type", "array"); putJsonObject("items") { put("type", "object")
                    putJsonObject("properties") { putJsonObject("s") { put("type", "integer") }; putJsonObject("d") { put("type", "string") } }
                    putJsonArray("required") { add(JsonPrimitive("s")); add(JsonPrimitive("d")) } } }
                putJsonObject("lines") { put("type", "array"); putJsonObject("items") { put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("i") { put("type", "integer") }; putJsonObject("s") { put("type", "integer") }
                        putJsonObject("w") { put("type", "string") }; putJsonObject("e") { put("type", "string") }; putJsonObject("t") { put("type", "string") }
                    }
                    putJsonArray("required") { add(JsonPrimitive("i")); add(JsonPrimitive("s")); add(JsonPrimitive("w")); add(JsonPrimitive("e")) } } }
            }
            putJsonArray("required") { add(JsonPrimitive("scenes")); add(JsonPrimitive("lines")) }
        }
        val known = scenes.entries.sortedBy { it.key }.takeLast(30).joinToString("\n") { "${it.key}: ${it.value}" }
        val res = provider.complete(ModelRequest(
            system = "Jesteś reżyserem polskiego lektora filmowego. Na podstawie opisu filmu, obsady i pełnego tekstu napisów " +
                "rozpisujesz kwestie dla lektora: do której sceny należy (krótki opis sceny: miejsce, sytuacja, nastrój), kto mówi " +
                "(imię postaci z obsady, a gdy nie wiadomo — krótki opis np. „policjant”, „?”), z jaką emocją i jak to podać " +
                "(2–6 słów, np. „ze złością, podniesionym głosem”, „szeptem, ze strachem”, „ironicznie”). Polski lektor czyta jednym " +
                "głosem i nie gra ról — emocja ma tylko lekko zabarwić intonację. Jeśli kwestia jest dłuższa niż podany limit znaków, " +
                "podaj w „t” skrócony tekst lektorski (zwięzłe streszczenie z zachowaniem sensu, imion i pytań, jak robi to lektor); " +
                "jeśli się mieści — pomiń „t”. Nie dodawaj treści, której nie ma w napisach.",
            context = "KONTEKST FILMU\n${filmContext()}\nPEŁNE NAPISY (numer [czas] tekst):\n$subtitles",
            task = (if (known.isNotBlank()) "Sceny już opisane (kontynuuj numerację, używaj tych samych numerów dla tych samych scen):\n$known\n\n" else "") +
                "Rozpisz te kwestie (numer TAB limit znaków):\n$wanted\n\nZwróć JSON {\"scenes\":[{\"s\":nr,\"d\":\"opis sceny\"}],\"lines\":[{\"i\":numer,\"s\":nr sceny,\"w\":\"kto mówi\",\"e\":\"emocja\",\"t\":\"tekst skrócony (opcjonalnie)\"}]}.",
            schema = schema,
            maxOutputTokens = 24_000,
        ))
        res.usage?.let { u -> ModelPricing.priceFor(model)?.let { p -> LektorSpend.add("scenariusz", ModelPricing.actualCost(p, u)) } }
        val root = Json.parseToJsonElement(res.text).jsonObject
        root["scenes"]?.jsonArray?.forEach { el -> val o = el as? JsonObject ?: return@forEach
            val s = o["s"]?.jsonPrimitive?.intOrNull ?: return@forEach
            o["d"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { scenes[s] = it.take(300) } }
        val byIdx = all.withIndex().associate { it.index to it.value }
        var n = 0
        root["lines"]?.jsonArray?.forEach { el -> val o = el as? JsonObject ?: return@forEach
            val l = o["i"]?.jsonPrimitive?.intOrNull?.let { byIdx[it] } ?: return@forEach
            val t = o["t"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() && it.length < l.text.length }
            entries[l.startMs] = Entry(o["s"]?.jsonPrimitive?.intOrNull ?: 0, o["w"]?.jsonPrimitive?.contentOrNull.orEmpty().take(60),
                o["e"]?.jsonPrimitive?.contentOrNull.orEmpty().take(80), t)
            n++
        }
        return n
    }

    private fun save() = runCatching {
        val f = file ?: return@runCatching
        f.parentFile?.mkdirs()
        val o = JSONObject()
        o.put("scenes", JSONObject().apply { scenes.forEach { (k, v) -> put(k.toString(), v) } })
        o.put("lines", JSONArray().apply { entries.forEach { (k, e) -> put(JSONObject().put("t0", k).put("s", e.scene).put("w", e.speaker).put("e", e.emotion).put("t", e.text ?: "")) } })
        f.writeText(o.toString())
    }.onFailure { Log.w(TAG, "scenariusz: zapis: ${it.javaClass.simpleName}: ${it.message?.take(120)}") }

    private fun load() = runCatching {
        val f = file?.takeIf { it.isFile } ?: return@runCatching
        val o = JSONObject(f.readText())
        o.optJSONObject("scenes")?.let { s -> s.keys().forEach { k -> scenes[k.toInt()] = s.getString(k) } }
        o.optJSONArray("lines")?.let { a -> for (i in 0 until a.length()) { val e = a.getJSONObject(i)
            entries[e.getLong("t0")] = Entry(e.optInt("s"), e.optString("w"), e.optString("e"), e.optString("t").takeIf { it.isNotEmpty() }) } }
        Log.i(TAG, "scenariusz z dysku: ${entries.size} kwestii, ${scenes.size} scen")
    }.onFailure { Log.w(TAG, "scenariusz: odczyt: ${it.javaClass.simpleName}: ${it.message?.take(120)}") }
}
