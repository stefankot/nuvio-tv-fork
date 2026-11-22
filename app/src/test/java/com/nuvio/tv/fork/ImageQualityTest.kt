package com.nuvio.tv.fork

import com.nuvio.tv.fork.images.ForkImageQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageQualityTest {
    @Test fun `TMDB zawsze original`() {
        assertEquals("https://image.tmdb.org/t/p/original/abc.jpg", ForkImageQuality.tmdbOriginal("https://image.tmdb.org/t/p/w1280/abc.jpg"))
        assertEquals("https://image.tmdb.org/t/p/original/p.jpg", ForkImageQuality.tmdbOriginal("https://image.tmdb.org/t/p/w342/p.jpg"))
        assertEquals("https://image.tmdb.org/t/p/original/x.png", ForkImageQuality.tmdbOriginal("https://image.tmdb.org/t/p/h632/x.png"))
        assertNull(ForkImageQuality.tmdbOriginal("https://image.tmdb.org/t/p/original/abc.jpg"))
        assertNull(ForkImageQuality.tmdbOriginal("https://example.com/a.jpg"))
    }

    @Test fun `metahub - IMDb do TMDB i najwiekszy wariant jako zapas`() {
        assertEquals(Triple("poster", "tt0111161", "https://images.metahub.space/poster/large/tt0111161/img"),
            ForkImageQuality.metahub("https://images.metahub.space/poster/small/tt0111161/img"))
        assertEquals(Triple("background", "tt1", "https://images.metahub.space/background/medium/tt1/img"),
            ForkImageQuality.metahub("https://images.metahub.space/background/small/tt1/img"))
        assertEquals(Triple("background", "tt1", null), ForkImageQuality.metahub("https://images.metahub.space/background/medium/tt1/img"))
        assertNull(ForkImageQuality.metahub("https://images.metahub.space/logo/medium/tt1/img"))
    }
}
