package com.nuvio.tv.fork.discover

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import androidx.tv.material3.Card
import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ModuleHomeRow
import com.nuvio.tv.fork.tv.ChannelStats
import com.nuvio.tv.fork.tv.most.TvBridge
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.delay

/** Odkrywaj (B08): rzędy z dziennika odcinków i oglądania na ekranie głównym. Wyłączone, gdy brak danych TV. */
object DiscoverModule : ForkModule {
    override val id = "discover"
    override val title = "Odkrywaj telewizję (rzędy na ekranie głównym)"
    override val defaultEnabled = true


    fun build(context: android.content.Context): List<DiscoverRow> {
        val data = TvBridge.data ?: return emptyList()
        val now = LiveTvClock.nowEpochMs()
        val genreText = runCatching { context.assets.open("fork/genres.json").bufferedReader().use { it.readText() } }.getOrDefault("{}")
        val buckets = GenreBuckets(genreText)
        val state = LiveTvRepository.uiState.value
        val groupOf = state.channels.associate { it.guideKey to it.group }
        val catchup = state.channels.filter { it.catchup != null }.associate { it.guideKey to 7 }
        val input = DiscoverInput(
            // Audyt UI p. 4: cały przewodnik, nie tylko archiwum (rzędy archiwum i tak filtrują po dniach archiwum kanału).
            log = (data.episodeLog() + TvBridge.guideAirings()).distinctBy { it.guideKey to it.startMs }, records = data.records(), followed = data.followedList(),
            yourChannels = ChannelStats.yourChannels(data.sessions(), now).toSet(), catchupDays = catchup,
            bucketOf = { e -> buckets.bucketOf(emptyList(), groupOf[e.guideKey]) }, nowMs = now,
        )
        val hidden = DiscoverRows.DEFAULT_ORDER.filterNot { ForkFeatures.enabled(context, "discover_show_$it", true) }.toSet()
        return DiscoverRows.build(input, hiddenRows = hidden)
    }

    private val rowTitles = mapOf("continue" to "Oglądaj dalej", "followed" to "Obserwowane", "tonight" to "Dziś wieczorem", "films" to "Filmy w archiwum", "series" to "Seriale w archiwum")

    override fun settings() = DiscoverRows.DEFAULT_ORDER.map { id ->
        com.nuvio.tv.fork.registry.ModuleSetting("discover_show_$id", com.nuvio.tv.fork.registry.SettingsLevel.ESSENTIAL, "Odkrywaj", "Rząd: " + (rowTitles[id] ?: id.removePrefix("genre_")), "", defaultValue = true)
    }

    override fun routes() = listOf(
        com.nuvio.tv.fork.registry.ModuleRoute("fork_tv_search") { composable("fork_tv_search") { TvSearchScreen() } },
        com.nuvio.tv.fork.registry.ModuleRoute("fork_discover") {
            composable("fork_discover") {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().padding(48.dp)) { item { Rows() } }
            }
        },
    )

    override fun actions() = listOf(
        com.nuvio.tv.fork.registry.ModuleAction("Odkrywaj telewizję (rzędy)", com.nuvio.tv.fork.registry.SettingsLevel.SIMPLE, "Co oglądać: wznów, nowe odcinki, gatunki z archiwum") { ForkFeatures.open?.invoke("fork_discover") },
    )

    override fun homeRows() = listOf(ModuleHomeRow("discover_rows", "Odkrywaj", order = 60) { Rows() })

    @Composable
    private fun Rows() {
        val c = LocalContext.current
        var rows by remember { mutableStateOf<List<DiscoverRow>>(emptyList()) }
        LaunchedEffect(Unit) { while (true) { rows = runCatching { build(c) }.getOrDefault(emptyList()); delay(120_000L) } }
        Card(onClick = { ForkFeatures.open?.invoke("fork_tv_search") }, modifier = Modifier.padding(bottom = 8.dp).width(240.dp)) {
            Text("Szukaj w telewizji", Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall)
        }
        rows.forEach { row ->
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(row.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 24.dp)) {
                    // Właściciel 19.11: „Odkrywaj” zamykało aplikację — ten sam program o tej samej godzinie na dwóch kanałach dawał zdublowany klucz.
                    itemsIndexed(row.cards, key = { i, it -> "$i|" + it.programKey + "|" + it.startMs }) { _, card ->
                        Card(onClick = { ForkFeatures.open?.invoke("fork_program/" + android.net.Uri.encode(card.programKey)) }, modifier = Modifier.width(200.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                card.badge?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                                Text(card.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}
