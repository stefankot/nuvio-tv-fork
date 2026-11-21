package com.nuvio.tv.core.player

import com.nuvio.tv.core.streams.ranking.StreamRegexRanking
import com.nuvio.tv.core.build.AppFeaturePolicy
import com.nuvio.tv.data.local.StreamAutoPlayMode
import com.nuvio.tv.data.local.StreamAutoPlaySource
import com.nuvio.tv.domain.model.AddonStreams
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamDebridCacheState
import com.nuvio.tv.domain.model.indexOfPluginBingeMatch
import com.nuvio.tv.domain.model.scraperIdFromPluginBingeGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StreamAutoPlaySelector {
    fun orderAddonStreams(
        streams: List<AddonStreams>,
        installedOrder: List<String>,
        preferredNames: Set<String> = emptySet()
    ): List<AddonStreams> {
        if (streams.isEmpty()) return streams
        if (preferredNames.isNotEmpty()) {
            val (preferred, rest) = streams.partition { it.addonName in preferredNames }
            return preferred + orderAddonStreams(rest, installedOrder)
        }

        val addonRankByName = HashMap<String, Int>(installedOrder.size)
        installedOrder.forEachIndexed { index, addonName ->
            if (addonName !in addonRankByName) {
                addonRankByName[addonName] = index
            }
        }

        val (directDebridEntries, remainingEntries) = streams.partition {
            it.streams.any { stream -> stream.isDirectDebrid() }
        }
        if (installedOrder.isEmpty()) return directDebridEntries + remainingEntries
        val (addonEntries, pluginEntries) = remainingEntries.partition { it.addonName in addonRankByName }
        val orderedAddons = addonEntries.sortedBy { addonRankByName.getValue(it.addonName) }
        return directDebridEntries + orderedAddons + pluginEntries
    }

    private fun isPlayable(stream: Stream): Boolean {
        // External URL streams (e.g. error pages, web links) are not playable.
        if (stream.isExternal()) return false
        if (stream.serverTarget != null) return true
        // Streams with a direct URL are always playable regardless of debrid cache status.
        if (stream.getStreamUrl() != null) return true
        when (stream.debridCacheStatus?.state) {
            StreamDebridCacheState.CHECKING,
            StreamDebridCacheState.NOT_CACHED,
            StreamDebridCacheState.UNKNOWN -> return false
            StreamDebridCacheState.CACHED,
            null -> Unit
        }
        return stream.isTorrent() ||
            stream.isDirectDebrid() ||
            // A YouTube id is resolved to a playable URL on the device when the stream is played.
            // Builds without in-app YouTube playback open the watch page instead, an external link.
            (AppFeaturePolicy.inAppTrailerPlaybackEnabled && stream.youTubeIdToResolve() != null)
    }

    /**
     * Exact binge group for addons. For a local plugin, the stream whose identity tokens
     * still match after the next episode's own shared words are removed.
     * Only playable streams are considered, matching the autoplay selector.
     */
    fun selectBingeGroupMatch(streams: List<Stream>, targetBingeGroup: String): Stream? {
        if (targetBingeGroup.isBlank()) return null
        val playableStreams = streams.filter { !it.isExternal() && isPlayable(it) }
        if (playableStreams.isEmpty()) return null

        val exact = playableStreams.firstOrNull { it.behaviorHints?.bingeGroup == targetBingeGroup }
        if (exact != null) return exact
        if (scraperIdFromPluginBingeGroup(targetBingeGroup) == null) return null

        val index = indexOfPluginBingeMatch(
            targetBingeGroup,
            playableStreams.map { it.behaviorHints?.bingeGroup }
        )
        return index?.let { playableStreams[it] }
    }

    /**
     * Filename and info-hash binge matches run as soon as a stream arrives, before a
     * torrent's debrid check finishes. Addon groups stay an exact string and are not
     * required to be playable yet. Plugin groups fall back to identity tokens, and
     * that fallback only considers streams that are already playable.
     */
    fun selectEarlyBingeGroupMatch(streams: List<Stream>, targetBingeGroup: String): Stream? {
        if (targetBingeGroup.isBlank()) return null
        val visible = streams.filter { !it.isExternal() }
        val exact = visible.firstOrNull { it.behaviorHints?.bingeGroup == targetBingeGroup }
        if (exact != null) return exact
        if (scraperIdFromPluginBingeGroup(targetBingeGroup) == null) return null
        return selectBingeGroupMatch(visible, targetBingeGroup)
    }

    private fun scopedCandidates(
        streams: List<Stream>,
        source: StreamAutoPlaySource,
        installedAddonNames: Set<String>,
        selectedAddons: Set<String>,
        selectedPlugins: Set<String>
    ): List<Stream> {
        val effectiveSource = if (!AppFeaturePolicy.pluginsEnabled && source == StreamAutoPlaySource.ENABLED_PLUGINS_ONLY) {
            StreamAutoPlaySource.INSTALLED_ADDONS_ONLY
        } else {
            source
        }

        val sourceScopedStreams = when (effectiveSource) {
            StreamAutoPlaySource.ALL_SOURCES -> streams
            StreamAutoPlaySource.INSTALLED_ADDONS_ONLY -> streams.filter { it.serverTarget != null || it.addonName in installedAddonNames }
            StreamAutoPlaySource.ENABLED_PLUGINS_ONLY -> streams.filter { it.addonName !in installedAddonNames }
        }
        return sourceScopedStreams.filter { stream ->
            val isAddonStream = stream.addonName in installedAddonNames
            if (stream.serverTarget != null) {
                true
            } else if (isAddonStream) {
                selectedAddons.isEmpty() || stream.addonName in selectedAddons
            } else {
                selectedPlugins.isEmpty() || stream.addonName in selectedPlugins
            }
        }
    }

    // Fork hook: shared first-source order for Echo and recovery.
    internal suspend fun orderFirstPlayableStreams(
        streams: List<Stream>,
        source: StreamAutoPlaySource,
        installedAddonNames: Set<String>,
        selectedAddons: Set<String>,
        selectedPlugins: Set<String>
    ): List<Stream> = withContext(Dispatchers.Default) {
        val scoped = scopedCandidates(streams, source, installedAddonNames, selectedAddons, selectedPlugins)
        com.nuvio.tv.fork.sources.KeRanking.order(scoped.filter(::isPlayable))
    }

    suspend fun selectAutoPlayStreamAsync(
        streams: List<Stream>,
        mode: StreamAutoPlayMode,
        regexPattern: String,
        source: StreamAutoPlaySource,
        installedAddonNames: Set<String>,
        selectedAddons: Set<String>,
        selectedPlugins: Set<String>,
        preferredBingeGroup: String? = null,
        preferBingeGroupInSelection: Boolean = false,
        bingeGroupOnly: Boolean = false,
        rankedRegexPatterns: List<String> = emptyList(),
        bingeKeepsFilters: Boolean = false, // Nuvio RS: source recovery retries stay inside the user's filters
        firstSourceOrder: List<Stream>? = null // Fork hook: shared first-source order
    ): Stream? {
        val order = firstSourceOrder ?: if (mode == StreamAutoPlayMode.FIRST_STREAM && !bingeGroupOnly) {
            orderFirstPlayableStreams(streams, source, installedAddonNames, selectedAddons, selectedPlugins)
        } else null
        return com.nuvio.tv.core.streams.ranking.RankedRegexWork.run(
            enabled = mode == StreamAutoPlayMode.REGEX_MATCH && rankedRegexPatterns.isNotEmpty() && !bingeGroupOnly
        ) { checkpoint ->
            selectAutoPlayStream(
                streams = streams,
                mode = mode,
                regexPattern = regexPattern,
                source = source,
                installedAddonNames = installedAddonNames,
                selectedAddons = selectedAddons,
                selectedPlugins = selectedPlugins,
                preferredBingeGroup = preferredBingeGroup,
                preferBingeGroupInSelection = preferBingeGroupInSelection,
                bingeGroupOnly = bingeGroupOnly,
                rankedRegexPatterns = rankedRegexPatterns,
                rankedRegexCheckCancelled = checkpoint,
                bingeKeepsFilters = bingeKeepsFilters,
                firstSourceOrder = order
            )
        }
    }

    fun selectAutoPlayStream(
        streams: List<Stream>,
        mode: StreamAutoPlayMode,
        regexPattern: String,
        source: StreamAutoPlaySource,
        installedAddonNames: Set<String>,
        selectedAddons: Set<String>,
        selectedPlugins: Set<String>,
        preferredBingeGroup: String? = null,
        preferBingeGroupInSelection: Boolean = false,
        bingeGroupOnly: Boolean = false,
        rankedRegexPatterns: List<String> = emptyList(),
        rankedRegexCheckCancelled: () -> Unit = {},
        bingeKeepsFilters: Boolean = false, // Nuvio RS: source recovery retries stay inside the user's filters
        firstSourceOrder: List<Stream>? = null // Fork hook: shared first-source order
    ): Stream? {
        if (streams.isEmpty()) return null

        val candidateStreams = scopedCandidates(streams, source, installedAddonNames, selectedAddons, selectedPlugins)
        // Binge group matching takes priority over mode and source filters — even in MANUAL mode,
        // a persisted binge group should auto-play without showing the picker.
        // Search ALL streams, not just candidateStreams, since binge group reuse should
        // not be limited by autoplay source/addon selection filters. An installed-addons-only
        // filter would otherwise drop a plugin result set and open the sidebar.
        val targetBingeGroup = preferredBingeGroup?.trim().orEmpty()
        if (preferBingeGroupInSelection && targetBingeGroup.isNotEmpty()) {
            val bingeGroupMatch = selectBingeGroupMatch(if (bingeKeepsFilters) candidateStreams else streams, targetBingeGroup)
            if (bingeGroupMatch != null) return bingeGroupMatch
            // When bingeGroupOnly is set (MANUAL mode with only binge-group
            // preference enabled), don't fall back to a non-matching stream —
            // return null so the caller shows the stream picker instead.
            if (bingeGroupOnly) return null
        }

        if (bingeGroupOnly) return null

        if (candidateStreams.isEmpty()) return null

        if (mode == StreamAutoPlayMode.MANUAL) return null

        return when (mode) {
            StreamAutoPlayMode.MANUAL -> null
            StreamAutoPlayMode.FIRST_STREAM -> (firstSourceOrder ?: com.nuvio.tv.fork.sources.KeRanking.order(candidateStreams)).firstOrNull { isPlayable(it) && !com.nuvio.tv.fork.sources.Placeholders.isMarked(it.getStreamUrl()) } // Fork hook: ke-rank
            StreamAutoPlayMode.REGEX_MATCH -> {
                if (rankedRegexPatterns.isNotEmpty()) {
                    return StreamRegexRanking.bestMatch(
                        candidateStreams.filter { isPlayable(it) }, rankedRegexPatterns, rankedRegexCheckCancelled
                    )
                }
                val pattern = regexPattern.trim()

                // Try to compile the user regex
                val userRegex = runCatching { Regex(pattern, RegexOption.IGNORE_CASE) }.getOrNull()
                if (userRegex == null) return null

                // Auto-extract exclusion patterns from negative lookaheads
                val exclusionMatches = Regex("\\(\\?![^)]*?\\(([^)]+)\\)").findAll(pattern)

                val exclusionWords = exclusionMatches
                    .flatMap { match -> match.groupValues[1].split("|") }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toList()

                // The legacy extraction is heuristic. A valid original expression can have
                // a capture we cannot turn into an independent expression, e.g. a ')' in a class.
                val excludeRegex = exclusionWords.takeIf { it.isNotEmpty() }?.let {
                    runCatching { Regex("\\b(${it.joinToString("|")})\\b", RegexOption.IGNORE_CASE) }.getOrNull()
                }

                candidateStreams.firstOrNull { stream ->
                    if (!isPlayable(stream)) return@firstOrNull false

                    val searchableText = buildString {
                        append(stream.addonName).append(' ')
                        append(stream.name.orEmpty()).append(' ')
                        append(stream.title.orEmpty()).append(' ')
                        append(stream.description.orEmpty()).append(' ')
                        append(stream.getStreamUrl().orEmpty())
                        if (stream.isTorrent()) append(' ').append(stream.infoHash.orEmpty())
                    }

                    userRegex.containsMatchIn(searchableText) && excludeRegex?.containsMatchIn(searchableText) != true
                }
            }

        }
    }
}
