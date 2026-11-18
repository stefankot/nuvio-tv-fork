package com.nuvio.tv.ainarrator

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap

/** Independent system audio stream: no modifications to ExoPlayer or mpv. */
internal class NarratorAudio {
    private var player: MediaPlayer? = null
    private var currentKey: String? = null
    private var paused = false

    fun isCurrent(key: String) = currentKey == key && player != null

    fun play(key: String, file: File, volume: Float) {
        stop()
        val next = MediaPlayer()
        currentKey = key
        player = next
        paused = false
        try {
            next.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            )
            next.setDataSource(file.absolutePath)
            next.setVolume(volume, volume)
            next.setOnPreparedListener { ready ->
                if (player === ready && currentKey == key && !paused) {
                    runCatching { ready.start() }
                }
            }
            next.setOnCompletionListener { if (player === it) stop() }
            next.setOnErrorListener { source, _, _ ->
                if (player === source) stop()
                true
            }
            next.prepareAsync()
        } catch (_: Exception) {
            stop()
        }
    }

    fun pause() {
        paused = true
        runCatching { if (player?.isPlaying == true) player?.pause() }
    }

    fun resume() {
        paused = false
        // An async prepare listener will start playback only when preparation is complete.
        runCatching {
            val active = player ?: return@runCatching
            if (!active.isPlaying) active.start()
        }
    }

    fun setVolume(value: Float) {
        runCatching { player?.setVolume(value, value) }
    }

    fun stop() {
        val old = player
        player = null
        currentKey = null
        paused = false
        if (old != null) {
            runCatching { old.reset() }
            runCatching { old.release() }
        }
    }
}

/**
 * Pure input contract: NarratorFrame + NarratorOptions. All network requests execute on IO.
 * Backend changes, seeking, disabling and switching media cancel obsolete prefetch jobs.
 */
internal class NarratorEngine(context: Context) {
    private val prefs = NarratorPreferences(context)
    private val cache = NarratorAudioCache(File(context.applicationContext.cacheDir, "ai-narrator"))
    private val gemini = GeminiNarratorTts()
    private val zonos = LocalZonosTts()
    private val workScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val geminiSlots = Semaphore(2)
    private val localSlots = Semaphore(1)
    private val pending = ConcurrentHashMap<String, Job>()
    private val retryAfter = ConcurrentHashMap<String, Long>()
    private val audio = NarratorAudio()
    private val played = HashSet<String>()
    private val mutableStatus = MutableStateFlow("Wyłączony")
    val status: StateFlow<String> = mutableStatus

    private var lastMedia = ""
    private var lastPosition = -1L
    private var lastClock = -1L
    private var lastText = ""
    private var currentLiveLine: NarratorLine? = null
    private var confirmedTimeline = false
    private var playingCueId: String? = null
    private var configIdentity = ""
    private var active = false

    /** Call on the UI thread, at regular intervals while enabled. */
    fun tick(frame: NarratorFrame, options: NarratorOptions, geminiKey: String, localToken: String = "") {
        val ready = when (options.backend) {
            NarratorBackend.GEMINI -> geminiKey.isNotBlank()
            NarratorBackend.ZONOS_LOCAL -> options.localEndpoint.isNotBlank()
        }
        if (!options.enabled || !ready || frame.mediaId.isBlank()) {
            if (active) {
                resetPlayback()
                active = false
            }
            mutableStatus.value = when {
                !options.enabled -> "Wyłączony"
                !ready && options.backend == NarratorBackend.GEMINI -> "Brak klucza Gemini API"
                !ready -> "Skonfiguruj adres lokalnego ZONOS3"
                else -> "Brak aktywnego filmu"
            }
            return
        }
        active = true
        val configuration = backendIdentity(options)
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (lastClock < 0L) 0L else (now - lastClock).coerceAtLeast(0L)
        if (configuration != configIdentity || frame.mediaId != lastMedia ||
            NarratorPlanner.isDiscontinuity(lastPosition, frame.positionMs, elapsed)) {
            resetPlayback()
            configIdentity = configuration
            lastMedia = frame.mediaId
        }
        lastClock = now
        lastPosition = frame.positionMs

        val visible = NarratorText.clean(frame.visibleText)
        if (visible != lastText) {
            lastText = visible
            currentLiveLine = if (NarratorText.eligible(visible)) {
                val start = frame.positionMs.coerceAtLeast(0L)
                NarratorLine(start, start + 6_000L, visible)
            } else null
        }

        val matched = NarratorPlanner.current(frame.timedLines, frame.positionMs)
        if (visible.isNotEmpty() && matched != null) {
            confirmedTimeline = NarratorText.clean(matched.text) == visible
        }
        val useTimeline = confirmedTimeline && frame.timedLines.isNotEmpty()
        val upcoming = if (useTimeline) {
            NarratorPlanner.upcoming(frame.timedLines, frame.positionMs, options.prefetchMs)
        } else {
            listOfNotNull(currentLiveLine).filter { it.endMs > frame.positionMs }
        }

        if (frame.isPlaying && !frame.isBuffering) {
            val maxPending = if (options.backend == NarratorBackend.ZONOS_LOCAL) 3 else 6
            for (line in upcoming) {
                if (pending.size >= maxPending) break
                val style = styleFor(options, line, frame.timedLines)
                val key = cacheKey(configuration, line.text, style)
                if (cache.find(key) == null && !pending.containsKey(key) &&
                    SystemClock.elapsedRealtime() >= (retryAfter[key] ?: 0L)) {
                    startSynthesis(key, line.text, style, options, geminiKey, localToken)
                }
            }
        }

        val current = if (useTimeline) matched else currentLiveLine?.takeIf {
            visible.isNotEmpty() && it.text == visible
        }
        if (current == null || !NarratorPlanner.stillAudible(current, frame.positionMs)) {
            audio.stop()
            playingCueId = null
            return
        }
        val id = "${frame.mediaId}:${current.startMs}:${current.endMs}:${current.text}"
        if (!frame.isPlaying || frame.isBuffering) {
            audio.pause()
            return
        }
        if (playingCueId == id && audio.isCurrent(id)) {
            audio.resume()
            audio.setVolume(options.narratorVolume)
            return
        }
        audio.stop()
        if (id in played) return
        val style = styleFor(options, current, frame.timedLines)
        val wav = cache.find(cacheKey(configuration, current.text, style), touch = true) ?: return
        if (current.endMs - frame.positionMs < 750L) return
        played.add(id)
        playingCueId = id
        audio.play(id, wav, options.narratorVolume)
        mutableStatus.value = "Odtwarzanie"
    }

