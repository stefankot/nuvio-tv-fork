package com.nuvio.tv.skin.kicia.livetv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.tv.skin.kicia.components.ForgeButton
import com.nuvio.tv.skin.kicia.components.forgeEdge
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.reshaped.livetv.LiveTvPlayerState

/**
 * Makieta 7.7 E25 + 7.7b H10: at the end of a programme from the archive, a card „Następny odcinek za 10 s”: the frame
 * 240×135 on the left; on the right the icon and the countdown 14/700/70%, the title 20/700 (2 lines), „S2 · O5 · wt 20:40”
 * 14/70% and „Odtwórz” (focus). No next episode: „Dalej na {kanał} za 10 s” and what really follows. The MiniGuide is
 * hidden meanwhile; Back cancels and shows it.
 */
@Composable
internal fun ForgeArchiveEndCard(state: LiveTvPlayerState) {
    val ended = state.archiveEnd ?: return
    val (channel, next) = remember(ended) { state.nextAfterArchiveEnd(ended) } ?: return
    val sameShow = next.title.trim().equals(ended.title.trim(), ignoreCase = true)
    var left by remember(ended) { mutableIntStateOf(10) }
    val play = remember { FocusRequester() }
    LaunchedEffect(ended) {
        runCatching { play.requestFocus() }
        while (left > 0) { kotlinx.coroutines.delay(1_000); left-- }
        state.playAfterArchiveEnd()
    }
    BackHandler { state.archiveEnd = null; state.showInfo() }
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.BottomEnd) {
        Row(
            Modifier.clip(shape).background(ForgeColors.GlassFallback).forgeEdge(shape).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(240.dp, 135.dp).clip(RoundedCornerShape(8.dp)).background(ForgeColors.CardBase)) {
                next.image?.takeIf { it.isNotBlank() }?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            }
            Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.SkipNext, null, tint = ForgeColors.Text2, modifier = Modifier.size(24.dp))
                    Text(
                        (if (sameShow) "Następny odcinek" else "Dalej na " + channel.name) + " za $left s",
                        style = ForgeText.label.copy(color = ForgeColors.Text2),
                    )
                }
                Text(next.title, style = ForgeText.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(forgeEpisodeShort(next.episode)?.replace(" ", " · "), forgeShortDay(next.startEpochMs) + " " + forgeTvTime(next.startEpochMs)).joinToString(" · "),
                    style = ForgeText.label.copy(color = ForgeColors.Text2), maxLines = 1,
                )
                ForgeButton(text = "Odtwórz", icon = Icons.Filled.PlayArrow, primary = true, focusRequester = play, onOk = { state.playAfterArchiveEnd() })
            }
        }
    }
}
