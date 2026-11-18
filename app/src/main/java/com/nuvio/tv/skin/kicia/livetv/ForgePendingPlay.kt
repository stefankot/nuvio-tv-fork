package com.nuvio.tv.skin.kicia.livetv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "Play this channel" handed from another screen (a channel tile on Home) to Live TV: Home asks for the
 * channel and opens the Live TV tab, and Live TV's browse screen starts it as soon as its channel list is
 * there. A request is only good for [ValidMs], so one that Live TV could not take (e.g. it opened on the
 * guide, or the channel is gone from the list) never starts playback later by surprise.
 */
object ForgePendingPlay {
    const val ValidMs = 10_000L

    data class Request(val streamUrl: String, val atMs: Long)

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request.asStateFlow()

    fun ask(streamUrl: String, nowMs: Long = System.currentTimeMillis()) {
        _request.value = Request(streamUrl, nowMs)
    }

    /** The channel to start now, if a fresh request matches one in [byUrl]; the request is used up either way once stale. */
    fun <T> take(byUrl: Map<String, T>, nowMs: Long = System.currentTimeMillis()): T? {
        val r = _request.value ?: return null
        if (nowMs - r.atMs > ValidMs) {
            _request.compareAndSet(r, null)
            return null
        }
        val hit = byUrl[r.streamUrl] ?: return null
        return if (_request.compareAndSet(r, null)) hit else null
    }

    internal fun clear() {
        _request.value = null
    }
}
