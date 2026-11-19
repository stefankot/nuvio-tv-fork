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
    ForgeFocusBox(
        radius = 8.dp,
        modifier = modifier.fillMaxWidth().height(36.dp),
        focusRequester = focusRequester,
        scaleOnFocus = false,
        onFocusChange = { if (it) onFocus() },
        onOk = onOk,
    ) { focused ->
        Row(
            Modifier.fillMaxSize().background(
                when { focused -> ForgeColors.Focus; selected -> ForgeColors.Surface3; else -> Color.Transparent },
                RoundedCornerShape(8.dp),
            ).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
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
    ForgeFocusBox(
        radius = 8.dp,
        modifier = modifier.fillMaxWidth().height(if (description.isNullOrEmpty()) 56.dp else 64.dp),
        focusRequester = focusRequester,
        scaleOnFocus = false,
        onOk = onOk,
    ) { focused ->
        Row(
            Modifier.fillMaxSize().background(if (focused) ForgeColors.Focus else ForgeColors.Surface, RoundedCornerShape(8.dp))
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
            if (icon != null) { Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(14.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = ForgeText.shelfTitle.copy(color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!description.isNullOrEmpty()) Text(
                    description, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = ForgeText.label5.copy(color = if (focused) ForgeColors.OnFocus.copy(alpha = .7f) else ForgeColors.Text2),
                )
            }
            if (value != null) NavValuePill(value, focused, on)
            if (chevron) Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = if (focused) ForgeColors.OnFocus else ForgeColors.Text2,
                modifier = Modifier.padding(start = 12.dp).size(16.dp),
            )
        }
    }
}

/** Mała plakietka wartości („Włączone”, „Prosty”): zielona dla włączonych. */
@Composable
internal fun NavValuePill(value: String, focused: Boolean, on: Boolean? = null) {
    val bg = when { on == true -> ForgeColors.Switch; focused -> ForgeColors.OnFocus.copy(alpha = .12f); else -> ForgeColors.Surface3 }
    val fg = when { on == true -> ForgeColors.OnFocus; focused -> ForgeColors.OnFocus; else -> ForgeColors.Text }
    Text(
        value, style = ForgeText.label5.copy(color = fg), maxLines = 1,
        modifier = Modifier.background(bg, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Nagłówek grupy w panelu (WIELKIE LITERY, jak w „Ustawieniach podstawowych”). */
@Composable
internal fun NavGroupHeader(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = ForgeText.captionBold.copy(color = ForgeColors.Text2), maxLines = 1, modifier = modifier.padding(top = 12.dp, bottom = 6.dp))
}
