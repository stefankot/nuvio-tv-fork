package com.nuvio.tv.ainarrator

import android.content.Context
import android.os.StatFs
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

internal enum class PreparationHalf { FIRST, SECOND }
internal enum class PreparationPhase { IDLE, WORKING, READY, PAUSED, BLOCKED, ERROR }

internal data class PreparationStatus(
    val phase: PreparationPhase = PreparationPhase.IDLE,
    val completed: Int = 0,
    val total: Int = 0,
    val cached: Int = 0,
    val elapsedSeconds: Long = 0,
    val etaSeconds: Long? = null,
    val message: String = ""
) {
    val percentage: Int get() = if (total == 0) 0 else completed * 100 / total
}

/** Pure planning logic: incomplete autosync/visible subtitles must never masquerade as a whole film. */
internal object NarratorPreparationPlan {
    fun allFilmCues(lines: List<NarratorLine>, durationMs: Long): List<NarratorLine>? {
        if (durationMs < 60_000L || lines.size < 20) return null
        val ordered = lines.filter { NarratorText.eligible(it.text) }.sortedBy { it.startMs }
        if (ordered.size < 20 || ordered.first().startMs > durationMs / 10L ||
            ordered.last().endMs < durationMs * 9L / 10L) return null
        return ordered
    }

    fun select(lines: List<NarratorLine>, durationMs: Long, half: PreparationHalf): List<NarratorLine> {
        val midpoint = durationMs / 2L
        return lines.filter { if (half == PreparationHalf.FIRST) it.startMs < midpoint else it.startMs >= midpoint }
    }

    fun identity(): String = "zonos3-device:ptq-keep2-v1:pl:neutral"

    fun key(line: NarratorLine): String =
        NarratorText.fingerprint(identity(), NarratorText.clean(line.text), "")

    fun shouldPause(elapsedMs: Long, budgetMs: Long = 15L * 60L * 1000L): Boolean =
        elapsedMs >= budgetMs
}

/**
 * Files survive app restarts and Android cache cleanup.
 * No silent eviction or regeneration; storage is constrained and cleanup is explicit.
 */
internal class NarratorPersistentAudio(context: Context) {
    private val directory = File(context.applicationContext.filesDir, "ai-narrator/zonos3-prepared/v1")
    private val keyPattern = Regex("[a-f0-9]{64}")
    private val minFreeBytes = 500L * 1024L * 1024L
    private val maxTotalBytes = 2L * 1024L * 1024L * 1024L

    init { directory.mkdirs() }

    fun find(key: String): File? {
        if (!keyPattern.matches(key)) return null
        return File(directory, "$key.wav").takeIf { it.isFile && it.length() > 44 }
    }

    @Synchronized fun store(key: String, wav: ByteArray): File {
        require(keyPattern.matches(key) && WavValidation.isWav(wav))
        val target = File(directory, "$key.wav")
        if (target.length() > 44) return target
        val existingBytes = directory.listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L
        if (existingBytes + wav.size > maxTotalBytes ||
            StatFs(directory.absolutePath).availableBytes < wav.size + minFreeBytes) {
            throw IOException("Brak miejsca na gotowe audio; nagrania pozostają zapisane")
        }
        val partial = File.createTempFile("narrator-", ".part", directory)
        try {
            partial.writeBytes(wav)
            if (!partial.renameTo(target) && !target.exists()) {
                throw IOException("Nie udało się zachować syntezy")
            }
        } finally { partial.delete() }
        return target
    }

    @Synchronized fun deleteOnUserRequest() {
        directory.listFiles()?.filter { it.isFile && (it.extension == "wav" || it.extension == "part") }
            ?.forEach(File::delete)
    }
}

/**
 * Only runs when video playback is PAUSED, and at most one inference at a time.
 * Progress counts successful/cached cues. Cancel preserves already committed WAVs.
 */
