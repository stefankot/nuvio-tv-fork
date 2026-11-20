package com.nuvio.tv.skin.niunia.player

import android.util.Log

/**
 * Wstecz krok po kroku w nakładce odtwarzacza Niuni (makieta `Player`: panel → odtwarzacz, szczegóły ▼▼ → rząd → pasek).
 * `PlayerScreen` łapie Wstecz na korzeniu (`onPreviewKeyEvent` → `handleBackPress`), zanim dostanie go `BackHandler` skórki,
 * więc wpięcie `// Fork hook: skin` pyta tu najpierw. Nakładka Niuni ustawia [handler], gdy ma otwarty panel albo niższy poziom;
 * poza Niunią [handler] jest null i wpięcie nic nie kosztuje.
 */
object NiuniaPlayerBack {
    @Volatile var handler: (() -> Boolean)? = null

    /** true = nakładka skórki obsłużyła Wstecz (odtwarzacz nie chowa sterowania i nie wychodzi). */
    fun consume(): Boolean {
        val h = handler ?: return false
        return try {
            h()
        } catch (e: RuntimeException) {
            Log.w("ForkNiunia", "odtwarzacz: Wstecz: ${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }
}
