package com.nuvio.tv.skin.niunia

import com.nuvio.tv.skin.api.Skin
import com.nuvio.tv.skin.api.SkinId
import com.nuvio.tv.skin.api.SkinScreen
import com.nuvio.tv.ui.screens.home.ContinueWatchingItem
import com.nuvio.tv.ui.screens.home.HomeUiState
import com.nuvio.tv.ui.screens.home.HomeViewModel

/**
 * Niunia — własny projekt właściciela (canvas „Nuvio TV Apple TV Style App”, specyfikacja ~/Desktop/appletv/skin/SKIN.md).
 * Jedyny adapter: zamienia argumenty wpięć skórki na parametry ekranów Niunii.
 * Etap 1–2: ekran główny, wyszukiwarka, biblioteka. Pozostałe trasy (profiles — PIN-y, youtube, odtwarzacz) zostają ekranami Nuvio.
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
        else -> null
    }
}