internal class NarratorPreparer(
    private val context: Context,
    private val models: ZonosOnDeviceModels,
    private val audio: NarratorPersistentAudio
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val guard = ZonosMemoryGuard(context)
    private val runtime = ZonosNativeServer(context, guard)
    private var job: Job? = null
    private val stopped = AtomicBoolean(false)
    private val mutableStatus = MutableStateFlow(PreparationStatus())
    val status: StateFlow<PreparationStatus> = mutableStatus

    fun begin(
        half: PreparationHalf,
        durationMs: Long,
        cues: List<NarratorLine>,
        isVideoPlaying: () -> Boolean
    ) {
        if (job?.isActive == true) return
        val all = NarratorPreparationPlan.allFilmCues(cues, durationMs)
        if (all == null) {
            mutableStatus.value = PreparationStatus(
                PreparationPhase.BLOCKED,
                message = "Brak pełnych napisów z czasami; nie można przygotować połowy filmu"
            )
            return
        }
        if (isVideoPlaying()) {
            mutableStatus.value = PreparationStatus(PreparationPhase.BLOCKED,
                message = "Wstrzymaj film przed syntezą, aby zwolnić pamięć")
            return
        }
        val paths = models.modelPaths()
        if (paths == null) {
            mutableStatus.value = PreparationStatus(PreparationPhase.BLOCKED,
                message = "Najpierw pobierz i zweryfikuj modele")
            return
        }
        if (!runtime.isPackaged()) {
            mutableStatus.value = PreparationStatus(PreparationPhase.BLOCKED,
                message = "Brak natywnego silnika ARM64 w APK. Sam model GGUF nie generuje mowy.")
            return
        }
        val toPrepare = NarratorPreparationPlan.select(all, durationMs, half)
        if (toPrepare.isEmpty()) {
            mutableStatus.value = PreparationStatus(PreparationPhase.BLOCKED,
                message = "Nie ma kwestii do przygotowania w tej części filmu")
            return
        }

        stopped.set(false)
        job = scope.launch {
            val started = SystemClock.elapsedRealtime()
            var completed = 0
            var cacheHits = 0
            var synthesized = 0
            var watchdog: Job? = null
            try {
                val memory = guard.check(starting = true, offline = true)
                if (!memory.allowed) throw IllegalStateException("Za mało wolnej RAM: " + memory.decision)
                watchdog = scope.launch {
                    while (isActive) {
                        delay(1000L)
                        if (isVideoPlaying() || !guard.check(starting = false).allowed) {
                            stopped.set(true)
                            runtime.stop()
                            break
                        }
                    }
                }
                for (line in toPrepare) {
                    ensureActive()
                    if (stopped.get() || isVideoPlaying()) throw PreparationPaused()
                    val elapsed = SystemClock.elapsedRealtime() - started
                    if (NarratorPreparationPlan.shouldPause(elapsed)) throw PreparationPaused()
                    val key = NarratorPreparationPlan.key(line)
                    if (audio.find(key) != null) {
                        cacheHits++
                    } else {
                        val wav = runtime.synthesize(paths, NarratorText.clean(line.text))
                        if (stopped.get()) throw PreparationPaused()
                        audio.store(key, wav)
                        synthesized++
                    }
                    completed++
                    val seconds = (SystemClock.elapsedRealtime() - started) / 1000L
                    val eta = if (synthesized > 0 && completed < toPrepare.size) {
                        (seconds * (toPrepare.size - completed) / completed.coerceAtLeast(1))
                    } else null
                    mutableStatus.value = PreparationStatus(PreparationPhase.WORKING,
                        completed, toPrepare.size, cacheHits, seconds, eta)
                }
                mutableStatus.value = PreparationStatus(PreparationPhase.READY,
                    completed, toPrepare.size, cacheHits,
                    (SystemClock.elapsedRealtime() - started) / 1000L,
                    message = "Ta część filmu jest przygotowana")
            } catch (_: PreparationPaused) {
                mutableStatus.value = PreparationStatus(PreparationPhase.PAUSED,
                    completed, toPrepare.size, cacheHits,
                    (SystemClock.elapsedRealtime() - started) / 1000L,
                    message = "Wstrzymano. Gotowe audio jest zapisane; można wznowić")
            } catch (e: CancellationException) {
                mutableStatus.value = PreparationStatus(PreparationPhase.PAUSED,
                    completed, toPrepare.size, cacheHits, message = "Anulowano, audio zachowane")
                throw e
            } catch (error: Exception) {
                mutableStatus.value = PreparationStatus(PreparationPhase.ERROR,
                    completed, toPrepare.size, cacheHits,
                    message = error.message ?: "Błąd przygotowania")
            } finally {
                watchdog?.cancel()
                runtime.stop()
            }
        }
    }

    fun pause() {
        stopped.set(true)
        job?.cancel()
        runtime.stop()
    }

    fun close() {
        pause()
        scope.cancel()
    }

    private class PreparationPaused : Exception()
}
