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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val rows = rememberComputed(channels, favUrls, now / (5 * MIN_MS), empty = NiuniaDiscoverCache.last, what = "odkrywaj") {
        val t0 = android.os.SystemClock.elapsedRealtime()
        // UX JMGO (ręczne testy 24.11): w „Ulubione” 4× „Kuchenne rewolucje” (kopie kanału z kilku list), a „Dla ciebie”
        // i „Teraz na żywo” proponowały kanały „Niedostępne” (RT, CNBC). Jeden kafel na program i godzinę, bez niedostępnych.
        discoverRows(channels, favUrls, current, now, titles).map { r ->
            NiuniaDiscoverRow(r.title, r.tiles.filterNot { NiuniaLiveStatus.unavailable(context, it.channel.streamUrl) }
                .distinctBy { t -> t.programme?.let { p -> p.title.trim().lowercase() + "|" + p.startEpochMs } ?: t.channel.streamUrl })
        }.filter { it.tiles.isNotEmpty() }.also {
            NiuniaDiscoverCache.last = it
            Log.i(TAG, "live tv: odkrywaj policzone w tle w ${android.os.SystemClock.elapsedRealtime() - t0} ms")
        }
    }
    val first = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(rows) {
        if (rows == null) return@LaunchedEffect
        Log.i(TAG, "live tv: odkrywaj — rzędy=${rows.size}: " + rows.joinToString { "${it.title}=${it.tiles.size}" })
        if (focused || rows.isEmpty()) return@LaunchedEffect
        delay(60)
        try { first.requestFocus(); focused = true } catch (e: IllegalStateException) { Log.w(TAG, "live tv: fokus odkrywaj: ${e.javaClass.simpleName}: ${e.message}") }
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

/** #77 (26.11): ostatnie rzędy „Odkrywaj” — przy następnym wejściu widać je od razu, zanim policzą się nowe. */
internal object NiuniaDiscoverCache {
    @Volatile var last: List<NiuniaDiscoverRow>? = null
}
