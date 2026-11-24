package com.nuvio.tv.fork.subfonts

import com.nuvio.tv.core.player.isSupportedSubtitleFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

/** Test 24.11: dołączone czcionki napisów przechodzą kontrolę importu Nuvio i mają polskie litery. */
class BundledSubtitleFontsTest {
    private fun bytes(f: BundledSubtitleFonts.Bundled) = File("src/main/assets/" + f.asset).readBytes()

    @Test fun `Atkinson pierwsza na liście`() {
        assertEquals(BundledSubtitleFonts.ATKINSON, BundledSubtitleFonts.ALL.first())
        assertEquals(listOf("Atkinson Hyperlegible", "Tiresias"), BundledSubtitleFonts.ALL.map { it.name })
    }

    @Test fun `import Nuvio przyjmuje pliki`() {
        BundledSubtitleFonts.ALL.forEach { f -> assertTrue(f.name, isSupportedSubtitleFont(f.name + ".ttf", bytes(f))) }
    }

    @Test fun `polskie litery w obu czcionkach`() {
        BundledSubtitleFonts.ALL.forEach { f ->
            val cps = cmap(bytes(f))
            val missing = "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ".filter { it.code !in cps }
            assertEquals("${f.name}: brak liter", "", missing)
        }
    }

    /** Znaki z tabeli `cmap` (formaty 4 i 12). */
    private fun cmap(b: ByteArray): Set<Int> {
        val buf = ByteBuffer.wrap(b)
        fun u16(o: Int) = buf.getShort(o).toInt() and 0xFFFF
        fun u32(o: Int) = buf.getInt(o).toLong() and 0xFFFFFFFFL
        val tables = (0 until u16(4)).associate { i ->
            String(b, 12 + i * 16, 4, Charsets.ISO_8859_1) to u32(12 + i * 16 + 8).toInt()
        }
        val off = tables.getValue("cmap")
        val out = HashSet<Int>()
        for (i in 0 until u16(off + 2)) {
            val s = off + u32(off + 4 + i * 8 + 4).toInt()
            when (u16(s)) {
                4 -> {
                    val seg = u16(s + 6) / 2
                    for (k in 0 until seg) {
                        val end = u16(s + 14 + k * 2)
                        val start = u16(s + 16 + seg * 2 + k * 2)
                        if (start != 0xFFFF) out.addAll(start..end)
                    }
                }
                12 -> for (g in 0 until u32(s + 12).toInt()) out.addAll(u32(s + 16 + g * 12).toInt()..u32(s + 20 + g * 12).toInt())
            }
        }
        return out
    }
}
