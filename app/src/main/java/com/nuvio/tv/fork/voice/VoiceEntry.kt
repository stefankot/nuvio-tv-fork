package com.nuvio.tv.fork.voice

import android.content.Context
import android.speech.SpeechRecognizer
import android.util.Log
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Paczka „oczywiste” pkt 12 (21.11): wejście do sterowania głosem w interfejsie (przycisk „Powiedz” w wyszukiwarce Niuni),
 * domyślnie włączone ([KEY]). Pilot JMGO nie wysyła klawisza mikrofonu do aplikacji — przycisk na ekranie działa zawsze.
 * Rozpoznawanie: systemowe (na Xiaomi/JMGO — usługa producenta albo Google), a gdy go nie ma albo zawiedzie — Whisper (model offline).
 */
object VoiceEntry {
    const val KEY = "voice_ui_button"
    private const val TAG = "ForkVoice"

    fun visible(c: Context): Boolean =
        ForkFeatures.enabled(c, com.nuvio.tv.fork.registry.ForkRegistry.switchKey(VoiceModule.id), VoiceModule.defaultEnabled) && ForkFeatures.enabled(c, KEY, true)

    fun start(c: Context, from: String) {
        Log.i(TAG, "przycisk głosu ($from): ${describe(c)}")
        VoiceTrigger.fire()
    }

    /** Co jest dostępne na tym urządzeniu (log przy każdym użyciu — do sprawdzenia na Xiaomi). */
    fun describe(c: Context): String {
        val system = SpeechRecognizer.isRecognitionAvailable(c)
        val onDevice = if (android.os.Build.VERSION.SDK_INT >= 31) SpeechRecognizer.isOnDeviceRecognitionAvailable(c).toString() else "brak (Android < 12)"
        val service = android.provider.Settings.Secure.getString(c.contentResolver, "voice_recognition_service") ?: "brak"
        return "systemowe=$system (usługa $service), na urządzeniu=$onDevice, whisper=${WhisperListen.available(c)}"
    }
}
