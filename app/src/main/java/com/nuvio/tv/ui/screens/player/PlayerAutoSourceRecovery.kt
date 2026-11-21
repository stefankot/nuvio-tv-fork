package com.nuvio.tv.ui.screens.player

import android.os.SystemClock
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import com.nuvio.tv.R
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.core.player.StreamAutoPlaySelector
import com.nuvio.tv.core.player.recovery.AutoSourceRecoveryHandoff
import com.nuvio.tv.core.player.recovery.AutoSourceRecoveryPlan
import com.nuvio.tv.core.player.recovery.AutoSourceRecoverySettings
import com.nuvio.tv.core.player.recovery.StartupSourceRecovery
import com.nuvio.tv.core.player.recovery.recoverySourceKey
import com.nuvio.tv.core.stream.ReleaseFactsParser
import com.nuvio.tv.data.local.StreamAutoPlayMode
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.fork.sources.Placeholders
import com.nuvio.tv.fork.sources.StartupChecks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.coroutineContext

private const val TAG = "ForkSourceRecovery"
private const val REFRESH_BUDGET_MS = 4_000L
private const val RESOLVE_BUDGET_MS = 5_000L
private const val VIDEO_TRACK_GRACE_MS = 1_000L
private const val PLAYBACK_STALL_MS = 12_000L

