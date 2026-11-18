package com.nuvio.tv.fork.tv

import kotlinx.serialization.Serializable

/**
 * Makieta 7.7 G31 / H16 / A10: a programme played from the archive and left part way. [positionMs] is the moment of the
 * airing reached (epoch ms, between [startMs] and [stopMs]); [updatedMs] orders „Kontynuuj oglądanie”.
 */
@Serializable
data class TvResume(
    val channelUrl: String,
    val channelName: String,
    val guideKey: String,
    val title: String,
    val episode: String? = null,
    val image: String? = null,
    val startMs: Long,
    val stopMs: Long,
    val positionMs: Long,
    val updatedMs: Long,
) {
    val key: String get() = "$channelUrl|$startMs"
    val fraction: Float get() = ((positionMs - startMs).toFloat() / (stopMs - startMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
    /** Makieta A10: ≥ 90% = watched. */
    val watched: Boolean get() = fraction >= .9f
    /** Left part way: watched at least 30 s and not to the end (≥ 90%). */
    val started: Boolean get() = positionMs - startMs >= 30_000L && !watched
    val leftMinutes: Int get() = ((stopMs - positionMs) / 60_000L).toInt().coerceAtLeast(1)
}
