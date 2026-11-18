package com.nuvio.tv.fork.sources

import com.nuvio.tv.domain.model.AddonStreams
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.core.streams.smart.HdrKind
import com.nuvio.tv.core.streams.smart.ReleaseInfo
import com.nuvio.tv.core.streams.smart.ReleaseParser
import com.nuvio.tv.core.streams.smart.SourceKind

/** What the detail screen says about the best source found before Play is pressed. */
data class ReadyInfo(
    /** e.g. "4K", "1080p"; null when no addon says. */
    val quality: String?,
    /** Badge names the addon gave (HDR, Atmos, REMUX...), at most [ReadyPick.MAX_TAGS]. */
    val tags: List<String>,
    val sizeBytes: Long?,
    /** True when the stream plays right away: a direct link, or a debrid file that is cached. */
    val ready: Boolean,
    /** Kicia: above zero = Play opens the source list (the viewer chooses), so no single file can be named. */
    val choices: Int = 0,
    /** Kicia: what the file's name says about the wanted audio; null when it says nothing useful. */
    val audio: ReadyAudio? = null,
)

/** Kicia 34c80c6: AI_SUBTITLES = no audio in the viewer's language and no subtitles found for this file. */
enum class ReadyAudioKind { LEKTOR, DUBBING, LANGUAGE, SUBTITLES, AI_SUBTITLES }

/** [language] is the viewer's audio language code in capitals ("PL"). */
data class ReadyAudio(val kind: ReadyAudioKind, val language: String)

object ReadyPick {
    const val MAX_TAGS = 4

    /** The stream Play would most likely take: playable now first, then quality, then size. */
    fun best(groups: List<AddonStreams>): Stream? {
        val all = groups.flatMap { it.streams }.filter { it.ytId == null && !it.isExternalOnly() }
        if (all.isEmpty()) return null
        return all.maxWithOrNull(
            compareBy<Stream>({ if (it.playsNow()) 1 else 0 }, { it.qualityValue }, { it.behaviorHints?.videoSize ?: 0L }),
        )
    }

    fun info(groups: List<AddonStreams>): ReadyInfo? {
        val stream = best(groups) ?: return null
        return ReadyInfo(
            quality = stream.quality?.takeIf { it.isNotBlank() },
            tags = stream.badges.map { it.name.trim() }.filter { it.isNotEmpty() }.distinct().take(MAX_TAGS),
            sizeBytes = stream.behaviorHints?.videoSize?.takeIf { it > 0 } ?: stream.debridCacheStatus?.cachedSize?.takeIf { it > 0 },
            ready = stream.playsNow(),
        )
    }

    /** Kicia: all streams in the order Play chooses from: the selector's own addon ordering. */
    fun playOrder(groups: List<AddonStreams>, installedOrder: List<String>): List<Stream> =
        com.nuvio.tv.core.player.StreamAutoPlaySelector.orderAddonStreams(groups, installedOrder).flatMap { it.streams }

    /**
     * Kicia: what is known about one exact stream (the file Play will start). Addons that send no quality or badges are
     * read from the release name. Audio language: the one the viewer wants (fork: answer to the Polish audio question).
     */
    fun info(stream: Stream, audioLanguage: String? = com.nuvio.tv.skin.kicia.KiciaSkin.audioLanguage): ReadyInfo {
        val text = listOfNotNull(stream.name, stream.title, stream.description, stream.behaviorHints?.filename).joinToString(" ")
        val size = stream.behaviorHints?.videoSize?.takeIf { it > 0 } ?: stream.debridCacheStatus?.cachedSize?.takeIf { it > 0 }
        val parsed = ReleaseParser.parse(text, size)
        val badges = stream.badges.map { it.name.trim() }.filter { it.isNotEmpty() }.distinct()
        return ReadyInfo(
            quality = stream.quality?.takeIf { it.isNotBlank() } ?: heightLabel(parsed.height),
            tags = badges.ifEmpty { parsedTags(parsed) }.take(MAX_TAGS),
            sizeBytes = size ?: parsed.textSizeBytes,
            ready = stream.playsNow(),
            audio = audio(parsed, audioLanguage),
        )
    }

    /** Fork: audio tier from the release name (Kicia used its own ranker, not carried over). */
    fun audio(info: ReleaseInfo, language: String?): ReadyAudio? {
        val lang = language?.trim()?.lowercase(java.util.Locale.ROOT)?.takeIf { it.isNotEmpty() } ?: return null
        val code = lang.uppercase(java.util.Locale.ROOT)
        val kind = when {
            lang == "pl" && info.lektor -> ReadyAudioKind.LEKTOR
            lang == "pl" && info.dubbing -> ReadyAudioKind.DUBBING
            lang in info.audioLanguages -> ReadyAudioKind.LANGUAGE
            lang in info.subtitleLanguages -> ReadyAudioKind.SUBTITLES
            else -> return null
        }
        return ReadyAudio(kind, code)
    }

    /** Kicia: Play will show the list: how many sources can be played, nothing about any one of them. */
    fun choice(streams: List<Stream>): ReadyInfo? {
        val playable = streams.count { it.ytId == null && !it.isExternalOnly() }
        return if (playable == 0) null else ReadyInfo(quality = null, tags = emptyList(), sizeBytes = null, ready = true, choices = playable)
    }

    fun heightLabel(height: Int?): String? = when {
        height == null -> null
        height >= 2160 -> "4K"
        else -> "${height}p"
    }

    private fun parsedTags(info: ReleaseInfo): List<String> = listOfNotNull(
        when (info.hdr) {
            HdrKind.DOLBY_VISION -> "Dolby Vision"
            HdrKind.HDR10_PLUS -> "HDR10+"
            HdrKind.HDR -> "HDR"
            null -> null
        },
        "REMUX".takeIf { info.source == SourceKind.REMUX },
    )

    private fun Stream.playsNow(): Boolean = (getStreamUrl() != null) || isDirectDebrid()
    private fun Stream.isExternalOnly(): Boolean = isExternal()

    /** "70,8 GB" / "850 MB" with the device's decimal comma left to the caller's locale. */
    fun sizeText(bytes: Long, locale: java.util.Locale = java.util.Locale.getDefault()): String {
        val gb = bytes / 1_073_741_824.0
        return if (gb >= 1.0) String.format(locale, "%.1f GB", gb) else String.format(locale, "%.0f MB", bytes / 1_048_576.0)
    }
}
