package com.nuvio.tv.fork.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

const val VOICE_HELP_ROUTE = "fork_voice_help"

/** Lista przykładowych poleceń głosowych (B07-9): po jednym przykładzie na wzorzec. */
@Composable
fun VoiceHelpScreen() {
    val rows = Patterns.all.map { it.examples.first() }.distinct()
    LazyColumn(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Text("Co mogę powiedzieć", style = MaterialTheme.typography.headlineMedium) }
        items(rows.size) { i -> Text("„" + rows[i] + "”") }
    }
}
