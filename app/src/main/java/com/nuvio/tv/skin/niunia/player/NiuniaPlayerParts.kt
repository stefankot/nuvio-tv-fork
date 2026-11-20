package com.nuvio.tv.skin.niunia.player

import com.nuvio.tv.skin.niunia.components.NiuniaFocusStyle
import com.nuvio.tv.skin.niunia.components.NiuniaMedia
import com.nuvio.tv.skin.niunia.components.NiuniaProgress
import com.nuvio.tv.skin.niunia.components.NiuniaTag
import com.nuvio.tv.skin.niunia.components.NiuniaTagKind
import com.nuvio.tv.skin.niunia.components.niuniaFocusLook
import com.nuvio.tv.skin.niunia.components.niuniaOk
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor as DsColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius as DsRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize as DsSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing as DsSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType as DsType

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.nuvio.tv.skin.niunia.components.NiuniaStatusDot
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpace
import com.nuvio.tv.skin.niunia.tokens.NiuniaText

/*
 * Wspólne części odtwarzaczy Niuni (canvas Player*, nuvio.css: .pline/.ptrack, .card.w, .btn.rnd, .shelf-title, .pdeep).
 * Każda część ma wariant „focused” podawany z zewnątrz, bo IPTV prowadzi fokus klawiszami (jak MiniGuide Kicii),
 * a YouTube/film — fokusem Compose.
 */

/** Stan odtwarzania = kolor paska (SKIN.md „One progress line”): biały film/archiwum, czerwony na żywo/YouTube, żółty z opóźnieniem. */
internal enum class NiuniaPlayState { FILM, LIVE, DELAY, ARCHIVE, YOUTUBE }

internal fun NiuniaPlayState.color(): Color = when (this) {
    NiuniaPlayState.LIVE, NiuniaPlayState.YOUTUBE -> com.nuvio.tv.skin.niunia.tokens.NiuniaStatus.Live
    NiuniaPlayState.DELAY -> com.nuvio.tv.skin.niunia.tokens.NiuniaStatus.Delay
    NiuniaPlayState.FILM, NiuniaPlayState.ARCHIVE -> NiuniaColors.Focus
}

/**
 * Tor paska (.ptrack): 8 dp (z fokusem 10 dp), wypełnienie w kolorze stanu, opcjonalnie bufor (jaśniejszy, do „teraz”),
 * kreska krawędzi na żywo (.edge), przerwy rozdziałów (.gap), gałka 16 dp (z fokusem 20 dp z poświatą).
 * Wszystkie wartości 0..1.
 */
@Composable
internal fun NiuniaTrack(
    played: Float,
    color: Color,
    focused: Boolean,
    modifier: Modifier = Modifier,
    buffer: Float? = null,
    edge: Float? = null,
    gaps: List<Float> = emptyList(),
    knob: Float? = played,
    knobColor: Color = color,
) {
    Canvas(modifier.height(20.dp)) {
        val h = (if (focused) 10.dp else 8.dp).toPx()
        val y = (size.height - h) / 2f
        val r = CornerRadius(h / 2f)
        drawRoundRect(NiuniaColors.Fill24, Offset(0f, y), Size(size.width, h), r)
        buffer?.let { drawRoundRect(NiuniaColors.Fill36, Offset(0f, y), Size(size.width * it.coerceIn(0f, 1f), h), r) }
        drawRoundRect(color, Offset(0f, y), Size(size.width * played.coerceIn(0f, 1f), h), r)
        gaps.forEach { g -> drawRect(Color(0x8C000000), Offset(size.width * g.coerceIn(0f, 1f) - 1.5.dp.toPx(), y), Size(3.dp.toPx(), h)) }
        edge?.let { e -> drawRoundRect(com.nuvio.tv.skin.niunia.tokens.NiuniaStatus.Live, Offset(size.width * e.coerceIn(0f, 1f) - 1.5.dp.toPx(), y - 4.dp.toPx()), Size(3.dp.toPx(), h + 8.dp.toPx()), CornerRadius(2.dp.toPx())) }
        knob?.let { k ->
            val cx = size.width * k.coerceIn(0f, 1f)
            if (focused) drawCircle(Color(0x47FFFFFF), radius = 15.dp.toPx(), center = Offset(cx, size.height / 2f))
            drawCircle(knobColor, radius = (if (focused) 10.dp else 8.dp).toPx(), center = Offset(cx, size.height / 2f))
        }
    }
}

