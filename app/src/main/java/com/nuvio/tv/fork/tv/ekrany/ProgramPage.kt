@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.fork.tv.ekrany

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.fork.tv.ProgramType
import com.nuvio.tv.fork.tv.ProgramTypes
import com.nuvio.tv.fork.tv.WatchState
import com.nuvio.tv.fork.tv.most.TvBridge

/** Strona programu (B03-5): tytuł, typ, przyciski i odcinki z dziennika (znacznik, postęp, dostępność). */
@Composable
fun ProgramPage(programKey: String) {
    val data = TvBridge.data
    val now = System.currentTimeMillis()
    val log = remember(programKey) { data?.episodeLog().orEmpty().filter { it.programKey == programKey } }
    val records = remember(programKey) { data?.records().orEmpty().filter { it.programKey == programKey }.associateBy { it.key } }
    var followed by remember { mutableStateOf(data?.followedList().orEmpty().any { it.programKey == programKey }) }
    val type = ProgramTypes.classify(log, now)
    val title = log.firstOrNull()?.title ?: programKey
    val catchup = log.associate { it.guideKey to 7 }
    val rows = episodeRows(log, records, catchup, now)
    LazyColumn(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(title, style = MaterialTheme.typography.headlineMedium) }
        item { Text(typeLabel(type), style = MaterialTheme.typography.bodyMedium) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    if (followed) data?.unfollow(programKey) else data?.follow(programKey, title, System.currentTimeMillis())
                    followed = !followed
                }) { Text(if (followed) "Przestań obserwować" else "Obserwuj") }
            }
        }
        if (type == ProgramType.BAND) {
            item { Text("Najbliższe emisje", style = MaterialTheme.typography.titleMedium) }
            items(log.filter { it.startMs >= now }.sortedBy { it.startMs }.take(5)) { Text(dayLabel(it.startMs, now) + " " + com.nuvio.tv.fork.tv.ekrany.MiniGuideLogic.time(it.startMs)) }
        } else {
            val bySeason = groupBySeason(log)
            bySeason.forEach { (season, eps) ->
                item { Text(season?.let { "Sezon $it" } ?: "Odcinki", style = MaterialTheme.typography.titleMedium) }
                items(eps) { e ->
                    val r = rows.firstOrNull { it.entry == e }
                    val mark = when (r?.state) { WatchState.WATCHED -> "✓ "; WatchState.IN_PROGRESS -> "▶ "; else -> "" }
                    val av = when (r?.availability) { Availability.LIVE -> "na żywo"; Availability.IN_ARCHIVE -> "w archiwum"; Availability.FUTURE -> dayLabel(e.startMs, now); else -> "niedostępny" }
                    Text(mark + (episodeLabel(e.episodeId) ?: dayLabel(e.startMs, now)) + " · " + av)
                }
            }
        }
    }
}

private fun typeLabel(t: ProgramType) = when (t) {
    ProgramType.SERIES -> "Serial"; ProgramType.FILM -> "Film"; ProgramType.BAND -> "Pasmo"; ProgramType.ONE_OFF -> "Program"
}
