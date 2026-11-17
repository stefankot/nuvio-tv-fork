package com.nuvio.tv.fork.tv.logo

import com.nuvio.tv.fork.tv.logo.forgeLogoInnerTile
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForgeLogoInnerTileTest {
    private val dark = (0xFF shl 24) or 0x202020
    private val white = (0xFF shl 24) or 0xFFFFFF
    private val clear = 0

    @Test
    fun `a white tile on a dark square is found`() {
        // 40x40 dark square, white tile from 10 to 29 on both axes, dark letter in the middle of the tile
        val box = forgeLogoInnerTile(40, 40) { x, y ->
            when {
                x in 18..21 && y in 18..21 -> dark
                x in 10..29 && y in 10..29 -> white
                else -> dark
            }
        }
        assertArrayEquals(intArrayOf(10, 10, 29, 29), box)
    }

    @Test
    fun `light letters on a dark tile are not a tile`() {
        // thin light letters crossing the middle row and column, corners of their box are dark
        val box = forgeLogoInnerTile(40, 40) { x, y -> if (x == 20 || y == 20) white else dark }
        assertNull(box)
    }

    @Test
    fun `a transparent logo is not a tile`() {
        assertNull(forgeLogoInnerTile(40, 40) { x, y -> if (x in 10..29 && y in 10..29) white else clear })
    }

    @Test
    fun `a small light patch is not a tile`() {
        assertNull(forgeLogoInnerTile(40, 40) { x, y -> if (x in 17..22 && y in 17..22) white else dark })
    }

    @Test
    fun `a rounded white tile is found (corners of the tile are still dark)`() {
        // tile 10..29 with 3 px corners cut away (a rounded tile)
        val box = forgeLogoInnerTile(40, 40) { x, y ->
            val inTile = x in 10..29 && y in 10..29
            val corner = (x < 13 || x > 26) && (y < 13 || y > 26)
            if (inTile && !corner) white else dark
        }
        assertArrayEquals(intArrayOf(10, 10, 29, 29), box)
    }
}
