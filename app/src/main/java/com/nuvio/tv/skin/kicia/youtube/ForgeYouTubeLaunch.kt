package com.nuvio.tv.skin.kicia.youtube

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.nuvio.tv.youtube.YtItem

/**
 * "Play this YouTube video" handed from Home's "YouTube dla Ciebie" shelf to the YouTube tab, which has the
 * navigation to the player: Home asks and opens the tab; the tab plays it and clears the request.
 */
object ForgeYouTubeLaunch {
    private val _pending = MutableStateFlow<YtItem?>(null)
    val pending: StateFlow<YtItem?> = _pending.asStateFlow()

    fun ask(item: YtItem) { _pending.value = item }

    /** The video to play now, once. */
    fun take(): YtItem? = _pending.value.also { _pending.value = null }
}
