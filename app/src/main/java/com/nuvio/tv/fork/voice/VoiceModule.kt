package com.nuvio.tv.fork.voice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ModuleSetting
import com.nuvio.tv.fork.registry.SettingsLevel
import kotlinx.coroutines.runBlocking

/** Sterowanie głosem (B07): wyzwalacz → nasłuch systemowy → parser reguł → komunikat z rozpoznanym poleceniem. Wykonanie poleceń: dalsze kroki B07. */
object VoiceModule : ForkModule {
    override val id = "voice"
    override val title = "Sterowanie głosem"
    override val defaultEnabled = true

    override fun settings() = listOf(
        ModuleSetting("voice_whisper", SettingsLevel.ESSENTIAL, "Sterowanie głosem", "Rozpoznawanie mowy offline (Whisper)", "Wymaga pobranego modelu mowy (ustawienia napisów: model mowy); działa bez internetu", defaultValue = false),
        ModuleSetting("voice_key_service", SettingsLevel.ESSENTIAL, "Sterowanie głosem", "Przycisk asystenta otwiera nasłuch", "Wymaga włączenia usługi ułatwień dostępu Nuvio w systemie", defaultValue = false),
    )

    override fun routes() = listOf(com.nuvio.tv.fork.registry.ModuleRoute(VOICE_HELP_ROUTE) { composable(VOICE_HELP_ROUTE) { VoiceHelpScreen() } })

