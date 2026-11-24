package com.nuvio.tv.skin.niunia.library

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.WatchedItem
import com.nuvio.tv.skin.niunia.components.NiuniaBlockKind
import com.nuvio.tv.skin.niunia.components.NiuniaCollectionHeader
import com.nuvio.tv.skin.niunia.components.NiuniaContentBlock
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing

private const val TAG = "ForkNiunia"

/**
 * Z-14 (makieta LibraryHistory): zakładka „Historia” w Bibliotece — rzędy „Dziś”, „W tym tygodniu”, „Wcześniej” z obejrzanych
 * tytułów (dane: [com.nuvio.tv.skin.kicia.library.ForgeHistoryViewModel], te same co historia Kicii). OK = strona tytułu,
 * długie OK = usuń z historii (okno Niuni). Pusta historia: „Tu pojawią się tytuły, które zaczniesz oglądać”.
 * Rząd „Ukryte z »Oglądaj dalej«” z makiety pominięty: baza nie przechowuje ukrytych pozycji jako listy.
 */
@Composable
internal fun NiuniaLibraryHistory(
    first: FocusRequester,
    onOpen: (WatchedItem) -> Unit,
    vm: com.nuvio.tv.skin.kicia.library.ForgeHistoryViewModel = hiltViewModel(),
) {
    val ctx = LocalContext.current
    val history by vm.history.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf<WatchedItem?>(null) }
    val groups = remember(history) {
        val cal = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
        val today = cal.timeInMillis
        val week = today - 6 * 24 * 3_600_000L
        val sorted = history.sortedByDescending { it.watchedAt }
        listOf(
            R.string.niunia_tv2_hist_today to sorted.filter { it.watchedAt >= today },
            R.string.niunia_tv2_hist_week to sorted.filter { it.watchedAt in week until today },
            R.string.niunia_tv2_hist_earlier to sorted.filter { it.watchedAt < week },
        ).filter { it.second.isNotEmpty() }
    }
    LaunchedEffect(history.size) { Log.i(TAG, "biblioteka: historia ${history.size} pozycji, rzędy=${groups.size}") }
    if (history.isEmpty()) {
        NiuniaContentBlock(NiuniaBlockKind.Empty, Modifier.padding(start = NiuniaDims.Gutter, top = NiuniaSpacing.Space40)) {
            NiuniaText(stringResource(R.string.niunia_tv2_hist_empty), variant = NiuniaTextVariant.Body, tone = NiuniaTone.Secondary, lines = 2)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = NiuniaSpacing.Space16, bottom = NiuniaSpacing.Space40), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space24)) {
        groups.forEachIndexed { gi, (title, list) ->
            item(key = "hist_$gi") {
                Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
                    NiuniaCollectionHeader(stringResource(title), active = true, modifier = Modifier.padding(start = NiuniaDims.Gutter).height(NiuniaSpacing.Space24),
                        sub = list.size.toString())
                    LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                        itemsIndexed(list, key = { _, w -> "${gi}_${w.contentId}_${w.season}_${w.episode}" }) { i, w ->
                            val ep = if (w.season != null && w.episode != null) "S${w.season} E${w.episode}" else null
                            NiuniaPoster(
                                art = w.poster, fallback = null, title = w.title, logo = null,
                                meta = listOfNotNull(ep, com.nuvio.tv.skin.kicia.library.forgeWatchedLabel(w.watchedAt)).joinToString(" · "),
                                label = null, baked = false,
                                onOk = { Log.i(TAG, "biblioteka: historia → ${w.contentType} ${w.contentId}"); onOpen(w) },
                                onLongOk = { Log.i(TAG, "biblioteka: historia, długie OK → ${w.contentId}"); menu = w },
                                modifier = if (gi == 0 && i == 0) Modifier.focusRequester(first) else Modifier,
                            )
                        }
                    }
                }
            }
        }
    }
    val m = menu
    if (m != null) com.nuvio.tv.skin.niunia.components.NiuniaActionsDialog(
        eyebrow = ctx.getString(R.string.niunia_tv2_hist_tab), title = m.title, state = com.nuvio.tv.skin.kicia.library.forgeWatchedLabel(m.watchedAt),
        onDismiss = { menu = null },
        actions = listOf(
            com.nuvio.tv.skin.niunia.components.NiuniaDialogAction(ctx.getString(R.string.niunia_ke_details_movie), icon = "info", chevron = true) { onOpen(m) },
            com.nuvio.tv.skin.niunia.components.NiuniaDialogAction(ctx.getString(R.string.niunia_tv2_hist_remove)) { Log.i(TAG, "biblioteka: usuń z historii ${m.contentId}"); vm.remove(m) },
        ),
    )
}