    private fun backendIdentity(options: NarratorOptions): String = when (options.backend) {
        NarratorBackend.GEMINI -> "gemini:${options.model.apiId}:${options.voice}"
        NarratorBackend.ZONOS_LOCAL -> "zonos3:${options.localEndpoint.trimEnd('/')}:${options.localSpeakerId}"
    }

    private fun styleFor(options: NarratorOptions, line: NarratorLine, lines: List<NarratorLine>): String {
        if (options.backend == NarratorBackend.ZONOS_LOCAL) return ""
        val index = lines.indexOfFirst { it.startMs == line.startMs && it.text == line.text }
        return NarratorText.style(line.text, lines.getOrNull(index - 1)?.text)
    }

    private fun cacheKey(identity: String, text: String, style: String): String =
        NarratorText.fingerprint(identity, NarratorText.clean(text), style)

    private fun startSynthesis(
        key: String,
        text: String,
        style: String,
        options: NarratorOptions,
        geminiKey: String,
        localToken: String
    ) {
        // LAZY + putIfAbsent means the same request is never submitted twice by a fast UI tick.
        val job = workScope.launch(start = CoroutineStart.LAZY) {
            try {
                val slots = if (options.backend == NarratorBackend.ZONOS_LOCAL) localSlots else geminiSlots
                slots.withPermit {
                    if (cache.find(key) != null) return@withPermit
                    val audioBytes = when (options.backend) {
                        NarratorBackend.ZONOS_LOCAL -> {
                            mutableStatus.value = "Lokalna synteza ZONOS3"
                            zonos.synthesize(options.localEndpoint, localToken, options.localSpeakerId, NarratorText.clean(text))
                        }
                        NarratorBackend.GEMINI -> {
                            val month = YearMonth.now()
                            val seconds = (text.length / 9.0).coerceIn(1.5, 25.0)
                            val estimated = NarratorPrices.estimatedUsd(
                                options.model, text.length, seconds * 1.8, month.year, month.monthValue
                            )
                            if (!prefs.reserve(estimated, options.monthlyUsdLimit)) {
                                retryAfter[key] = SystemClock.elapsedRealtime() + 3_600_000L
                                mutableStatus.value = "Osiągnięto limit szacowanych kosztów"
                                return@withPermit
                            }
                            mutableStatus.value = "Synteza Gemini"
                            gemini.synthesize(geminiKey, options.model, options.voice, NarratorText.clean(text), style)
                        }
                    }
                    cache.store(key, audioBytes)
                    mutableStatus.value = "Gotowy"
                }
            } catch (_: Exception) {
                retryAfter[key] = SystemClock.elapsedRealtime() + 180_000L
                mutableStatus.value = when (options.backend) {
                    NarratorBackend.GEMINI -> "Błąd Gemini — sprawdź klucz, model i połączenie"
                    NarratorBackend.ZONOS_LOCAL -> "Błąd ZONOS3 — sprawdź serwer, głos i token"
                }
            } finally {
                pending.remove(key, coroutineContext[Job])
            }
        }
        val existing = pending.putIfAbsent(key, job)
        if (existing == null) job.start() else job.cancel()
    }

    private fun resetPlayback() {
        audio.stop()
        pending.values.forEach { it.cancel() }
        pending.clear()
        played.clear()
        playingCueId = null
        confirmedTimeline = false
        lastText = ""
        currentLiveLine = null
        lastClock = -1L
        lastPosition = -1L
        retryAfter.clear()
    }

    fun clearCache() = cache.clear()

    fun close() {
        resetPlayback()
        workScope.cancel()
    }
}
