package com.nuvio.tv.skin.niunia.livetv

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.kicia.livetv.forgeChannelLogo
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaText

/**
 * Paczka „oczywiste” pkt 5 (21.11): kanały bez logo i bez nazwy. Przyczyna: adres epg.ovh jest budowany z nazwy kanału
 * (`forgeChannelLogo` → `epgOvhLogo`) także wtedy, gdy takiego pliku nie ma — obrazek po cichu się nie wczytywał,
 * a nazwa kanału nie pojawiała się, bo „logo było”. Teraz po kolei: logo EPG/paczki, logo z przewodnika, logo z playlisty;
 * gdy żadne się nie wczyta — nazwa kanału.
 */
internal fun niuniaLogoChain(channel: LiveTvChannel): List<String> = listOfNotNull(
    forgeChannelLogo(channel),
    LiveTvRepository.uiState.value.logoFor(channel),
    channel.logoUrl?.takeIf { it.isNotBlank() },
).distinct()

private val failedLogos = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

@Composable
internal fun NiuniaChannelLogo(
    channel: LiveTvChannel,
    modifier: Modifier = Modifier.fillMaxSize(),
    nameStyle: TextStyle = NiuniaText.Button,
    alignment: Alignment = Alignment.Center,
) {
    val chain = remember(channel.streamUrl, channel.logoUrl) { niuniaLogoChain(channel) }
    var index by remember(chain) { mutableIntStateOf(chain.indexOfFirst { it !in failedLogos }.let { if (it < 0) chain.size else it }) }
    val url = chain.getOrNull(index)
    if (url == null) {
        Box(modifier, contentAlignment = alignment) {
            androidx.compose.material3.Text(com.nuvio.tv.fork.tv.names.ChannelNames.shown(channel.name), style = nameStyle, color = NiuniaColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
        return
    }
    AsyncImage(
        model = url, contentDescription = channel.name, contentScale = ContentScale.Fit, alignment = alignment, modifier = modifier,
        onError = { st ->
            failedLogos += url
            Log.w("ForkNiunia", "logo kanału ${channel.name}: ${st.result.throwable.javaClass.simpleName}: ${st.result.throwable.message} → ${if (index + 1 < chain.size) "następne" else "nazwa"}")
            index += 1
        },
    )
}
