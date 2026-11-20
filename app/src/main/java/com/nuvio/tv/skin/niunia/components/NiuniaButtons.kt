package com.nuvio.tv.skin.niunia.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaOpacity
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType

/*
 * NButton i NTag (molekuły design systemu Nuvio TV v2, components/NButton, components/NTag).
 * Zastępuje dawny przycisk Niuni (40 dp, skala 1,06): teraz 44 dp, min. 160 dp, skala 1,08, wygląd z tokenów.
 */

enum class NiuniaButtonKind { Standard, Icon, Bare, Chip, Tab, Segment, Key, KeyWide, Action }

/**
 * Przycisk. [label] i/lub [icon] (nazwa z zestawu NIcon) albo [glyph] (np. „UHD”). [progress] 0..1 = mini pasek przed etykietą
 * (Wznów). [compact] = bez min. szerokości 160. [active] = wypełnienie 36% (np. panel otwarty), [selected] = 24% (pigułki, zakładki).
 * OK = [onClick], długie OK (520 ms) = [onLongClick]. Wyłączony: 50% i pomijany przez fokus.
 */
@Composable
fun NiuniaButton(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: String? = null,
    glyph: String? = null,
    kind: NiuniaButtonKind = if (label == null) NiuniaButtonKind.Icon else NiuniaButtonKind.Standard,
    small: Boolean = false,
    compact: Boolean = false,
    progress: Float? = null,
    trailing: String? = null,
    labelFirst: Boolean = false,
    active: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    description: String? = label,
    focusRequester: FocusRequester? = null,
    onFocus: (Boolean) -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val chipLike = kind == NiuniaButtonKind.Chip || kind == NiuniaButtonKind.Tab || kind == NiuniaButtonKind.Segment
    val keyLike = kind == NiuniaButtonKind.Key || kind == NiuniaButtonKind.KeyWide
    val shape = if (keyLike) RoundedCornerShape(NiuniaRadius.Radius16) else CircleShape
    val height = when {
        kind == NiuniaButtonKind.Segment -> NiuniaSize.H36 - NiuniaSpacing.Space8
        chipLike || keyLike || small || kind == NiuniaButtonKind.Action -> NiuniaSize.H36
        kind == NiuniaButtonKind.Bare -> NiuniaSize.Icon28
        else -> NiuniaSize.H44
    }
    val bg = when {
        focused -> NiuniaColor.Focus
        active -> NiuniaColor.Fill36
        selected && (chipLike || kind == NiuniaButtonKind.Standard || kind == NiuniaButtonKind.Icon) -> NiuniaColor.Fill24
        chipLike || keyLike || kind == NiuniaButtonKind.Bare -> Color.Transparent
        else -> NiuniaColor.Button
    }
    val fg = when {
        focused -> NiuniaColor.OnFocus
        (chipLike && !selected) || keyLike -> NiuniaColor.Text70
        else -> NiuniaColor.White
    }
    val textStyle = when {
        kind == NiuniaButtonKind.Key -> NiuniaType.TitleSection.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        kind == NiuniaButtonKind.KeyWide -> NiuniaType.Caption
        kind == NiuniaButtonKind.Segment -> NiuniaType.BodySmall
        chipLike || small || kind == NiuniaButtonKind.Action -> NiuniaType.LabelSmall
        else -> NiuniaType.Label
    }
    val focusStyle = if (chipLike || keyLike) NiuniaFocusStyle.Fill else NiuniaFocusStyle.Button
    val square = kind == NiuniaButtonKind.Icon || kind == NiuniaButtonKind.Bare || kind == NiuniaButtonKind.Key
    val name = description ?: label ?: icon ?: glyph ?: "przycisk"
    Row(
        modifier
            .niuniaOk(name, focusRequester, enabled, onFocus = { focused = it; onFocus(it) }, onLongClick = onLongClick, onClick = onClick)
            .niuniaFocusLook(focusStyle, focused, shape)
            .then(if (!enabled) Modifier.alpha(NiuniaOpacity.OpacityDisabled) else Modifier)
            .height(height)
            .then(
                when {
                    square -> Modifier.width(height)
                    kind == NiuniaButtonKind.Standard && !compact && !small -> Modifier.widthIn(min = NiuniaSize.ButtonMinW)
                    else -> Modifier
                }
            )
            .clip(shape)
            .background(bg)
            .then(if (!focused && !chipLike && !keyLike && kind != NiuniaButtonKind.Bare && !active) Modifier.border(1.dp, NiuniaColor.Fill12, shape) else Modifier)
            .padding(
                horizontal = when {
                    square -> 0.dp
                    kind == NiuniaButtonKind.Segment || kind == NiuniaButtonKind.KeyWide -> NiuniaSpacing.Space8
                    chipLike || small -> NiuniaSpacing.Space16
                    else -> NiuniaSpacing.Space24
                }
            )
            .semantics { if (description != null) contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) NiuniaIcon(icon, size = if (kind == NiuniaButtonKind.Bare) NiuniaSize.Icon16 else NiuniaSize.Icon20, tint = fg)
        if (glyph != null) Text(glyph, style = NiuniaType.Caption, color = fg, maxLines = 1)
        // Kolejność jak w makiecie: zwykle ikona · postęp · etykieta; [labelFirst] = ikona · etykieta · postęp · [trailing] („Wznów ━ 39m”).
        if (labelFirst && label != null && !square) Text(label, style = textStyle, color = fg, maxLines = 1, overflow = TextOverflow.Clip)
        if (progress != null) NiuniaProgress(progress, onFocus = focused)
        if (!labelFirst && label != null && !square) Text(label, style = textStyle, color = fg, maxLines = 1, overflow = TextOverflow.Clip)
        else if (label != null && kind == NiuniaButtonKind.Key) Text(label, style = textStyle, color = fg, maxLines = 1)
        if (trailing != null) Text(trailing, style = textStyle, color = fg, maxLines = 1)
    }
}

