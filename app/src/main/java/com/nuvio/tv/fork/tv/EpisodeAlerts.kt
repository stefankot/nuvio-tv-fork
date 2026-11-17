package com.nuvio.tv.fork.tv

import kotlinx.serialization.Serializable

/** One airing the guide lists. [guideKey] is the channel's guide key. */
@Serializable
data class Airing(
    val guideKey: String,
    val title: String,
    val startMs: Long,
    val stopMs: Long,
    val description: String? = null,
)

/** What the guide can say about a show's airing. */
enum class AlertKind {
    /** An episode the viewer has not opened, told from earlier ones by a marker or its own description. */
    NEW_EPISODE,

    /** The same, already aired, and the channel keeps it in its archive. */
    MISSED_IN_ARCHIVE,

    /** The guide cannot tell episodes apart for this show (same description every time): only "it is on again". */
    UPCOMING,
}

data class EpisodeAlert(
    /** Stable: the same episode (or, without episode info, the same airing) gives the same id, so it is announced once. */
    val id: String,
    val showKey: String,
    val showTitle: String,
    val kind: AlertKind,
    val airing: Airing,
)

/** What the detector needs to know about a channel. */
data class AlertChannel(val name: String, val catchupDays: Int)

object EpisodeAlerts {
    private const val HOUR = 3_600_000L
    private const val DAY = 24 * HOUR
    const val HORIZON_MS = 7 * DAY
    private const val MAX_PER_SHOW = 3

    /**
     * Alerts for [shows] from [airings]. A show's episodes count as told apart when its airings carry
     * markers or at least two different descriptions (or it has been watched with another one);
     * otherwise a repeat of one blurb is never called "new".
     */
    fun detect(
        shows: List<RecentShow>,
        airings: List<Airing>,
        channels: (String) -> AlertChannel?,
        nowMs: Long,
    ): List<EpisodeAlert> {
        if (shows.isEmpty() || airings.isEmpty()) return emptyList()
        val byShow = HashMap<String, MutableList<Airing>>()
        val wanted = shows.mapTo(HashSet()) { it.key }
        val keyOf = HashMap<String, String>()
        airings.forEach { airing ->
            val key = keyOf.getOrPut(airing.title) { ShowIdentity.normalizeTitle(airing.title) }
            if (key in wanted) byShow.getOrPut(key) { ArrayList() } += airing
        }
        val out = ArrayList<EpisodeAlert>()
        for (show in shows) {
            val own = byShow[show.key]?.distinctBy { it.guideKey to it.startMs }?.sortedBy { it.startMs } ?: continue
            val ids = own.map { ShowIdentity.episodeId(it.title, it.description) }
            val distinct = (ids.filter { it.isNotEmpty() } + show.seenEpisodes.filter { it.isNotEmpty() }).toSet()
            val told = ids.any { it.startsWith("s") || it.startsWith("e") } || distinct.size >= 2
            val seen = show.seenEpisodes.toHashSet()
            val announced = HashSet<String>()
            val perShow = ArrayList<EpisodeAlert>()
            for ((index, airing) in own.withIndex()) {
                val id = ids[index]
                val channel = channels(airing.guideKey)
                val future = airing.stopMs > nowMs && airing.startMs <= nowMs + HORIZON_MS
                val past = airing.stopMs <= nowMs
                if (told && id.isNotEmpty()) {
                    if (id in seen || !announced.add(id)) continue
                    val inArchive = past && channel != null && channel.catchupDays > 0 &&
                        airing.startMs >= nowMs - minOf(channel.catchupDays, 7) * DAY
                    when {
                        future -> perShow += EpisodeAlert("${show.key}|$id", show.key, show.title, AlertKind.NEW_EPISODE, airing)
                        inArchive -> perShow += EpisodeAlert("${show.key}|$id", show.key, show.title, AlertKind.MISSED_IN_ARCHIVE, airing)
                    }
                } else if (future && airing.startMs >= show.lastViewedMs && perShow.none { it.kind == AlertKind.UPCOMING }) {
                    // One "on again" per show: the nearest airing that is not the one being watched.
                    val watchingNow = airing.startMs <= show.lastViewedMs && airing.stopMs > show.lastViewedMs
                    if (!watchingNow) perShow += EpisodeAlert("${show.key}@${airing.startMs}", show.key, show.title, AlertKind.UPCOMING, airing)
                }
            }
            out += perShow.sortedBy { it.airing.startMs }.take(MAX_PER_SHOW)
        }
        return out.sortedBy { it.airing.startMs }
    }

    /** Alerts not announced yet, given the ids that were. */
    fun fresh(alerts: List<EpisodeAlert>, announced: Set<String>): List<EpisodeAlert> = alerts.filter { it.id !in announced }
}
