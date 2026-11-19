package com.nuvio.tv.ainarrator

import android.content.Context
import android.util.Base64
import android.util.Log
import com.nuvio.tv.fork.lektor.Lektor
import com.nuvio.tv.fork.lektor.LektorPrefs
import com.nuvio.tv.fork.lektor.LektorSpend
import com.nuvio.tv.fork.lektor.Pcm
import com.nuvio.tv.fork.lektor.StyledLektor
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fork 19.11 (właściciel: „podepnij Gemini, tylko przez Cloud”; strategia `plan/LEKTOR_GEMINI_STRATEGIA.md`):
 * głos lektora z modelu Gemini 3.8 Flash-Lite TTS przez Vertex AI (Google Cloud, płatność za użycie — bez limitu
 * 100 zapytań/dzień z AI Studio). Test 19.11 z Maca: HTTP 200 w 1,9 s, WAV 24 kHz mono, polski działa.
 * Każda kwestia osobno, z poleceniem stylu (scena, kto mówi, emocja) w `speechMetadata.style`.
 * Klucz: `local.properties` GOOGLE_VERTEX_API_KEY → BuildConfig (tryb express, nagłówek x-goog-api-key).
 * Koszt z `usageMetadata`: wejście 0,50 USD / 1 mln tokenów, wyjście (audio, ~25 tokenów/s) 6 USD / 1 mln.
 */
class VertexGeminiLektor(context: Context) : Lektor, StyledLektor {
    private val app = context.applicationContext
    private val cache = NarratorAudioCache(File(app.cacheDir, "lektor-vertex"))
    private val parallel = Semaphore(3)

    override fun voices(): List<String> = ChirpLektor.CHIRP_VOICES

    override suspend fun synthesize(text: String, rate: Float): Pcm =
        synthesizeStyled(text, LektorPrefs.baseStyle(app) + if (rate > 1.05f) " Mów wyraźnie szybciej niż zwykle." else "")

    override suspend fun synthesizeStyled(text: String, style: String): Pcm {
        val key = apiKey()
        if (key.isBlank()) throw Lektor.Unavailable("Brak klucza Google Cloud (Vertex AI) dla lektora Gemini")
        val clean = NarratorText.clean(text)
        if (clean.isBlank()) throw Lektor.Unavailable("Pusta kwestia")
        val voice = voice(app)
        val id = NarratorText.fingerprint("vertex/$MODEL/$voice", clean, style)
        cache.find(id, touch = true)?.readBytes()?.let { return GeminiLektor.pcmFromWav(it) }
        if (!LektorSpend.allowed(app)) throw Lektor.Unavailable("Limit wydatków lektora na ten tytuł")
        val wav = parallel.withPermit {
            try { send(key, voice, clean, style) } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "Gemini Cloud TTS: ${e.javaClass.simpleName}: ${e.message?.take(160)}")
                throw Lektor.Unavailable("Błąd głosu Gemini (Cloud): ${e.message?.take(80) ?: "połączenie"}")
            }
        }
        runCatching { cache.store(id, wav) }.onFailure { Log.w(TAG, "Gemini Cloud: zapis w pamięci podręcznej: ${it.javaClass.simpleName}: ${it.message?.take(120)}") }
        return GeminiLektor.pcmFromWav(wav)
    }

    private suspend fun send(key: String, voice: String, text: String, style: String): ByteArray = withContext(Dispatchers.IO) {
        val part = JSONObject().put("text", text).put("speechMetadata", JSONObject().put("style", style.take(3_500)))
        val payload = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(part))))
            .put("generationConfig", JSONObject()
                .put("responseModalities", JSONArray().put("AUDIO"))
                .put("speechConfig", JSONObject()
                    .put("voiceConfig", JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", voice)))
                    .put("languageCode", "pl-PL")))
        val c = URL("https://aiplatform.googleapis.com/v1/publishers/google/models/$MODEL:generateContent").openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"; c.connectTimeout = 10_000; c.readTimeout = 30_000; c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            c.setRequestProperty("x-goog-api-key", key)
            com.nuvio.tv.fork.lektor.LektorDump.save("glos-gemini", payload.toString()) // fork 20.11: 3 ostatnie surowe zapytania (bez klucza)
            c.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = c.responseCode
            if (status !in 200..299) {
                // Bez treści kwestii: z odpowiedzi tylko status Google (np. RESOURCE_EXHAUSTED, PERMISSION_DENIED).
                val body = runCatching { c.errorStream?.bufferedReader()?.use { it.readText().take(4_000) }.orEmpty() }
                    .onFailure { Log.w(TAG, "Gemini Cloud: odczyt błędu: ${it.javaClass.simpleName}") }.getOrDefault("")
                val st = Regex("\"status\"\\s*:\\s*\"([A-Z_]{1,40})\"").find(body)?.groupValues?.get(1)
                throw IllegalStateException("TTS HTTP $status" + (st?.let { " ($it)" } ?: ""))
            }
            val json = JSONObject(c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            json.optJSONObject("usageMetadata")?.let { u ->
                val cost = u.optLong("promptTokenCount") * 0.50 / 1e6 + u.optLong("candidatesTokenCount") * 6.0 / 1e6
                LektorSpend.add("głos", cost)
            }
            val data = json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                .getJSONObject(0).getJSONObject("inlineData").getString("data")
            Base64.decode(data, Base64.DEFAULT)
        } finally { c.disconnect() }
    }

    fun clearCache() = cache.clear()

    companion object {
        private const val TAG = "ForkLektor"
        const val MODEL = "gemini-3.8-flash-lite-tts" // wybór właściciela 19.11
        const val K_VOICE = "lektor_vertex_voice"
        const val DEFAULT_VOICE = "Charon"
        fun apiKey(): String = com.nuvio.tv.BuildConfig.GOOGLE_VERTEX_API_KEY
        fun keyPresent() = apiKey().isNotBlank()
        fun voice(c: Context): String = ForkFeatures.str(c, K_VOICE, DEFAULT_VOICE).takeIf { it in ChirpLektor.CHIRP_VOICES } ?: DEFAULT_VOICE
    }
}
