package com.nuvio.tv.core.streams.smart

import com.nuvio.tv.domain.model.Stream

/** The three viewer presets. Stored by name, so do not rename. */
enum class SmartPreset { SAVER, BALANCED, BEST }

/**
 * Everything the automatic choice may depend on. Null always means UNKNOWN: the ranker then stays neutral
 * instead of guessing.
 */
data class SmartPolicy(
    val preset: SmartPreset = SmartPreset.BALANCED,
    /** ISO 639-1 code of the audio the viewer wants; null = no preference. */
    val audioLanguage: String? = null,
    /** Tallest picture the screen shows; null = unknown. */
    val displayHeight: Int? = null,
    /** Video decoders this device reports; null = not probed. A codec missing from a probed map has no decoder. */
    val decoders: Map<VideoCodec, DecoderCap>? = null,
    /** Measured link (90th percentile of downloaded bytes); null = unknown. */
    val linkBps: Long? = null,
    /** HDR kinds the screen shows; null = unknown, empty = an SDR screen. */
    val hdr: Set<HdrKind>? = null,
    val lowRam: Boolean = false,
    /** forge M3b (N9): the network says it is metered (a phone hotspot): the session ranks as SAVER. */
    val metered: Boolean = false,
    /** forge hook M4: the session ceiling (height of the level that worked after a step down for the link); null = none. */
    val sessionCeilingHeight: Int? = null,
    /** forge (check 13.11): the viewer agreed to P2P streaming; without it P2P sources go last (no consent dialog). */
    val p2pAllowed: Boolean = true,
) {
    /**
     * Tallest picture worth playing with this preset. Taller sources are kept, but tried last. The screen mode
     * describes the interface layer, not the video path, so BEST and BALANCED follow the hardware decoders.
     */
    val heightCap: Int
        get() {
            val cap = when (preset) {
                SmartPreset.SAVER -> minOf(SAVER_MAX_HEIGHT, displayHeight ?: SAVER_MAX_HEIGHT)
                SmartPreset.BALANCED, SmartPreset.BEST ->
                    decoders?.values?.filter { it.hardware }?.mapNotNull { it.maxHeight }?.maxOrNull() ?: UNKNOWN_DISPLAY_HEIGHT
            }
            val device = if (lowRam) minOf(cap, LOW_RAM_MAX_HEIGHT) else cap
            // forge hook M4: the session ceiling cuts the cap for the next episode
            return sessionCeilingHeight?.let { minOf(device, it) } ?: device
        }

    /** Part of the measured link one source may use on average. */
    val linkShare: Double
        get() = when (preset) {
            SmartPreset.SAVER -> 0.25
            SmartPreset.BALANCED -> 0.40
            SmartPreset.BEST -> 0.70
        }

    companion object {
        const val SAVER_MAX_HEIGHT = 1080
        const val UNKNOWN_DISPLAY_HEIGHT = 2160
        const val LOW_RAM_MAX_HEIGHT = 1080

        /** size × 8 / duration; anything missing or not positive is UNKNOWN (null). */
        fun averageBitrateBps(sizeBytes: Long?, durationSec: Long?): Long? {
            if (sizeBytes == null || durationSec == null || sizeBytes <= 0 || durationSec <= 0) return null
            return sizeBytes * 8 / durationSec
        }
    }
}

/** What the screen knows about the title. null = not known; never guessed. */
data class SmartExpected(
    val season: Int? = null,
    val episode: Int? = null,
    val year: Int? = null,
    val durationSec: Long? = null,
    /** forge M3b (N8): the title's id, so the ranking can find the file it last played. */
    val contentId: String? = null,
    /** forge M7 (N14): the family of the episode before, when it played without trouble. */
    val family: SmartFamily? = null,
)

enum class IdentityVerdict { MATCH, UNKNOWN, WRONG_YEAR, WRONG_EPISODE }

/** What the device says about its best decoder for one codec. Null fields are UNKNOWN. */
data class DecoderCap(val hardware: Boolean, val maxHeight: Int? = null)

