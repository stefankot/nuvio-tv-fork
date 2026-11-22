package com.nuvio.tv.skin.niunia

import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.skin.api.SkinId
import com.nuvio.tv.skin.niunia.components.niuniaPortrait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NiuniaLogicTest {
    private val metahub = "https://images.metahub.space/poster/medium/tt0137523/img"
    private fun item(shape: PosterShape = PosterShape.POSTER, poster: String? = "https://img/p.jpg", raw: String? = null, genres: List<String> = listOf("Dramat"), rating: Float? = 7.44f) =
        MetaPreview(id = "tt0137523", type = ContentType.MOVIE, name = "Fight Club", poster = poster, posterShape = shape, background = null, logo = null,
            description = null, releaseInfo = "1999", imdbRating = rating, genres = genres, rawPosterUrl = raw)

    @Test fun `skorka z klucza`() {
        assertEquals(SkinId.NIUNIA, SkinId.fromKey("niunia"))
        assertEquals(SkinId.KICIA, SkinId.fromKey("kicia"))
        assertEquals(SkinId.NUVIO, SkinId.fromKey(null))
        assertEquals(SkinId.NUVIO, SkinId.fromKey("cos-innego"))
    }

    @Test fun `rzad rankingowy`() {
        assertTrue(NiuniaLabels.isRankingRow("Popular on Nuvio"))
        assertTrue(NiuniaLabels.isRankingRow("Trending Movies"))
        assertTrue(NiuniaLabels.isRankingRow("Na czasie"))
        assertFalse(NiuniaLabels.isRankingRow("Klasyka"))
    }

    @Test fun `linia gatunek i ocena`() {
        assertEquals("Dramat · ★ 7.4", NiuniaLabels.meta(item()))
        assertEquals("Dramat", NiuniaLabels.meta(item(rating = 0f)))
        assertEquals("★ 7.4", NiuniaLabels.meta(item(genres = emptyList())))
        assertNull(NiuniaLabels.meta(item(genres = emptyList(), rating = null)))
    }

    @Test fun `plakat pionowy bez wzoru zostaje z zapasem`() {
        assertEquals("https://img/p.jpg" to "https://raw/p.jpg", niuniaPortrait(item(raw = "https://raw/p.jpg"), ""))
        // 22.11: zapas = plakat Cinemeta po IMDb, gdy nie ma osobnego oryginału
        assertEquals("https://img/p.jpg" to metahub, niuniaPortrait(item(), "https://x/{imdb_id}.jpg"))
        assertEquals("brak plakatu (biblioteka Trakt/MDBList) → Cinemeta", metahub to null, niuniaPortrait(item(poster = null), ""))
    }

    @Test fun `pozycja pozioma dostaje pionowy plakat ze wzoru`() {
        val (art, fallback) = niuniaPortrait(item(shape = PosterShape.LANDSCAPE, poster = "https://img/wide.jpg"), "https://posters.example/{imdb_id}.jpg")
        assertEquals("https://posters.example/tt0137523.jpg", art)
        assertEquals("zapas: pionowy z Cinemeta, nie wycinek z poziomego", metahub, fallback)
    }

    @Test fun `pozycja pozioma bez wzoru - pionowy plakat Cinemeta, potem oryginal`() {
        assertEquals(metahub to "https://img/wide.jpg", niuniaPortrait(item(shape = PosterShape.LANDSCAPE, poster = "https://img/wide.jpg"), ""))
        assertEquals("prawdziwy pionowy oryginał przed Cinemeta", "https://raw/p.jpg" to metahub,
            niuniaPortrait(item(shape = PosterShape.LANDSCAPE, poster = "https://img/wide.jpg", raw = "https://raw/p.jpg"), ""))
    }
}
