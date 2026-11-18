package com.nuvio.tv.skin.kicia.livetv

import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.ui.reshaped.livetv.rememberLiveTvPreviewPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How many live card previews play right now; the background frame grabber waits while any does. */
object ForgeLivePreview {
    @Volatile var running: Int = 0
}

/**
 * Owner 9.11: the focused programme card shows its channel live, without sound. Starts after a short pause
 * (scrolling past a card plays nothing), fades in once the picture runs, and on leaving keeps its last frame
 * as the card's picture (only inside the advert-safe window of the programme).
 */
@Composable
internal fun ForgeLiveCardVideo(channel: LiveTvChannel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = rememberLiveTvPreviewPlayer()
    remember(player) { player.soundEnabled = false }
    val view = remember { TextureView(context) }
    DisposableEffect(view) {
        player.attach(view)
        ForgeLivePreview.running++
        onDispose {
            if (player.showingVideo) {
                val now = LiveTvClock.nowEpochMs()
                val programme = LiveTvRepository.uiState.value.currentProgrammes[channel.guideKey]
                val safe = programme == null || forgeFrameWindowOpen(programme.startEpochMs, programme.stopEpochMs, now)
                val frame = if (safe) runCatching { view.getBitmap(320, 180) }.getOrNull() else null
                if (frame != null) CoroutineScope(Dispatchers.IO).launch {
                    ForgeChannelSnapshots.save(channel.guideKey, programme?.startEpochMs ?: now, frame)
                }
            }
            player.detach(view)
            ForgeLivePreview.running = (ForgeLivePreview.running - 1).coerceAtLeast(0)
        }
    }
    LaunchedEffect(channel.guideKey) {
        delay(700)
        player.play(channel)
    }
    AndroidView(factory = { view }, modifier = modifier.graphicsLayer { alpha = if (player.showingVideo) 1f else 0f })
}
