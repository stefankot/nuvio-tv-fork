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
    val posterPattern = remember(state.customPosterUrlPattern, state.customPosterEnabledScreens) {
        com.nuvio.tv.core.poster.patternForScreen(state.customPosterUrlPattern, com.nuvio.tv.core.poster.CustomPosterScreen.LIBRARY, state.customPosterEnabledScreens)
    }
    LaunchedEffect(items.size) {
        Log.i(TAG, "biblioteka: plakaty — bez plakatu=${items.count { it.poster.isNullOrBlank() }}, poziome=${items.count { it.posterShape != com.nuvio.tv.domain.model.PosterShape.POSTER }}")
    }
    // Test JMGO build151: bez requestera wyjście z menu robiło „w prawo” i fokus lądował na 2. plakacie → 1. plakat.
    val first = remember { androidx.compose.ui.focus.FocusRequester() }
    com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(first)
    // Test JMGO build151: ▲ z górnego rzędu (i ◀ z pierwszej kolumny) nie otwierał menu — siatka zjadała klawisz.
    val grid = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    var focusedIndex by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(NiuniaDims.PosterW),
        state = grid,
        modifier = Modifier.fillMaxSize().background(NiuniaColors.Bg).onPreviewKeyEvent { e ->
            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val info = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.index == focusedIndex } ?: return@onPreviewKeyEvent false
            when {
                e.key == Key.DirectionUp && info.row == 0 -> com.nuvio.tv.skin.niunia.nav.NiuniaNavActions.toMenu("biblioteka ▲")
                e.key == Key.DirectionLeft && info.column == 0 -> com.nuvio.tv.skin.niunia.nav.NiuniaNavActions.toMenu("biblioteka ◀")
                else -> false
            }
        },
        contentPadding = PaddingValues(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter, top = 84.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        itemsIndexed(items, key = { _, it -> "${it.type}_${it.id}" }) { index, entry ->
            val rating = entry.imdbRating?.takeIf { it > 0f }?.let { "★ %.1f".format(java.util.Locale.US, it) }
            // Właściciel 22.11: brakowało plakatów — teraz z zapasem (oryginał, gdy wzór z ustawień nie da obrazu; Cinemeta po IMDb,
            // gdy Trakt/MDBList nie podał plakatu) i pionowy plakat dla pozycji poziomych.
            val (art, fallback) = remember(entry, posterPattern) {
                com.nuvio.tv.skin.niunia.components.niuniaPortrait(entry.toMetaPreview().copy(imdbId = entry.imdbId), posterPattern)
            }
            NiuniaPoster(
                art = art, fallback = fallback, title = entry.name, logo = entry.logo,
                meta = listOfNotNull(com.nuvio.tv.skin.niunia.components.NiuniaGenres.pl(entry.genres.firstOrNull()), rating).joinToString(" · ").ifBlank { null },
                label = null, baked = art != null,
                onOk = { onNavigateToDetail(entry.id, entry.type, entry.addonBaseUrl) },
                modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                onFocus = { if (it) focusedIndex = index },
            )
        }
    }
}
