package com.nuvio.tv.fork.sources

import android.util.Log
import com.nuvio.tv.core.stream.ReleaseFactsParser
import com.nuvio.tv.domain.model.ReleaseResolution
import com.nuvio.tv.domain.model.Stream

/**
 * Fork 19.11 (zaległe z testów JMGO): źródło „MediaFusion ElfHosted TRB 2160p” wygrało (#0), a pośrednik ElfHosted
 * daje ~6 Mbit/s — za mało na 4K. Ranking nie zna szybkości tej usługi (brak pomiaru KE), więc przy 4K pośrednik
 * idzie tuż za ostatnie bezpośrednie źródło 4K, które nie jest gorsze w kluczach jakości języka/gotowości
 * ([rank]: mniejszy = lepszy). Reszta kolejności bez zmian; przy 1080p i niżej nic nie zmieniamy.
 */
internal object SlowProxy4k {
    private const val TAG = "ForkSources"
    private val SLOW_HOSTS = listOf("elfhosted")

    /** Klucze porównywane po kolei (mniejszy = lepszy): czy [a] nie jest gorszy od [b]. */
    private fun notWorse(a: List<Int>, b: List<Int>): Boolean {
        for (k in a.indices) { if (a[k] != b.getOrElse(k) { 0 }) return a[k] < b.getOrElse(k) { 0 } }
        return true
    }

    fun isSlowProxy(s: Stream): Boolean {
        val where = listOfNotNull(s.url, s.addonName).joinToString(" ").lowercase()
        return SLOW_HOSTS.any { it in where }
    }

    fun demote(list: List<Stream>, rank: (Stream) -> List<Int>): List<Stream> {
        if (list.size < 2) return list
        val is4k = list.map { ReleaseFactsParser.factsFor(it).resolution == ReleaseResolution.P2160 }
        val slow = list.indices.filter { is4k[it] && isSlowProxy(list[it]) }
        if (slow.isEmpty()) return list
        val keys = HashMap<Int, List<Int>>()
        fun key(i: Int) = keys.getOrPut(i) { rank(list[i]) }
        val out = list.toMutableList()
        var moved = 0
        // Od końca: przeniesienie późniejszych nie zmienia indeksów wcześniejszych.
        for (i in slow.asReversed()) {
            val target = (list.indices.last downTo i + 1).firstOrNull { j ->
                is4k[j] && !isSlowProxy(list[j]) && notWorse(key(j), key(i))
            } ?: continue
            val s = list[i]
            out.remove(s)
            out.add(out.indexOf(list[target]) + 1, s)
            moved++
        }
        if (moved > 0) Log.i(TAG, "wolny pośrednik 4K za bezpośrednim: przeniesione $moved")
        return out
    }
}
