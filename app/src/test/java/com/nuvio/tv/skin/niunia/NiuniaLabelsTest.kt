package com.nuvio.tv.skin.niunia

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.skin.niunia.components.niuniaTabTextColor
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Etykiety plakatów Niuni (SKIN.md, WPIECIE.md §5): „#n Dziś”, „Nowość”, „Klasyka”, kolor tekstu etykiety z jasności. */
class NiuniaLabelsTest {
    private val ctx: Context = mockk {
        every { getString(R.string.niunia_label_rank, *anyVararg()) } answers { "#${(it.invocation.args[1] as Array<*>)[0]} Dziś" }
        every { getString(R.string.niunia_label_new) } returns "Nowość"
        every { getString(R.string.niunia_label_classic) } returns "Klasyka"
    }

    private fun item(released: String? = null, releaseInfo: String? = null) = MetaPreview(
        id = "tt1", type = ContentType.MOVIE, name = "X", poster = null, posterShape = PosterShape.POSTER, background = null, logo = null,
        description = null, releaseInfo = releaseInfo, imdbRating = null, genres = emptyList(), released = released,
    )

    @Test fun `ranking pierwsze dziesiec`() {
        assertEquals("#1 Dziś", NiuniaLabels.label(ctx, item(), 0, rankingRow = true))
        assertEquals("#10 Dziś", NiuniaLabels.label(ctx, item(), 9, rankingRow = true))
        assertNull(NiuniaLabels.label(ctx, item(), 10, rankingRow = true))
    }

    @Test fun `nowosc do 45 dni`() {
        assertEquals("Nowość", NiuniaLabels.label(ctx, item(released = LocalDate.now().minusDays(10).toString()), 0, false))
        assertEquals("Nowość", NiuniaLabels.label(ctx, item(released = LocalDate.now().minusDays(45).toString()), 0, false))
        assertNull(NiuniaLabels.label(ctx, item(released = LocalDate.now().minusDays(46).toString(), releaseInfo = "2025"), 0, false))
        assertNull(NiuniaLabels.label(ctx, item(released = LocalDate.now().plusDays(3).toString(), releaseInfo = "2026"), 0, false)) // przyszła premiera
    }

    @Test fun `klasyka przed 2000`() {
        assertEquals("Klasyka", NiuniaLabels.label(ctx, item(releaseInfo = "1999"), 0, false))
        assertNull(NiuniaLabels.label(ctx, item(releaseInfo = "2000"), 0, false))
        assertEquals("#3 Dziś", NiuniaLabels.label(ctx, item(releaseInfo = "1985"), 2, rankingRow = true)) // ranking ma pierwszeństwo
    }

    @Test fun `kolor tekstu etykiety z jasnosci tla`() {
        assertEquals(Color(0xFF111111), niuniaTabTextColor(Color(0xFF9BD4ED))) // jasny błękit (makieta: tab-text #111)
        assertEquals(Color(0xFF111111), niuniaTabTextColor(Color(0xFFF3FDEF)))
        assertEquals(Color(0xFFFFFFFF), niuniaTabTextColor(Color(0xFF203650))) // ciemny granat (makieta: tab-text #fff)
        assertEquals(Color(0xFFFFFFFF), niuniaTabTextColor(Color(0xFF2E120A)))
    }
}
