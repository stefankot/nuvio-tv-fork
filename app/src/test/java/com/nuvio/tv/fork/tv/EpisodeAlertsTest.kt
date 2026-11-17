package com.nuvio.tv.fork.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeAlertsTest {
    private val hour = 3_600_000L
    private val day = 24 * hour
    private val now = 1_800_000_000_000L
    private val channels = { key: String -> if (key == "arch") AlertChannel("TVN", 7) else AlertChannel("TVP", 0) }

    private fun airing(title: String, start: Long, desc: String?, key: String = "g") =
        Airing(key, title, start, start + hour, desc)

    private fun watched(title: String, seen: List<String>, viewedAt: Long = now - hour) = RecentShow(
        key = ShowIdentity.normalizeTitle(title), title = title, channelName = "TVP", channelUrl = "u",
        guideKey = "g", lastViewedMs = viewedAt, seenEpisodes = seen,
    )

    private val episodeA = "Pod mostem w Sacramento znaleziono ciało asystentki senator."
    private val episodeB = "CBI otrzymuje anonimową informację o znalezieniu zwłok niejakiego McTeera."

    @Test fun anEpisodeWithItsOwnDescriptionIsNewWhenNotSeen() {
        val seen = ShowIdentity.episodeId("Mentalista", episodeA)
        val alerts = EpisodeAlerts.detect(
            listOf(watched("Mentalista", listOf(seen))),
            listOf(airing("Mentalista", now + 2 * day, episodeA), airing("Mentalista", now + 3 * day, episodeB)),
            channels, now,
        )
        assertEquals(1, alerts.size)
        assertEquals(AlertKind.NEW_EPISODE, alerts.single().kind)
        assertEquals(now + 3 * day, alerts.single().airing.startMs)
    }

    @Test fun theSameEpisodeOnTwoChannelsIsOneAlert() {
        val alerts = EpisodeAlerts.detect(
            listOf(watched("Mentalista", listOf(ShowIdentity.episodeId("Mentalista", episodeA)))),
            listOf(airing("Mentalista", now + 3 * day, episodeB, "g1"), airing("Mentalista", now + 4 * day, episodeB, "g2")),
            channels, now,
        )
        assertEquals(1, alerts.size)
        assertEquals(now + 3 * day, alerts.single().airing.startMs)
    }

    @Test fun aMarkerTellsEpisodesApartEvenWithoutDescriptions() {
        val alerts = EpisodeAlerts.detect(
            listOf(watched("Wojna domowa", listOf("e11"))),
            listOf(airing("Wojna domowa (11)", now + day, null), airing("Wojna domowa (12)", now + 2 * day, null)),
            channels, now,
        )
        assertEquals(listOf("e12"), alerts.map { it.id.substringAfter('|') })
    }

    @Test fun aShowWithOneBlurbEverywhereIsNeverCalledNew() {
        val blurb = "Program skierowany do widzów, którzy lubią poznawać nowe miejsca i kraje."
        val alerts = EpisodeAlerts.detect(
            listOf(watched("Makłowicz w podróży", emptyList())),
            listOf(airing("Makłowicz w podróży", now + hour, blurb), airing("Makłowicz w podróży", now + day, blurb)),
            channels, now,
        )
        assertEquals(1, alerts.size)
        assertEquals(AlertKind.UPCOMING, alerts.single().kind)
        assertEquals(now + hour, alerts.single().airing.startMs)
    }

    @Test fun missedEpisodesCountOnlyOnChannelsWithAnArchive() {
        val seen = listOf(ShowIdentity.episodeId("Mentalista", episodeA))
        val past = now - 2 * day
        val inArchive = EpisodeAlerts.detect(listOf(watched("Mentalista", seen)),
            listOf(airing("Mentalista", past, episodeB, "arch"), airing("Mentalista", now + day, episodeA)), channels, now)
        assertEquals(AlertKind.MISSED_IN_ARCHIVE, inArchive.single().kind)
        val noArchive = EpisodeAlerts.detect(listOf(watched("Mentalista", seen)),
            listOf(airing("Mentalista", past, episodeB, "g"), airing("Mentalista", now + day, episodeA)), channels, now)
        assertTrue(noArchive.isEmpty())
    }

    @Test fun airingsBeyondAWeekAreLeftOut() {
        val alerts = EpisodeAlerts.detect(
            listOf(watched("Mentalista", listOf(ShowIdentity.episodeId("Mentalista", episodeA)))),
            listOf(airing("Mentalista", now + 9 * day, episodeB), airing("Mentalista", now + day, episodeA)), channels, now,
        )
        assertTrue(alerts.isEmpty())
    }

    @Test fun otherShowsAndNothingWatchedGiveNothing() {
        assertTrue(EpisodeAlerts.detect(emptyList(), listOf(airing("X", now + hour, episodeA)), channels, now).isEmpty())
        assertTrue(EpisodeAlerts.detect(listOf(watched("Mentalista", emptyList())), listOf(airing("Inny serial", now + hour, episodeA)), channels, now).isEmpty())
    }

    @Test fun announcedAlertsAreNotRepeated() {
        val alerts = EpisodeAlerts.detect(
            listOf(watched("Wojna domowa", listOf("e11"))),
            listOf(airing("Wojna domowa (12)", now + day, null), airing("Wojna domowa (11)", now + hour, null)), channels, now,
        )
        assertEquals(1, alerts.size)
        assertTrue(EpisodeAlerts.fresh(alerts, alerts.map { it.id }.toSet()).isEmpty())
    }
}