// ================================================================ NTag

enum class NiuniaTagKind { Quality, Age, Live, Delay, Archive, Badge, BadgeStrong, Chip, Status, Rating, Text }

/** Znacznik: jakość (4K, HDR), wiek, LIVE (live-badge, 5,2:1), opóźnienie, archiwum, plakietka na obrazie, chip, ocena. */
@Composable
fun NiuniaTag(text: String, kind: NiuniaTagKind, modifier: Modifier = Modifier) {
    val (bg, fg) = when (kind) {
        NiuniaTagKind.Quality -> NiuniaColor.Fmt to NiuniaColor.OnFmt
        NiuniaTagKind.Live -> NiuniaColor.LiveBadge to NiuniaColor.White
        NiuniaTagKind.Delay -> NiuniaColor.Delay to NiuniaColor.OnDelay
        NiuniaTagKind.Archive -> NiuniaColor.Focus to NiuniaColor.OnFocus
        NiuniaTagKind.Badge -> NiuniaColor.BadgeOnImage to NiuniaColor.White
        NiuniaTagKind.BadgeStrong -> NiuniaColor.Focus to NiuniaColor.OnFocus
        NiuniaTagKind.Chip -> NiuniaColor.ChipOnImage to NiuniaColor.Text
        NiuniaTagKind.Status -> Color.Transparent to NiuniaColor.Archive
        NiuniaTagKind.Age, NiuniaTagKind.Rating, NiuniaTagKind.Text -> Color.Transparent to NiuniaColor.Text
    }
    val pill = kind == NiuniaTagKind.Badge || kind == NiuniaTagKind.BadgeStrong
    val shape = if (pill) CircleShape else RoundedCornerShape(NiuniaRadius.Radius6)
    val small = kind == NiuniaTagKind.Quality || kind == NiuniaTagKind.Age || pill
    val bare = kind == NiuniaTagKind.Status || kind == NiuniaTagKind.Rating || kind == NiuniaTagKind.Text
    Box(
        modifier
            .then(if (bare) Modifier else Modifier.height(if (small) NiuniaSize.Icon16 else NiuniaSize.TagH))
            .clip(shape)
            .background(bg)
            .then(
                when (kind) {
                    NiuniaTagKind.Age -> Modifier.border(1.dp, NiuniaColor.LineStrong, shape)
                    NiuniaTagKind.Chip -> Modifier.border(1.dp, NiuniaColor.OnImageRing, shape)
                    else -> Modifier
                }
            )
            .padding(horizontal = when { bare -> 0.dp; kind == NiuniaTagKind.Quality || kind == NiuniaTagKind.Age -> NiuniaSpacing.Space4; else -> NiuniaSpacing.Space8 }),
        contentAlignment = Alignment.Center,
    ) {
        val style = when (kind) {
            NiuniaTagKind.Status -> NiuniaType.LabelSmall
            NiuniaTagKind.Rating -> NiuniaType.BodySmall
            NiuniaTagKind.Live, NiuniaTagKind.Delay, NiuniaTagKind.Archive -> NiuniaType.Caption.copy(letterSpacing = NiuniaType.Caption.fontSize * 0.06f)
            else -> NiuniaType.Caption
        }
        Text(text, style = style, color = fg, maxLines = 1)
    }
}

/** Kolumna przycisku z podpisem pod spodem (`.n-btn__stack`), np. przyciski odtwarzacza YouTube. */
@Composable
fun NiuniaButtonStack(caption: String, modifier: Modifier = Modifier, button: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8)) {
        button()
        Text(caption, style = NiuniaType.BodySmall, color = NiuniaColor.Text, maxLines = 1)
    }
}

/** Kółko statusu do wiersza (np. w przewodniku). */
@Composable
fun NiuniaStatusTag(text: String, status: NiuniaCardStatus, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8), verticalAlignment = Alignment.CenterVertically) {
        NiuniaDot(status)
        Text(text, style = NiuniaType.LabelSmall, color = if (status == NiuniaCardStatus.Live) NiuniaColor.White else status.color, maxLines = 1)
    }
}

