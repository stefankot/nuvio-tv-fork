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
import java.util.Locale

/**
 * Sole app adapter; engine and transport know nothing about NuvioTV's models.
 * No player state is modified, no mpv callbacks replaced.
 */
@Composable
fun NarratorOverlay(controller: PlayerRuntimeController, visible: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember(context) { NarratorPreferences(context) }
    val engine = remember(controller) { NarratorEngine(context) }
    var options by remember(prefs) { mutableStateOf(prefs.load()) }
    var geminiKey by remember(prefs) { mutableStateOf(prefs.apiKey()) }
    var zonosToken by remember(prefs) { mutableStateOf(prefs.localToken()) }
    var showDialog by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(options) }
    var newGeminiKey by remember { mutableStateOf("") }
    var newZonosToken by remember { mutableStateOf("") }
    var serverAddress by remember { mutableStateOf(options.localEndpoint) }
    var speakerId by remember { mutableStateOf(options.localSpeakerId) }
    var inputError by remember { mutableStateOf("") }
    val liveOptions by rememberUpdatedState(options)
    val liveGeminiKey by rememberUpdatedState(geminiKey)
    val liveZonosToken by rememberUpdatedState(zonosToken)
    val status by engine.status.collectAsState()

    DisposableEffect(engine) { onDispose { engine.close() } }
    LaunchedEffect(controller, engine) {
        var previousUrl = ""
        var mediaIdentity = ""
        var previousCueList: Any? = null
        var cachedLines = emptyList<NarratorLine>()
        while (isActive) {
            val cfg = liveOptions
            if (!cfg.enabled ||
                (cfg.backend == NarratorBackend.GEMINI && liveGeminiKey.isBlank()) ||
                (cfg.backend == NarratorBackend.ZONOS_LOCAL && cfg.localEndpoint.isBlank())) {
                engine.tick(NarratorFrame("", 0L, false, false, ""), cfg, liveGeminiKey, liveZonosToken)
                delay(900L)
                continue
            }
            val ui = controller.uiState.value
            val timeline = controller.playbackTimeline.value
            val currentText = if (ui.internalPlayerEngine == InternalPlayerEngine.MVP_PLAYER) {
                runCatching { controller.mpvView?.mpv?.getPropertyString("sub-text") }.getOrNull().orEmpty()
            } else {
                controller.subtitleCueTracker.state.value.text
            }
            if (ui.subtitleAutoSyncCues !== previousCueList) {
                previousCueList = ui.subtitleAutoSyncCues
                cachedLines = ui.subtitleAutoSyncCues.mapNotNull { cue ->
                    if (cue.startTimeMs < 0L || cue.endTimeMs <= cue.startTimeMs) null
                    else NarratorLine(cue.startTimeMs, cue.endTimeMs, cue.text)
                }
            }
            val mediaUrl = controller.getCurrentStreamUrl()
            if (mediaUrl != previousUrl) {
                previousUrl = mediaUrl
                mediaIdentity = if (mediaUrl.isEmpty()) "" else {
                    MessageDigest.getInstance("SHA-256").digest(mediaUrl.toByteArray(Charsets.UTF_8))
                        .take(12).joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 255) }
                }
            }
            engine.tick(
                NarratorFrame(
                    mediaIdentity,
                    timeline.currentPosition,
                    ui.isPlaying,
                    ui.isBuffering,
                    currentText,
                    cachedLines
                ),
                cfg,
                liveGeminiKey,
                liveZonosToken
            )
            delay(220L)
        }
    }

    if (visible) {
        Button(onClick = {
            draft = options
            serverAddress = options.localEndpoint
            speakerId = options.localSpeakerId
            newGeminiKey = ""
            newZonosToken = ""
            inputError = ""
            showDialog = true
        }, modifier = modifier.padding(12.dp)) {
            Text(if (options.enabled) "Lektor AI: wł." else "Lektor AI")
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Column(
                modifier = Modifier.width(480.dp).background(Color(0xFF202126)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Lektor AI — ustawienia")
                Text("Status: $status")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        draft = draft.copy(backend = if (draft.backend == NarratorBackend.GEMINI)
                            NarratorBackend.ZONOS_LOCAL else NarratorBackend.GEMINI)
                    }) {
                        Text(if (draft.backend == NarratorBackend.GEMINI) "Gemini (chmura)" else "ZONOS3 (lokalnie)")
                    }
                    Button(onClick = { draft = draft.copy(enabled = !draft.enabled) }) {
                        Text(if (draft.enabled) "Wyłącz" else "Włącz")
                    }
                }
                if (draft.backend == NarratorBackend.GEMINI) {
                    Text("Model Gemini i głos:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            draft = draft.copy(model = if (draft.model == NarratorModel.FLASH)
                                NarratorModel.FLASH_LITE else NarratorModel.FLASH)
                        }) {
                            Text(if (draft.model == NarratorModel.FLASH) "Flash" else "Flash-Lite")
                        }
                        Button(onClick = {
                            draft = draft.copy(voice = if (draft.voice == "Kore") "Puck" else "Kore")
                        }) { Text(draft.voice) }
                    }
                    Text("Klucz Gemini API (wpisz nowy lub pozostaw pusty):")
                    NarratorField(newGeminiKey, { newGeminiKey = it.take(300) }, secret = true)
                    Text("Szacowany koszt zarezerwowany w tym miesiącu: " +
                        "${"%.4f".format(Locale.ROOT, prefs.spentThisMonth())} USD")
                    Text("Szacowany limit: ${draft.monthlyUsdLimit} USD / miesiąc")
                    Button(onClick = {
                        draft = draft.copy(monthlyUsdLimit = when (draft.monthlyUsdLimit) {
                            2.0 -> 5.0
                            5.0 -> 10.0
                            10.0 -> 20.0
                            else -> 2.0
                        })
                    }) { Text("Zmień limit") }
                } else {
                    Text("Lokalny ZONOS3-Mini — serwer w tej samej sieci LAN.")
                    Text("Pełny adres bazowy serwera z portem (HTTP/HTTPS):")
                    NarratorField(serverAddress, { serverAddress = it.take(200) })
                    Text("Identyfikator głosu (puste = pierwszy głos serwera):")
                    NarratorField(speakerId, { speakerId = it.take(120) })
                    Text("Token serwera, jeśli włączono autoryzację (nowy):")
                    NarratorField(newZonosToken, { newZonosToken = it.take(300) }, secret = true)
                    Text("Bez opłat Gemini; zasoby i energia lokalnego komputera.")
                }
                Text("Głośność lektora: ${(draft.narratorVolume * 100).toInt()}%")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        draft = draft.copy(narratorVolume = (draft.narratorVolume - 0.1f).coerceAtLeast(0.1f))
                    }) { Text("Ciszej") }
                    Button(onClick = {
                        draft = draft.copy(narratorVolume = (draft.narratorVolume + 0.1f).coerceAtMost(1f))
                    }) { Text("Głośniej") }
                }
                if (inputError.isNotEmpty()) Text(inputError)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val result = runCatching {
                            val endpoint = if (draft.backend == NarratorBackend.ZONOS_LOCAL && serverAddress.isNotBlank())
                                ZonosEndpoint.parse(serverAddress).toString()
                            else options.localEndpoint
                            val chosen = draft.copy(localEndpoint = endpoint, localSpeakerId = speakerId.trim())
                            if (chosen.enabled && chosen.backend == NarratorBackend.ZONOS_LOCAL && endpoint.isBlank()) {
                                throw IllegalArgumentException("Podaj adres serwera ZONOS3.")
                            }
                            if (newGeminiKey.isNotBlank()) prefs.saveApiKey(newGeminiKey)
                            if (newZonosToken.isNotBlank()) prefs.saveLocalToken(newZonosToken)
                            prefs.save(chosen)
                            chosen
                        }
                        result.onSuccess { chosen ->
                            geminiKey = prefs.apiKey()
                            zonosToken = prefs.localToken()
                            options = chosen
                            newGeminiKey = ""
                            newZonosToken = ""
                            showDialog = false
                        }.onFailure {
                            inputError = "Nie udało się zapisać: sprawdź adres serwera i ustawienia."
                        }
                    }) { Text("Zapisz") }
                    Button(onClick = {
                        if (draft.backend == NarratorBackend.GEMINI) {
                            prefs.saveApiKey("")
                            geminiKey = ""
                        } else {
                            prefs.saveLocalToken("")
                            zonosToken = ""
                        }
                    }) { Text("Usuń token") }
                    Button(onClick = { engine.clearCache() }) { Text("Wyczyść cache") }
                }
                Text("ZONOS3 przetwarza napisy tylko na wybranym serwerze lokalnym. " +
                    "Gemini przesyła je do Google. Bitstream/passthrough może uniemożliwić miksowanie.")
            }
        }
    }
}

@Composable
private fun NarratorField(value: String, onValueChange: (String) -> Unit, secret: Boolean = false) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.width(424.dp).background(Color.White).padding(7.dp),
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        textStyle = TextStyle(color = Color.Black)
    )
}
