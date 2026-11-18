package com.nuvio.tv.fork.family

import android.content.Context
import android.view.KeyEvent
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Statystyki używania dla panelu „Nuvio Rodzina” (19.11.2026, ulepszanie UX/UI). Tylko liczniki, bez tytułów i adresów:
 * - ekrany (szablon trasy, np. `detail/{itemId}/{itemType}`): wejścia i czas,
 * - klawisze pilota na każdym ekranie (OK, Wstecz, strzałki, multimedia, inne) — dużo strzałek = trudno dojść do celu,
 * - funkcje: czynności modułów, przełączniki forka, zdarzenia odtwarzacza (zmiana napisów, dźwięku, szybkości, źródła).
 * Wysyłane zbiorczo z raportem rodziny (FamilySync.report) jako zdarzenie `usage`. Przełącznik: `family_usage_stats`.
 */
object FamilyUsage {
    const val KEY = "family_usage_stats"
    private val counts = ConcurrentHashMap<String, Long>()
    @Volatile private var screen = "start"
    @Volatile private var since = android.os.SystemClock.elapsedRealtime()
    private val watched = java.util.Collections.newSetFromMap(java.util.WeakHashMap<Any, Boolean>())

    private fun inc(k: String, by: Long = 1) { if (counts.size < 2000 || counts.containsKey(k)) counts.merge(k, by, Long::plus) }

    /** ForkFeatures.bind: śledź zmiany ekranu tego nawigatora. */
    fun watch(nav: androidx.navigation.NavController) {
        synchronized(watched) { if (!watched.add(nav)) return }
        nav.addOnDestinationChangedListener { _, dest, _ -> onScreen(dest.route ?: dest.label?.toString() ?: "?") }
    }

    private fun onScreen(route: String) {
        val r = route.substringBefore('?').take(80)
        val now = android.os.SystemClock.elapsedRealtime()
        inc("t|$screen", now - since)
        screen = r; since = now
        inc("s|$r")
    }

    /** ForkHooks.onGlobalKey: każde naciśnięcie (bez powtórzeń przy przytrzymaniu). */
    fun key(e: KeyEvent) {
        if (e.action != KeyEvent.ACTION_DOWN || e.repeatCount > 0) return
        val k = when (e.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> "ok"
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> "back"
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> "dpad"
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "media"
            KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_VOICE_ASSIST, KeyEvent.KEYCODE_ASSIST -> "voice"
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> "menu"
            else -> "other"
        }
        inc("k|$screen|$k")
    }

    /** Użycie funkcji (nazwa bez danych osobowych). */
    fun feature(name: String) = inc("f|${name.take(80)}")

    // Odtwarzacz: zmiany stanu = użycie przycisków (napisy, dźwięk, szybkość, źródło, pauza).
    private var lastSub = Int.MIN_VALUE; private var lastAudio = Int.MIN_VALUE; private var lastSpeed = 1f
    private var lastUrl: String? = null; private var lastPlaying = false
    fun player(subIndex: Int, audioIndex: Int, speed: Float, url: String?, playing: Boolean) {
        if (url != lastUrl) { if (lastUrl != null && url != null) feature("player:zmiana_źródła"); lastUrl = url; lastSub = subIndex; lastAudio = audioIndex; lastPlaying = playing; return }
        if (subIndex != lastSub && lastSub != Int.MIN_VALUE) feature(if (subIndex < 0) "player:napisy_wył" else "player:napisy_zmiana")
        if (audioIndex != lastAudio && lastAudio != Int.MIN_VALUE) feature("player:dźwięk_zmiana")
        if (speed != lastSpeed) feature("player:szybkość_$speed")
        if (playing != lastPlaying && !playing) feature("player:pauza")
        lastSub = subIndex; lastAudio = audioIndex; lastSpeed = speed; lastPlaying = playing
    }

    /** FamilySync.report: zabiera liczniki (pusty, gdy przełącznik wyłączony). */
    fun drain(c: Context): JSONObject? {
        val now = android.os.SystemClock.elapsedRealtime()
        inc("t|$screen", now - since); since = now
        if (!com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, KEY, true)) { counts.clear(); return null }
        if (counts.isEmpty()) return null
        val o = JSONObject()
        for (k in counts.keys.toList()) { val v = counts.remove(k) ?: continue; if (v > 0) o.put(k, v) }
        return o
    }
}
