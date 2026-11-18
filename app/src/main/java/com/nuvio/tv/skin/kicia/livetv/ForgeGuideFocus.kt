package com.nuvio.tv.skin.kicia.livetv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester

/**
 * S07: ▲ from the first channel of the guide goes to the day row (mockup), not to whatever the focus search finds
 * (it picked the recent row). The header registers the day pill; the guide asks for it.
 */
object ForgeGuideFocus {
    private var dayRow: FocusRequester? = null

    @Composable
    fun rememberDayRow(): FocusRequester {
        val requester = remember { FocusRequester() }
        DisposableEffect(requester) {
            dayRow = requester
            onDispose { if (dayRow === requester) dayRow = null }
        }
        return requester
    }

    /** v7.4 (6.7, W-68): the tool used last in the tools row (default "Kategorie"); ▲ from the grid goes there, never to the days. */
    var lastTool: FocusRequester? = null

    /** Moves the focus to the tools row (the last used tool, else the day pill); false when there is none (variant B, header hidden). */
    fun toDayRow(): Boolean = (lastTool ?: dayRow)?.let { runCatching { it.requestFocus() }.isSuccess } ?: false
}
