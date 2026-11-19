package com.nuvio.tv.fork.tv

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.nuvio.tv.fork.logged

/**
 * A show the viewer chose to watch on TV, as Freely's "Recently Viewed" keeps them: the last
 * [RecentlyViewed.MAX], newest first, each removable. [seenEpisodes] are the episodes of it the
 * viewer has opened, so only later ones are announced.
 */
@Serializable
data class RecentShow(
    /** [ShowIdentity.normalizeTitle] of the title. */
    val key: String,
    val title: String,
    /** The channel it was last watched on, to open it again. */
    val channelName: String,
    val channelUrl: String,
    val guideKey: String,
    val logoUrl: String? = null,
    /** The guide's picture for the programme, when it had one; never a guess. */
    val imageUrl: String? = null,
    val lastViewedMs: Long,
    val seenEpisodes: List<String> = emptyList(),
)

object RecentlyViewed {
    const val MAX = 15
    private const val MAX_SEEN = 60
    private val json = Json { ignoreUnknownKeys = true }

    /** [show] first (merging what was seen of it before), the rest after it, [MAX] at most. */
    fun record(list: List<RecentShow>, show: RecentShow): List<RecentShow> {
        val before = list.firstOrNull { it.key == show.key }
        val merged = if (before == null) {
            show.copy(seenEpisodes = show.seenEpisodes.distinct().takeLast(MAX_SEEN))
        } else {
            show.copy(seenEpisodes = (before.seenEpisodes + show.seenEpisodes).distinct().takeLast(MAX_SEEN))
        }
        return (listOf(merged) + list.filter { it.key != show.key }).take(MAX)
    }

    fun remove(list: List<RecentShow>, key: String): List<RecentShow> = list.filter { it.key != key }

    fun encode(list: List<RecentShow>): String = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(RecentShow.serializer()), list)

    /** The saved list, or empty when the text is missing or damaged. */
    fun decode(text: String?): List<RecentShow> {
        if (text.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(RecentShow.serializer()), text)
        }.logged("ForkTv", "RecentlyViewed.kt:50").getOrDefault(emptyList()).take(MAX)
    }
}
