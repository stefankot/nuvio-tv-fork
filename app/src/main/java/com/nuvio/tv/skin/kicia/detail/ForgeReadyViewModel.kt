package com.nuvio.tv.skin.kicia.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.core.player.StreamAutoPlaySelector
import com.nuvio.tv.data.local.BingeGroupCacheDataStore
import com.nuvio.tv.data.local.PlayerSettingsDataStore
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.enabledAddons
import com.nuvio.tv.domain.repository.AddonRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import com.nuvio.tv.domain.repository.StreamRepository
import com.nuvio.tv.fork.sources.ReadyInfo
import com.nuvio.tv.fork.sources.ReadyPick
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Looks for the source of the title on the detail screen while the viewer reads it, so "Ready" can say
 * what Play will start. It reuses the repository's own search (and its cache): the same results the stream
 * list shows moments later, so Play is not slower for it.
 *
 * The file named here is chosen by the same selector and the same profile settings Play uses (mode, regex,
 * presets, addon filters, series continuity). When Play would open the list instead, only the number of
 * sources is shown. Not mirrored: "reuse the last link" and the addon wait time, so a slow addon that Play
 * does not wait for can still change the answer. No link is opened here, so the size is the addon's claim.
 */
@HiltViewModel
class ForgeReadyViewModel @Inject constructor(
    private val streams: StreamRepository,
    private val playerSettings: PlayerSettingsDataStore,
    private val addons: AddonRepository,
    private val bingeGroups: BingeGroupCacheDataStore,
) : ViewModel() {
    private val _info = MutableStateFlow<ReadyInfo?>(null)
    val info: StateFlow<ReadyInfo?> = _info.asStateFlow()
    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private var key: String? = null
    private var job: Job? = null

    fun load(type: String, videoId: String, season: Int?, episode: Int?, contentId: String? = null) {
        val wanted = "$type|$videoId|$season|$episode"
        if (wanted == key) return
        key = wanted
        job?.cancel()
        _info.value = null
        _searching.value = true
        job = viewModelScope.launch {
            try {
                streams.getStreamsFromAllAddons(type, videoId, season, episode).collectLatest { result ->
                    when (result) {
                        is NetworkResult.Success -> _info.value = pick(result.data, contentId ?: videoId.substringBefore(":"), season, episode)
                        is NetworkResult.Error -> Unit
                        NetworkResult.Loading -> Unit
                    }
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // No result is simply no "Ready" line.
            } finally {
                _searching.value = false
            }
        }
    }

    private suspend fun pick(groups: List<com.nuvio.tv.domain.model.AddonStreams>, contentId: String, season: Int?, episode: Int?): ReadyInfo? {
        val installedOrder = addons.getInstalledAddons().first().enabledAddons().map { it.displayName }
        // the order Play uses: the stream screen orders the addons the same way before choosing
        val all: List<Stream> = ReadyPick.playOrder(groups, installedOrder)
        if (all.isEmpty()) return null
        val settings = playerSettings.playerSettings.first()
        // Like the stream screen: Regex mode without a usable pattern opens the source list.
        if (settings.streamAutoPlayMode == com.nuvio.tv.data.local.StreamAutoPlayMode.REGEX_MATCH &&
            !com.nuvio.tv.core.player.StreamAutoPlayPolicy.isRegexSelectionConfigured(settings.streamAutoPlayRegex, settings.streamAutoPlayRankedRegex)
        ) return ReadyPick.choice(all)
        val installed = installedOrder.toSet()
        val bingeGroup = if (settings.streamAutoPlayPreferBingeGroupForNextEpisode && settings.streamAutoPlayReuseBingeGroup) {
            bingeGroups.get(contentId)
        } else null
        // off the main thread: a viewer's regex over hundreds of sources is not free
        val chosen = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { StreamAutoPlaySelector.selectAutoPlayStreamAsync(
            streams = all,
            mode = settings.streamAutoPlayMode,
            regexPattern = settings.streamAutoPlayRegex,
            rankedRegexPatterns = settings.streamAutoPlayRankedRegex,
            source = settings.streamAutoPlaySource,
            installedAddonNames = installed,
            selectedAddons = settings.streamAutoPlaySelectedAddons,
            selectedPlugins = settings.streamAutoPlaySelectedPlugins,
            preferredBingeGroup = bingeGroup,
            preferBingeGroupInSelection = bingeGroup != null,
            smartExpected = com.nuvio.tv.core.streams.smart.SmartTitleFacts.expected(contentId, season, episode),
        ) }
        return chosen?.let { ReadyPick.info(it) } ?: ReadyPick.choice(all)
    }
}
