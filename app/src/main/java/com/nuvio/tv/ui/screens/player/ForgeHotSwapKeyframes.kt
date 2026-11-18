package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.core.autoquality.port.Keyframes
import com.nuvio.tv.ui.screens.player.seekpreview.engine.AviIndexReader
import com.nuvio.tv.ui.screens.player.seekpreview.engine.HttpRangeReader
import com.nuvio.tv.ui.screens.player.seekpreview.engine.MkvIndexReader
import com.nuvio.tv.ui.screens.player.seekpreview.engine.Mp4IndexReader

/**
 * forge (owner 13.11: finish 4A): Echo's FileKeyframes for the hot swap, read with Kicia's own seek-preview index
 * readers (MKV Cues, MP4 sync samples, AVI idx1) instead of a second copy. The swap then lands on the new file's
 * keyframe, so its first picture needs nothing decoded before it. Null for HLS/DASH or anything unreadable; the
 * caller (AutoQualityHotSwap.hotSwapPoint) runs it off the main thread under its own time limit.
 */
internal object ForgeHotSwapKeyframes : Keyframes {
    override fun read(url: String, headers: Map<String, String>, userAgent: String?, durationMs: Long): Keyframes.Summary? {
        val withAgent = if (userAgent != null && headers.keys.none { it.equals("User-Agent", ignoreCase = true) }) {
            headers + ("User-Agent" to userAgent)
        } else {
            headers
        }
        val reader = HttpRangeReader(url, withAgent, PlayerPlaybackNetworking.playbackHttpClient)
        return try {
            val head = reader.read(0, MkvIndexReader.HEAD_BYTES)
            val index = when {
                MkvIndexReader.looksLikeMatroska(head) -> MkvIndexReader.read(reader, head)
                AviIndexReader.looksLikeAvi(head) -> AviIndexReader.read(reader, head)
                Mp4IndexReader.looksLikeMp4(head) -> Mp4IndexReader.read(reader, head)
                else -> return null
            }
            index.keyframes.ptsUs.map { it / 1000L }.takeIf { it.size >= 2 }?.let { Keyframes.Summary(it) }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { reader.close() }
        }
    }
}
