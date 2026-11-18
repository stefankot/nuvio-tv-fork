package com.nuvio.tv.ui.screens.settings

import android.content.Context

object ForkFeatures {
    /** Set by NuvioNavHost; opens a route. */
    var open: ((String) -> Unit)? = null
    private fun prefs(c: Context) = c.getSharedPreferences("fork_features", Context.MODE_PRIVATE)
    /**
     * Funkcje forków włączone od pierwszego uruchomienia (polecenie właściciela 18.11): wygrywa z domyślną wartością
     * podaną w miejscu odczytu. Zapisany wybór użytkownika zawsze ma pierwszeństwo.
     */
    val ON_BY_DEFAULT = setOf(
        // Echo
        "echo_source_fallback", "echo_auto_quality", "echo_hot_swap", "echo_race", "echo_audio_menu", "echo_playback_intro",
        // YouTube, katalog KE
        "fork_youtube", "fork_ke_catalog",
        // Moduły rejestru wyłączone domyślnie przez autora
        "module_lektor",
        // Lektor sam, gdy brak polskiego dźwięku; głos: Whisper offline (gdy model pobrany), przycisk asystenta przez usługę
        "lektor_auto", "voice_whisper", "voice_key_service",
        // Start Nuvio z telewizorem
        "launcher_autostart",
    )
    fun enabled(c: Context, key: String, default: Boolean = false) = prefs(c).getBoolean(key, default || key in ON_BY_DEFAULT)
    fun set(c: Context, key: String, v: Boolean) = prefs(c).edit().putBoolean(key, v).apply()
    fun str(c: Context, key: String, default: String = "") = prefs(c).getString(key, default) ?: default
    fun setStr(c: Context, key: String, v: String) = prefs(c).edit().putString(key, v).apply()
}

