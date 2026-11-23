package com.nuvio.tv.fork.tv.guide

import android.content.Context
import android.util.Log
import com.nuvio.tv.reshaped.livetv.LiveTvGuideCache
import com.nuvio.tv.reshaped.livetv.LiveTvGuideWindow

/**
 * Zapis przewodnika ważny mimo drobnych zmian listy kanałów (właściciel 23.11: „ekran IPTV startuje ok. 100 sekund”).
 *
 * JMGO ram1/vod1 23.11: przy KAŻDYM wejściu `saved programmes: miss (no file)` — klucz zapisu Reshaped zawiera wszystkie
 * kanały (klucze przewodnika, nazwy, archiwum), a lista Xtream dostawcy zmienia się co kilka godzin (dodane/usunięte
 * kanały), więc zapis nie pasował i 5 przewodników XMLTV było czytanych od zera: program „teraz” po ~67 s, pełny
 * przewodnik po ~130 s, sterta do 476 MB.
 *
 * Włączone ([KEY], domyślnie): klucz zapisu tylko z adresów przewodników, źródeł (profil ma własne) i okna przewodnika.
 * Zmiana listy kanałów nie kasuje zapisu: kanały, które były, mają program od razu; nowe kanały dostaną go przy
 * następnym zwykłym odczycie (nowe pobranie przewodnika, najpóźniej co kilkanaście godzin, albo „Odśwież”).
 * Dodanie lub usunięcie źródła czy przewodnika nadal czyta wszystko od nowa. Wyłączone — jak w Reshaped.
 */
object GuideReuse {
    const val KEY = "tv_guide_reuse"
    private const val TAG = "ForkTvGuide"

    @Volatile private var enabled: Boolean? = null

    /** Klucz zapisu przewodnika albo null (moduł wyłączony → klucz Reshaped). Wołane z łatki `dostawca/LiveTvRepository/14`. */
    internal fun cacheKey(context: Context, epgUrls: List<String>, sourceIds: List<String>, window: LiveTvGuideWindow): Long? {
        val on = enabled ?: com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(context, KEY, true).also { enabled = it }
        if (!on) return null
        val key = LiveTvGuideCache.key(epgUrls, sourceIds.toSortedSet(), window) * 31 + 0x5EED
        Log.i(TAG, "zapis przewodnika: klucz z ${epgUrls.size} przewodników i ${sourceIds.size} źródeł (zmiany listy kanałów nie kasują zapisu)")
        return key
    }

    /** Ustawienia: zmiana przełącznika działa od następnego odczytu przewodnika. */
    fun reset() { enabled = null }
}
