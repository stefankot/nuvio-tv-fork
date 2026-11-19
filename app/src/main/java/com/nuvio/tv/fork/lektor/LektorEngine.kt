package com.nuvio.tv.fork.lektor

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap

/**
 * Lektor w odtwarzaczu (B09-4/5): kwestie z napisów, głos z pamięci, ściszanie oryginału i domiks.
 * Pozycja filmu przychodzi z interfejsu (co ok. sekundę), między aktualizacjami jest szacowana zegarem.
 */
object LektorEngine {
    @Volatile var enabled = false
    @Volatile var lines: List<Line> = emptyList()
    val voice = ConcurrentHashMap<Long, Pcm>()
    @Volatile var voiceGain = 1f
    @Volatile private var envelope = DuckEnvelope()
    fun setDuckDepth(db: Float) { envelope = DuckEnvelope(depthDb = db) }

    @Volatile private var anchorPosMs = 0L
    @Volatile private var anchorNanos = 0L
    @Volatile private var playing = false

    fun updatePosition(posMs: Long, isPlaying: Boolean) { anchorPosMs = posMs; anchorNanos = System.nanoTime(); playing = isPlaying }
    fun positionMs(): Long = if (playing) anchorPosMs + (System.nanoTime() - anchorNanos) / 1_000_000 else anchorPosMs

    fun load(cues: List<SubtitleCue>) { lines = CuePlanner.plan(cues); voice.clear() }
    fun clear() { enabled = false; lines = emptyList(); voice.clear() }

    fun gainAt(tMs: Long): Float = envelope.gain(tMs, lines)

    /**
     * Fork 19.11 (build117 JMGO: „nic nie słychać, aplikacja się zacina, pierdzi”): mikser przeszukiwał wszystkie kwestie
     * dla KAŻDEJ próbki (48 000/s × kanały) — wątek dźwięku nie nadążał. Teraz raz na bufor: kwestie z okna (wyszukiwanie
     * binarne po starcie, lista posortowana przez CuePlanner) i obwiednia na początku/końcu bufora.
     */
    fun near(fromMs: Long, toMs: Long): List<Line> {
        val ls = lines
        var lo = 0; var hi = ls.size
        while (lo < hi) { val m = (lo + hi) ushr 1; if (ls[m].startMs < fromMs) lo = m + 1 else hi = m }
        val out = ArrayList<Line>(4)
        var i = (lo - 3).coerceAtLeast(0) // kwestie zaczęte wcześniej, ale jeszcze trwające (z ogonem głosu)
        while (i < ls.size && ls[i].startMs <= toMs) { if (ls[i].endMs + 4_000 >= fromMs) out += ls[i]; i++ }
        return out
    }
    fun gainAt(tMs: Long, near: List<Line>): Float = envelope.gain(tMs, near)

    /** Próbka głosu (−1..1) w chwili [tMs]: w kwestii, której głos jest gotowy; inaczej 0 (kwestia niegotowa jest pomijana, napis zostaje). */
    fun voiceAt(tMs: Long): Float {
        val l = lines.firstOrNull { tMs >= it.startMs && tMs <= it.endMs + 4_000 } ?: return 0f
        val pcm = voice[l.startMs] ?: return 0f
        val idx = ((tMs - l.startMs) * pcm.sampleRate / 1000).toInt()
        return if (idx in pcm.samples.indices) pcm.samples[idx] / 32768f else 0f
    }
}

/** Procesor dźwięku: oryginał × obwiednia ściszania + głos lektora (mono do wszystkich kanałów). Nieaktywny, gdy lektor wyłączony. */
class LektorAudioProcessor : BaseAudioProcessor() {
    private var frames = 0L
    private var baseMs = 0L
    private var mixLogged = false

    override fun isActive(): Boolean = super.isActive() && LektorEngine.enabled

    // Fork 19.11 (właściciel: „lektor nie działa”): także PCM float (FFmpeg / wyjście float) — wcześniej mikser po cichu się wyłączał.
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val ok = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT || inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        android.util.Log.i("ForkLektor", "mikser: format=${inputAudioFormat.encoding} kanały=${inputAudioFormat.channelCount} ${inputAudioFormat.sampleRate} Hz → ${if (ok) "aktywny" else "pominięty"}")
        return if (ok) inputAudioFormat else AudioProcessor.AudioFormat.NOT_SET
    }

    override fun onFlush() { frames = 0; baseMs = LektorEngine.positionMs() }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val out = replaceOutputBuffer(inputBuffer.remaining()).order(ByteOrder.nativeOrder())
        val ch = inputAudioFormat.channelCount.coerceAtLeast(1); val rate = inputAudioFormat.sampleRate
        val inp = inputBuffer.order(ByteOrder.nativeOrder())
        if (!LektorEngine.enabled || LektorEngine.lines.isEmpty()) { out.put(inp); out.flip(); return }
        var t0 = baseMs
        // Poprawka z zegara interfejsu przy dużym rozjeździe (przewinięcie).
        val ui = LektorEngine.positionMs(); if (kotlin.math.abs(ui - (baseMs + frames * 1000 / rate)) > 1500) { baseMs = ui; frames = 0; t0 = ui }
        val float = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val bytes = if (float) 4 else 2
        if (!mixLogged) { mixLogged = true; android.util.Log.i("ForkLektor", "mikser: miksuję (float=$float, kwestii z głosem=${LektorEngine.voice.size})") }
        val n = inp.remaining() / (bytes * ch)
        val startUs = t0 * 1000 + frames * 1_000_000L / rate
        val durMs = n * 1000L / rate
        val near = LektorEngine.near(startUs / 1000 - 1_000, startUs / 1000 + durMs + 1_000)
        val g0 = LektorEngine.gainAt(startUs / 1000, near); val g1 = LektorEngine.gainAt(startUs / 1000 + durMs, near)
        val vGain = LektorEngine.voiceGain
        val nearPcm = arrayOfNulls<Pcm>(near.size).also { a -> near.forEachIndexed { k, l -> a[k] = LektorEngine.voice[l.startMs] } } // raz na bufor (bez obiektów na próbkę)
        var i = 0
        while (inp.remaining() >= bytes * ch) {
            // Czas w mikrosekundach (wcześniej milisekundy: ta sama próbka głosu ~48 razy, potem skok = trzeszczenie).
            val tUs = t0 * 1000 + frames * 1_000_000L / rate
            val tMs = tUs / 1000
            val g = if (n > 1) g0 + (g1 - g0) * i / (n - 1) else g0
            var v = 0f
            for (k in near.indices.reversed()) { // fork 19.11: przy nakładaniu wygrywa nowsza kwestia (nie ucina jej początku)
                val l = near[k]
                if (tMs >= l.startMs && tMs <= l.endMs + 4_000) {
                    val pcm = nearPcm[k]
                    if (pcm != null) { val idx = ((tUs - l.startMs * 1000) * pcm.sampleRate / 1_000_000L).toInt(); if (idx in pcm.samples.indices) v = pcm.samples[idx] / 32768f * vGain }
                    break
                }
            }
            i++
            for (c in 0 until ch) {
                if (float) out.putFloat((inp.float * g + v).coerceIn(-1f, 1f))
                else out.putShort(((inp.short / 32768f * g + v).coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            }
            frames++
        }
        out.flip()
    }
}
