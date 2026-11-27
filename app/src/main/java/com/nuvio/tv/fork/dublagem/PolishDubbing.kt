package com.nuvio.tv.fork.dublagem

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.FilteringMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import com.nuvio.app.features.player.DubbingAnalysisSource
import com.nuvio.app.features.player.DubbingAudioSyncAnalyzer
import com.nuvio.app.features.player.DubbingAudioTrackHint
import com.nuvio.app.features.player.DubbingSyncConfidence
import com.nuvio.app.features.player.DubbingSyncRequest
import com.nuvio.app.features.player.DubbingSyncResult
import com.nuvio.tv.R
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.core.stream.ReleaseFactsParser
import com.nuvio.tv.core.streams.smart.ReleaseParser
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.fork.ForkLog
import com.nuvio.tv.fork.sources.PlayerBridge
import com.nuvio.tv.fork.sources.TrackScout
import com.nuvio.tv.ui.screens.player.DubbingOffsetMediaSource
import com.nuvio.tv.ui.screens.player.DubbingRelaySource
import com.nuvio.tv.ui.screens.player.DubbingSourceRelay
import com.nuvio.tv.ui.screens.player.PlayerMediaSourceFactory
import com.nuvio.tv.ui.screens.player.PlayerRuntimeController
import com.nuvio.tv.ui.screens.player.ignoreLocalBuffering
import com.nuvio.tv.ui.screens.player.isUsingMpvEngine
import com.nuvio.tv.ui.screens.player.isSourceSwitching
import com.nuvio.tv.ui.screens.player.rejectSourceSwitchAttempt
import com.nuvio.tv.ui.screens.player.sourceSwitchOrigin
import com.nuvio.tv.ui.screens.player.switchSourceForAudio
import kotlinx.coroutines.flow.update
import com.nuvio.tv.ui.screens.player.subtitleRoutes
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.TimeUnit

/** Automatic selection policy around the upstream Dublagem clocks, FFT and range relay. */
@OptIn(UnstableApi::class)
internal object PolishDubbing {
    const val KEY = "fork_polish_dubbing"
    private const val TAG = "ForkDublagem"
    private const val WINDOW_MS = 8_000L
    private const val SESSION_BUDGET_MS = 115_000L
    private const val MATCH_BUDGET_MS = 68_000L
    private const val MIN_BUFFER_MS = 4_000L
    private const val DONOR_PROBE_BUDGET_MS = 8_000L
    private const val RADIUS_MS = 3_000L
    private const val MAX_DONORS = 3

    // Accessed on the player thread. A weak player reference also avoids retaining listeners.
    private val sessions = WeakHashMap<PlayerRuntimeController, Session>()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
        .build()

    private class Session(
        val type: String,
        val videoId: String,
        val url: String,
        val player: WeakReference<ExoPlayer>,
    ) {
        var collecting = true
        var manualAudio = false
        var job: Job? = null
        var analyzer: DubbingAudioSyncAnalyzer? = null
        var binding: Binding? = null
        /** Prośba z menu dźwięku (zgłoszenie 27.11): gdy dołączenie się nie uda — wydanie z polskim dźwiękiem (Echo, sprawdzane). */
        var menuOption: com.nuvio.tv.core.streams.AudioMenuOption? = null
    }

    private data class Donor(val stream: Stream, val info: TrackScout.Tracks, val audio: DubbingAudioTrack)

    private data class Binding(
        val donor: Donor,
        val relay: DubbingSourceRelay,
        val audioUrl: String,
        val offsetMs: Long,
        val preferences: TrackSelectionParameters,
        var confirmed: Boolean = false,
    )

    private data class ExoAudio(val group: Tracks.Group, val index: Int, val track: DubbingAudioTrack)

    fun enabled(context: Context): Boolean = ForkFeatures.enabled(context, KEY, default = true)

