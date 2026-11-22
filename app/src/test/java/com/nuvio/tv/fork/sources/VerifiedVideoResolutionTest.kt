package com.nuvio.tv.fork.sources

import com.nuvio.tv.domain.model.ReleaseResolution
import org.junit.Assert.assertEquals
import org.junit.Test

class VerifiedVideoResolutionTest {
    @Test
    fun keepsFullWidthClassWhenLetterboxBarsAreCropped() {
        assertEquals(ReleaseResolution.P2160, verifiedVideoResolution(3840, 1608))
        assertEquals(ReleaseResolution.P1080, verifiedVideoResolution(1920, 804))
    }

    @Test
    fun classifiesARecorded720pTrackFromPixels() {
        assertEquals(ReleaseResolution.P720, verifiedVideoResolution(1280, 720))
        assertEquals(ReleaseResolution.UNKNOWN, verifiedVideoResolution(0, 0))
    }
}
