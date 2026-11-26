package com.nuvio.tv.fork.sources

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.media3.common.Player
import com.nuvio.tv.R
import com.nuvio.tv.core.streams.smart.ReleaseParser
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.Subtitle
import com.nuvio.tv.fork.ai.subtitles.DocCue
import com.nuvio.tv.fork.ai.subtitles.MkvSubtitleText
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import com.nuvio.tv.ui.screens.player.PlayerRuntimeController
import com.nuvio.tv.ui.screens.player.PlayerSubtitleUtils
import com.nuvio.tv.ui.screens.player.currentPlaybackDurationMs
import com.nuvio.tv.ui.screens.player.isUsingMpvEngine
import com.nuvio.tv.ui.screens.player.selectAddonSubtitle
import com.nuvio.tv.ui.screens.player.tryAutoSelectPreferredSubtitleFromAvailableTracks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs

/** Caches indexed Polish text tracks. A cache entry belongs to a single movie or episode and edition. */
internal object EmbeddedSourceSubtitles {
    private const val TAG = "ForkEmbeddedSubs"
    private const val DIR = "embedded-source-subtitles"
    private const val MAX_FILES = 64
    private const val MAX_CUES = 2_500
    private const val DURATION_TOLERANCE_MS = 1_000L
    private const val EXTRACTION_BUDGET_MS = 90_000L
    private const val ID_PREFIX = "embedded-source:"
    private val cacheName = Regex("""([0-9a-f]{24})--(\d+)--([A-Z_]+|standard)--([0-9a-f]{24})\.srt""")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val extractionGate = Semaphore(1)
    private val offered = LruCache<String, Boolean>(96)
    private val ready = MutableStateFlow(0)
    @Volatile private var catalog = emptyList<CachedTrack>()

    private data class CachedTrack(
        val key: String, val durationMs: Long, val edition: String, val donor: String, val file: File
    )

    fun enabled(context: Context): Boolean = ForkFeatures.enabled(context, "fork_detail_preload", default = true)

