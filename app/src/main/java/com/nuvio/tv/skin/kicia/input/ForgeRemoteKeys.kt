package com.nuvio.tv.skin.kicia.input

import android.view.KeyEvent

/** Jobs a remote button can be given from anywhere in the app. */
enum class ForgeRemoteAction {
    /** Open Search and start listening (the Assistant / microphone button). */
    VoiceSearch,
}

/**
 * Decides which remote buttons the app takes for itself.
 *
 * The microphone button of a Google TV / Android TV remote is normally eaten by the system and starts
 * the Assistant. Some remotes (and some firmware) still pass it to the app, as one of the codes below.
 * Whichever code arrives is recorded with [describe] in debug builds, so the exact code of a given
 * remote can be read off with adb and added to [learnedVoiceKeyCodes].
 */
class ForgeRemoteKeys(
    /** Extra key codes (or scan codes, see [learnedVoiceScanCodes]) learnt for a specific remote. */
    private val learnedVoiceKeyCodes: Set<Int> = emptySet(),
    private val learnedVoiceScanCodes: Set<Int> = emptySet(),
) {
    fun actionFor(keyCode: Int, scanCode: Int = 0): ForgeRemoteAction? = when {
        keyCode in BuiltInVoiceKeyCodes -> ForgeRemoteAction.VoiceSearch
        keyCode in learnedVoiceKeyCodes -> ForgeRemoteAction.VoiceSearch
        scanCode != 0 && scanCode in learnedVoiceScanCodes -> ForgeRemoteAction.VoiceSearch
        else -> null
    }

    companion object {
        /** Search, Assistant and voice-assistant keys; any of them means "I want to speak". */
        val BuiltInVoiceKeyCodes: Set<Int> = setOf(
            KeyEvent.KEYCODE_SEARCH,
            KeyEvent.KEYCODE_ASSIST,
            KeyEvent.KEYCODE_VOICE_ASSIST,
        )

        /** One-line description for the debug log, e.g. "down code=219 (KEYCODE_ASSIST) scan=0 device=7". */
        fun describe(event: KeyEvent): String {
            val action = when (event.action) {
                KeyEvent.ACTION_DOWN -> "down"
                KeyEvent.ACTION_UP -> "up"
                else -> "action=${event.action}"
            }
            return "$action code=${event.keyCode} (${KeyEvent.keyCodeToString(event.keyCode)}) " +
                "scan=${event.scanCode} device=${event.deviceId} repeat=${event.repeatCount}"
        }
    }
}
