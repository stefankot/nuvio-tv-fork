package com.nuvio.tv.fork.tv

import android.content.Context
import android.util.Log
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

/**
 * Prawdziwa długość archiwum (22.11, właściciel: „sprawdzać ręcznie, ile naprawdę jest archiwum — np. TV Team 14 dni
 * zamiast deklarowanych 7”). Raz na dobę na źródło (lista IPTV): pierwszy kanał z archiwum, sztuczny program sprzed
 * D dni, link archiwum z Reshaped (`catchupChannel`) i sprawdzenie, czy strumień odpowiada (ForgeAiringRace.answers);
 * wyszukiwanie binarne między 1 a [MAX_DAYS] dni (ok. 5 zapytań). Wynik w prefs, logi `ForkCatchup`.
 *
 * Uwaga: przewodnik i archiwum programów Reshaped trzymają najwyżej 7 dni wstecz (LiveTvEpg/LiveTvArchive) — starsze
 * programy nie są w ogóle na liście; pokazanie 14 dni wymaga zmiany w bazie (plan/BLOKADY.md). Tu prawdziwa długość
 * służy etykiecie „Dostępny jeszcze …” (program znika z archiwum dostawcy dopiero po prawdziwej długości).
 */
object CatchupDepth {
    private const val TAG = "ForkCatchup"
    private const val PREFS = "fork_catchup_depth"
    const val MAX_DAYS = 21
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val HOUR_MS = 60L * 60 * 1000
    private const val EVERY_MS = DAY_MS
    private val days = ConcurrentHashMap<String, Int>()
    @Volatile private var loaded = false
    private val running = Mutex()

    fun sourceKey(ch: LiveTvChannel): String = ch.sourceId.ifBlank { ch.streamUrl.substringAfter("://").substringBefore('/') }

    fun ensure(c: Context) {
        if (loaded) return
        val p = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.all.forEach { (k, v) -> if (k.startsWith("days_") && v is Int && v > 0) days[k.removePrefix("days_")] = v }
        loaded = true
        if (days.isNotEmpty()) Log.i(TAG, "archiwum: zapisane długości ${days.entries.joinToString { "${it.key.take(24)}=${it.value} dni" }}")
    }

    /** Sprawdzona długość archiwum źródła kanału albo null (jeszcze nie sprawdzono). */
    fun realDays(ch: LiveTvChannel): Int? = days[sourceKey(ch)]

    /** Ile godzin program jest jeszcze w archiwum (prawdziwa długość, inaczej deklarowana); null = bez archiwum. */
    fun hoursLeft(ch: LiveTvChannel, p: LiveTvProgramme, now: Long): Int? {
        val d = realDays(ch) ?: ch.catchup?.days ?: return null
        if (d <= 0 || p.stopEpochMs > now) return null
        return ((p.startEpochMs + d * DAY_MS - now) / HOUR_MS).toInt().takeIf { it >= 0 }
    }

    /** Sprawdza źródła, których wynik jest starszy niż doba (w tle, jedno sprawdzanie naraz). */
    suspend fun probeStale(c: Context, channels: List<LiveTvChannel>) {
        ensure(c)
        if (!running.tryLock()) return
        try {
            val prefs = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            // JMGO 22.11 (252 827 kanałów): wołane z ekranu Live TV — przegląd listy poza wątkiem UI.
            val firsts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { firstWithArchive(channels) }
            firsts.forEach { (src, ch) ->
                if (now - prefs.getLong("at2_$src", 0L) < EVERY_MS) return@forEach
                val declared = ch.catchup?.days ?: 0
                val real = try { probe(ch, declared, now) } catch (e: CancellationException) { throw e } catch (e: Exception) {
                    Log.w(TAG, "archiwum: ${ch.name}: ${e.javaClass.simpleName}: ${e.message}"); null
                }
                prefs.edit().putLong("at2_$src", now).apply()
                if (real == null) { Log.i(TAG, "archiwum: ${ch.name} (${src.take(24)}): nie odpowiada nawet dzień wstecz — zostaje deklarowane $declared dni"); return@forEach }
                days[src] = real
                prefs.edit().putInt("days_$src", real).apply()
                Log.i(TAG, "archiwum: ${src.take(24)} (${ch.name}): deklarowane $declared dni, naprawdę $real${if (real >= MAX_DAYS) "+" else ""} dni")
            }
        } finally {
            running.unlock()
        }
    }

    /** Pierwszy kanał z archiwum każdego źródła, w kolejności listy. */
    internal fun firstWithArchive(channels: List<LiveTvChannel>): Map<String, LiveTvChannel> {
        val out = LinkedHashMap<String, LiveTvChannel>()
        for (ch in channels) if (ch.catchup != null) out.putIfAbsent(sourceKey(ch), ch)
        return out
    }

    /** Najdłuższe D (dni), dla którego program sprzed D dni odpowiada; null = archiwum nie działa. */
    private suspend fun probe(ch: LiveTvChannel, declared: Int, now: Long): Int? {
        // Test JMGO build163: Reshaped nie buduje linku archiwum dla programu starszego niż deklarowane dni (null bez
        // zapytania) — sonda nigdy nie wychodziła poza deklarację. Na czas sondy kopia kanału z limitem [MAX_DAYS].
        val probeCh = ch.copy(catchup = ch.catchup?.copy(days = MAX_DAYS))
        suspend fun ok(d: Int): Boolean {
            val start = now - d * DAY_MS + HOUR_MS
            val p = LiveTvProgramme("sonda archiwum", start, start + 30 * 60_000L)
            val replay = LiveTvRepository.catchupChannel(probeCh, p) ?: run { Log.i(TAG, "archiwum: ${ch.name} sprzed $d dni → brak linku"); return false }
            val r = com.nuvio.tv.skin.kicia.livetv.ForgeAiringRace.answers(replay.playback)
            Log.i(TAG, "archiwum: ${ch.name} sprzed $d dni → ${if (r) "jest" else "brak"}")
            return r
        }
        if (!ok(1)) return null
        var lo = 1
        var hi = MAX_DAYS + 1
        val d0 = declared.coerceIn(1, MAX_DAYS)
        if (d0 > 1) { if (ok(d0)) lo = d0 else hi = d0 }
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (ok(mid)) lo = mid else hi = mid
        }
        return lo
    }
}
