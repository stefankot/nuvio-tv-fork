package com.nuvio.tv.ainarrator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
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
    val models = remember(context) { ZonosOnDeviceModels(context) }
    val durable = remember(context) { NarratorPersistentAudio(context) }
    val preparation = remember(controller) { NarratorPreparer(context, models, durable) }
    var options by remember(prefs) { mutableStateOf(prefs.load()) }
    var geminiKey by remember(prefs) { mutableStateOf(prefs.apiKey()) }
    var showDialog by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(options) }
    var newGeminiKey by remember { mutableStateOf("") }
    var confirmDeleteAudio by remember { mutableStateOf(false) }
    var inputError by remember { mutableStateOf("") }
    val liveOptions by rememberUpdatedState(options)
    val liveGeminiKey by rememberUpdatedState(geminiKey)
    val status by engine.status.collectAsState()
    val download by models.status.collectAsState()
    val progress by preparation.status.collectAsState()
    val nativeAvailable = remember(context) {
        java.io.File(context.applicationInfo.nativeLibraryDir, "libzonos3_server.so").let {
            it.isFile && it.canExecute()
        }
    }

    DisposableEffect(engine, preparation, models) {
        onDispose {
            preparation.close()
            models.close()
            engine.close()
        }
    }
    LaunchedEffect(controller, engine) {
        var previousUrl = ""
        var mediaIdentity = ""
        var previousCueList: Any? = null
        var cachedLines = emptyList<NarratorLine>()
        while (isActive) {
            val cfg = liveOptions
            if (!cfg.enabled ||
                (cfg.backend == NarratorBackend.GEMINI && liveGeminiKey.isBlank())) {
                engine.tick(NarratorFrame("", 0L, false, false, ""), cfg, liveGeminiKey)
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
                liveGeminiKey
            )
            delay(220L)
        }
    }

    if (visible) {
        Button(onClick = {
            draft = options
            newGeminiKey = ""
            confirmDeleteAudio = false
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
                    Text("ZONOS3-Mini — bezpośrednio na tym Android TV; bez serwera LAN.")
                    Text("Model GGUF: około 1,12 GB. Pobierany wyłącznie po wybraniu opcji poniżej.")
                    Text("Stan modelu: " + when (download.phase) {
                        ModelPhase.CHECKING -> "Sprawdzanie plików"
                        ModelPhase.MISSING -> "Niepobrany"
                        ModelPhase.DOWNLOADING -> "Pobieranie"
                        ModelPhase.READY -> "Pobrany i zweryfikowany"
                        ModelPhase.ERROR -> "Błąd: " + download.message
                    })
                    if (download.phase == ModelPhase.DOWNLOADING) {
                        NarratorProgressBar((100L * download.downloadedBytes /
                            download.expectedBytes.coerceAtLeast(1L)).toInt().coerceIn(0, 100))
                        Text("Pobrano ${download.downloadedBytes / 1_000_000L} / " +
                            "${download.expectedBytes / 1_000_000L} MB")
                        Button(onClick = { models.cancelDownload() }) { Text("Przerwij pobieranie") }
                    } else if (download.phase != ModelPhase.READY && download.phase != ModelPhase.CHECKING) {
                        Button(onClick = { models.downloadOnUserRequest() }) {
                            Text("Pobierz / wznów pobieranie modelu")
                        }
                    }
                    if (download.phase == ModelPhase.READY) {
                        Text(if (nativeAvailable) "Silnik ARM64: dostępny" else
                            "Silnik ARM64 nie jest dołączony do APK — synteza zablokowana.")
                        Text("Audio przygotowuje się WYŁĄCZNIE przy wstrzymanym filmie.")
                        Text("Niezbędne są pełne napisy z czasami. Ukończone WAV pozostają na dysku.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val cues = controller.uiState.value.subtitleAutoSyncCues.mapNotNull { cue ->
                                    if (cue.startTimeMs < 0 || cue.endTimeMs <= cue.startTimeMs) null
                                    else NarratorLine(cue.startTimeMs, cue.endTimeMs, cue.text)
                                }
                                preparation.begin(PreparationHalf.FIRST,
                                    controller.playbackTimeline.value.duration, cues) {
                                    controller.uiState.value.isPlaying
                                }
                            }) { Text("Przygotuj 1. połowę") }
                            Button(onClick = {
                                val cues = controller.uiState.value.subtitleAutoSyncCues.mapNotNull { cue ->
                                    if (cue.startTimeMs < 0 || cue.endTimeMs <= cue.startTimeMs) null
                                    else NarratorLine(cue.startTimeMs, cue.endTimeMs, cue.text)
                                }
                                preparation.begin(PreparationHalf.SECOND,
                                    controller.playbackTimeline.value.duration, cues) {
                                    controller.uiState.value.isPlaying
                                }
                            }) { Text("Przygotuj 2. połowę") }
                        }
                    }
                    if (progress.total > 0) {
                        NarratorProgressBar(progress.percentage)
                        Text("Gotowe: ${progress.completed}/${progress.total} " +
                            "(${progress.percentage}%), z cache: ${progress.cached}")
                        if (progress.etaSeconds != null) {
                            Text("Szacowany pozostały czas: ${progress.etaSeconds / 60L} min")
                        }
                    }
                    if (progress.message.isNotBlank()) Text(progress.message)
                    if (progress.phase == PreparationPhase.WORKING) {
                        Button(onClick = { preparation.pause() }) {
                            Text("Wstrzymaj przygotowanie i zachowaj gotowe WAV")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            preparation.pause()
                            models.deleteOnUserRequest()
                        }) { Text("Usuń pobrany model") }
                        Button(onClick = { confirmDeleteAudio = !confirmDeleteAudio }) {
                            Text("Usuń przygotowane audio")
                        }
                    }
                    if (confirmDeleteAudio) {
                        Text("Potwierdź: usunąć wszystkie trwale zapisane kwestie lektora?")
                        Button(onClick = {
                            preparation.pause()
                            engine.deletePreparedAudioOnUserRequest()
                            confirmDeleteAudio = false
                        }) { Text("Potwierdź usunięcie WAV") }
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
                            if (newGeminiKey.isNotBlank()) prefs.saveApiKey(newGeminiKey)
                            prefs.save(draft)
                            draft
                        }
                        result.onSuccess { chosen ->
                            geminiKey = prefs.apiKey()
                            options = chosen
                            newGeminiKey = ""
                            showDialog = false
                        }.onFailure {
                            inputError = "Nie udało się zapisać ustawień."
                        }
                    }) { Text("Zapisz") }
                    if (draft.backend == NarratorBackend.GEMINI) {
                        Button(onClick = {
                            prefs.saveApiKey("")
                            geminiKey = ""
                        }) { Text("Usuń klucz Gemini") }
                    }
                    Button(onClick = { engine.clearCache() }) { Text("Wyczyść cache") }
                }
                Text("ZONOS3 generuje lokalnie tylko przy pauzie. Podczas filmu odtwarzane są " +
                    "wyłącznie zapisane WAV; Gemini przesyła napisy do Google. " +
                    "15 minut to limit pojedynczego przygotowania, nie gwarancja czasu dla połowy filmu.")
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

@Composable
private fun NarratorProgressBar(percent: Int) {
    val fraction = percent.coerceIn(0, 100) / 100f
    Box(Modifier.width(420.dp).height(9.dp).background(Color.DarkGray)) {
        Box(Modifier.width((420f * fraction).dp).height(9.dp).background(Color(0xFF8CC4D1)))
    }
}
