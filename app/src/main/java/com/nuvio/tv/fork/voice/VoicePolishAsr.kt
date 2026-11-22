package com.nuvio.tv.fork.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.nuvio.tv.ui.screens.player.audiosync.SileroVad
import com.nuvio.tv.ui.screens.player.audiosync.SileroVadWeights
import java.io.File

/**
 * Sterowanie głosem (22.11, właściciel: „wymuszony polski, initial_prompt z komendami, VAD przed Whisperem”).
 *
 * Sprawdzone: głos używał wspólnego rozpoznawacza AutoSync napisów (`SherpaSpeechToText`, `language = ""` = wykrywanie
 * języka) i prostego progu głośności. Teraz:
 *  - własny rozpoznawacz Whisper z `language = "pl"` (bez wykrywania; ten sam pobrany model tiny/base/small);
 *  - Silero VAD (wagi z aplikacji) przed Whisperem: bez mowy nic nie trafia do Whispera (brak zmyślonych tekstów),
 *    nagranie przycięte do mowy (szybciej), koniec po 0,8 s ciszy;
 *  - `initial_prompt`: sherpa-onnx 1.13.8 nie ma tego pola dla Whispera → zamiast tego [VoiceVocabulary] po rozpoznaniu.
 * Rozpoznawacz zwalniany 2 min po ostatnim użyciu.
 */
internal object VoicePolishAsr {
    private const val TAG = "ForkVoice"
    private const val RATE = 16_000
    private const val KEEP_MS = 120_000L
    private var recognizer: OfflineRecognizer? = null
    private var dir: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val closer = Runnable { synchronized(this) { recognizer?.release(); recognizer = null; dir = null; Log.i(TAG, "whisper pl: zwolniony") } }
    @Volatile private var weights: SileroVadWeights? = null

    /** Nowy detektor mowy (osobny stan na nagranie); null = wag nie da się wczytać (wtedy próg głośności). */
    fun vad(c: Context): SileroVad? = try {
        val w = weights ?: c.resources.openRawResource(com.nuvio.tv.R.raw.silero_vad_v5_16k).use { SileroVadWeights.read(it) }.also { weights = it }
        SileroVad(w)
    } catch (e: Exception) {
        Log.w(TAG, "VAD: ${e.javaClass.simpleName}: ${e.message} — próg głośności"); null
    }

    @Synchronized
    fun transcribe(modelDir: File, samples: FloatArray): String {
        handler.removeCallbacks(closer)
        val r = recognizer?.takeIf { dir == modelDir.absolutePath } ?: create(modelDir).also { recognizer?.release(); recognizer = it; dir = modelDir.absolutePath }
        try {
            val stream = r.createStream()
            try {
                stream.acceptWaveform(samples, RATE)
                r.decode(stream)
                return r.getResult(stream).text.trim()
            } finally { stream.release() }
        } finally {
            handler.postDelayed(closer, KEEP_MS)
        }
    }

    private fun create(modelDir: File): OfflineRecognizer {
        val size = listOf("tiny", "base", "small").firstOrNull { File(modelDir, "$it-encoder.int8.onnx").isFile }
            ?: throw IllegalStateException("brak modelu Whisper w ${modelDir.name}")
        Log.i(TAG, "whisper pl: wczytuję model $size (język wymuszony: pl)")
        return OfflineRecognizer(
            config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = RATE, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    whisper = OfflineWhisperModelConfig(
                        encoder = File(modelDir, "$size-encoder.int8.onnx").absolutePath,
                        decoder = File(modelDir, "$size-decoder.int8.onnx").absolutePath,
                        language = "pl",
                        task = "transcribe",
                    ),
                    tokens = File(modelDir, "$size-tokens.txt").absolutePath,
                    numThreads = 2,
                    modelType = "whisper",
                    provider = "cpu",
                ),
                decodingMethod = "greedy_search",
            ),
        )
    }
}

/**
 * Słownik komend i nazw (zamiast `initial_prompt`): słowo rozpoznane z drobnym błędem (1 litera; 2 przy dłuższych)
 * zamieniane na słowo ze słownika, np. „netflik” → „Netflix”, „ciszy” → „ciszej”. Krótkie słowa (< 4 litery) bez zmian.
 */
object VoiceVocabulary {
    val words = listOf(
        "włącz", "wyłącz", "ciszej", "głośniej", "wycisz", "pauza", "zatrzymaj", "wznów", "dalej", "wstecz", "przewiń", "cofnij",
        "następny", "poprzedni", "odcinek", "sezon", "napisy", "lektor", "kanał", "program", "film", "serial", "szukaj", "otwórz",
        "Netflix", "YouTube", "Disney", "Prime", "Max", "Polsat", "TVN", "TVP", "Canal", "Eurosport", "telewizja", "minut", "sekund",
    )
    private val byFold = words.associateBy { fold(it) }

    private fun fold(s: String) = java.text.Normalizer.normalize(s.lowercase().replace('ł', 'l'), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    fun snap(text: String): String {
        val out = text.split(' ').map { raw ->
            val core = raw.trim { !it.isLetterOrDigit() }
            if (core.length < 4) return@map raw
            val f = fold(core)
            byFold[f]?.let { return@map raw.replace(core, it) }
            val limit = if (f.length >= 7) 2 else 1
            val best = byFold.entries.filter { it.key.length >= 5 && it.key.first() == f.first() && kotlin.math.abs(it.key.length - f.length) <= limit }
                .map { it to distance(f, it.key) }.filter { it.second <= limit }.minByOrNull { it.second }?.first?.value
            if (best != null) raw.replace(core, best) else raw
        }.joinToString(" ")
        if (out != text) Log.i("ForkVoice", "słownik komend: poprawione słowa (${text.length} → ${out.length} znaków)")
        return out
    }

    fun distance(a: String, b: String): Int {
        val d = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = d[0]; d[0] = i
            for (j in 1..b.length) {
                val tmp = d[j]
                d[j] = minOf(d[j] + 1, d[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = tmp
            }
        }
        return d[b.length]
    }
}
