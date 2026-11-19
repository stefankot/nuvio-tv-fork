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

/**
 * Scalenie (19.11, właściciel): głos Gemini dla jednego lektora forka (`fork/lektor`).
 * Silnik lektora (ściszanie oryginału, tempo, tryb auto) zostaje stary; tu tylko synteza kwestii.
 * Kwestie w pamięci podręcznej (ta sama kwestia = bez ponownego kosztu), szacowany limit miesięczny.
 */
class GeminiLektor(context: Context) : Lektor {
    private val app = context.applicationContext
    private val prefs = NarratorPreferences(app)
    private val tts = GeminiNarratorTts()
    private val cache = NarratorAudioCache(File(app.cacheDir, "ai-narrator"))

    override fun voices(): List<String> = LektorVoiceChoice.GEMINI_VOICES

    override suspend fun synthesize(text: String, rate: Float): Pcm {
        val key = narratorGeminiKey(app, prefs)
        if (key.isBlank()) throw Lektor.Unavailable("Brak klucza Gemini (Ustawienia → AI napisy)")
        val options = prefs.load()
        val clean = NarratorText.clean(text)
        if (clean.isBlank()) throw Lektor.Unavailable("Pusta kwestia")
        val style = NarratorText.style(clean, null) + tempoHint(rate)
        val id = NarratorText.fingerprint(options.model, options.voice, clean, style)
        val wav = cache.find(id, touch = true)?.readBytes() ?: run {
            val month = YearMonth.now()
            val seconds = (clean.length / 9.0).coerceIn(1.5, 25.0)
            val usd = NarratorPrices.estimatedUsd(options.model, clean.length, seconds * 1.8, month.year, month.monthValue)
            if (!prefs.reserve(usd, options.monthlyUsdLimit)) throw Lektor.Unavailable("Lektor Gemini: osiągnięto miesięczny limit kosztów")
            // Fork 19.11 (właściciel: „Błąd Gemini: 503, następnym razem timeout”): przy przeciążeniu (503/429) jedna
            // szybka próba na drugim modelu TTS (inna pula serwerów), potem błąd do kontrolera (on robi przerwy).
            val bytes = try { tts.synthesize(key, options.model, options.voice, clean, style) }
                catch (e: Exception) {
                    val busy = e.message?.let { it.contains("503") || it.contains("429") || it.contains("500") } == true
                    android.util.Log.w("ForkLektor", "Gemini TTS ${options.model.apiId}: ${e.javaClass.simpleName}: ${e.message?.take(120)}")
                    if (!busy) throw Lektor.Unavailable("Błąd Gemini: ${e.message ?: "połączenie"}")
                    val other = if (options.model == NarratorModel.FLASH) NarratorModel.FLASH_LITE else NarratorModel.FLASH
                    kotlinx.coroutines.delay(800)
                    try { tts.synthesize(key, other, options.voice, clean, style).also { android.util.Log.i("ForkLektor", "Gemini TTS: zapasowy model ${other.apiId} OK") } }
                    catch (e2: Exception) {
                        android.util.Log.w("ForkLektor", "Gemini TTS zapasowy ${other.apiId}: ${e2.javaClass.simpleName}: ${e2.message?.take(120)}")
                        throw Lektor.Unavailable("Serwer Gemini przeciążony (${e.message?.take(20)})")
                    }
                }
            cache.store(id, bytes)
            bytes
        }
        return pcmFromWav(wav)
    }

    private fun tempoHint(rate: Float): String =
        if (rate > 1.05f) " Mów szybciej niż zwykle, o około ${((rate - 1f) * 100).toInt()}%, nadal wyraźnie." else ""

    fun clearCache() = cache.clear()

    companion object {
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
