package com.nuvio.tv.fork.lektor

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import com.nuvio.tv.fork.logged

/**
 * P3/P9 (18.11): czy na urządzeniu jest silnik mowy z polskim głosem (JMGO: brak, Xiaomi: jest).
 * Sprawdzane raz po starcie w tle; [ready] = null, dopóki nie wiadomo. Od wyniku zależą lektor auto i ranking źródeł.
 */
object LektorVoice {
    @Volatile var ready: Boolean? = null
        private set
    @Volatile private var started = false
    /** Komunikat o braku głosu przy lektorze auto już pokazany (raz na uruchomienie). */
    @Volatile var notified = false

    const val MISSING_MSG = "Brak głosu lektora na tym telewizorze. Zainstaluj „Google Mowa” (Speech Services by Google) ze Sklepu Play i pobierz język polski."

    /** Scalenie 19.11: lektor ma głos, gdy jest polski głos systemowy ALBO klucz Gemini. */
    fun usable(c: Context): Boolean = ready != false || com.nuvio.tv.ainarrator.LektorVoiceChoice.cloudVoicePresent(c) // 19.11: + Chirp

    fun probe(context: Context, onResult: ((Boolean) -> Unit)? = null) {
        if (started) { ready?.let { onResult?.invoke(it) }; return }
        started = true
        val app = context.applicationContext
        runCatching {
            // Fork 19.11: na JMGO (brak silnika) odpowiedź przychodzi od razu, w konstruktorze — przed przypisaniem t.
            var t: TextToSpeech? = null
            var answered = false
            t = TextToSpeech(app) { status ->
                val ok = status == TextToSpeech.SUCCESS && runCatching {
                    t!!.isLanguageAvailable(Locale("pl", "PL")) >= TextToSpeech.LANG_AVAILABLE
                }.logged("ForkLektor", "LektorVoice: język").getOrDefault(false)
                ready = ok
                android.util.Log.i("ForkLektor", "polish_tts=$ok status=$status engine=${t?.defaultEngine}")
                answered = true
                t?.let { e -> runCatching { e.shutdown() }.logged("ForkLektor", "LektorVoice: shutdown") }
                onResult?.invoke(ok)
            }
            if (answered) runCatching { t?.shutdown() }.logged("ForkLektor", "LektorVoice: shutdown po konstruktorze")
        }.onFailure { android.util.Log.w("ForkLektor", "LektorVoice init: ${it.javaClass.simpleName}: ${it.message?.take(200)}"); ready = false; onResult?.invoke(false) }
    }
}
