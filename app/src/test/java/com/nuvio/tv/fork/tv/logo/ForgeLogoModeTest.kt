package com.nuvio.tv.fork.tv.logo

import com.nuvio.tv.fork.tv.logo.ForgeLogoMode
import com.nuvio.tv.fork.tv.logo.forgeLogoModeFor
import org.junit.Assert.assertEquals
import org.junit.Test

class ForgeLogoModeTest {
    private fun px(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun `a transparent logo keeps the knockout filter`() {
        assertEquals(ForgeLogoMode.Knockout, forgeLogoModeFor(IntArray(4) { 0 }))
    }

    @Test
    fun `a white plate keeps the knockout filter (the plate disappears)`() {
        assertEquals(ForgeLogoMode.Knockout, forgeLogoModeFor(IntArray(4) { px(255, 255, 255, 255) }))
    }

    @Test
    fun `a coloured or dark tile uses the light filter (white letters stay, the tile disappears)`() {
        assertEquals(ForgeLogoMode.Light, forgeLogoModeFor(IntArray(4) { px(255, 0, 60, 160) }))
        assertEquals(ForgeLogoMode.Light, forgeLogoModeFor(IntArray(4) { px(255, 20, 20, 20) }))
    }

    @Test
    fun `one coloured corner is not a tile`() {
        assertEquals(ForgeLogoMode.Knockout, forgeLogoModeFor(intArrayOf(px(255, 0, 0, 200), 0, 0, 0)))
    }

    @Test
    fun `a rounded dark tile (transparent corners, dark 20 percent in) uses the light filter`() {
        val clear = px(0, 0, 0, 0); val dark = px(255, 20, 40, 120)
        assertEquals(ForgeLogoMode.Light, forgeLogoModeFor(intArrayOf(clear, clear, clear, clear, dark, dark, dark, dark)))
    }

    @Test
    fun `a transparent logo with a coloured mark keeps the knockout filter`() {
        val clear = px(0, 0, 0, 0); val dark = px(255, 20, 40, 120)
        assertEquals(ForgeLogoMode.Knockout, forgeLogoModeFor(intArrayOf(clear, clear, clear, clear, dark, clear, dark, clear)))
    }
}
