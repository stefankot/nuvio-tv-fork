package com.nuvio.tv.fork.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.nuvio.tv.ui.screens.player.audiosync.asr.AsrModel
import com.nuvio.tv.ui.screens.player.audiosync.asr.SharedRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import com.nuvio.tv.fork.logged

/**
 * Mowa na tekst offline (B07-6, silnik 2): mikrofon 16 kHz → prosty detektor ciszy → Whisper z sherpa-onnx (model wybrany w ustawieniach
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
        val chunk = ShortArray(RATE / 10)
        var speech = false; var quietChunks = 0
        try {
            rec.startRecording()
            val limit = RATE * 6
            while (all.size < limit) {
                val n = rec.read(chunk, 0, chunk.size); if (n <= 0) break
                var peak = 0; for (i in 0 until n) peak = maxOf(peak, abs(chunk[i].toInt()))
                for (i in 0 until n) all += chunk[i]
                if (peak > 1500) { speech = true; quietChunks = 0 } else if (speech) quietChunks++
                if (speech && quietChunks >= 9) break
            }
        } finally { runCatching { rec.stop() }.logged("ForkVoice", "WhisperListen.kt:41"); rec.release() }
        if (!speech || all.size < RATE / 3) return@withContext null
        val samples = FloatArray(all.size) { all[it] / 32768f }
        val stt = SharedRecognizer.acquire(AsrModel.directory(c), 2)
        try { stt.transcribe(samples).joinToString(" ") { it.second }.trim().takeIf { it.isNotEmpty() } } finally { SharedRecognizer.release() }
    }
}
