package com.nuvio.tv.ui.screens.player

import android.os.SystemClock
import android.util.Log
import androidx.media3.common.Player
import com.nuvio.tv.core.autoquality.AutoQualityFlags
import com.nuvio.tv.core.player.BitrateAwareLoadControl
import com.nuvio.tv.core.streams.smart.SmartPolicyHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * forge M4c F (N11): the buffer follows the file. The byte budget = the file's bitrate × 60 s (within the device's
 * memory budget, less the hot swap's loading limit), and after the first stall playback resumes only with 10 s
 * buffered; after 120 s without a stall the viewer's setting comes back. Off with BUFFER_TUNING_ENABLED = false.
 */
private const val TAG = "AutoQuality"
private const val REBUFFER_START_MS = 10_000
private const val CALM_MS = 120_000L
private const val BUDGET_SECONDS = 60L
private const val MIN_BUDGET_BYTES = 32L * 1024 * 1024

private var forgeBufferJob: Job? = null

internal fun PlayerRuntimeController.startForgeBufferTuning() {
    if (!AutoQualityFlags.BUFFER_TUNING_ENABLED) return
    forgeBufferJob?.cancel()
    forgeBufferJob = scope.launch {
        var lastStallAt = 0L
        var wasBuffering = false
        var tightened = false
        while (isActive) {
            delay(1_000)
            val control = _loadControl as? BitrateAwareLoadControl ?: continue
            val player = _exoPlayer ?: continue
            // budget from the file's bitrate, never above the device's own
            val bps = currentVideoBitrate?.takeIf { it > 0 }?.toLong()
                ?: com.nuvio.tv.core.streams.smart.SmartPolicy.averageBitrateBps(currentVideoSize, _playbackTimeline.value.duration / 1000)
            if (bps != null) {
                val device = control.effectiveBufferBudgetBytes().coerceAtLeast(MIN_BUDGET_BYTES)
                val swapReserve = PlayerAutoQualityHost(this@startForgeBufferTuning).preloadLimitBytes()
                val want = (bps / 8 * BUDGET_SECONDS).coerceIn(MIN_BUDGET_BYTES, (device - swapReserve).coerceAtLeast(MIN_BUDGET_BYTES))
                if (!SmartPolicyHolder.current.lowRam) control.setBudgetBytesOverride(want)
            }
            val buffering = hasRenderedFirstFrame && player.playbackState == Player.STATE_BUFFERING && player.playWhenReady
            val now = SystemClock.elapsedRealtime()
            if (buffering && !wasBuffering) {
                lastStallAt = now
                if (!tightened) {
                    control.setRebufferStartOverrideMs(REBUFFER_START_MS)
                    tightened = true
                    Log.i(TAG, "buffer: resume after a stall at ${REBUFFER_START_MS / 1000}s buffered")
                }
            }
            if (tightened && !buffering && now - lastStallAt >= CALM_MS) {
                control.setRebufferStartOverrideMs(-1)
                tightened = false
                Log.i(TAG, "buffer: calm for ${CALM_MS / 1000}s, viewer's setting back")
            }
            wasBuffering = buffering
        }
    }
}

internal fun stopForgeBufferTuning() { forgeBufferJob?.cancel(); forgeBufferJob = null }
