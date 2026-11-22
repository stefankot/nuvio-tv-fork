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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nuvio.tv.R
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.components.niuniaPortrait
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
    val posterPattern = remember(state.customPosterUrlPattern, state.customPosterEnabledScreens) {
        com.nuvio.tv.core.poster.patternForScreen(state.customPosterUrlPattern, com.nuvio.tv.core.poster.CustomPosterScreen.LIBRARY, state.customPosterEnabledScreens)
    }
    LaunchedEffect(items.size) {
        Log.i(TAG, "biblioteka: ${items.size} pozycji, bez plakatu=${items.count { it.poster.isNullOrBlank() }}, poziome=${items.count { it.posterShape != com.nuvio.tv.domain.model.PosterShape.POSTER }}")
    }
    if (items.isEmpty()) {
        // Pkt 3 (canvas v86): bez centrowania w poziomie — komunikat od lewej (40 dp).
        Box(Modifier.fillMaxSize().background(NiuniaColors.Bg).padding(start = NiuniaDims.Gutter), contentAlignment = Alignment.CenterStart) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(stringResource(R.string.niunia_library_empty), style = NiuniaText.Meta.copy(fontSize = NiuniaText.Meta.fontSize), textAlign = TextAlign.Start)
                Text(stringResource(R.string.niunia_library_empty_hint), style = NiuniaText.Body, color = NiuniaColors.Text50, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 4.dp))
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(NiuniaDims.PosterW),
        modifier = Modifier.fillMaxSize().background(NiuniaColors.Bg),
        contentPadding = PaddingValues(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter, top = 84.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        items(items, key = { "${it.type}_${it.id}" }) { entry ->
            val rating = entry.imdbRating?.takeIf { it > 0f }?.let { "★ %.1f".format(java.util.Locale.US, it) }
            // Właściciel 22.11: brakowało plakatów — teraz z zapasem (oryginał, gdy wzór z ustawień nie da obrazu; Cinemeta po IMDb,
            // gdy Trakt/MDBList nie podał plakatu) i pionowy plakat dla pozycji poziomych.
            val (art, fallback) = remember(entry, posterPattern) {
                niuniaPortrait(entry.toMetaPreview().copy(imdbId = entry.imdbId), posterPattern)
            }
            NiuniaPoster(
                art = art, fallback = fallback, title = entry.name, logo = entry.logo,
                meta = listOfNotNull(entry.genres.firstOrNull(), rating).joinToString(" · ").ifBlank { null },
                label = null, baked = art != null,
                onOk = { onNavigateToDetail(entry.id, entry.type, entry.addonBaseUrl) },
            )
        }
    }
}
