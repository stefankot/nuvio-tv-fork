package com.nuvio.tv.fork.tv.guide

/**
 * Właściciel 19.11: kolejność danych o programie: TVDB → TMDB → przewodnik epg.ovh → przewodnik tv.team.
 * Tu część przewodników: gdy kanał jest w kilku przewodnikach, program bierze się z tego wyżej na liście
 * (przed regułami Reshaped: przewodnik dodany ręcznie, kolejność z playlisty). Dopasowanie po identyfikatorze
 * nadal wygrywa z dopasowaniem po samej nazwie. tv.team podaje przewodnik pod adresem epg.team.
 */
object GuidePriority {
    private val ORDER = listOf(
        listOf("epg.ovh"),
        listOf("tv.team", "epg.team"),
    )

    /** Miejsce przewodnika na liście właściciela (0 = najwyżej) albo null, gdy go na niej nie ma. */
    fun rank(url: String): Int? {
        val host = runCatching { java.net.URI(url.trim()).host }.getOrNull()?.lowercase() ?: return null
        val i = ORDER.indexOfFirst { names -> names.any { host == it || host.endsWith(".$it") } }
        return i.takeIf { it >= 0 }
    }

    /** Ranga dla LiveTvRepository: przewodniki z listy przed wszystkimi innymi; [nameRank] = ranga dopasowania po nazwie. */
    fun repoRank(url: String, nameMatched: Boolean, nameRank: Int): Int? =
        rank(url)?.let { if (nameMatched) nameRank - 100 + it else -100 + it }
}
