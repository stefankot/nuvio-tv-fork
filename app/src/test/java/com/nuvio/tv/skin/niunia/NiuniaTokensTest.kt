package com.nuvio.tv.skin.niunia

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nuvio.tv.skin.niunia.tokens.NIUNIA_DS_VERSION
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaMotion
import com.nuvio.tv.skin.niunia.tokens.NiuniaScale
import com.nuvio.tv.skin.niunia.tokens.NiuniaTokenIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tokeny Niuni = design-system/tokens.json (kopię w test/resources/niunia zapisuje fork-tools/niunia/gen_tokens.py).
 * Sprawdza każdą wartość koloru, odstępu, promienia, rozmiaru i stylu tekstu oraz skale z README design systemu.
 */
class NiuniaTokensTest {
    private val json: JsonObject by lazy {
        val stream = javaClass.classLoader!!.getResourceAsStream("niunia/tokens.json")
        assertNotNull("brak test/resources/niunia/tokens.json — uruchom fork-tools/niunia/gen_tokens.py", stream)
        JsonParser.parseReader(stream!!.reader()).asJsonObject
    }

    private fun tokens(group: String) = json.getAsJsonObject(group).getAsJsonArray("tokens").map { it.asJsonObject }
    private fun px(v: String) = v.trim().removeSuffix("px").toFloat()

    private fun cssColor(v: String, all: Map<String, String>): Int {
        Regex("""\{(.+)\}""").matchEntire(v)?.let { return cssColor(all.getValue(it.groupValues[1]), all) }
        Regex("""#([0-9a-fA-F]{6})""").matchEntire(v)?.let { return (0xFF000000 or it.groupValues[1].toLong(16)).toInt() }
        val m = Regex("""rgba?\(([^)]*)\)""").matchEntire(v) ?: error("kolor $v")
        val n = m.groupValues[1].split(",").map { it.trim() }
        val a = if (n.size > 3) Math.round(n[3].toFloat() * 255) else 255
        return (a shl 24) or (n[0].toInt() shl 16) or (n[1].toInt() shl 8) or n[2].toInt()
    }

    @Test fun `wersja design systemu`() {
        assertEquals(json.get("version").asString, NIUNIA_DS_VERSION)
    }

    @Test fun `kazdy kolor rowny tokens json`() {
        val raw = tokens("color").associate { it.get("name").asString to it.get("value").asString }
        assertEquals("liczba kolorów", raw.size, NiuniaTokenIndex.colors.size)
        raw.forEach { (name, value) ->
            val kotlin = NiuniaTokenIndex.colors[name] ?: error("brak koloru $name w NiuniaColor")
            assertEquals("kolor $name", cssColor(value, raw), kotlin.toArgb())
        }
    }

    @Test fun `odstepy promienie i rozmiary rowne tokens json`() {
        for ((group, map) in listOf("spacing" to NiuniaTokenIndex.spacing, "radius" to NiuniaTokenIndex.radius, "size" to NiuniaTokenIndex.size)) {
            val t = tokens(group)
            assertEquals("liczba $group", t.size, map.size)
            t.forEach { o -> assertEquals("$group ${o.get("name").asString}", px(o.get("value").asString), map.getValue(o.get("name").asString).value, 0.001f) }
        }
    }

    @Test fun `style tekstu rowne tokens json`() {
        val styles = json.getAsJsonObject("type").getAsJsonArray("groups").flatMap { it.asJsonObject.getAsJsonArray("styles").map { s -> s.asJsonObject } }
        assertEquals(styles.size, NiuniaTokenIndex.type.size)
        styles.forEach { s ->
            val k = NiuniaTokenIndex.type.getValue(s.get("name").asString)
            assertEquals("rozmiar ${s.get("name")}", px(s.get("fontSize").asString), k.fontSize.value, 0.001f)
            assertEquals("linia ${s.get("name")}", px(s.get("lineHeight").asString), k.lineHeight.value, 0.001f)
            assertEquals("grubość ${s.get("name")}", s.get("fontWeight").asInt, k.fontWeight!!.weight)
        }
    }

