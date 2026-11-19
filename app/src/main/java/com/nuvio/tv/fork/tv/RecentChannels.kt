package com.nuvio.tv.fork.tv

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import com.nuvio.tv.fork.logged

/** A channel the viewer tuned to, as the TV tab's "recent channels" row keeps them. */
@Serializable
data class RecentChannel(
    val streamUrl: String,
    val name: String,
    val logoUrl: String? = null,
    val group: String = "",
    val tvgId: String? = null,
    val guideKey: String = "",
    val lastViewedMs: Long,
)

/** The last [MAX] channels watched, newest first, each once. */
object RecentChannels {
    const val MAX = 15
    private val json = Json { ignoreUnknownKeys = true }

    fun record(list: List<RecentChannel>, channel: RecentChannel): List<RecentChannel> =
        (listOf(channel) + list.filter { it.streamUrl != channel.streamUrl }).take(MAX)

    fun remove(list: List<RecentChannel>, streamUrl: String): List<RecentChannel> = list.filter { it.streamUrl != streamUrl }

    fun encode(list: List<RecentChannel>): String = json.encodeToString(ListSerializer(RecentChannel.serializer()), list)

    /** The saved list, or empty when the text is missing or damaged. */
    fun decode(text: String?): List<RecentChannel> {
        if (text.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(RecentChannel.serializer()), text) }.logged("ForkTv", "RecentChannels.kt:34")
            .getOrDefault(emptyList()).take(MAX)
    }
}
