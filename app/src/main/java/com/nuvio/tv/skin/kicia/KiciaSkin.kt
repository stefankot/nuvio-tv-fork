package com.nuvio.tv.skin.kicia

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.skin.api.Skin
import com.nuvio.tv.skin.api.SkinId
import com.nuvio.tv.skin.api.SkinScreen
import com.nuvio.tv.ui.screens.home.ContinueWatchingItem
import com.nuvio.tv.ui.screens.home.HomeUiState
import com.nuvio.tv.ui.screens.home.HomeViewModel
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.PlayerViewModel

/**
 * Wygląd starego forka (nuvio-tv-kicia, `ui/forge`, kopia 1:1 w tym pakiecie; pochodzenie: UPSTREAM_SOURCES.md).
 * Ten plik jest jedynym adapterem: zamienia argumenty wpięć skórki na parametry ekranów Kicii.
 * Ekran, którego tu nie ma, zostaje ekranem Nuvio.
 */
object KiciaSkin : Skin {
    override val id = SkinId.KICIA

    override fun screen(route: String): SkinScreen? = when (route) {
        "home" -> SkinScreen { a -> Home(a); true }
        "search" -> SkinScreen { a ->
            com.nuvio.tv.skin.kicia.search.ForgeSearchScreen(
                a["viewModel"] as com.nuvio.tv.ui.screens.search.SearchViewModel,
                onNavigateToDetail = a["onNavigateToDetail"] as (String, String, String) -> Unit,
            ); true
        }
        "library" -> SkinScreen { a ->
            com.nuvio.tv.skin.kicia.library.ForgeLibraryScreen(onNavigateToDetail = a["onNavigateToDetail"] as (String, String, String?) -> Unit); true
        }
        "youtube" -> SkinScreen { a ->
            com.nuvio.tv.skin.kicia.youtube.ForgeYouTubeScreen(onPlay = a["onPlay"] as (com.nuvio.tv.youtube.YtPlayable) -> Unit); true
        }
        "profiles" -> SkinScreen { a ->
            com.nuvio.tv.skin.kicia.profile.ForgeProfilePicker(
                viewModel = a["viewModel"] as com.nuvio.tv.ui.screens.profile.ProfileSelectionViewModel,
                onAddProfile = a["onAddProfile"] as () -> Unit,
                onProfileSelected = a["onProfileSelected"] as () -> Unit,
                onProfileClicked = a["onProfileClicked"] as () -> Unit,
                onProfileSelectionFailed = a["onProfileSelectionFailed"] as () -> Unit,
                onProfileFocusChanged = a["onProfileFocusChanged"] as ((String?, String?, String?) -> Unit)?,
            ); true
        }
        "wizard_mode" -> SkinScreen { a ->
            com.nuvio.tv.skin.kicia.wizard.ForgeWizardMode(a["focus"] as FocusRequester, onEssential = a["onEssential"] as () -> Unit, onAdvanced = a["onAdvanced"] as () -> Unit); true
        }
        "wizard_layout" -> SkinScreen { a -> WizardLayout(a); true } // wpięcie w LayoutSelectionScreen zdjęte (limit 35 plików)
        else -> null
    }

    /** Krok 2 kreatora Kicii: układy ekranu głównego Nuvio jako wiersze (kopia z Kicia LayoutSelectionScreen, forge hook S16). */
    @Suppress("UNCHECKED_CAST")
    @Composable
    private fun WizardLayout(a: Map<String, Any?>) {
        val selected = a["selected"] as com.nuvio.tv.domain.model.HomeLayout
        val onSelect = a["onSelect"] as (com.nuvio.tv.domain.model.HomeLayout) -> Unit
        com.nuvio.tv.skin.kicia.wizard.ForgeWizardScaffold(step = 2, title = "Układ ekranu głównego", primaryText = "Dalej", primaryFocus = a["focus"] as FocusRequester, onBack = null,
            onPrimary = a["onContinue"] as () -> Unit) {
            listOf(com.nuvio.tv.domain.model.HomeLayout.MODERN to "Nowoczesny", com.nuvio.tv.domain.model.HomeLayout.GRID to "Siatka", com.nuvio.tv.domain.model.HomeLayout.CLASSIC to "Klasyczny").forEach { (layout, name) ->
                com.nuvio.tv.skin.kicia.wizard.ForgeWizardRow(title = name, value = if (selected == layout) "wybrany" else null, selected = selected == layout, onOk = { onSelect(layout) })
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Composable
    private fun Home(a: Map<String, Any?>) {
        val vm = a["viewModel"] as HomeViewModel
        com.nuvio.tv.skin.kicia.home.ForgeHomeContent(
            uiState = a["uiState"] as HomeUiState,
            onNavigateToDetail = a["onNavigateToDetail"] as (String, String, String) -> Unit,
            onContinueWatchingClick = a["onContinueWatchingClick"] as (ContinueWatchingItem) -> Unit,
            onCatalogItemLongPress = a["onCatalogItemLongPress"] as (MetaPreview, String) -> Unit,
            onItemFocus = { vm.onItemFocus(it) },
            trailerPreviewUrls = vm.trailerPreviewUrls,
            trailerPreviewAudioUrls = vm.trailerPreviewAudioUrls,
            onRequestTrailerPreview = { id, title, release, type -> vm.requestTrailerPreview(id, title, release, type) },
        )
    }

    @Suppress("UNCHECKED_CAST")
    @Composable
    override fun PlayerChrome(uiState: PlayerUiState, onEvent: (PlayerEvent) -> Unit, args: Map<String, Any?>): Boolean {
        val vm = args["viewModel"] as? PlayerViewModel ?: return false
        // Kicia: własne sterowanie tylko dla filmów i seriali; Live TV ma swoją nakładkę.
        if (vm.playbackTimeline.value.isLive) return false
        com.nuvio.tv.skin.kicia.player.ForgePlayerControls(
            uiState = uiState,
            viewModel = vm,
            playPauseFocusRequester = args["playPause"] as FocusRequester,
            progressBarFocusRequester = args["progressBar"] as FocusRequester,
            onPlayPause = { onEvent(PlayerEvent.OnPlayPause) },
            onSeekTo = { onEvent(PlayerEvent.OnSeekTo(it)) },
            onShowSubtitleDialog = { onEvent(PlayerEvent.OnShowSubtitleOverlay) },
            onShowAudioDialog = { onEvent(PlayerEvent.OnShowAudioOverlay) },
            onShowSourcesPanel = { onEvent(PlayerEvent.OnShowSourcesPanel) },
            onShowEpisodesPanel = { onEvent(PlayerEvent.OnShowEpisodesPanel) },
            onShowStreamInfo = { onEvent(PlayerEvent.OnShowStreamInfo) },
            onShowSpeedDialog = { onEvent(PlayerEvent.OnShowSpeedDialog) },
            onResetHideTimer = { vm.scheduleHideControls(); vm.onUserInteraction() },
            onSubtitleClearanceChanged = (args["clearance"] as? (Float) -> Unit) ?: {},
        )
        return true
    }
}
