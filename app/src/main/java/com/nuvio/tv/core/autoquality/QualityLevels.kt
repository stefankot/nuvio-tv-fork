package com.nuvio.tv.core.autoquality

import com.nuvio.tv.core.autoquality.echo.AutoQualityPolicy
import com.nuvio.tv.core.autoquality.echo.StreamQuality
import com.nuvio.tv.core.streams.smart.ReleaseInfo

/**
 * forge M4a: bridge from our [ReleaseInfo] to Echo's [StreamQuality]. The nearest level at or below the height the
 * text names (576p → 480p); no height → null (unknown, never guessed).
 */
fun levelOf(info: ReleaseInfo): StreamQuality? = levelOfHeight(info.height)

fun levelOfHeight(height: Int?): StreamQuality? {
    if (height == null || height <= 0) return null
    return StreamQuality.entries.firstOrNull { it.height <= height } ?: StreamQuality.P144
}

/** Size ÷ running time, in bits/s; null when either is missing or not positive. */
fun fileBitrateBps(sizeBytes: Long?, durationMs: Long?): Long? {
    if (sizeBytes == null || durationMs == null || sizeBytes <= 0L || durationMs <= 0L) return null
    return sizeBytes * 8L * 1_000L / durationMs
}

/** The file's own bitrate when size and time are known, else what the level typically needs (Echo's table). */
fun bitrateBps(level: StreamQuality, sizeBytes: Long?, durationMs: Long?): Long =
    fileBitrateBps(sizeBytes, durationMs) ?: AutoQualityPolicy.typicalBitrateBps(level)
