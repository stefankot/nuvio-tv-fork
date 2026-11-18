package com.nuvio.tv.skin.kicia.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.input.forgeOkKeys
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/**
 * Makieta 7.7 A12: Suwak. Above the track the level's name 14/700 and a chevron 16 (70%). Track: the given width,
 * 24 high, full radius, rgba(255,255,255,.12); fill #4A7DEB up to the knob's centre; knob 28 white with the focus
 * shadow; 4 dp dots (50%) at the levels. ◀▶ change the level at once, ▲▼ leave (not consumed). Focused: the
 * track gets the focus fill like a Wiersz.
 */
@Composable
fun ForgeSlider(
    levels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 240.dp,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    /** OK / long OK on the track (the same key handling as every Forge button). */
    onOk: (() -> Unit)? = null,
    onLongOk: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val last = (levels.size - 1).coerceAtLeast(1)
    val index = selected.coerceIn(0, levels.lastIndex.coerceAtLeast(0))
    Column(modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(levels.getOrElse(index) { "" }, style = ForgeText.label)
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = ForgeColors.Text2, modifier = Modifier.size(16.dp))
        }
        // test 15.11 (JMGO, B-2): inside BoxWithConstraints the focusable lost the focus after ~1 s (OK then did nothing);
        // a plain Box with the measured width keeps it
        var trackPx by remember { mutableStateOf(0) }
        val density = androidx.compose.ui.platform.LocalDensity.current
        Box(
            Modifier.fillMaxWidth().height(28.dp)
                .onSizeChanged { trackPx = it.width }
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionLeft -> { if (index > 0) onSelect(index - 1); true }
                        Key.DirectionRight -> { if (index < levels.lastIndex) onSelect(index + 1); true }
                        else -> false
                    }
                }
                .then(if (onOk != null) Modifier.forgeOkKeys(onLongOk = onLongOk, onOk = onOk) else Modifier)
                .focusable(),
            contentAlignment = Alignment.CenterStart,
        ) {
            val trackW = with(density) { trackPx.toDp() }
            val knobX = trackW * (index.toFloat() / last)
            Box(Modifier.fillMaxWidth().height(24.dp).clip(CircleShape).background(if (focused) ForgeColors.Focus.copy(alpha = .24f) else Color(0x1FFFFFFF))) {
                Box(Modifier.width(knobX).fillMaxHeight().background(Color(0xFF4A7DEB)))
            }
            levels.indices.forEach { i ->
                Box(Modifier.offset(x = trackW * (i.toFloat() / last) - 2.dp).size(4.dp).clip(CircleShape).background(Color.White.copy(alpha = .5f)))
            }
            Box(Modifier.offset(x = knobX - 14.dp).size(28.dp).shadow(if (focused) 8.dp else 2.dp, CircleShape).clip(CircleShape).background(Color.White))
        }
    }
}
