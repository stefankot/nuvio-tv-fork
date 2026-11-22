package com.nuvio.tv.fork

import com.nuvio.tv.fork.images.ForkImageQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageQualityTest {
    @Test fun `duze obrazki TMDB w original, metahub tlo 1920`() {
        assertEquals("https://image.tmdb.org/t/p/original/abc.jpg", ForkImageQuality.upgrade("https://image.tmdb.org/t/p/w1280/abc.jpg", 1920))
        assertEquals("https://image.tmdb.org/t/p/original/x.png", ForkImageQuality.upgrade("https://image.tmdb.org/t/p/h632/x.png", 1920))
        assertEquals("https://images.metahub.space/background/medium/tt0137523/img", ForkImageQuality.upgrade("https://images.metahub.space/background/small/tt0137523/img", 1920))
        assertNull(ForkImageQuality.upgrade("https://images.metahub.space/background/medium/tt0137523/img", 1920)) // large = medium (1920 px)
        assertNull(ForkImageQuality.upgrade("https://image.tmdb.org/t/p/original/abc.jpg", 1920))
        assertNull(ForkImageQuality.upgrade("https://example.com/a.jpg", 1920))
    }

    @Test fun `plakaty w rzedach do rozmiaru na ekranie`() {
        // plakat 130 dp × 2 = 260 px → potrzeba 325 px
        assertEquals("https://images.metahub.space/poster/medium/tt1/img", ForkImageQuality.upgrade("https://images.metahub.space/poster/small/tt1/img", 260))
        assertNull(ForkImageQuality.upgrade("https://images.metahub.space/poster/medium/tt1/img", 260))
        assertEquals("https://images.metahub.space/poster/large/tt1/img", ForkImageQuality.upgrade("https://images.metahub.space/poster/medium/tt1/img", 500))
        assertNull(ForkImageQuality.upgrade("https://image.tmdb.org/t/p/w342/p.jpg", 260))
        assertEquals("https://image.tmdb.org/t/p/w500/p.jpg", ForkImageQuality.upgrade("https://image.tmdb.org/t/p/w185/p.jpg", 260))
        assertEquals("https://image.tmdb.org/t/p/w1280/p.jpg", ForkImageQuality.upgrade("https://image.tmdb.org/t/p/w780/p.jpg", 700))
    }
}
