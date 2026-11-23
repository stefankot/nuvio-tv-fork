package com.nuvio.tv.fork.voice

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

const val VOICE_HELP_ROUTE = "fork_voice_help"

/**
 * Lista przykładowych poleceń głosowych (B07-9): po jednym przykładzie na wzorzec.
 * UX 22.11: wiersze bez fokusu — pilotem nie dało się przewinąć listy dłuższej niż ekran; teraz każdy wiersz przyjmuje fokus
 * (pierwszy od razu), ▼▲ przewija.
 */
@Composable
fun VoiceHelpScreen() {
    val rows = remember { Patterns.all.map { it.examples.first() }.distinct() }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        Log.i("ForkVoice", "przykłady poleceń: ${rows.size}")
        try { first.requestFocus() } catch (e: IllegalStateException) { Log.w("ForkVoice", "przykłady poleceń: fokus: ${e.javaClass.simpleName}: ${e.message}") }
    }
    LazyColumn(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Text("Co mogę powiedzieć", style = MaterialTheme.typography.headlineMedium) }
        items(rows.size) { i ->
            var focused by remember { mutableStateOf(false) }
            Text(
                "„" + rows[i] + "”",
                modifier = Modifier.fillMaxWidth()
                    .then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                    .onFocusChanged { focused = it.isFocused }
                    .focusable()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (focused) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