    private fun toast(c: android.content.Context, m: String) = android.widget.Toast.makeText(c, m, android.widget.Toast.LENGTH_LONG).show()
    private fun openSettings(c: android.content.Context, vararg actions: String) {
        for (a in actions) if (runCatching { c.startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        toast(c, "Ten telewizor nie ma takiego ekranu ustawień")
    }

    override fun actions() = listOf(
        // P7 (18.11): mikrofon pilota (np. JMGO) nie wysyła klawisza do aplikacji; trzy drogi do włączenia przycisku głosu.
        com.nuvio.tv.fork.registry.ModuleAction("Przycisk mikrofonu: włącz usługę Nuvio", SettingsLevel.SIMPLE,
            "Otwiera Ułatwienia dostępu: wybierz „Nuvio Debug” i włącz. Usługa przechwytuje tylko przycisk głosu") { c ->
            com.nuvio.tv.ui.screens.settings.ForkFeatures.set(c, "voice_key_service", true)
            if (VoiceKeyLearn.serviceOn(c)) toast(c, "Usługa Nuvio jest już włączona. Teraz „Naucz przycisk mikrofonu”.")
            else { toast(c, "Wybierz „Nuvio Debug” na liście i włącz. Potem wróć i użyj „Naucz przycisk mikrofonu”."); openSettings(c, android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS) }
        },
        com.nuvio.tv.fork.registry.ModuleAction("Przycisk mikrofonu: naucz przycisk", SettingsLevel.SIMPLE,
            "Po wybraniu naciśnij w ciągu 15 s przycisk mikrofonu (albo inny) na pilocie") { c ->
            if (!VoiceKeyLearn.serviceOn(c)) toast(c, "Najpierw „Przycisk mikrofonu: włącz usługę Nuvio”.")
            else { VoiceKeyLearn.arm(); toast(c, "Naciśnij teraz przycisk na pilocie, który ma włączać sterowanie głosem") }
        },
        com.nuvio.tv.fork.registry.ModuleAction("Ustaw Nuvio jako asystenta", SettingsLevel.SIMPLE,
            "Przycisk asystenta otworzy nasłuch Nuvio zamiast asystenta systemu (Android 12+)") { c ->
            toast(c, "Wybierz „Nuvio” jako asystenta / aplikację asystującą")
            openSettings(c, android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS, android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, android.provider.Settings.ACTION_SETTINGS)
        },
        com.nuvio.tv.fork.registry.ModuleAction("Sterowanie głosem: przykłady poleceń", SettingsLevel.SIMPLE, "Lista tego, co można powiedzieć") { com.nuvio.tv.ui.screens.settings.ForkFeatures.open?.invoke(VOICE_HELP_ROUTE) },
    )

    private val parser = RuleParser()

    /** Kanały z Live TV (id = adres strumienia, żeby od razu poprosić o odtwarzanie). */
    private fun channelIndex() = EntityIndex(
        com.nuvio.tv.reshaped.livetv.LiveTvRepository.uiState.value.channels.map { Entity(it.streamUrl, listOf(it.name)) },
    )

    /** Jedno nasłuchiwanie systemowym rozpoznawaniem mowy (pl-PL); null = błąd/brak mowy. Musi być wołane z wątku głównego. */
    private suspend fun listen(c: android.content.Context, onError: (Int) -> Unit): String? =
        if (com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, "voice_whisper", false) && WhisperListen.available(c)) WhisperListen.listen(c) else systemListen(c, onError)

    private suspend fun systemListen(c: android.content.Context, onError: (Int) -> Unit): String? = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        val rec = SpeechRecognizer.createSpeechRecognizer(c)
        rec.setRecognitionListener(object : RecognitionListener {
            override fun onResults(r: Bundle?) { rec.destroy(); if (cont.isActive) cont.resume(r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull(), null) }
            override fun onError(error: Int) { Log.i("ForkVoice", "error=$error"); onError(error); rec.destroy(); if (cont.isActive) cont.resume(null, null) }
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        cont.invokeOnCancellation { runCatching { rec.destroy() } }
        rec.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL"))
    }

    private val yes = setOf("tak", "potwierdzam", "dobrze", "jasne", "zgoda")

    @Composable
    private fun Overlay() {
        val c = LocalContext.current
        val count by VoiceTrigger.count.collectAsState()
        var shown by remember { mutableStateOf("") }
        val permission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
        LaunchedEffect(count) {
            if (count == 0) return@LaunchedEffect
            if (androidx.core.content.ContextCompat.checkSelfPermission(c, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) { shown = "Zezwól na mikrofon i powtórz polecenie"; permission.launch(android.Manifest.permission.RECORD_AUDIO); return@LaunchedEffect }
            if (!(com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, "voice_whisper", false) && WhisperListen.available(c)) && !SpeechRecognizer.isRecognitionAvailable(c)) { shown = "Brak rozpoznawania mowy w systemie"; return@LaunchedEffect }
            shown = "Słucham…"
            val ctx = VoiceContext(channels = channelIndex())
            val t = listen(c) { shown = "Błąd nasłuchu ($it)" }
            if (t == null) { kotlinx.coroutines.delay(3000); shown = ""; return@LaunchedEffect }
            val call = parser.parse(t, ctx)
            Log.i("ForkVoice", "text='$t' -> ${call?.id} ${call?.params} conf=${call?.confidence}")
            if (call == null) { shown = "Nie zrozumiałem: „$t”"; kotlinx.coroutines.delay(4000); shown = ""; return@LaunchedEffect }
            VoiceActions.lastMessage = null
            when (val d = Confidence.decide(listOf(call))) {
                is Confidence.Decision.Run -> { val ok = VoiceActions.run(c, d.call); shown = "„$t” → ${d.call.id}" + (if (ok) " ✓" else "") + (VoiceActions.lastMessage?.let { " · $it" } ?: "") }
                is Confidence.Decision.Confirm -> {
                    shown = "Czy na pewno: ${d.call.id}? Powiedz „tak”"
                    val a = listen(c) { shown = "Błąd nasłuchu ($it)" }
                    shown = if (a != null && com.nuvio.tv.fork.voice.Text.norm(a) in yes) { VoiceActions.run(c, d.call); "${d.call.id} ✓" } else "Anulowano"
                }
                is Confidence.Decision.Suggest -> {
                    val name = call.params["kanal"]
                    val options = if (call.id == "channel.play" && name != null) ctx.channels.match(name).take(3) else emptyList()
                    if (options.isEmpty()) { shown = "Nie jestem pewien: „$t”"; }
                    else {
                        shown = "Którą? " + options.mapIndexed { i, o -> "${i + 1}. ${o.name}" }.joinToString("  ") + " — powiedz numer"
                        val a = listen(c) { shown = "Błąd nasłuchu ($it)" }
                        val n = a?.let { com.nuvio.tv.fork.voice.Text.digits(com.nuvio.tv.fork.voice.Text.norm(it)).split(' ').firstNotNullOfOrNull { w -> w.toIntOrNull() } }
                        val pick = n?.let { options.getOrNull(it - 1) }
                        shown = if (pick != null) { VoiceActions.run(c, call.copy(params = call.params + ("kanal_id" to pick.id), confidence = 1f)); "${pick.name} ✓" } else "Anulowano"
                    }
                }
                Confidence.Decision.Nothing -> shown = "Nie zrozumiałem: „$t”"
            }
            kotlinx.coroutines.delay(4000); shown = ""
        }
        if (shown.isNotEmpty()) Box(Modifier.padding(24.dp).background(Color(0xCC000000))) {
            Text(shown, color = Color.White, modifier = Modifier.padding(16.dp).align(Alignment.Center))
        }
    }

    override fun rootOverlays(): List<@Composable () -> Unit> = listOf({ Overlay() })
}
