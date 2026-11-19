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
    /** Fork 19.11 (JMGO: rozjazd i powtórki): szacunek najwyżej 1,5 s za ostatnią pozycją — przy buforowaniu zegar nie ucieka do przodu. */
    fun positionMs(): Long = if (playing) anchorPosMs + ((System.nanoTime() - anchorNanos) / 1_000_000).coerceAtMost(1_500) else anchorPosMs
    /** Wiek ostatniej pozycji z interfejsu (ms). */
    fun anchorAgeMs(): Long = (System.nanoTime() - anchorNanos) / 1_000_000

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

/**
 * Procesor dźwięku: oryginał × obwiednia ściszania + głos lektora (mono do wszystkich kanałów). Nieaktywny, gdy lektor wyłączony.
 *
 * Fork 19.11 (JMGO build123, właściciel: „rozjeżdża się, powtarza kwestie, ucina w połowie słowa, ucina 1–3 słowa”):
 * - kwestia gra od początku do końca własnym licznikiem próbek (skok zegara nie powtarza ani nie przeskakuje jej fragmentu);
 * - każda kwestia gra raz (lista zagranych czyszczona tylko przy przewinięciu);
 * - następna kwestia czeka na koniec bieżącej do [MAX_WAIT_MS]; dopiero dłużej → bieżąca ściszana płynnie (60 ms), nie ucinana;
 * - napis bez gotowego głosu nie ucisza bieżącej kwestii (wcześniej po cichu ją przerywał);
 * - zegar poprawiany z interfejsu tylko świeżą pozycją i tylko przy dużym rozjeździe.
 */
class LektorAudioProcessor : BaseAudioProcessor() {
    private companion object {
        const val MAX_WAIT_MS = 1_200L
        const val MAX_LATE_MS = 2_500L
        const val FADE_MS = 60
        const val TAG = "ForkLektor"
    }
    private var frames = 0L
    private var baseMs = 0L
    private var mixLogged = false
    private val played = HashSet<Long>()
    private var curKey = -1L; private var curPcm: Pcm? = null; private var curPos = 0.0
    private var fadePcm: Pcm? = null; private var fadePos = 0.0; private var fadeLeft = 0; private var fadeTotal = 1
    private var lastMissing = -1L
    private var waits = 0; private var cuts = 0; private var skipped = 0; private var waitKey = -1L; private var statsLogged = 0

    override fun isActive(): Boolean = super.isActive() && LektorEngine.enabled

