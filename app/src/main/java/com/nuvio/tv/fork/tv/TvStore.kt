package com.nuvio.tv.fork.tv

import android.content.Context
import android.util.Log
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * The TV tab's memory: the 15 recently viewed shows and the alerts for new episodes found in the
 * guide. Kept apart from the Live TV engine (only a few calls reach in), per profile, in
 * `files/forge_tv/`. Everything here is best effort: a failed read or write loses a list, never playback.
 */
object ForgeTv {
    private const val TAG = "TvStore"
    private const val MAX_AIRINGS = 6_000
    private const val MAX_ANNOUNCED = 400
    private val json = Json { ignoreUnknownKeys = true }
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    private val _recent = MutableStateFlow<List<RecentShow>>(emptyList())
    val recent: StateFlow<List<RecentShow>> = _recent.asStateFlow()
    private val _recentChannels = MutableStateFlow<List<RecentChannel>>(emptyList())
    /** The 15 channels watched last, newest first. */
    val recentChannels: StateFlow<List<RecentChannel>> = _recentChannels.asStateFlow()
    private val _alerts = MutableStateFlow<List<EpisodeAlert>>(emptyList())
    /** Every alert the guide gives now, announced or not. */
    val alerts: StateFlow<List<EpisodeAlert>> = _alerts.asStateFlow()
    private val _unannounced = MutableStateFlow<List<EpisodeAlert>>(emptyList())
    /** The alerts not yet announced: what a badge or a toast shows. */
    val unannounced: StateFlow<List<EpisodeAlert>> = _unannounced.asStateFlow()

    private var dir: File? = null
    private var profileId = Int.MIN_VALUE
    private var storedAirings: List<Airing> = emptyList()
    private var announced: Set<String> = emptySet()
    private var channelsByKey: Map<String, LiveTvChannel> = emptyMap()
    private var keptAirings: List<Airing> = emptyList()

    /** Loads the lists of [profileId] (once per profile). */
    fun attach(context: Context, profileId: Int) = synchronized(lock) {
        if (this.profileId == profileId && dir != null) return@synchronized
        this.profileId = profileId
        val folder = File(context.applicationContext.filesDir, "fork_tv").also { it.mkdirs() }
        dir = folder
        _recent.value = RecentlyViewed.decode(read(folder, "recent_$profileId.json"))
        _recentChannels.value = RecentChannels.decode(read(folder, "recent_channels_$profileId.json"))
        storedAirings = runCatching {
            json.decodeFromString(ListSerializer(Airing.serializer()), read(folder, "airings_$profileId.json").orEmpty())
        }.getOrDefault(emptyList())
        announced = runCatching {
            json.decodeFromString(SetSerializer(String.serializer()), read(folder, "announced_$profileId.json").orEmpty())
        }.getOrDefault(emptySet())
        recompute(System.currentTimeMillis())
    }

    /** Titles (as [ShowIdentity.normalizeTitle]) the guide read should collect every airing of. */
    fun watchedKeys(): Set<String> = _recent.value.mapTo(HashSet()) { it.key }

