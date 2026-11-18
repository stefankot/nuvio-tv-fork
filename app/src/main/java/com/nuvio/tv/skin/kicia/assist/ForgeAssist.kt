package com.nuvio.tv.skin.kicia.assist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.nuvio.tv.MainActivity
import com.nuvio.tv.skin.kicia.input.ForgeRemoteHook

/**
 * The remote's Assistant button, handled the way Android intends it: Nuvio registers as a digital assistant.
 * When the viewer chooses Nuvio as the assistant in the TV's settings, the system starts it for the Assistant
 * button (the key itself never reaches an app). The session just opens Nuvio on Search with the microphone on.
 */
internal fun openSearchWithVoice(context: Context): Intent {
    ForgeRemoteHook.requestVoiceOnSearch()
    return Intent(context, MainActivity::class.java).addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP,
    )
}

/** Handles ACTION_ASSIST / ACTION_VOICE_ASSIST when the system (or a launcher) asks for "the assistant". */
class ForgeAssistActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(openSearchWithVoice(this))
        ForgeRemoteHook.triggerVoice() // app already running: its bar and Search screen react at once
        finish()
    }
}

class ForgeVoiceInteractionService : VoiceInteractionService()

class ForgeVoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = ForgeVoiceSession(this)
}

private class ForgeVoiceSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        startAssistantActivity(openSearchWithVoice(context))
        ForgeRemoteHook.triggerVoice()
        hide()
    }
}
