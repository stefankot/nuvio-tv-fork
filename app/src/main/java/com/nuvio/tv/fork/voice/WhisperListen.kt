package com.nuvio.tv.fork.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.nuvio.tv.ui.screens.player.audiosync.asr.AsrModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import com.nuvio.tv.fork.logged

/**
 * Mowa na tekst offline (B07-6, silnik 2): mikrofon 16 kHz → Silero VAD (22.11) → Whisper z sherpa-onnx, język wymuszony „pl” (VoicePolishAsr) (model wybrany w ustawieniach
 * „model mowy”: tiny/base/small). Wymaga pobranego modelu ([AsrModel.isReady]). Nagrywa do 6 s, kończy po ~0,9 s ciszy po mowie.
 */
object WhisperListen {
    private const val RATE = 16_000

    fun available(c: Context) = AsrModel.isReady(c)

    @SuppressLint("MissingPermission")
    suspend fun listen(c: Context): String? = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, RATE))
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return@withContext null }
        val all = ArrayList<Short>(RATE * 6)
        // 22.11: Silero VAD co 32 ms (512 próbek); bez wag — dawny próg głośności.
        val vad = VoicePolishAsr.vad(c)
        val chunk = ShortArray(512)
        val fchunk = FloatArray(512)
        val speechAt = ArrayList<Int>() // indeksy kawałków z mową
        var speech = false; var quietChunks = 0; var k = 0
        try {
            rec.startRecording()
            val limit = RATE * 6
            while (all.size < limit) {
                val n = rec.read(chunk, 0, chunk.size); if (n <= 0) break
                for (i in 0 until n) all += chunk[i]
                val isSpeech = if (vad != null && n == chunk.size) {
                    for (i in 0 until n) fchunk[i] = chunk[i] / 32768f
                    vad.process(fchunk) > 0.5f
                } else { var peak = 0; for (i in 0 until n) peak = maxOf(peak, abs(chunk[i].toInt())); peak > 1500 }
                if (isSpeech) { speech = true; quietChunks = 0; speechAt += k } else if (speech) quietChunks++
                k++
                if (speech && quietChunks >= 25) break // ok. 0,8 s ciszy po mowie
            }
        } finally { runCatching { rec.stop() }.logged("ForkVoice", "WhisperListen.kt:41"); rec.release() }
        if (!speech || speechAt.size < 3) { android.util.Log.i("ForkVoice", "głos: bez mowy (VAD ${if (vad != null) "Silero" else "próg"}) — Whisper pominięty"); return@withContext null }
        // Tylko fragment z mową (+0,2 s przed, +0,3 s po): krócej = szybciej, mniej zmyśleń na ciszy.
        val from = ((speechAt.first() * 512) - RATE / 5).coerceAtLeast(0)
        val to = ((speechAt.last() + 1) * 512 + RATE * 3 / 10).coerceAtMost(all.size)
        val samples = FloatArray(to - from) { all[from + it] / 32768f }
        val text = try {
            VoicePolishAsr.transcribe(AsrModel.directory(c), samples)
        } catch (e: Exception) {
            android.util.Log.w("ForkVoice", "głos: Whisper pl: ${e.javaClass.simpleName}: ${e.message}"); ""
        }
        android.util.Log.i("ForkVoice", "głos: ${samples.size * 1000 / RATE} ms mowy z ${all.size * 1000 / RATE} ms nagrania → ${text.length} znaków")
        VoiceVocabulary.snap(text).takeIf { it.isNotEmpty() }
    }
}
