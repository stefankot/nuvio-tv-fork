package com.nuvio.tv.skin.niunia.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaGradient
import com.nuvio.tv.skin.niunia.tokens.NiuniaOpacity
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType

/* NListRow, NMediaCard, NContentBlock (molekuły design systemu Nuvio TV v2). */

// ================================================================ NListRow

enum class NiuniaRowKind { Setting, Track, Suggestion, Tree, Nav, Account, Header }
enum class NiuniaTrailing { None, Chevron, Check, SwitchOn, SwitchOff }

/**
 * Wiersz listy: ustawienie (36), ścieżka w panelu odtwarzacza (44), podpowiedź, drzewo (głęb. 0–2), pozycja menu (pigułka 36
 * z ikoną w kółku 24), konto (56, awatar + „Zmień profil”), nagłówek sekcji. Fokus = białe tło bez skali; [selected] = 24%.
 */
@Composable
fun NiuniaListRow(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    kind: NiuniaRowKind = NiuniaRowKind.Track,
    value: String? = null,
    supporting: String? = null,
    icon: String? = null,
    leading: (@Composable (focused: Boolean) -> Unit)? = null,
    trailing: NiuniaTrailing = NiuniaTrailing.None,
    count: String? = null,
    depth: Int = 0,
    selected: Boolean = false,
    enabled: Boolean = true,
    dimDisabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFocus: (Boolean) -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    if (kind == NiuniaRowKind.Header) {
        NiuniaText(label, modifier.padding(start = NiuniaSpacing.Space12, end = NiuniaSpacing.Space12, top = NiuniaSpacing.Space12), variant = NiuniaTextVariant.Caption, tone = NiuniaTone.Tertiary, upper = true, lines = 1)
        return
    }
    var focused by remember { mutableStateOf(false) }
    val pill = kind == NiuniaRowKind.Nav || kind == NiuniaRowKind.Account
    val shape = if (pill) CircleShape else RoundedCornerShape(NiuniaRadius.Radius16)
    val minH = when (kind) {
        NiuniaRowKind.Setting, NiuniaRowKind.Nav, NiuniaRowKind.Tree -> NiuniaSize.H36
        NiuniaRowKind.Suggestion -> NiuniaSize.Icon28
        NiuniaRowKind.Account -> NiuniaSize.H56
        else -> if (supporting != null) NiuniaSize.H56 else NiuniaSize.H44
    }
    val bg = when {
        focused -> NiuniaColor.Focus
        selected -> NiuniaColor.Fill24
        kind == NiuniaRowKind.Setting -> NiuniaColor.Surface
        else -> Color.Transparent
    }
    val fg = when {
        focused -> NiuniaColor.OnFocus
        kind == NiuniaRowKind.Suggestion || (kind == NiuniaRowKind.Tree && depth > 0) -> NiuniaColor.Text70
        else -> NiuniaColor.White
    }
    val sub = if (focused) NiuniaColor.OnFocus70 else NiuniaColor.Text70
    val labelStyle = when (kind) {
        NiuniaRowKind.Setting, NiuniaRowKind.Account -> NiuniaType.LabelSmall
        NiuniaRowKind.Nav, NiuniaRowKind.Suggestion, NiuniaRowKind.Tree -> NiuniaType.BodySmall
        else -> NiuniaType.Body
    }
    val start = when (kind) {
        NiuniaRowKind.Nav -> NiuniaSpacing.Space12
        NiuniaRowKind.Account, NiuniaRowKind.Suggestion -> NiuniaSpacing.Space8
        NiuniaRowKind.Tree -> when (depth) { 0 -> NiuniaSpacing.Space8; 1 -> NiuniaSpacing.Space24; else -> NiuniaSpacing.Space40 }
        else -> NiuniaSpacing.Space16
    }
    val end = when (kind) { NiuniaRowKind.Nav, NiuniaRowKind.Tree -> NiuniaSpacing.Space12; NiuniaRowKind.Account, NiuniaRowKind.Suggestion -> NiuniaSpacing.Space8; else -> NiuniaSpacing.Space16 }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = minH)
            .niuniaOk(label, focusRequester, enabled, onFocus = { focused = it; onFocus(it) }, onLongClick = onLongClick, onClick = onClick)
            .then(if (!enabled && dimDisabled) Modifier.alpha(NiuniaOpacity.OpacityDisabled) else Modifier)
            .clip(shape)
            .background(bg)
            .padding(start = start, end = end),
        horizontalArrangement = Arrangement.spacedBy(if (kind == NiuniaRowKind.Suggestion || kind == NiuniaRowKind.Tree) NiuniaSpacing.Space8 else NiuniaSpacing.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) leading(focused)
        else if (icon != null && kind == NiuniaRowKind.Nav) {
            Box(Modifier.size(NiuniaSize.Icon24).clip(CircleShape).background(if (focused) NiuniaColor.Fill12 else NiuniaColor.Fill24), contentAlignment = Alignment.Center) {
                NiuniaIcon(icon, size = NiuniaSpacing.Space12, tint = fg)
            }
        } else if (icon != null) NiuniaIcon(icon, size = NiuniaSize.Icon16, tint = if (focused) NiuniaColor.OnFocus70 else if (kind == NiuniaRowKind.Suggestion) NiuniaColor.Text50 else fg)
        Column(Modifier.weight(1f)) {
            // Wiersze panelu odtwarzacza: etykieta do 2 linii zamiast wielokropka (długa wartość, np. „bez ściszania”).
            Text(label, style = labelStyle, color = fg, maxLines = if (kind == NiuniaRowKind.Track) 2 else 1, overflow = TextOverflow.Ellipsis,
                modifier = if (kind == NiuniaRowKind.Track) Modifier.padding(vertical = NiuniaSpacing.Space4) else Modifier)
            if (supporting != null) Text(supporting, style = NiuniaType.CaptionRegular, color = if (focused) NiuniaColor.OnFocus70 else NiuniaColor.Text50, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (count != null) Text(count, style = NiuniaType.CaptionRegular, color = sub, maxLines = 1)
        if (value != null) Text(value, style = NiuniaType.BodySmall, color = sub, maxLines = 1)
        when (trailing) {
            NiuniaTrailing.Chevron -> NiuniaIcon("chevron-right", size = NiuniaSize.Icon16, tint = if (focused) NiuniaColor.OnFocus70 else NiuniaColor.Text50)
            NiuniaTrailing.Check -> NiuniaIcon("check", size = NiuniaSize.Icon16, tint = fg)
            NiuniaTrailing.SwitchOn -> NiuniaSwitch(true, onLight = focused)
            NiuniaTrailing.SwitchOff -> NiuniaSwitch(false, onLight = focused)
            NiuniaTrailing.None -> Unit
        }
    }
}

