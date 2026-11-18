package com.nuvio.tv.core.streams.smart

/** Where the picture came from. [rank] is a hint for ordering, not a measure of quality. */
enum class SourceKind(val rank: Int) { REMUX(5), BLURAY(4), WEBDL(3), WEBRIP(2), HDTV(1), DVD(1) }

enum class VideoCodec { AVC, HEVC, AV1 }

enum class HdrKind { DOLBY_VISION, HDR10_PLUS, HDR }

/** Why a source must never be picked automatically. */
enum class Junk { CINEMA_RECORDING, SAMPLE, TRAILER, DISC_IMAGE, ARCHIVE, STEREO_3D }

enum class Edition { EXTENDED, DIRECTORS_CUT, UNRATED, UNCUT, THEATRICAL, IMAX }

/**
 * What a release name says about the file. Every field is a claim made by the addon text, not a verified
 * property of the video: null or empty means "the text does not say", never "the file does not have it".
 */
data class ReleaseInfo(
    val height: Int? = null,
    val source: SourceKind? = null,
    val codec: VideoCodec? = null,
    val hdr: HdrKind? = null,
    val junk: Junk? = null,
    /** Polish voice-over ("lektor"). */
    val lektor: Boolean = false,
    val dubbing: Boolean = false,
    /** ISO 639-1 codes the text names as audio (tokens or flag emoji). */
    val audioLanguages: Set<String> = emptySet(),
    /** ISO 639-1 codes the text names only as subtitles ("PL.SUB", "napisy PL"). */
    val subtitleLanguages: Set<String> = emptySet(),
    val multiAudio: Boolean = false,
    /**
     * Size written in the text, read in binary units: "GB" and "GiB" = 2^30, "MB" and "MiB" = 2^20. Addons write
     * "GB" meaning GiB, and the player's panel shows real bytes divided by 2^30, so this keeps both the same.
     */
    val textSizeBytes: Long? = null,
    /** "10bit", "10 bit", "Hi10P", "Hi10" as whole tokens. */
    val tenBit: Boolean = false,
    val edition: Edition? = null,
    /** Burnt-in subtitles: HC (capitals only), HARDCODED, HARDSUB, KORSUB. */
    val hardcodedSubs: Boolean = false,
    /** forge (owner 13.11): an old speech-synthesis voice-over (IVONA, syntezator, TTS) — poor quality, ranked last. */
    val oldTtsVoice: Boolean = false,
    /** Sound recorded in a cinema: MD, LINE (capitals only), MIC.DUBBED, LINE.AUDIO. */
    val cinemaAudio: Boolean = false,
    val upscaled: Boolean = false,
    /** One part of a split film: CD1..CD9, PART1..PART9 after the picture/source tag or last before the extension. */
    val multiPart: Boolean = false,
    /** REPACK, PROPER, RERIP: a corrected release. */
    val repack: Boolean = false,
    /** The name names Dolby Vision together with HDR/HDR10/HDR10+: a layer for screens without DV. */
    val hdrFallback: Boolean = false,
    /** forge M7 (N14): the release group ("…x264-GROUP.mkv", "[GROUP] …"), lower-case; null when the name has none. */
    val group: String? = null,
)

/** Pure text parser. It reads names only: no URL is fetched, opened or stored. */
object ReleaseParser {
    /** Bigger inputs are cut: a description is never worth more than this to parse. */
    const val MAX_TEXT = 8_192
    /** A "sample"/"trailer" word only counts when the file is known to be smaller than this. */
    const val SMALL_FILE_BYTES = 300L * 1024 * 1024

