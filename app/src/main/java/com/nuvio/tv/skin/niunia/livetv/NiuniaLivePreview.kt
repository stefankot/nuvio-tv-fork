package com.nuvio.tv.skin.niunia.livetv

import android.content.Context
import android.util.Log
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.niunia.components.NiuniaTag
import com.nuvio.tv.skin.niunia.components.NiuniaTagKind
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.ui.reshaped.livetv.LiveTvPreviewPlayer

private const val TAG = "ForkNiunia"

/**
 * Z-21 (P-32, makieta LiveTVRow: `NHero kind="preview"`): podgląd kanału na żywo u góry po prawej, gdy fokus jest
 * w przewodniku. Odtwarzacz podglądu z Reshaped ([LiveTvPreviewPlayer]: kanały HLS bez `.m3u8`, klatki bez IDR, limity
 * słabego TV, zatrzymanie po minucie bez zmiany kanału, dźwięk wg ustawienia podglądu Reshaped) — jeden na ekran; przy
 * zmianie kanału ten sam odtwarzacz dostaje nowy strumień, przed pełnym ekranem ([release] w `play`) i przy wyjściu
 * z przewodnika jest zwalniany (Android 11, JMGO: nigdy dwa dekodery). Start po [DELAY_MS] postoju fokusu. Kanały
 * Stalker (link tworzony przy odtwarzaniu) i błąd: kadr programu albo logo. Przełącznik `niunia_live_preview`.
 */
internal object NiuniaLivePreview {
    const val KEY = "niunia_live_preview"
    const val DELAY_MS = 1_200L
    @Volatile var current: LiveTvPreviewPlayer? = null

    fun enabled(c: Context) = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, KEY, true)

    fun release(why: String) {
        val p = current ?: return
        try { p.release() } catch (e: Exception) { Log.w(TAG, "live tv: podgląd — zwolnienie: ${e.javaClass.simpleName}: ${e.message}") }
        Log.i(TAG, "live tv: podgląd zwolniony ($why)")
    }
}

/** Podgląd 520 × 288 (`stage-w − hero-info-w − 20`, 2 × landscape-h + 60) przy prawej górnej krawędzi; brzegi przechodzą w tło. */
@Composable
internal fun NiuniaLivePreviewBox(channel: LiveTvChannel, programme: LiveTvProgramme?, modifier: Modifier = Modifier) {
    val preview = com.nuvio.tv.ui.reshaped.livetv.rememberLiveTvPreviewPlayer() // zwalniany przy wyjściu z kompozycji i ON_STOP
    val sound = com.nuvio.tv.reshaped.livetv.rememberLiveTvPreviewSoundEnabled()
    SideEffect { NiuniaLivePreview.current = preview; preview.soundEnabled = sound }
    DisposableEffect(preview) { onDispose { if (NiuniaLivePreview.current === preview) NiuniaLivePreview.current = null; Log.i(TAG, "live tv: podgląd zamknięty (fokus poza przewodnikiem)") } }
    LaunchedEffect(channel.streamUrl) {
        preview.stop()
        if (LiveTvRepository.isStalker(channel)) { Log.i(TAG, "live tv: podgląd — ${channel.name} (Stalker) bez strumienia, kadr programu"); return@LaunchedEffect }
        kotlinx.coroutines.delay(NiuniaLivePreview.DELAY_MS)
        Log.i(TAG, "live tv: podgląd → ${channel.name}")
        preview.play(channel)
    }
    val showing = preview.showingVideo
    // UX JMGO 26.11 (#68, zdjęcie 205): kanał z „Niedostępny” grał w podglądzie, a znacznik zostawał — obraz przez 3 s zdejmuje go.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(showing, channel.streamUrl) {
        if (!showing) return@LaunchedEffect
        Log.i(TAG, "live tv: podgląd — obraz ${channel.name}")
        kotlinx.coroutines.delay(3_000)
        if (preview.showingVideo) NiuniaLiveStatus.ok(ctx, channel.streamUrl, channel.name)
    }
    Box(modifier.size(520.dp, 288.dp)) {
        val still = programme?.image?.takeIf { it.isNotBlank() } ?: com.nuvio.tv.skin.kicia.livetv.forgeChannelLogo(channel)
        if (!showing && still != null) AsyncImage(model = still, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        AndroidView(
            factory = { c -> TextureView(c).also { preview.attach(it) } },
            onRelease = { preview.detach(it) },
            modifier = Modifier.fillMaxSize().alpha(if (showing) 1f else 0f), // do pierwszej klatki widać kadr programu
        )
        // Maska z makiety: od lewej przezroczysto → pełno od 45 %, od dołu pełno do 55 % → przezroczysto (tu: tło skórki na wierzchu).
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to NiuniaColor.Bg, 0.45f to Color.Transparent)))
        // Przegląd 27.11 (11 pkt 1): od 90 % wysokości pełne tło, a na samej krawędzi pasek tła (także 4 dp poniżej pola) —
        // dolna krawędź wideo (kilka pikseli poza polem) nie wystaje nad osią godzin przewodnika. Powiększenie (pkt 2) — BLOKADY.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 0.9f to NiuniaColor.Bg, 1f to NiuniaColor.Bg)))
        Box(Modifier.align(Alignment.BottomCenter).offset(y = 4.dp).fillMaxWidth().height(10.dp).background(NiuniaColor.Bg))
        Column(Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 72.dp)) {
            NiuniaTag(stringResource(R.string.niunia_tv2_preview_badge, com.nuvio.tv.fork.tv.names.ChannelNames.shown(channel.name)), NiuniaTagKind.Live)
        }
    }
}
