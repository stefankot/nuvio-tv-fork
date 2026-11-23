package com.nuvio.tv.fork.ai.subtitles

import androidx.media3.extractor.text.CuesWithTiming
import com.nuvio.tv.data.local.SubtitleStyleSettings
import com.nuvio.tv.domain.model.Subtitle
import com.nuvio.tv.fork.sources.EmbeddedSourceSubtitles
import com.nuvio.tv.ui.screens.player.PlayerRuntimeController
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.TrackInfo
import com.nuvio.tv.ui.screens.player.currentSidecarGenerationFor
import com.nuvio.tv.ui.screens.player.disableSubtitles
import com.nuvio.tv.ui.screens.player.hasBurnedInServerSubtitle
import com.nuvio.tv.ui.screens.player.isUsingMpvEngine
import com.nuvio.tv.ui.screens.player.selectSubtitleTrack
import com.nuvio.tv.ui.screens.player.startSidecarAddonSubtitle
import com.nuvio.tv.ui.screens.player.tryAutoSelectPreferredSubtitleFromAvailableTracks
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Zgłoszenie 23.11: „komunikat, że napisy zostały przetłumaczone, a napisów nie ma”. Od 21.11 automat wyboru napisów
 * bazy najpierw wybiera ścieżkę wbudowaną; przy zmianie ścieżek po publikacji napisów AI wybierał z powrotem wbudowane
 * (tu: angielskie, drugi preferowany język) i zatrzymywał napisy AI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiSubtitlesStayOnScreenTest {
    private lateinit var controller: PlayerRuntimeController
    private lateinit var state: MutableStateFlow<PlayerUiState>
    private var explicit = false
    private var autoSelected = true
    private var generation: Long? = 7L
    private var loadedCues: List<CuesWithTiming> = emptyList()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(
            "com.nuvio.tv.ui.screens.player.PlayerRuntimeControllerTrackSelectionKt",
            "com.nuvio.tv.ui.screens.player.PlayerSidecarSubtitlesKt",
            "com.nuvio.tv.ui.screens.player.PlayerRuntimeControllerServerKt",
            "com.nuvio.tv.ui.screens.player.PlayerRuntimeControllerMpvKt",
        )
        mockkObject(EmbeddedSourceSubtitles)
        controller = mockk(relaxed = true)
        state = MutableStateFlow(
            PlayerUiState(
                subtitleTracks = listOf(TrackInfo(index = 0, name = "English", language = "en")),
                selectedSubtitleTrackIndex = 0,
                subtitleStyle = SubtitleStyleSettings(preferredLanguage = "pl", secondaryPreferredLanguage = "en", useForcedSubtitles = false),
            )
        )
        every { controller._uiState } returns state
        every { controller._exoPlayer } returns mockk(relaxed = true)
        every { controller.currentVideoId } returns "tt0000001"
        every { controller.contentId } returns "tt0000001"
        every { controller.isUserExplicitSubtitleSelection } answers { explicit }
        every { controller.isUserExplicitSubtitleSelection = any() } answers { explicit = firstArg(); Unit }
        every { controller.autoSubtitleSelected } answers { autoSelected }
        every { controller.autoSubtitleSelected = any() } answers { autoSelected = firstArg(); Unit }
        every { controller.subtitleDisabledByPersistedPreference } returns false
        every { controller.hasScannedTextTracksOnce } returns true
        every { controller.sidecarTimedCues } answers { loadedCues }
        every { controller.hasBurnedInServerSubtitle } returns false
        every { controller.isUsingMpvEngine() } returns false
        every { controller.disableSubtitles() } just Runs
        every { controller.selectSubtitleTrack(any()) } answers {
            state.value = state.value.copy(selectedSubtitleTrackIndex = firstArg(), selectedAddonSubtitle = null)
        }
        every { controller.startSidecarAddonSubtitle(any(), any()) } returns true
        every { controller.currentSidecarGenerationFor(any()) } answers { generation }
        every { EmbeddedSourceSubtitles.select(any(), any(), any()) } returns false
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private val cues = listOf(DocCue("c00001", 1_000, 2_000, "Cześć"), DocCue("c00002", 3_000, 4_000, "Jak leci?"))

    @Test
    fun `bez oznaczenia jako wybór użytkownika automat bazy zastępuje napisy AI wbudowanymi`() {
        // Stan sprzed poprawki: napisy AI wybrane, ale flaga ręcznego wyboru nieustawiona.
        val ai = Subtitle(id = "ai_pl_x", url = "ai://pl/x.srt", lang = "pl", addonName = "AI PL", addonLogo = null)
        state.value = state.value.copy(selectedAddonSubtitle = ai, selectedSubtitleTrackIndex = -1)

        controller.tryAutoSelectPreferredSubtitleFromAvailableTracks() // zmiana ścieżek po disableSubtitles()

        verify(exactly = 1) { controller.selectSubtitleTrack(0) }
        assertNull("napisy AI zniknęły", state.value.selectedAddonSubtitle)
    }

    @Test
    fun `po publikacji napisy AI zostają mimo zmiany ścieżek`() = runBlocking {
        val port = ExoPlayerPort(controller) { "doc1" }
        assertEquals(PublishResult.OK, port.publish(cues, "AI PL"))
        assertTrue("napisy AI to wybór użytkownika", explicit)

        controller.tryAutoSelectPreferredSubtitleFromAvailableTracks()

        verify(exactly = 0) { controller.selectSubtitleTrack(any()) }
        assertEquals("ai://pl/doc1.srt", state.value.selectedAddonSubtitle?.url)
        assertEquals(-1, state.value.selectedSubtitleTrackIndex)
    }

    @Test
    fun `na ekranie tylko gdy odtwarzacz ma kwestie AI`() = runBlocking {
        val port = ExoPlayerPort(controller) { "doc1" }
        assertFalse("przed publikacją nic nie ma", port.showing())
        port.publish(cues, "AI PL")
        loadedCues = listOf(CuesWithTiming(emptyList(), 1_000_000, 1_000_000))
        assertTrue(port.showing())

        generation = 8L // inne napisy zajęły miejsce
        assertFalse(port.showing())
        assertEquals(PublishResult.USER_SWITCHED, port.publish(cues, "AI PL"))
    }
}
