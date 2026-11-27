package com.nuvio.tv.skin.niunia.livetv

import android.content.Context
import android.util.Log
import com.nuvio.tv.skin.kicia.livetv.ForgeTvSearchMemory

/**
 * Przegląd ekranów 27.11, ekran 17 pkt 2: „Ostatnio wyszukiwane” pod klawiaturą Szukaj Live TV zostają po
 * ponownym uruchomieniu (wcześniej tylko w pamięci, więc po starcie aplikacji pod klawiaturą było pusto).
 * Długie OK na wpisie = „Usuń z historii”. Historia Live TV osobna od historii filmów (DECYZJE 27.11).
 */
internal object NiuniaLiveRecent {
    private const val TAG = "ForkNiuniaLive"
    private const val PREFS = "niunia_live_recent_searches"
    private const val KEY = "queries"
    private const val MAX = 8
    @Volatile private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val saved = try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        } catch (e: Exception) {
            Log.w(TAG, "historia szukania: odczyt: ${e.javaClass.simpleName}: ${e.message}"); null
        } ?: return
        val inMemory = ForgeTvSearchMemory.recent.toList()
        // Zapis: najnowsze pierwsze. `add` wstawia na początek, więc od najstarszego; wpisy z tej sesji zostają na górze.
        saved.split('\n').filter { it.isNotBlank() }.asReversed().forEach { q ->
            if (inMemory.none { it.equals(q, ignoreCase = true) }) ForgeTvSearchMemory.add(q)
        }
        inMemory.asReversed().forEach { ForgeTvSearchMemory.add(it) }
        Log.i(TAG, "historia szukania: wczytano ${ForgeTvSearchMemory.recent.size} wpisów")
    }

    fun add(context: Context, query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        ForgeTvSearchMemory.add(q)
        save(context)
    }

    fun remove(context: Context, query: String) {
        ForgeTvSearchMemory.recent.removeAll { it.equals(query, ignoreCase = true) }
        Log.i(TAG, "historia szukania: usunięto wpis (zostało ${ForgeTvSearchMemory.recent.size})")
        save(context)
    }

    private fun save(context: Context) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY, ForgeTvSearchMemory.recent.take(MAX).joinToString("\n")).apply()
        } catch (e: Exception) {
            Log.w(TAG, "historia szukania: zapis: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
