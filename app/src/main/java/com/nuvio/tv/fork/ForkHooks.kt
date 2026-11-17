package com.nuvio.tv.fork

import android.app.Activity
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import com.nuvio.tv.fork.arc.RemoteGlobalHook
import com.nuvio.tv.fork.registry.ForkRegistry
import com.nuvio.tv.fork.registry.ModuleHomeRow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import android.content.Context
import androidx.navigation.NavGraphBuilder
import com.nuvio.tv.skin.api.SkinRegistry
import com.nuvio.tv.skin.api.rememberSkinId
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState

/** Jedyne punkty wpięcia forka w pliki rdzenia. Każda funkcja wraca natychmiast, gdy funkcja jest wyłączona. */
object ForkHooks {
    /** MainActivity.dispatchKeyEvent: pilot Arc poza odtwarzaczem. true = klawisz obsłużony. */
    fun onGlobalKey(activity: Activity, event: KeyEvent): Boolean = RemoteGlobalHook.onKey(activity, event)

    /** HomeScreen / trasy: true = aktywny skin narysował ekran tej trasy. */
    @Composable
    fun skinScreen(route: String, args: Map<String, Any?>): Boolean {
        val skin = SkinRegistry.skinFor(rememberSkinId()) ?: return false
        return skin.screen(route)?.Content(args) ?: false
    }

    /** PlayerScreen: true = aktywny skin narysował nakładkę (sterowanie i panele) odtwarzacza. */
    @Composable
    fun skinPlayerChrome(uiState: PlayerUiState, onEvent: (PlayerEvent) -> Unit): Boolean {
        val skin = SkinRegistry.skinFor(rememberSkinId()) ?: return false
        return skin.PlayerChrome(uiState, onEvent)
    }

    /** NuvioNavHost: intro odtwarzania (Echo A6) rysowane nad aplikacją; Wstecz je pomija. Wyłączone = sama treść. */
    @Composable
    fun introWrap(content: @Composable () -> Unit) = com.nuvio.tv.fork.echo.EchoIntroHook.Wrap(content)

    /** StreamScreen: autoodtwarzanie wchodzi od razu w intro. */
    @Composable
    fun introBeforePlayer(uiState: com.nuvio.tv.ui.screens.stream.StreamScreenUiState) =
        com.nuvio.tv.fork.echo.EchoIntroHook.BeforePlayer(uiState.autoPlayDecided, uiState.showDirectAutoPlayOverlay)

    /** PlayerScreen: ręczne otwarcie źródła też pokazuje intro. */
    @Composable
    fun introOnPlayerOpen() = com.nuvio.tv.fork.echo.EchoIntroHook.OnPlayerOpen()

    /** PlayerRuntimeController: obowiązujący poziom trybu nocnego (harmonogram) zamiast surowego ustawienia (B05-6). */
    fun effectiveNight(mode: com.nuvio.tv.data.local.NightMode): com.nuvio.tv.data.local.NightMode {
        val eff = com.nuvio.tv.fork.night.NightModule.controller.effective(System.currentTimeMillis())
        val key = com.nuvio.tv.fork.night.NightEffects.effectiveAudio(mode.settingValue, eff, eff)
        return com.nuvio.tv.data.local.NightMode.entries.firstOrNull { it.settingValue == key } ?: mode
    }

    /** NuvioNavHost: na poziomie Prostym ekran Ustawień rysuje moduł. true = narysowany. */
    @Composable
    fun simpleSettings(navigate: (String) -> Unit): Boolean = com.nuvio.tv.fork.settings.SimpleSettingsGate(navigate)

    /** PlayerRuntimeControllerTracks: po poznaniu ścieżek dźwięku (B04-4). true = brak polskiej ścieżki i profil chce polskiej: spróbuj następnego źródła. */
    fun wantsNextSourceForAudio(c: Context, languages: List<String?>, attemptsUsed: Int, maxAttempts: Int): Boolean =
        com.nuvio.tv.fork.sources.AudioCheck.decide(
            wantsPolish = com.nuvio.tv.fork.sources.SourceChoice.answer(c) == "pl_always" || com.nuvio.tv.fork.sources.SourceChoice.answer(c) == "pl_if_good",
            trackLanguages = languages, attemptsUsed = attemptsUsed, maxAttempts = maxAttempts,
        ) == com.nuvio.tv.fork.sources.AudioCheck.Decision.TRY_NEXT

    // --- Rejestr modułów (B00) ---

    /** PlaybackNavHost: trasy włączonych modułów. */
    fun moduleRoutes(builder: NavGraphBuilder, c: Context) {
        ForkRegistry.forContext(c).routes().forEach { it.register(builder) }
    }

    /** HomeScreen: rzędy włączonych modułów (rysuje je B03; pusta lista = bez zmian). */
    @Composable
    fun moduleHomeRows(): List<ModuleHomeRow> {
        val c = LocalContext.current
        return remember(c) { ForkRegistry.forContext(c).homeRows() }
    }

    /** MainActivity: nakładki na całe okno. */
    @Composable
    fun moduleOverlays() {
        val c = LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) { com.nuvio.tv.fork.settings.FirstRunDefaults.run(c) }
        val overlays = remember(c) { ForkRegistry.forContext(c).rootOverlays() }
        overlays.forEach { it() }
    }

    /** PlayerScreen: klawisze odtwarzacza dla modułów. true = obsłużony. */
    fun modulePlayerKey(c: Context, e: KeyEvent): Boolean =
        ForkRegistry.forContext(c).playerHandlers()
            .any { it.onKey(e.keyCode, e.action == KeyEvent.ACTION_DOWN) }
}