    /** The viewer chose to watch [programme] on [channel]: it goes first in the list. */
    fun onWatched(channel: LiveTvChannel, programme: LiveTvProgramme?, nowMs: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            val entry = RecentChannel(
                streamUrl = channel.streamUrl, name = channel.name, logoUrl = channel.logoUrl, group = channel.group,
                tvgId = channel.tvgId, guideKey = channel.guideKey, lastViewedMs = nowMs,
            )
            _recentChannels.value = RecentChannels.record(_recentChannels.value, entry)
            save("recent_channels_$profileId.json", RecentChannels.encode(_recentChannels.value))
        }
        programme ?: return
        val key = ShowIdentity.normalizeTitle(programme.title)
        if (key.isEmpty()) return
        synchronized(lock) {
            val episode = ShowIdentity.episodeId(programme.title, programme.description)
            val show = RecentShow(
                key = key, title = programme.title.trim(), channelName = channel.name, channelUrl = channel.streamUrl,
                guideKey = channel.guideKey, logoUrl = channel.logoUrl, imageUrl = programme.image,
                lastViewedMs = nowMs, seenEpisodes = listOfNotNull(episode.takeIf { it.isNotEmpty() }),
            )
            _recent.value = RecentlyViewed.record(_recent.value, show)
            save("recent_$profileId.json", RecentlyViewed.encode(_recent.value))
            recompute(nowMs)
        }
    }

    /** "Remove from recent channels". */
    fun removeChannel(streamUrl: String) = synchronized(lock) {
        _recentChannels.value = RecentChannels.remove(_recentChannels.value, streamUrl)
        save("recent_channels_$profileId.json", RecentChannels.encode(_recentChannels.value))
    }

    /** "Remove from Recently Viewed". */
    fun remove(key: String) = synchronized(lock) {
        _recent.value = RecentlyViewed.remove(_recent.value, key)
        save("recent_$profileId.json", RecentlyViewed.encode(_recent.value))
        recompute(System.currentTimeMillis())
    }

    /** What a full guide read found for the watched shows (all of its days, not only the kept window). */
    fun storeAirings(airings: List<Airing>) = synchronized(lock) {
        storedAirings = airings.sortedBy { it.startMs }.takeLast(MAX_AIRINGS)
        save("airings_$profileId.json", json.encodeToString(ListSerializer(Airing.serializer()), storedAirings))
        recompute(System.currentTimeMillis())
    }

    /** The guide's kept programmes changed (each minute-ish tick or a read): looks for new episodes again. */
    fun updateSchedule(channels: List<LiveTvChannel>, kept: Map<String, List<LiveTvProgramme>>, nowMs: Long) = synchronized(lock) {
        val watched = watchedKeys()
        if (watched.isEmpty()) return@synchronized
        channelsByKey = channels.associateBy { it.guideKey }
        val titleKeys = HashMap<String, String>()
        keptAirings = kept.flatMap { (guideKey, list) ->
            list.mapNotNull { p ->
                val key = titleKeys.getOrPut(p.title) { ShowIdentity.normalizeTitle(p.title) }
                if (key in watched) Airing(guideKey, p.title, p.startEpochMs, p.stopEpochMs, p.description) else null
            }
        }
        recompute(nowMs)
    }

    /** The viewer has seen [ids] announced. */
    fun markAnnounced(ids: Collection<String>) = synchronized(lock) {
        if (ids.isEmpty()) return@synchronized
        announced = (announced + ids).toList().takeLast(MAX_ANNOUNCED).toSet()
        save("announced_$profileId.json", json.encodeToString(SetSerializer(String.serializer()), announced))
        _unannounced.value = EpisodeAlerts.fresh(_alerts.value, announced)
    }

    private fun recompute(nowMs: Long) {
        val all = (storedAirings + keptAirings).distinctBy { Triple(it.guideKey, it.startMs, it.title) }
        val found = EpisodeAlerts.detect(
            shows = _recent.value,
            airings = all,
            channels = { key -> channelsByKey[key]?.let { AlertChannel(it.name, it.catchup?.days ?: 0) } },
            nowMs = nowMs,
        )
        _alerts.value = found
        _unannounced.value = EpisodeAlerts.fresh(found, announced)
    }

    private fun read(folder: File, name: String): String? = runCatching { File(folder, name).takeIf { it.isFile }?.readText() }.getOrNull()

    private fun save(name: String, text: String) {
        val folder = dir ?: return
        io.launch {
            runCatching {
                val temp = File(folder, "$name.tmp")
                temp.writeText(text)
                if (!temp.renameTo(File(folder, name))) {
                    File(folder, name).delete()
                    temp.renameTo(File(folder, name))
                }
            }.onFailure { Log.w(TAG, "Could not save $name", it) }
        }
    }
}
