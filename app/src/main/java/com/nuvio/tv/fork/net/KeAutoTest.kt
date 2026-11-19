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
import com.nuvio.tv.fork.logged

/**
 * Kolejka 19.11 (W1 etap 2, KE PerPlayNetwork „a play from it tests”): test łącza sam, raz na usługę.
 * KE wstrzymuje start filmu na czas testu (PlayerNetworkTestHold, ekran postępu, Wstecz pomija); właściciel 19.11
 * woli bez blokowania: (1) [onSources] — w tle na ekranie wyboru źródeł, gdy wybierasz ręcznie, bez postępu na ekranie;
 * (2) [onPlayerClosed] — zapas po filmie (autostart nie zostawia czasu na ekranie źródeł).
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

    private var sourcesPending: Job? = null

    /** Która usługa ze źródeł jeszcze nie ma pomiaru (null = nie ma czego mierzyć albo próbowano w ciągu doby). */
    private fun due(app: Context, url: String): String? {
        val key = serviceKeyOf(url) ?: return null
        val partial = KeNetwork.isPartial(app, key)
        if (!partial && KeNetwork.tables(app).currentTable(key, KeNetwork.transport(app)) != null) return null
        val last = ForkFeatures.str(app, TRIED + key).toLongOrNull() ?: 0L
        // wynik częściowy uzupełniamy przy najbliższej okazji (co najmniej 10 min odstępu), brak wyniku: raz na dobę
        val gap = if (partial) 10 * 60_000L else 24 * 3_600_000L
        return key.takeIf { System.currentTimeMillis() - last >= gap }
    }

    /**
     * Ekran źródeł (hook w StreamScreenViewModel, przy każdej nowej liście): gdy nie ma autostartu, po 3 s ciszy
     * test usługi najwyżej ocenionego źródła HTTP bez pomiaru. Po cichu (tylko log); start filmu go przerywa.
     */
    fun onSources(c: Context, streams: List<com.nuvio.tv.domain.model.Stream>, ready: Boolean, autoPlay: Boolean) {
        val app = c.applicationContext
        if (autoPlay) { sourcesPending?.cancel(); sourcesPending = null; return } // autostart: test po filmie (onPlayerClosed)
        // Zgłoszenie 19.11 („Władca Pierścieni”: „Gotowe 4K”, start ≥ 0,5–2 min): test w tle ruszał już z ekranu szczegółów
        // (lista źródeł dla „Gotowe”) i trwał przy starcie filmu — anulowanie zadania nie zatrzymuje pobierania w wątkach testu,
        // a bazowy tester zwalnia sesję pobierania odtwarzacza (ParallelRangeDataSource.releaseRetainedSession). Test tylko po filmie.
        if (true) return
        if (!ready || streams.isEmpty()) return // dopiero gdy wszystkie dodatki odpowiedziały (autostart rozstrzygnięty)
        if (!ForkFeatures.enabled(app, KeNetwork.KEY, true) || !ForkFeatures.enabled(app, KEY, true)) return
        if (test?.isActive == true) return
        sourcesPending?.cancel()
        sourcesPending = scope.launch {
            delay(3_000) // lista się uspokaja; szybki wybór = brak testu
            val pick = com.nuvio.tv.fork.sources.KeRanking.order(streams).asSequence()
                .filter { !it.isTorrent() }
                .mapNotNull { s -> s.getStreamUrl()?.takeIf { it.startsWith("http") }?.let { u -> Triple(s, u, due(app, u)) } }
                .firstOrNull { it.third != null } ?: return@launch
            ForkFeatures.setStr(app, TRIED + pick.third, System.currentTimeMillis().toString())
            Log.i(TAG, "sources screen test: ${pick.third}")
            test = KeNetwork.test(app, pick.second, pick.first.behaviorHints?.proxyHeaders?.request.orEmpty()) { msg -> Log.i(TAG, "sources screen test done: $msg") }
        }
    }

    /** Odtwarzacz otwarty: nic nie mierzymy w tle. */
    fun onPlayerOpened() {
        pending?.cancel(); pending = null
        sourcesPending?.cancel(); sourcesPending = null
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
            }.logged("ForkNet", "KeAutoTest.kt:89").getOrNull() ?: return@launch
            val url = d.streamUrl?.takeIf { it.startsWith("http") } ?: return@launch
            val key = due(app, url) ?: return@launch
            ForkFeatures.setStr(app, TRIED + key, System.currentTimeMillis().toString())
            val headers = runCatching {
                val o = org.json.JSONObject(d.headersJson ?: "{}")
                o.keys().asSequence().associateWith { o.getString(it) }
            }.logged("ForkNet", "KeAutoTest.kt:95").getOrDefault(emptyMap())
            Log.i(TAG, "auto test: $key")
            test = KeNetwork.test(app, url, headers) { msg -> Log.i(TAG, "auto test done: $msg") }
        }
    }
}
