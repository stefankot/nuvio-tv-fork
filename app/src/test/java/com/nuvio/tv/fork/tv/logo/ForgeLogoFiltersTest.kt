package com.nuvio.tv.fork.tv.logo

import com.nuvio.tv.fork.tv.logo.ForgeLogoFilters
import com.nuvio.tv.fork.tv.logo.ForgeLogoMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeLogoFiltersTest {
    private val eps = 0.01f

    @Test
    fun `knockout turns dark and coloured pixels white and keeps white pixels clear`() {
        // opaque black -> fully visible
        assertEquals(1f, ForgeLogoFilters.alphaFor(ForgeLogoMode.Knockout, 0f, 0f, 0f, 1f), eps)
        // opaque white -> transparent (cut out)
        assertEquals(0f, ForgeLogoFilters.alphaFor(ForgeLogoMode.Knockout, 1f, 1f, 1f, 1f), eps)
        // opaque blue field -> visible
        assertTrue(ForgeLogoFilters.alphaFor(ForgeLogoMode.Knockout, 0f, 0.2f, 0.8f, 1f) > 0.9f)
        // fully transparent stays transparent
        assertEquals(0f, ForgeLogoFilters.alphaFor(ForgeLogoMode.Knockout, 0f, 0f, 0f, 0f), eps)
    }

    @Test
    fun `silhouette keeps the original shape`() {
        assertEquals(0.5f, ForgeLogoFilters.alphaFor(ForgeLogoMode.Silhouette, 0.3f, 0.3f, 0.3f, 0.5f), eps)
        assertEquals(0f, ForgeLogoFilters.alphaFor(ForgeLogoMode.Silhouette, 1f, 1f, 1f, 0f), eps)
    }

    @Test
    fun `light keeps white parts and drops the coloured field`() {
        assertEquals(1f, ForgeLogoFilters.alphaFor(ForgeLogoMode.Light, 1f, 1f, 1f, 1f), eps)
        assertEquals(0f, ForgeLogoFilters.alphaFor(ForgeLogoMode.Light, 0f, 0.2f, 0.8f, 1f), eps)
    }

    @Test
    fun `matrices are 4 by 5 and always output white`() {
        ForgeLogoMode.entries.forEach { mode ->
            val m = ForgeLogoFilters.matrix(mode)
            assertEquals(20, m.size)
            // R, G, B rows: no input terms, constant 255
            for (row in 0..2) {
                for (col in 0..3) assertEquals(0f, m[row * 5 + col], 0f)
                assertEquals(255f, m[row * 5 + 4], 0f)
            }
        }
    }

    @Test
    fun `matrix coefficients are the ones from the spec`() {
        val k = ForgeLogoFilters.matrix(ForgeLogoMode.Knockout)
        assertEquals(listOf(-0.680f, -2.289f, -0.231f, 3.2f, 0f), k.slice(15..19))
        val l = ForgeLogoFilters.matrix(ForgeLogoMode.Light)
        assertEquals(listOf(0.680f, 2.289f, 0.231f, 1f), l.slice(15..18))
        assertEquals(-816f, l[19], 0.001f)
    }
}
