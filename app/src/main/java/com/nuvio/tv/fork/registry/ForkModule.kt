package com.nuvio.tv.fork.registry

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder

enum class SettingsLevel { SIMPLE, ESSENTIAL, ADVANCED }

/** Pozycja ustawień modułu. Wartość trzyma [key] w prefs `fork_features`. */
data class ModuleSetting(
    val key: String,
    val level: SettingsLevel,
    val group: String,
    val title: String,
    val description: String = "",
    val defaultValue: Boolean = false,
)

/** Jednorazowa czynność w ustawieniach (np. wyczyść historię). */
data class ModuleAction(val title: String, val level: SettingsLevel, val description: String = "", val run: (android.content.Context) -> Unit)

data class ModuleHomeRow(val id: String, val title: String, val order: Int = 0, val content: @Composable () -> Unit)

/** Klawisze i zdarzenia odtwarzacza. Domyślnie nic nie robi. */
interface PlayerHandler {
    fun onKey(keyCode: Int, isDown: Boolean): Boolean = false
    fun onPlaybackStart(contentId: String?) {}
    fun onPosition(positionMs: Long, durationMs: Long) {}
    fun onPlaybackEnd(positionMs: Long, durationMs: Long) {}
}

data class VoiceCommand(val id: String, val patterns: List<String>)

/** Moduł forka: wszystko opcjonalne, wyłączony moduł nic nie zgłasza (patrz [ForkRegistry]). */
interface ForkModule {
    val id: String
    val title: String
    val defaultEnabled: Boolean get() = false
    fun settings(): List<ModuleSetting> = emptyList()
    fun actions(): List<ModuleAction> = emptyList()
    fun routes(): List<ModuleRoute> = emptyList()
    fun homeRows(): List<ModuleHomeRow> = emptyList()
    fun rootOverlays(): List<@Composable () -> Unit> = emptyList()
    fun playerHandlers(): List<PlayerHandler> = emptyList()
    fun voiceCommands(): List<VoiceCommand> = emptyList()
}

data class ModuleRoute(val route: String, val register: NavGraphBuilder.() -> Unit)
