package com.nuvio.tv.skin.niunia.livetv

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import com.nuvio.tv.ui.reshaped.livetv.LiveTvScreenModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private const val TAG = "ForkNiunia"
private const val HOUR_MS = 3_600_000L
private const val WINDOW_MS = 165 * 60_000L // 2 h 45 min na osi (canvas: 20:00 → 22:45)
private val HOUR_DP = 280.dp // canvas: 280 dp na godzinę (152 → 432)
private val CHANNEL_COL = 112.dp // canvas: kanały od x 40, programy od x 152
private val ROW_H = 48.dp

/**
 * Live TV Niunii (canvas: LiveTV): rząd „Teraz w telewizji” (kadr programu albo logo, plakietka LIVE, „Kanał · do HH:MM” z paskiem),
 * „Przewodnik” z pigułkami kategorii, oś czasu (pełne pół godziny wstecz + 2 h 45 min) z czerwoną linią „teraz”,
 * kanały po lewej i komórki programów (trwający = jaśniejszy, fokus = biały). Na dole linia programu z fokusem.
 * Dane: [LiveTvRepository] (kanały, program teraz, ramówka); OK = kanał na żywo (ta sama trasa odtwarzacza co w Live TV Nuvio).
 * Archiwum, ulubione, źródła i ukrywanie kategorii zostają w Live TV Nuvio (skórka Nuvio/Kicia).
 */