    fun offer(context: Context, type: String, videoId: String, stream: Stream) {
        if (!enabled(context)) return
        val url = stream.getStreamUrl() ?: return
        val app = context.applicationContext
        val key = titleKey(type, videoId)
        val donor = digest(url)
        val offerKey = "$key/$donor"
        synchronized(offered) {
            if (offered.get(offerKey) == true) return
            offered.put(offerKey, true)
        }
        scope.launch {
            extractionGate.withPermit {
                try {
                    cacheDonor(app, key, donor, stream, url)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "cache extraction failed: ${e.javaClass.simpleName}")
                }
            }
        }
    }

    private suspend fun cacheDonor(context: Context, key: String, donor: String, stream: Stream, url: String) {
        refreshCatalog(context)
        val forTitle = catalog.filter { it.key == key }
        if (forTitle.size >= 2 || forTitle.any { it.donor == donor }) return
        val cut = edition(stream.behaviorHints?.filename, stream.title, stream.description, stream.name)
        val outcome = withTimeoutOrNull(EXTRACTION_BUDGET_MS) {
            val mkv = MkvSubtitleText.open(url, stream.behaviorHints?.proxyHeaders?.request.orEmpty())
                ?: return@withTimeoutOrNull false
            val duration = mkv.durationMs ?: return@withTimeoutOrNull false
            if (forTitle.any { compatible(it, duration, cut) }) return@withTimeoutOrNull true
            val track = mkv.tracks.asSequence()
                .filter { it.lang2 == "pl" && !it.forced && it.cues.size <= MAX_CUES }
                .sortedWith(compareBy({ it.hearingImpaired }, { -it.cues.size }))
                .firstOrNull() ?: return@withTimeoutOrNull false
            // Test VOD JMGO 26.11: serwer odpowiadał HTTP 416 na każdą kwestię, a pętla pytała dalej (1003 nieudane zapytania
            // w 78 s, w czasie startu filmu). Niepełna ścieżka i tak odpada, więc pierwsza nieudana paczka kończy pobieranie.
            val cues = ArrayList<DocCue?>(track.cues.size)
            for (batch in track.cues.indices.chunked(4)) {
                val got = coroutineScope {
                    batch.map { i ->
                        async {
                            mkv.fetchCue(track, track.cues[i], track.cues.getOrNull(i + 1)?.timeMs)
                                ?.takeIf { it.endMs > it.startMs && it.text.isNotBlank() }
                        }
                    }.awaitAll()
                }
                cues += got
                if (got.any { it == null }) {
                    Log.i(TAG, "cue fetch failed at ${cues.size}/${track.cues.size}: stop (incomplete track is not cached)")
                    break
                }
            }
            // Incomplete tracks stay out of automatic selection.
            if (cues.size < track.cues.size || cues.any { it == null }) return@withTimeoutOrNull false
            writeCache(context, "$key--$duration--$cut--$donor.srt", cues.filterNotNull())
            Log.i(TAG, "cached Polish text cues=${cues.size} durationMs=$duration")
            true
        }
        if (outcome != true) Log.i(TAG, "no complete indexed Polish text track cached")
    }

    private fun writeCache(context: Context, name: String, cues: List<DocCue>) {
        val dir = File(context.cacheDir, DIR).also { it.mkdirs() }
        val tmp = File.createTempFile("track-", ".tmp", dir)
        try {
            tmp.bufferedWriter().use { writer ->
                cues.forEachIndexed { index, cue ->
                    writer.appendLine((index + 1).toString())
                    writer.appendLine("${srtTime(cue.startMs)} --> ${srtTime(cue.endMs)}")
                    writer.appendLine(cue.text).appendLine()
                }
            }
            check(tmp.renameTo(File(dir, name))) { "Cannot publish cached subtitle" }
            dir.listFiles()?.filter { cacheName.matches(it.name) }
                ?.sortedByDescending { it.lastModified() }?.drop(MAX_FILES)?.forEach { it.delete() }
            refreshCatalog(context)
        } finally {
            tmp.delete()
        }
    }

    @Synchronized
    private fun refreshCatalog(context: Context) {
        catalog = File(context.cacheDir, DIR).listFiles().orEmpty().mapNotNull { file ->
            val match = cacheName.matchEntire(file.name) ?: return@mapNotNull null
            val (key, duration, cut, donor) = match.destructured
            CachedTrack(key, duration.toLongOrNull() ?: return@mapNotNull null, cut, donor, file)
        }
        ready.update { it + 1 }
    }

    fun observe(player: PlayerRuntimeController) {
        if (!enabled(player.context)) return
        player.scope.launch {
            withContext(Dispatchers.IO) { refreshCatalog(player.context) }
            ready.collect {
                player.tryAutoSelectPreferredSubtitleFromAvailableTracks()
            }
        }
    }

    fun select(player: PlayerRuntimeController, primaryTarget: String?, internalIndex: Int): Boolean {
        if (TrackScout.lang(primaryTarget) != "pl" || !player.hasScannedTextTracksOnce) return false
        if (!player.isUsingMpvEngine() && player._exoPlayer?.playbackState != Player.STATE_READY) return false
        val state = player._uiState.value
        val internal = state.subtitleTracks.getOrNull(internalIndex)
        if (internal != null && PlayerSubtitleUtils.matchesLanguageCode(internal.language, "pl")) return false
        val cached = matching(player) ?: return false
        if (state.addonSubtitles.none { it.id == cached.id }) {
            player._uiState.update { it.copy(addonSubtitles = listOf(cached) + it.addonSubtitles) }
        }
        player.autoSubtitleSelected = true
        if (state.selectedAddonSubtitle?.id != cached.id) {
            player.selectAddonSubtitle(cached)
            Log.i(TAG, "selected cached Polish text from matching edition")
        }
        return true
    }

    private fun matching(player: PlayerRuntimeController): Subtitle? {
        val type = player.contentType ?: return null
        val id = player.currentVideoId ?: player.contentId ?: return null
        if (!enabled(player.context)) return null
        val duration = player.currentPlaybackDurationMs()
        if (duration <= 0L) return null
        val key = titleKey(type, id)
        val state = player._uiState.value
        val cut = edition(player.currentFilename, state.currentStreamName, state.currentStreamDescription)
        val cached = catalog.firstOrNull { it.key == key && compatible(it, duration, cut) } ?: return null
        return Subtitle(
            id = "$ID_PREFIX${cached.file.name}",
            url = Uri.fromFile(cached.file).toString(),
            lang = "pl",
            addonName = player.context.getString(R.string.embedded_source_subtitles_label),
            addonLogo = null,
            isStreamProvided = true
        )
    }

    /** Only our generated cache files can be read through the local subtitle path. Runs on IO. */
    fun read(context: Context, url: String): String? {
        if (!url.startsWith("file:")) return null
        val path = Uri.parse(url).path ?: return null
        val file = File(path).canonicalFile
        val dir = File(context.cacheDir, DIR).canonicalFile
        if (file.parentFile != dir || !cacheName.matches(file.name)) return null
        return file.readText(Charsets.UTF_8)
    }

    private fun compatible(track: CachedTrack, durationMs: Long, cut: String): Boolean =
        track.edition == cut && abs(track.durationMs - durationMs) <= DURATION_TOLERANCE_MS

    private fun titleKey(type: String, id: String): String = digest("${type.lowercase(Locale.ROOT)}|$id")
    private fun edition(vararg labels: String?): String =
        ReleaseParser.parse(labels.filterNotNull().joinToString(" ")).edition?.name ?: "standard"

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).take(12)
            .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private fun srtTime(ms: Long): String = String.format(
        Locale.ROOT, "%02d:%02d:%02d,%03d", ms / 3_600_000, ms / 60_000 % 60, ms / 1_000 % 60, ms % 1_000
    )
}
