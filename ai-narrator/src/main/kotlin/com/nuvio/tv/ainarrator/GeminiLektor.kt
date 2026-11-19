package com.nuvio.tv.ainarrator

import android.content.Context
import com.nuvio.tv.fork.lektor.Lektor
import com.nuvio.tv.fork.lektor.Pcm
import com.nuvio.tv.fork.registry.ModuleAction
import com.nuvio.tv.fork.registry.SettingsLevel
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.YearMonth
import java.util.Locale
import kotlinx.coroutines.sync.withLock

/**
 * Scalenie (19.11, właściciel): głos Gemini dla jednego lektora forka (`fork/lektor`).
 * Silnik lektora (ściszanie oryginału, tempo, tryb auto) zostaje stary; tu tylko synteza kwestii.
 * Kwestie w pamięci podręcznej (ta sama kwestia = bez ponownego kosztu), szacowany limit miesięczny.
 */
class GeminiLektor(context: Context) : Lektor, com.nuvio.tv.fork.lektor.SceneLektor {
    private val app = context.applicationContext
    private val prefs = NarratorPreferences(app)
    private val tts = GeminiNarratorTts()
    private val cache = NarratorAudioCache(File(app.cacheDir, "ai-narrator"))
    /** Fork 19.11 (build116: flash-lite stale 429, flash OK): po udanym przełączeniu zostajemy na działającym modelu w tej sesji. */
    @Volatile private var sticky: NarratorModel? = null
    /** Fork 19.11 (build118: seryjne 429 na obu modelach): równe tempo zapytań — min. [MIN_GAP_MS] między wywołaniami. */
    private val pace = kotlinx.coroutines.sync.Mutex()
    @Volatile private var lastCallAt = 0L
    private suspend fun <T> paced(block: suspend () -> T): T = pace.withLock {
        val wait = lastCallAt + MIN_GAP_MS - System.currentTimeMillis()
        if (wait > 0) kotlinx.coroutines.delay(wait)
        try { block() } finally { lastCallAt = System.currentTimeMillis() }
    }

    override fun voices(): List<String> = LektorVoiceChoice.GEMINI_VOICES

    override suspend fun synthesize(text: String, rate: Float): Pcm {
        val key = narratorGeminiKey(app, prefs)
        if (key.isBlank()) throw Lektor.Unavailable("Brak klucza Gemini (Ustawienia → AI napisy)")
        val options = prefs.load()
        val clean = NarratorText.clean(text)
        if (clean.isBlank()) throw Lektor.Unavailable("Pusta kwestia")
        val style = NarratorText.style(clean, null) + tempoHint(rate)
        return pcmFromWav(wavFor(key, options, clean, style, (clean.length / 9.0).coerceIn(1.5, 25.0)))
    }

    /**
     * Fork 19.11 (właściciel: „TTS w partiach z podziałem na sceny i z dużym wyprzedzeniem”): cała scena jednym zapytaniem,
     * kwestie w osobnych akapitach z wyraźną pauzą; nagranie cięte po ciszy ([SilenceSplit]). null = cięcie się nie zgadza.
     */
    override suspend fun synthesizeScene(texts: List<String>): List<Pcm>? {
        if (texts.size < 2) return null
        val key = narratorGeminiKey(app, prefs)
        if (key.isBlank()) throw Lektor.Unavailable("Brak klucza Gemini (Ustawienia → AI napisy)")
        val options = prefs.load()
        val cleaned = texts.map { NarratorText.clean(it) }
        if (cleaned.any { it.isBlank() }) return null
        val joined = cleaned.joinToString("\n\n")
        if (joined.length > 1_200) return null
        val style = NarratorText.style("", null) + SCENE_NOTE
        val pcm = pcmFromWav(wavFor(key, options, joined, style, (joined.length / 9.0 + cleaned.size * 0.8).coerceIn(3.0, 120.0)))
        val parts = com.nuvio.tv.fork.lektor.SilenceSplit.split(pcm, cleaned.size)
        if (parts == null) android.util.Log.i("ForkLektor", "scena ${cleaned.size} kwestii (${pcm.durationMs} ms): cięcie po ciszy się nie zgadza")
        return parts
    }

