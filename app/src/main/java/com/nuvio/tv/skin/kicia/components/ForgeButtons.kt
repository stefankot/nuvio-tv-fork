package com.nuvio.tv.skin.kicia.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/**
 * Text button, 44 dp tall (or 36 dp when [small]). Resting: glass surface; focused: solid white with
 * dark text. A [primary] button is white already at rest and gets a ring when focused (one per screen).
 */
@Composable
fun ForgeButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    small: Boolean = false,
    /** 44 dp, as the Play button of the details page (owner 10.11: the Home hero's was too small). */
    large: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false, // v7.4: kółko zamiast ikony
    focusRequester: FocusRequester? = null,
    onLongOk: (() -> Unit)? = null,
    onOk: () -> Unit,
) {
    val height = if (large) 44.dp else if (small) ForgeDims.ButtonSmHeight else ForgeDims.ButtonHeight
    val radius = height / 2
    ForgeFocusBox(
        radius = radius,
        modifier = modifier.height(height).alpha(if (enabled) 1f else .5f), // v7.4: wyłączony = krycie .5 całego elementu
        outline = false, // v7.4: fokus nigdy nie jest obrysem
        enabled = enabled,
        focusRequester = focusRequester,
        onLongOk = onLongOk,
        onOk = onOk,
    ) { focused ->
        // v7.4 (SYSTEM): główny = białe 24%, dodatkowy = 12%, fokus = #EBEBE8 z tekstem #14161A
        val bg = if (focused) ForgeColors.Focus else if (primary) ForgeColors.Surface3 else ForgeColors.Surface
        val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
        Row(
            modifier = Modifier
                .height(height)
                .clip(RoundedCornerShape(radius))
                .background(bg)
                .defaultMinSize(minWidth = if (small) 0.dp else 160.dp) // v7.4: przycisk-min-szer
                .padding(horizontal = if (small) 16.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            val iconSize = if (small) 16.dp else 20.dp // v7.4: ikona 20, w małym 16
            if (loading) ForgeSpinner(iconSize, fg) else if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(iconSize))
            Text(text = text, style = (if (small) ForgeText.label else ForgeText.button).copy(color = fg)) // v7.4: mały 14/700
        }
    }
}

/**
 * Round icon button. In players the name of the action appears under the icon while it has focus
 * (the pattern of the official YouTube TV app) - pass [label] for that.
 */
@Composable
fun ForgeIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = ForgeDims.IconButton,
    label: String? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false,
    focusRequester: FocusRequester? = null,
    onLongOk: (() -> Unit)? = null,
    onOk: () -> Unit,
) {
    ForgeFocusBox(
        radius = size / 2,
        modifier = modifier.size(size).alpha(if (enabled) 1f else .5f),
        enabled = enabled,
        focusRequester = focusRequester,
        onLongOk = onLongOk,
        onOk = onOk,
    ) { focused ->
        val bg = when {
            focused -> ForgeColors.Focus
            active -> ForgeColors.Surface3
            else -> ForgeColors.Surface
        }
        val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
        Box(
            modifier = Modifier.size(size).clip(RoundedCornerShape(size / 2)).background(bg),
            contentAlignment = Alignment.Center,
        ) {
            val iconSize = if (size <= 36.dp) 24.dp else 28.dp // v7.4: ikona 28 w kole 44, 24 w kole 36
            if (loading) ForgeSpinner(iconSize, fg) else Icon(icon, contentDescription = contentDescription, tint = fg, modifier = Modifier.size(iconSize))
        }
        if (focused && label != null) {
            Box(
                modifier = Modifier
                    .requiredWidth(160.dp)
                    .offset(x = (size - 160.dp) / 2, y = size + 6.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = label,
                    textAlign = TextAlign.Center,
                    style = ForgeText.captionBold,
                    modifier = Modifier
                        .wrapContentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xDB14161A)) // rgba(20,22,26,.86)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/** Pill used for sub-tabs, filters and the day picker (36 dp). Selected but unfocused = a slightly brighter glass. */
@Composable
fun ForgePillSelect(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onLongOk: (() -> Unit)? = null,
    /** A dropdown pill of the mockup: "Typ  Filmy ▾" (label lighter, value bold, chevron). */
    label: String? = null,
    chevron: Boolean = false,
    /** Lighter text after the value, e.g. the count in "Wszystko 15". */
    trailing: String? = null,
    /** v7.4 Pigułka wyboru: no fill at rest (selected 24%, focus #EBEBE8). */
    bare: Boolean = true, // v7.4: makieta ma tylko taką pigułkę
    enabled: Boolean = true,
    onOk: () -> Unit,
) {
    val radius = ForgeDims.RadiusPillSelect
    ForgeFocusBox(
        radius = radius,
        modifier = modifier.height(ForgeDims.PillSelectHeight).alpha(if (enabled) 1f else .5f),
        enabled = enabled,
        scaleOnFocus = false, // v6: selection pills do not scale
        focusRequester = focusRequester,
        onFocusChange = onFocusChange,
        onLongOk = onLongOk,
        onOk = onOk,
    ) { focused ->
        val bg = when {
            focused -> ForgeColors.Focus
            selected -> ForgeColors.Surface3
            bare -> androidx.compose.ui.graphics.Color.Transparent
            else -> ForgeColors.Surface
        }
        val fg = if (focused) ForgeColors.OnFocus else if (bare && !selected) ForgeColors.Text2 else ForgeColors.Text
        Row(
            modifier = Modifier
                .height(ForgeDims.PillSelectHeight)
                .clip(RoundedCornerShape(radius))
                .background(bg)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            if (label != null) Text(text = label, style = ForgeText.label5.copy(color = if (focused) ForgeColors.OnFocus.copy(alpha = .7f) else ForgeColors.Text2))
            Text(text = text, style = ForgeText.label.copy(color = fg))
            if (trailing != null) Text(text = trailing, style = ForgeText.label5.copy(color = if (focused) ForgeColors.OnFocus.copy(alpha = .7f) else ForgeColors.Text2))
            if (chevron) Icon(androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        }
    }
}

/** v7.4 Kółko (ładowanie): a ring in the text colour with a gap, one turn in 1200 ms. */
@Composable
fun ForgeSpinner(size: Dp, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val turn = androidx.compose.animation.core.rememberInfiniteTransition(label = "forgeSpinner").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween<Float>(1200, easing = androidx.compose.animation.core.LinearEasing)),
        label = "forgeSpinnerTurn",
    )
    androidx.compose.foundation.Canvas(modifier.size(size)) {
        val w = 2.dp.toPx()
        drawArc(color, startAngle = turn.value, sweepAngle = 270f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w),
            style = androidx.compose.ui.graphics.drawscope.Stroke(w))
    }
}

/** v7.4 Szkielet: a flat surface shape shown while data loads. */
@Composable
fun ForgeSkeleton(width: Dp, height: Dp, radius: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(width, height).clip(RoundedCornerShape(radius)).background(ForgeColors.Surface2))
}

