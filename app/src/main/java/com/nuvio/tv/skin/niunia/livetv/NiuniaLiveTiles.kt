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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.nuvio.tv.skin.niunia.components.niuniaFocusLook
import com.nuvio.tv.skin.niunia.components.niuniaOk
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText

/**
 * Kafelek Live TV Niuni (canvas `.card` 160×114): kadr programu (albo logo kanału), kropka statusu (czerwona = leci,
 * zielona = w archiwum), znacznik jednym słowem (Nowy odcinek / Nowy / Wznów / Wkrótce), podpis z ▶ i paskiem postępu.
 * Serial = stos pod kafelkiem ([NiuniaSeriesStack]). Kafelek kanału ([NiuniaLiveTile.programme] = null): logo + nazwa.
 */
/**
 * UX 22.11 (właściciel: „w większości elementów nie działa długie naciśnięcie”): długie OK na kanale (kafelek, wiersz
 * przewodnika) = ulubione tak/nie, z krótkim potwierdzeniem na ekranie.
 */
internal fun niuniaToggleFavourite(context: android.content.Context, ch: com.nuvio.tv.reshaped.livetv.LiveTvChannel) {
    val before = ch.streamUrl in LiveTvRepository.uiState.value.favoriteUrls
    LiveTvRepository.toggleFavorite(ch)
    val after = ch.streamUrl in LiveTvRepository.uiState.value.favoriteUrls
    if (before == after) {
        com.nuvio.tv.fork.ForkLog.say(context, "ForkNiunia", context.getString(R.string.niunia_live_fav_failed, ch.name))
        return
    }
    android.util.Log.i("ForkNiunia", "live tv: długie OK → ulubione ${if (after) "dodane" else "usunięte"}: ${ch.name}")
    android.widget.Toast.makeText(context, context.getString(if (after) R.string.niunia_live_fav_added else R.string.niunia_live_fav_removed, com.nuvio.tv.fork.tv.names.ChannelNames.shown(ch.name)), android.widget.Toast.LENGTH_SHORT).show()
}

