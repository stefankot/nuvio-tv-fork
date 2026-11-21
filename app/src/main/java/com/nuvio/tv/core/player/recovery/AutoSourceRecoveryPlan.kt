package com.nuvio.tv.core.player.recovery

import com.nuvio.tv.core.player.StreamAutoPlaySelector
import com.nuvio.tv.core.stream.ReleaseFactsParser
import com.nuvio.tv.domain.model.ReleaseResolution
import com.nuvio.tv.domain.model.StreamDebridCacheState
import com.nuvio.tv.fork.sources.Placeholders
import com.nuvio.tv.data.local.PlayerSettings
import com.nuvio.tv.data.local.StreamAutoPlayMode
import com.nuvio.tv.data.local.StreamAutoPlaySource
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Immutable selection snapshot. Retry order is owned by the existing autoplay selector. */
data class AutoSourceRecoveryPlan(
    val streams: List<Stream>,
    val mode: StreamAutoPlayMode,
    val regex: String,
    val rankedRegex: List<String>,
    val source: StreamAutoPlaySource,
    val installedAddonNames: Set<String>,
    val selectedAddons: Set<String>,
    val selectedPlugins: Set<String>,
    val preferredBingeGroup: String?,
    val bingeGroupOnly: Boolean,
    val initialSource: Stream?,
    val profileId: Int,
    val videoId: String?,
    val attempted: Set<String> = emptySet(),
    val startupState: StartupSourceRecovery.State? = null,
    private val firstSourceOrder: List<Stream>? = null
) {
    private var rankedForRecovery = firstSourceOrder

    fun withStreams(updated: List<Stream>): AutoSourceRecoveryPlan = copy(streams = updated, firstSourceOrder = null)

    suspend fun next(attempted: Set<String>, maxPreferredHeight: Int? = null): Stream? = withContext(Dispatchers.Default) {
        fun untried(stream: Stream): Boolean =
            recoverySourceKey(stream) !in attempted &&
                "url:${stream.getStreamUrl()}" !in attempted &&
                !Placeholders.isMarked(stream.getStreamUrl()) &&
                stream.debridCacheStatus?.state != StreamDebridCacheState.NOT_CACHED

        val available = streams.filter(::untried)
        val ranked = if (mode == StreamAutoPlayMode.FIRST_STREAM && !bingeGroupOnly) {
            val order = rankedForRecovery ?: StreamAutoPlaySelector.orderFirstPlayableStreams(
                streams, source, installedAddonNames, selectedAddons, selectedPlugins
            ).also { rankedForRecovery = it }
            val candidates = order.filter(::untried)
            if (maxPreferredHeight == null) candidates else {
                val (lighter, rest) = candidates.partition {
                    val resolution = ReleaseFactsParser.resolutionOf(it)
                    resolution != ReleaseResolution.UNKNOWN &&
                        (resolution.label.removeSuffix("p").toIntOrNull() ?: Int.MAX_VALUE) <= maxPreferredHeight
                }
                lighter + rest
            }
        } else null
        StreamAutoPlaySelector.selectAutoPlayStreamAsync(
            streams = available,
            mode = mode,
            regexPattern = regex,
            rankedRegexPatterns = rankedRegex,
            source = source,
            installedAddonNames = installedAddonNames,
            selectedAddons = selectedAddons,
            selectedPlugins = selectedPlugins,
            preferredBingeGroup = preferredBingeGroup,
            preferBingeGroupInSelection = preferredBingeGroup != null && maxPreferredHeight == null,
            bingeGroupOnly = bingeGroupOnly,
            bingeKeepsFilters = true,
            firstSourceOrder = ranked
        )
    }

    companion object {
        fun create(
            streams: List<Stream>, settings: PlayerSettings, installedAddonNames: Set<String>,
            preferredBingeGroup: String?, initialSource: Stream?, profileId: Int, videoId: String?,
            bingeGroupOnly: Boolean = settings.streamAutoPlayMode == StreamAutoPlayMode.MANUAL,
            firstSourceOrder: List<Stream>? = null
        ) = AutoSourceRecoveryPlan(
            streams.toList(), settings.streamAutoPlayMode, settings.streamAutoPlayRegex,
            settings.streamAutoPlayRankedRegex.toList(), settings.streamAutoPlaySource,
            installedAddonNames.toSet(), settings.streamAutoPlaySelectedAddons.toSet(),
            settings.streamAutoPlaySelectedPlugins.toSet(), preferredBingeGroup, bingeGroupOnly,
            initialSource, profileId, videoId, firstSourceOrder = firstSourceOrder
        )
    }
}

/** Keep signed-URL refreshes of one provider/file from repeatedly consuming the retry queue. */
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
