// Kopia z Kicii 34c80c6 (gałąź kicia-zrodla-wip, core/player/recovery), dopasowana do forka: tylko pakiet
package com.nuvio.tv.fork.sources.kicia

import androidx.media3.common.PlaybackException

/**
 * forge M3: true when a playback error says "this device cannot play this FILE", so the same file offered by
 * another addon or host would fail the same way and must not be the next attempt.
 *
 * Decoder errors were already treated this way. Container errors were not: a broken AVI offered by three
 * addons used up all three startup attempts (13.11.2026, ERROR_CODE_PARSING_CONTAINER_MALFORMED).
 * Manifest errors (HLS/DASH) are left out on purpose: a manifest belongs to the host, not to the file.
 */
fun isFileInherentPlaybackError(errorCode: Int, unrecognizedFormat: Boolean = false): Boolean =
    unrecognizedFormat || errorCode in FILE_INHERENT_ERROR_CODES

private val FILE_INHERENT_ERROR_CODES = setOf(
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
)
