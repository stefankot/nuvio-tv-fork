package com.nuvio.tv.skin.niunia.library

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nuvio.tv.R
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import com.nuvio.tv.ui.screens.library.LibraryViewModel

private const val TAG = "ForkNiunia"

/**
 * Biblioteka Niunii (canvas: Library): siatka plakatów (wariant „baked”: gatunek · ocena IMDb), pusta — dwie linie tekstu na środku.
 * Dane: [LibraryViewModel] Nuvio (widoczne pozycje wg wybranej listy i sortowania).
 */
@Composable
fun NiuniaLibrary(onNavigateToDetail: (String, String, String?) -> Unit, viewModel: LibraryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val items = state.visibleItems
    // UX 22.11: długie OK na pozycji = menu opcji (szczegóły, biblioteka, obejrzane).
    val posterOptions = com.nuvio.tv.skin.niunia.components.rememberNiuniaPosterOptions { id, type, url -> onNavigateToDetail(id, type, url) }
    LaunchedEffect(items.size) { Log.i(TAG, "biblioteka: ${items.size} pozycji") }
    if (items.isEmpty()) {
        // Makieta Library (NContentBlock empty): tytuł 20/26, podpowiedź 14/20 w 70%, przycisk „Przeglądaj filmy i seriale” z fokusem.
        // Pionowo na środku, poziomo od lewej 40 dp (decyzja: nic nie centrujemy w poziomie).
        val browse = remember { androidx.compose.ui.focus.FocusRequester() }
        com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(browse)
        LaunchedEffect(Unit) {
            Log.i(TAG, "biblioteka: pusta, fokus startowy → Przeglądaj")
            try { browse.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "biblioteka: fokus: ${e.javaClass.simpleName}: ${e.message}") }
        }
        Box(Modifier.fillMaxSize().background(com.nuvio.tv.skin.niunia.tokens.NiuniaColor.Bg).padding(start = com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space40), contentAlignment = Alignment.CenterStart) {
            com.nuvio.tv.skin.niunia.components.NiuniaContentBlock(com.nuvio.tv.skin.niunia.components.NiuniaBlockKind.Empty) {
                com.nuvio.tv.skin.niunia.components.NiuniaText(stringResource(R.string.niunia_ds_library_empty), variant = com.nuvio.tv.skin.niunia.components.NiuniaTextVariant.TitleSection)
                com.nuvio.tv.skin.niunia.components.NiuniaText(stringResource(R.string.niunia_ds_library_hint), variant = com.nuvio.tv.skin.niunia.components.NiuniaTextVariant.BodySmall, tone = com.nuvio.tv.skin.niunia.components.NiuniaTone.Secondary)
                com.nuvio.tv.skin.niunia.components.NiuniaButton(
                    onClick = { if (!com.nuvio.tv.skin.niunia.nav.NiuniaNavActions.open("home")) Log.w(TAG, "biblioteka: Przeglądaj — brak przejścia") },
                    label = stringResource(R.string.niunia_ds_library_browse), compact = true, focusRequester = browse,
                    modifier = Modifier.padding(top = com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space8),
                )
            }
        }
        return
    }
    // Z-14 (makiety LibraryShelves / LibrarySort): zakładki, „Sortuj i filtruj”, półki; przy filtrze siatka jak dotąd.
    NiuniaLibraryContent(state, viewModel, posterOptions, onNavigateToDetail)
}
