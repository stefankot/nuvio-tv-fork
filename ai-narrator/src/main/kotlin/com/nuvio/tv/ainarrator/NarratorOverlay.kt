package com.nuvio.tv.ainarrator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.nuvio.tv.data.local.InternalPlayerEngine
import com.nuvio.tv.ui.screens.player.PlayerRuntimeController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.security.MessageDigest

/**
 * The single upstream adapter. This is the ONLY file depending on Nuvio player classes.
 * Accesses existing package-internal StateFlows and public mpv property adapter.
 */
@Composable
fun NarratorOverlay(controller: PlayerRuntimeController, visible: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember(context) { NarratorPreferences(context) }
    val engine = remember(controller) { NarratorEngine(context) }
    var options by remember(prefs) { mutableStateOf(prefs.load()) }
    var currentKey by remember(prefs) { mutableStateOf(prefs.apiKey()) }
    var showDialog by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    val liveOptions by rememberUpdatedState(options)
    val liveKey by rememberUpdatedState(currentKey)
    val status by engine.status.collectAsState()
    DisposableEffect(engine) {
        onDispose { engine.close() }
    }
    LaunchedEffect(controller, engine) {
        while (isActive) {
            val ui = controller.uiState.value
            val timeline = controller.playbackTimeline.value
            val mpv = ui.internalPlayerEngine == InternalPlayerEngine.MVP_PLAYER
            val currentText = if (mpv) {
                runCatching { controller.mpvView?.mpv?.getPropertyString("sub-text") }.getOrNull().orEmpty()
            } else {
                controller.subtitleCueTracker.state.value.text
            }
            val lines = ui.subtitleAutoSyncCues.mapNotNull { cue ->
                if (cue.startTimeMs < 0L || cue.endTimeMs <= cue.startTimeMs) null
                else NarratorLine(cue.startTimeMs, cue.endTimeMs, cue.text)
            }
            val mediaUrl = controller.getCurrentStreamUrl()
            val mediaIdentity = if (mediaUrl.isEmpty()) "" else {
                MessageDigest.getInstance("SHA-256").digest(mediaUrl.toByteArray())
                    .take(12).joinToString("") { "%02x".format(it.toInt() and 255) }
            }
            engine.tick(
                NarratorFrame(mediaIdentity, timeline.currentPosition, ui.isPlaying, ui.isBuffering, currentText, lines),
                liveOptions,
                liveKey
            )
            delay(220L)
        }
    }

    if (visible) {
        Button(onClick = { showDialog = true }, modifier = modifier.padding(12.dp)) {
            Text(if (options.enabled) "Lektor AI: wł." else "Lektor AI")
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Column(
                modifier = Modifier.width(460.dp).background(Color(0xFF202126)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Lektor AI — ustawienia")
                Text("Status: $status")
                Text("Koszt zarezerwowany w tym miesiącu: ${"%.4f".format(prefs.spentThisMonth())} USD")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { options = options.copy(enabled = !options.enabled) }) {
                        Text(if (options.enabled) "Wyłącz" else "Włącz")
                    }
                    Button(onClick = {
                        options = options.copy(model = if (options.model == NarratorModel.FLASH) NarratorModel.FLASH_LITE
                            else NarratorModel.FLASH)
                    }) { Text(if (options.model == NarratorModel.FLASH) "Flash" else "Flash-Lite") }
                }
                Text("Klucz Gemini API (nowy, nie jest wyświetlany ponownie):")
                BasicTextField(
                    value = newKey, onValueChange = { newKey = it.take(300) },
                    modifier = Modifier.width(390.dp).background(Color.White).padding(8.dp),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = TextStyle(color = Color.Black)
                )
                Text("Głośność lektora: ${(options.narratorVolume * 100).toInt()}%")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { options = options.copy(narratorVolume = (options.narratorVolume - 0.1f).coerceAtLeast(0.1f)) }) {
                        Text("Ciszej")
                    }
                    Button(onClick = { options = options.copy(narratorVolume = (options.narratorVolume + 0.1f).coerceAtMost(1f)) }) {
                        Text("Głośniej")
                    }
                }
                Text("Orientacyjny limit na urządzeniu: ${options.monthlyUsdLimit} USD / miesiąc")
                Button(onClick = {
                    options = options.copy(monthlyUsdLimit = when (options.monthlyUsdLimit) {
                        2.0 -> 5.0
                        5.0 -> 10.0
                        10.0 -> 20.0
                        else -> 2.0
                    })
                }) { Text("Zmień limit") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (newKey.isNotBlank()) {
                            prefs.saveApiKey(newKey)
                            currentKey = prefs.apiKey()
                            newKey = ""
                        }
                        prefs.save(options)
                        showDialog = false
                    }) { Text("Zapisz") }
                    Button(onClick = {
                        prefs.saveApiKey("")
                        currentKey = ""
                        options = options.copy(enabled = false)
                        prefs.save(options)
                    }) { Text("Usuń klucz") }
                    Button(onClick = { engine.clearCache() }) { Text("Wyczyść cache") }
                }
                Text("Włączając lektora, przesyłasz tekst napisów do usługi Google Gemini. " +
                    "Działa z napisami tekstowymi; wyjście bitstream/passthrough może wykluczać miksowanie. " +
                    "Nie jest gwarantowane nadążanie za krótkimi dialogami.")
            }
        }
    }
}
