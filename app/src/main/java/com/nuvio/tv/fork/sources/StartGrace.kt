package com.nuvio.tv.fork.sources

import android.content.Context
import android.util.Log
import com.nuvio.tv.ui.screens.player.PlayerRuntimeController
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Test VOD na JMGO 26.11 („Oppenheimer”, `uxr15`): plik ruszył po 14,7 s od startu próby, a 0,3 s później
 * „Gdy film się zatnie” (Reshaped `StartupSourceRecovery`, 15 s na próbę) uznał start za nieudany i przełączył na inny,
 * cięższy plik. Reshaped potwierdza start dopiero drugą próbką pozycji (co 0,5 s), a limit minął wcześniej.
 *
 * Tu: gdy obraz już jest, film gra (bez doczytywania i błędu), a do końca limitu próby zostały najwyżej 2 s, czuwanie
 * startu kończy się tak jak po potwierdzeniu w Reshaped — bez zmiany źródła. Zero zmian w bazie: wołane z
 * [com.nuvio.tv.fork.ForkHooks.playerBridge] (plik forka). Wyłącznik: ForkFeatures [KEY].
 */
object StartGrace {
    const val KEY = "fork_start_grace"
    private const val TAG = "ForkSources"
    private const val NEAR_DEADLINE_MS = 2_000L

    /** Wołane na wątku głównym przy każdej zmianie stanu i pozycji odtwarzacza. */
    fun onState(c: Context, controller: PlayerRuntimeController, s: PlayerUiState) {
        val recovery = controller.autoSourceRecovery ?: return
        if (!s.isPlaying || s.isBuffering || s.error != null || !controller.hasRenderedFirstFrame) return
        val left = recovery.remainingMs()
        if (left > NEAR_DEADLINE_MS) return // Reshaped zdąży potwierdzić start sam
        if (!ForkFeatures.enabled(c, KEY, true)) return
        try {
            recovery.cancel()
            Log.i(TAG, "start: film już gra (obraz jest), do limitu próby zostało $left ms — koniec czuwania startu bez zmiany źródła")
        } catch (e: IllegalStateException) {
            Log.w(TAG, "start: nie udało się zakończyć czuwania startu: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