    private val heights = listOf(
        2160 to setOf("2160p", "4k", "uhd"), 1440 to setOf("1440p"), 1080 to setOf("1080p", "1080i", "fhd"),
        720 to setOf("720p"), 576 to setOf("576p"), 480 to setOf("480p"), 360 to setOf("360p"),
    )
    private val sources = listOf(
        SourceKind.REMUX to listOf("remux", "bdremux"),
        SourceKind.BLURAY to listOf("bluray", "blu ray", "bdrip", "brrip", "bd"),
        SourceKind.WEBDL to listOf("web dl", "webdl", "web"),
        SourceKind.WEBRIP to listOf("webrip", "web rip"),
        SourceKind.HDTV to listOf("hdtv", "pdtv", "sdtv"),
        SourceKind.DVD to listOf("dvdrip", "dvd"),
    )
    private val certainCinema = listOf(
        "hdcam", "camrip", "hdcamrip", "telesync", "hdts", "telecine", "hdtc", "workprint", "dvdscr", "screener",
    )
    /** A real title can be one of these words ("Cam" 2018), so they only count without a source tag. */
    private val weakCinema = listOf("cam", "scr")
    private val weakCinemaUpper = Regex("(?<![A-Za-z0-9])(TS|TC)(?![A-Za-z0-9])")
    private val languages = mapOf(
        "pl" to setOf("pl", "pol", "polish", "polski", "plk"),
        "en" to setOf("eng", "english"),
        "de" to setOf("ger", "german", "deutsch"),
        "fr" to setOf("fre", "french", "vff", "vfq"),
        "es" to setOf("spa", "spanish", "castellano"),
        "it" to setOf("ita", "italian"),
        "cs" to setOf("cze", "czech", "cz"),
        "uk" to setOf("ukr", "ukrainian"),
        "ru" to setOf("rus", "russian"),
    )
    private val flagLanguages = mapOf(
        "PL" to "pl", "GB" to "en", "US" to "en", "DE" to "de", "FR" to "fr", "ES" to "es", "IT" to "it",
        "CZ" to "cs", "UA" to "uk", "RU" to "ru",
    )
    private val subWords = setOf("sub", "subs", "subbed", "napisy", "subtitles", "subtitle")
    private val heightTokens = setOf("2160p", "4k", "uhd", "1440p", "1080p", "1080i", "fhd", "720p", "576p", "480p", "360p")
    private val sourceTokens = setOf("remux", "bdremux", "bluray", "bdrip", "brrip", "bd", "webdl", "web", "webrip", "hdtv", "pdtv", "sdtv", "dvdrip", "dvd")
    private val videoExtensions = setOf("mkv", "mp4", "avi", "m4v", "ts", "m2ts", "wmv", "mov", "webm")
    private val discImageEnd = Regex("(?i)\\.(iso|img)(?=\\s|$)")
    private val archiveEnd = Regex("(?i)\\.(rar|zip|7z|exe|r00)(?=\\s|$)")
    /**
     * forge (owner 13.11): old speech synthesis read as a voice-over (IVONA and its voices, Windows voices, "syntezator",
     * TTS). A modern AI voice-over (ElevenLabs and the like) is fine and is NOT listed; "AI" alone says nothing.
     */
    private val oldTtsPhrases = listOf(
        "ivona", "lektor ivona", "ivo lektor", "lektor ivo", "lektor jacek", "ivona jacek", "lektor maja", "lektor jan",
        "lektor ewa", "lektor paulina", "microsoft paulina", "loquendo", "expressivo", "realspeaker",
        "syntezator", "syntezatorem", "syntezator mowy", "lektor syntezator", "lektor tts", "tts", "tts pl", "pl tts",
        "lektor maszynowy", "lektor automatyczny", "robot lektor", "lektor robot",
    )

    private val sizePattern = Regex("(\\d{1,4}(?:[.,]\\d{1,2})?)\\s*(GiB|GB|MiB|MB)(?![A-Za-z])")

