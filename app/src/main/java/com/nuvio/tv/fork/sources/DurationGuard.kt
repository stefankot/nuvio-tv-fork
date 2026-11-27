package com.nuvio.tv.fork.sources

import android.util.Log
import androidx.media3.common.C
import com.nuvio.tv.R
import com.nuvio.tv.core.debrid.DirectDebridPlayableResult
import com.nuvio.tv.core.streams.smart.ReleaseParser
import com.nuvio.tv.core.util.parseRuntimeMinutes
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.fork.ForkLog
import com.nuvio.tv.fork.logged
import com.nuvio.tv.fork.sources.kicia.BadReason
import com.nuvio.tv.fork.sources.kicia.DurationSanity
import com.nuvio.tv.fork.sources.kicia.DurationVerdict
import com.nuvio.tv.fork.sources.kicia.SmartBadSources
import com.nuvio.tv.fork.sources.kicia.badSourceKey
import com.nuvio.tv.fork.sources.kicia.judgeDuration
import com.nuvio.tv.ui.screens.player.PlayerRuntimeController
import com.nuvio.tv.ui.screens.player.loadSourceStreams
import com.nuvio.tv.ui.screens.player.nextSourceFallbackCandidate
import com.nuvio.tv.ui.screens.player.sourceFallbackKey
import com.nuvio.tv.ui.screens.player.streamAddonsFor
import com.nuvio.tv.ui.screens.player.switchToSourceStream
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.WeakHashMap

/**
 * Przegląd ekranów 27.11, ekran 03 (DECYZJE 27.11): kontrola długości pliku przy natywnym autoplayu.
 * Wołane z istniejącego wpięcia „gotowy do odtwarzania” (ExoPlayer STATE_READY, przez `PolishDubbing.onReady`).
 *  - film: plik różny od długości z opisu tytułu o > ±10% → następne źródło (reguły autoplay Nuvio),
 *  - odcinek: stara reguła Kicii (ponad 2× dłuższy albo ponad 2× krótszy),
 *  - klip < 3 min („Too many requests”, „not cached”) → zawsze następne źródło.
 * Źródło wybrane ręcznie: klip zawsze zmieniany, zła długość tylko komunikat (widz sam wybrał to wydanie).
 * Zły plik: usunięty z pamięci „ostatnio działającego linku”, odłożony w pamięci złych źródeł (Kicia), klip
 * oznaczony jako zaślepka — nie wraca przy następnym uruchomieniu.
 * Przełącznik: [StartupChecks.KEY] (kontrola pliku, domyślnie włączona).
 */
object DurationGuard {
    private const val TAG = "ForkDurationGuard"
    private const val CLIP_MS = 180_000L
    private const val MAX_SWITCHES = 5
    private const val POLLS = 30
    private const val POLL_MS = 500L

    private class State {
        var checkedUrl: String? = null
        var switches = 0
        val sanity = DurationSanity()
        var titleKey: String? = null
    }

    private val states = WeakHashMap<PlayerRuntimeController, State>()

    fun onReady(owner: PlayerRuntimeController) {
        if (!ForkFeatures.enabled(owner.context, StartupChecks.KEY, true)) return
        runCatching { check(owner) }.logged(TAG, "DurationGuard.onReady")
    }

    private fun check(owner: PlayerRuntimeController) {
        val exo = owner._exoPlayer ?: return
        val url = owner.currentStreamUrl.takeIf { it.isNotBlank() } ?: return
        val state = synchronized(states) { states.getOrPut(owner) { State() } }
        val title = "${owner.contentId}:${owner.currentSeason}:${owner.currentEpisode}"
        if (state.titleKey != title) { state.titleKey = title; state.switches = 0; state.checkedUrl = null }
        if (state.checkedUrl == url) return
        val fileMs = exo.duration
        if (fileMs == C.TIME_UNSET || fileMs <= 0L) return
        state.checkedUrl = url
        val ui = owner._uiState.value
        if (ui.isLive || !exo.isCurrentMediaItemSeekable) { Log.i(TAG, "skip: live or not seekable"); return }
        val episode = owner.currentEpisode != null
        val expectedSec = expectedSec(owner)
        val text = listOfNotNull(ui.currentStreamName, ui.currentStreamDescription).joinToString(" ")
        val edition = runCatching { ReleaseParser.parse(text).edition }.logged(TAG, "edition").getOrNull()
        val clip = fileMs < CLIP_MS && (expectedSec == null || expectedSec * 1000L >= 2 * CLIP_MS)
        val verdict = judgeDuration(fileMs, expectedSec, episode, edition)
        val auto = owner.autoSelectedSourceUrl == url
        Log.i(TAG, "file_s=${fileMs / 1000} expected_s=$expectedSec episode=$episode edition=$edition verdict=$verdict clip=$clip auto=$auto addon=${ui.currentStreamAddonName}")
        if (!clip && verdict != DurationVerdict.WRONG) return
        val current = currentStream(owner, url)
        if (!clip) {
            val key = current?.let { badSourceKey(it) } ?: "url:$url"
            if (state.sanity.onWrong(key, fileMs)) { Log.i(TAG, "two agreeing files: title runtime is wrong, playing"); return }
        }
        val reason = BadReason.WRONG_DURATION
        current?.let { s -> runCatching { SmartBadSources.mark(s, reason) }.logged(TAG, "mark bad") }
        if (clip) Placeholders.mark(url, "długość ${fileMs / 1000} s")
        owner.streamCacheKey?.let { key -> owner.scope.launch { runCatching { owner.streamLinkCacheDataStore.remove(key) }.logged(TAG, "link cache remove") } }
        com.nuvio.tv.fork.family.FamilyStats.badSource(reason.name, ui.currentStreamName)
        val c = owner.context
        if (!clip && !auto) {
            ForkLog.say(c, TAG, c.getString(R.string.duration_guard_manual_wrong, minutes(fileMs), expectedSec?.div(60) ?: 0))
            return
        }
        if (state.switches >= MAX_SWITCHES) {
            ForkLog.say(c, TAG, c.getString(R.string.duration_guard_limit))
            return
        }
        state.switches++
        ForkLog.say(c, TAG, if (clip) c.getString(R.string.duration_guard_clip, fileMs / 1000) else c.getString(R.string.duration_guard_wrong, minutes(fileMs), expectedSec?.div(60) ?: 0))
        owner.sourceFallbackTriedKeys += "url:$url"
        current?.let { owner.sourceFallbackTriedKeys += it.sourceFallbackKey() }
        owner.scope.launch { runCatching { moveOn(owner, url, current) }.logged(TAG, "moveOn") }
    }

