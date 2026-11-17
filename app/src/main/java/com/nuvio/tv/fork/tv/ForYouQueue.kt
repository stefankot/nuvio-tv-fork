package com.nuvio.tv.fork.tv

enum class Badge { RESUME, NEW_EPISODE, LIVE }

data class QueueItem(val programKey: String, val title: String, val guideKey: String, val startMs: Long, val badge: Badge)

/** Dane wejściowe kolejki „Dla Ciebie” (ekran dostarcza je z mostu). */
data class QueueInput(
    val inProgress: List<QueueItem>,
    val newEpisodes: List<QueueItem>,
    val liveOnFavourites: List<QueueItem>,
    val liveUsual: List<QueueItem>,
    /** Cokolwiek, co jest teraz na antenie, gdy reszta jest pusta. */
    val anyLive: List<QueueItem> = emptyList(),
)

object ForYouQueue {
    const val MAX = 12

    /** Do 12 pozycji: w trakcie, nowy odcinek obserwowanego, na żywo na ulubionych, na żywo wg pory dnia. Bez duplikatów programu. */
    fun build(input: QueueInput): List<QueueItem> {
        val seen = HashSet<String>()
        val out = ArrayList<QueueItem>(MAX)
        for (group in listOf(input.inProgress, input.newEpisodes, input.liveOnFavourites, input.liveUsual)) {
            for (it in group) if (out.size < MAX && seen.add(it.programKey)) out += it
        }
        if (out.isEmpty()) for (it in input.anyLive) if (out.size < MAX && seen.add(it.programKey)) out += it
        return out
    }
}