/**
 * The policy of the active profile on this device. Player settings write the viewer's part, the device probe
 * writes the hardware part, the autoplay selector reads it.
 */
object SmartPolicyHolder {
    @Volatile private var stored: SmartPolicy = SmartPolicy()

    /**
     * forge N13: the quality level picked in the player for this session; the profile's preset stays.
     * Cleared when the player closes.
     */
    @Volatile var playPreset: SmartPreset? = null

    /** The stored policy with the link measured right now (and the one-play preset, when one was picked). */
    val current: SmartPolicy
        get() = stored.copy(linkBps = SmartLinkMeter.estimateBps(android.os.SystemClock.elapsedRealtime()))
            .let { policy -> playPreset?.let { policy.copy(preset = it) } ?: policy }

    @Synchronized fun update(change: (SmartPolicy) -> SmartPolicy) { stored = change(stored).copy(linkBps = null) }
}

data class SmartCandidate<T>(
    val item: T,
    /** Names only (addon, title, description, file name). Never a URL. */
    val text: String,
    val sizeBytes: Long? = null,
    /** True when playback can start without waiting for a torrent or an uncached debrid file. */
    val playsNow: Boolean = true,
    /** Only behaviorHints.filename / clientResolve.filename, never title or description. */
    val fileName: String? = null,
    /** In the memory of bad sources (M3a): ranked right after the year, so it goes last but stays offered. */
    val blocked: Boolean = false,
    /** forge M6 (N6): addon + service ("Comet/TorBox"), for the memory of results. Never a URL. */
    val origin: String? = null,
)

data class SmartRanked<T>(
    val item: T,
    val info: ReleaseInfo,
    val audioTier: Int,
    val identity: IdentityVerdict = IdentityVerdict.UNKNOWN,
    /** The size the ranking used (stream field first, then the text), so nobody computes it again. */
    val sizeBytes: Long? = null,
)

/**
 * Orders sources for automatic playback. Priorities, most important first:
 * never a cinema recording, sample, trailer, disc image, archive, 3D or another episode → not a wrong year → not in
 * the memory of bad sources → a codec this device decodes and shows well → no burnt-in subtitles, upscale or split film → wanted audio → starts right
 * away → fits the preset's picture height → fits the link budget → the preset's own taste (its place in the size
 * distribution) → a corrected release → the addon's order.
 */
object SmartSourceRanker {
    const val AUDIO_LEKTOR = 5
    const val AUDIO_DUBBING = 4
    const val AUDIO_LANGUAGE = 3
    const val AUDIO_SUBTITLES_ONLY = 2
    const val AUDIO_UNKNOWN = 1
    const val AUDIO_OTHER = 0
    /** forge (owner 13.11): an old speech-synthesis voice-over sounds worse than anything else: the last resort. */
    const val AUDIO_OLD_TTS = -1
    /** Above this height a software-only decoder is not trusted to keep up. */
    const val SOFTWARE_DECODE_MAX_HEIGHT = 720

    /** Shorter durations are not trusted for a bitrate (menus, trailers, wrong metadata). */
    const val MIN_DURATION_SEC = 120L

