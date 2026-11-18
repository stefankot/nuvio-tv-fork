package com.nuvio.tv.skin.kicia.home

import com.nuvio.tv.skin.kicia.livetv.forgeChannelLogo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.nuvio.tv.fork.tv.TvStore
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.kicia.livetv.ForgeSnapshotter
import com.nuvio.tv.skin.kicia.youtube.forgeSharedYouTubeViewModel
import com.nuvio.tv.ui.reshaped.livetv.LiveTvScreenModel
import kotlinx.coroutines.delay

/**
 * Owner 9.11: once the open tab has its content, the other tabs are made ready in the background, so they open
 * at once: the Live TV channel list and guide, frames of the recent channels (silent, off screen), channel logos,
 * and the YouTube feed with its first thumbnails (the YouTube tab and the Home shelf share that feed).
 * Starts a few seconds after [ready]; the frame grabber waits longer, it decodes video.
 */
@Composable
fun ForgeBackgroundPreload(ready: Boolean) {
    if (!com.nuvio.tv.skin.kicia.ForgeUi.EAGER_BACKGROUND_LOAD) return
    var started by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ready) {
        if (ready && !started) { delay(4_000); started = true }
    }
    if (!started) return
    val context = LocalContext.current

    // Live TV: channels + guide into the shared repository.
    val live: LiveTvScreenModel = hiltViewModel(key = "forge_preload_live")
    remember(live) { live.ensureLoaded() }

    // YouTube: the shared feed and its first thumbnails.
    val youtube = forgeSharedYouTubeViewModel()
    val yt by youtube.state.collectAsState()
    LaunchedEffect(yt.grid.size) {
        val loader = SingletonImageLoader.get(context)
        yt.grid.take(18).mapNotNull { it.thumbnail?.takeIf(String::isNotBlank) }.forEach { url ->
            loader.enqueue(ImageRequest.Builder(context).data(url).build())
        }
    }

    // TV: logos of the channels the guide shows first, then frames of the recent ones.
    val tv by LiveTvRepository.uiState.collectAsState()
    val recent by TvStore.recent.collectAsState()
    val channels: List<LiveTvChannel> = remember(tv.channels, recent) {
        val byUrl = tv.channels.associateBy { it.streamUrl }
        val first = recent.mapNotNull { byUrl[it.channelUrl] ?: tv.channels.firstOrNull { c -> c.guideKey == it.guideKey } }
        (first + tv.channels.take(12)).distinctBy { it.streamUrl }.take(14)
    }
    LaunchedEffect(channels.size) {
        val loader = SingletonImageLoader.get(context)
        channels.mapNotNull { forgeChannelLogo(it) }.forEach { url ->
            loader.enqueue(ImageRequest.Builder(context).data(url).build())
        }
    }
    var frames by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(20_000); frames = true }
    if (frames && channels.isNotEmpty()) ForgeSnapshotter(channels)
}