/** Etykieta pod gałką (.plab: 12/16 bold, odstęp liter 1,2). [at] 0..1 względem toru o szerokości [trackWidth]. */
@Composable
internal fun BoxScope.NiuniaTrackLabel(text: String, at: Float, trackWidth: Dp, color: Color, small: Boolean = false) {
    Text(
        text, style = NiuniaText.Caption.copy(fontSize = if (small) 10.sp else 12.sp, lineHeight = 16.sp, letterSpacing = 1.2.sp), color = color, maxLines = 1,
        modifier = Modifier.align(Alignment.TopStart).offset(x = trackWidth * at.coerceIn(0f, 1f) - 18.dp, y = 22.dp).width(36.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

/** Okrągły przycisk 40×40 z ikoną (.btn.rnd); fokus = białe tło, ciemna ikona. Etykieta pod spodem (opcjonalnie). */
@Composable
internal fun NiuniaIconCircle(icon: ImageVector, description: String, focused: Boolean, label: String? = null, modifier: Modifier = Modifier) {
    // NButton icon (design system v2): 44 dp, w spoczynku `button` z linią 1 dp, fokus biały ×1,08 z cieniem.
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            Modifier.size(DsSize.H44).niuniaFocusLook(NiuniaFocusStyle.Button, focused, CircleShape)
                .background(if (focused) DsColor.Focus else DsColor.Button, CircleShape)
                .then(if (focused) Modifier else Modifier.border(1.dp, DsColor.Fill12, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = description, tint = if (focused) DsColor.OnFocus else DsColor.White, modifier = Modifier.size(DsSize.Icon20))
        }
        if (label != null) Text(label, style = DsType.BodySmall, color = DsColor.Text, maxLines = 1, modifier = Modifier.padding(top = DsSpacing.Space8))
    }
}

/** [NiuniaIconCircle] z fokusem Compose. */
@Composable
internal fun NiuniaIconButton(icon: ImageVector, description: String, focusRequester: FocusRequester? = null, onFocus: ((Boolean) -> Unit)? = null, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.size(DsSize.H44).niuniaOk(description, focusRequester, onFocus = { focused = it; onFocus?.invoke(it) }, onClick = onOk)) {
        NiuniaIconCircle(icon, description, focused)
    }
}

/** NButton small (36 dp, 14/20 pogrubione, odstęp 8; z ikoną albo kropką statusu). */
@Composable
internal fun NiuniaPill(text: String, focused: Boolean, icon: ImageVector? = null, height: Dp = DsSize.H36, dot: Color? = null, modifier: Modifier = Modifier) {
    val shape = CircleShape
    Row(
        modifier.height(height).niuniaFocusLook(NiuniaFocusStyle.Button, focused, shape)
            .background(if (focused) DsColor.Focus else DsColor.Button, shape)
            .then(if (focused) Modifier else Modifier.border(1.dp, DsColor.Fill12, shape))
            .padding(start = if (icon != null || dot != null) DsSpacing.Space12 else DsSpacing.Space16, end = DsSpacing.Space16),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DsSpacing.Space8),
    ) {
        if (dot != null) Box(Modifier.size(DsSize.Dot).background(dot, CircleShape))
        if (icon != null) Icon(icon, contentDescription = null, tint = if (focused) DsColor.OnFocus else DsColor.White, modifier = Modifier.size(DsSize.Icon20))
        Text(text, style = DsType.LabelSmall, color = if (focused) DsColor.OnFocus else DsColor.White, maxLines = 1)
    }
}

/** [NiuniaPill] z fokusem Compose. */
@Composable
internal fun NiuniaPillButton(text: String, icon: ImageVector? = null, focusRequester: FocusRequester? = null, onFocus: ((Boolean) -> Unit)? = null, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.niuniaOk(text, focusRequester, onFocus = { focused = it; onFocus?.invoke(it) }, onClick = onOk)) {
        NiuniaPill(text, focused, icon)
    }
}

