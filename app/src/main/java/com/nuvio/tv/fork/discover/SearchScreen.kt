package com.nuvio.tv.fork.discover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import com.nuvio.tv.fork.tv.ekrany.PendingPlay
import com.nuvio.tv.fork.tv.most.TvBridge
import com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/** Szukaj w telewizji (B08): kanały, teraz i wkrótce, archiwum. Dopasowanie po początku słów, od 2 liter. */
@Composable
fun TvSearchScreen() {
    val channels = remember { LiveTvRepository.uiState.value.channels }
    val index = remember {
        TvSearchIndex(
            channels.map { SearchChannel(it.guideKey, it.name, it.group) },
            TvBridge.data?.episodeLog().orEmpty().map { SearchAiring(it.programKey, it.title, it.guideKey, it.startMs, it.startMs + it.durMs) },
        )
    }
    var q by remember { mutableStateOf("") }
    val r = remember(q) { index.search(q, LiveTvClock.nowEpochMs()) }
    val first = remember { FocusRequester() }
    val firstId = r.channels.firstOrNull()?.key?.let { "c$it" } ?: r.nowAndSoon.firstOrNull()?.programKey?.let { "n$it" } ?: r.archive.firstOrNull()?.programKey?.let { "a$it" }
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        OutlinedTextField(
            value = q, onValueChange = { q = it }, label = { Text("Szukaj kanału lub programu") }, singleLine = true,
            // ▼ z pola przechodzi do pierwszego wyniku (domyślnie fokus zostawał w polu i wyników nie dało się wybrać pilotem)
            modifier = Modifier.onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown && firstId != null) { runCatching { first.requestFocus() }.isSuccess } else false },
        )
        LazyColumn {
            if (r.channels.isNotEmpty()) item { Text("Kanały", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            items(r.channels, key = { "c" + it.key }) { c ->
                Card(onClick = {
                    channels.firstOrNull { it.guideKey == c.key }?.let { PendingPlay.ask(it.streamUrl) }
                    ForkFeatures.open?.invoke(LIVE_TV_ROUTE)
                }, modifier = Modifier.padding(vertical = 4.dp).then(if (firstId == "c" + c.key) Modifier.focusRequester(first) else Modifier)) { Text(c.name, Modifier.padding(12.dp)) }
            }
            if (r.nowAndSoon.isNotEmpty()) item { Text("Teraz i wkrótce", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            items(r.nowAndSoon, key = { "n" + it.programKey }) { a -> programCard(a, if (firstId == "n" + a.programKey) Modifier.focusRequester(first) else Modifier) }
            if (r.archive.isNotEmpty()) item { Text("W archiwum", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            items(r.archive, key = { "a" + it.programKey }) { a -> programCard(a, if (firstId == "a" + a.programKey) Modifier.focusRequester(first) else Modifier) }
        }
    }
}

@Composable
private fun programCard(a: SearchAiring, extra: Modifier = Modifier) {
    Card(onClick = { ForkFeatures.open?.invoke("fork_program/" + android.net.Uri.encode(a.programKey)) }, modifier = Modifier.padding(vertical = 4.dp).then(extra)) {
        Text(a.title, Modifier.padding(12.dp))
    }
}
