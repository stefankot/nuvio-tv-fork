package com.nuvio.tv.fork.net

import android.content.Context
import android.util.Log
import com.nuvio.tv.core.assessment.serviceKeyOf
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Kolejka 19.11 (W1 etap 2, KE PerPlayNetwork „a play from it tests”): test łącza sam, raz na usługę.
 * Uruchamiany PO zamknięciu odtwarzacza (test w trakcie filmu zerwałby jego pobieranie: bazowy
 * StreamSpeedTester zwalnia sesję ParallelRangeDataSource; test przed startem opóźniałby film).
 * Warunki: limit łącza KE włączony, przełącznik „Test łącza sam” (domyślnie wł.), brak aktualnej tabeli
 * tej usługi na tym łączu, nie próbowano tej usługi w ostatnich 24 h. Otwarcie odtwarzacza przerywa test.
 */
object KeAutoTest {
    const val KEY = "ke_auto_network_test"
    private const val TAG = "KeAutoTest"
    private const val TRIED = "ke_auto_test_tried_"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var pending: Job? = null
    @Volatile private var test: Job? = null

    /** Odtwarzacz otwarty: nic nie mierzymy w tle. */
    fun onPlayerOpened() {
        pending?.cancel(); pending = null
        test?.let { if (it.isActive) { Log.i(TAG, "player opened: test cancelled"); it.cancel() } }
        test = null
    }

    /** Odtwarzacz zamknięty: po chwili (sesja pobierania zwolniona) test usługi ostatniego filmu, jeśli jej nie znamy. */
    fun onPlayerClosed(c: Context) {
        val app = c.applicationContext
        if (!ForkFeatures.enabled(app, KeNetwork.KEY, true) || !ForkFeatures.enabled(app, KEY, true)) return
        pending?.cancel()
        pending = scope.launch {
            delay(8_000)
            val d = runCatching {
                EntryPointAccessors.fromApplication(app, com.nuvio.tv.fork.sources.SourceChoice.Deps::class.java).player().lastPlaybackDiagnostics.first()
            }.getOrNull() ?: return@launch
            val url = d.streamUrl?.takeIf { it.startsWith("http") } ?: return@launch
            val key = serviceKeyOf(url) ?: return@launch
            if (KeNetwork.tables(app).currentTable(key, KeNetwork.transport(app)) != null) return@launch
            val last = ForkFeatures.str(app, TRIED + key).toLongOrNull() ?: 0L
            if (System.currentTimeMillis() - last < 24 * 3_600_000L) return@launch
            ForkFeatures.setStr(app, TRIED + key, System.currentTimeMillis().toString())
            val headers = runCatching {
                val o = org.json.JSONObject(d.headersJson ?: "{}")
                o.keys().asSequence().associateWith { o.getString(it) }
            }.getOrDefault(emptyMap())
            Log.i(TAG, "auto test: $key")
            test = KeNetwork.test(app, url, headers) { msg -> Log.i(TAG, "auto test done: $msg") }
        }
    }
}
