package com.nuvio.tv.fork.voice

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.nuvio.tv.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Wyzwalacz sterowania głosem: asystent systemowy, usługa klawisza albo akcja pilota Arc ustawiają licznik; nakładka „Słucham…” go obserwuje. */
object VoiceTrigger {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()
    fun fire() { _count.value = _count.value + 1 }
}

private fun openApp(context: Context) = Intent(context, MainActivity::class.java).addFlags(
    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP,
)

/** ACTION_ASSIST / ACTION_VOICE_ASSIST: otwiera Nuvio i uruchamia nasłuch poleceń zamiast wyszukiwania. */
class ForkAssistActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(openApp(this)); VoiceTrigger.fire(); finish()
    }
}

class ForkVoiceInteractionService : VoiceInteractionService()

class ForkVoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = ForkVoiceSession(this)
}

private class ForkVoiceSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        startAssistantActivity(openApp(context)); VoiceTrigger.fire(); hide()
    }
}

/**
 * Usługa ułatwień dostępu tylko do jednego klawisza (domyślnie przycisk asystenta). Przejmuje wyłącznie wybrany klawisz
 * ([KeyFilter]), resztę przepuszcza bez opóźnienia, niczego nie czyta z ekranu.
 */
class ForkKeyService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val chosen = getSharedPreferences("fork_features", MODE_PRIVATE).getString("voice_key", KeyFilter.KEYCODE_ASSIST.toString())?.toIntOrNull() ?: KeyFilter.KEYCODE_ASSIST
        val enabled = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(this, "voice_key_service")
        if (!KeyFilter.shouldIntercept(event.keyCode, chosen, enabled)) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            startActivity(openApp(this)); VoiceTrigger.fire()
        }
        return true
    }
}