    // Fork 19.11 (właściciel: „lektor nie działa”): także PCM float (FFmpeg / wyjście float) — wcześniej mikser po cichu się wyłączał.
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val ok = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT || inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        android.util.Log.i(TAG, "mikser: format=${inputAudioFormat.encoding} kanały=${inputAudioFormat.channelCount} ${inputAudioFormat.sampleRate} Hz → ${if (ok) "aktywny" else "pominięty"}")
        return if (ok) inputAudioFormat else AudioProcessor.AudioFormat.NOT_SET
    }

    override fun onFlush() {
        frames = 0; baseMs = LektorEngine.positionMs()
        played.clear(); curPcm = null; curKey = -1; fadePcm = null; fadeLeft = 0
    }

    private fun stopCurrent(fade: Boolean) {
        val p = curPcm
        if (fade && p != null) { fadePcm = p; fadePos = curPos; fadeTotal = (inputAudioFormat.sampleRate * FADE_MS / 1000).coerceAtLeast(1); fadeLeft = fadeTotal }
        curPcm = null; curKey = -1
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val out = replaceOutputBuffer(inputBuffer.remaining()).order(ByteOrder.nativeOrder())
        val ch = inputAudioFormat.channelCount.coerceAtLeast(1); val rate = inputAudioFormat.sampleRate
        val inp = inputBuffer.order(ByteOrder.nativeOrder())
        if (!LektorEngine.enabled || LektorEngine.lines.isEmpty()) { out.put(inp); out.flip(); return }
        // Poprawka z zegara interfejsu: tylko świeża pozycja (≤1,2 s) i rozjazd > 2 s (przewinięcie bez flush). Procesor
        // przetwarza dźwięk przed odtworzeniem, więc bywa o bufor przed pozycją interfejsu — to nie jest rozjazd.
        val clock = baseMs + frames * 1000 / rate
        if (LektorEngine.anchorAgeMs() < 1_200) {
            val ui = LektorEngine.positionMs(); val d = clock - ui
            if (d < -2_000 || d > 3_000) { android.util.Log.i(TAG, "mikser: zegar poprawiony o ${ui - clock} ms"); baseMs = ui; frames = 0 }
        }
        val t0 = baseMs
        val float = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val bytes = if (float) 4 else 2
        if (!mixLogged) { mixLogged = true; android.util.Log.i(TAG, "mikser: miksuję (float=$float, kwestii z głosem=${LektorEngine.voice.size})") }
        val n = inp.remaining() / (bytes * ch)
        val startMs = t0 + frames * 1000 / rate
        val durMs = n * 1000L / rate
        val near = LektorEngine.near(startMs - MAX_LATE_MS, startMs + durMs + 1_000)
        val g0 = LektorEngine.gainAt(startMs, near); val g1 = LektorEngine.gainAt(startMs + durMs, near)
        val vGain = LektorEngine.voiceGain
        val nearPcm = arrayOfNulls<Pcm>(near.size).also { a -> near.forEachIndexed { k, l -> a[k] = LektorEngine.voice[l.startMs] } } // raz na bufor
        if (played.size > 2_000) played.retainAll { it >= startMs - 60_000 }
        var i = 0
        while (inp.remaining() >= bytes * ch) {
            val tMs = t0 + frames * 1000 / rate
            val g = if (n > 1) g0 + (g1 - g0) * i / (n - 1) else g0
            // Bieżąca kwestia skończona?
            curPcm?.let { p -> if (curPos >= p.samples.size) { curPcm = null; curKey = -1 } }
            // Najnowsza kwestia, której czas już nadszedł, z gotowym głosem, jeszcze nie grana.
            var due = -1
            for (k in near.indices.reversed()) {
                val l = near[k]
                if (l.startMs > tMs || l.startMs in played) continue
                if (nearPcm[k] == null) {
                    if (tMs - l.startMs < 300 && lastMissing != l.startMs) { lastMissing = l.startMs; android.util.Log.i(TAG, "mikser: kwestia ${l.startMs} bez gotowego głosu w chwili napisu") }
                    continue
                }
                if (tMs - l.startMs <= MAX_LATE_MS) { due = k; break }
            }
            if (due >= 0) {
                val l = near[due]; val p = curPcm
                val start = if (p == null) true else {
                    val leftMs = ((p.samples.size - curPos) * 1000 / p.sampleRate).toLong()
                    val lateMs = tMs - l.startMs
                    if (lateMs + leftMs <= MAX_WAIT_MS) { if (waitKey != l.startMs) { waitKey = l.startMs; waits++ }; false } else {
                        cuts++
                        android.util.Log.i(TAG, "mikser: kwestia $curKey ściszona przy ${l.startMs} po ${(curPos * 100 / p.samples.size).toInt()}% (zostało $leftMs ms, czekała $lateMs ms)")
                        stopCurrent(fade = true); true
                    }
                }
                if (start) {
                    // Starsze, niezagrane kwestie przed tą już nie zagrają (były by nie na czasie).
                    for (k in 0 until due) { val o = near[k].startMs; if (o !in played && nearPcm[k] != null && o != curKey) { played += o; skipped++ } }
                    played += l.startMs; curKey = l.startMs; curPcm = nearPcm[due]; curPos = 0.0
                    if (tMs - l.startMs > 300) android.util.Log.i(TAG, "mikser: kwestia ${l.startMs} zaczęta ${tMs - l.startMs} ms po napisie")
                }
            }
            var v = 0f
            curPcm?.let { p -> val idx = curPos.toInt(); if (idx < p.samples.size) v += p.samples[idx] / 32768f; curPos += p.sampleRate.toDouble() / rate }
            fadePcm?.let { p ->
                val idx = fadePos.toInt()
                if (fadeLeft <= 0 || idx >= p.samples.size) fadePcm = null
                else { v += p.samples[idx] / 32768f * fadeLeft / fadeTotal; fadePos += p.sampleRate.toDouble() / rate; fadeLeft-- }
            }
            v *= vGain
            i++
            for (c in 0 until ch) {
                if (float) out.putFloat((inp.float * g + v).coerceIn(-1f, 1f))
                else out.putShort(((inp.short / 32768f * g + v).coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            }
            frames++
        }
        val sum = waits + cuts + skipped
        if (sum >= statsLogged + 25) { statsLogged = sum; android.util.Log.i(TAG, "mikser: kwestie czekały na poprzednią=$waits, ściszone przed końcem=$cuts, pominięte=$skipped") }
        out.flip()
    }
}
