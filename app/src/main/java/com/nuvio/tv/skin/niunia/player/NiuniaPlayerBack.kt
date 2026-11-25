package com.nuvio.tv.skin.niunia.player

import android.util.Log
import com.nuvio.tv.ui.screens.player.cancelPauseOverlay
import com.nuvio.tv.ui.screens.player.hideControls

/**
 * Wstecz krok po kroku w nakładce odtwarzacza Niuni (makieta `Player`: panel → odtwarzacz, szczegóły ▼▼ → rząd → pasek).
 * `PlayerScreen` łapie Wstecz na korzeniu (`onPreviewKeyEvent` → `handleBackPress`), zanim dostanie go `BackHandler` skórki,
 * więc wpięcie `// Fork hook: skin` pyta tu najpierw. Nakładka Niuni ustawia [handler], gdy ma otwarty panel albo niższy poziom;
 * poza Niunią [handler] jest null i wpięcie nic nie kosztuje.
 */
object NiuniaPlayerBack {
    @Volatile var handler: (() -> Boolean)? = null

    /**
     * Odtwarzacz z ostatnią nakładką Niuni (słabo — nie trzyma odtwarzacza po wyjściu). UX JMGO 25.11 (#49): na ekranie pauzy bazy
     * Wstecz pokazywał z powrotem panel (wygląda jak „nic się nie stało”), wyjście z zatrzymanego filmu = 3× Wstecz.
     */
    @Volatile var controller: java.lang.ref.WeakReference<com.nuvio.tv.ui.screens.player.PlayerRuntimeController>? = null

    /** true = nakładka skórki obsłużyła Wstecz (odtwarzacz nie chowa sterowania i nie wychodzi). */
    fun consume(): Boolean {
        if (closePauseScreen()) return true
        val h = handler ?: return false
        return try {
            h()
        } catch (e: RuntimeException) {
            Log.w("ForkNiunia", "odtwarzacz: Wstecz: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }

    /** Ekran pauzy bazy → zatrzymany film bez panelu (jedno Wstecz mniej). */
    private fun closePauseScreen(): Boolean {
        val c = controller?.get() ?: return false
        return try {
            if (!c.uiState.value.showPauseOverlay) return false
            c.cancelPauseOverlay()
            c.hideControls()
            Log.i("ForkNiunia", "odtwarzacz: Wstecz na ekranie pauzy → zatrzymany film (bez panelu)")
            true
        } catch (e: RuntimeException) {
            Log.w("ForkNiunia", "odtwarzacz: Wstecz na ekranie pauzy: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }
}