    fun <T> rank(
        candidates: List<SmartCandidate<T>>,
        policy: SmartPolicy,
        expected: SmartExpected? = null,
    ): List<SmartRanked<T>> {
        val cap = policy.heightCap
        val duration = expected?.durationSec?.takeIf { it >= MIN_DURATION_SEC }
        val linkBudget = policy.linkBps?.let { it * policy.linkShare }
        // forge fix 14.11 (JMGO, B1): an addon answered Star Trek TNG S06E13 with Deep Space Nine S06E13, „id=MATCH”
        val otherShow = if (expected?.episode != null) otherShowFiles(candidates.map { it.fileName }) else emptySet()
        // pass 1: every key except the place in the size distribution
        val pending = candidates.mapIndexedNotNull { index, candidate ->
            val info = ReleaseParser.parse(candidate.text, candidate.sizeBytes)
            if (info.junk != null) return@mapIndexedNotNull null
            val verdict = identity(candidate.fileName, expected)
            if (verdict == IdentityVerdict.WRONG_EPISODE) return@mapIndexedNotNull null
            val size = candidate.sizeBytes?.takeIf { it > 0 } ?: info.textSizeBytes
            val bitrate = SmartPolicy.averageBitrateBps(size, duration)
            val budget = if (linkBudget == null || bitrate == null || bitrate <= linkBudget) 1L else 0L
            // a cinema sound track is one step worse than the tier the text claims
            val audio = audioTier(info, policy.audioLanguage).let { if (info.cinemaAudio && it > AUDIO_OTHER) maxOf(AUDIO_OTHER, it - 1) else it }
            val height = info.height
            val fit = when {
                height == null -> 1
                height <= cap -> 2
                else -> 0
            }
            // Above the cap the smallest step over it is the least wasteful.
            val heightKey = when {
                height == null -> 0
                fit == 0 -> -height
                else -> height
            }
            val noRemux = if (info.source == SourceKind.REMUX) 0L else 1L
            val device = if (decodesWell(info, policy) && displaysWell(info, policy)) 1L else 0L
            val clean = if (info.hardcodedSubs || info.upscaled || info.multiPart) 0L else 1L
            val known = if (verdict == IdentityVerdict.WRONG_YEAR || index in otherShow) 0L else 1L
            val notBlocked = if (candidate.blocked) 0L else 1L
            // forge M7 (N14): the next episode keeps the family of the one before (same height, audio tier, origin)
            val family = expected?.family?.let { f ->
                val same = (f.height == null || f.height == height) && f.audioTier == audio && (f.origin == null || f.origin == candidate.origin)
                // the same release group on top: the same encode settings, subtitles timing and intro length (N14)
                if (!same) 0L else if (f.group != null && f.group == info.group) 2L else 1L
            } ?: 0L
            val prefix = listOf(known, notBlocked, device, clean, audio.toLong(), if (candidate.playsNow) 1L else 0L, fit.toLong(), budget, family)
            val group = prefix + heightKey.toLong() + (if (policy.preset == SmartPreset.BEST) 0L else noRemux)
            Pending(SmartRanked(candidate.item, info, audio, verdict, size), prefix, group, heightKey.toLong(), noRemux, index, candidate.origin)
        }
        // pass 2: each group's sorted known sizes give the place of every file in it
        val sizesByGroup = HashMap<List<Long>, LongArray>()
        pending.groupBy { it.group }.forEach { (group, rows) ->
            sizesByGroup[group] = rows.mapNotNull { it.ranked.sizeBytes }.toLongArray().also { it.sort() }
        }
        return pending.map { row ->
            val info = row.ranked.info
            val size = row.ranked.sizeBytes
            val sizeFit = sizeFit(sizesByGroup.getValue(row.group), size, policy.preset)
            val dvOk = if (dvWithoutFallbackOnNonDvScreen(info, policy)) 0L else 1L
            val sourceRank = (info.source?.rank ?: 0).toLong()
            val taste: List<Long> = when (policy.preset) {
                // the lower quarter of the sizes: no remux, then efficient codec; ties → the smaller file
                SmartPreset.SAVER -> listOf(
                    row.heightKey, dvOk, row.noRemux, sizeFit, -(size ?: Long.MAX_VALUE),
                    if (info.codec == VideoCodec.HEVC) 1 else 0,
                )
                // around 60% of the sizes, a good encode rather than a disc copy; ties → the bigger file
                SmartPreset.BALANCED -> listOf(row.heightKey, dvOk, row.noRemux, sizeFit, size ?: 0L, sourceRank)
                SmartPreset.BEST -> listOf(
                    row.heightKey, dvOk, sourceRank, if (hdrShown(info, policy)) 1L else 0L, sizeFit, size ?: 0L,
                )
            }
            // forge M6 (N6): an origin that fails often goes after the others of its quality group; results break ties last
            val origin = row.origin
            val originOk = if (SmartOutcomeStore.isBad(origin)) 0L else 1L
            val keys = row.prefix + listOf(taste[0], originOk) + taste.drop(1) + (if (info.repack) 1L else 0L) + SmartOutcomeStore.tieKey(origin)
            Row(row.ranked, keys, row.index)
        }.sortedWith(rowOrder).map { it.ranked }
    }

