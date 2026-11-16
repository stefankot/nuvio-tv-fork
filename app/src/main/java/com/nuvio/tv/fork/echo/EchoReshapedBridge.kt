package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.domain.model.Stream

/**
 * Echo -> Reshaped bridges: functions with Echo's signature that call the fork's (Reshaped's) version.
 */

/** Echo: switchToSourceStream(stream, startIntro, loadSavedProgress). Reshaped: switchToSourceStream(stream, resumeSavedProgress). */
internal fun PlayerRuntimeController.switchToSourceStream(
    stream: Stream,
    startIntro: Boolean,
    loadSavedProgress: Boolean
) = switchToSourceStream(stream, resumeSavedProgress = loadSavedProgress)

/** Echo: how a picked source plays (UP/echo PlayerRuntimeControllerStreams.kt 608-645); Reshaped has no such type. */
internal class StreamPlayback(
    val url: String,
    val headers: Map<String, String>,
    val filename: String?,
    val responseHeaders: Map<String, String>,
    val mimeType: String?,
    val cacheKey: String?
)

internal fun PlayerRuntimeController.streamPlayback(
    stream: Stream,
    url: String,
    headers: Map<String, String>
): StreamPlayback {
    val playbackRequest = PlayerMediaSourceFactory.normalizePlaybackRequest(url, headers)
    val filename = stream.behaviorHints?.filename ?: navigationArgs.filename
    val responseHeaders = stream.behaviorHints?.proxyHeaders?.response.orEmpty()
    val fileIdx = stream.getEffectiveFileIdx()
    return StreamPlayback(
        url = playbackRequest.url,
        headers = playbackRequest.headers,
        filename = filename,
        responseHeaders = responseHeaders,
        mimeType = PlayerMediaSourceFactory.inferMimeType(
            url = playbackRequest.url,
            filename = filename,
            responseHeaders = responseHeaders
        ),
        cacheKey = stream.getEffectiveInfoHash()?.lowercase()?.takeIf { fileIdx != null }?.let { hash -> "$hash:$fileIdx" }
    )
}
