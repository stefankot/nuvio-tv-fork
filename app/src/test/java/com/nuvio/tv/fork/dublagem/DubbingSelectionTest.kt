package com.nuvio.tv.fork.dublagem

import org.junit.Assert.*
import org.junit.Test

class DubbingSelectionTest {
    @Test
    fun prefersTheBetterPolishProgramTrackWithoutTreatingCommentaryAsDialogue() {
        val stereo = DubbingAudioTrack(0, "pl-PL", "audio/mp4a-latm", 2)
        val surround = DubbingAudioTrack(1, "pol", "audio/eac3", 6)
        val commentary = DubbingAudioTrack(2, "pol", "audio/flac", 8, label = "Komentarz reżysera")
        val foreign = DubbingAudioTrack(3, "en", "audio/flac", 8, label = "PL Studio")
        val best = listOf(stereo, surround, commentary, foreign)
            .filter { it.isPolish && it.isMainProgram }
            .maxWithOrNull(DubbingAudioTrack.quality)
        assertEquals(surround, best)
    }

    @Test
    fun rejectsADifferentCutOrPlaybackTempo() {
        val movie = DubbingEdition(null, 120_000L, 23.976f)
        assertTrue(movie.matches(DubbingEdition(null, 121_000L, 23.976f)))
        assertFalse(movie.matches(DubbingEdition("EXTENDED", 120_000L, 23.976f)))
        assertFalse(movie.matches(DubbingEdition(null, 121_000L, 25f)))
        assertFalse(movie.matches(DubbingEdition(null, 121_000L, 24f)))
    }

    @Test
    fun matchingTwoIndependentOffsetsRejectsDriftAndOutOfWindowOffsets() {
        assertEquals(630L, matchingDubbingOffset(630L, 650L))
        assertNull(matchingDubbingOffset(630L, 1_030L))
        assertNull(matchingDubbingOffset(-3_100L, -3_140L))
    }
}
