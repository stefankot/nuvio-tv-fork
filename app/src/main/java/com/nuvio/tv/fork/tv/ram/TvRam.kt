package com.nuvio.tv.fork.tv.ram

import android.content.Context
import android.util.Log
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Telewizja w pamięci tylko na ekranie telewizji (właściciel 23.11: „aplikacja ma wczytać Whisper, będzie miała model TTS,
 * które muszą być w RAM — wczytywać do RAM wszystko, co związane z IPTV, tylko gdy użytkownik jest w zakładce IPTV,
 * a w RAM trzymać małą próbkę startową”).
 *
 * JMGO vod1 (23.11): po odsianiu VOD sterta nadal ~365 MB z 512, bo (1) rozgrzewka [LiveTvRepository.warmUp] wczytywała
 * listy i przewodnik 8 s po starcie aplikacji, (2) Reshaped zwalnia je dopiero 5 min po wyjściu z telewizji, (3) moduły
 * forka trzymały własne kopie (TvBridge.lastKept — cały przewodnik, TvStore — mapa kanałów, pamięci indeksów, kategorie
 * programów, mapy list Niuni, indeks szukania), więc nawet zwolnienie w Reshaped nic nie oddawało.
 *
 * Włączone ([KEY], domyślnie): bez rozgrzewki; Reshaped zwalnia stan [IDLE_RELEASE_MS] po tym, jak nic go nie pokazuje
 * (ekran telewizji i kanał w odtwarzaczu się liczą); po zwolnieniu ([LiveTvRepository.releases]) moduły forka oddają
 * swoje kopie — na dysku zostaje wszystko (listy, przewodnik, indeksy), następne wejście tylko wczytuje.
 * Wyłączone — jak dotąd (rozgrzewka, 5 min).
 */
object TvRam {
    const val KEY = "tv_ram_only_in_tv"
    private const val TAG = "ForkTvRam"
    /** Po tylu ms bez ekranu telewizji i bez kanału w odtwarzaczu dane telewizji wychodzą z pamięci. */
    const val IDLE_RELEASE_MS = 45_000L

    @Volatile var enabled = true
        private set
    @Volatile private var watching = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Odczyt przełącznika i nasłuch zwolnień Reshaped (raz, przy starcie modułu TV). */
    fun start(c: Context) {
        enabled = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, KEY, true)
        Log.i(TAG, "telewizja w pamięci tylko na ekranie telewizji: ${if (enabled) "tak" else "nie"}")
        if (watching) return
        watching = true
        scope.launch { TvSnapshot.load(c) }
        scope.launch {
            LiveTvRepository.releases.drop(1).collect { n -> if (enabled) releaseForkCopies(n) }
        }
    }

    /** Czas, po którym Reshaped zwalnia nieużywaną telewizję (wołane z łatki `dostawca/LiveTvRepository/12`). */
    fun idleReleaseMs(reshapedDefault: Long): Long = if (enabled) IDLE_RELEASE_MS else reshapedDefault

    /** Rozgrzewka po starcie aplikacji tylko przy wyłączonym module. */
    fun warmUpAllowed(): Boolean = !enabled

    private fun releaseForkCopies(n: Int) {
        val rt = Runtime.getRuntime()
        val before = (rt.totalMemory() - rt.freeMemory()) / 1_048_576
        fun step(name: String, f: () -> Unit) = try { f() } catch (e: Exception) { Log.w(TAG, "zwalnianie $name: ${e.javaClass.simpleName}: ${e.message}") }
        step("przewodnik modułu TV") { com.nuvio.tv.fork.tv.most.TvBridge.releaseMemory() }
        step("indeks szukania") { com.nuvio.tv.fork.tv.index.TvTextIndex.releaseMemory() }
        step("pamięci indeksów") { com.nuvio.tv.fork.tv.index.TvIndexStore.unload() }
        step("kategorie") { com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories.releaseMemory() }
        step("listy Niuni") { com.nuvio.tv.skin.niunia.livetv.NiuniaListMaps.clear() }
        step("szukanie TV") { com.nuvio.tv.fork.discover.TvSearchIndexCache.clear() }
        System.gc()
        val after = (rt.totalMemory() - rt.freeMemory()) / 1_048_576
        Log.i(TAG, "telewizja zwolniona z pamięci (#$n): sterta $before → $after MB")
    }
}
