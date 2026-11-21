package com.nuvio.app.features.player

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DubbingAudioSyncDspTest {
    @Test
    fun `late unindexed source prefers platform seek over linear Media3 scan`() {
        assertTrue(requiresDubbingPlatformSeek(400_000_000L, 0L, 0L))
        assertFalse(requiresDubbingPlatformSeek(30_000_000L, 0L, 0L))
        assertFalse(requiresDubbingPlatformSeek(400_000_000L, 12_345L, 390_000_000L))
    }

    @Test
    fun `linear demux discards compressed samples until decoder preroll`() {
        assertTrue(shouldDiscardDubbingSample(399_749_999L, 400_000_000L))
        assertFalse(shouldDiscardDubbingSample(399_750_000L, 400_000_000L))
        assertFalse(shouldDiscardDubbingSample(400_000_000L, 400_000_000L))
    }

    @Test
    fun `unindexed matroska estimates a byte range near requested time`() {
        assertEquals(250_000_000L, approximateDubbingBytePosition(1_800_000_000L, 7_200_000_000L, 1_000_000_000L))
        assertEquals(null, approximateDubbingBytePosition(10L, 0L, 1_000L))
    }

    @Test
    fun `matroska seek may keep a close forward cluster with enough window remaining`() {
        assertTrue(canAcceptApproximateDubbingForwardSeek(508_649_000L, 503_601_000L, 534_601_000L))
        assertFalse(canAcceptApproximateDubbingForwardSeek(509_601_001L, 503_601_000L, 534_601_000L))
        assertFalse(canAcceptApproximateDubbingForwardSeek(508_000_000L, 503_601_000L, 522_000_000L))
        assertFalse(canAcceptApproximateDubbingForwardSeek(503_601_000L, 503_601_000L, 534_601_000L))
    }

    @Test
    fun `two real matroska timestamps extrapolate close to target`() {
        assertEquals(
            233_580_817L,
            extrapolateDubbingBytePosition(
                targetTimeUs = 433_795_000L,
                firstPosition = 174_444_167L,
                firstTimeUs = 337_856_000L,
                secondPosition = 210_441_836L,
                secondTimeUs = 396_256_000L,
                minimumPosition = 210_441_837L,
                maximumPosition = 1_087_290_982L,
            ),
        )
    }

    @Test
    fun `matroska cluster marker is located without consuming following bytes`() {
        val bytes = byteArrayOf(9, 8, 0x1f, 0x43, 0xb6.toByte(), 0x75, 7, 6)

        assertEquals(2, findMatroskaClusterMarker(bytes))
        assertEquals(-1, findMatroskaClusterMarker(bytes, length = 5))
    }

    @Test
    fun `single extractor track survives a stale player ordinal`() {
        val onlyTrack = DubbingAudioTrackCandidate(3, 0, "por", "audio/mp4a-latm", 2)

        val resolved = resolveDubbingAudioTrack(
            candidates = listOf(onlyTrack),
            requestedOrdinal = 4,
            hint = DubbingAudioTrackHint(language = "pt-BR", mimeType = "audio/mp4a-latm", channelCount = 2),
        )

        assertEquals(onlyTrack, resolved)
    }

    @Test
    fun `language hint repairs different player and extractor ordering`() {
        val english = DubbingAudioTrackCandidate(1, 0, "eng", "audio/eac3", 6)
        val portuguese = DubbingAudioTrackCandidate(2, 1, "por", "audio/eac3", 6)

        val resolved = resolveDubbingAudioTrack(
            candidates = listOf(english, portuguese),
            requestedOrdinal = 0,
            hint = DubbingAudioTrackHint(language = "pt", mimeType = "audio/eac3", channelCount = 6),
        )

        assertEquals(portuguese, resolved)
    }

    @Test
    fun `feature correlation returns signed B minus A offset`() {
        val random = Random(42)
        val referenceChannels = List(6) { channel ->
            FloatArray(3_000) { index ->
                val pulse = if ((index + channel * 17) % 137 == 0) 3f else 0f
                pulse + random.nextFloat() * 0.08f
            }
        }
        val insertion = 2_345
        val searchChannels = referenceChannels.mapIndexed { channel, reference ->
            FloatArray(8_000) { random.nextFloat() * if (channel < 3) 0.2f else 0.04f }.also { search ->
                reference.forEachIndexed { index, value -> search[insertion + index] += value }
            }
        }

        val peak = requireNotNull(
            DubbingAudioSyncDsp.correlateFeatures(
                DubbingFeatureSeries(180_000L, 10, referenceChannels),
                DubbingFeatureSeries(60_000L, 10, searchChannels),
            )
        )

        val expected = 60_000L + insertion * 10L - 180_000L
        assertTrue("expected=$expected actual=${peak.offsetMs}", abs(peak.offsetMs - expected) <= 10L)
        assertTrue(peak.peakToSidelobeRatio > 8f)
        assertTrue(!peak.isAtSearchBoundary(
            DubbingFeatureSeries(180_000L, 10, referenceChannels),
            DubbingFeatureSeries(60_000L, 10, searchChannels),
        ))
    }

    @Test
    fun `correlation peak at search edge is rejected`() {
        val channels = List(6) { channel ->
            FloatArray(2_000) { index -> if ((index + channel) % 97 == 0) 1f else 0f }
        }
        val reference = DubbingFeatureSeries(100_000L, 10, channels)
        val search = DubbingFeatureSeries(
            70_000L,
            10,
            channels.map { referenceChannel ->
                FloatArray(8_000).also { destination -> referenceChannel.copyInto(destination) }
            },
        )

        val peak = requireNotNull(DubbingAudioSyncDsp.correlateFeatures(reference, search))

        assertEquals(-30_000L, peak.offsetMs)
        assertTrue(peak.isAtSearchBoundary(reference, search))
    }

    @Test
    fun `borderline peak corroborates nearby nonzero offset without validating a new offset`() {
        val peak = DubbingCorrelationPeak(
            offsetMs = -13_076L,
            score = 0.05755103f,
            peakToSidelobeRatio = 4.960549f,
            peakToSecondRatio = 1.3306305f,
        )

        assertTrue(peak.corroboratesCurrentOffset(-12_970L))
        assertFalse(peak.corroboratesCurrentOffset(0L))
        assertFalse(peak.corroboratesCurrentOffset(-12_000L))
        assertFalse(peak.copy(peakToSidelobeRatio = 4.49f).corroboratesCurrentOffset(-12_970L))
        assertFalse(peak.copy(peakToSecondRatio = 1.19f).corroboratesCurrentOffset(-12_970L))
    }

    @Test
    fun `borderline full peak is accepted only when independent slices agree`() {
        val random = Random(73)
        val referenceChannels = List(6) {
            FloatArray(3_000) { random.nextFloat() * 2f - 1f }
        }
        val insertion = 3_003
        val searchChannels = referenceChannels.map { reference ->
            FloatArray(9_000) { random.nextFloat() * 0.02f - 0.01f }.also { search ->
                reference.copyInto(search, destinationOffset = insertion)
            }
        }
        val reference = DubbingFeatureSeries(300_000L, 10, referenceChannels)
        val search = DubbingFeatureSeries(270_000L, 10, searchChannels)
        val measured = requireNotNull(DubbingAudioSyncDsp.correlateFeatures(reference, search))
        val borderline = measured.copy(
            score = 0.051577356f,
            peakToSidelobeRatio = 4.0632777f,
            peakToSecondRatio = 1.1810184f,
        )

        val consensus = requireNotNull(
            DubbingAudioSyncDsp.correlateIndependentSlices(reference, search, borderline)
        )

        assertTrue(abs(consensus.firstSlice.offsetMs - borderline.offsetMs) <= 200L)
        assertTrue(abs(consensus.secondSlice.offsetMs - borderline.offsetMs) <= 200L)
        assertEquals(null, DubbingAudioSyncDsp.correlateIndependentSlices(
            reference,
            search,
            borderline.copy(peakToSidelobeRatio = 3.99f),
        ))
    }

    @Test
    fun `borderline new offset requires a matching independent temporal window`() {
        val primary = DubbingCorrelationPeak(
            offsetMs = 23_188L,
            score = 0.053194582f,
            peakToSidelobeRatio = 4.1322227f,
            peakToSecondRatio = 1.121917f,
        )
        val confirmation = DubbingCorrelationPeak(
            offsetMs = 23_240L,
            score = 0.049f,
            peakToSidelobeRatio = 4.4f,
            peakToSecondRatio = 1.08f,
        )

        assertTrue(primary.canRequestIndependentWindow())
        assertTrue(primary.isCorroboratedByIndependentWindow(confirmation))
        assertFalse(primary.isCorroboratedByIndependentWindow(confirmation.copy(offsetMs = 23_400L)))
        assertFalse(primary.isCorroboratedByIndependentWindow(confirmation.copy(peakToSidelobeRatio = 3.99f)))
        assertFalse(primary.copy(peakToSecondRatio = 1.09f).canRequestIndependentWindow())
    }

    @Test
    fun `exceptionally strong near-zero peak confirms the default without opening generic ambiguity`() {
        val observed = DubbingCorrelationPeak(
            offsetMs = 40L,
            score = 0.13809328f,
            peakToSidelobeRatio = 9.172386f,
            peakToSecondRatio = 1.0531877f,
        )

        assertTrue(observed.corroboratesStrongZeroOffset(0L))
        assertFalse(observed.copy(offsetMs = 251L).corroboratesStrongZeroOffset(0L))
        assertFalse(observed.copy(score = 0.099f).corroboratesStrongZeroOffset(0L))
        assertFalse(observed.copy(peakToSidelobeRatio = 7.99f).corroboratesStrongZeroOffset(0L))
        assertFalse(observed.copy(peakToSecondRatio = 1.049f).corroboratesStrongZeroOffset(0L))
    }
}