    fun supportsCurrentSource(owner: PlayerRuntimeController): Boolean {
        if (!enabled(owner.context) || owner.isUsingMpvEngine() || owner._uiState.value.isTorrentStream) return false
        if (owner.contentType != "movie" && owner.contentType != "series") return false
        val url = owner.currentStreamUrl
        return isHttp(url) &&
            Util.inferContentTypeForUriAndMimeType(Uri.parse(url), owner.currentStreamMimeType) == C.CONTENT_TYPE_OTHER
    }

    fun onPlaying(owner: PlayerRuntimeController) {
        if (!supportsCurrentSource(owner)) return
        val exo = owner._exoPlayer ?: return
        if (!exo.isPlaying || !exo.isCurrentMediaItemSeekable) return
        start(owner, exo, restart = false)
    }

    /**
     * Zgłoszenie właściciela 27.11 („The Social Reckoning”): menu dźwięku → „Polski”, którego nie ma w pliku, przełączało
     * źródło (Echo) — skoki po wydaniach i na końcu 1080p bez polskiego. Teraz: polski dźwięk z innego wydania dołączony
     * do obecnego obrazu (zsynchronizowany). true = obsłużone (bez przełączania źródła). Inne języki i źródła, których
     * Dublagem nie obsługuje (mpv, torrent, HLS) — dalej przełączanie źródła, ale tylko na plik, który ma ten język
     * ([onSwitchTracks]).
     */
    fun onAudioMenuLanguage(owner: PlayerRuntimeController, option: com.nuvio.tv.core.streams.AudioMenuOption): Boolean {
        if (!enabled(owner.context)) return false // wyłączony Dublagem: zachowanie Echo bez zmian
        val lang = option.language
        if (option.trackIndex != null) return false
        wantedLanguage[owner] = lang
        if (lang?.code != "pl") return false
        if (!supportsCurrentSource(owner)) { Log.i(TAG, "menu: Polish asked, source not supported here → source switch"); return false }
        val exo = owner._exoPlayer ?: return false
        if (!exo.isCurrentMediaItemSeekable) return false
        owner._uiState.update { it.copy(showAudioOverlay = false) }
        val session = sessions[owner]
        val url = owner.currentStreamUrl
        val c = owner.context
        when {
            session != null && session.url == url && session.binding != null -> {
                val polish = audioTracks(exo.currentTracks).firstOrNull { it.group.mediaTrackGroup.id.startsWith("1:") && it.track.isPolish }
                Log.i(TAG, "menu: Polish asked, donor already joined → select (found=${polish != null})")
                session.manualAudio = false
                polish?.let { chooseTrack(exo, it) }
                ForkLog.say(c, TAG, c.getString(R.string.dublagem_menu_joined))
            }
            session != null && session.url == url && session.job?.isActive == true && !session.manualAudio -> {
                session.menuOption = option
                Log.i(TAG, "menu: Polish asked, donor search running → wait")
                ForkLog.say(c, TAG, c.getString(R.string.dublagem_menu_searching))
            }
            else -> {
                Log.i(TAG, "menu: Polish asked → donor search from the menu")
                ForkLog.say(c, TAG, c.getString(R.string.dublagem_menu_searching))
                start(owner, exo, restart = true)
                sessions[owner]?.menuOption = option
            }
        }
        return true
    }

    /** Język wybrany w menu dźwięku, gdy szło przełączanie źródła (sprawdzany przy ścieżkach nowego pliku). */
    private val wantedLanguage = WeakHashMap<PlayerRuntimeController, com.nuvio.tv.core.streams.AudioLanguage?>()

