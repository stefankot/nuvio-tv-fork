package com.nuvio.tv.fork.tv.ekrany

/** Filtry siatki kanałów (B03-3): Ulubione, Twoje kanały, Ostatnie, Wszystkie, potem grupy od największej. */
sealed interface ChannelFilter {
    object Favourites : ChannelFilter
    object Yours : ChannelFilter
    object Recent : ChannelFilter
    object All : ChannelFilter
    data class Group(val name: String) : ChannelFilter
}

data class FilterEntry(val filter: ChannelFilter, val title: String, val count: Int)

data class ChannelRef(val key: String, val group: String, val hasProgramme: Boolean)

object ChannelFilters {
    const val DEFAULT_FAVOURITES_MIN = 5

    /** Domyślny filtr: Ulubione przy co najmniej 5, inaczej Wszystkie. */
    fun default(favouriteCount: Int): ChannelFilter = if (favouriteCount >= DEFAULT_FAVOURITES_MIN) ChannelFilter.Favourites else ChannelFilter.All

    /** Lista panelu z licznikami; grupy ukryte i puste pomijane, od największej. */
    fun entries(favourites: Int, yours: Int, recent: Int, total: Int, groupCounts: Map<String, Int>, hidden: Set<String>): List<FilterEntry> = buildList {
        add(FilterEntry(ChannelFilter.Favourites, "Ulubione", favourites))
        add(FilterEntry(ChannelFilter.Yours, "Twoje kanały", yours))
        add(FilterEntry(ChannelFilter.Recent, "Ostatnie", recent))
        add(FilterEntry(ChannelFilter.All, "Wszystkie", total))
        groupCounts.filter { it.value > 0 && it.key !in hidden }.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .forEach { add(FilterEntry(ChannelFilter.Group(it.key), it.key, it.value)) }
    }

    /** Kolejność kanałów: najczęściej oglądane na górze (dla „Wszystkie”), kanały bez programu na końcu. */
    fun order(channels: List<ChannelRef>, usual: List<String>): List<ChannelRef> {
        val rank = usual.withIndex().associate { it.value to it.index }
        val sorted = channels.sortedBy { rank[it.key] ?: Int.MAX_VALUE }
        return sorted.filter { it.hasProgramme } + sorted.filterNot { it.hasProgramme }
    }
}
