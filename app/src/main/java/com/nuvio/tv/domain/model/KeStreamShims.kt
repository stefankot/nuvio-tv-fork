package com.nuvio.tv.domain.model

import androidx.compose.runtime.Immutable

/*
 * Nakładka forka: to, co KE (0a87a08) dodało do modelu źródła w swoim `domain/model/Stream.kt`, a czego baza nie ma.
 * Model bazy zostaje bez zmian. Właściciel nie używa AIOStreams (18.11), więc dane AIOStreams są zawsze puste i ranking
 * KE czyta nazwy oraz opisy źródeł z dodatków. Przy aktualizacji KE: porównaj klasy niżej z KE `Stream.kt`.
 */

/** KE: AIOStreams' `streamData` (rozmiar, nazwa pliku, cache debrid, rozpoznane cechy wydania). Tu zawsze null. */
@Immutable
data class AioStreamData(
    val size: Long?,
    val filename: String?,
    val cached: Boolean?,
    val parsedFile: AioParsedFile?,
    val upstream: String? = null,
    val type: String? = null,
    val serviceId: String? = null,
    val durationMs: Long? = null
)

/** KE: AIOStreams' labels as sent ("2160p", "BluRay REMUX", "DV", "DD+", "English", "IMAX"). */
@Immutable
data class AioParsedFile(
    val resolution: String?,
    val quality: String?,
    val encode: String?,
    val releaseGroup: String?,
    val visualTags: List<String>,
    val audioTags: List<String>,
    val audioChannels: List<String>,
    val languages: List<String>,
    val editions: List<String>
)

/** KE `Stream.streamData`: baza nie czyta tych danych z odpowiedzi dodatku. */
val Stream.streamData: AioStreamData? get() = null

/** KE `Stream.badgeMergeKey()` (kopia 1:1): tożsamość źródła niezależna od plakietek. */
fun Stream.badgeMergeKey(): String {
    infoHash?.lowercase()?.let { hash -> return "$addonName|$hash:${fileIdx ?: ""}" }
    val playableUrl = url ?: clientResolve?.let { resolve ->
        resolve.stream?.raw?.filename ?: resolve.infoHash
    }
    if (playableUrl != null) return "$addonName|$playableUrl"
    return "$addonName|${name}:${title}:${description?.hashCode() ?: 0}"
}

/** KE `AddonStreams.missingUpstreams` (dodatki-bramy jak AIOStreams); baza ich nie zgłasza. */
val AddonStreams.missingUpstreams: List<String> get() = emptyList()

/** KE: every group's missing upstreams, once each, in group order. */
fun List<AddonStreams>.missingUpstreams(): List<String> = flatMap { it.missingUpstreams }.distinct()
