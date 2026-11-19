package com.nuvio.tv.ainarrator

import android.content.Context
import android.util.Base64
import android.util.Log
import com.nuvio.tv.fork.lektor.Lektor
import com.nuvio.tv.fork.lektor.Pcm
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Fork 19.11 (właściciel: „Tak, Chirp 3 HD”): głos lektora z Google Cloud Text-to-Speech, Chirp 3 HD pl-PL.
 * Powód: Gemini TTS w AI Studio ma na Tier 1 limit 100 zapytań/dzień; Cloud TTS Chirp 3 HD — 200 zapytań/min,
 * bez limitu dziennego, 1 mln znaków/miesiąc za darmo (potem 30 USD / 1 mln znaków).
 * Klucz: `local.properties` GOOGLE_TTS_API_KEY → BuildConfig (osobny klucz Cloud, ograniczony do Text-to-Speech API;
 * klucz z AI Studio jest związany z kontem usługi i Cloud TTS go nie przyjmuje).
 * Sceny jednym zapytaniem ([synthesizeScene]), zapas kwestia po kwestii; pamięć podręczna na dysku, licznik znaków w miesiącu (log).
 */
class ChirpLektor(context: Context) : Lektor, com.nuvio.tv.fork.lektor.SceneLektor {
    private val app = context.applicationContext
    private val cache = NarratorAudioCache(File(app.cacheDir, "lektor-chirp"))

    override fun voices(): List<String> = CHIRP_VOICES