@Composable
internal fun NiuniaLiveTileCard(
    tile: NiuniaLiveTile,
    longBadge: Boolean,
    focusRequester: FocusRequester? = null,
    onFocus: (Boolean) -> Unit = {},
    onLongOk: (() -> Unit)? = null,
    onOk: () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val p = tile.programme
    val logo = forgeChannelLogo(tile.channel) ?: LiveTvRepository.uiState.value.logoFor(tile.channel)
    val image = p?.image?.takeIf { it.isNotBlank() }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(com.nuvio.tv.skin.niunia.tokens.NiuniaRadius.Radius16) // DS v2: karty 16
    NiuniaSeriesStack(tile.series, image ?: logo, NiuniaDims.WideW, NiuniaDims.WideH) {
        Box(
            Modifier.size(NiuniaDims.WideW, NiuniaDims.WideH)
                // DS v2 (NowCard / ChannelCard): uniesienie, ×1,1, cień i obwódka 3 dp.
                // IPTV #83 (właściciel 26.11): długie OK na „Gry małżeńskie” mówiło „dodano”, a dodawało KANAŁ (w „Ulubione” był potem
                // „Nasz nowy dom”, który na nim leciał) — kafelek programu dodaje program do ulubionych programów, kafelek kanału kanał.
                .niuniaOk(caption(tile), focusRequester, onFocus = { focused = it; onFocus(it) },
                    onLongClick = onLongOk ?: { if (p != null) niuniaToggleFollow(ctx, p.title) else niuniaToggleFavourite(ctx, tile.channel) }, onClick = onOk)
                .niuniaFocusLook(com.nuvio.tv.skin.niunia.components.NiuniaFocusStyle.Lift, focused, shape)
                .clip(shape).background(NiuniaColors.Surface),
        ) {
            if (p == null) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (logo != null) NiuniaChannelLogo(tile.channel, Modifier.size(64.dp, 44.dp))
                    Text(com.nuvio.tv.fork.tv.names.ChannelNames.shown(tile.channel.name), style = NiuniaText.Meta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = NiuniaColors.Text,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp))
                }
                // Z-1: bez kropek statusu.
                return@Box
            }
            // Paczka „oczywiste” pkt 5: obraz programu, a gdy go nie ma albo się nie wczyta — logo kanału, a na końcu jego nazwa.
            var imageFailed by remember(image) { mutableStateOf(false) }
            if (image != null && !imageFailed) AsyncImage(model = image, contentDescription = p.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                onError = { st -> imageFailed = true; android.util.Log.w("ForkNiunia", "obraz programu „${p.title}”: ${st.result.throwable.javaClass.simpleName}: ${st.result.throwable.message}") })
            else NiuniaChannelLogo(tile.channel, Modifier.fillMaxSize().padding(start = 36.dp, end = 36.dp, top = 18.dp, bottom = 40.dp),
                nameStyle = NiuniaText.Logo.copy(fontSize = 22.sp, lineHeight = 26.sp))
            // Z-1: bez kropek statusu i ikony play; stan mówi mini pasek w podpisie (czerwony na żywo, biały obejrzane archiwum).
            val dot: Boolean? = null
            val badge = when (tile.mark) {
                NiuniaTileMark.NEW -> stringResource(if (longBadge) R.string.niunia_tv_badge_new_episode else R.string.niunia_tv_badge_new)
                NiuniaTileMark.RESUME -> stringResource(R.string.niunia_tv_badge_resume)
                NiuniaTileMark.SOON -> stringResource(R.string.niunia_tv_badge_soon)
                else -> null
            }
            val expires = tile.expiresHours?.takeIf { badge == null && tile.mark == NiuniaTileMark.ARCHIVE }
            if (expires != null) com.nuvio.tv.skin.niunia.components.NiuniaTag(
                // 22.11: żółta etykieta (jak „opóźnienie”) — program wkrótce zniknie z archiwum dostawcy.
                androidx.compose.ui.platform.LocalContext.current.resources.getQuantityString(R.plurals.niunia_catchup_left_hours, expires.coerceAtLeast(1), expires.coerceAtLeast(1)),
                com.nuvio.tv.skin.niunia.components.NiuniaTagKind.Delay,
                Modifier.padding(start = if (dot != null) 26.dp else 8.dp, top = 8.dp))
            if (badge != null) {
                val newOne = tile.mark == NiuniaTileMark.NEW && longBadge
                com.nuvio.tv.skin.niunia.components.NiuniaTag(badge,
                    if (newOne) com.nuvio.tv.skin.niunia.components.NiuniaTagKind.BadgeStrong else com.nuvio.tv.skin.niunia.components.NiuniaTagKind.Badge,
                    Modifier.padding(start = if (dot != null) 26.dp else 8.dp, top = 8.dp))
            }
            Row(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(com.nuvio.tv.skin.niunia.tokens.NiuniaGradient.ScrimCaption.brush())
                    .padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Z-1: [mini pasek 24×4] + „Tytuł · Kanał” (tytuł caption 700, „· kanał” caption-regular 500), maks. 2 linie, kanał zawsze widoczny.
                val live = tile.mark == NiuniaTileMark.LIVE
                val bar = when { live -> tile.progress ?: 0f; (tile.progress ?: 0f) > 0f -> tile.progress; else -> null }
                if (bar != null) com.nuvio.tv.skin.niunia.components.NiuniaProgress(bar, fillColor = if (live) com.nuvio.tv.skin.niunia.tokens.NiuniaColor.Live else null)
                Text(tileCaption(tile), style = com.nuvio.tv.skin.niunia.tokens.NiuniaType.Caption, color = NiuniaColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { contentDescription = caption(tile) })
            }
            Box(Modifier.matchParentSize().background(com.nuvio.tv.skin.niunia.tokens.NiuniaGradient.Gloss.brush()))
        }
    }
}

/** Z-1: podpis na kafelku — „**Tytuł** · Kanał” z krótką nazwą kanału; tytuł skracany, żeby kanał został. */
internal fun tileCaption(tile: NiuniaLiveTile): androidx.compose.ui.text.AnnotatedString {
    val p = tile.programme ?: return androidx.compose.ui.text.AnnotatedString(com.nuvio.tv.skin.niunia.components.niuniaShortChannel(tile.channel.name))
    val ch = if (tile.withChannel) com.nuvio.tv.skin.niunia.components.niuniaShortChannel(tile.channel.name) else null
    val title = listOfNotNull(p.title, tile.extra?.takeIf { it.isNotBlank() }).joinToString(" · ")
    return androidx.compose.ui.text.buildAnnotatedString {
        // Przegląd 27.11, ekran 12: tytuł bez skracania (płynie przez 2 linie); gdy całość się nie mieści, ucina się kanał na końcu.
        append(title)
        if (ch != null) {
            pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
            append(" · $ch")
            pop()
        }
    }
}

/** „program · E5 · kanał” (przewodnik) albo „program · Pt 20:15” (Odkrywaj — godzina tylko dla tego, co dopiero będzie). */
internal fun caption(tile: NiuniaLiveTile): String {
    val p = tile.programme ?: return com.nuvio.tv.fork.tv.names.ChannelNames.shown(tile.channel.name)
    return listOfNotNull(p.title, tile.extra?.takeIf { it.isNotBlank() }, com.nuvio.tv.fork.tv.names.ChannelNames.shown(tile.channel.name).takeIf { tile.withChannel }).joinToString(" · ")
}
