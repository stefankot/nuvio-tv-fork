package com.nuvio.tv.skin.kicia.detail

import com.nuvio.tv.core.streams.smart.HdrKind
import com.nuvio.tv.core.streams.smart.ReleaseInfo
import com.nuvio.tv.core.streams.smart.SourceKind
import com.nuvio.tv.core.streams.smart.VideoCodec

/**
 * forge (owner 13.11, N15; makieta 7.5 05): every file with its badges — resolution, bitrate, language (only the
 * viewer's audio and subtitle languages), codec, HDR. The narrow source list (before playing) fits three badges
 * (resolution, language, HDR); bitrate and codec go to its second line.
 */
data class FileBadges(
    val type: String?,
    val resolution: String?,
    val bitrate: String?,
    val language: String?,
    val codec: String?,
    val hdr: String?,
) {
    val all: List<String> get() = listOfNotNull(resolution, bitrate, language, codec, hdr)
    val narrow: List<String> get() = listOfNotNull(resolution, language, hdr)
}

object ForgeFileBadges {
    /** The title the source list shows (set by the stream screen), for the bitrate from the title's length. */
    @Volatile var title: Triple<String?, Int?, Int?>? = null

    fun of(info: ReleaseInfo, sizeBytes: Long?, durationSec: Long?, audioLanguage: String?, subtitleLanguage: String?): FileBadges =
        FileBadges(
            type = when (info.source) {
                SourceKind.REMUX -> "REMUX"
                SourceKind.BLURAY -> "BluRay"
                SourceKind.WEBDL -> "WEB-DL"
                SourceKind.WEBRIP -> "WEBRip"
                SourceKind.HDTV -> "HDTV"
                SourceKind.DVD -> "DVD"
                null -> null
            },
            resolution = info.height?.let { if (it >= 2160) "2160p" else "${it}p" },
            bitrate = bitrate(sizeBytes, durationSec),
            language = language(info, audioLanguage, subtitleLanguage),
            codec = when (info.codec) {
                VideoCodec.AVC -> "H.264"
                VideoCodec.HEVC -> "HEVC"
                VideoCodec.AV1 -> "AV1"
                null -> null
            },
            hdr = when (info.hdr) {
                HdrKind.DOLBY_VISION -> "DV"
                HdrKind.HDR10_PLUS -> "HDR10+"
                HdrKind.HDR -> "HDR"
                null -> null
            },
        )

    /** "18 Mb/s" from the size over the title's length; nothing when either is unknown or the result is absurd. */
    fun bitrate(sizeBytes: Long?, durationSec: Long?): String? {
        if (sizeBytes == null || durationSec == null || sizeBytes <= 0 || durationSec < 60) return null
        val mbps = sizeBytes * 8.0 / durationSec / 1_000_000
        if (mbps < 0.3 || mbps > 200) return null
        return "${if (mbps < 10) "%.1f".format(java.util.Locale("pl"), mbps).removeSuffix(",0") else mbps.toInt().toString()} Mb/s"
    }

    /** Only the viewer's languages: "lektor PL", "dubbing PL", "PL" (audio), else "napisy PL"; nothing for others. */
    fun language(info: ReleaseInfo, audioLanguage: String?, subtitleLanguage: String?): String? {
        val audio = audioLanguage?.trim()?.lowercase()?.take(2)?.takeIf { it.isNotEmpty() }
        val subs = subtitleLanguage?.trim()?.lowercase()?.take(2)?.takeIf { it.isNotEmpty() && it != "no" }
        if (audio != null) {
            val code = audio.uppercase()
            if (audio == "pl" && info.lektor) return "lektor $code"
            if (audio == "pl" && info.dubbing) return "dubbing $code"
            if (info.audioLanguages.any { it.lowercase().startsWith(audio) }) return code
        }
        if (subs != null && info.subtitleLanguages.any { it.lowercase().startsWith(subs) }) return "napisy ${subs.uppercase()}"
        return null
    }
}
