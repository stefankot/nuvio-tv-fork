package com.nuvio.tv.fork.lektor

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * P3/P9 (18.11): czy na urządzeniu jest silnik mowy z polskim głosem (JMGO: brak, Xiaomi: jest).
 * Sprawdzane raz po starcie w tle; [ready] = null, dopóki nie wiadomo. Od wyniku zależą lektor auto i ranking źródeł.
 */
object LektorVoice {
    @Volatile var ready: Boolean? = null
        private set
    @Volatile private var started = false

    const val MISSING_MSG = "Brak głosu lektora na tym telewizorze. Zainstaluj „Google Mowa” (Speech Services by Google) ze Sklepu Play i pobierz język polski."

    fun probe(context: Context, onResult: ((Boolean) -> Unit)? = null) {
        if (started) { ready?.let { onResult?.invoke(it) }; return }
        started = true
        val app = context.applicationContext
        runCatching {
            lateinit var t: TextToSpeech
            t = TextToSpeech(app) { status ->
                val ok = status == TextToSpeech.SUCCESS && runCatching {
                    t.isLanguageAvailable(Locale("pl", "PL")) >= TextToSpeech.LANG_AVAILABLE
                }.getOrDefault(false)
                ready = ok
                android.util.Log.i("LektorVoice", "polish_tts=$ok engine=${runCatching { t.defaultEngine }.getOrNull()}")
                runCatching { t.shutdown() }
                onResult?.invoke(ok)
            }
        }.onFailure { ready = false; onResult?.invoke(false) }
    }
}