    /** Where in the size distribution each preset aims; index into the sorted known sizes. */
    fun percentileTarget(preset: SmartPreset, n: Int): Int = when (preset) {
        // floor(p × n), kept inside the list: 5 files → SAVER the 2nd, BALANCED the 4th; 2 files → smaller / larger
        SmartPreset.SAVER -> (0.25 * n).toInt().coerceIn(0, (n - 1).coerceAtLeast(0))
        SmartPreset.BALANCED -> (0.60 * n).toInt().coerceIn(0, (n - 1).coerceAtLeast(0))
        SmartPreset.BEST -> (n - 1).coerceAtLeast(0)
    }

    /** -distance of this size's place from the preset's target; files of equal size share it; unknown → -n. */
    private fun sizeFit(sorted: LongArray, size: Long?, preset: SmartPreset): Long {
        val n = sorted.size
        if (size == null || n == 0) return -n.toLong()
        val target = percentileTarget(preset, n)
        val lo = firstIndexAbove(sorted, size - 1)
        val hi = firstIndexAbove(sorted, size) - 1
        val distance = when {
            target < lo -> lo - target
            target > hi -> target - hi
            else -> 0
        }
        return -distance.toLong()
    }

    /** First index whose value is greater than [value] (binary search, so the ranking stays O(n log n)). */
    private fun firstIndexAbove(sorted: LongArray, value: Long): Int {
        var lo = 0
        var hi = sorted.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sorted[mid] <= value) lo = mid + 1 else hi = mid
        }
        return lo
    }

    /** P9: plain Dolby Vision (no HDR layer named) on a screen known to show HDR but not DV. */
    private fun dvWithoutFallbackOnNonDvScreen(info: ReleaseInfo, policy: SmartPolicy): Boolean {
        val screen = policy.hdr ?: return false
        return screen.isNotEmpty() && HdrKind.DOLBY_VISION !in screen && info.hdr == HdrKind.DOLBY_VISION && !info.hdrFallback
    }

    private class Pending<T>(
        val ranked: SmartRanked<T>,
        val prefix: List<Long>,
        val group: List<Long>,
        val heightKey: Long,
        val noRemux: Long,
        val index: Int,
        val origin: String? = null, // forge M6 (N6)
    )

    private val episodeRange = Regex("(?i)(?<![a-z])EP?\\s?\\d{1,4}(?:\\s?-\\s?(?:E|EP)?\\s?\\d{1,4}(?![0-9a-z])|EP?\\d{1,4})")

    private val episodeMark = Regex("(?i)(?<![a-z0-9])(?:S\\d{1,2}\\s?E\\d{1,4}|\\d{1,2}x\\d{2,3})(?![0-9])")
    private val showWord = Regex("[a-z0-9]+")
    private val showNoise = setOf("the", "a", "and", "of", "complete", "series", "season", "www", "tv")

    /** The show name a file of an episode starts with ("star trek next generation"), null when it names none. */
    internal fun showWords(fileName: String?): Set<String>? {
        val name = fileName?.lowercase() ?: return null
        val mark = episodeMark.find(name) ?: return null
        val words = showWord.findAll(name.substring(0, mark.range.first)).map { it.value }
            .filter { it !in showNoise && !(it.length == 4 && it.all(Char::isDigit)) }.toSet()
        return words.takeIf { it.isNotEmpty() }
    }

    /**
     * Indexes of the files whose show name is plainly another show than the one most files name (at least 3 files,
     * 40 % of those naming one): fewer than half the words in common and neither name inside the other. Only a
     * penalty, never dropped: „Star.Trek.TNG” may be the right show under a short name.
     */
    internal fun otherShowFiles(fileNames: List<String?>): Set<Int> {
        val shows = fileNames.map { showWords(it) }
        val named = shows.filterNotNull()
        val (top, count) = named.groupingBy { it }.eachCount().maxByOrNull { it.value }?.toPair() ?: return emptySet()
        if (count < 3 || count * 5 < named.size * 2) return emptySet()
        return shows.withIndex().filter { (_, words) ->
            words != null && !top.containsAll(words) && !words.containsAll(top) &&
                (words intersect top).size * 2 < (words union top).size
        }.map { it.index }.toSet()
    }

    /**
     * Does the file name belong to the expected title? Only the file name is read; anything it does not say is
     * UNKNOWN. A season pack or a range of episodes is UNKNOWN, never wrong.
     */
    fun identity(fileName: String?, expected: SmartExpected?): IdentityVerdict {
        if (expected == null || fileName.isNullOrBlank()) return IdentityVerdict.UNKNOWN
        val name = if (fileName.length > ReleaseParser.MAX_TEXT) fileName.substring(0, ReleaseParser.MAX_TEXT) else fileName
        val wantedEpisode = expected.episode
        if (wantedEpisode != null) {
            if (episodeRange.containsMatchIn(name)) return IdentityVerdict.UNKNOWN
            val (season, episode) = com.nuvio.tv.core.player.FilenameEpisodeMatcher.extractSeasonEpisode(name)
                ?: return IdentityVerdict.UNKNOWN
            val wrongSeason = season != null && expected.season != null && season != expected.season
            return if (wrongSeason || episode != wantedEpisode) IdentityVerdict.WRONG_EPISODE else IdentityVerdict.MATCH
        }
        val wantedYear = expected.year ?: return IdentityVerdict.UNKNOWN
        val years = ReleaseParser.tokenize(name).mapNotNull { token ->
            if (token.length == 4 && token.all { it in '0'..'9' }) token.toInt().takeIf { it in 1900..2099 } else null
        }
        return when {
            years.isEmpty() -> IdentityVerdict.UNKNOWN
            years.any { kotlin.math.abs(it - wantedYear) <= 1 } -> IdentityVerdict.MATCH
            else -> IdentityVerdict.WRONG_YEAR
        }
    }

    /**
     * False when the screen cannot show the source well: HDR on a screen known to be SDR, or 10-bit AVC (Hi10P has
     * no hardware decoders). Dolby Vision on an HDR screen without DV is fine: the app converts it.
     */
    fun displaysWell(info: ReleaseInfo, policy: SmartPolicy): Boolean {
        if (policy.hdr != null && policy.hdr.isEmpty() && info.hdr != null) return false
        if (info.codec == VideoCodec.AVC && info.tenBit) return false
        return true
    }

    /** BEST only: the source's HDR is one the screen shows (DV and HDR10+ fall back to any HDR). */
    private fun hdrShown(info: ReleaseInfo, policy: SmartPolicy): Boolean {
        val kind = info.hdr ?: return false
        val screen = policy.hdr ?: return false
        return kind in screen || (screen.isNotEmpty() && (kind == HdrKind.DOLBY_VISION || kind == HdrKind.HDR10_PLUS))
    }

    /**
     * False only when the device has spoken against this source: no decoder for the named codec, a decoder
     * smaller than the picture, or software decoding of a large picture. Unknown codec, unknown height or an
     * unprobed device are never held against a source. Such sources stay in the list as a last resort,
     * because another player engine may still handle them.
     */
    fun decodesWell(info: ReleaseInfo, policy: SmartPolicy): Boolean {
        val decoders = policy.decoders ?: return true
        val codec = info.codec ?: return true
        val cap = decoders[codec] ?: return false
        val height = info.height ?: return true
        if (cap.maxHeight != null && height > cap.maxHeight) return false
        return cap.hardware || height <= SOFTWARE_DECODE_MAX_HEIGHT
    }

    /** How well the text supports the wanted audio. Missing text is uncertainty, not absence. */
    fun audioTier(info: ReleaseInfo, wanted: String?): Int {
        if (info.oldTtsVoice) return AUDIO_OLD_TTS
        val language = wanted?.lowercase()?.takeIf { it.isNotBlank() } ?: return AUDIO_UNKNOWN
        val named = language in info.audioLanguages
        return when {
            language == "pl" && info.lektor -> AUDIO_LEKTOR
            named && info.dubbing -> AUDIO_DUBBING
            named -> AUDIO_LANGUAGE
            language in info.subtitleLanguages -> AUDIO_SUBTITLES_ONLY
            info.audioLanguages.isEmpty() || info.multiAudio -> AUDIO_UNKNOWN
            else -> AUDIO_OTHER
        }
    }

    private class Row<T>(val ranked: SmartRanked<T>, val keys: List<Long>, val index: Int)

    private val rowOrder = Comparator<Row<*>> { a, b ->
        for (i in a.keys.indices) {
            val c = b.keys[i].compareTo(a.keys[i])
            if (c != 0) return@Comparator c
        }
        a.index.compareTo(b.index)
    }
}