    /** Nagranie z pamięci albo z Gemini (rezerwacja kosztu, równe tempo, zapasowy model przy 503/429, zapis modelu, który mówi). */
    private suspend fun wavFor(key: String, options: NarratorOptions, clean: String, style: String, seconds: Double): ByteArray {
        // Fork 19.11 (właściciel: „za każdym razem lektor ma inny głos”): klucz pamięci = model, który naprawdę mówi.
        val current = sticky ?: options.model
        val id = NarratorText.fingerprint(current, options.voice, clean, style)
        cache.find(id, touch = true)?.readBytes()?.let { return it }
        var storeId = id
        val month = YearMonth.now()
        val usd = NarratorPrices.estimatedUsd(current, clean.length, seconds * 1.8, month.year, month.monthValue)
        if (!prefs.reserve(usd, options.monthlyUsdLimit)) throw Lektor.Unavailable("Lektor Gemini: osiągnięto miesięczny limit kosztów")
        // Fork 19.11 (właściciel: „Błąd Gemini: 503, następnym razem timeout”): przy przeciążeniu (503/429) jedna
        // szybka próba na drugim modelu TTS (inna pula serwerów), potem błąd do kontrolera (on robi przerwy).
        val model = current
        val bytes = try { paced { tts.synthesize(key, model, options.voice, clean, style) } }
            catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val busy = e.message?.let { it.contains("503") || it.contains("429") || it.contains("500") } == true
                android.util.Log.w("ForkLektor", "Gemini TTS ${model.apiId}: ${e.javaClass.simpleName}: ${e.message?.take(120)}")
                if (!busy) throw Lektor.Unavailable("Błąd Gemini: ${e.message ?: "połączenie"}")
                val other = if (model == NarratorModel.FLASH) NarratorModel.FLASH_LITE else NarratorModel.FLASH
                kotlinx.coroutines.delay(800)
                try { paced { tts.synthesize(key, other, options.voice, clean, style) }.also {
                    storeId = NarratorText.fingerprint(other, options.voice, clean, style)
                    if (sticky != other) {
                        sticky = other
                        // Na stałe: dwa modele = dwa różne głosy; zostaje ten, który działa.
                        runCatching { prefs.save(prefs.load().copy(model = other)) }.onFailure { e3 -> android.util.Log.w("ForkLektor", "Gemini TTS: zapis modelu: ${e3.javaClass.simpleName}") }
                        android.util.Log.i("ForkLektor", "Gemini TTS: przełączam na ${other.apiId} na stałe (${model.apiId}: ${e.message?.take(20)})")
                    }
                } }
                catch (e2: Exception) {
                    if (e2 is kotlinx.coroutines.CancellationException) throw e2
                    android.util.Log.w("ForkLektor", "Gemini TTS zapasowy ${other.apiId}: ${e2.javaClass.simpleName}: ${e2.message?.take(120)}")
                    throw Lektor.Unavailable("Serwer Gemini przeciążony (${e.message?.take(20)})")
                }
            }
        cache.store(storeId, bytes)
        return bytes
    }

    private fun tempoHint(rate: Float): String =
        if (rate > 1.05f) " Mów szybciej niż zwykle, o około ${((rate - 1f) * 100).toInt()}%, nadal wyraźnie." else ""

    fun clearCache() = cache.clear()

    companion object {
        private const val MIN_GAP_MS = 2_000L
        private const val SCENE_NOTE = " To kilka kolejnych kwestii z jednej sceny, każda w osobnym akapicie. Czytaj je po kolei tym samym głosem " +
            "i w tym samym tempie, a między akapitami rób wyraźną pauzę, około jednej sekundy. Nie czytaj numerów ani pustych linii."

        /** WAV (RIFF, PCM 16-bit) → próbki mono dla miksera lektora. */
        fun pcmFromWav(b: ByteArray): Pcm {
            val bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
            var rate = 24_000; var channels = 1; var pos = 12
            var data: ByteArray? = null
            while (pos + 8 <= b.size) {
                val id = String(b, pos, 4, Charsets.US_ASCII); val len = bb.getInt(pos + 4).coerceAtLeast(0)
                val body = pos + 8
                if (id == "fmt " && body + 16 <= b.size) { channels = bb.getShort(body + 2).toInt().coerceAtLeast(1); rate = bb.getInt(body + 4) }
                if (id == "data") { data = b.copyOfRange(body, minOf(b.size, body + len)); break }
                pos = body + len + (len and 1)
            }
            val raw = data ?: ByteArray(0)
            val all = ShortArray(raw.size / 2); ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(all)
            val mono = if (channels == 1) all else ShortArray(all.size / channels) { i -> all[i * channels] }
            return Pcm(mono, rate)
        }
    }
}