@Composable
fun NiuniaLiveTv(onPlay: (String) -> Unit, viewModel: LiveTvScreenModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    remember(viewModel) { viewModel.ensureLoaded() }
    val state by LiveTvRepository.uiState.collectAsState()
    var group by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var focusedInfo by remember { mutableStateOf<String?>(null) }
    var launching by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }

    val shown = state.shownChannels
    val groups = remember(shown) { shown.map { it.group }.filter { it.isNotBlank() }.distinct() }
    val channels = remember(shown, group) { if (group == null) shown else shown.filter { it.group == group } }
    val windowStart = remember(now) { Calendar.getInstance().apply { timeInMillis = now; set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); set(Calendar.MINUTE, if (get(Calendar.MINUTE) >= 30) 30 else 0) }.timeInMillis }
    val onAir = remember(channels, state.currentProgrammes, now) {
        channels.mapNotNull { ch -> state.currentProgrammes[ch.guideKey]?.takeIf { it.stopEpochMs > now }?.let { ch to it } }.take(8)
    }
    LaunchedEffect(shown.size, state.isLoaded) {
        Log.i(TAG, "live tv: ${shown.size} kanałów, kategorie=${groups.size}, teraz w TV=${onAir.size}, wczytane=${state.isLoaded}")
    }

    fun play(channel: LiveTvChannel) {
        if (launching) return
        launching = true
        scope.launch {
            try {
                LiveTvRepository.setZapList(channels, folderKey = group)
                val route = com.nuvio.tv.ui.reshaped.livetv.liveTvPlayerRoute(channel, viewModel.profileId)
                Log.i(TAG, "live tv: odtwarzam kanał ${channel.id} (${channel.group})")
                viewModel.launchedUrl = channel.streamUrl
                onPlay(route)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "live tv: start kanału ${channel.id}: ${e.javaClass.simpleName}: ${e.message}")
                Toast.makeText(context, context.getString(R.string.niunia_live_play_failed), Toast.LENGTH_SHORT).show()
            } finally {
                launching = false
            }
        }
    }

    if (!state.hasSource || (state.isLoaded && shown.isEmpty())) {
        Box(Modifier.fillMaxSize().background(NiuniaColors.Bg).padding(start = NiuniaDims.Gutter), contentAlignment = Alignment.CenterStart) { // pkt 3: od lewej
            Text(stringResource(if (!state.hasSource) R.string.niunia_live_no_source else R.string.niunia_live_empty), style = NiuniaText.Meta, color = NiuniaColors.Text70)
        }
        return
    }

    Column(Modifier.fillMaxSize().background(NiuniaColors.Bg).padding(top = 76.dp)) {
        if (onAir.isNotEmpty()) {
            LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                items(onAir, key = { it.first.id }) { (ch, p) ->
                    OnAirCard(ch, p, state.logoFor(ch), now, onFocus = { focusedInfo = info(ch, p) }, onOk = { play(ch) })
                }
            }
        }
        // Przewodnik + kategorie (canvas: y 246).
        Row(Modifier.padding(start = NiuniaDims.Gutter, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.niunia_live_guide), style = NiuniaText.Button.copy(fontSize = 20.sp, lineHeight = 26.sp), color = NiuniaColors.Text, modifier = Modifier.padding(end = 16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = NiuniaDims.Gutter)) {
                item { Pill(stringResource(R.string.niunia_live_all), selected = group == null) { group = null } }
                items(groups.take(40)) { g -> Pill(state.groupNames[g] ?: g, selected = group == g) { Log.i(TAG, "live tv: kategoria „$g”"); group = g } }
            }
        }
        // Oś czasu.
        Box(Modifier.fillMaxWidth().height(24.dp).padding(start = NiuniaDims.Gutter + CHANNEL_COL, top = 6.dp)) {
            var t = windowStart
            while (t < windowStart + WINDOW_MS) {
                Text(hhmm(t), style = NiuniaText.Caption.copy(fontSize = 12.sp), color = NiuniaColors.Text50, modifier = Modifier.offset(x = dpFor(t - windowStart)))
                t += HOUR_MS / 2
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(contentPadding = PaddingValues(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(channels, key = { "${it.sourceId}_${it.id}" }) { ch ->
                    ChannelRow(ch, state.logoFor(ch), windowStart, now, state.guideVersion,
                        onFocus = { p -> focusedInfo = if (p != null) info(ch, p) else ch.name },
                        onOk = { play(ch) })
                }
            }
            // Linia „teraz” (canvas: 2 dp, kolor live, kropka 10 dp).
            val x = NiuniaDims.Gutter + CHANNEL_COL + dpFor(now - windowStart)
            Box(Modifier.offset(x = x - 4.dp, y = (-4).dp).size(10.dp).background(NiuniaColors.Live, CircleShape))
            Box(Modifier.offset(x = x).width(2.dp).fillMaxSize().background(NiuniaColors.Live))
        }
        Row(Modifier.padding(start = NiuniaDims.Gutter, top = 6.dp, bottom = 14.dp).height(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("LIVE", style = NiuniaText.Caption.copy(fontWeight = FontWeight.Bold), color = NiuniaColors.Text, modifier = Modifier.background(NiuniaColors.Live, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
            Text(focusedInfo.orEmpty(), style = NiuniaText.Body, color = NiuniaColors.Text70, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun info(ch: LiveTvChannel, p: LiveTvProgramme) = "${ch.name} · ${p.title} · ${hhmm(p.startEpochMs)} – ${hhmm(p.stopEpochMs)}"

private fun dpFor(ms: Long): Dp = HOUR_DP * (ms.toFloat() / HOUR_MS)

private fun hhmm(ms: Long): String = Calendar.getInstance().run { timeInMillis = ms; "%02d:%02d".format(get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE)) }

/** canvas .card (160×114): kadr programu albo logo kanału, plakietka LIVE, podpis z paskiem postępu. */
@Composable
private fun OnAirCard(ch: LiveTvChannel, p: LiveTvProgramme, logo: String?, now: Long, onFocus: () -> Unit, onOk: () -> Unit) {
    val shape = RoundedCornerShape(NiuniaDims.RadiusCard)
    Box(
        Modifier.size(NiuniaDims.WideW, NiuniaDims.WideH)
            .niuniaFocusable(shape, onFocus = { if (it) onFocus() }, onOk = onOk)
            .clip(shape).background(NiuniaColors.Surface),
    ) {
        if (p.image != null) AsyncImage(model = p.image, contentDescription = p.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else if (logo != null) AsyncImage(model = logo, contentDescription = ch.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(24.dp))
        // Kropka statusu zamiast napisu „LIVE” na kafelku (decyzja 20.11); napis LIVE zostaje tylko w opisie na dole.
        com.nuvio.tv.skin.niunia.components.NiuniaStatusDot(live = true)
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))).padding(8.dp)) {
            val f = ((now - p.startEpochMs).toFloat() / (p.stopEpochMs - p.startEpochMs).coerceAtLeast(1)).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth(0.5f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(NiuniaColors.Fill36)) {
                Box(Modifier.fillMaxWidth(f).height(3.dp).background(NiuniaColors.Text))
            }
            Text(stringResource(R.string.niunia_live_until, ch.name, hhmm(p.stopEpochMs)), style = NiuniaText.Caption, color = NiuniaColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Jeden kanał: logo + nazwa (canvas .ch), potem komórki programów w oknie czasu (canvas .cell, .now, .foc). */
@Composable
private fun ChannelRow(ch: LiveTvChannel, logo: String?, windowStart: Long, now: Long, guideVersion: Int, onFocus: (LiveTvProgramme?) -> Unit, onOk: () -> Unit) {
    val windowEnd = windowStart + WINDOW_MS
    val programmes = remember(ch.guideKey, guideVersion, windowStart) {
        LiveTvRepository.schedule(ch.guideKey).filter { it.stopEpochMs > windowStart && it.startEpochMs < windowEnd }
    }
    Box(Modifier.fillMaxWidth().height(ROW_H).padding(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter)) {
        Row(Modifier.width(CHANNEL_COL - 8.dp).height(ROW_H), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(NiuniaColors.Fill12), contentAlignment = Alignment.Center) {
                if (logo != null) AsyncImage(model = logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(24.dp))
            }
            Text(ch.name, style = NiuniaText.Body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = NiuniaColors.Text70, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
        }
        Box(Modifier.offset(x = CHANNEL_COL).fillMaxWidth().height(ROW_H)) {
            if (programmes.isEmpty()) {
                Cell(stringResource(R.string.niunia_live_no_guide), null, x = 0.dp, w = dpFor(WINDOW_MS) - 4.dp, now = true, onFocus = { onFocus(null) }, onOk = onOk)
            } else {
                programmes.forEach { p ->
                    val start = maxOf(p.startEpochMs, windowStart)
                    val stop = minOf(p.stopEpochMs, windowEnd)
                    Cell(p.title, "${hhmm(p.startEpochMs)} – ${hhmm(p.stopEpochMs)}", x = dpFor(start - windowStart), w = (dpFor(stop - start) - 4.dp).coerceAtLeast(24.dp),
                        now = now in p.startEpochMs until p.stopEpochMs, onFocus = { onFocus(p) }, onOk = onOk)
                }
            }
        }
    }
}

@Composable
private fun Cell(title: String, time: String?, x: Dp, w: Dp, now: Boolean, onFocus: () -> Unit, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.offset(x = x).width(w).height(ROW_H)
            .niuniaFocusable(shape, scale = 1f, lift = false, onFocus = { focused = it; if (it) onFocus() }, onOk = onOk)
            .background(when { focused -> NiuniaColors.Focus; now -> NiuniaColors.Fill24; else -> NiuniaColors.Fill12 }, shape)
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = NiuniaText.Meta.copy(fontWeight = FontWeight.Bold), color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (time != null) Text(time, style = NiuniaText.Caption.copy(fontSize = 11.sp), color = if (focused) NiuniaColors.OnFocus.copy(alpha = 0.7f) else NiuniaColors.Text50, maxLines = 1)
    }
}

/** Pigułka kategorii (canvas .btn.sec.sm: 34 wys.; wybrana = białe 24 %, fokus = białe tło). */
@Composable
private fun Pill(text: String, selected: Boolean, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.height(34.dp)
            .niuniaFocusable(shape, scale = NiuniaDims.ScaleButton, lift = false, onFocus = { focused = it }, onOk = onOk)
            .background(when { focused -> NiuniaColors.Focus; selected -> NiuniaColors.Fill24; else -> NiuniaColors.Btn }, shape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = NiuniaText.Button.copy(fontSize = 14.sp, lineHeight = 20.sp), color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text, maxLines = 1)
    }
}
