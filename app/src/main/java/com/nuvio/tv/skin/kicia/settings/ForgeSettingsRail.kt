package com.nuvio.tv.skin.kicia.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/**
 * A category of the Settings rail as the mockup draws it (S13, c10): 42 dp, icon 20 + bold label, the open
 * category on glass, the focused one white. Same inputs as Nuvio's rail button.
 */
@Composable
fun ForgeSettingsRailButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    icon: ImageVector?,
    painter: Painter?,
    modifier: Modifier = Modifier,
) {
    ForgeFocusBox(
        radius = 8.dp, // v7.4: Wiersz-szyna r 8
        modifier = modifier.padding(vertical = 2.dp).fillMaxWidth().height(36.dp), // mockup: 36 dp rows 4 apart
        scaleOnFocus = false,
        focusRequester = focusRequester,
        onFocusChange = { if (it) onFocused() },
        onOk = onClick,
    ) { focused ->
        val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
        Row(
            Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                .background(if (focused) ForgeColors.Focus else if (selected) ForgeColors.Surface3 else ForgeColors.Surface2) // v7.4: every rail row is a tile (surface), selected 24%
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                painter != null -> Image(painter, null, modifier = Modifier.size(16.dp), colorFilter = ColorFilter.tint(fg))
                icon != null -> Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(title, style = ForgeText.label.copy(color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis) // v7.4: 14/700
        }
    }
}
