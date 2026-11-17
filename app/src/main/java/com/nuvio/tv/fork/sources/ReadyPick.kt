package com.nuvio.tv.fork.sources

import com.nuvio.tv.domain.model.AddonStreams
import com.nuvio.tv.domain.model.Stream

/** What the detail screen says about the best source found before Play is pressed. */
data class ReadyInfo(
    /** e.g. "4K", "1080p"; null when no addon says. */
    val quality: String?,
    /** Badge names the addon gave (HDR, Atmos, REMUX...), at most [ReadyPick.MAX_TAGS]. */
    val tags: List<String>,
    val sizeBytes: Long?,
    /** True when the stream plays right away: a direct link, or a debrid file that is cached. */
    val ready: Boolean,
)

object ReadyPick {
    const val MAX_TAGS = 4

    /** The stream Play would most likely take: playable now first, then quality, then size. */
    fun best(groups: List<AddonStreams>): Stream? {
        val all = groups.flatMap { it.streams }.filter { it.ytId == null && !it.isExternalOnly() }
        if (all.isEmpty()) return null
        return all.maxWithOrNull(
            compareBy<Stream>({ if (it.playsNow()) 1 else 0 }, { it.qualityValue }, { it.behaviorHints?.videoSize ?: 0L }),
        )
    }

    fun info(groups: List<AddonStreams>): ReadyInfo? {
        val stream = best(groups) ?: return null
        return ReadyInfo(
            quality = stream.quality?.takeIf { it.isNotBlank() },
            tags = stream.badges.map { it.name.trim() }.filter { it.isNotEmpty() }.distinct().take(MAX_TAGS),
            sizeBytes = stream.behaviorHints?.videoSize?.takeIf { it > 0 } ?: stream.debridCacheStatus?.cachedSize?.takeIf { it > 0 },
            ready = stream.playsNow(),
        )
    }

    private fun Stream.playsNow(): Boolean = (getStreamUrl() != null) || isDirectDebrid()
    private fun Stream.isExternalOnly(): Boolean = isExternal()

    /** "70,8 GB" / "850 MB" with the device's decimal comma left to the caller's locale. */
    fun sizeText(bytes: Long, locale: java.util.Locale = java.util.Locale.getDefault()): String {
        val gb = bytes / 1_073_741_824.0
        return if (gb >= 1.0) String.format(locale, "%.1f GB", gb) else String.format(locale, "%.0f MB", bytes / 1_048_576.0)
    }
}
