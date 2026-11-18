package com.nuvio.tv.skin.kicia.livetv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.kicia.components.ForgeButton
import com.nuvio.tv.skin.kicia.keyboard.ForgeKeyboardLayout
import com.nuvio.tv.skin.kicia.keyboard.ForgeOnScreenKeyboard
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/**
 * v6 S18 "Szukaj" of Live TV, EPG only: types on the 6-column board (left), results on the right —
 * programmes on air now and channels whose name matches (offline, no API). OK plays the channel.
 */
@Composable
fun ForgeTvSearch(
    channels: List<LiveTvChannel>,
    onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val state by LiveTvRepository.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstKey.requestFocus() } }
    val q = query.trim().lowercase()
    val hits = remember(q, channels, state.currentProgrammes) { forgeTvSearchHits(q, channels, state.currentProgrammes) }
    Box(Modifier.fillMaxSize().background(ForgeColors.Bg).padding(start = 40.dp, top = 96.dp, end = 40.dp, bottom = 24.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.width(380.dp)) {
                Text(query.ifEmpty { "Szukaj w TV" }, style = ForgeText.head, maxLines = 1, modifier = Modifier.padding(bottom = 12.dp))
                ForgeOnScreenKeyboard(
                    text = query, onTextChange = { query = it }, onSearch = {},
                    layout = ForgeKeyboardLayout.Search6, firstKeyFocus = firstKey,
                )
            }
            Column(Modifier.fillMaxWidth()) {
                if (q.length < 2) {
                    Text("Wpisz co najmniej 2 litery: programy emitowane teraz i nazwy kanałów.", style = ForgeText.body)
                } else if (hits.isEmpty()) {
                    Text("Brak wyników dla „$query”.", style = ForgeText.body)
                } else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(hits, key = { it.first.streamUrl + it.third }) { (ch, prog, kind) ->
                        ForgeButton(
                            text = (prog?.title ?: ch.name) + "  ·  " + (if (prog != null) ch.name else kind),
                            onOk = { onPlay(ch, null); onClose() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** Hits of the Live TV search: programmes on air now whose title contains [q], then channels whose name does (q lower-case, ≥ 2 letters). */
internal fun forgeTvSearchHits(
    q: String,
    channels: List<LiveTvChannel>,
    current: Map<String, LiveTvProgramme>,
): List<Triple<LiveTvChannel, LiveTvProgramme?, String>> {
    if (q.length < 2) return emptyList()
    return buildList {
        channels.forEach { ch ->
            val p = current[ch.guideKey]
            if (p != null && p.title.lowercase().contains(q)) add(Triple(ch, p, "Teraz na żywo"))
        }
        channels.forEach { ch -> if (ch.name.lowercase().contains(q)) add(Triple(ch, null, "Kanał")) }
    }.take(40)
}
