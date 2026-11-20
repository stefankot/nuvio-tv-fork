package com.nuvio.tv.skin.niunia.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.kicia.livetv.forgeChannelLogo
import com.nuvio.tv.skin.niunia.components.NiuniaSeriesStack
import com.nuvio.tv.skin.niunia.components.NiuniaStatusDot
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText

/**
 * Kafelek Live TV Niuni (canvas `.card` 160×114): kadr programu (albo logo kanału), kropka statusu (czerwona = leci,
 * zielona = w archiwum), znacznik jednym słowem (Nowy odcinek / Nowy / Wznów / Wkrótce), podpis z ▶ i paskiem postępu.
 * Serial = stos pod kafelkiem ([NiuniaSeriesStack]). Kafelek kanału ([NiuniaLiveTile.programme] = null): logo + nazwa.
 */
@Composable
internal fun NiuniaLiveTileCard(
    tile: NiuniaLiveTile,
    longBadge: Boolean,
    focusRequester: FocusRequester? = null,
    onFocus: (Boolean) -> Unit = {},
    onOk: () -> Unit,
) {
    val p = tile.programme
    val logo = forgeChannelLogo(tile.channel) ?: LiveTvRepository.uiState.value.logoFor(tile.channel)
    val image = p?.image?.takeIf { it.isNotBlank() }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(NiuniaDims.RadiusCard)
    NiuniaSeriesStack(tile.series, image ?: logo, NiuniaDims.WideW, NiuniaDims.WideH) {
        Box(
            Modifier.size(NiuniaDims.WideW, NiuniaDims.WideH)
                .niuniaFocusable(shape, focusRequester = focusRequester, onFocus = { focused = it; onFocus(it) }, onOk = onOk)
                .clip(shape).background(NiuniaColors.Surface),
        ) {
            if (p == null) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (logo != null) AsyncImage(model = logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(64.dp, 44.dp))
                    Text(tile.channel.name, style = NiuniaText.Meta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = NiuniaColors.Text,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp))
                }
                NiuniaStatusDot(live = true)
                return@Box
            }
            if (image != null) AsyncImage(model = image, contentDescription = p.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else if (logo != null) AsyncImage(model = logo, contentDescription = tile.channel.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(start = 36.dp, end = 36.dp, top = 18.dp, bottom = 40.dp))
            val dot = when (tile.mark) { NiuniaTileMark.LIVE -> true; NiuniaTileMark.ARCHIVE, NiuniaTileMark.RESUME -> false; else -> null }
            if (dot != null) NiuniaStatusDot(live = dot)
            val badge = when (tile.mark) {
                NiuniaTileMark.NEW -> stringResource(if (longBadge) R.string.niunia_tv_badge_new_episode else R.string.niunia_tv_badge_new)
                NiuniaTileMark.RESUME -> stringResource(R.string.niunia_tv_badge_resume)
                NiuniaTileMark.SOON -> stringResource(R.string.niunia_tv_badge_soon)
                else -> null
            }
            if (badge != null) {
                val newOne = tile.mark == NiuniaTileMark.NEW && longBadge
                Text(badge, style = NiuniaText.Caption.copy(fontSize = 10.sp, lineHeight = 16.sp), color = if (newOne) NiuniaColors.OnFocus else NiuniaColors.Text,
                    modifier = Modifier.padding(start = if (dot != null) 24.dp else 7.dp, top = 7.dp)
                        .background(if (newOne) NiuniaColors.Focus else NiuniaColors.Badge, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp))
            }
            Row(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xA6000000))))
                    .padding(start = 8.dp, end = 8.dp, top = 22.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (tile.withChannel) Icon(Icons.Filled.PlayArrow, null, tint = if (focused) NiuniaColors.Text else NiuniaColors.Text70, modifier = Modifier.size(10.dp))
                tile.progress?.let { f ->
                    Box(Modifier.width(22.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x59FFFFFF))) {
                        Box(Modifier.fillMaxWidth(f).height(3.dp).background(NiuniaColors.Text))
                    }
                }
                Text(caption(tile), style = NiuniaText.Caption, color = if (focused) NiuniaColors.Text else NiuniaColors.Text70, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** „program · E5 · kanał” (przewodnik) albo „program · Pt 20:15” (Odkrywaj — godzina tylko dla tego, co dopiero będzie). */
internal fun caption(tile: NiuniaLiveTile): String {
    val p = tile.programme ?: return tile.channel.name
    return listOfNotNull(p.title, tile.extra?.takeIf { it.isNotBlank() }, tile.channel.name.takeIf { tile.withChannel }).joinToString(" · ")
}
