package com.nuvio.tv.skin.niunia.livetv

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.skin.niunia.components.NiuniaTag
import com.nuvio.tv.skin.niunia.components.NiuniaTagKind
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor

private const val TAG = "ForkNiunia"

/**
 * Z-21 (P-32, makieta LiveTVRow: `NHero kind="preview"`): podgląd kanału na żywo u góry po prawej, gdy fokus jest
 * w przewodniku. Jeden odtwarzacz na cały ekran (Android 11, JMGO: bez drugiego dekodera) — przy zmianie kanału ten sam
 * odtwarzacz dostaje nowy strumień; przed pełnym ekranem ([release] w `play`) i przy wyjściu z przewodnika jest zwalniany.
 * Bez dźwięku, start po [DELAY_MS] postoju fokusu na kanale (szybkie przewijanie nie włącza strumieni). Kanały Stalker
 * (link tworzony przy odtwarzaniu) i błąd strumienia: zamiast obrazu kadr programu albo logo. Przełącznik
 * `niunia_live_preview` (domyślnie włączony).
 */
internal object NiuniaLivePreview {
    const val KEY = "niunia_live_preview"
    const val DELAY_MS = 1_200L
    private var player: ExoPlayer? = null
    private var url: String? = null

    fun enabled(c: Context) = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, KEY, true)

    /** Odtwarzacz podglądu (tworzony przy pierwszym kanale), strumień [ch] bez dźwięku. */
    fun play(c: Context, ch: LiveTvChannel, listener: Player.Listener): ExoPlayer? {
        val p = player ?: try {
            ExoPlayer.Builder(c.applicationContext).build().also { player = it; Log.i(TAG, "live tv: podgląd — odtwarzacz utworzony") }
        } catch (e: Exception) {
            Log.w(TAG, "live tv: podgląd — odtwarzacz: ${e.javaClass.simpleName}: ${e.message}"); return null
        }
        if (url == ch.streamUrl) return p
        try {
            val http = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(8_000).setReadTimeoutMs(8_000)
            if (ch.headers.isNotEmpty()) http.setDefaultRequestProperties(ch.headers)
            ch.headers.entries.firstOrNull { it.key.equals("User-Agent", true) }?.let { http.setUserAgent(it.value) }
            p.removeListener(listener); p.addListener(listener)
            p.setMediaSource(DefaultMediaSourceFactory(http).createMediaSource(MediaItem.fromUri(ch.streamUrl)))
            p.volume = 0f
            p.playWhenReady = true
            p.prepare()
            url = ch.streamUrl
            Log.i(TAG, "live tv: podgląd → ${ch.name}")
        } catch (e: Exception) {
            Log.w(TAG, "live tv: podgląd ${ch.name}: ${e.javaClass.simpleName}: ${e.message}")
        }
        return p
    }

    fun release(why: String) {
        val p = player ?: return
        try { p.release() } catch (e: Exception) { Log.w(TAG, "live tv: podgląd — zwolnienie: ${e.javaClass.simpleName}: ${e.message}") }
        player = null; url = null
        Log.i(TAG, "live tv: podgląd zwolniony ($why)")
    }
}

/** Podgląd 520 × 288 (`stage-w − hero-info-w − 20`, 2 × landscape-h + 60) przy prawej górnej krawędzi; brzegi przechodzą w tło. */
@Composable
internal fun NiuniaLivePreviewBox(channel: LiveTvChannel, programme: LiveTvProgramme?, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var view by remember { mutableStateOf<PlayerView?>(null) }
    var showing by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val listener = remember {
        object : Player.Listener {
            override fun onRenderedFirstFrame() { showing = true; Log.i(TAG, "live tv: podgląd — pierwsza klatka") }
            override fun onPlayerError(error: PlaybackException) {
                failed = true; showing = false
                Log.w(TAG, "live tv: podgląd — błąd strumienia: ${error.javaClass.simpleName}: ${error.errorCodeName} ${error.message}")
            }
        }
    }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) NiuniaLivePreview.release("aplikacja w tle") }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); view?.player = null; NiuniaLivePreview.release("fokus poza przewodnikiem") }
    }
    LaunchedEffect(channel.streamUrl) {
        showing = false; failed = false
        if (channel.stalkerCommand != null) { Log.i(TAG, "live tv: podgląd — ${channel.name} (Stalker) bez strumienia, kadr programu"); return@LaunchedEffect }
        kotlinx.coroutines.delay(NiuniaLivePreview.DELAY_MS)
        val p = NiuniaLivePreview.play(ctx, channel, listener)
        view?.player = p
    }
    Box(modifier.size(520.dp, 288.dp)) {
        val still = programme?.image?.takeIf { it.isNotBlank() } ?: com.nuvio.tv.skin.kicia.livetv.forgeChannelLogo(channel)
        if (!showing && still != null) AsyncImage(model = still, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (!failed) AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setKeepContentOnPlayerReset(false)
                    isFocusable = false; isFocusableInTouchMode = false
                }.also { view = it }
            },
            modifier = Modifier.fillMaxSize(),
        )
        // Maska z makiety: od lewej przezroczysto → pełno od 45 %, od dołu pełno do 55 % → przezroczysto (tu: tło skórki na wierzchu).
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to NiuniaColor.Bg, 0.45f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to NiuniaColor.Bg)))
        Column(Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 72.dp)) {
            NiuniaTag(stringResource(R.string.niunia_tv2_preview_badge, com.nuvio.tv.fork.tv.names.ChannelNames.shown(channel.name)), NiuniaTagKind.Live)
        }
    }
}
