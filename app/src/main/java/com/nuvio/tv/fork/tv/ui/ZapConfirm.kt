@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.fork.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/** Potwierdzenie zmiany kanału podczas oglądania programu z archiwum. Bezpieczna odpowiedź („Zostań”) ma fokus. */
@Composable
fun ForkZapConfirm(programmeTitle: String?, onStay: () -> Unit, onChange: () -> Unit) {
    val stay = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { stay.requestFocus() } }
    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(Modifier.width(520.dp)) {
                Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Zmienić kanał?", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Oglądasz program z archiwum" + (programmeTitle?.let { ": $it" } ?: "") +
                            ". Po zmianie kanału wrócisz do niego przez przewodnik.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onStay, modifier = Modifier.focusRequester(stay)) { Text("Zostań") }
                        Button(onClick = onChange) { Text("Zmień kanał") }
                    }
                }
            }
        }
    }
}