    /**
     * Zgłoszenie 27.11: przełączanie źródła z menu dźwięku przyjmowało plik BEZ wybranego języka („new source has no pl
     * track”) i na nim zostawało. Teraz taki plik = nieudana próba → następne źródło (albo powrót do poprzedniego).
     */
    private fun onSwitchTracks(owner: PlayerRuntimeController, tracks: Tracks) {
        val lang = wantedLanguage[owner] ?: return
        if (!owner.isSourceSwitching()) { if (owner.currentStreamUrl.isNotBlank()) wantedLanguage.remove(owner); return }
        val (origin, _, _) = owner.sourceSwitchOrigin()
        if (origin?.getStreamUrl() == owner.currentStreamUrl) return
        val audio = audioTracks(tracks)
        if (audio.isEmpty()) return
        val has = audio.any { com.nuvio.tv.core.streams.StreamAudioParser.trackLanguage(it.track.language, it.track.label) == lang }
        if (has) return
        Log.w(TAG, "source switch: new file has no ${lang.code} track (${audio.size} tracks) → next source")
        owner.rejectSourceSwitchAttempt()
    }

    private fun start(owner: PlayerRuntimeController, exo: ExoPlayer, restart: Boolean) {
        val type = owner.contentType ?: return
        val videoId = owner.currentVideoId ?: owner.contentId ?: return
        val url = owner.currentStreamUrl
        if (!restart && sessions[owner]?.let { it.videoId == videoId && it.url == url && it.player.get() === exo } == true) return
        release(owner)
        val session = Session(type, videoId, url, WeakReference(exo))
        sessions[owner] = session
        session.job = owner.scope.launch {
            try {
                val completed = withTimeoutOrNull(SESSION_BUDGET_MS) {
                    attachBest(owner, session, exo)
                    true
                }
                if (completed != true) {
                    Log.i(TAG, "audio search exceeded the background budget")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "selection failed: ${e.javaClass.simpleName}")
            } finally {
                session.job = null
                session.analyzer = null
                session.collecting = false
                val asked = session.menuOption
                session.menuOption = null
                if (asked != null && isCurrent(owner, session) && session.binding == null) {
                    Log.i(TAG, "menu: Polish could not be joined to this picture → release with Polish audio (checked)")
                    ForkLog.say(owner.context, TAG, owner.context.getString(R.string.dublagem_menu_failed))
                    owner.switchSourceForAudio(asked)
                }
                if (isCurrent(owner, session) && !session.manualAudio && session.binding == null) {
                    PlayerBridge.maybeAutoLektor(owner.context)
                }
            }
        }
    }

    fun awaitingAudio(url: String?): Boolean = url != null && sessions.values.any {
        it.url == url && (it.collecting || it.binding?.confirmed == false)
    }

    fun onManualSelection(owner: PlayerRuntimeController) {
        val session = sessions[owner] ?: return
        session.manualAudio = true
        session.collecting = false
        session.analyzer?.cancelActiveAnalysis()
        session.job?.cancel()
    }

    fun release(owner: PlayerRuntimeController) {
        val session = sessions.remove(owner) ?: return
        session.analyzer?.cancelActiveAnalysis()
        session.job?.cancel()
        session.binding?.relay?.let(::closeRelay)
    }