/**
 * Kafel 16:9 rzędu pod odtwarzaczem = NMediaCard StillCard (rozmiar z decyzji 200×113, 4 + kawałek piątego): obraz, pasek
 * podpisu (`scrim-caption`), kropka statusu albo plakietka, nieaktywny [dim] (YouTube: 55 % bez fokusu, decyzja), postęp.
 * Fokus: uniesienie 4 dp, ×1,1, cień i obwódka 3 dp.
 */
@Composable
internal fun NiuniaWideTile(
    image: Any?,
    caption: String?,
    focused: Boolean,
    modifier: Modifier = Modifier,
    dot: Boolean? = null,
    badge: String? = null,
    dim: Boolean = false,
    progress: Float? = null,
    fallbackTitle: String? = null,
    fallbackSub: String? = null,
) {
    val shape = RoundedCornerShape(DsRadius.Radius16)
    Box(
        modifier.size(NiuniaSpace.TileW, NiuniaSpace.TileH)
            .niuniaFocusLook(NiuniaFocusStyle.Lift, focused, shape)
            .graphicsLayer { alpha = if (dim && !focused) 0.55f else 1f }
            .clip(shape)
            .background(com.nuvio.tv.skin.niunia.tokens.NiuniaGradient.Tile1.brush()),
    ) {
        if (image != null) NiuniaMedia(image, Modifier.fillMaxSize(), alt = caption ?: fallbackTitle, fallbackLabel = fallbackTitle ?: caption)
        else if (fallbackTitle != null) {
            Column(Modifier.align(Alignment.BottomStart).padding(DsSpacing.Space12)) {
                Text(fallbackTitle, style = DsType.Label, color = DsColor.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                fallbackSub?.let { Text(it, style = DsType.CaptionRegular, color = DsColor.Text70, maxLines = 1) }
            }
        }
        if (caption != null || (progress != null && progress > 0f)) Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().background(com.nuvio.tv.skin.niunia.tokens.NiuniaGradient.ScrimCaption.brush())
                .padding(start = DsSpacing.Space8, end = DsSpacing.Space8, top = DsSpacing.Space20, bottom = DsSpacing.Space8),
            horizontalArrangement = Arrangement.spacedBy(DsSpacing.Space8), verticalAlignment = Alignment.CenterVertically,
        ) {
            if (progress != null && progress > 0f) NiuniaProgress(progress)
            if (caption != null) Text(caption, style = DsType.Caption, color = DsColor.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        when {
            dot != null -> NiuniaStatusDot(live = dot, modifier = Modifier.align(Alignment.TopStart))
            badge != null -> NiuniaTag(badge, NiuniaTagKind.Badge, Modifier.align(Alignment.TopStart).padding(DsSpacing.Space8))
        }
        Box(Modifier.matchParentSize().background(com.nuvio.tv.skin.niunia.tokens.NiuniaGradient.Gloss.brush()))
    }
}

/** Kafel tekstowy = NMediaCard ChapterCard / ScheduleCard (160×114, `surface`, padding 12/16): fokus białe tło bez skali. */
@Composable
internal fun NiuniaTextTile(top: String, middle: String, bottomLeft: String?, bottomRight: String?, focused: Boolean, big: Boolean = false, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(DsRadius.Radius16)
    val fg = if (focused) DsColor.OnFocus else DsColor.Text
    val sub = if (focused) DsColor.OnFocus70 else DsColor.Text70
    Column(
        modifier.size(DsSize.LandscapeW, DsSize.LandscapeH).zIndex(if (focused) 1f else 0f)
            .background(if (focused) DsColor.Focus else DsColor.Surface, shape)
            .padding(horizontal = DsSpacing.Space16, vertical = DsSpacing.Space12),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(top, style = DsType.Caption, color = sub, maxLines = 1)
        Text(middle, style = if (big) DsType.TitleScreen else DsType.Label, color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(bottomLeft ?: " ", style = DsType.Caption, color = if (big) fg else sub, maxLines = 1)
            if (!bottomRight.isNullOrBlank()) Text(bottomRight, style = DsType.CaptionRegular, color = if (focused) DsColor.OnFocus70 else DsColor.Text50, maxLines = 1)
        }
    }
}

/** Tytuł sekcji (.shelf-title: 16/22 bold; bez fokusu w sekcji 50 % bieli). */
@Composable
internal fun NiuniaShelfTitle(text: String, on: Boolean = true, modifier: Modifier = Modifier) {
    com.nuvio.tv.skin.niunia.components.NiuniaCollectionHeader(text, active = on, modifier = modifier)
}

/** Osoba: kółko z inicjałami (bez zdjęć, decyzja 20.11), imię i rola od lewej. */
@Composable
internal fun NiuniaPersonInitials(name: String, role: String?, focused: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.width(100.dp)) {
        val s by animateFloatAsState(if (focused) NiuniaDims.ScaleFocus else 1f, tween(180), label = "niuniaPerson")
        Box(
            Modifier.size(84.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(CircleShape)
                .background(if (focused) NiuniaColors.Focus else NiuniaColors.Surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(niuniaInitials(name), style = NiuniaText.Logo.copy(fontSize = 26.sp, lineHeight = 32.sp), color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text50)
        }
        Text(name, style = NiuniaText.Meta.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), color = if (focused) NiuniaColors.Text else NiuniaColors.Text70,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        role?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = NiuniaText.Body.copy(fontSize = 12.sp, lineHeight = 16.sp), color = NiuniaColors.Text50, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

internal fun niuniaInitials(name: String): String = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/** Informacje: siatka 3 kolumn, etykieta wersalikami 12 sp + wartość 14 sp; puste pola pominięte (wołający filtruje). */
@Composable
internal fun NiuniaInfoFields(fields: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(modifier.width(880.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        fields.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.width(272.dp)) {
                        Text(label.uppercase(), style = NiuniaText.Caption.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp), color = NiuniaColors.Text50, maxLines = 1)
                        Text(value, style = NiuniaText.Meta, color = NiuniaColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

/** Mocno rozmyte tło stron ▼▼ (.pdeep: blur 70, jasność 0,4) — tylko natywnie (API 31+); na Androidzie 11 zwykłe ciemne tło. */
@Composable
internal fun NiuniaDeepBackground(image: Any?) {
    Box(Modifier.fillMaxSize().background(NiuniaColors.Bg)) {
        val blurOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        if (image != null && blurOk) {
            AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(70.dp))
        }
        Box(Modifier.fillMaxSize().background(if (blurOk) Color(0x99141416) else Color(0xF0141416)))
    }
}

/** Znacznik wieku (.age: obwódka 70 % bieli, 9/1 bold) i jakości (.fmt: tło 60 % bieli). */
@Composable
internal fun NiuniaChip(text: String, filled: Boolean = false) {
    val shape = RoundedCornerShape(3.dp)
    Text(
        text, style = NiuniaText.Tab.copy(fontSize = 9.sp, lineHeight = 14.sp), color = if (filled) Color(0xFF1C1C1E) else NiuniaColors.Text,
        modifier = Modifier.then(if (filled) Modifier.background(Color(0x99FFFFFF), shape) else Modifier.border(1.dp, Color(0xB3FFFFFF), shape)).padding(horizontal = 4.dp),
    )
}

/** Znacznik stanu IPTV (.pst): LIVE czerwony, −12:40 żółty, ARCHIWUM biały. */
@Composable
internal fun NiuniaStateChip(text: String, state: NiuniaPlayState) {
    val bg = state.color()
    val fg = when (state) { NiuniaPlayState.DELAY -> Color(0xFF1B1500); NiuniaPlayState.ARCHIVE, NiuniaPlayState.FILM -> NiuniaColors.OnFocus; else -> Color.White }
    Text(text, style = NiuniaText.Caption.copy(fontSize = 12.sp, lineHeight = 20.sp, letterSpacing = 0.8.sp), color = fg,
        modifier = Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp))
}

/** Przyciemnienie dołu kadru (gradient z makiet odtwarzaczy). */
@Composable
internal fun BoxScope.NiuniaPlayerScrim(from: Float = 0.34f) {
    Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color.Transparent, from to Color.Transparent, ((from + 1f) / 2f) to Color(0x8C000000), 1f to Color(0xE6000000))))
}