    fun parse(rawText: String, knownSizeBytes: Long? = null): ReleaseInfo {
        val raw = if (rawText.length > MAX_TEXT) rawText.substring(0, MAX_TEXT) else rawText
        val tokens = tokenize(raw)
        val joined = tokens.joinToString(" ", prefix = " ", postfix = " ")
        fun has(phrase: String) = joined.contains(" $phrase ")

        val source = sources.firstOrNull { (_, words) -> words.any(::has) }?.first
            // "web" alone means WEB-DL, but "web rip" must stay a rip
            ?.let { if (it == SourceKind.WEBDL && (has("webrip") || has("web rip"))) SourceKind.WEBRIP else it }

        val textSize = sizePattern.find(raw)?.let { sizeBytes(it.groupValues[1], it.groupValues[2]) }
        val size = knownSizeBytes?.takeIf { it > 0 } ?: textSize
        val cased = tokenize(raw, lowercase = false)
        // first picture, source or year tag: what follows it describes the file, not the title
        val firstTag = tokens.indexOfFirst { it in heightTokens || it in sourceTokens || isYear(it) }
        val junk = when {
            certainCinema.any(::has) -> Junk.CINEMA_RECORDING
            source == null && (weakCinema.any(::has) || weakCinemaUpper.containsMatchIn(raw)) -> Junk.CINEMA_RECORDING
            discImageEnd.containsMatchIn(raw) || has("bdmv") || has("video ts") || has("complete bluray") ||
                has("complete uhd bluray") -> Junk.DISC_IMAGE
            archiveEnd.containsMatchIn(raw) -> Junk.ARCHIVE
            has("sbs") || has("hsbs") || has("hou") || has("h ou") || has("half ou") ||
                (firstTag >= 0 && tokens.subList(firstTag + 1, tokens.size).contains("3d")) -> Junk.STEREO_3D
            size != null && size < SMALL_FILE_BYTES && has("sample") -> Junk.SAMPLE
            size != null && size < SMALL_FILE_BYTES && (has("trailer") || has("zwiastun")) -> Junk.TRAILER
            else -> null
        }

        val subs = mutableSetOf<String>()
        val audio = mutableSetOf<String>()
        tokens.forEachIndexed { i, token ->
            val code = languageOf(token)
            when {
                code == null -> Unit
                tokens.getOrNull(i + 1) in subWords || tokens.getOrNull(i - 1) in subWords -> subs += code
                else -> audio += code
            }
        }
        // glued forms: "plsub", "subpl", "plsubbed"
        if (tokens.any { it == "plsub" || it == "subpl" || it == "plsubbed" || it == "plsubs" }) subs += "pl"
        audio += flagCodes(raw)
        val plDub = tokens.any { it == "pldub" || it == "dubpl" || it == "pldubbed" }
        if (plDub) audio += "pl"
        val lektor = has("lektor") || has("lektorpl") || has("lek pl")
        if (lektor) audio += "pl"

        return ReleaseInfo(
            height = heights.firstOrNull { (_, words) -> words.any(::has) }?.first,
            source = source,
            codec = when {
                has("av1") -> VideoCodec.AV1
                has("hevc") || has("x265") || has("h265") || has("h 265") -> VideoCodec.HEVC
                has("avc") || has("x264") || has("h264") || has("h 264") -> VideoCodec.AVC
                else -> null
            },
            hdr = when {
                has("dv") || has("dovi") || has("dolby vision") -> HdrKind.DOLBY_VISION
                has("hdr10plus") || raw.contains("hdr10+", ignoreCase = true) -> HdrKind.HDR10_PLUS
                has("hdr") || has("hdr10") -> HdrKind.HDR
                else -> null
            },
            junk = junk,
            lektor = lektor,
            dubbing = plDub || has("dubbing") || has("dubbed") || has("dub"),
            audioLanguages = audio,
            subtitleLanguages = subs - audio,
            multiAudio = has("multi") || has("dual") || has("dual audio"),
            textSizeBytes = textSize,
            tenBit = has("10bit") || has("10 bit") || has("hi10p") || has("hi10"),
            edition = when {
                has("directors cut") || has("director s cut") || has("directorscut") -> Edition.DIRECTORS_CUT
                has("extended") -> Edition.EXTENDED
                has("unrated") -> Edition.UNRATED
                has("uncut") -> Edition.UNCUT
                has("theatrical") -> Edition.THEATRICAL
                has("imax") -> Edition.IMAX
                else -> null
            },
            hardcodedSubs = "HC" in cased || has("hardcoded") || has("hardsub") || has("hardsubs") || has("korsub"),
            oldTtsVoice = oldTtsPhrases.any(::has),
            cinemaAudio = "LINE" in cased || has("mic dubbed") || has("line audio") ||
                cased.indices.any { i -> cased[i] == "MD" && cased.getOrNull(i - 1)?.lowercase() !in setOf("dts", "hd") },
            upscaled = has("upscaled") || has("upscale"),
            multiPart = multiPart(tokens, firstTag),
            repack = has("repack") || has("proper") || has("rerip"),
            hdrFallback = (has("dv") || has("dovi") || has("dolby vision")) &&
                (has("hdr") || has("hdr10") || has("hdr10plus") || raw.contains("hdr10+", ignoreCase = true)),
            group = releaseGroup(raw),
        )
    }

