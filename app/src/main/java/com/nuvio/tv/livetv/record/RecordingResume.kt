package com.nuvio.tv.livetv.record

import android.content.Context

// Nuvio One adapter: resume points for YouTube videos (own prefs, never Continue Watching).
object RecordingResume {
    private const val PREFS = "livetv_recording_resume"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun save(context: Context, contentId: String?, positionMs: Long, durationMs: Long) {
        val id = contentId ?: return
        runCatching { prefs(context).edit().putString(id, "$positionMs,$durationMs").apply() }
    }
    fun load(context: Context, contentId: String?): Pair<Long, Long>? {
        val v = runCatching { prefs(context).getString(contentId ?: return null, null) }.getOrNull() ?: return null
        val p = v.split(',')
        return (p.getOrNull(0)?.toLongOrNull() ?: return null) to (p.getOrNull(1)?.toLongOrNull() ?: 0L)
    }
}