    private suspend fun attachBest(owner: PlayerRuntimeController, session: Session, exo: ExoPlayer) {
        val duration = exo.duration
        val edition = DubbingEdition(
            releaseEdition(owner.currentFilename, owner._uiState.value.currentStreamName, owner._uiState.value.currentStreamDescription),
            duration,
            exo.videoFormat?.frameRate?.takeIf { it > 0 },
        )
        val tracks = audioTracks(exo.currentTracks)
        val reference = tracks.firstOrNull { it.group.isTrackSelected(it.index) } ?: return
        val internal = tracks.filter { it.track.isPolish && it.track.isMainProgram && it.group.isTrackSupported(it.index) }
            .maxWithOrNull(compareBy(DubbingAudioTrack.quality) { it.track })
        if (internal != null) chooseTrack(exo, internal)
        if (duration < 48_000L) return
        val candidates = findDonors(owner, session, edition, internal?.track)
        Log.i(TAG, "donors with Polish audio: ${candidates.size} (internal Polish=${internal != null}, durationS=${duration / 1000})")
        if (candidates.isEmpty() || !isCurrent(owner, session)) return
        val primary = PlayerMediaSourceFactory.normalizePlaybackRequest(session.url, owner.currentHeaders)
        val analyzer = DubbingAudioSyncAnalyzer(minimumWindowMs = WINDOW_MS)
        session.analyzer = analyzer
        var waitedForBuffer = false
        for (donor in candidates.take(MAX_DONORS)) {
            // Heavy remote files often build a useful buffer just after the first few seconds.
            if (!exo.isPlaying || exo.totalBufferedDuration < MIN_BUFFER_MS) {
                if (!waitedForBuffer) Log.i(TAG, "waiting for playback buffer before comparing audio")
                waitedForBuffer = true
                while (isCurrent(owner, session) && !session.manualAudio &&
                    (!exo.isPlaying || exo.totalBufferedDuration < MIN_BUFFER_MS)
                ) delay(400L)
            }
            if (!isCurrent(owner, session) || session.manualAudio) return
            // Shared original narration/music is a better reference than comparing
            // two dubs. The donor still plays the independently ranked Polish track.
            val comparison = sharedComparisonTracks(tracks.map { it.track }, donor.info.audioDetails)
            val aTrack = comparison?.first ?: reference.track
            val bTrack = comparison?.second ?: donor.audio
            val a = analysisSource(primary.url, primary.headers, aTrack, duration, "A")
            val url = donor.stream.getStreamUrl() ?: continue
            val proxyHeaders = donor.stream.behaviorHints?.proxyHeaders
            val request = PlayerMediaSourceFactory.normalizePlaybackRequest(url, proxyHeaders?.request)
            val relay = DubbingSourceRelay(DubbingRelaySource(request.url, request.headers, proxyHeaders?.response.orEmpty()), httpClient)
            var attached = false
            try {
                val endpoints = withContext(Dispatchers.IO) { relay.startRelay() }
                val b = analysisSource(endpoints.analysisUrl, emptyMap(), bTrack, donor.info.durationMs, "B")
                val start = exo.currentPosition.coerceIn(3_000L, duration / 3)
                Log.i(TAG, "checking donor after bufferMs=${exo.totalBufferedDuration} sharedLanguage=${comparison != null}")
                val offset = withTimeoutOrNull(MATCH_BUDGET_MS) { matchOffset(analyzer, a, b, start, duration) }
                if (offset == null) {
                    Log.i(TAG, "donor rejected: synchronization could not be confirmed")
                    continue
                }
                if (!isCurrent(owner, session) || session.manualAudio) return
                val binding = Binding(donor, relay, endpoints.mainPlayerUrl, offset, exo.trackSelectionParameters)
                session.binding = binding
                attached = bind(owner, exo, binding)
                if (attached) return
                session.binding = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                session.binding = null
                Log.w(TAG, "donor initialization failed: ${e.javaClass.simpleName}")
            } finally {
                if (!attached) closeRelay(relay)
            }
        }
    }

