package com.nuvio.tv.fork.tv.most

import com.nuvio.tv.fork.tv.Badge
import com.nuvio.tv.fork.tv.ChannelStats
import com.nuvio.tv.fork.tv.DayPart
import com.nuvio.tv.fork.tv.ForYouQueue
import com.nuvio.tv.fork.tv.QueueInput
import com.nuvio.tv.fork.tv.QueueItem
import com.nuvio.tv.fork.tv.TvStore
import com.nuvio.tv.fork.tv.WatchState
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository

/** Pozycja kolejki z rozwiązanym kanałem i emisją (do odtworzenia). */
data class ResolvedItem(val item: QueueItem, val channel: LiveTvChannel, val programme: LiveTvProgramme?)

/** Buduje kolejkę „Dla Ciebie” ze stanu Reshaped i danych modułu TV. */
object TvQueue {
    fun build(): List<ResolvedItem> {
        val live = LiveTvRepository.uiState.value
        val data = TvBridge.data ?: return emptyList()
        val now = LiveTvClock.nowEpochMs()
        // 23.11 (TvRam): telewizja poza jej ekranem nie jest w pamięci — „Dla Ciebie” z próbki startowej.
        val snap = if (live.channels.isEmpty() && com.nuvio.tv.fork.tv.ram.TvRam.enabled) com.nuvio.tv.fork.tv.ram.TvSnapshot.data else null
        val state = if (snap == null) live else live.copy(channels = snap.channels, currentProgrammes = snap.currentProgrammes(now), favoriteUrls = live.favoriteUrls.ifEmpty { snap.favoriteUrls })
        val scheduleOf: (String) -> List<LiveTvProgramme> = { key -> snap?.programmes?.get(key) ?: LiveTvRepository.schedule(key) }
        val byGuide = LinkedHashMap<String, LiveTvChannel>()
        state.channels.forEach { byGuide.putIfAbsent(it.guideKey, it) }
        val onNow = state.currentProgrammes
        val log = data.episodeLog()
        val titleOf = log.associate { it.programKey to it.title }

        fun live(c: LiveTvChannel): QueueItem? = onNow[c.guideKey]?.let { QueueItem(com.nuvio.tv.fork.tv.ShowIdentity.normalizeTitle(it.title).ifEmpty { c.guideKey }, it.title, c.guideKey, it.startEpochMs, Badge.LIVE) }

        val inProgress = data.records().filter { it.state == WatchState.IN_PROGRESS }.sortedByDescending { it.lastWatchedMs }
            .map { QueueItem(it.programKey, titleOf[it.programKey] ?: it.programKey, it.guideKey, it.startMs, Badge.RESUME) }
        val alerts = TvStore.alerts.value.map { QueueItem(it.showKey, it.showTitle, it.airing.guideKey, it.airing.startMs, Badge.NEW_EPISODE) }
        val favourites = state.channels.filter { it.streamUrl in state.favoriteUrls }.mapNotNull(::live)
        val usual = ChannelStats.usualForDayPart(data.sessions(), DayPart.of(now), now).mapNotNull { byGuide[it] }.mapNotNull(::live)
        val any = byGuide.values.mapNotNull(::live).take(ForYouQueue.MAX)

        return ForYouQueue.build(QueueInput(inProgress, alerts, favourites, usual, any)).mapNotNull { item ->
            val channel = byGuide[item.guideKey] ?: return@mapNotNull null
            val programme = if (item.badge == Badge.LIVE) onNow[item.guideKey]
            else scheduleOf(item.guideKey).firstOrNull { it.startEpochMs == item.startMs }
            ResolvedItem(item, channel, programme)
        }
    }
}
