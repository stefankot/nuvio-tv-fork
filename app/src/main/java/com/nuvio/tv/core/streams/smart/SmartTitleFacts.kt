package com.nuvio.tv.core.streams.smart

/**
 * forge: year and running time of titles the detail screen has already shown, so every place that picks a source
 * automatically reads the same facts under the same key. Memory only, at most [MAX_TITLES] titles (the oldest
 * goes first), no request and nothing on disk. No entry = null = UNKNOWN.
 */
object SmartTitleFacts {
    const val MAX_TITLES = 64

    /** [episodeDurationSec]: (season, episode) → seconds. A series' general running time is never used for an episode. */
    data class Facts(
        val year: Int? = null,
        val durationSec: Long? = null,
        val episodeDurationSec: Map<Pair<Int, Int>, Long> = emptyMap(),
    )

    private val titles = object : LinkedHashMap<String, Facts>(16, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Facts>?) = size > MAX_TITLES
    }

    /** Stores the facts under every id the title is known by (navigation id, canonical id). */
    @Synchronized
    fun remember(ids: Collection<String?>, facts: Facts) {
        ids.filterNotNull().filter { it.isNotBlank() }.distinct().forEach { id ->
            titles.remove(id)
            titles[id] = facts
        }
    }

    @Synchronized
    fun get(contentId: String?): Facts? = contentId?.let { titles[it] }

    /** The one place that builds [SmartExpected] for every automatic choice. */
    fun expected(contentId: String?, season: Int?, episode: Int?): SmartExpected {
        val facts = get(contentId)
        val duration = if (episode != null) {
            facts?.episodeDurationSec?.get((season ?: 0) to episode)
        } else facts?.durationSec
        return SmartExpected(season = season, episode = episode, year = facts?.year, durationSec = duration, contentId = contentId,
            family = SmartFamilyMemory.get(contentId)) // forge M7 (N14)
    }

    @Synchronized
    fun clear() = titles.clear()

    /**
     * forge fix 13.11 (JMGO): the facts of a title from its metadata, wherever that is read: the detail screen, and now
     * also the source screen (Home's „Odtwórz” skips the details, so the choice ran with no running time: the link budget
     * could not be applied and a 91 GB 2160p file won on a 69 Mb/s link). A series' general running time is never stored
     * for its episodes.
     */
    fun rememberMeta(ids: Collection<String?>, meta: com.nuvio.tv.domain.model.Meta) {
        val isMovie = meta.type == com.nuvio.tv.domain.model.ContentType.MOVIE
        remember(
            ids + meta.id,
            Facts(
                year = yearOf(meta.releaseInfo) ?: yearOf(meta.released),
                durationSec = if (isMovie) runtimeSec(meta.runtime) else null,
                episodeDurationSec = meta.videos.mapNotNull { video ->
                    val episode = video.episode ?: return@mapNotNull null
                    val minutes = video.runtime?.takeIf { it > 0 } ?: return@mapNotNull null
                    ((video.season ?: 0) to episode) to minutes * 60L
                }.toMap(),
            ),
        )
    }

    private val yearPattern = Regex("(?<![0-9])(19[0-9]{2}|20[0-9]{2})(?![0-9])")
    private val runtimePattern = Regex(
        "(?i)^\\s*(?:([0-9]{1,2})\\s?(?:h|hr|hrs|hours?|godz\\.?|g)\\s?)?(?:([0-9]{1,3})\\s?(?:m|min|mins|minutes?|minut)\\.?)?\\s*$"
    )
    private val bareMinutes = Regex("^\\s*([0-9]{1,3})\\s*$")

    /** First year 1900–2099 in a text such as "2019" or "2019–2023"; null when there is none. */
    fun yearOf(text: String?): Int? = text?.take(64)?.let { yearPattern.find(it)?.value?.toInt() }

    /** "2h 10min", "2h10m", "130 min", "130" (minutes) → seconds; anything else → null. */
    fun runtimeSec(text: String?): Long? {
        val t = text?.take(64)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        bareMinutes.matchEntire(t)?.let { return it.groupValues[1].toLong().takeIf { m -> m > 0 }?.times(60) }
        val match = runtimePattern.matchEntire(t) ?: return null
        val hours = match.groupValues[1].toLongOrNull()
        val minutes = match.groupValues[2].toLongOrNull()
        if (hours == null && minutes == null) return null
        val total = (hours ?: 0) * 60 + (minutes ?: 0)
        return if (total > 0) total * 60 else null
    }
}