    private suspend fun findDonors(
        owner: PlayerRuntimeController, session: Session, edition: DubbingEdition, internal: DubbingAudioTrack?,
    ): List<Donor> = withContext(Dispatchers.IO) {
        fun shortlist() = TrackScout.candidates(session.type, session.videoId)
        if (shortlist().isEmpty()) {
            withTimeoutOrNull(3_000L) {
                owner.streamRepository.getStreamsFromAllAddons(session.type, session.videoId).collect { result ->
                    if (result is NetworkResult.Success) {
                        TrackScout.onSources(owner.context, session.type, session.videoId, result.data.flatMap { it.streams })
                    }
                }
            }
        }
        withTimeoutOrNull(DONOR_PROBE_BUDGET_MS) {
            delay(450L) // the shared probe coalesces rapid source-list updates
            while (shortlist().any(TrackScout::probing)) delay(200L)
        }
        val decoderSupport = mutableMapOf<DubbingAudioTrack, Boolean>()
        val why = HashMap<String, Int>()
        fun skip(r: String) { why[r] = (why[r] ?: 0) + 1 }
        shortlist().flatMap { stream ->
            val url = stream.getStreamUrl() ?: return@flatMap emptyList<Donor>().also { skip("no_url") }
            val info = TrackScout.of(stream) ?: return@flatMap emptyList<Donor>().also { skip("not_probed") }
            val donorEdition = DubbingEdition(
                releaseEdition(stream.behaviorHints?.filename, stream.title, stream.name, stream.description),
                info.durationMs, info.frameRate,
            )
            if (url == session.url) return@flatMap emptyList<Donor>().also { skip("same_file") }
            if (!info.byteRanges) return@flatMap emptyList<Donor>().also { skip("no_byte_ranges") }
            if (!edition.matches(donorEdition)) return@flatMap emptyList<Donor>().also { skip("other_edition_or_length") }
            if (info.audioDetails.none { it.isPolish }) skip("no_polish_track")
            info.audioDetails.filter { track ->
                track.isPolish && track.isMainProgram &&
                    (internal == null || DubbingAudioTrack.quality.compare(track, internal) > 0) &&
                    decoderSupport.getOrPut(track) { canPlay(owner.context, track) }
            }.map { Donor(stream, info, it) }
        }.sortedWith(compareByDescending<Donor, DubbingAudioTrack>(DubbingAudioTrack.quality) { it.audio }
            .thenBy { it.stream.behaviorHints?.videoSize ?: ReleaseFactsParser.factsFor(it.stream).size ?: Long.MAX_VALUE })
            .also { Log.i(TAG, "donor shortlist=${shortlist().size} kept=${it.size} skipped=$why") }
    }

    private fun sharedComparisonTracks(
        primary: List<DubbingAudioTrack>,
        secondary: List<DubbingAudioTrack>,
    ): Pair<DubbingAudioTrack, DubbingAudioTrack>? {
        val secondaryByLanguage = secondary.filter { it.isMainProgram && it.mime != null }
            .groupBy { TrackScout.lang(it.language) }
        return primary.asSequence().filter { it.isMainProgram && it.mime != null }
            .flatMap { a ->
                val language = TrackScout.lang(a.language)
                if (language == "und") emptySequence()
                else secondaryByLanguage[language].orEmpty().asSequence().map { b -> a to b }
            }
            .maxWithOrNull(compareBy<Pair<DubbingAudioTrack, DubbingAudioTrack>>(
                { TrackScout.lang(it.first.language) == "en" },
                { it.first.mime == it.second.mime },
                { it.first.channels == it.second.channels },
                { it.first.mime in comparisonCodecs && it.second.mime in comparisonCodecs },
            ))
    }

    private val comparisonCodecs = setOf("audio/ac3", "audio/eac3", "audio/mp4a-latm")

    private fun canPlay(context: Context, track: DubbingAudioTrack): Boolean {
        val mime = track.mime ?: return false
        if (FfmpegLibrary.supportsFormat(mime)) return true
        val platform = MediaFormat().apply {
            setString(MediaFormat.KEY_MIME, mime)
            if (track.channels > 0) setInteger(MediaFormat.KEY_CHANNEL_COUNT, track.channels)
            if (track.sampleRate > 0) setInteger(MediaFormat.KEY_SAMPLE_RATE, track.sampleRate)
        }
        return MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(platform) != null ||
            AudioCapabilities.getCapabilities(context).isPassthroughPlaybackSupported(
                Format.Builder().setSampleMimeType(mime).setChannelCount(track.channels).setSampleRate(track.sampleRate).build()
            )
    }

    private suspend fun matchOffset(
        analyzer: DubbingAudioSyncAnalyzer,
        a: DubbingAnalysisSource,
        b: DubbingAnalysisSource,
        start: Long,
        durationMs: Long,
    ): Long? {
        suspend fun offset(at: Long): Long? =
            (analyzer.analyze(a, b, DubbingSyncRequest(at, windowDurationMs = WINDOW_MS, searchRadiusMs = RADIUS_MS)) {} as? DubbingSyncResult.Match)
                ?.takeIf { it.confidence == DubbingSyncConfidence.HIGH }?.offsetMs
        val first = offset(start) ?: return null
        val confirmAt = maxOf(start + WINDOW_MS + 2_000L, durationMs * 2 / 3)
            .coerceAtMost(durationMs - WINDOW_MS - RADIUS_MS)
        if (confirmAt < start + WINDOW_MS) return null
        val second = offset(confirmAt) ?: return null
        return matchingDubbingOffset(first, second)?.also {
            Log.i(TAG, "synchronized two samples offsetMs=$it confirmationMs=$second")
        }
    }