/** Wybór głosu lektora i ustawienia Gemini (w Modułach, sekcja Lektor). */
object LektorVoiceChoice {
    const val K_VOICE = "lektor_voice_backend" // "gemini" | "system"
    val GEMINI_VOICES = listOf("Kore", "Puck", "Charon", "Aoede")

    fun geminiKeyPresent(c: Context) = narratorGeminiKey(c.applicationContext, NarratorPreferences(c.applicationContext)).isNotBlank()

    /** Gemini, chyba że wybrano systemowy; bez klucza Gemini — systemowy; bez systemowego głosu — Gemini. */
    fun useGemini(c: Context, systemVoiceOk: Boolean?): Boolean {
        val wantGemini = ForkFeatures.str(c, K_VOICE, "gemini") != "system"
        val key = geminiKeyPresent(c)
        return when {
            wantGemini && key -> true
            !wantGemini && systemVoiceOk == false && key -> true
            else -> false
        }
    }

    fun create(c: Context, systemVoiceOk: Boolean?): Lektor =
        if (useGemini(c, systemVoiceOk)) GeminiLektor(c) else com.nuvio.tv.fork.lektor.SystemLektor(c)

    fun label(c: Context, systemVoiceOk: Boolean?): String = if (useGemini(c, systemVoiceOk)) "głos Gemini" else "głos systemowy"

    fun actions(): List<ModuleAction> = listOf(
        ModuleAction("Lektor: głos (Gemini / systemowy)", SettingsLevel.ESSENTIAL, "Gemini: naturalny głos, płatny wg zużycia (klucz z AI napisów); systemowy: darmowy, jeśli jest polski głos") { c ->
            ForkFeatures.setStr(c, K_VOICE, if (ForkFeatures.str(c, K_VOICE, "gemini") == "system") "gemini" else "system")
        },
        ModuleAction("Lektor Gemini: barwa głosu (Kore / Puck / Charon / Aoede)", SettingsLevel.ESSENTIAL, "Kolejne naciśnięcia przełączają głos") { c ->
            val p = NarratorPreferences(c); val o = p.load()
            p.save(o.copy(voice = GEMINI_VOICES[(GEMINI_VOICES.indexOf(o.voice) + 1).mod(GEMINI_VOICES.size)]))
        },
        ModuleAction("Lektor Gemini: model (Flash-Lite tańszy / Flash)", SettingsLevel.ADVANCED, "Kolejne naciśnięcia przełączają model") { c ->
            val p = NarratorPreferences(c); val o = p.load()
            p.save(o.copy(model = if (o.model == NarratorModel.FLASH) NarratorModel.FLASH_LITE else NarratorModel.FLASH))
        },
        ModuleAction("Lektor Gemini: limit miesięczny (2 / 5 / 10 / 20 USD)", SettingsLevel.ESSENTIAL, "Szacunek lokalny; po przekroczeniu lektor milknie do następnego miesiąca") { c ->
            val p = NarratorPreferences(c); val o = p.load()
            p.save(o.copy(monthlyUsdLimit = when (o.monthlyUsdLimit) { 2.0 -> 5.0; 5.0 -> 10.0; 10.0 -> 20.0; else -> 2.0 }))
            android.widget.Toast.makeText(c, "Limit: ${p.load().monthlyUsdLimit} USD, wydano w tym miesiącu ok. ${"%.3f".format(Locale.ROOT, p.spentThisMonth())} USD", android.widget.Toast.LENGTH_LONG).show()
        },
    )
}
