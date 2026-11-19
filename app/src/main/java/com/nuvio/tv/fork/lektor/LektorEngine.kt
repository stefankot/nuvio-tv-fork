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
        while (inp.remaining() >= bytes * ch) {
            val t = t0 + frames * 1000 / rate
            val g = LektorEngine.gainAt(t); val v = LektorEngine.voiceAt(t) * LektorEngine.voiceGain
            for (c in 0 until ch) {
                if (float) out.putFloat((inp.float * g + v).coerceIn(-1f, 1f))
                else out.putShort(((inp.short / 32768f * g + v).coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            }
            frames++
        }
        out.flip()
    }
}
