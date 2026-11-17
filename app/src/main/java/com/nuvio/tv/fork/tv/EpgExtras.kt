package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.most.ProgrammeExtra
import java.util.concurrent.ConcurrentHashMap

/**
 * Side table filled by the EPG reader (one hook in LiveTvEpg.kt) for the programmes it reads with details, i.e. those near "now".
 * Not saved with the guide cache: after a cache-only start the extras are absent until the next guide read, and the UI simply hides them.
 */
object EpgExtras {
    private const val MAX = 6000
    private val map = ConcurrentHashMap<String, ProgrammeExtra>()

    private fun key(title: String, startMs: Long) = startMs.toString() + "|" + title.trim().lowercase()

    fun put(title: String, startMs: Long, extra: ProgrammeExtra) {
        if (extra.isEmpty) return
        if (map.size >= MAX) map.clear() // a new guide read refills the near-now window
        map[key(title, startMs)] = extra
    }

    fun get(title: String, startMs: Long): ProgrammeExtra? = map[key(title, startMs)]
}