    override suspend fun synthesize(text: String, rate: Float): Pcm {
        val key = apiKey()
        if (key.isBlank()) throw Lektor.Unavailable("Brak klucza Google Cloud TTS (lektor Chirp)")
        val clean = NarratorText.clean(text)
        if (clean.isBlank()) throw Lektor.Unavailable("Pusta kwestia")
        val voice = voice(app)
        val speed = rate.coerceIn(0.75f, 2.0f) * BASE_RATE
        val id = NarratorText.fingerprint("chirp3hd/$voice/${"%.2f".format(java.util.Locale.ROOT, speed)}", clean, "")
        cache.find(id, touch = true)?.readBytes()?.let { return GeminiLektor.pcmFromWav(it) }
        val wav = try { request(key, voice, clean, speed) } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Chirp TTS $voice: ${e.javaClass.simpleName}: ${e.message?.take(160)}")
            throw Lektor.Unavailable("Błąd głosu Google Chirp: ${e.message?.take(80) ?: "połączenie"}")
        }
        runCatching { cache.store(id, wav) }.onFailure { Log.w(TAG, "Chirp: zapis w pamięci podręcznej: ${it.javaClass.simpleName}: ${it.message?.take(120)}") } // fork 19.11: błąd pamięci podręcznej nie przerywa głosu
        countChars(clean.length)
        return GeminiLektor.pcmFromWav(wav)
    }

    /**
     * Fork 19.11 (właściciel: „miał być mechanizm dzielenia na sceny i wysyłania scenami”): cała scena jednym zapytaniem —
     * kwestie w polu `markup` rozdzielone `[pause long]` (test 19.11: pauzy 1,1–1,5 s, inne ciche miejsca ≤0,3 s),
     * nagranie cięte po ciszy ([com.nuvio.tv.fork.lektor.SilenceSplit]). null = cięcie się nie zgadza (kontroler czyta pojedynczo).
     */
    /** Limit Cloud TTS: 5000 bajtów na zapytanie (dokumentacja „Quotas and limits”, 19.11); zapas na znaczniki i czyszczenie. */
    // Test JMGO 19.11: scena 4700 B (53 kwestie) = nagranie > 8 MB (limit pamięci podręcznej), 68 s czekania, zacinanie obrazu
    // (odpowiedź ~15 MB w base64 na 32-bitowym TV). ~1500 B ≈ 60–90 s mowy ≈ 3–4 MB — nadal kilkanaście kwestii kontekstu.
    override val sceneMaxBytes: Int get() = 1_500

    override suspend fun synthesizeScene(texts: List<String>): List<Pcm>? {
        if (texts.size < 2) return null
        val key = apiKey()
        if (key.isBlank()) throw Lektor.Unavailable("Brak klucza Google Cloud TTS (lektor Chirp)")
        // Nawiasy kwadratowe w napisach udawałyby znaczniki markup.
        val cleaned = texts.map { NarratorText.clean(it).replace('[', '(').replace(']', ')') }
        if (cleaned.any { it.isBlank() }) return null
        val markup = cleaned.joinToString(" [pause long] ")
        if (markup.toByteArray(Charsets.UTF_8).size > 4_800) return null // limit Cloud TTS: 5000 bajtów na zapytanie
        val voice = voice(app)
        val id = NarratorText.fingerprint("chirp3hd-scene/$voice/${"%.2f".format(java.util.Locale.ROOT, BASE_RATE)}", markup, "")
        val wav = cache.find(id, touch = true)?.readBytes() ?: try {
            request(key, voice, markup, BASE_RATE, asMarkup = true).also { w -> runCatching { cache.store(id, w) }.onFailure { Log.w(TAG, "Chirp: zapis sceny w pamięci podręcznej: ${it.javaClass.simpleName}: ${it.message?.take(120)}") }; countChars(cleaned.sumOf { it.length }) }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Chirp TTS scena ${cleaned.size} kwestii, $voice: ${e.javaClass.simpleName}: ${e.message?.take(160)}")
            throw Lektor.Unavailable("Błąd głosu Google Chirp: ${e.message?.take(80) ?: "połączenie"}")
        }
        val pcm = GeminiLektor.pcmFromWav(wav)
        val parts = com.nuvio.tv.fork.lektor.SilenceSplit.split(pcm, cleaned.size)
        if (parts == null) Log.i(TAG, "Chirp scena ${cleaned.size} kwestii (${pcm.durationMs} ms): cięcie po ciszy się nie zgadza")
        return parts
    }

    /** Limit Cloud TTS Chirp 3 HD: 200 zapytań/min na projekt; kontroler na starcie potrafi wysłać ~7/s → równe tempo. */
    private val pace = kotlinx.coroutines.sync.Mutex()
    @Volatile private var lastCallAt = 0L

    private suspend fun request(key: String, voice: String, text: String, speed: Float, asMarkup: Boolean = false): ByteArray = pace.withLock {
        val wait = lastCallAt + MIN_GAP_MS - System.currentTimeMillis()
        if (wait > 0) kotlinx.coroutines.delay(wait)
        try { send(key, voice, text, speed, asMarkup) } finally { lastCallAt = System.currentTimeMillis() }
    }

    private suspend fun send(key: String, voice: String, text: String, speed: Float, asMarkup: Boolean): ByteArray = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("input", JSONObject().put(if (asMarkup) "markup" else "text", text))
            .put("voice", JSONObject().put("languageCode", "pl-PL").put("name", "pl-PL-Chirp3-HD-$voice"))
            .put("audioConfig", JSONObject().put("audioEncoding", "LINEAR16").put("sampleRateHertz", 24_000).put("speakingRate", speed.toDouble()))
        val c = URL("https://texttospeech.googleapis.com/v1/text:synthesize").openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"; c.connectTimeout = 10_000; c.readTimeout = if (asMarkup) 40_000 else 15_000; c.doOutput = true // fork 19.11: duże sceny (do ~4700 B) generują się dłużej
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            c.setRequestProperty("x-goog-api-key", key)
            c.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = c.responseCode
            if (status !in 200..299) {
                // Bez treści kwestii: z odpowiedzi tylko status Google (np. RESOURCE_EXHAUSTED, PERMISSION_DENIED).
                val body = runCatching { c.errorStream?.bufferedReader()?.use { it.readText().take(4_000) }.orEmpty() }
                    .onFailure { Log.w(TAG, "Chirp TTS: odczyt błędu: ${it.javaClass.simpleName}") }.getOrDefault("")
                val st = Regex("\"status\"\\s*:\\s*\"([A-Z_]{1,40})\"").find(body)?.groupValues?.get(1)
                throw IllegalStateException("TTS HTTP $status" + (st?.let { " ($it)" } ?: ""))
            }
            val json = c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            Base64.decode(JSONObject(json).getString("audioContent"), Base64.DEFAULT)
        } finally { c.disconnect() }
    }

    private fun countChars(n: Int) {
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val month = YearMonth.now().toString()
        val used = (if (p.getString("month", "") == month) p.getLong("chars", 0) else 0L) + n
        p.edit().putString("month", month).putLong("chars", used).apply()
        if (used > FREE_CHARS && used - n <= FREE_CHARS) Log.w(TAG, "Chirp: przekroczono darmowy 1 mln znaków w $month — dalej płatne (30 USD / 1 mln)")
        if (used % 50_000 < n) Log.i(TAG, "Chirp: w $month zużyto $used znaków")
    }

    fun clearCache() = cache.clear()

    companion object {
        private const val TAG = "ForkLektor"
        private const val PREFS = "fork_lektor_chirp"
        private const val FREE_CHARS = 1_000_000L
        private const val MIN_GAP_MS = 350L // ≈170 zapytań/min, poniżej limitu 200/min
        /** Lekko szybciej niż domyślnie — kwestie lektora muszą mieścić się w czasie napisu. */
        private const val BASE_RATE = 1.1f
        const val K_CHIRP_VOICE = "lektor_chirp_voice"
        const val DEFAULT_VOICE = "Enceladus" // wybór właściciela 19.11 (odsłuch 30 próbek)
        /** Polskie głosy Chirp 3 HD (lista z API 19.11). */
        val CHIRP_VOICES = listOf(
            "Enceladus", "Charon", "Orus", "Algenib", "Algieba", "Achird", "Alnilam", "Fenrir", "Iapetus", "Puck",
            "Rasalgethi", "Sadachbia", "Sadaltager", "Schedar", "Umbriel", "Zubenelgenubi",
            "Kore", "Aoede", "Achernar", "Autonoe", "Callirrhoe", "Despina", "Erinome", "Gacrux", "Laomedeia", "Leda",
            "Pulcherrima", "Sulafat", "Vindemiatrix", "Zephyr",
        )

        fun apiKey(): String = com.nuvio.tv.BuildConfig.GOOGLE_TTS_API_KEY
        fun keyPresent() = apiKey().isNotBlank()
        fun voice(c: Context): String = ForkFeatures.str(c, K_CHIRP_VOICE, DEFAULT_VOICE).takeIf { it in CHIRP_VOICES } ?: DEFAULT_VOICE
    }
}
