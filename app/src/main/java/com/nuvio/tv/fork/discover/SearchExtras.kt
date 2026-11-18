package com.nuvio.tv.fork.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.fork.tv.ekrany.PendingPlay
import com.nuvio.tv.fork.tv.most.TvBridge
import com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Fork 19.11 (właściciel: „search nie szuka w YouTube i TV”): pod wynikami katalogu w ekranie Szukaj
 * półka „Telewizja” (kanały i programy z przewodnika, [TvSearchIndex]) i karta „YouTube”, która otwiera
 * YouTube od razu z wynikami tego zapytania. Pusta fraza = nic.
 */
@Composable
fun SearchExtras(query: String, modifier: Modifier = Modifier) {
    val q = query.trim()
    if (q.length < 2) return
    val c = LocalContext.current
    val channels = LiveTvRepository.uiState.value.channels
    val index = remember(channels.size) {
        TvSearchIndex(
            channels.map { SearchChannel(it.guideKey, it.name, it.group) },
            (TvBridge.data?.episodeLog().orEmpty() + TvBridge.guideAirings()).distinctBy { it.guideKey to it.startMs }
                .map { SearchAiring(it.programKey, it.title, it.guideKey, it.startMs, it.startMs + it.durMs) },
        )
    }
    val r = remember(q, index) { runCatching { index.search(q, LiveTvClock.nowEpochMs()) }.getOrNull() }
    val names = remember(channels) { channels.associate { it.guideKey to it.name } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val tvCards = buildList<Triple<String, String, () -> Unit>> {
            r?.channels?.take(8)?.forEach { ch -> add(Triple("Kanał", ch.name) {
                channels.firstOrNull { it.guideKey == ch.key }?.let { PendingPlay.ask(it.streamUrl) }
                ForkFeatures.open?.invoke(LIVE_TV_ROUTE); Unit
            }) }
            (r?.nowAndSoon.orEmpty().take(10) + r?.archive.orEmpty().take(10)).forEach { a -> add(Triple(names[a.channelKey] ?: "Program", a.title) {
                ForkFeatures.open?.invoke("fork_program/" + android.net.Uri.encode(a.programKey)); Unit
            }) }
        }
        if (tvCards.isNotEmpty()) Column {
            Text("Telewizja", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 58.dp)) {
                itemsIndexed(tvCards, key = { i, it -> "tv$i|" + it.second }) { _, (label, title, open) ->
                    Card(onClick = open, modifier = Modifier.width(220.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                        }
                    }
                }
            }
        }
        if (ForkFeatures.enabled(c, "fork_youtube", true)) Column {
            Text("YouTube", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
            Card(onClick = {
                com.nuvio.tv.skin.kicia.youtube.ForgeYouTubeLaunch.askSearch(q)
                ForkFeatures.open?.invoke(com.nuvio.tv.ui.navigation.Screen.YouTube.route)
            }, modifier = Modifier.width(320.dp)) {
                Text("Szukaj „$q” w YouTube", Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall, maxLines = 2)
            }
        }
    }
}
