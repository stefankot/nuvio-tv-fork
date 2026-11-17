package com.nuvio.tv.fork.lektor

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Lektor na systemowym TextToSpeech (B09-3): synteza do pliku WAV, język polski. */
class SystemLektor(private val context: Context) : Lektor {
    private var tts: TextToSpeech? = null

    private suspend fun engine(): TextToSpeech = tts ?: suspendCancellableCoroutine { cont ->
        lateinit var t: TextToSpeech
        t = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) { cont.resumeWithException(Lektor.Unavailable("Brak silnika mowy")); return@TextToSpeech }
            val r = t.setLanguage(Locale("pl", "PL"))
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) { cont.resumeWithException(Lektor.Unavailable("Brak polskiego głosu")); return@TextToSpeech }
            tts = t; cont.resume(t)
        }
    }

    override fun voices(): List<String> = tts?.voices?.filter { it.locale.language == "pl" }?.map { it.name }.orEmpty()

    override suspend fun synthesize(text: String, rate: Float): Pcm {
        val t = engine(); t.setSpeechRate(rate)
        val file = File(context.cacheDir, "lektor_${UUID.randomUUID()}.wav")
        try {
            suspendCancellableCoroutine<Unit> { cont ->
                val id = UUID.randomUUID().toString()
                t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { if (utteranceId == id && cont.isActive) cont.resume(Unit) }
                    @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { if (utteranceId == id && cont.isActive) cont.resumeWithException(Lektor.Unavailable("Błąd syntezy")) }
                })
                if (t.synthesizeToFile(text, null, file, id) != TextToSpeech.SUCCESS) cont.resumeWithException(Lektor.Unavailable("Synteza odrzucona"))
            }
            return readWav(file)
        } finally { file.delete() }
    }

    private fun readWav(f: File): Pcm {
        val b = f.readBytes(); val bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        val rate = if (b.size >= 28) bb.getInt(24) else 22050
        val data = if (b.size > 44) b.copyOfRange(44, b.size) else ByteArray(0)
        val s = ShortArray(data.size / 2); ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(s)
        return Pcm(s, rate)
    }

    fun shutdown() { tts?.shutdown(); tts = null }
}