    @Test fun `wartosci na skalach`() {
        val spacing = setOf(4f, 8f, 12f, 16f, 20f, 24f, 40f)
        NiuniaTokenIndex.spacing.values.forEach { assertTrue("odstęp ${it.value} poza skalą", it.value in spacing) }
        val radii = setOf(6f, 16f, 28f, 999f)
        NiuniaTokenIndex.radius.values.forEach { assertTrue("promień ${it.value} poza skalą", it.value in radii) }
        val text = setOf(12f, 14f, 16f, 18f, 20f, 32f, 40f)
        NiuniaTokenIndex.type.values.forEach { assertTrue("tekst ${it.fontSize.value} poza skalą", it.fontSize.value in text) }
        listOf("h-36", "h-44", "h-56").forEach { assertTrue(NiuniaTokenIndex.size.containsKey(it)) }
    }

    @Test fun `ruch i skale fokusu`() {
        assertEquals(200f, NiuniaMotion.DurFocus.toFloat(), 0f)
        assertEquals(520f, NiuniaMotion.LongPress.toFloat(), 0f)
        assertEquals(1.1f, NiuniaScale.ScaleFocus, 0f)
        assertEquals(1.08f, NiuniaScale.ScaleButton, 0f)
        assertEquals(0.97f, NiuniaScale.ScalePress, 0f)
        assertEquals(-4f, NiuniaScale.LiftFocus.value, 0f)
    }

    // ------------------------------------------------------------------ kontrast (WCAG 2.x)

    private fun lin(c: Float) = if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    private fun lum(c: Color) = 0.2126f * lin(c.red) + 0.7152f * lin(c.green) + 0.0722f * lin(c.blue)
    private fun over(fg: Color, bg: Color) = Color(fg.red * fg.alpha + bg.red * (1 - fg.alpha), fg.green * fg.alpha + bg.green * (1 - fg.alpha), fg.blue * fg.alpha + bg.blue * (1 - fg.alpha))
    private fun contrast(fg: Color, bg: Color, base: Color = NiuniaColor.Bg): Float {
        val b = over(bg, base); val f = over(fg, b)
        val l1 = lum(f); val l2 = lum(b)
        return (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
    }

    @Test fun `pary tekst tlo co najmniej 4,5 do 1`() {
        val pairs = listOf(
            "text/bg" to (NiuniaColor.Text to NiuniaColor.Bg),
            "text-70/bg" to (NiuniaColor.Text70 to NiuniaColor.Bg),
            "text-50/bg" to (NiuniaColor.Text50 to NiuniaColor.Bg),
            "text-70/surface" to (NiuniaColor.Text70 to NiuniaColor.Surface),
            "text/surface" to (NiuniaColor.Text to NiuniaColor.Surface),
            "on-focus/focus" to (NiuniaColor.OnFocus to NiuniaColor.Focus),
            "on-focus-70/focus" to (NiuniaColor.OnFocus70 to NiuniaColor.Focus),
            "white/live-badge" to (NiuniaColor.White to NiuniaColor.LiveBadge),
            "on-delay/delay" to (NiuniaColor.OnDelay to NiuniaColor.Delay),
            "on-fmt/fmt" to (NiuniaColor.OnFmt to NiuniaColor.Fmt),
            "on-avatar/avatar" to (NiuniaColor.OnAvatar to NiuniaColor.Avatar),
            "error/bg" to (NiuniaColor.Error to NiuniaColor.Bg),
            "text/panel" to (NiuniaColor.Text to NiuniaColor.Panel),
            "text-70/panel" to (NiuniaColor.Text70 to NiuniaColor.Panel),
            "white/button" to (NiuniaColor.White to NiuniaColor.Button),
        )
        pairs.forEach { (name, p) -> val c = contrast(p.first, p.second); assertTrue("$name = ${"%.2f".format(c)}:1", c >= 4.5f) }
    }

    @Test fun `znacznik LIVE ma live-badge a nie live`() {
        assertTrue(contrast(NiuniaColor.White, NiuniaColor.Live) < 4.5f)
        assertTrue(contrast(NiuniaColor.White, NiuniaColor.LiveBadge) >= 4.5f)
    }
}
