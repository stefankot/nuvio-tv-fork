package com.nuvio.tv.ui.screens.browse

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/** Echo shims: fade at scroll edges is not ported. */
internal fun Modifier.scrollFade(state: ScrollState, edge: Dp = 28.dp): Modifier = this
internal fun Modifier.scrollFade(state: LazyListState, edge: Dp = 28.dp): Modifier = this

/** Echo shim: a pill chip with Echo's signature. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun BrowsePill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    outlined: Boolean = true,
    onFocused: () -> Unit = {},
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier.onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.28f)
        )
    ) {
        Text(text = text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}
