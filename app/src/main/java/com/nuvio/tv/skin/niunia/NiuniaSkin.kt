package com.nuvio.tv.skin.niunia

import com.nuvio.tv.skin.api.Skin
import com.nuvio.tv.skin.api.SkinId
import com.nuvio.tv.skin.api.SkinScreen
import com.nuvio.tv.ui.screens.home.ContinueWatchingItem
import com.nuvio.tv.ui.screens.home.HomeUiState
import com.nuvio.tv.ui.screens.home.HomeViewModel
import com.nuvio.tv.ui.screens.home.togglePosterLibrary

/**
 * Niunia — własny projekt właściciela (canvas „Nuvio TV Apple TV Style App”, specyfikacja ~/Desktop/appletv/skin/SKIN.md).
 * Jedyny adapter: zamienia argumenty wpięć skórki na parametry ekranów Niunii.
 * Etap 1–2: ekran główny, wyszukiwarka, biblioteka. Etap 3: profile (z PIN-em), nakładka odtwarzacza, strona filmu/serialu (wpięcie „detail” w MetaDetailsScreen). YouTube zostaje ekranem Nuvio.
 */
object NiuniaSkin : Skin {
    override val id = SkinId.NIUNIA

    @Suppress("UNCHECKED_CAST")
    override fun screen(route: String): SkinScreen? = when (route) {
        "home" -> SkinScreen { a ->
            val vm = a["viewModel"] as HomeViewModel
            com.nuvio.tv.skin.niunia.home.NiuniaHome(
                uiState = a["uiState"] as HomeUiState,
                onNavigateToDetail = a["onNavigateToDetail"] as (String, String, String) -> Unit,
                onContinueWatchingClick = a["onContinueWatchingClick"] as (ContinueWatchingItem) -> Unit,
                onItemFocus = { vm.onItemFocus(it) },
                onToggleLibrary = { item, addon -> vm.togglePosterLibrary(item, addon) },
                // UX 22.11: długie OK — menu plakatu bazy (HomeScreen rysuje okno) i menu „Oglądaj dalej”.
                onItemLongPress = (a["onCatalogItemLongPress"] as? (com.nuvio.tv.domain.model.MetaPreview, String) -> Unit) ?: { _, _ -> },
                cwActions = androidx.compose.runtime.remember(vm, a["showContinueWatchingManualPlay"]) { niuniaCwActions(vm, a) },
            ); true
        }
        "search" -> SkinScreen { a ->
            com.nuvio.tv.skin.niunia.search.NiuniaSearch(
                viewModel = a["viewModel"] as com.nuvio.tv.ui.screens.search.SearchViewModel,
                onNavigateToDetail = a["onNavigateToDetail"] as (String, String, String) -> Unit,
            ); true
        }
        "library" -> SkinScreen { a ->
            com.nuvio.tv.skin.niunia.library.NiuniaLibrary(onNavigateToDetail = a["onNavigateToDetail"] as (String, String, String?) -> Unit); true
        }
        "profiles" -> SkinScreen { a ->
            com.nuvio.tv.skin.niunia.profile.NiuniaProfiles(
                viewModel = a["viewModel"] as com.nuvio.tv.ui.screens.profile.ProfileSelectionViewModel,
                onAddProfile = a["onAddProfile"] as () -> Unit,
                onProfileSelected = a["onProfileSelected"] as () -> Unit,
                onProfileClicked = a["onProfileClicked"] as () -> Unit,
                onProfileSelectionFailed = a["onProfileSelectionFailed"] as () -> Unit,
                onProfileFocusChanged = a["onProfileFocusChanged"] as ((String?, String?, String?) -> Unit)?,
            ); true
        }
        "detail" -> SkinScreen { a ->
            com.nuvio.tv.skin.niunia.detail.NiuniaDetail(
                meta = a["meta"] as com.nuvio.tv.domain.model.Meta,
                uiState = a["uiState"] as com.nuvio.tv.ui.screens.detail.MetaDetailsUiState,
                viewModel = a["viewModel"] as com.nuvio.tv.ui.screens.detail.MetaDetailsViewModel,
                heroVideo = a["heroVideo"] as com.nuvio.tv.domain.model.Video?,
                playEpisode = a["playEpisode"] as (com.nuvio.tv.domain.model.Video) -> Unit,
                playTitle = a["playTitle"] as (String) -> Unit,
                onNavigateToDetail = a["onNavigateToDetail"] as (String, String, String?) -> Unit,
                onNavigateToCastDetail = a["onNavigateToCastDetail"] as (Int, String, Boolean) -> Unit,
            ); true
        }
        "person" -> SkinScreen { a ->
            com.nuvio.tv.skin.niunia.person.NiuniaPerson(
                person = a["person"] as com.nuvio.tv.domain.model.PersonDetail,
                bioExpanded = a["bioExpanded"] as Boolean,
                onBioExpanded = a["onBioExpanded"] as (Boolean) -> Unit,
                onNavigateToDetail = a["onNavigateToDetail"] as (String, String, String?) -> Unit,
            ); true
        }
        "livetv" -> SkinScreen { a ->
            com.nuvio.tv.skin.niunia.livetv.NiuniaLiveTv(onPlay = a["onPlay"] as (String) -> Unit); true
        }
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    private fun niuniaCwActions(vm: HomeViewModel, a: Map<String, Any?>): com.nuvio.tv.skin.niunia.home.NiuniaCwActions {
        val click = a["onContinueWatchingClick"] as (ContinueWatchingItem) -> Unit
        return com.nuvio.tv.skin.niunia.home.NiuniaCwActions(
            remove = { cw ->
                val (id, s, e) = when (cw) {
                    is ContinueWatchingItem.InProgress -> Triple(cw.progress.contentId, cw.progress.season, cw.progress.episode)
                    is ContinueWatchingItem.NextUp -> Triple(cw.info.contentId, cw.info.seedSeason, cw.info.seedEpisode)
                }
                vm.onEvent(com.nuvio.tv.ui.screens.home.HomeEvent.OnRemoveContinueWatching(id, s, e, cw is ContinueWatchingItem.NextUp))
            },
            markWatched = { cw -> vm.onEvent(com.nuvio.tv.ui.screens.home.HomeEvent.OnMarkContinueWatchingWatched(cw)) },
            fromStart = (a["onContinueWatchingStartFromBeginning"] as? (ContinueWatchingItem) -> Unit) ?: click,
            playManually = (a["onContinueWatchingPlayManually"] as? (ContinueWatchingItem) -> Unit) ?: click,
            showManual = a["showContinueWatchingManualPlay"] as? Boolean ?: false,
        )
    }

    /** Nakładka odtwarzacza Niunii: filmy, seriale i YouTube. Live TV: wygląd Niuni rysuje MiniGuide (skin/niunia/live). */
    @Suppress("UNCHECKED_CAST")
    @androidx.compose.runtime.Composable
    override fun PlayerChrome(uiState: com.nuvio.tv.ui.screens.player.PlayerUiState, onEvent: (com.nuvio.tv.ui.screens.player.PlayerEvent) -> Unit, args: Map<String, Any?>): Boolean {
        val vm = args["viewModel"] as? com.nuvio.tv.ui.screens.player.PlayerViewModel ?: return false
        if (vm.playbackTimeline.value.isLive) return false
        // Fork 20.11: YouTube (karta YouTube, zwiastuny) — własny odtwarzacz Niuni wg canvasu PlayerLeanback.
        val ytSession = com.nuvio.tv.skin.kicia.youtube.LocalForgeYouTubeSession.current ?: com.nuvio.tv.skin.kicia.youtube.ForgeYouTubeHost.session
        if (com.nuvio.tv.skin.niunia.player.niuniaIsYouTube(vm.controller.contentId, ytSession)) {
            com.nuvio.tv.skin.niunia.player.NiuniaYouTubeChrome(
                uiState = uiState,
                viewModel = vm,
                session = ytSession,
                progressBarFocusRequester = args["progressBar"] as androidx.compose.ui.focus.FocusRequester,
                playPauseFocusRequester = args["playPause"] as androidx.compose.ui.focus.FocusRequester,
                onEvent = onEvent,
                onSubtitleClearanceChanged = (args["clearance"] as? (Float) -> Unit) ?: {},
            )
            return true
        }
        com.nuvio.tv.skin.niunia.player.NiuniaPlayerChrome(
            uiState = uiState,
            viewModel = vm,
            playPauseFocusRequester = args["playPause"] as androidx.compose.ui.focus.FocusRequester,
            progressBarFocusRequester = args["progressBar"] as androidx.compose.ui.focus.FocusRequester,
            onEvent = onEvent,
            onSubtitleClearanceChanged = (args["clearance"] as? (Float) -> Unit) ?: {},
        )
        return true
    }
}
