package com.nuvio.tv.ainarrator

import org.junit.Assert.*
import org.junit.Test

class NarratorBackendTest {
    @Test fun parsesOnlyBaseServerEndpoints() {
        assertEquals("http://127.0.0.1:1919/", ZonosEndpoint.parse("http://127.0.0.1:1919").toString())
        assertEquals("https://192.168.1.22/", ZonosEndpoint.parse("https://192.168.1.22").toString())
        listOf(
            "https://example.com/private/path",
            "ftp://192.168.1.2:1919",
            "http://user:password@192.168.1.2:1919/",
            "http://192.168.1.2:1919/v2/generate",
            "http://192.168.1.2:1919/?token=123",
            "http://192.168.1.2",
            " "
        ).forEach { input ->
            try {
                ZonosEndpoint.parse(input)
                fail("Accepted malformed endpoint: $input")
            } catch (_: IllegalArgumentException) {
                // Expected: neither secrets nor API paths may appear in endpoint URLs.
            }
        }
    }

    @Test fun rejectsInvalidAudioAndAcceptsWavContainer() {
        assertFalse(WavValidation.isWav(ByteArray(0)))
        assertFalse(WavValidation.isWav(ByteArray(44)))
        assertFalse(WavValidation.isWav("RIFF-WRONG".toByteArray()))
        val wav = ByteArray(44)
        "RIFF".toByteArray().copyInto(wav, 0)
        "WAVE".toByteArray().copyInto(wav, 8)
        assertTrue(WavValidation.isWav(wav))
    }

    @Test fun backendSpecificCacheNeverCollides() {
        val text = "Czy jesteś gotowy?"
        val cloud = NarratorText.fingerprint("gemini:flash:Kore", text, NarratorText.style(text, null))
        val local = NarratorText.fingerprint("zonos3:http://192.168.1.10:1919/:auto", text, "")
        val differentVoice = NarratorText.fingerprint("zonos3:http://192.168.1.10:1919/:voice2", text, "")
        assertNotEquals(cloud, local)
        assertNotEquals(local, differentVoice)
    }

    @Test fun seekDetectionDoesNotTreatPauseAsSeek() {
        assertFalse(NarratorPlanner.isDiscontinuity(5000, 5000, 2500))
        assertFalse(NarratorPlanner.isDiscontinuity(5000, 5200, 220))
        assertTrue(NarratorPlanner.isDiscontinuity(5000, 1000, 220))
        assertTrue(NarratorPlanner.isDiscontinuity(5000, 12000, 220))
        assertFalse(NarratorPlanner.isDiscontinuity(-1, 2000, 0))
    }

    @Test fun localModeStartsDisabled() {
        val defaults = NarratorOptions()
        assertEquals(NarratorBackend.GEMINI, defaults.backend)
        assertFalse(defaults.enabled)
        assertEquals("", defaults.localEndpoint)
    }
}
