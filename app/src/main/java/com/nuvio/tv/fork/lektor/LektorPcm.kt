package com.nuvio.tv.fork.lektor

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Fork 19.11 (build116, „Strefa gangsterów”, E-AC-3 5.1): przy przekazywaniu Dolby (passthrough) Android omija procesory
 * dźwięku, więc lektor syntezował głos, a mikser w ogóle się nie włączał (brak wpisu „mikser: format”). Lektor włączony =
 * Dolby dekodowane do PCM (hook w PlaybackSpeedAwareAudioSink.shouldRejectDirectPlayback), jak przy słuchawkach Bluetooth;
 * ponowny wybór ścieżek w miejscu (ten sam sposób co applyBluetoothAudioRouteInPlace), film gra dalej.
 */
object LektorPcm {
    private const val TAG = "ForkLektor"

    fun reapply(reason: String) {
        Handler(Looper.getMainLooper()).post {
            runCatching {
                val c = com.nuvio.tv.fork.ai.subtitles.AiSubtitles.controller()
                if (c == null) { Log.w(TAG, "PCM ($reason): brak odtwarzacza Exo"); return@runCatching }
                c.playbackSpeedAwareAudioSink?.notifyAudioProcessingRequirementChanged()
                c._exoPlayer?.let { p -> p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().build() }
                Log.i(TAG, "PCM ($reason): lektor=${LektorEngine.enabled}, ścieżka dźwięku wybrana ponownie")
            }.onFailure { Log.w(TAG, "PCM ($reason): ${it.javaClass.simpleName}: ${it.message?.take(200)}") }
        }
    }
}
