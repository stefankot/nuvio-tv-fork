package com.nuvio.tv.skin.niunia.livetv

import android.util.Log

/**
 * Paczka „oczywiste” pkt 2 (21.11): Wstecz z odtwarzacza IPTV wraca do Live TV z fokusem na ostatnio oglądanym programie —
 * na kafelku nad przewodnikiem albo na kanale w przewodniku, zależnie od tego, skąd program został włączony.
 * Pamięć w procesie (ekran Live TV powstaje od nowa po powrocie z odtwarzacza); ważna [VALID_MS].
 */
internal object NiuniaLiveReturn {
    private const val TAG = "ForkNiunia"
    private const val VALID_MS = 6L * 60 * 60 * 1000

    sealed class Entry {
        data class Tile(val key: String) : Entry()
        data class Guide(val streamUrl: String, val startMs: Long?) : Entry()
    }

    private var pending: Entry? = null
    private var entry: Entry? = null
    var filter: NiuniaLiveFilter? = null
        private set
    var dayOffset: Int = 0
        private set
    private var savedAt = 0L

    /** Kafel nad przewodnikiem ([key] = [NiuniaLiveTile.key]). */
    fun fromTile(key: String) { pending = Entry.Tile(key) }

    /** Komórka programu w przewodniku. */
    fun fromGuide(streamUrl: String, startMs: Long?) { pending = Entry.Guide(streamUrl, startMs) }

    /** Szukaj, Odkrywaj: bez powrotu na konkretne miejsce. */
    fun fromOther() { pending = null }

    /** Tuż przed przejściem do odtwarzacza. */
    fun commit(filter: NiuniaLiveFilter, dayOffset: Int) {
        entry = pending
        pending = null
        this.filter = filter
        this.dayOffset = dayOffset
        savedAt = System.currentTimeMillis()
        Log.i(TAG, "live tv: zapamiętany powrót $entry (dzień $dayOffset)")
    }

    /** Miejsce powrotu (raz) — null, gdy brak albo przeterminowane. */
    fun consume(): Entry? {
        val e = entry ?: return null
        entry = null
        if (System.currentTimeMillis() - savedAt > VALID_MS) { Log.i(TAG, "live tv: powrót przeterminowany"); return null }
        return e
    }

    /** Filtr i dzień z ostatniego wyjścia do odtwarzacza (żeby lista kanałów była ta sama). */
    fun restoredFilter(): NiuniaLiveFilter? = filter?.takeIf { System.currentTimeMillis() - savedAt <= VALID_MS && entry != null }
    fun restoredDay(): Int = if (entry != null && System.currentTimeMillis() - savedAt <= VALID_MS) dayOffset else 0
}
