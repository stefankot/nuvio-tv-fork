package com.nuvio.tv.ainarrator

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.SystemClock
import kotlin.math.max

/**
 * Conservative fail-closed guard for CPU inference on Android TV while a video is running.
 * Thresholds are provisional and must be validated against peak native PSS on each device.
 * A native worker must expose cooperative cancellation to make an in-flight stop effective.
 */
internal data class ZonosMemorySnapshot(
    val totalBytes: Long,
    val availableBytes: Long,
    val lowMemoryThresholdBytes: Long,
    val systemLowMemory: Boolean,
    val applicationPssBytes: Long
)

internal enum class ZonosMemoryDecision { ALLOW, INSUFFICIENT_TOTAL_RAM, LOW_AVAILABLE_RAM, EXCESS_PROCESS_MEMORY }

internal data class ZonosMemoryVerdict(
    val decision: ZonosMemoryDecision,
    val freeNeededBytes: Long,
    val reserveBytes: Long,
    val processLimitBytes: Long
) {
    val allowed: Boolean get() = decision == ZonosMemoryDecision.ALLOW
}

internal object ZonosMemoryPolicy {
    private const val MIB = 1024L * 1024L
    private const val GIB = 1024L * MIB

    /**
     * 2 GB boxes are deliberately NOT approved for simultaneous 4K playback + 1.1 GB GGUF.
     * 3/4 GB classes may run only if live headroom and native process-size checks pass.
     */
    fun evaluate(snapshot: ZonosMemorySnapshot, starting: Boolean, offline: Boolean = false): ZonosMemoryVerdict {
        val ram = snapshot.totalBytes
        val mib = 1024L * 1024L
        val isTwoGbClass = ram < 2_500L * mib
        val isThreeGbClass = ram < 3_500L * mib
        val freeNeeded = when {
            isTwoGbClass -> 1_250L * mib
            isThreeGbClass -> 1_500L * mib
            else -> 1_750L * mib
        }
        val reserve = max(snapshot.lowMemoryThresholdBytes + 256L * mib, when {
            isTwoGbClass -> 450L * mib
            isThreeGbClass -> 520L * mib
            else -> 700L * mib
        })
        val processLimit = when {
            isTwoGbClass -> 1_100L * mib
            isThreeGbClass -> 1_300L * mib
            else -> 1_950L * mib
        }
        val decision = when {
            ram < 1_700L * mib || (isTwoGbClass && !offline) ->
                ZonosMemoryDecision.INSUFFICIENT_TOTAL_RAM
            snapshot.systemLowMemory || snapshot.availableBytes < reserve ->
                ZonosMemoryDecision.LOW_AVAILABLE_RAM
            snapshot.applicationPssBytes > processLimit ->
                ZonosMemoryDecision.EXCESS_PROCESS_MEMORY
            starting && snapshot.availableBytes < freeNeeded ->
                ZonosMemoryDecision.LOW_AVAILABLE_RAM
            else -> ZonosMemoryDecision.ALLOW
        }
        return ZonosMemoryVerdict(decision, freeNeeded, reserve, processLimit)
    }
}

/** Avoid polling ActivityManager on every subtitle-render frame. */
internal class ZonosMemoryGuard(private val context: Context) {
    private val manager: ActivityManager =
        context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var lastSampleMs: Long = -1L
    private var cached: ZonosMemoryVerdict? = null

    fun check(starting: Boolean): ZonosMemoryVerdict {
        val now = SystemClock.elapsedRealtime()
        if (!starting && cached != null && lastSampleMs >= 0 && now - lastSampleMs < 1_500L) {
            return cached!!
        }
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        val verdict = ZonosMemoryPolicy.evaluate(
            ZonosMemorySnapshot(
                totalBytes = info.totalMem,
                availableBytes = info.availMem,
                lowMemoryThresholdBytes = info.threshold,
                systemLowMemory = info.lowMemory,
                // Debug.getPss() returns KiB. Include native and mapped GGUF pages in this process.
                applicationPssBytes = Debug.getPss().toLong() * 1024L
            ),
            starting = starting
        )
        cached = verdict
        lastSampleMs = now
        return verdict
    }
}
