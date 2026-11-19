package com.nuvio.tv.ainarrator

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Fork 19.11 (właściciel: „dodaj w ustawieniach wybór głosu z samplami”): lista 30 polskich głosów Chirp 3 HD;
 * zaznaczenie głosu odtwarza próbkę z assets (`fork/lektor-chirp/<Głos>.m4a`, nagrane raz z Cloud TTS — offline, bez kosztu),
 * „Wybierz” zapisuje głos lektora.
 */
object ChirpVoicePicker {
    private const val TAG = "ForkLektor"
    /** Podział wg próbek (19.11); Google nie podaje płci w API. */
    private val FEMALE = setOf("Kore", "Aoede", "Achernar", "Autonoe", "Callirrhoe", "Despina", "Erinome", "Gacrux",
        "Laomedeia", "Leda", "Pulcherrima", "Sulafat", "Vindemiatrix", "Zephyr")

    fun open(c: Context) {
        val activity = c.findActivity()
        if (activity == null) {
            Log.w(TAG, "wybór głosu Chirp: brak ekranu (Activity) — okno nie otwarte")
            Toast.makeText(c, "Nie mogę otworzyć wyboru głosu na tym ekranie", Toast.LENGTH_LONG).show()
            return
        }
        val voices = ChirpLektor.CHIRP_VOICES
        val current = ChirpLektor.voice(c)
        var picked = voices.indexOf(current).coerceAtLeast(0)
        var player: MediaPlayer? = null
        fun stop() { runCatching { player?.release() }.onFailure { Log.w(TAG, "próbka: zwolnienie: ${it.javaClass.simpleName}") }; player = null }
        fun play(voice: String) {
            stop()
            try {
                val fd = activity.assets.openFd("fork/lektor-chirp/$voice.m4a")
                player = MediaPlayer().apply {
                    fd.use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                    setOnCompletionListener { stop() }
                    prepare(); start()
                }
                Log.i(TAG, "wybór głosu Chirp: próbka $voice")
            } catch (e: Exception) {
                Log.w(TAG, "wybór głosu Chirp: próbka $voice: ${e.javaClass.simpleName}: ${e.message}")
                Toast.makeText(activity, "Nie udało się odtworzyć próbki: $voice", Toast.LENGTH_SHORT).show()
            }
        }
        val labels = voices.map { v -> (if (v in FEMALE) "♀ " else "♂ ") + v + if (v == current) "  (obecny)" else "" }.toTypedArray()
        Log.i(TAG, "wybór głosu Chirp: otwarty (obecny $current)")
        AlertDialog.Builder(activity)
            .setTitle("Głos lektora (Chirp) — OK odtwarza próbkę")
            .setSingleChoiceItems(labels, picked) { _, which -> picked = which; play(voices[which]) }
            .setPositiveButton("Wybierz") { _, _ ->
                val v = voices[picked]
                ForkFeatures.setStr(activity, ChirpLektor.K_CHIRP_VOICE, v)
                ForkFeatures.setStr(activity, LektorVoiceChoice.K_VOICE, "chirp")
                Log.i(TAG, "wybór głosu Chirp: $current → $v")
                Toast.makeText(activity, "Głos lektora: $v", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Anuluj", null)
            .setOnDismissListener { stop() }
            .show()
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
