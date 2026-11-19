package com.nuvio.tv.fork.lektor

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nuvio.tv.ui.screens.player.cancelAutoQualityChange

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
                // Fork 19.11 (build118 „stream się wyłączył”): chwilowo pusty bufor po zmianie dekodowania auto jakość brała
                // za słabe łącze (1080p → 720p, potem źródła bez polskiego dźwięku do wyczerpania). Odwołanie jej decyzji
                // z tych kilku sekund (zmiana zapowiadana 5 s przed wykonaniem).
                val main = Handler(Looper.getMainLooper())
                for (delayMs in longArrayOf(3_000, 6_000, 9_000)) main.postDelayed({
                    runCatching { c.cancelAutoQualityChange(byViewer = false) }.onFailure { e -> Log.w(TAG, "PCM: auto jakość: ${e.javaClass.simpleName}: ${e.message}") }
                }, delayMs)
            }.onFailure { Log.w(TAG, "PCM ($reason): ${it.javaClass.simpleName}: ${it.message?.take(200)}") }
        }
    }
}
