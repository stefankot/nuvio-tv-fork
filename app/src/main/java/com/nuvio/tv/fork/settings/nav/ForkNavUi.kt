package com.nuvio.tv.fork.settings.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/** Wspólny wygląd menu ustawień forka (tokeny i komponenty skórki, jak „Ustawienia podstawowe”). */

/** Wiersz szyny kategorii: 36 dp, ikona 20, zaznaczony = Surface3, fokus = Focus. */
@Composable
internal fun NavRailItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocus: () -> Unit = {},
    onOk: () -> Unit,
) {
    val look = settingsLook()
    ForgeFocusBox(
        radius = look.railRadius,
        modifier = modifier.fillMaxWidth().height(36.dp),
        focusRequester = focusRequester,
        scaleOnFocus = false,
        onFocusChange = { if (it) onFocus() },
        onOk = onOk,
    ) { focused ->
        Row(
            Modifier.fillMaxSize().background(
                when { focused -> look.focus; selected -> look.selected; else -> Color.Transparent },
                RoundedCornerShape(look.railRadius),
            ).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val fg = if (focused) look.onFocus else look.text
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, style = ForgeText.label.copy(color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Wiersz panelu: 56 dp (z opisem 64), tytuł + opis, po prawej wartość albo strzałka. */
@Composable
internal fun NavRow(
    title: String,
    description: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    chevron: Boolean = true,
    on: Boolean? = null,
    onOk: () -> Unit,
) {
    val look = settingsLook()
    ForgeFocusBox(
        radius = look.rowRadius,
        modifier = modifier.fillMaxWidth().height(if (description.isNullOrEmpty()) 56.dp else 64.dp),
        focusRequester = focusRequester,
        scaleOnFocus = false,
        onOk = onOk,
    ) { focused ->
        Row(
            Modifier.fillMaxSize().background(if (focused) look.focus else look.row, RoundedCornerShape(look.rowRadius))
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val fg = if (focused) look.onFocus else look.text
            if (icon != null) { Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(14.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = ForgeText.shelfTitle.copy(color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!description.isNullOrEmpty()) Text(
                    description, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = ForgeText.label5.copy(color = if (focused) look.onFocus.copy(alpha = .7f) else look.text2),
                )
            }
            if (value != null) NavValuePill(value, focused, on)
            if (chevron) Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = if (focused) look.onFocus else look.text2,
                modifier = Modifier.padding(start = 12.dp).size(16.dp),
            )
        }
    }
}

/** Mała plakietka wartości („Włączone”, „Prosty”): zielona dla włączonych. */
@Composable
internal fun NavValuePill(value: String, focused: Boolean, on: Boolean? = null) {
    val look = settingsLook()
    val bg = when { on == true -> look.on; focused -> look.onFocus.copy(alpha = .12f); else -> look.selected }
    val fg = when { on == true -> look.onFocus; focused -> look.onFocus; else -> look.text }
    Text(
        value, style = ForgeText.label5.copy(color = fg), maxLines = 1,
        modifier = Modifier.background(bg, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Nagłówek grupy w panelu (WIELKIE LITERY, jak w „Ustawieniach podstawowych”). */
@Composable
internal fun NavGroupHeader(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = ForgeText.captionBold.copy(color = settingsLook().text2), maxLines = 1, modifier = modifier.padding(top = 12.dp, bottom = 6.dp))
}
