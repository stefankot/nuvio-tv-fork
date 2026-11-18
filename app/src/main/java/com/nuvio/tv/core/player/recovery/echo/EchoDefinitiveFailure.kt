// Pochodzenie: Laking22/Nuvio-Echo @ e952419, app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerQualityFallback.kt
// Kopia wprost samej funkcji isDefinitiveSourceFailure (M3c, K4). Zmiany oznaczone „// KICIA:”. Rejestr: ECHO-PORT.md
// KICIA: pakiet echo (osobny katalog na kod Echo)
package com.nuvio.tv.core.player.recovery.echo

import androidx.media3.common.PlaybackException

/**
 * A failure no retry of the same link can fix: the server refused or doesn't have the file.
 * These skip the retry/engine-switch ladder and go straight to the next source.
 */
// KICIA: kod HTTP podaje wołający (Kicia ma findInvalidResponseCodeException w pakiecie odtwarzacza; echo go nie importuje)
internal fun PlaybackException.isDefinitiveSourceFailure(responseCode: Int?): Boolean = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
    PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> true
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
        val code = responseCode // KICIA: zamiast findInvalidResponseCodeException()?.responseCode
        isDefinitiveHttpStatus(code) // KICIA: reguła kodów wydzielona dla mpv i testu JVM
    }
    else -> false
}

// KICIA: reguła kodów z gałęzi ERROR_CODE_IO_BAD_HTTP_STATUS, osobno — mpv podaje sam kod HTTP
internal fun isDefinitiveHttpStatus(code: Int?): Boolean =
    code != null && code in 400..499 && code !in setOf(408, 416, 429)