/** Owns automatic start and recovery for one VOD, across player rebuilds and source changes. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class PlayerAutoSourceRecovery(
    private val player: PlayerRuntimeController,
    private var plan: AutoSourceRecoveryPlan
) {
    private var policy = SystemClock.elapsedRealtime().let { now ->
        StartupSourceRecovery(now, plan.startupState?.whenPlayerStarts(now))
    }
    private val enabled = AutoSourceRecoverySettings.enabled(player.context)
    private val attempted = plan.attempted.toMutableSet()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var job: Job? = null
    private var active = true
    private var confirmed = false
    private var failure: String? = null
    private var refreshedCandidates = false
    private var resumeAtMs: Long? = null
    private var currentSource = plan.initialSource
    private var lighterMaxHeight: Int? = null
    private val forkChecks = StartupChecks.Session().also { it.attempt(currentSource) } // Fork hook: kicia-checks
    private enum class NextResult { Started, Failed, Empty }

    val isTryingSources: Boolean get() = active && enabled.value && !confirmed

    fun remainingMs(): Long = if (confirmed) 3_000L else policy.remainingMs(SystemClock.elapsedRealtime())

    fun cancel() {
        active = false
        job?.cancel()
        if (player.autoSourceRecovery === this) player.autoSourceRecovery = null
    }

    /** Claims terminal errors before they reach the UI. Do not retain raw URLs or server responses. */
    fun failDefinitively(reason: String): Boolean {
        if (!active || !enabled.value || interrupted() || player.isSourceSwitching()) return false
        if (failure == null) {
            failure = "playback_error"
            Log.w(TAG, "player error, chars=${reason.length}, afterStart=$confirmed")
        }
        showSwitchProgress()
        wake.trySend(Unit)
        return true
    }

    fun start() {
        currentSource?.let { attempted += recoverySourceKey(it) }
        rememberCurrentUrl()
        job = player.scope.launch {
            try {
                watchPlayback()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "recovery stopped: ${e.javaClass.simpleName}")
                if (active && !interrupted()) showPicker()
            } finally {
                active = false
                if (player.autoSourceRecovery === this@PlayerAutoSourceRecovery) player.autoSourceRecovery = null
            }
        }
    }

    private suspend fun watchPlayback() {
        var samplePosition: Long? = null
        var positionBeforeStall: Long? = null
        var missingVideoAt: Long? = null
        var bufferingSince: Long? = null
        var lastBufferedPosition: Long? = null
        while (active && enabled.value) {
            val state = player._uiState.value
            val now = SystemClock.elapsedRealtime()
            val position = player.currentPlaybackPositionMs()
            if (state.playbackEnded && confirmed) return
            if (interrupted() || player.isSourceSwitching()) {
                if (!confirmed) return
                bufferingSince = null
                lastBufferedPosition = null
                waitForSignal(1_000L)
                continue
            }
            if (confirmed) {
                if (failure == null && state.error == null && player.isChangingLocalPlayback(now)) {
                    bufferingSince = null
                    lastBufferedPosition = null
                    samplePosition = position
                    waitForSignal(1_000L)
                    continue
                }
                val exo = player._exoPlayer
                val wantsToPlay = exo?.playWhenReady ?: player.hasActivePlayIntent()
                val isBuffering = exo?.let { it.playbackState == Player.STATE_BUFFERING }
                    ?: state.isBuffering
                val bufferedPosition = exo?.bufferedPosition
                    ?: player.mpvView?.demuxerCacheDurationSec()?.let { (it * 1_000).toLong() }
                val downloadAdvanced = lastBufferedPosition != null && bufferedPosition != null &&
                    bufferedPosition > lastBufferedPosition
                if (!wantsToPlay || !isBuffering || downloadAdvanced || position != samplePosition) {
                    bufferingSince = null
                } else if (bufferingSince == null) {
                    bufferingSince = now
                }
                lastBufferedPosition = bufferedPosition
                if (player.isPlaybackCurrentlyPlaying() && position != null) positionBeforeStall = position
                samplePosition = position
                val stalled = bufferingSince?.let { now - it >= PLAYBACK_STALL_MS } == true
                if (failure == null && state.error == null && !stalled) {
                    waitForSignal(1_000L)
                    continue
                }
                resumeAtMs = position?.takeIf { it > 0L } ?: positionBeforeStall?.takeIf { it > 0L }
                policy = StartupSourceRecovery(now)
                confirmed = false
                if (stalled) failure = "buffer_stall"
                bufferingSince = null
                lastBufferedPosition = null
                samplePosition = null
            }

            val video = videoStatus()
            if (video == VideoStatus.NoTrack && missingVideoAt == null) missingVideoAt = now
            if (video != VideoStatus.NoTrack) missingVideoAt = null
            if (failure == null && missingVideoAt?.let { now - it >= VIDEO_TRACK_GRACE_MS } == true) {
                failure = "no_video_track"
            }
            val seekTarget = resumeAtMs
            if (failure == null && seekTarget != null && resumeUnavailable(seekTarget)) failure = "cannot_resume"
            // Do not seed from a seek, a previous source, audio-only playback or a transient frame.
            val ready = video == VideoStatus.Visible && player.isPlaybackCurrentlyPlaying() &&
                state.pendingSeekPosition == null && (seekTarget == null || (position ?: 0L) >= seekTarget - 1_000L)
            val forkVerdict = forkChecks.verdict(player)
            if (failure == null && forkVerdict is StartupChecks.Verdict.Bad) failure = forkVerdict.reason
            val advancing = ready && position != null && samplePosition?.let { position - it >= 100L } == true
            samplePosition = if (ready) position else null
            val succeeded = failure == null && state.error == null && advancing && forkVerdict == StartupChecks.Verdict.Ok
            when (policy.evaluate(now, succeeded, interrupted = false, failed = failure != null || state.error != null)) {
                StartupSourceRecovery.Decision.Complete -> {
                    confirmPlayback(position)
                    if (seekTarget != null) Log.i(TAG, "resumed targetMs=$seekTarget actualMs=$position")
                    positionBeforeStall = position
                }
                StartupSourceRecovery.Decision.Wait -> waitForSignal(minOf(500L, remainingMs()).coerceAtLeast(1L))
                StartupSourceRecovery.Decision.Stop -> {
                    Log.i(TAG, "exhausted reason=${failure ?: "startup_timeout"} attempts=${policy.snapshot().attempts}")
                    forgetLastLink()
                    showPicker()
                    return
                }
                StartupSourceRecovery.Decision.NextSource -> {
                    val reason = failure ?: if (state.error == null) "startup_timeout" else "playback_error"
                    if (reason in setOf("startup_timeout", "buffer_stall", "no_video_track")) preferLighterSources()
                    Log.i(TAG, "changing source reason=$reason attempt=${policy.snapshot().attempts} resumeMs=$resumeAtMs")
                    rememberCurrentUrl()
                    forkChecks.onLeaving(player) // Fork hook: kicia-checks (pamięć złych źródeł)
                    showSwitchProgress()
                    stopAttempt()
                    forgetLastLink()
                    if (!policy.beginNextAttempt(SystemClock.elapsedRealtime())) {
                        showPicker()
                        return
                    }
                    missingVideoAt = null
                    samplePosition = null
                    val result = withTimeoutOrNull(remainingMs()) { tryNext() } ?: NextResult.Failed
                    coroutineContext.ensureActive()
                    if (!active || player.autoSourceRecovery !== this || interrupted()) return
                    if (result == NextResult.Empty) {
                        Log.i(TAG, "exhausted: no untried eligible sources")
                        showPicker()
                        return
                    }
                    failure = if (result == NextResult.Failed) "source_unavailable" else null
                    if (result == NextResult.Failed) wake.trySend(Unit)
                }
            }
        }
    }

    private fun interrupted(): Boolean {
        val state = player._uiState.value
        return player.isInBackground || player.userPausedManually || state.showSourcesPanel || state.showEpisodesPanel ||
            player._playbackTimeline.value.isLive
    }

    private suspend fun waitForSignal(timeMs: Long) {
        withTimeoutOrNull(timeMs) { wake.receive() }
    }

    private enum class VideoStatus { Waiting, NoTrack, Visible }

    private fun videoStatus(): VideoStatus {
        player._exoPlayer?.let { exo ->
            val videoSelected = exo.currentTracks.isTypeSelected(C.TRACK_TYPE_VIDEO)
            if (videoSelected && player.hasRenderedFirstFrame) return VideoStatus.Visible
            return if (exo.playbackState == Player.STATE_READY && !exo.currentTracks.isEmpty && !videoSelected) {
                VideoStatus.NoTrack
            } else VideoStatus.Waiting
        }
        val mpv = player.mpvView ?: return VideoStatus.Waiting
        if (!mpv.isPositionFromRequestedMedia()) return VideoStatus.Waiting
        if (!mpv.hasVideoTrackSelectedNow()) return VideoStatus.NoTrack
        return if (player.hasRenderedFirstFrame) VideoStatus.Visible else VideoStatus.Waiting
    }

    private fun resumeUnavailable(targetMs: Long): Boolean {
        val exo = player._exoPlayer
        if (exo?.playbackState == Player.STATE_READY && !exo.isCurrentMediaItemSeekable) return true
        if (player.hasRenderedFirstFrame && player.mpvView?.isLiveStreamNow() == true) return true
        val duration = player.currentPlaybackDurationMs()
        return duration > 0L && targetMs >= duration
    }

    private fun preferLighterSources() {
        if (plan.mode != StreamAutoPlayMode.FIRST_STREAM) return
        val height = player.currentVideoTrackHeight.takeIf { it > 0 }
            ?: currentSource?.let { ReleaseFactsParser.resolutionOf(it).label.removeSuffix("p").toIntOrNull() }
        val limit = when {
            height == null || height > 1_080 -> 1_080
            height > 720 -> 720
            else -> null
        } ?: return
        lighterMaxHeight = minOf(lighterMaxHeight ?: limit, limit)
    }

    private fun rememberCurrentUrl() {
        player.currentStreamUrl.takeIf { it.isNotBlank() }?.let { attempted += "url:$it" }
    }

    private fun confirmPlayback(position: Long?) {
        confirmed = true
        Log.i(TAG, "confirmed video positionMs=$position attempt=${policy.snapshot().attempts}")
        rememberCurrentUrl()
        if (!player._uiState.value.isTorrentStream) {
            currentSource?.let { player.persistSelectedStreamForReuse(it, player.currentStreamUrl, player.currentHeaders) }
        }
        failure = null
        resumeAtMs = null
        lighterMaxHeight = null
    }

    private fun showSwitchProgress() {
        player._uiState.update { state ->
            state.copy(
                error = null, isBuffering = true, showControls = false,
                showLoadingOverlay = state.loadingOverlayEnabled, showPauseOverlay = false,
                loadingMessage = player.context.getString(R.string.player_auto_source_recovery_trying)
            )
        }
    }

    private suspend fun forgetLastLink() {
        player.streamCacheKey?.let { player.streamLinkCacheDataStore.remove(it) }
    }

    private suspend fun tryNext(): NextResult {
        var candidate = plan.next(attempted, lighterMaxHeight)
        if (candidate == null && !refreshedCandidates) {
            refreshedCandidates = true
            var latest = player._uiState.value.sourceAllStreams
            if (latest.isEmpty()) {
                player.streamRepository.setLocalPluginSearchPaused(false)
                withTimeoutOrNull(minOf(REFRESH_BUDGET_MS, remainingMs())) {
                    player.streamRepository.getStreamsFromAllAddons(
                        type = player.contentType ?: "movie",
                        videoId = player.currentVideoId ?: player.contentId ?: return@withTimeoutOrNull,
                        season = player.currentSeason, episode = player.currentEpisode
                    ).collect { result ->
                        if (result is NetworkResult.Success) {
                            latest = StreamAutoPlaySelector.orderAddonStreams(result.data, plan.installedAddonNames.toList())
                                .flatMap { it.streams }
                        }
                    }
                }
            }
            if (latest.isNotEmpty()) plan = plan.withStreams(latest)
            candidate = plan.next(attempted, lighterMaxHeight)
        }
        val next = candidate ?: return NextResult.Empty
        attempted += recoverySourceKey(next)
        val resolved = withTimeoutOrNull(minOf(RESOLVE_BUDGET_MS, remainingMs())) {
            when {
                next.serverTarget != null -> player.prepareServerStream(next)
                next.youTubeIdToResolve() != null -> player.resolveYouTubeStream(next)
                else -> player.resolveDirectDebridStreamIfNeeded(next, player.currentSeason, player.currentEpisode)
            }
        } ?: return NextResult.Failed
        coroutineContext.ensureActive()
        if (!active || player.autoSourceRecovery !== this || interrupted()) return NextResult.Empty
        val url = resolved.getStreamUrl()
        if (url != null && (Placeholders.isMarked(url) || (url != next.getStreamUrl() && "url:$url" in attempted))) {
            return NextResult.Failed
        }
        if (url == null && (resumeAtMs != null || !player.torrentSettings.settings.first().p2pEnabled)) return NextResult.Failed
        attempted += recoverySourceKey(resolved)
        url?.let { attempted += "url:$it" }
        currentSource = resolved
        forkChecks.attempt(resolved) // Fork hook: kicia-checks
        failure = null
        Log.i(TAG, "opening alternative heightLimit=$lighterMaxHeight resumeMs=$resumeAtMs")
        player.switchToSourceStream(
            resolved,
            resumeSavedProgress = player.currentVideoId != player.navigationArgs.videoId || !player.navigationArgs.startFromBeginning,
            recoveryPositionMs = resumeAtMs
        )
        return NextResult.Started
    }

    private fun stopAttempt() {
        player.playerInitializationJob?.cancel()
        player.playerInitializationJob = null
        player.playbackPreparationJob?.cancel()
        player.debridResolveJob?.cancel()
        player.torrentStreamJob?.cancel()
        player.cancelAutoQualityChange(byViewer = false)
        player.resetErrorRetryState()
        player.cancelFirstFrameWatchdog()
        player.cancelStallWatchdog()
        player.releasePlayer(flushPlaybackState = false)
        player.stopTorrentStream()
        player.hasRenderedFirstFrame = false
        player._uiState.update { it.copy(error = null, isBuffering = true, isPlaying = false) }
    }

    private fun showPicker() {
        stopAttempt()
        player.sourceRecoveryPickerError = player.context.getString(R.string.player_auto_source_recovery_exhausted)
        player._uiState.update {
            it.copy(error = null, isBuffering = false, showLoadingOverlay = false, loadingMessage = null)
        }
        player.showSourcesPanel()
        android.widget.Toast.makeText(player.context, R.string.player_auto_source_recovery_exhausted, android.widget.Toast.LENGTH_SHORT).show()
    }
}

internal fun PlayerRuntimeController.beginAutoSourceRecovery(plan: AutoSourceRecoveryPlan) {
    autoSourceRecovery?.cancel()
    if (!AutoSourceRecoverySettings.enabled(context).value || plan.mode == StreamAutoPlayMode.MANUAL) return
    if (contentType in listOf("tv", "channel", "cloud")) return
    val recovery = PlayerAutoSourceRecovery(this, plan)
    autoSourceRecovery = recovery
    recovery.start()
}

internal fun PlayerRuntimeController.claimAutoSourceRecovery() {
    AutoSourceRecoveryHandoff.take(navigationArgs.autoSourceRecoveryToken, profileId, currentVideoId)
        ?.let { beginAutoSourceRecovery(it) }
}