/** The only place that turns a [Stream] into ranker input. */
object SmartStreamRanking {
    /**
     * Diagnostics: one line per pick, set by the app at start. Names only, never a URL. Check 13.11: lines equal to the
     * previous one were left out, which hid the resume=hit of a repeated pick; every pick now leaves its line.
     */
    @Volatile var log: ((String) -> Unit)? = null

    fun rank(streams: List<Stream>, policy: SmartPolicy, expected: SmartExpected? = null): List<Stream> {
        // forge M3a (13.11): one pick between recovery attempts took 7–10 s on the emulator; the times say where.
        val t0 = System.nanoTime()
        val candidates = streams.map(::candidate)
        val t1 = System.nanoTime()
        // M3b (N9): a metered network ranks as SAVER; the stored preset is left alone.
        val byNetwork = if (policy.metered) policy.copy(preset = SmartPreset.SAVER) else policy
        // forge hook M4: the session ceiling (memory of the quality change during playback) cuts the height cap
        val ceiling = com.nuvio.tv.core.autoquality.AutoQualitySession.let { it.memory.ceiling(it.nowMs()) }
        val effective = if (ceiling != null) byNetwork.copy(sessionCeilingHeight = ceiling.height) else byNetwork
        val rankedRaw = SmartSourceRanker.rank(candidates, effective, expected)
        val t2 = System.nanoTime()
        // M3b (N8): the file this title last played goes first, unless it is put aside. Looked up with the stored
        // preset, so a preset the viewer changed since does not bring the old file back.
        val resumeKey = expected?.let { SmartLastFile.get(it.contentId, it.season, it.episode, policy.preset) }
        val (resumed, resume) = resumeFirst(rankedRaw, resumeKey, keyOf = { lastFileKeyOf(it.item) },
            blocked = { com.nuvio.tv.core.player.recovery.SmartBadSources.isBlocked(it.item) ||
                com.nuvio.tv.core.autoquality.AutoQualitySession.isServerBlocked(it.item) }) // forge hook M4
        // Check 13.11: without the viewer's P2P consent a P2P source (also the remembered file) goes after all others.
        val (ranked, p2pMoved) = p2pLast(resumed, policy.p2pAllowed) { it.item.isTorrent() }
        log?.let { sink ->
            val dropped = candidates.count { SmartSourceRanker.identity(it.fileName, expected) == IdentityVerdict.WRONG_EPISODE }
            val line = describe(streams.size, ranked, effective, dropped, expected).let { if (policy.metered) it.replaceFirst("preset=SAVER", "preset=SAVER(metered)") else it }
            val p2p = if (policy.p2pAllowed) "ok" else "skip:$p2pMoved"
            sink("$line resume=$resume p2p=$p2p ms=${(t1 - t0) / 1_000_000}/${(t2 - t1) / 1_000_000}")
        }
        return ranked.map { it.item }
    }

