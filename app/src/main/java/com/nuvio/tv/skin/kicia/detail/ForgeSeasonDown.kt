package com.nuvio.tv.skin.kicia.detail

import android.view.KeyEvent
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.focus.FocusRequester

/**
 * ▼ from the selected season tab (JMGO report 13.11: "can't get into an episode of season 6").
 * Nuvio points ▼ at the remembered / next episode, but the row keeps its scroll from the previous
 * season, so that card is often not composed and focus goes nowhere. Here ▼ first tries Nuvio's
 * target and, when it is not on screen, falls back to the first visible episode of the row.
 */
object ForgeSeasonDown {
    private var rowFallback: (() -> Boolean)? = null

    /** Called by the episode row (forge hook) while it is on screen. */
    fun register(state: LazyListState, idAt: (Int) -> String?, requesters: Map<String, FocusRequester>): () -> Unit {
        val fallback: () -> Boolean = fallback@{
            val info = state.layoutInfo
            val visible = info.visibleItemsInfo
            val firstFull = visible.firstOrNull { it.offset >= info.viewportStartOffset } ?: visible.firstOrNull()
            val id = firstFull?.index?.let(idAt) ?: return@fallback false
            runCatching { requesters[id]?.requestFocus() == true }.getOrDefault(false)
        }
        rowFallback = fallback
        return { if (rowFallback === fallback) rowFallback = null }
    }

    /** True when the key was handled (focus moved into the episode row). */
    fun onSeasonTabKey(event: KeyEvent, isSelected: Boolean, down: FocusRequester?): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_DPAD_DOWN || event.action != KeyEvent.ACTION_DOWN) return false
        if (!isSelected || down == null) return false
        if (runCatching { down.requestFocus() }.getOrDefault(false)) return true
        return rowFallback?.invoke() == true
    }
}