    private suspend fun moveOn(owner: PlayerRuntimeController, failedUrl: String, failed: Stream?) {
        val videoId = owner.currentVideoId ?: owner.contentId ?: return
        owner.loadSourceStreams(forceRefresh = false)
        val settings = owner.playerSettingsDataStore.playerSettings.first()
        val installed = owner.streamAddonsFor(videoId).map { it.displayName }.toSet()
        var polls = 0
        var tries = 0
        while (tries < 8) {
            if (owner.currentStreamUrl != failedUrl) { Log.i(TAG, "stream changed meanwhile, stop"); return }
            val sources = owner._uiState.value
            val untried = sources.sourceAllStreams.filterNot { s ->
                s.sourceFallbackKey() in owner.sourceFallbackTriedKeys || Placeholders.isMarked(s.getStreamUrl()) ||
                    runCatching { SmartBadSources.isBlocked(s) }.logged(TAG, "isBlocked").getOrDefault(false)
            }
            val next = nextSourceFallbackCandidate(
                streams = untried, settings = settings, installedAddonNames = installed,
                triedKeys = owner.sourceFallbackTriedKeys, failedAddonName = failed?.addonName,
                failedInfoHash = failed?.getEffectiveInfoHash(),
            ) ?: untried.firstOrNull()
            if (next == null) {
                val done = !sources.isLoadingSourceStreams && (sources.sourceAllStreams.isNotEmpty() || sources.sourceStreamsError != null)
                if (done || polls >= POLLS) break
                delay(POLL_MS); polls++; continue
            }
            tries++
            owner.sourceFallbackTriedKeys += next.sourceFallbackKey()
            val playable = (owner.directDebridResolver.resolveToPlayableStream(next, owner.currentSeason, owner.currentEpisode)
                as? DirectDebridPlayableResult.Success)?.stream
            val url = playable?.getStreamUrl()
            if (playable == null || url.isNullOrBlank() || url == failedUrl) { Log.i(TAG, "candidate from ${next.addonName} has no playable link"); continue }
            Log.i(TAG, "next source: ${playable.addonName} «${playable.name?.take(60)}»")
            owner.switchToSourceStream(playable)
            owner.autoSelectedSourceUrl = owner.currentStreamUrl
            owner.sourceFallbackTriedKeys += "url:${owner.currentStreamUrl}"
            return
        }
        Log.w(TAG, "no other source")
        ForkLog.say(owner.context, TAG, owner.context.getString(R.string.duration_guard_none))
    }

    private fun currentStream(owner: PlayerRuntimeController, url: String): Stream? {
        val ui = owner._uiState.value
        return (ui.sourceAllStreams + ui.episodeAllStreams).firstOrNull { it.getStreamUrl() == url }
    }

    private fun minutes(ms: Long) = ((ms + 30_000L) / 60_000L)

    /** Czas tytułu z metadanych w pamięci (bez zapytań): film = runtime, odcinek = runtime odcinka. */
    private fun expectedSec(owner: PlayerRuntimeController): Long? {
        val type = owner.contentType ?: return null
        val id = owner.contentId ?: return null
        val meta = runCatching { owner.metaRepository.getCachedMeta(type, id) }.logged(TAG, "getCachedMeta").getOrNull() ?: return null
        val ep = owner.currentEpisode
        val minutes = if (ep != null) meta.videos.firstOrNull { it.season == owner.currentSeason && it.episode == ep }?.runtime
            else parseRuntimeMinutes(meta.runtime)
        return minutes?.takeIf { it > 0 }?.times(60L)
    }
}
