package com.nuvio.tv.fork.net

import android.content.Context
import com.nuvio.tv.core.assessment.NetworkTransport
import com.nuvio.tv.core.assessment.ServiceNetworkTables
import com.nuvio.tv.core.assessment.serviceKeyOf
import com.nuvio.tv.core.assessment.tableFromSweep
import com.nuvio.tv.core.network.NetworkTransportReader
import com.nuvio.tv.core.assessment.ServiceTable
import com.nuvio.tv.core.network.StreamSweepEngine
import com.nuvio.tv.core.stream.StreamLimitSettings
import com.nuvio.tv.core.stream.debridServiceTableKeys
import com.nuvio.tv.core.stream.streamLimitVerdict
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * W1 (18.11, zgoda właściciela 19.11): adapter testu i limitu łącza KE (0a87a08, pliki 1:1 w core/network,
 * core/assessment, core/stream). Tabele KE (prędkość na usługę i łącze) trzymamy w prefs forka zamiast
 * pola `serviceNetworkTablesJson` w PlayerSettingsDataStore KE. Wynik testu ustawia też liczbę połączeń
 * i rozmiar kawałka bazowego pobierania równoległego (ParallelRangeDataSource bazy).
 */
object KeNetwork {
    const val KEY = "ke_stream_limit"           // limit łącza w rankingu (domyślnie włączony)
    private const val TABLES = "ke_service_network_tables"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile var status: String = ""
        private set
    @Volatile private var running = false

    fun tables(c: Context): ServiceNetworkTables = ServiceNetworkTables.decode(ForkFeatures.str(c, TABLES).ifEmpty { null })
    private fun save(c: Context, t: ServiceNetworkTables) = ForkFeatures.setStr(c, TABLES, t.encode())

    fun transport(c: Context): NetworkTransport = runCatching { NetworkTransportReader.current(c) }.getOrDefault(NetworkTransport.OTHER)

    /** Test łącza na adresie [url] (zwykle bieżący film): mierzy usługę i zapisuje tabelę; [onDone] dostaje komunikat. */
    fun test(c: Context, url: String, headers: Map<String, String>, onDone: (String) -> Unit): kotlinx.coroutines.Job? {
        if (running) { onDone("Test łącza już trwa"); return null }
        val app = c.applicationContext
        running = true; status = "Test łącza…"
        return scope.launch {
            try {
            val msg = runCatching {
                val requestKey = serviceKeyOf(url) ?: return@runCatching "Tego źródła nie da się zmierzyć"
                // Baza nie zwraca adresu po przekierowaniach (KE ContentProbe.finalUrl): tabela pod kluczem adresu żądania.
                val servingKey = requestKey
                var step = 0
                val outcome = StreamSweepEngine.run(app, url, headers, null, onState = {}, onPassAdded = {},
                    onPassResult = { _, _, _ -> }, onRunStarted = { status = "Test łącza: krok ${++step}" })
                val table = tableFromSweep(outcome, transport(app), System.currentTimeMillis()) ?: return@runCatching "Test łącza nic nie zmierzył"
                save(app, tables(app).withTest(requestKey, servingKey, table))
                val best = table.rows.maxByOrNull { it.mbps }
                if (best != null && best.connections > 1) {
                    val store = EntryPointAccessors.fromApplication(app, com.nuvio.tv.fork.sources.SourceChoice.Deps::class.java).player()
                    store.setUseParallelConnections(true)
                    store.setParallelConnectionCount(best.connections)
                    if (best.chunkMb > 0) store.setParallelChunkSizeKb(best.chunkMb * 1024)
                }
                "Łącze z $servingKey: ${best?.mbps?.toInt() ?: 0} Mbit/s" + (best?.takeIf { it.connections > 1 }?.let { " (${it.connections} połączenia)" } ?: "")
            }.getOrElse { if (it is kotlinx.coroutines.CancellationException) throw it; "Test łącza nie powiódł się" }
            status = msg
            kotlinx.coroutines.withContext(Dispatchers.Main) { onDone(msg) }
            } finally { running = false } // przerwany (np. KeAutoTest przy starcie filmu) też zwalnia blokadę
        }
    }

    /**
     * Limit łącza w rankingu: źródła, których szacowany bitrate przekracza zmierzoną prędkość usługi
     * (KE StreamLimit, tryb NETWORK_TEST), idą na koniec — nie znikają. Bez testu danej usługi bez zmian.
     */
    fun overLimit(c: Context, streams: List<Stream>, runtimeMinutes: Int?, episode: Boolean): Set<Stream> {
        if (!ForkFeatures.enabled(c, KEY, true)) return emptySet()
        val t = tables(c)
        if (t.tables.isEmpty()) return emptySet()
        val tr = transport(c); val debrid = debridServiceTableKeys(streams, t)
        return streams.filter { s ->
            runCatching { streamLimitVerdict(s, StreamLimitSettings(), t, tr, runtimeMinutes, episode, debrid)?.hidden == true }.getOrDefault(false)
        }.toSet()
    }
}

/** Kopia 1:1 z KE 0a87a08 `core/assessment/ServiceNetworkTestRunner.kt` (reszta runnera zależy od DeviceAssessmentApplier KE). */
fun ServiceNetworkTables.withTest(requestKey: String, servingKey: String, table: ServiceTable): ServiceNetworkTables {
    val nextAliases = (aliases - servingKey - requestKey)
        .let { if (requestKey == servingKey) it else it + (requestKey to servingKey) }
    val unreached = listOfNotNull(aliases[requestKey], requestKey).filter { it != servingKey }
    val orphaned = unreached.filter { key ->
        key !in nextAliases.values && tables[key]?.let(::retestDue) == true
    }.toSet()
    return copy(tables = tables - orphaned + (servingKey to table), aliases = nextAliases)
}
