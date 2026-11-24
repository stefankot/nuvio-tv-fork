package com.nuvio.tv.skin.niunia.player

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Display
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.data.local.FrameRateMatchingMode
import com.nuvio.tv.skin.niunia.components.NiuniaTag
import com.nuvio.tv.skin.niunia.components.NiuniaTagKind
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType
import com.nuvio.tv.ui.screens.player.PlayerUiState

private const val TAG = "ForkNiunia"

/** Z-22 (P-35): tryb odtwarzania inny niż plik + przyczyna prostymi słowami. */
internal data class NiuniaNotice(val mode: String, val cause: String)

/**
 * Z-22 (P-35): przyczyny tylko z danych, które odtwarzacz zna: Dolby Vision bez obsługi DV w ekranie (Display HDR caps)
 * i klatki pliku niedopasowane do częstotliwości ekranu. Dźwięk (E-AC3 → PCM) pominięty: baza nie podaje, czy dekoder go odrzucił.
 */
internal fun niuniaPlaybackNotices(ui: PlayerUiState, display: Display?): List<NiuniaNotice> {
    val out = ArrayList<NiuniaNotice>()
    val codec = ui.streamInfoData?.videoCodec?.lowercase().orEmpty()
    val dv = "dolby" in codec || "dvhe" in codec || "dvh1" in codec || "dovi" in codec
    if (dv && display != null) {
        @Suppress("DEPRECATION")
        val types = display.hdrCapabilities?.supportedHdrTypes ?: IntArray(0)
        if (Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION !in types) out += NiuniaNotice("Dolby Vision jako HDR10", "ten projektor nie obsługuje Dolby Vision")
    }
    val fps = ui.detectedFrameRate.takeIf { it > 1f } ?: ui.streamInfoData?.videoFrameRate?.takeIf { it > 1f }
    val hz = display?.refreshRate?.takeIf { it > 1f }
    if (fps != null && hz != null) {
        val ratio = hz / fps
        val mismatch = kotlin.math.abs(ratio - kotlin.math.round(ratio)) > 0.04f
        if (mismatch) {
            val cause = if (ui.frameRateMatchingMode == FrameRateMatchingMode.OFF) "dopasowanie częstotliwości wyłączone w ustawieniach"
            else "ekran nie przełącza częstotliwości"
            out += NiuniaNotice("${kotlin.math.round(fps).toInt()} kl/s przy ${kotlin.math.round(hz).toInt()} Hz", cause)
        }
    }
    return out
}

@Suppress("DEPRECATION")
private fun niuniaDisplay(ctx: Context): Display? = try {
    if (Build.VERSION.SDK_INT >= 30) ctx.display else (ctx.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
} catch (e: UnsupportedOperationException) { Log.w(TAG, "odtwarzacz: ekran: ${e.javaClass.simpleName}: ${e.message}"); null }

/** NContentBlock `toast`: chip trybu + przyczyna, lewy górny róg (40, nav-top), 6 s, bez fokusu; raz na strumień. */
@Composable
internal fun NiuniaPlaybackNotice(uiState: PlayerUiState) {
    val ctx = LocalContext.current
    val key = uiState.currentStreamUrl ?: uiState.currentStreamName ?: uiState.title
    var shownFor by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<NiuniaNotice?>(null) }
    val ready = uiState.isPlaying
    LaunchedEffect(key, ready, uiState.detectedFrameRate, uiState.streamInfoData?.videoCodec) {
        if (!ready || shownFor == key) return@LaunchedEffect
        kotlinx.coroutines.delay(1500) // przełączenie trybu ekranu i wykrycie klatek
        val n = niuniaPlaybackNotices(uiState, niuniaDisplay(ctx)).firstOrNull() ?: return@LaunchedEffect
        shownFor = key
        Log.i(TAG, "odtwarzacz: komunikat „${n.mode} · ${n.cause}” (6 s)")
        notice = n
        kotlinx.coroutines.delay(6000)
        notice = null
    }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(notice != null, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart).padding(start = NiuniaSpacing.Space40, top = NiuniaSize.NavTop)) {
            val n = notice ?: return@AnimatedVisibility
            val shape = RoundedCornerShape(NiuniaRadius.Radius16)
            Row(
                // Nad wideo (SurfaceView) szkło nie ma czego próbkować: zostaje FrostFlat (szron 12 % byłby nieczytelny) + obwódka szkła.
                with(com.nuvio.tv.fork.glass.ForkGlass) { Modifier.widthIn(max = NiuniaSize.HeroInfoW).forkGlass(com.nuvio.tv.fork.glass.ForkGlass.Role.Toast, shape, fallback = NiuniaColor.FrostFlat, layer = LAYER_BACK) }
                    .padding(horizontal = NiuniaSpacing.Space16, vertical = NiuniaSpacing.Space12),
                horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12), verticalAlignment = Alignment.CenterVertically,
            ) {
                NiuniaTag(n.mode, NiuniaTagKind.Chip)
                Text(n.cause, style = NiuniaType.BodySmall, color = NiuniaColor.Text70, maxLines = 2)
            }
        }
    }
}
