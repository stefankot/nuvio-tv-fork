package com.nuvio.tv.skin.niunia.livetv

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import kotlinx.coroutines.delay

private const val TAG = "ForkNiunia"

/**
 * Live TV → Odkrywaj (canvas `LiveTVDiscover`): rzędy wszystkich zbiorów programów, które aplikacja już liczy
 * ([discoverRows]); krótkie tytuły rzędów, na kafelku sam tytuł (godzina tylko dla przyszłych), znaczniki jednym słowem.
 */
@Composable
internal fun NiuniaLiveDiscover(
    channels: List<LiveTvChannel>,
    favUrls: Set<String>,
    current: Map<String, LiveTvProgramme>,
    now: Long,
    onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit,
) {
    val context = LocalContext.current
    val titles = remember {
        mapOf(
            "fav" to context.getString(R.string.niunia_tv_favourites), "foryou" to context.getString(R.string.niunia_tv_row_foryou),
            "live" to context.getString(R.string.niunia_tv_row_live), "new" to context.getString(R.string.niunia_tv_row_new),
            "recent" to context.getString(R.string.niunia_tv_row_recent), "channels" to context.getString(R.string.niunia_tv_row_channels),
            "recommended" to context.getString(R.string.niunia_tv_row_recommended), "popular" to context.getString(R.string.niunia_tv_row_popular),
            "soon" to context.getString(R.string.niunia_tv_row_soon), "archive" to context.getString(R.string.niunia_tv_row_archive),
        )
    }
    val rows = rememberComputed(channels, favUrls, now / (5 * MIN_MS), empty = null as List<NiuniaDiscoverRow>?, what = "odkrywaj") {
        discoverRows(channels, favUrls, current, now, titles)
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(rows?.size) {
        if (rows == null) return@LaunchedEffect
        Log.i(TAG, "live tv: odkrywaj — rzędy=${rows.size}: " + rows.joinToString { "${it.title}=${it.tiles.size}" })
        delay(60)
        if (rows.isNotEmpty()) try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "live tv: fokus odkrywaj: ${e.javaClass.simpleName}: ${e.message}") }
    }
    when {
        rows == null -> Text(context.getString(R.string.niunia_tv_loading), style = NiuniaText.Body, color = NiuniaColors.Text50, modifier = Modifier.padding(start = NiuniaDims.Gutter, top = 100.dp))
        rows.isEmpty() -> Text(context.getString(R.string.niunia_tv_rail_empty), style = NiuniaText.Body, color = NiuniaColors.Text50, modifier = Modifier.padding(start = NiuniaDims.Gutter, top = 100.dp))
        else -> com.nuvio.tv.skin.niunia.components.NiuniaVerticalScroll(pivot = 42.dp) { // rząd z fokusem zawsze tuż pod przyciskami u góry; lista ucięta pod nimi
            LazyColumn(Modifier.fillMaxSize().padding(top = 80.dp).clipToBounds(), contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)) {
                itemsIndexed(rows, key = { i, r -> "$i${r.title}" }) { i, r ->
                    NiuniaLiveShelf(r.title, r.tiles, longBadge = false, first = if (i == 0) first else null, onPlay = onPlay)
                }
            }
        }
    }
}
