package com.nuvio.tv.fork.sources

import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamDebridCacheState

/**
 * Fork 19.11 (JMGO: „Spróbuj innego źródła” wybrało „Comet 1080p [TB⬇️]” — pliku nie było w pamięci TorBox, serwis
 * podał 2-minutowy klip zastępczy): źródło debrid bez gotowego pliku nie może wygrać z gotowym.
 * 1 = niegotowe (stan NOT_CACHED albo znak pobierania w nazwie: ⬇ ⏳ „download”, „uncached”, Torrentio „[TB download]”).
 */
object SourceReady {
    private val NOT_READY = Regex("""(?i)⬇|⏳|\buncached\b|\bnot[ _-]?cached\b|\[(?:tb|rd|ad|pm|dl|ed|oc)\s*download\]|\bdownload\b""")

    fun tier(s: Stream): Int {
        // JMGO 19.11 (build98): goły Torrentio (P2P, bez debrid) wygrał z [TB+] i wymagał zgody na P2P. P2P = jak niegotowe.
        if (s.isTorrent()) return 1
        if (s.debridCacheStatus?.state == StreamDebridCacheState.NOT_CACHED) return 1
        if (s.debridCacheStatus?.state == StreamDebridCacheState.CACHED) return 0
        val head = listOfNotNull(s.name, s.title).joinToString(" ")
        return if (NOT_READY.containsMatchIn(head)) 1 else 0
    }
}
