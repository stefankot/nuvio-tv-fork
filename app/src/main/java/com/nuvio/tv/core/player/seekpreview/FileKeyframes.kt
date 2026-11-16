package com.nuvio.tv.core.player.seekpreview

import okhttp3.OkHttpClient

/**
 * Echo stub (Echo reads the file's index with its seek-thumbnail code, which the fork does not have):
 * always "unreadable" (null), the answer Echo itself gives for HLS/DASH and unreadable files.
 */
internal object FileKeyframes {
    class Summary(
        val keyframesUs: LongArray?,
        val hasAudio: Boolean?
    )

    fun read(
        client: OkHttpClient,
        url: String,
        headers: Map<String, String>,
        userAgent: String,
        durationMs: Long
    ): Summary? = null
}
