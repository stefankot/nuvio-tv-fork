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
        ModuleSetting("voice_key_service", SettingsLevel.ESSENTIAL, "Sterowanie głosem", "Przycisk asystenta otwiera nasłuch", "Wymaga włączenia usługi ułatwień dostępu Nuvio w systemie", defaultValue = false),
    )

    private val parser = RuleParser()

    @Composable
    private fun Overlay() {
        val c = LocalContext.current
        val count by VoiceTrigger.count.collectAsState()
        var shown by remember { mutableStateOf("") }
        LaunchedEffect(count) {
            if (count == 0) return@LaunchedEffect
            if (!SpeechRecognizer.isRecognitionAvailable(c)) { shown = "Brak rozpoznawania mowy w systemie"; return@LaunchedEffect }
            shown = "Słucham…"
            val rec = SpeechRecognizer.createSpeechRecognizer(c)
            rec.setRecognitionListener(object : RecognitionListener {
                override fun onResults(r: Bundle?) {
                    val t = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    val call = runBlocking { parser.parse(t, VoiceContext()) }
                    Log.i("ForkVoice", "text='$t' -> ${call?.id} ${call?.params}")
                    val done = call != null && VoiceActions.run(c, call)
                    shown = if (call != null) "„$t” → ${call.id}" + (if (done) " ✓" else "") else "Nie zrozumiałem: „$t”"
                    rec.destroy()
                }
                override fun onError(error: Int) { Log.i("ForkVoice", "error=$error"); shown = "Błąd nasłuchu ($error)"; rec.destroy() }
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(p: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}
            })
            rec.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL"))
            kotlinx.coroutines.delay(6000); shown = ""
        }
        if (shown.isNotEmpty()) Box(Modifier.padding(24.dp).background(Color(0xCC000000))) {
            Text(shown, color = Color.White, modifier = Modifier.padding(16.dp).align(Alignment.Center))
        }
    }

    override fun rootOverlays(): List<@Composable () -> Unit> = listOf({ Overlay() })
}
