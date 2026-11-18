package com.nuvio.tv.skin.kicia.livetv

import android.content.Context
import android.graphics.Bitmap
import android.view.TextureView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.ui.reshaped.livetv.rememberLiveTvPreviewPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Pictures of what is on each channel, for the cards above the guide and the MiniGuide tile (owner 8.11:
 * "the app goes through the channels and takes screenshots"). One frame per programme, taken only well inside
 * it, never near its start or end, where the adverts are: at least [AFTER_START_MS] after it began and
 * [BEFORE_END_MS] before it ends. Without a guide for the channel, at most one frame every [NO_GUIDE_MS].
 * Frames are small JPEGs in the app's cache (newest per channel kept).
 * Owner 9.11: the pictures refresh in a cycle — a channel gets a new frame every [REFRESH_MS] while its programme
 * stays in the advert-safe window; the focused card's live preview also leaves its last frame behind.
 */
object ForgeChannelSnapshots {
    const val AFTER_START_MS = 8 * 60_000L
    const val BEFORE_END_MS = 5 * 60_000L
    const val NO_GUIDE_MS = 30 * 60_000L
    const val REFRESH_MS = 5 * 60_000L
    /** Channel guide key → when its current frame was taken (this run of the app). */
    private val taken = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private const val INDEX = "index.tsv"

    /** Channel guide key → (programme start or capture time, file). */
    private val _frames = MutableStateFlow<Map<String, Pair<Long, File>>>(emptyMap())
    val frames: StateFlow<Map<String, Pair<Long, File>>> = _frames.asStateFlow()
    private var dir: File? = null

    fun ensure(context: Context) {
        if (dir != null) return
        val d = File(context.cacheDir, "forge_snaps").apply { mkdirs() }
        dir = d
        val index = File(d, INDEX)
        if (!index.exists()) return
        _frames.value = runCatching {
            index.readLines().mapNotNull { line ->
                val f = line.split('\t')
                if (f.size < 3) null else File(d, f[2]).takeIf { it.exists() }?.let { file -> f[0] to ((f[1].toLongOrNull() ?: 0L) to file) }
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    fun fileFor(guideKey: String): File? = _frames.value[guideKey]?.second

    /** Whether a frame may be taken now for [programme] (null = the channel has no guide). */
    fun due(guideKey: String, programme: LiveTvProgramme?, nowMs: Long): Boolean {
        val have = _frames.value[guideKey]?.first
        return if (programme == null) {
            have == null || nowMs - have >= NO_GUIDE_MS
        } else {
            forgeFrameWindowOpen(programme.startEpochMs, programme.stopEpochMs, nowMs) &&
                (have != programme.startEpochMs || nowMs - (taken[guideKey] ?: 0L) >= REFRESH_MS)
        }
    }

    internal fun save(guideKey: String, stamp: Long, bitmap: Bitmap) {
        val d = dir ?: return
        val takenAt = System.currentTimeMillis()
        // The capture time is in the name, so a refreshed frame is a new address (no stale cached picture).
        val name = "s" + Integer.toHexString(guideKey.hashCode()) + "_" + stamp + "_" + takenAt + ".jpg"
        val file = File(d, name)
        runCatching { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) } }.onFailure { return }
        _frames.value[guideKey]?.second?.takeIf { it.name != name }?.delete()
        _frames.value = _frames.value + (guideKey to (stamp to file))
        taken[guideKey] = takenAt
        runCatching { File(d, INDEX).writeText(_frames.value.entries.joinToString("\n") { (k, v) -> "${k.replace('\t', ' ')}\t${v.first}\t${v.second.name}" }) }
    }
}

/** The advert-safe window of a programme: ≥ 8 min after its start and ≥ 5 min before its end. */
internal fun forgeFrameWindowOpen(startMs: Long, stopMs: Long, nowMs: Long): Boolean =
    nowMs - startMs >= ForgeChannelSnapshots.AFTER_START_MS && stopMs - nowMs >= ForgeChannelSnapshots.BEFORE_END_MS

/**
 * Goes through [channels] one by one in an invisible, silent preview player and keeps a frame of each when its
 * programme is in the advert-safe window. Runs only while it is on screen (the guide); stops when a channel is
 * opened or the app leaves (the preview player is released then).
 */
@Composable
internal fun ForgeSnapshotter(channels: List<LiveTvChannel>) {
    if (!com.nuvio.tv.skin.kicia.ForgeUi.CHANNEL_FRAMES) return
    // No preview player at all while every channel's current programme has an EPG picture.
    val guideNow by LiveTvRepository.uiState.collectAsState()
    if (channels.none { guideNow.currentProgrammes[it.guideKey]?.image.isNullOrBlank() }) return
    val context = LocalContext.current
    remember { ForgeChannelSnapshots.ensure(context.applicationContext) }
    val player = rememberLiveTvPreviewPlayer()
    remember(player) { player.soundEnabled = false }
    val view = remember { TextureView(context) }
    DisposableEffect(view) {
        player.attach(view)
        onDispose { player.detach(view) }
    }
    val sizeDp = with(LocalDensity.current) { 320.toDp() }
    val heightDp = with(LocalDensity.current) { 180.toDp() }
    AndroidView(factory = { view }, modifier = Modifier.size(sizeDp, heightDp).graphicsLayer { alpha = 0.01f })
    val current = rememberUpdatedState(channels)
    LaunchedEffect(Unit) {
        delay(3_000) // let the guide settle first
        while (true) {
            val list = current.value
            for (channel in list) {
                val now = LiveTvClock.nowEpochMs()
                val programme = LiveTvRepository.uiState.value.currentProgrammes[channel.guideKey]
                if (!programme?.image.isNullOrBlank()) continue // the EPG has a picture of it: no stream frame needed
                if (!ForgeChannelSnapshots.due(channel.guideKey, programme, now)) continue
                while (ForgeLivePreview.running > 0) delay(1_000) // a focused card plays live: one decoder at a time
                player.play(channel)
                var waited = 0
                while (!player.showingVideo && waited < 10_000) { delay(250); waited += 250 }
                if (player.showingVideo) {
                    delay(2_000) // past the first, often grey or frozen, frames
                    val frame = runCatching { view.getBitmap(320, 180) }.getOrNull()
                    if (frame != null) withContext(Dispatchers.IO) { ForgeChannelSnapshots.save(channel.guideKey, programme?.startEpochMs ?: now, frame) }
                }
                player.stop()
                delay(1_500)
            }
            delay(30_000)
        }
    }
}
