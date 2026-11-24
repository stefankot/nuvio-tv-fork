package com.nuvio.tv.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fork (UX JMGO 24.11, `plan/ux/jmgo/PROMPT.md`): focus journeys measured with [FrameTimingMetric] only — no profile
 * generation. Kept in its own file so the base benchmark files stay untouched.
 *
 * NOT runnable on the JMGO projector (`plan/BLOKADY.md`, 24.11): the shared setup pins the device locale to English
 * (system setting) and the fork target is debuggable. Run on an emulator or a spare TV:
 * `-Pandroid.testInstrumentationRunnerArguments.class=com.nuvio.tv.baselineprofile.ForkFocusFrameBenchmark`.
 */
@RunWith(AndroidJUnit4::class)
class ForkFocusFrameBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /** ROW_MEMORY: ▼ ► ▼ ▲ — the focus returns to the same poster of the row. */
    @Test fun rowMemory() = measure { rowMemory() }

    /** MENU_ROUNDTRIP: ◄ into the drawer and ► back to the remembered content item. */
    @Test fun menuRoundtrip() = measure { menuRoundtrip() }

    /** DETAILS_BACK: OK on a poster opens the title, Back restores the poster. */
    @Test fun detailsBack() = measure { detailsBack() }

    /** LIVE_LIST_HOLD: Live TV channel list, held ▼ and ▲ (key repeat without idle waits). */
    @Test fun liveListHold() = measure { liveListHold() }

    /** EPG_GRID: Live TV guide, ► in time and ▼▲ between channels. */
    @Test fun epgGrid() = measure { epgGrid() }

    private fun measure(block: UiDevice.() -> Unit) {
        val device = uiDevice()
        val packageName = resolveTargetPackage(device, BENCHMARK_TARGET_PACKAGE)
        device.withEnglishLocale {
            benchmarkRule.measureRepeated(
                packageName = packageName,
                metrics = listOf(FrameTimingMetric()),
                compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
                iterations = 5,
                setupBlock = {
                    pressHome()
                    startActivityAndWait()
                    device.prepareForJourney(packageName)
                },
                measureBlock = { device.block() },
            )
        }
    }
}

private fun UiDevice.rowMemory() {
    repeat(4) {
        dpad("down", steps = 1, delayMs = 250L)
        dpad("right", steps = 3, delayMs = 200L)
        dpad("down", steps = 1, delayMs = 250L)
        dpad("up", steps = 1, delayMs = 250L)
    }
    waitForIdle()
}

private fun UiDevice.menuRoundtrip() {
    dpad("down", steps = 1, delayMs = 250L)
    repeat(4) {
        dpad("left", steps = 8, delayMs = 120L)
        dpad("right", steps = 1, delayMs = 300L)
        dpad("right", steps = 2, delayMs = 200L)
    }
    waitForIdle()
}

private fun UiDevice.detailsBack() {
    dpad("down", steps = 2, delayMs = 250L)
    repeat(3) {
        dpad("right", steps = 1, delayMs = 200L)
        pressDPadCenter()
        waitForIdle()
        Thread.sleep(2_500)
        goBack(steps = 1)
        Thread.sleep(800)
    }
}

/** Live TV through the drawer: ◄ to the drawer, ▼ until the entry is focused, OK. */
private fun UiDevice.openLiveTvFromDrawer(): Boolean {
    dpad("left", steps = 8, delayMs = 120L)
    repeat(10) {
        if (findObject(By.focused(true).hasDescendant(By.textContains("Live"))) != null ||
            findObject(By.focused(true).textContains("Live")) != null
        ) {
            pressDPadCenter(); waitForIdle(); Thread.sleep(2_000); return true
        }
        pressDPadDown(); waitForIdle()
    }
    return false
}

private fun UiDevice.liveListHold() {
    if (!openLiveTvFromDrawer()) return
    dpad("down", steps = 2, delayMs = 250L)
    repeat(60) { runCatching { pressDPadDown() }; Thread.sleep(50) }
    repeat(60) { runCatching { pressDPadUp() }; Thread.sleep(50) }
    waitForIdle()
}

private fun UiDevice.epgGrid() {
    if (!openLiveTvFromDrawer()) return
    dpad("down", steps = 2, delayMs = 250L)
    repeat(3) {
        dpad("right", steps = 3, delayMs = 220L)
        dpad("down", steps = 3, delayMs = 220L)
        dpad("left", steps = 3, delayMs = 220L)
        dpad("up", steps = 2, delayMs = 220L)
    }
    waitForIdle()
}
