package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBehaviorHints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DubbingTimingTest {
    @Test
    fun `positive offset advances source B`() {
        assertEquals(12_500L, DubbingTiming.sourcePositionMs(10_000L, 2_500L))
    }

    @Test
    fun `negative source position is clamped`() {
        assertEquals(0L, DubbingTiming.sourcePositionMs(1_000L, -2_000L))
    }

    @Test
    fun `offset is clamped to thirty minutes`() {
        assertEquals(DubbingTiming.MAX_OFFSET_MS, DubbingTiming.clampOffset(Long.MAX_VALUE))
        assertEquals(DubbingTiming.MIN_OFFSET_MS, DubbingTiming.clampOffset(Long.MIN_VALUE))
    }

    @Test
    fun `device delay and dubbing offset remain independent`() {
        assertEquals(-1.75, DubbingTiming.effectiveMpvAudioDelaySeconds(250, 2_000L), 0.0001)
    }

    @Test
    fun `media source maps B timestamps onto A clock`() {
        assertEquals(12_500_000L, DubbingTiming.sourcePositionUs(10_000_000L, 2_500L))
        assertEquals(10_000_000L, DubbingTiming.presentationPositionUs(12_500_000L, 2_500L))
        assertEquals(0L, DubbingTiming.sourcePositionUs(1_000_000L, -2_500L))
        assertEquals(2_500_000L, DubbingTiming.presentationPositionUs(0L, -2_500L))
    }

    @Test
    fun `negative offset keeps requested seek inside initial silent gap`() {
        assertEquals(0L, DubbingTiming.resolvedSeekPositionUs(0L, 0L, -12_930L))
        assertEquals(5_000_000L, DubbingTiming.resolvedSeekPositionUs(5_000_000L, 0L, -12_930L))
        assertEquals(12_930_000L, DubbingTiming.resolvedSeekPositionUs(12_930_000L, 0L, -12_930L))
        assertEquals(20_000_000L, DubbingTiming.resolvedSeekPositionUs(20_000_000L, 7_070_000L, -12_930L))
    }

    @Test
    fun `current calibration revision restores saved offset`() {
        assertEquals(-12_970L, restoreDubbingOffset(DUBBING_CALIBRATION_REVISION, -12_970L))
    }

    @Test
    fun `old calibration revision cannot restore stale offset`() {
        assertEquals(0L, restoreDubbingOffset(DUBBING_CALIBRATION_REVISION - 1, 52_290L))
    }

    @Test
    fun `declared source identity survives signed url renewal`() {
        val first = stream(
            url = "https://broker.test/play/random-ticket-one",
            sourceIdentity = "cinevs-candidate-42",
        )
        val second = stream(
            url = "https://broker.test/play/random-ticket-two",
            sourceIdentity = "cinevs-candidate-42",
        )

        assertEquals(
            first.dubbingSourceIdentity(allowDisplayFallback = true),
            second.dubbingSourceIdentity(allowDisplayFallback = true),
        )
    }

    @Test
    fun `different declared candidates never share identity`() {
        val first = stream(sourceIdentity = "cinevs-candidate-42")
        val second = stream(sourceIdentity = "cinevs-candidate-43")

        assertNotEquals(
            first.dubbingSourceIdentity(allowDisplayFallback = true),
            second.dubbingSourceIdentity(allowDisplayFallback = true),
        )
    }

    @Test
    fun `display fallback is accepted only when unique in current source list`() {
        val selected = stream(sourceIdentity = null, name = "CineVS · Dublado", title = "1080p")
        val duplicate = stream(
            url = "https://broker.test/play/other-ticket",
            sourceIdentity = null,
            name = "CineVS · Dublado",
            title = "1080p",
        )

        assertEquals(
            selected.dubbingSourceIdentity(allowDisplayFallback = true)?.value,
            uniqueDubbingSourceIdentity(selected, listOf(selected)),
        )
        assertNull(uniqueDubbingSourceIdentity(selected, listOf(selected, duplicate)))
    }

    @Test
    fun `insufficient fallback metadata is not persisted`() {
        val source = stream(sourceIdentity = null, title = null)

        assertNull(source.dubbingSourceIdentity(allowDisplayFallback = true))
    }

    private fun stream(
        url: String = "https://broker.test/play/random-ticket",
        sourceIdentity: String? = "cinevs-candidate",
        name: String? = "CineVS · Dublado",
        title: String? = "1080p",
    ) = Stream(
        name = name,
        title = title,
        description = null,
        url = url,
        ytId = null,
        infoHash = null,
        fileIdx = null,
        externalUrl = null,
        behaviorHints = StreamBehaviorHints(
            notWebReady = true,
            bingeGroup = null,
            countryWhitelist = null,
            proxyHeaders = null,
            sourceIdentity = sourceIdentity,
        ),
        addonName = "SourceBroker · CineVS",
        addonLogo = null,
        addonId = "com.leiasaude.sourcebroker.cinevs",
        addonBaseUrl = "https://broker.test/stremio/cinevs",
    )
}
