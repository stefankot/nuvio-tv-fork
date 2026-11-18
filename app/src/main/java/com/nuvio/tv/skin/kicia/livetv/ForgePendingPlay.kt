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

    /** [programme]: makieta 7.7 G31 — an archive airing to resume (its start = the place it was left), else the channel live. */
    data class Request(val streamUrl: String, val atMs: Long, val programme: com.nuvio.tv.reshaped.livetv.LiveTvProgramme? = null, val guideKey: String? = null)
    /** The programme of the request [take] used up last (null = live). */
    var lastProgramme: com.nuvio.tv.reshaped.livetv.LiveTvProgramme? = null
        private set

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request.asStateFlow()

    fun ask(streamUrl: String, nowMs: Long = System.currentTimeMillis(), programme: com.nuvio.tv.reshaped.livetv.LiveTvProgramme? = null, guideKey: String? = null) {
        _request.value = Request(streamUrl, nowMs, programme, guideKey)
    }

    /** The channel to start now, if a fresh request matches one in [byUrl]; the request is used up either way once stale. */
    fun <T> take(byUrl: Map<String, T>, nowMs: Long = System.currentTimeMillis(), byGuide: Map<String, T> = emptyMap()): T? {
        val r = _request.value ?: return null
        android.util.Log.i("ForgePendingPlay", "take ${r.programme?.title ?: "live"} age=${nowMs - r.atMs}ms channels=${byUrl.size} hit=${byUrl.containsKey(r.streamUrl)} guide=${r.guideKey?.let { byGuide.containsKey(it) }}") // test 15.11
        // test 15.11 (JMGO): after a cold start Live TV reads its channels and guide for longer than 10 s — a resume waits 2 min
        if (nowMs - r.atMs > (if (r.programme != null) 120_000L else ValidMs)) {
            _request.compareAndSet(r, null)
            return null
        }
        // test 15.11 (JMGO, log „hit=false”): a resume's saved address can differ from the list's — its guide key finds the channel
        val hit = byUrl[r.streamUrl] ?: r.guideKey?.let { byGuide[it] } ?: return null
        lastProgramme = r.programme
        return if (_request.compareAndSet(r, null)) hit else null
    }

    internal fun clear() {
        _request.value = null
    }
}
