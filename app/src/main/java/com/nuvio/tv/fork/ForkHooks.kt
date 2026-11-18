package com.nuvio.tv.fork

import android.app.Activity
import android.view.KeyEvent
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.fillMaxSize
import com.nuvio.tv.fork.arc.RemoteGlobalHook
import com.nuvio.tv.fork.registry.ForkRegistry
import com.nuvio.tv.fork.registry.ModuleHomeRow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import android.content.Context
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nuvio.tv.skin.api.SkinRegistry
import com.nuvio.tv.skin.api.rememberSkinId
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState

/** Jedyne punkty wpięcia forka w pliki rdzenia. Każda funkcja wraca natychmiast, gdy funkcja jest wyłączona. */
object ForkHooks {
    /** MainActivity.dispatchKeyEvent: pilot Arc poza odtwarzaczem. true = klawisz obsłużony. */
    fun onGlobalKey(activity: Activity, event: KeyEvent): Boolean {
        com.nuvio.tv.fork.family.FamilyUsage.key(event) // statystyki używania (panel Rodzina)
        // P7 (18.11): przycisk Szukaj/Asystent na pilocie = nasłuch poleceń forka (moduł głosu) PRZED skórką Kicia,
        // która otwierała ekran Szukaj z systemowym rozpoznawaniem Google (na JMGO zamykało się od razu).
        if (event.keyCode in VOICE_KEYS && com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(activity, "module_voice", true)) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) com.nuvio.tv.fork.voice.VoiceTrigger.fire()
            return true
        }
        // Skórka Kicia: jej pilot (Esc = Wstecz, odzyskanie fokusu odtwarzacza).
        if (com.nuvio.tv.skin.kicia.ForgeUi.ON && com.nuvio.tv.skin.kicia.input.ForgeRemoteHook.onKey(event, activity::dispatchKeyEvent)) return true
        return RemoteGlobalHook.onKey(activity, event)
    }
    /** MainActivity: YouTube jako pozycja menu (audyt UI p. 5), gdy włączony. */
    fun youTubeInMenu(c: Context): Boolean = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, "fork_youtube", true)

    /**
     * MainActivity: lista menu. Dodaje YouTube; przy skórce Kicia kolejność z makiety Kicii (34c80c6): Szukaj, Główna,
     * Odkrywaj, YouTube, Live TV, Biblioteka, reszta, Ustawienia.
     */
    fun drawerOrder(c: Context, items: List<com.nuvio.tv.DrawerItem>): List<com.nuvio.tv.DrawerItem> {
        val all = if (!youTubeInMenu(c) || items.any { it.route == "youtube" }) items else {
            val i = items.indexOfFirst { it.route == "library" }.let { if (it < 0) items.size else it + 1 }
            items.toMutableList().apply { add(i, com.nuvio.tv.DrawerItem(route = "youtube", label = "YouTube", icon = androidx.compose.material.icons.Icons.Default.PlayArrow)) }
        }
        if (!com.nuvio.tv.skin.kicia.ForgeUi.ON) return all
        val order = listOf("search", "home", "discover", "youtube", com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE, "library")
        val first = order.mapNotNull { r -> all.firstOrNull { it.route == r } }
        val settings = all.filter { it.route == "settings" }
        return first + all.filter { it !in first && it !in settings } + settings
    }

    /** Ekran startowy (logowanie QR): trzecia droga obok QR i „bez konta” — ustawienia od rodziny kodem 6 cyfr. */
    @Composable
    fun FamilyStartButton() = com.nuvio.tv.fork.family.FamilyStartButton()

    private val VOICE_KEYS = setOf(KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_VOICE_ASSIST, KeyEvent.KEYCODE_ASSIST)

    /** HomeScreen / trasy: true = aktywny skin narysował ekran tej trasy. */
    @Composable
    fun skinScreen(route: String, args: Map<String, Any?>): Boolean {
        val skin = SkinRegistry.skinFor(rememberSkinId()) ?: return false
        return skin.screen(route)?.Content(args) ?: false
    }

    /** PlayerScreen: true = aktywny skin narysował nakładkę (sterowanie i panele) odtwarzacza. */
    @Composable
    fun skinPlayerChrome(uiState: PlayerUiState, args: Map<String, Any?> = emptyMap(), onEvent: (PlayerEvent) -> Unit): Boolean {
        val skin = SkinRegistry.skinFor(rememberSkinId()) ?: return false
        return skin.PlayerChrome(uiState, onEvent, args)
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

    /** PlayerScreen: udostępnia stan i zdarzenia odtwarzacza modułom (napisy nie pasują, głos). */
    @Composable
    fun playerBridge(uiState: PlayerUiState, timeline: kotlinx.coroutines.flow.StateFlow<com.nuvio.tv.ui.screens.player.PlaybackTimelineState>, onEvent: (PlayerEvent) -> Unit) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.runtime.SideEffect { com.nuvio.tv.fork.sources.PlayerBridge.appContext = ctx.applicationContext; com.nuvio.tv.fork.sources.PlayerBridge.set(uiState, onEvent) }
        androidx.compose.runtime.LaunchedEffect(uiState.audioTracks.size, uiState.isPlaying) { com.nuvio.tv.fork.sources.PlayerBridge.maybeAutoLektor(ctx) }
        androidx.compose.runtime.LaunchedEffect(timeline) { timeline.collect { com.nuvio.tv.fork.sources.PlayerBridge.position(it.currentPosition, uiState.isPlaying) } }
        androidx.compose.runtime.DisposableEffect(Unit) { onDispose { com.nuvio.tv.fork.sources.PlayerBridge.clear(); com.nuvio.tv.fork.lektor.LektorController.stop() } }
    }

    /** Tylko build debug: `adb shell am start -a com.nuvio.fork.OPEN -n <activity> --es route <trasa>` otwiera trasę (testy bez klikania). */
    fun onDebugIntent(c: Context, intent: android.content.Intent?) {
        if (!com.nuvio.tv.BuildConfig.APPLICATION_ID.contains("debug") || intent?.action != "com.nuvio.fork.OPEN") return
        // Tylko debug, do pomiarów A/B: --ez previews false (podgląd kanałów), --es module id --ez on false (moduł forka)
        if (intent.hasExtra("previews")) com.nuvio.tv.reshaped.livetv.LiveTvPreferences.setPreviews(c, intent.getBooleanExtra("previews", true))
        intent.getStringExtra("module")?.let { c.getSharedPreferences("fork_features", Context.MODE_PRIVATE).edit().putBoolean("module_$it", intent.getBooleanExtra("on", true)).apply() }
        val route = intent.getStringExtra("route") ?: return
        com.nuvio.tv.ui.screens.settings.ForkFeatures.openDebug(route)
    }

    // --- Rejestr modułów (B00) ---

    /** PlaybackNavHost: trasy włączonych modułów. */
    fun moduleRoutes(builder: NavGraphBuilder, c: Context) {
        ForkRegistry.forContext(c).routes().forEach { it.register(builder) }
        builder.composable(com.nuvio.tv.fork.settings.MODULES_ROUTE) { com.nuvio.tv.fork.settings.ModulesScreen() }
        builder.composable(com.nuvio.tv.fork.settings.TOOLS_ROUTE) { com.nuvio.tv.fork.settings.ToolsHubScreen() }
    }

    /** HomeScreen: rzędy włączonych modułów (rysuje je B03; pusta lista = bez zmian). */
    @Composable
    fun moduleHomeRows(): List<ModuleHomeRow> {
        val c = LocalContext.current
        return remember(c) { ForkRegistry.forContext(c).homeRows() }
    }

    /** ClassicHomeContent: rysuje rzędy modułów pod „Kontynuuj oglądanie” (B03/B08). */
    @Composable
    fun HomeModuleRows() {
        moduleHomeRows().sortedBy { it.order }.forEach { row -> androidx.compose.runtime.key(row.id) { androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(horizontal = 48.dp)) { row.content() } } }
    }

    /** MainActivity: nakładki na całe okno. */
    @Composable
    fun moduleOverlays() {
        val c = LocalContext.current
        remember(c) { com.nuvio.tv.skin.api.SkinPreferences.ensureLoaded(c); com.nuvio.tv.fork.sources.KeRanking.warm(c) } // skórka przed pierwszym ekranem; ranking KE: dekodery i TV
        // Rodzina: przy pokazaniu aplikacji właściciel wysyła zmiany / rodzina pobiera; przy schowaniu właściciel wysyła.
        val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        androidx.compose.runtime.DisposableEffect(owner) {
            val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
                val act = c as? android.app.Activity
                if (e == androidx.lifecycle.Lifecycle.Event.ON_START && act != null) com.nuvio.tv.fork.family.FamilySync.onAppVisible(act)
                if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP) com.nuvio.tv.fork.family.FamilySync.onAppHidden(c)
            }
            owner.lifecycle.addObserver(obs)
            onDispose { owner.lifecycle.removeObserver(obs) }
        }
        androidx.compose.runtime.LaunchedEffect(Unit) { com.nuvio.tv.fork.settings.FirstRunDefaults.run(c) }
        val overlays = remember(c) { ForkRegistry.forContext(c).rootOverlays() }
        // Rysowane nad treścią aplikacji (wcześniej leżały pod nią, bo hook jest pierwszym elementem Box).
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().zIndex(100f)) { overlays.forEach { it() } }
    }

    /** PlayerScreen: klawisze odtwarzacza dla modułów. true = obsłużony. */
    fun modulePlayerKey(c: Context, e: KeyEvent): Boolean {
        if (e.action == KeyEvent.ACTION_DOWN) com.nuvio.tv.fork.sources.PlayerBridge.lastKeyMs = android.os.SystemClock.elapsedRealtime()
        return ForkRegistry.forContext(c).playerHandlers()
            .any { it.onKey(e.keyCode, e.action == KeyEvent.ACTION_DOWN) }
    }
}
