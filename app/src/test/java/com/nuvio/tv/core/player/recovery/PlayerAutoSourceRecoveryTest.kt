package com.nuvio.tv.core.player.recovery

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.nuvio.tv.ui.screens.player.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerAutoSourceRecoveryTest {
    @After fun tearDown() = unmockkAll()

    private class Fixture(scope: TestScope) {
        val player = mockk<PlayerRuntimeController>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val state = MutableStateFlow(PlayerUiState())
        val enabled = MutableStateFlow(true)
        val plan = mockk<AutoSourceRecoveryPlan>(relaxed = true)
        var nextSourceAtMs: Long? = null
        val recovery: PlayerAutoSourceRecovery

        init {
            mockkObject(AutoSourceRecoverySettings)
            mockkStatic(SystemClock::class)
            mockkStatic(
                "com.nuvio.tv.ui.screens.player.PlayerRuntimeControllerMpvKt",
                "com.nuvio.tv.ui.screens.player.PlayerRuntimeControllerLifecycleKt",
                "com.nuvio.tv.ui.screens.player.PlayerRuntimeControllerErrorRecoveryKt",
                "com.nuvio.tv.ui.screens.player.PlayerRuntimeControllerObserversKt",
                "com.nuvio.tv.ui.screens.player.PlayerRuntimeControllerTorrentKt",
                "com.nuvio.tv.ui.screens.player.PlayerRuntimeControllerSourceSwitchKt",
                "com.nuvio.tv.ui.screens.player.PlayerRuntimeControllerAutoQualityKt"
            )
            every { SystemClock.elapsedRealtime() } answers { scope.testScheduler.currentTime }
            every { AutoSourceRecoverySettings.enabled(context) } returns enabled
            every { context.getString(any()) } returns "Buffering"
            every { player.context } returns context
            every { player.scope } returns scope.backgroundScope
            every { player._uiState } returns state
            every { player._playbackTimeline } returns MutableStateFlow(PlaybackTimelineState())
            every { player._exoPlayer } returns null
            every { player.mpvView } returns null
            every { player.currentStreamUrl } returns "https://example.test/first"
            every { player.currentPlaybackPositionMs() } returns 0L
            every { player.isPlaybackCurrentlyPlaying() } returns false
            every { player.resetErrorRetryState() } just Runs
            every { player.cancelFirstFrameWatchdog() } just Runs
            every { player.cancelStallWatchdog() } just Runs
            every { player.releasePlayer(flushPlaybackState = false) } just Runs
            every { player.stopTorrentStream() } just Runs
            every { player.cancelAutoQualityChange(byViewer = false) } just Runs
            every { player.isSourceSwitching() } returns false
            every { player.streamCacheKey } returns null
            every { plan.startupState } returns null
            every { plan.initialSource } returns null
            every { plan.attempted } returns emptySet()
            coEvery { plan.next(any(), any()) } coAnswers {
                nextSourceAtMs = scope.testScheduler.currentTime
                // Keep URL selection pending so tests can observe the exact handoff time.
                awaitCancellation()
            }
            recovery = PlayerAutoSourceRecovery(player, plan)
            every { player.autoSourceRecovery } returns recovery
        }
    }

    @Test fun `terminal error wakes recovery before the next 500 ms sample`() = runTest {
        val f = Fixture(this)
        f.recovery.start()
        runCurrent()
        advanceTimeBy(123L)

        assertTrue(f.recovery.failDefinitively("HTTP 403: access denied\n\nTechnical details"))
        assertNull(f.state.value.error)
        assertTrue(f.state.value.showLoadingOverlay)
        runCurrent()

        assertEquals(123L, f.nextSourceAtMs)
        f.recovery.cancel()
    }

    @Test fun `failure before recovery coroutine starts is not lost`() = runTest {
        val f = Fixture(this)
        f.recovery.start()
        assertTrue(f.recovery.failDefinitively("Source unavailable"))
        runCurrent()

        assertEquals(0L, f.nextSourceAtMs)
        assertNull(f.state.value.error)
        f.recovery.cancel()
    }

    @Test fun `waiting source keeps its full startup deadline and explains a timeout`() = runTest {
        val f = Fixture(this)
        f.recovery.start()
        runCurrent()
        advanceTimeBy(14_999L)
        runCurrent()
        assertNull(f.nextSourceAtMs)
        advanceTimeBy(1L)
        runCurrent()

        assertEquals(15_000L, f.nextSourceAtMs)
        f.recovery.cancel()
    }

    @Test fun `manual pause disabled setting and cancellation do not claim errors`() = runTest {
        val f = Fixture(this)
        every { f.player.userPausedManually } returns true
        assertFalse(f.recovery.failDefinitively("Paused"))
        every { f.player.userPausedManually } returns false
        f.enabled.value = false
        assertFalse(f.recovery.failDefinitively("Disabled"))
        f.enabled.value = true
        f.recovery.cancel()
        assertFalse(f.recovery.failDefinitively("Cancelled"))
    }

    @Test fun `ready playback with audio only is rejected without using the full timeout`() = runTest {
        val f = Fixture(this)
        val exo = mockk<ExoPlayer>(relaxed = true)
        val tracks = mockk<Tracks>(relaxed = true)
        every { f.player._exoPlayer } returns exo
        every { exo.playbackState } returns Player.STATE_READY
        every { exo.currentTracks } returns tracks
        every { tracks.isEmpty } returns false
        every { tracks.isTypeSelected(C.TRACK_TYPE_VIDEO) } returns false
        every { f.player.isPlaybackCurrentlyPlaying() } returns true
        every { f.player.currentPlaybackPositionMs() } answers { testScheduler.currentTime }
        f.recovery.start()
        runCurrent()
        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(1_000L, f.nextSourceAtMs)
        f.recovery.cancel()
    }

}
