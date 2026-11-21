package com.nuvio.tv.fork.tv

import android.util.Log
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Paczka „oczywiste” pkt 2 (21.11): w archiwum IPTV Wstecz i zmiana kanału bez pytania „czy na pewno…”.
 * Wstecz wychodzi z odtwarzacza (powrót do Live TV z fokusem na programie — `skin/niunia/livetv/NiuniaLiveReturn`),
 * ▲▼ / CH± od razu zmienia kanał. Wyłączone = dawne pytanie.
 */
object LiveNoAsk {
    const val KEY = "tv_no_ask"
    private const val TAG = "ForkTv"

    fun on(c: android.content.Context): Boolean = ForkFeatures.enabled(c, KEY, true)

    /** Wstecz w archiwum: true = obsłużone (wyjście z odtwarzacza). */
    fun back(c: android.content.Context, hide: () -> Unit, exit: (() -> Unit)?): Boolean {
        if (!on(c)) return false
        if (exit == null) { Log.w(TAG, "archiwum: Wstecz bez wyjścia (brak exit) — pytanie jak dawniej"); return false }
        Log.i(TAG, "archiwum: Wstecz → wyjście bez pytania")
        hide(); exit()
        return true
    }

    /** Zmiana kanału w archiwum: true = bez pytania. */
    fun zapNow(c: android.content.Context, step: Int): Boolean = on(c).also { if (it) Log.i(TAG, "archiwum: zmiana kanału o $step bez pytania") }
}
