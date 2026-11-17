package com.nuvio.tv.fork.voice

import android.content.Context
import com.nuvio.tv.fork.tv.ekrany.PendingPlay
import com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoiceActionsTest {
    private val opened = ArrayList<String>()
    private val ctx = mockk<Context>(relaxed = true)

    @Before fun setUp() { ForkFeatures.open = { opened += it } }
    @After fun tearDown() { ForkFeatures.open = null }

    @Test fun `nawigacja otwiera właściwe trasy`() {
        assertTrue(VoiceActions.run(ctx, VoiceCommandCall("home")))
        assertTrue(VoiceActions.run(ctx, VoiceCommandCall("tv")))
        assertTrue(VoiceActions.run(ctx, VoiceCommandCall("settings")))
        assertTrue(VoiceActions.run(ctx, VoiceCommandCall("apps")))
        assertEquals(listOf("home", LIVE_TV_ROUTE, "settings", "fork_apps"), opened)
    }

    @Test fun `kanal prosi o odtwarzanie i otwiera Live TV`() {
        assertTrue(VoiceActions.run(ctx, VoiceCommandCall("channel.play", mapOf("kanal_id" to "http://x/stream"))))
        assertEquals(listOf(LIVE_TV_ROUTE), opened)
        assertEquals("http://x/stream", PendingPlay.request.value?.streamUrl)
    }

    @Test fun `kanal bez identyfikatora i nieznane polecenie nie są obsłużone`() {
        assertFalse(VoiceActions.run(ctx, VoiceCommandCall("channel.play", mapOf("kanal" to "tvp"))))
        assertFalse(VoiceActions.run(ctx, VoiceCommandCall("zupelnie.nieznane")))
        assertTrue(opened.isEmpty())
    }

    @Test fun `napisy nie pasują bez odtwarzacza nie są obsłużone`() {
        com.nuvio.tv.fork.sources.PlayerBridge.clear()
        assertFalse(VoiceActions.run(ctx, VoiceCommandCall("subs.mismatch")))
    }
}
