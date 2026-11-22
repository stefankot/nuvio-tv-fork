package com.nuvio.tv.fork.discover

import androidx.compose.foundation.lazy.itemsIndexed
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
    val index = rememberTvSearchIndex(channels.size).value
    var q by remember { mutableStateOf("") }
    val r = remember(q, index) { index?.search(q, LiveTvClock.nowEpochMs()) ?: SearchResults(emptyList(), emptyList(), emptyList()) }
    val first = remember { FocusRequester() }
    // UX 22.11 (test emulatora): po otwarciu nic nie miało fokusu — pierwszy klawisz pilota był „zjadany”.
    val field = remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos { }
        runCatching { field.requestFocus() }.onFailure { android.util.Log.w("ForkTvSearch", "fokus pola: ${it.javaClass.simpleName}: ${it.message}") }
    }
    val firstId = r.channels.firstOrNull()?.key?.let { "c$it" } ?: r.nowAndSoon.firstOrNull()?.programKey?.let { "n$it" } ?: r.archive.firstOrNull()?.programKey?.let { "a$it" }
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        OutlinedTextField(
            value = q, onValueChange = { q = it }, label = { Text("Szukaj kanału lub programu") }, singleLine = true,
            // ▼ z pola przechodzi do pierwszego wyniku (domyślnie fokus zostawał w polu i wyników nie dało się wybrać pilotem)
            modifier = Modifier.focusRequester(field).onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown && firstId != null) { runCatching { first.requestFocus() }.isSuccess } else false },
        )
        if (channels.isEmpty()) Text(androidx.compose.ui.res.stringResource(com.nuvio.tv.R.string.niunia_live_no_source), modifier = Modifier.padding(top = 12.dp))
        LazyColumn {
            if (r.channels.isNotEmpty()) item { Text("Kanały", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            itemsIndexed(r.channels, key = { i, it -> "c$i|" + it.key }) { _, c ->
                Card(onClick = {
                    channels.firstOrNull { it.guideKey == c.key }?.let { PendingPlay.ask(it.streamUrl) }
                    ForkFeatures.open?.invoke(LIVE_TV_ROUTE)
                }, modifier = Modifier.padding(vertical = 4.dp).then(if (firstId == "c" + c.key) Modifier.focusRequester(first) else Modifier)) { Text(c.name, Modifier.padding(12.dp)) }
            }
            if (r.nowAndSoon.isNotEmpty()) item { Text("Teraz i wkrótce", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            itemsIndexed(r.nowAndSoon, key = { i, it -> "n$i|" + it.programKey }) { _, a -> programCard(a, if (firstId == "n" + a.programKey) Modifier.focusRequester(first) else Modifier) }
            if (r.archive.isNotEmpty()) item { Text("W archiwum", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
            itemsIndexed(r.archive, key = { i, it -> "a$i|" + it.programKey }) { _, a -> programCard(a, if (firstId == "a" + a.programKey) Modifier.focusRequester(first) else Modifier) }
        }
    }
}

@Composable
private fun programCard(a: SearchAiring, extra: Modifier = Modifier) {
    Card(onClick = { ForkFeatures.open?.invoke("fork_program/" + android.net.Uri.encode(a.programKey)) }, modifier = Modifier.padding(vertical = 4.dp).then(extra)) {
        Text(a.title, Modifier.padding(12.dp))
    }
}