/** v7.4 Awatar 44 / 72 / 120: picture, or the initial on the surface colour. */
@Composable
fun ForgeAvatar(name: String, size: Dp, modifier: Modifier = Modifier, imageUrl: String? = null) {
    val style = when { size >= 120.dp -> ForgeText.display; size >= 72.dp -> ForgeText.title; else -> ForgeText.head }
    Box(modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape).background(ForgeColors.Surface2), contentAlignment = Alignment.Center) {
        Text((name.trim().firstOrNull() ?: '?').uppercaseChar().toString(), style = style.copy(color = ForgeColors.Text, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified))
        if (!imageUrl.isNullOrBlank()) coil3.compose.AsyncImage(model = imageUrl, contentDescription = name, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape))
    }
}

/** v7.4 Przełącznik 44 × 24, gałka 20. */
@Composable
fun ForgeSwitch(on: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.size(ForgeDims.SwitchWidth, ForgeDims.SwitchHeight).clip(RoundedCornerShape(12.dp)).background(if (on) ForgeColors.Switch else ForgeColors.Surface3).padding(2.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) { Box(Modifier.size(ForgeDims.SwitchKnob).clip(androidx.compose.foundation.shape.CircleShape).background(androidx.compose.ui.graphics.Color.White)) }
}

/** v7.4 Pole tekstowe: 44 dp, pełna kapsuła, 12%, odstęp boczny 20, ikona 20 + 8, tekst 16/22 (podpowiedź 50%). */
@Composable
fun ForgeField(
    text: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = androidx.compose.material.icons.Icons.Outlined.Search,
    focusRequester: FocusRequester? = null,
    onOk: () -> Unit,
) {
    ForgeFocusBox(
        radius = ForgeDims.RadiusButton,
        modifier = modifier.height(ForgeDims.ButtonHeight).alpha(if (enabled) 1f else .5f),
        scaleOnFocus = false,
        enabled = enabled,
        focusRequester = focusRequester,
        onOk = onOk,
    ) { focused ->
        val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
        Row(
            (if (fill) Modifier.fillMaxWidth() else Modifier).height(ForgeDims.ButtonHeight).clip(RoundedCornerShape(ForgeDims.RadiusButton))
                .background(if (focused) ForgeColors.Focus else ForgeColors.Surface)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Text(
                text = text.ifEmpty { placeholder },
                style = ForgeText.body.copy(color = if (text.isEmpty() && !focused) ForgeColors.Text3 else fg),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}