    private fun analysisSource(
        url: String, headers: Map<String, String>, track: DubbingAudioTrack, durationMs: Long?, label: String,
    ) = DubbingAnalysisSource(
        url = url, requestHeaders = headers, audioTrackOrdinal = track.ordinal,
        audioTrackHint = DubbingAudioTrackHint(track.language, track.mime, track.channels.takeIf { it > 0 }),
        sourceLabel = label, expectedDurationMs = durationMs,
        dataSourceFactory = OkHttpDataSource.Factory(httpClient).setDefaultRequestProperties(headers)::createDataSource,
        dataSourceRequestHeaders = emptyMap(), allowSlowFallbacks = false,
    )

    private fun bind(owner: PlayerRuntimeController, exo: ExoPlayer, binding: Binding): Boolean {
        if (owner._exoPlayer !== exo) return false
        val mergedVideo = FilteringMediaSource(
            videoSource(owner, exo, includeAudio = false),
            setOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_TEXT, C.TRACK_TYPE_IMAGE, C.TRACK_TYPE_METADATA),
        )
        val donor = DefaultMediaSourceFactory(OkHttpDataSource.Factory(httpClient))
            .createMediaSource(MediaItem.fromUri(binding.audioUrl))
        val mergedAudio = DubbingOffsetMediaSource(FilteringMediaSource(donor, C.TRACK_TYPE_AUDIO)) { binding.offsetMs }
        owner.ignoreLocalBuffering("polish_audio_attach", 8_000L)
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO).setPreferredAudioLanguages("pl", "pol").build()
        val position = exo.currentPosition.coerceAtLeast(0L)
        val play = exo.playWhenReady
        exo.setMediaSource(MergingMediaSource(true, false, mergedVideo, mergedAudio), position)
        exo.prepare()
        exo.playWhenReady = play
        Log.i(TAG, "joining video and Polish audio channels=${binding.donor.audio.channels} codec=${binding.donor.audio.mime} positionMs=$position")
        return true
    }

    private fun videoSource(owner: PlayerRuntimeController, exo: ExoPlayer, includeAudio: Boolean) =
        owner.mediaSourceFactory.createMediaSource(
            context = owner.context, url = owner.currentStreamUrl, headers = owner.currentHeaders,
            subtitleConfigurations = exo.currentMediaItem?.localConfiguration?.subtitleConfigurations.orEmpty(),
            subtitleRoutes = owner.subtitleRoutes(owner._uiState.value.addonSubtitles),
            filename = owner.currentFilename, responseHeaders = owner.currentStreamResponseHeaders,
            mimeTypeOverride = owner.currentStreamMimeType,
            audioDelayUsProvider = if (includeAudio) owner.audioDelayUs::get else null,
            mediaMetadata = exo.currentMediaItem?.mediaMetadata, cacheKey = owner.currentStreamCacheKey,
        )

    fun onTracks(owner: PlayerRuntimeController, tracks: Tracks) {
        runCatching { onSwitchTracks(owner, tracks) }.onFailure { Log.w(TAG, "onSwitchTracks: ${it.javaClass.simpleName}: ${it.message}") }
        val session = sessions[owner] ?: return
        val binding = session.binding ?: return
        val exo = owner._exoPlayer ?: return
        if (!isCurrent(owner, session) || session.manualAudio) return
        val candidates = audioTracks(tracks).filter { it.group.mediaTrackGroup.id.startsWith("1:") }
        if (candidates.isEmpty()) return
        val target = candidates.firstOrNull {
            val audio = it.track
            val wanted = binding.donor.audio
            audio.ordinal == wanted.ordinal && audio.isPolish && audio.isMainProgram &&
                (wanted.channels <= 0 || audio.channels == wanted.channels) && it.group.isTrackSupported(it.index)
        }
        if (target == null) {
            owner.scope.launch { if (isCurrent(owner, session)) recover(owner, "selected_track_missing") }
            return
        }
        if (!target.group.isTrackSelected(target.index)) {
            chooseTrack(exo, target)
            return
        }
        if (!binding.confirmed) {
            binding.confirmed = true
            Log.i(TAG, "Polish audio selected codec=${target.track.mime} channels=${target.track.channels} offsetMs=${binding.offsetMs}")
            ForkLog.say(owner.context, TAG, owner.context.getString(R.string.dublagem_audio_joined))
        }
    }

    fun onReady(owner: PlayerRuntimeController) {
        com.nuvio.tv.fork.sources.DurationGuard.onReady(owner) // przegląd 27.11, ekran 03: kontrola długości pliku (to samo wpięcie STATE_READY)
        val session = sessions[owner] ?: return
        if (session.binding == null || !isCurrent(owner, session)) return
        val exo = owner._exoPlayer ?: return
        if (audioTracks(exo.currentTracks).none { it.group.mediaTrackGroup.id.startsWith("1:") }) {
            owner.scope.launch { if (isCurrent(owner, session)) recover(owner, "audio_source_missing") }
        }
    }

    fun recover(owner: PlayerRuntimeController, reason: String): Boolean {
        val session = sessions[owner] ?: return false
        val binding = session.binding ?: return false
        val exo = owner._exoPlayer ?: return false
        if (!isCurrent(owner, session)) return false
        session.binding = null
        val source = videoSource(owner, exo, includeAudio = true)
        val position = exo.currentPosition.coerceAtLeast(0L)
        val play = exo.playWhenReady
        exo.trackSelectionParameters = binding.preferences
        owner.ignoreLocalBuffering("polish_audio_removed", 8_000L)
        exo.setMediaSource(source, position)
        exo.prepare()
        exo.playWhenReady = play
        closeRelay(binding.relay)
        Log.i(TAG, "original audio restored positionMs=$position reason=$reason")
        return true
    }

    private fun chooseTrack(exo: ExoPlayer, audio: ExoAudio) {
        if (audio.group.isTrackSelected(audio.index)) return
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(audio.group.mediaTrackGroup, audio.index)).build()
    }

    private fun audioTracks(tracks: Tracks): List<ExoAudio> = buildList {
        for (group in tracks.groups) {
            if (group.type != C.TRACK_TYPE_AUDIO) continue
            for (index in 0 until group.length) {
                val f = group.getTrackFormat(index)
                add(ExoAudio(group, index, DubbingAudioTrack(
                    size, f.language.orEmpty(), f.sampleMimeType, f.channelCount, f.bitrate, f.sampleRate, f.label,
                    (f.roleFlags and (C.ROLE_FLAG_COMMENTARY or C.ROLE_FLAG_DESCRIBES_VIDEO)) != 0,
                )))
            }
        }
    }

    private fun isCurrent(owner: PlayerRuntimeController, session: Session): Boolean =
        sessions[owner] === session && session.player.get() === owner._exoPlayer &&
            session.url == owner.currentStreamUrl && session.videoId == (owner.currentVideoId ?: owner.contentId)

    private fun releaseEdition(vararg labels: String?): String? =
        ReleaseParser.parse(labels.filterNotNull().joinToString(" ")).edition?.name

    private fun isHttp(url: String): Boolean = url.startsWith("http://", true) || url.startsWith("https://", true)

    private fun closeRelay(relay: DubbingSourceRelay) {
        // NanoHTTPD can wait for its connection threads. Never close it on the player thread.
        CLOSED_RELAYS.launch { relay.close() }
    }

    private val CLOSED_RELAYS = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
}
