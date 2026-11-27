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
import com.nuvio.tv.skin.niunia.components.NiuniaBlurBottom
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
    val epgImage = p?.image?.takeIf { it.isNotBlank() }
    // Przegląd 27.11 (10 pkt 3): bez obrazka z przewodnika — okładka programu z TMDB (ścisła zgodność tytułu), logo dopiero gdy jej brak.
    val minutes = p?.let { ((it.stopEpochMs - it.startEpochMs) / 60_000L).toInt().takeIf { m -> m > 0 } }
    val tmdbCover by androidx.compose.runtime.produceState(if (epgImage == null && p != null) com.nuvio.tv.fork.tv.covers.TvCovers.cached(p.title, minutes) else null, p?.title, epgImage) {
        if (epgImage == null && p != null && value == null) value = com.nuvio.tv.fork.tv.covers.TvCovers.cover(ctx, p.title, minutes)
    }
    val image = epgImage ?: tmdbCover
    // Przegląd 27.11 (10 pkt 2): kafel = komponent kart skórki NiuniaMediaCard (NowCard dla programu, ChannelCard dla kanału);
    // podpis „Tytuł · Kanał”, pasek postępu, rozmyty dół i logo zamiast brakującego obrazu są w komponencie.
    // Przegląd 27.11 (10 pkt 1): obrócone warstwy z logo stacji dawały kolorowe strzępy pod kartą — warstwy gładkie (bez obrazka).
    NiuniaSeriesStack(tile.series, null, NiuniaDims.WideW, NiuniaDims.WideH) {
        // IPTV #83 (właściciel 26.11): kafelek programu dodaje długim OK program do ulubionych programów, kafelek kanału kanał.
        val longOk: () -> Unit = onLongOk ?: { if (p != null) niuniaToggleFollow(ctx, p.title) else niuniaToggleFavourite(ctx, tile.channel) }
        if (p == null) {
            com.nuvio.tv.skin.niunia.components.NiuniaMediaCard(
                com.nuvio.tv.skin.niunia.components.NiuniaCardPreset.ChannelCard, image = null, onClick = onOk,
                width = NiuniaDims.WideW, height = NiuniaDims.WideH, description = caption(tile),
                focusRequester = focusRequester, onFocus = onFocus, onLongClick = longOk,
                fallback = {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        if (logo != null) NiuniaChannelLogo(tile.channel, Modifier.size(64.dp, 44.dp))
                        Text(com.nuvio.tv.fork.tv.names.ChannelNames.shown(tile.channel.name), style = NiuniaText.Meta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = NiuniaColors.Text,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp))
                    }
                },
            )
        } else NowTile(tile, p, image, longBadge, focusRequester, onFocus, longOk, onOk)
    }
}

@Composable
private fun NowTile(tile: NiuniaLiveTile, p: com.nuvio.tv.reshaped.livetv.LiveTvProgramme, image: String?, longBadge: Boolean,
                    focusRequester: FocusRequester?, onFocus: (Boolean) -> Unit, longOk: () -> Unit, onOk: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    run {
        // Z-1: bez kropek statusu i ikony play; stan mówi pasek nad podpisem (czerwony na żywo, biały obejrzane archiwum).
        val badge = when (tile.mark) {
            NiuniaTileMark.NEW -> stringResource(if (longBadge) R.string.niunia_tv_badge_new_episode else R.string.niunia_tv_badge_new)
            NiuniaTileMark.RESUME -> stringResource(R.string.niunia_tv_badge_resume)
            NiuniaTileMark.SOON -> stringResource(R.string.niunia_tv_badge_soon)
            else -> null
        }
        val expires = tile.expiresHours?.takeIf { badge == null && tile.mark == NiuniaTileMark.ARCHIVE }
        val expiresText = expires?.let { ctx.resources.getQuantityString(R.plurals.niunia_catchup_left_hours, it.coerceAtLeast(1), it.coerceAtLeast(1)) }
        val live = tile.mark == NiuniaTileMark.LIVE
        // Przegląd 27.11 (13): pasek postępu nad podpisem, na całą szerokość karty.
        val bar = when { live -> tile.progress ?: 0f; (tile.progress ?: 0f) > 0f -> tile.progress; else -> null }
        com.nuvio.tv.skin.niunia.components.NiuniaMediaCard(
            com.nuvio.tv.skin.niunia.components.NiuniaCardPreset.NowCard, image = image, onClick = onOk,
            width = NiuniaDims.WideW, height = NiuniaDims.WideH, description = caption(tile),
            focusRequester = focusRequester, onFocus = onFocus, onLongClick = longOk,
            captionRich = tileCaption(tile), progressBar = bar,
            progressColor = if (live) com.nuvio.tv.skin.niunia.tokens.NiuniaColor.Live else null,
            // Przegląd 27.11 (CAŁA APLIKACJA): podpis na rozmytym dole obrazu (liczone raz, pamięć Coil + dysk).
            blurCaption = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(ctx, "niunia_blur_captions", true),
            topTag = when {
                // 22.11: żółta etykieta (jak „opóźnienie”) — program wkrótce zniknie z archiwum dostawcy.
                expiresText != null -> ({ com.nuvio.tv.skin.niunia.components.NiuniaTag(expiresText, com.nuvio.tv.skin.niunia.components.NiuniaTagKind.Delay) })
                badge != null -> ({ com.nuvio.tv.skin.niunia.components.NiuniaTag(badge,
                    if (tile.mark == NiuniaTileMark.NEW && longBadge) com.nuvio.tv.skin.niunia.components.NiuniaTagKind.BadgeStrong else com.nuvio.tv.skin.niunia.components.NiuniaTagKind.Badge) })
                else -> null
            },
            // Paczka „oczywiste” pkt 5: bez obrazu programu (albo gdy się nie wczyta) — logo kanału, a na końcu jego nazwa.
            fallback = { NiuniaChannelLogo(tile.channel, Modifier.fillMaxSize().padding(start = 36.dp, end = 36.dp, top = 18.dp, bottom = 40.dp),
                nameStyle = NiuniaText.Logo.copy(fontSize = 22.sp, lineHeight = 26.sp)) },
        )
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