    /** [list] with the P2P entries moved behind the others (order kept) unless [allowed]; and how many were moved. */
    internal fun <T> p2pLast(list: List<T>, allowed: Boolean, isP2p: (T) -> Boolean): Pair<List<T>, Int> {
        if (allowed) return list to 0
        val (p2p, other) = list.partition(isP2p)
        return (other + p2p) to p2p.size
    }

    /**
     * [list] with the first entry whose key is [resumeKey] moved to the front, unless it is [blocked]; and what
     * happened: "hit", "blocked" or "-" (no key, or no such entry).
     */
    internal fun <T> resumeFirst(list: List<T>, resumeKey: String?, keyOf: (T) -> String, blocked: (T) -> Boolean): Pair<List<T>, String> {
        if (resumeKey == null) return list to "-"
        val index = list.indexOfFirst { keyOf(it) == resumeKey }
        if (index < 0) return list to "-"
        if (blocked(list[index])) return list to "blocked"
        if (index == 0) return list to "hit"
        return (listOf(list[index]) + list.subList(0, index) + list.subList(index + 1, list.size)) to "hit"
    }

    internal fun describe(offered: Int, ranked: List<SmartRanked<Stream>>, policy: SmartPolicy, dropped: Int = 0, expected: SmartExpected? = null): String {
        fun mbps(bps: Double?) = bps?.let { String.format(java.util.Locale.ROOT, "%.1f", it / 1_000_000) } ?: "-"
        val head = "preset=${policy.preset} audio=${policy.audioLanguage ?: "-"} cap=${policy.heightCap} " +
            "device=${if (policy.decoders == null) "UNKNOWN" else "probed"} offered=$offered safe=${ranked.size} " +
            "link=${mbps(policy.linkBps?.toDouble())} budget=${mbps(policy.linkBps?.let { it * policy.linkShare })} " +
            "id=${ranked.firstOrNull()?.identity ?: "-"} year=${expected?.year ?: "-"} dropped=$dropped" // forge 14.11: why id=UNKNOWN
        val top = ranked.firstOrNull() ?: return "$head pick=NONE"
        val stream = top.item
        val size = top.sizeBytes
        val name = (stream.behaviorHints?.filename ?: stream.clientResolve?.filename ?: stream.title ?: stream.name).orEmpty()
            .lineSequence().firstOrNull().orEmpty().take(90)
        return "$head pick=[${top.info.height ?: "?"}p ${top.info.source ?: "?"} ${top.info.codec ?: "?"} audioTier=${top.audioTier} " +
            "decodes=${SmartSourceRanker.decodesWell(top.info, policy)} size=${size?.let { it / 1_000_000 } ?: "?"}MB addon=${stream.addonName}] name=$name"
    }

