package com.nuvio.tv.fork.sources

import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ModuleAction
import com.nuvio.tv.fork.registry.SettingsLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Źródła i napisy bez tarcia (B04): jedno pytanie o polski dźwięk lub jakość, rozmiar napisów. */
object SourcesModule : ForkModule {
    override val id = "sources"
    override val title = "Źródła i napisy: automatyczny dobór"
    override val defaultEnabled = true
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Przegląd ekranów 27.11 („Gotowe” w lewym dolnym rogu, właściciel: „nie wiem, co to i po co”): linia „Gotowe · jakość”
    // z B04-6 była warstwą całej aplikacji — zostawała na odtwarzaczu i innych stronach. Stan źródła pokazują już ekrany
    // (Kicia: linia pod przyciskami, Niunia: ekran wczytywania), więc warstwa wyłączona. Cofnięcie: listOf({ ReadyOverlay() }).
    override fun rootOverlays(): List<@androidx.compose.runtime.Composable () -> Unit> = emptyList()

    override fun actions(): List<ModuleAction> {
        val answer = com.nuvio.tv.fork.sources.SourceChoiceState.current
        fun answerAction(id: String, text: String) = ModuleAction(
            (if (answer == id) "✓ " else "") + text, SettingsLevel.SIMPLE,
            if (id == SourcePresets.PL_ALWAYS) "Co ważniejsze: polski dźwięk czy jakość obrazu?" + (if (answer == SourcePresets.CUSTOM) " (teraz: Własne)" else "") else "",
        ) { c -> scope.launch { SourceChoice.apply(c, id); SourceChoiceState.current = id } }
        return listOf(
            answerAction(SourcePresets.PL_ALWAYS, "Zawsze po polsku"),
            answerAction(SourcePresets.PL_IF_GOOD, "Po polsku przy dobrej jakości"),
            answerAction(SourcePresets.ORIGINAL, "Oryginał z napisami"),
            ModuleAction("Napisy: małe", SettingsLevel.SIMPLE, "Rozmiar napisów na półprzezroczystym tle") { c -> scope.launch { SourceChoice.applySubtitleSize(c, SubtitleSizeChoice.SMALL) } },
            ModuleAction("Napisy: średnie", SettingsLevel.SIMPLE) { c -> scope.launch { SourceChoice.applySubtitleSize(c, SubtitleSizeChoice.MEDIUM) } },
            ModuleAction("Napisy: duże", SettingsLevel.SIMPLE) { c -> scope.launch { SourceChoice.applySubtitleSize(c, SubtitleSizeChoice.LARGE) } },
        )
    }
}

/** Ostatnia odpowiedź w pamięci (ustawiana przy starcie z [SourceChoice.refresh]). */
object SourceChoiceState { @Volatile var current: String = "" }
