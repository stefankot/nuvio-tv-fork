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
        // P7 (18.11): diagnostyka JMGO (mikrofon pilota nie dociera do aplikacji) i „Naucz przycisk mikrofonu”.
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) android.util.Log.i("ForkVoiceKey", "key=${event.keyCode} scan=${event.scanCode} device=${event.deviceId}")
        if (VoiceKeyLearn.consume(this, event)) return true
        val chosen = getSharedPreferences("fork_features", MODE_PRIVATE).getString("voice_key", KeyFilter.KEYCODE_ASSIST.toString())?.toIntOrNull() ?: KeyFilter.KEYCODE_ASSIST
        val enabled = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(this, "voice_key_service")
        if (!KeyFilter.shouldIntercept(event.keyCode, chosen, enabled)) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            startActivity(openApp(this)); VoiceTrigger.fire()
        }
        return true
    }
}

/** „Naucz przycisk mikrofonu”: następny naciśnięty klawisz (poza strzałkami, OK, Wstecz, Home) zostaje przyciskiem głosu. */
object VoiceKeyLearn {
    @Volatile var armedUntil = 0L
    private val IGNORED = setOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME)

    fun arm() { armedUntil = android.os.SystemClock.elapsedRealtime() + 15_000 }

    fun consume(c: Context, event: KeyEvent): Boolean {
        if (android.os.SystemClock.elapsedRealtime() > armedUntil || event.keyCode in IGNORED) return false
        if (event.action != KeyEvent.ACTION_DOWN) return true
        armedUntil = 0L
        com.nuvio.tv.ui.screens.settings.ForkFeatures.setStr(c, "voice_key", event.keyCode.toString())
        com.nuvio.tv.ui.screens.settings.ForkFeatures.set(c, "voice_key_service", true)
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(c, "Zapamiętano przycisk głosu (kod ${event.keyCode})", android.widget.Toast.LENGTH_LONG).show()
        }
        return true
    }

    fun serviceOn(c: Context): Boolean {
        val list = android.provider.Settings.Secure.getString(c.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return list.contains(c.packageName + "/") && list.contains("ForkKeyService")
    }
}
