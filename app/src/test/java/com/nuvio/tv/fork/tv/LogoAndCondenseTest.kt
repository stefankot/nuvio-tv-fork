package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.lektor.Condense
import com.nuvio.tv.fork.tv.logo.ForgeLogoPack
import com.nuvio.tv.fork.tv.logo.ForkLogoPrefs
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy 22.11: paczka logo kanałów (zip), logo z wyłączonym modułem, tempo mowy lektora do skracania kwestii. */
class LogoAndCondenseTest {
    @After fun reset() { ForkLogoPrefs.enabled = true }

    @Test fun `paczka logo to plik zip, także z parametrami`() {
        assertTrue(ForgeLogoPack.isZip("https://x/logos.zip"))
        assertTrue(ForgeLogoPack.isZip(" https://x/LOGOS.ZIP?dl=1 "))
        assertFalse(ForgeLogoPack.isZip("https://x/logo/{name}.png"))
        assertFalse(ForgeLogoPack.isZip("https://x/a?file=b.zip"))
    }

    @Test fun `logo przy wyłączonym module - jak w Reshaped`() {
        ForkLogoPrefs.enabled = false
        val ch = LiveTvChannel(id = "1", name = "TVN", streamUrl = "http://a/1.ts", logoUrl = "http://logo/tvn.png", guideKey = "tvn.pl")
        assertEquals("http://logo/tvn.png", ForkLogoPrefs.resolve(ch, mapOf("tvn.pl" to "http://guide/tvn.png")))
        assertEquals("http://guide/tvn.png", ForkLogoPrefs.resolve(ch.copy(logoUrl = " "), mapOf("tvn.pl" to "http://guide/tvn.png")))
    }

    @Test fun `tempo mowy - uczy się tylko z sensownych nagrań`() {
        val text = "To jest zdanie testowe lektora o długości."
        val before = Condense.msPerCharNow()
        Condense.learn("krótko", 10_000)
        Condense.learn(text, 0)
        Condense.learn(text, text.length * 1_000L)
        assertEquals("krótkie, puste i nierealne nagrania pomijane", before, Condense.msPerCharNow(), 0.0)
        Condense.learn(text, (text.length * (before + 50)).toLong())
        assertTrue("wolniejsza mowa podnosi tempo", Condense.msPerCharNow() > before)
        assertEquals((text.length * Condense.msPerCharNow()).toLong(), Condense.estimateMs(text))
    }
}
