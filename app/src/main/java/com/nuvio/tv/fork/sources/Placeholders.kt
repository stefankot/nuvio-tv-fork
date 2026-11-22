package com.nuvio.tv.fork.sources

import android.util.Log
import com.nuvio.tv.domain.model.Stream
import java.util.concurrent.ConcurrentHashMap

/**
 * Zgłoszenie 21.11 (JMGO build161o, „Godzilla Minus Zero”, „Godzilla II”): TorBox/StremThru/Comet zamiast filmu podają
 * krótki klip „not cached yet” (17 KB–kilka MB, 30 s). Wyścig źródeł brał go za działający (przyszły bajty), odtwarzacz
 * wykrywał złą długość dopiero po starcie, a odzyskiwanie po 3 klipach się poddawało. Teraz: plik mniejszy niż [MIN_FILM_BYTES]
 * przegrywa wyścig od razu, a każdy rozpoznany klip (rozmiar, zła długość) spada na koniec kolejności źródeł.
 * Wyłączone ([KEY]) = jak dawniej.
 */
object Placeholders {
    const val KEY = "sources_skip_placeholders"
    private const val TAG = "ForkSources"
    /** Najmniejszy prawdziwy film/odcinek: ~80 MB (klipy „not cached” mają do kilku MB). */
    private const val MIN_FILM_BYTES = 80L * 1024 * 1024
    private val marked = ConcurrentHashMap.newKeySet<String>()

    private fun on(): Boolean = PlayerBridge.appContext?.let { com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(it, KEY, true) } ?: true

    /** Wyścig: plik o znanym rozmiarze poniżej progu = klip zastępczy (powód przegranej), inaczej null. */
    fun tooSmall(total: Long?, url: String): String? {
        if (total == null || total >= MIN_FILM_BYTES || !on()) return null
        mark(url, "rozmiar ${total / 1024} KB")
        return "only ${total / 1024} KB in all (a placeholder clip, e.g. not cached yet)"
    }

    fun mark(url: String?, why: String) {
        if (url == null || !on()) return
        if (marked.add(url)) Log.i(TAG, "klip zastępczy ($why), link=" + url.hashCode().toUInt().toString(16))
    }

    fun isMarked(url: String?): Boolean = url != null && url in marked && on()

    /** Wyścig bez zwycięzcy: pierwsze źródło, które nie jest znanym klipem (null = wszystkie są). */
    fun firstReal(candidates: List<Stream>): Stream? = if (!on()) null else candidates.firstOrNull { !isMarked(it.getStreamUrl()) }

    /** Kolejność źródeł: znane klipy na koniec. */
    fun demote(list: List<Stream>): List<Stream> {
        if (marked.isEmpty() || !on()) return list
        val (bad, good) = list.partition { isMarked(it.getStreamUrl()) }
        if (bad.isNotEmpty()) Log.i(TAG, "klipy zastępcze na koniec: ${bad.size} z ${list.size}")
        return good + bad
    }
}
