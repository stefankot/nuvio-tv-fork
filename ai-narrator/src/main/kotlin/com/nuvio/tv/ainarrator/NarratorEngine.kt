package com.nuvio.tv.ainarrator

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/** A separate Android audio stream. Neither ExoPlayer nor mpv output pipeline is modified. */
internal class NarratorAudio {
    private var player: MediaPlayer? = null
    private var currentKey: String? = null
    fun isCurrent(key: String) = currentKey == key

    fun play(key: String, file: File, volume: Float) {
        stop()
        val next = MediaPlayer()
        currentKey = key
        player = next
        try {
            next.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            next.setDataSource(file.absolutePath)
            next.setVolume(volume, volume)
            next.setOnPreparedListener { prepared ->
                if (player === prepared && currentKey == key) prepared.start()
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

    fun pause() { runCatching { if (player?.isPlaying == true) player?.pause() } }
    fun resume() { runCatching { if (player?.isPlaying == false) player?.start() } }
    fun setVolume(value: Float) { runCatching { player?.setVolume(value, value) } }
    fun stop() {
        val old = player
        player = null
        currentKey = null
        if (old != null) runCatching { old.reset(); old.release() }
    }
}

internal class NarratorEngine(context: Context) {
    private val prefs = NarratorPreferences(context)
    private val cache = NarratorAudioCache(File(context.applicationContext.cacheDir, "ai-narrator"))
    private val synthesis = GeminiNarratorTts()
    private val workScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(2)
    private val pending = ConcurrentHashMap<String, Job>()
    private val retryAfter = ConcurrentHashMap<String, Long>()
    private val audio = NarratorAudio()
    private val alreadySpoken = mutableSetOf<String>()
    private val mutableStatus = MutableStateFlow("Wyłączony")
    val status: StateFlow<String> = mutableStatus

    private var lastMediaId: String = ""
    private var lastPosition: Long = -1L
    private var lastVisibleText: String = ""
    private var liveLine: NarratorLine? = null
    private var confirmedTimeline: Boolean = false
    private var spokenCueId: String? = null

    /** Called on the UI thread; synthesis runs solely on Dispatchers.IO. */
    fun tick(frame: NarratorFrame, options: NarratorOptions, apiKey: String) {
        if (!options.enabled || apiKey.isBlank() || frame.mediaId.isEmpty()) {
            audio.stop()
            mutableStatus.value = if (options.enabled && apiKey.isBlank()) "Brak klucza Gemini API" else "Wyłączony"
            return
        }
        if (frame.mediaId != lastMediaId || (lastPosition >= 0 && abs(frame.positionMs - lastPosition) > 3500)) {
            resetPlayback()
            lastMediaId = frame.mediaId
        }
        lastPosition = frame.positionMs
        val visible = NarratorText.clean(frame.visibleText)
        if (visible != lastVisibleText) {
            lastVisibleText = visible
            liveLine = if (NarratorText.eligible(visible)) {
                NarratorLine(frame.positionMs.coerceAtLeast(0), frame.positionMs.coerceAtLeast(0) + 6000L, visible)
            } else null
        }
        val timedCurrent = NarratorPlanner.current(frame.timedLines, frame.positionMs)
        if (visible.isNotBlank() && timedCurrent != null &&
            NarratorText.clean(timedCurrent.text) == visible) confirmedTimeline = true
        if (confirmedTimeline && visible.isNotBlank() && timedCurrent != null &&
            NarratorText.clean(timedCurrent.text) != visible) confirmedTimeline = false
        val useTimeline = confirmedTimeline && frame.timedLines.isNotEmpty()
        val planned = if (useTimeline) NarratorPlanner.upcoming(frame.timedLines, frame.positionMs, options.prefetchMs)
            else listOfNotNull(liveLine).filter { it.endMs > frame.positionMs }

        if (frame.isPlaying && !frame.isBuffering) {
            planned.forEach { line ->
                val style = styleFor(line, frame.timedLines)
                val key = cacheKey(options, line, style)
                if (cache.find(key) == null && !pending.containsKey(key) &&
                    System.currentTimeMillis() >= (retryAfter[key] ?: 0L)) {
                    schedule(key, line.text, style, options, apiKey)
                }
            }
        }

        val current = if (useTimeline) timedCurrent else liveLine?.takeIf {
            visible.isNotBlank() && it.text == visible && NarratorPlanner.stillAudible(it, frame.positionMs)
        }
        if (current == null || !NarratorPlanner.stillAudible(current, frame.positionMs)) {
            audio.stop()
            spokenCueId = null
            return
        }
        val id = "${frame.mediaId}:${current.startMs}:${current.endMs}:${current.text}"
        if (!frame.isPlaying || frame.isBuffering) {
            audio.pause()
            return
        }
        if (spokenCueId == id && audio.isCurrent(id)) {
            audio.resume()
            audio.setVolume(options.narratorVolume)
            return
        }
        audio.stop()
        if (id in alreadySpoken) return
        val style = styleFor(current, frame.timedLines)
        val path = cache.find(cacheKey(options, current, style)) ?: return
        // Never begin a delayed narration after most of a subtitle has elapsed.
        if (current.endMs - frame.positionMs < 550L) return
        spokenCueId = id
        alreadySpoken.add(id)
        audio.play(id, path, options.narratorVolume)
        mutableStatus.value = "Odtwarzanie"
    }

    private fun styleFor(line: NarratorLine, lines: List<NarratorLine>): String {
        val index = lines.indexOfFirst { it.startMs == line.startMs && it.text == line.text }
        return NarratorText.style(line.text, lines.getOrNull(index - 1)?.text)
    }
    private fun cacheKey(options: NarratorOptions, line: NarratorLine, style: String): String =
        NarratorText.fingerprint(options.model, options.voice, NarratorText.clean(line.text), style)

    private fun schedule(key: String, text: String, style: String, options: NarratorOptions, apiKey: String) {
        val job = workScope.launch(start = CoroutineStart.LAZY) {
            try {
                gate.withPermit {
                    if (cache.find(key) != null) return@withPermit
                    val now = YearMonth.now()
                    val estimatedSeconds = (text.length / 9.0).coerceIn(1.5, 25.0)
                    val conservative = NarratorPrices.estimatedUsd(
                        options.model, text.length, estimatedSeconds * 1.8, now.year, now.monthValue
                    )
                    if (!prefs.reserve(conservative, options.monthlyUsdLimit)) {
                        retryAfter[key] = System.currentTimeMillis() + 3_600_000L
                        mutableStatus.value = "Osiągnięto miesięczny limit kosztów"
                        return@withPermit
                    }
                    mutableStatus.value = "Synteza mowy"
                    val wav = synthesis.synthesize(apiKey, options.model, options.voice, NarratorText.clean(text), style)
                    cache.store(key, wav)
                    mutableStatus.value = "Gotowy"
                }
            } catch (_: Exception) {
                retryAfter[key] = System.currentTimeMillis() + 180_000L
                mutableStatus.value = "Błąd syntezy — sprawdź model, klucz i połączenie"
            } finally {
                pending.remove(key)
            }
        }
        pending[key] = job
        job.start()
    }

    private fun resetPlayback() {
        audio.stop()
        alreadySpoken.clear()
        spokenCueId = null
        confirmedTimeline = false
        lastVisibleText = ""
        liveLine = null
    }

    fun clearCache() = cache.clear()
    fun close() {
        resetPlayback()
        workScope.cancel()
    }
}