// ================================================================ NMediaCard

/**
 * Karta z obrazem (bez plakatu — plakaty rysuje [NiuniaPoster] z kolorem z grafiki, `NiuniaArtColors`).
 * Presety: ContinueWatchingCard / LandscapeCard / NowCard (160×114), StillCard (205×115), ChannelCard, ProgrammeTile.
 * Pasek podpisu u dołu: ▶ (gdy [play]), mini postęp, podpis; u góry kropka statusu i plakietka. Stany: ładowanie (migotanie), błąd
 * (podpisany zapas), nieaktywna 30%. Fokus: uniesienie, skala 1,1, cień i obwódka 3 dp.
 */
@Composable
fun NiuniaMediaCard(
    preset: NiuniaCardPreset,
    image: Any?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    caption: String? = null,
    progress: Int? = null,
    play: Boolean = false,
    badge: String? = null,
    badgeStrong: Boolean = false,
    status: NiuniaCardStatus? = null,
    label: String? = null,
    state: NiuniaCardState = NiuniaCardState.Ready,
    inactive: Boolean = false,
    tone: Int? = null,
    width: Dp = preset.width,
    height: Dp = preset.height,
    description: String? = title ?: caption,
    focusRequester: FocusRequester? = null,
    focusable: Boolean = true,
    onFocus: (Boolean) -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    // Przegląd 27.11 (10 pkt 2): elementy kafli Live TV w komponencie (domyślne = karta jak dawniej).
    captionRich: androidx.compose.ui.text.AnnotatedString? = null, // podpis do 2 linii („Tytuł · Kanał”), pasek postępu nad nim
    progressBar: Float? = null,
    progressColor: androidx.compose.ui.graphics.Color? = null,
    blurCaption: Boolean = false, // podpis na rozmytym dole obrazu (gotowe rozmycie), nie na szarym gradiencie
    topTag: (@Composable () -> Unit)? = null,
    fallback: (@Composable BoxScope.() -> Unit)? = null, // gdy obrazu brak albo się nie wczytał (np. logo kanału)
) {
    var focused by remember { mutableStateOf(false) }
    val parts = niuniaCardParts(preset, state, title, caption, progress, play, badge, status)
    var imageFailed by remember(image) { mutableStateOf(false) }
    val shape = RoundedCornerShape(NiuniaRadius.Radius16)
    Box(
        modifier
            .size(width, height)
            .then(if (focusable) Modifier.niuniaOk(description ?: preset.name, focusRequester, onFocus = { focused = it; onFocus(it) }, onLongClick = onLongClick, onClick = onClick) else Modifier)
            .niuniaFocusLook(preset.focusStyle, focused, shape)
            .then(if (inactive) Modifier.alpha(NiuniaOpacity.OpacityDim) else Modifier)
            .clip(shape)
            .background(
                when (tone) {
                    1 -> NiuniaGradient.Tile1.brush(); 2 -> NiuniaGradient.Tile2.brush(); 3 -> NiuniaGradient.Tile3.brush()
                    4 -> NiuniaGradient.Tile4.brush(); 5 -> NiuniaGradient.Tile5.brush()
                    else -> androidx.compose.ui.graphics.SolidColor(NiuniaColor.Surface)
                }
            )
            .semantics { if (description != null) contentDescription = description },
    ) {
        when {
            parts.shimmer -> NiuniaShimmer()
            parts.fallbackLabel != null -> NiuniaMedia(null, Modifier.fillMaxSize(), fallbackLabel = parts.fallbackLabel)
            fallback != null -> {
                if (image != null && !imageFailed) coil3.compose.AsyncImage(model = image, contentDescription = description, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                    onError = { st -> imageFailed = true; Log.w(DS_TAG, "NMediaCard: obraz „${description.orEmpty()}”: ${st.result.throwable.javaClass.simpleName}: ${st.result.throwable.message}") })
                val blurred = blurCaption && image is String && !imageFailed
                if (blurred) NiuniaBlurBottom(image as String) else if (image == null || imageFailed) fallback()
            }
            image != null -> NiuniaMedia(image, Modifier.fillMaxSize(), fallbackLabel = title ?: caption)
        }
        if (preset.kind == NiuniaCardKind.Tile && title != null) {
            Column(Modifier.align(Alignment.BottomStart).padding(NiuniaSpacing.Space12)) {
                Text(title, style = NiuniaType.Label, color = NiuniaColor.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = NiuniaType.CaptionRegular, color = NiuniaColor.Text70, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (parts.hasTopRow) Row(Modifier.padding(NiuniaSpacing.Space8), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4), verticalAlignment = Alignment.CenterVertically) {
            parts.statusDot?.let { NiuniaDot(it) }
            parts.badge?.let { NiuniaTag(it, if (badgeStrong) NiuniaTagKind.BadgeStrong else NiuniaTagKind.Badge) }
        }
        // Przegląd 27.11 (CAŁA APLIKACJA krok 2): pasek podpisu zwykłych kart na rozmytym dole obrazu (gotowe rozmycie Coil/dysk).
        val ctxBand = androidx.compose.ui.platform.LocalContext.current
        val bandBlurOn = remember(ctxBand) { com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(ctxBand, "niunia_blur_captions", true) }
        val bandBlur = captionRich == null && label == null && parts.hasCaptionBand && image is String && !imageFailed && bandBlurOn
        if (bandBlur) NiuniaBlurBottom(image as String, from = .55f, to = .8f)
        if (topTag != null) Box(Modifier.padding(start = NiuniaSpacing.Space8, top = NiuniaSpacing.Space8)) { topTag() }
        if (captionRich != null) {
            val blurred = blurCaption && image is String && !imageFailed
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(if (blurred) androidx.compose.ui.graphics.Brush.verticalGradient(0f to androidx.compose.ui.graphics.Color.Transparent, 1f to androidx.compose.ui.graphics.Color(0x59000000))
                        else NiuniaGradient.ScrimCaption.brush())
                    .padding(start = NiuniaSpacing.Space8, end = NiuniaSpacing.Space8, top = NiuniaSpacing.Space20, bottom = NiuniaSpacing.Space8),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (progressBar != null) NiuniaProgress(progressBar, bar = true, fillColor = progressColor)
                Text(captionRich, style = if (blurred) NiuniaType.Caption.copy(shadow = androidx.compose.ui.graphics.Shadow(androidx.compose.ui.graphics.Color(0x99000000), blurRadius = 6f)) else NiuniaType.Caption,
                    color = NiuniaColor.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        } else if (label != null) Text(label, style = NiuniaType.Label, color = NiuniaColor.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(NiuniaSpacing.Space12))
        else if (parts.hasCaptionBand) Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().background(
                if (bandBlur) androidx.compose.ui.graphics.Brush.verticalGradient(0f to androidx.compose.ui.graphics.Color.Transparent, 1f to androidx.compose.ui.graphics.Color(0x59000000))
                else NiuniaGradient.ScrimCaption.brush())
                .padding(start = NiuniaSpacing.Space8, end = NiuniaSpacing.Space8, top = NiuniaSpacing.Space20, bottom = NiuniaSpacing.Space8),
            horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8), verticalAlignment = Alignment.CenterVertically,
        ) {
            if (parts.playIcon) NiuniaIcon("play-small", size = NiuniaSpacing.Space12)
            parts.progress?.let { NiuniaProgress(it) }
            parts.caption?.let { Text(it, style = NiuniaType.Caption, color = NiuniaColor.Text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        // Delikatny połysk w lewym górnym rogu każdej karty (`.n-card::after`).
        Box(Modifier.matchParentSize().background(NiuniaGradient.Gloss.brush()))
    }
}

// ================================================================ NContentBlock

enum class NiuniaBlockKind { Info, Wide, Full, Panel, Credits, Empty, Status }

/** Blok treści: kolumna z odstępem 8, szer. do hero-info-w (420); Credits = 12 wąskich linii po prawej obok przycisków. */
@Composable
fun NiuniaContentBlock(kind: NiuniaBlockKind = NiuniaBlockKind.Info, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val m = when (kind) {
        NiuniaBlockKind.Info -> modifier.widthIn(max = NiuniaSize.HeroInfoW)
        NiuniaBlockKind.Wide -> modifier.widthIn(max = NiuniaSize.HeroInfoW + NiuniaSize.StillW + NiuniaSpacing.Space40 + NiuniaSpacing.Space16)
        NiuniaBlockKind.Full, NiuniaBlockKind.Status -> modifier.fillMaxWidth()
        NiuniaBlockKind.Panel -> with(com.nuvio.tv.fork.glass.ForkGlass) { modifier.width(NiuniaSize.HeroInfoW).forkGlass(com.nuvio.tv.fork.glass.ForkGlass.Role.Panel, RoundedCornerShape(NiuniaRadius.Radius16), fallback = NiuniaColor.Fill12) }
            .padding(horizontal = NiuniaSpacing.Space16, vertical = NiuniaSpacing.Space12)
        NiuniaBlockKind.Credits -> modifier.width(NiuniaSize.LandscapeW + NiuniaSpacing.Space40 * 2 + NiuniaSpacing.Space12)
        NiuniaBlockKind.Empty -> modifier
    }
    Column(m, verticalArrangement = Arrangement.spacedBy(if (kind == NiuniaBlockKind.Credits) NiuniaSpacing.Space4 else NiuniaSpacing.Space8), content = content)
}
