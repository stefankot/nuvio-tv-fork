package com.nuvio.tv.livetv.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// KE adapter: only the kind enum from KE's LiveSource.kt (SourceFetcher sniffing).
@Serializable
enum class SourceKind {
    @SerialName("m3u") M3U,
    @SerialName("xmltv") XMLTV,
    @SerialName("stremio") STREMIO,
    @SerialName("pack") PACK
}
