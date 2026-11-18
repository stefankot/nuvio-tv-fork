@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.fork.tv.ekrany

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.fork.tv.Badge
import com.nuvio.tv.fork.tv.most.ResolvedItem
import com.nuvio.tv.fork.tv.most.TvQueue
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.delay

/**
 * Pozycje „Dla Ciebie” (odświeżane co minutę); pusta lista, gdy moduł TV albo rząd wyłączony.
 * Skórka Kicia wstawia je do własnej półki kart (19.11: osobny rząd ucinał przyciski przewodnika).
 */
@Composable
fun rememberForYouItems(): List<ResolvedItem> {
    val context = LocalContext.current
    if (!ForkFeatures.enabled(context, "module_tv", true) || !ForkFeatures.enabled(context, "tv_for_you_row", true)) return emptyList()
    var items by remember { mutableStateOf<List<ResolvedItem>>(emptyList()) }
    LaunchedEffect(Unit) { while (true) { items = runCatching { TvQueue.build() }.getOrDefault(emptyList()); delay(60_000L) } }
    return items
}

fun forYouBadgeLabel(b: Badge) = badgeLabel(b)

/** Rząd „Dla Ciebie” nad przewodnikiem (B03-1/2). Pusty lub wyłączony = nic nie rysuje. */
@Composable
fun ForYouRow(onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit) {
    val items = rememberForYouItems()
    if (items.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text("Dla Ciebie", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 24.dp)) {
            items(items, key = { it.item.programKey + "|" + it.item.guideKey }) { r ->
                Card(onClick = { onPlay(r.channel, r.programme) }, onLongClick = { ForkFeatures.open?.invoke("fork_program/" + android.net.Uri.encode(r.item.programKey)) }, modifier = Modifier.width(220.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(badgeLabel(r.item.badge), style = MaterialTheme.typography.labelSmall)
                        Text(r.item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                        Text(r.channel.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun badgeLabel(b: Badge) = when (b) { Badge.RESUME -> "Wznów"; Badge.NEW_EPISODE -> "Nowy odcinek"; Badge.LIVE -> "Na żywo" }