    fun candidate(stream: Stream) = SmartCandidate(
        item = stream,
        text = buildString {
            append(stream.name.orEmpty()).append('\n')
            append(stream.title.orEmpty()).append('\n')
            append(stream.description.orEmpty()).append('\n')
            append(stream.quality.orEmpty()).append('\n')
            append(stream.behaviorHints?.filename.orEmpty()).append('\n')
            append(stream.clientResolve?.filename.orEmpty()).append('\n')
            append(stream.clientResolve?.torrentName.orEmpty()).append('\n')
            stream.badges.forEach { append(it.name).append(' ') }
        },
        sizeBytes = stream.behaviorHints?.videoSize?.takeIf { it > 0 }
            ?: stream.debridCacheStatus?.cachedSize?.takeIf { it > 0 },
        playsNow = stream.getStreamUrl() != null || stream.isDirectDebrid(),
        origin = SmartOutcomeStore.originOf(stream.addonName, stream.clientResolve?.service), // forge M6 (N6)
        fileName = (stream.behaviorHints?.filename ?: stream.clientResolve?.filename)?.takeIf { it.isNotBlank() },
        // forge hook M4: a source whose server is blocked in the session goes after the rest, like a bad source
        blocked = com.nuvio.tv.core.player.recovery.SmartBadSources.isBlocked(stream) ||
            com.nuvio.tv.core.autoquality.AutoQualitySession.isServerBlocked(stream),
    )
}

/** Player setting → ISO 639-1 code; "default" (follow the file) is no preference. */
fun smartAudioLanguage(setting: String?, deviceLanguage: String = java.util.Locale.getDefault().language): String? =
    when (val value = setting?.trim()?.lowercase().orEmpty()) {
        "", "default", "none" -> null
        "device" -> deviceLanguage.take(2).lowercase().takeIf { it.length == 2 }
        "pol" -> "pl"
        "eng" -> "en"
        else -> value.take(2)
    }
