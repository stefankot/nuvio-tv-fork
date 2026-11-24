package com.nuvio.tv.skin.niunia.livetv

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf

/**
 * Z-21 (P-28, makieta LiveTV: `NEpgRow status`): „Niedostępny” pod nazwą kanału przez 24 godziny po oknie „… nie odpowiada”
 * (strażnik startu kanału: dwie nieudane próby). Kanał, który potem ruszył (obraz gra kilka sekund), traci znacznik od razu.
 * Zapis na tym TV (SharedPreferences: skrót adresu → czas). „Źródło zniknęło” z makiety pominięte: fork nie pamięta
 * kanałów usuniętych z playlisty.
 */
internal object NiuniaLiveStatus {
    private const val TAG = "ForkNiunia"
    private const val PREFS = "niunia_live_status"
    private const val VALID_MS = 24L * 3600 * 1000
    /** Zmienia się przy każdym wpisie — wiersze przewodnika czytają ją, żeby odświeżyć znacznik. */
    val version = mutableIntStateOf(0)
    @Volatile private var map: Map<String, Long>? = null

    private fun key(url: String) = Integer.toHexString(url.hashCode()) + ":" + url.length

    private fun load(c: Context): Map<String, Long> = map ?: c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
        .mapNotNull { (k, v) -> (v as? Long)?.let { k to it } }.toMap().also { map = it }

    fun failed(c: Context, url: String, name: String) {
        val now = System.currentTimeMillis()
        val next = load(c).filterValues { now - it < VALID_MS } + (key(url) to now)
        map = next
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply { next.forEach { (k, v) -> putLong(k, v) } }.apply()
        version.intValue++
        Log.i(TAG, "live tv: kanał $name oznaczony „Niedostępny” na 24 h")
    }

    fun ok(c: Context, url: String, name: String) {
        val m = load(c)
        if (!m.containsKey(key(url))) return
        map = m - key(url)
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key(url)).apply()
        version.intValue++
        Log.i(TAG, "live tv: kanał $name znów działa — znacznik „Niedostępny” zdjęty")
    }

    fun unavailable(c: Context, url: String): Boolean = load(c)[key(url)]?.let { System.currentTimeMillis() - it < VALID_MS } == true
}
