package com.nuvio.tv.fork.sources

import com.nuvio.tv.domain.model.Stream

/**
 * Kopia 1:1 z Reshaped 0ca5a08c2 (`core/player/recovery/AutoSourceRecoveryPlan.kt`, funkcja `recoverySourceKey`).
 * Aktualizacja bazy 26.11 usunęła „automatyczne próbowanie innego źródła” z Reshaped (oficjalne Nuvio ma własne);
 * klucz źródła zostaje, bo „Spróbuj inne źródło” i lista złych źródeł forka rozpoznają nim ten sam plik.
 */
fun recoverySourceKey(stream: Stream): String {
    val hash = stream.getEffectiveInfoHash()
    return if (hash != null) {
        val file = stream.getEffectiveFileIdx()?.toString()
            ?: stream.clientResolve?.filename ?: stream.behaviorHints?.filename.orEmpty()
        if (stream.isTorrent()) "torrent:${hash.lowercase()}:$file"
        else "file:${stream.addonName}:${hash.lowercase()}:$file"
    } else {
        "url:${stream.getStreamUrl() ?: stream.youTubeIdToResolve() ?: stream.toString()}"
    }
}