    /**
     * forge M7 (N14): the group from the file name's last "-GROUP" (before the extension), else a leading "[GROUP]".
     * Only a plain word of 2–20 letters/digits that is not a known tag (a codec, a source, a language) counts.
     */
    internal fun releaseGroup(raw: String): String? {
        val name = raw.lineSequence().map { it.trim() }.lastOrNull { videoExtensionEnd.containsMatchIn(it) } ?: raw.trim()
        val stem = videoExtensionEnd.replace(name, "")
        val dashed = Regex("-([A-Za-z0-9]{2,20})(?:\\[[^\\]]*\\])?\\s*$").find(stem)?.groupValues?.get(1)
        val bracketed = Regex("^\\[([A-Za-z0-9 ._-]{2,20})\\]").find(name)?.groupValues?.get(1)?.trim()
        return (dashed ?: bracketed)?.lowercase()?.takeIf { it !in notGroups && !it.all(Char::isDigit) }
    }
    private val videoExtensionEnd = Regex("(?i)\\.(mkv|mp4|avi|m4v|ts|m2ts|wmv|mov|webm)\\s*$")
    private val notGroups = setOf(
        "dl", "web", "webdl", "webrip", "rip", "x264", "x265", "h264", "h265", "hevc", "avc", "aac", "ac3", "dts", "ddp",
        "dd", "eac3", "atmos", "hdr", "hdr10", "dv", "sdr", "pl", "eng", "en", "sub", "subs", "dub", "lektor", "dubbing",
        "multi", "remux", "bluray", "1080p", "2160p", "720p", "480p", "4k", "uhd", "proper", "repack", "hd", "fhd",
    )

    private fun isYear(token: String) = token.length == 4 && token.all { it in '0'..'9' } && token.toInt() in 1900..2099

    /** CDn / PARTn (glued or apart), only after a picture/source/year tag or as the last token before the extension. */
    private fun multiPart(tokens: List<String>, firstTag: Int): Boolean {
        val extensionAt = tokens.indexOfLast { it in videoExtensions }
        for (i in tokens.indices) {
            val t = tokens[i]
            val end = when {
                (t.startsWith("cd") || t.startsWith("part")) && t.length in 3..5 && t.last() in '1'..'9' &&
                    (t == "cd${t.last()}" || t == "part${t.last()}") -> i
                (t == "cd" || t == "part") && tokens.getOrNull(i + 1)?.let { it.length == 1 && it[0] in '1'..'9' } == true -> i + 1
                else -> continue
            }
            if ((firstTag in 0 until i) || (extensionAt >= 0 && end == extensionAt - 1)) return true
        }
        return false
    }

    /** Lower-case words; everything that is not a letter or digit separates them. */
    internal fun tokenize(text: String, lowercase: Boolean = true): List<String> {
        val out = ArrayList<String>()
        val word = StringBuilder()
        for (ch in text) {
            if (ch.isLetterOrDigit()) word.append(if (lowercase) ch.lowercaseChar() else ch)
            else if (word.isNotEmpty()) { out += word.toString(); word.setLength(0) }
        }
        if (word.isNotEmpty()) out += word.toString()
        return out
    }

    private fun languageOf(token: String): String? = languages.entries.firstOrNull { token in it.value }?.key

    /** Flag emoji are pairs of regional indicators (U+1F1E6..U+1F1FF). */
    private fun flagCodes(text: String): Set<String> {
        val out = mutableSetOf<String>()
        var i = 0
        var pending: Char? = null
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            if (cp in 0x1F1E6..0x1F1FF) {
                val letter = 'A' + (cp - 0x1F1E6)
                val first = pending
                if (first == null) pending = letter
                else { flagLanguages["$first$letter"]?.let(out::add); pending = null }
            } else pending = null
        }
        return out
    }

    internal fun sizeBytes(number: String, unit: String): Long? {
        val value = number.replace(',', '.').toDoubleOrNull() ?: return null
        val factor = when (unit) {
            "GiB", "GB" -> 1_073_741_824.0
            "MiB", "MB" -> 1_048_576.0
            else -> return null
        }
        return Math.round(value * factor).takeIf { it > 0 }
    }
}
