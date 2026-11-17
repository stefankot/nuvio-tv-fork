package com.nuvio.tv.fork.tv.ekrany

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * „Odtwórz ten kanał” przekazane z innego ekranu (kafel na Home) do Live TV. Prośba jest ważna [ValidMs],
 * więc taka, której Live TV nie zdążył przyjąć, nie uruchomi później odtwarzania „z zaskoczenia”.
 */
object PendingPlay {
    const val ValidMs = 10_000L

    data class Request(val streamUrl: String, val atMs: Long)

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request.asStateFlow()

    fun ask(streamUrl: String, nowMs: Long = System.currentTimeMillis()) { _request.value = Request(streamUrl, nowMs) }

    fun <T> take(byUrl: Map<String, T>, nowMs: Long = System.currentTimeMillis()): T? {
        val r = _request.value ?: return null
        if (nowMs - r.atMs > ValidMs) { _request.compareAndSet(r, null); return null }
        val hit = byUrl[r.streamUrl] ?: return null
        return if (_request.compareAndSet(r, null)) hit else null
    }

    internal fun clear() { _request.value = null }
}
