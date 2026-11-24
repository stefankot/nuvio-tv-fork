#!/usr/bin/env python3
"""Runda 2, Z-1: kafelek programu TV bez kropek i ikony play, czerwony mini pasek na żywo, tytuł + krótki kanał w 2 liniach."""
import os
N = "/Users/milajovovich/Desktop/nuvio 2/wt-niunia-ke/app/src/main/java/com/nuvio/tv/skin/niunia"

open(f"{N}/components/NiuniaShortChannel.kt", "w").write('''package com.nuvio.tv.skin.niunia.components

/**
 * Krótka nazwa kanału (Z-1, makieta `Nuvio.shortChannel`): bez prefiksu kraju („PL:”, „PL |”, „[PL]”, „UK -”), bez końcówek jakości
 * (HD, FHD, UHD, SD, 4K, HEVC, H265, RAW, +1) i bez „ TV” na końcu, jeśli zostają ≥ 3 znaki. „PL: Trwam TV” → „Trwam”,
 * „PL | TVN 24 HD” → „TVN 24”, „[PL] Polsat FHD” → „Polsat”, „TV Puls” → bez zmian. Pełna nazwa zostaje w opisie dostępności.
 */
fun niuniaShortChannel(name: String): String {
    val orig = name.trim()
    var s = orig.replace(Regex("""^(\\[[A-Za-z]{2,3}\\]|[A-Za-z]{2,3}\\s*[:|]|[A-Za-z]{2,3}\\s+-)\\s*"""), "")
    val q = Regex("""\\s+(FHD|UHD|HD|SD|4K|HEVC|H\\.?265|RAW|\\+1)$""", RegexOption.IGNORE_CASE)
    while (q.containsMatchIn(s)) s = s.replace(q, "")
    val tv = Regex("""\\s+TV$""", RegexOption.IGNORE_CASE)
    if (tv.containsMatchIn(s) && s.replace(tv, "").length >= 3) s = s.replace(tv, "")
    return s.trim().ifEmpty { orig }
}

/** Podpis kafelka (Z-1): tytuł skrócony tak, żeby „ · kanał” zawsze się zmieścił w budżecie [chars] znaków na dwie linie. */
fun niuniaTileTitle(title: String, channel: String?, chars: Int = 32): String {
    if (channel.isNullOrBlank()) return title
    val room = chars - channel.length - 3
    return if (title.length <= room || room < 6) title else title.take(room - 1).trimEnd() + "…"
}
''')

p = f"{N}/components/NiuniaAtoms.kt"; s = open(p).read()
a = '''fun NiuniaProgress(value: Float, modifier: Modifier = Modifier, bar: Boolean = false, onFocus: Boolean = false) {
    val track = if (onFocus) NiuniaColor.OnFocus70 else NiuniaColor.Fill36
    val fill = if (onFocus) NiuniaColor.OnFocus else NiuniaColor.White'''
assert a in s
s = s.replace(a, '''fun NiuniaProgress(value: Float, modifier: Modifier = Modifier, bar: Boolean = false, onFocus: Boolean = false, fillColor: Color? = null) {
    val track = if (onFocus) NiuniaColor.OnFocus70 else NiuniaColor.Fill36
    // Z-1: kafelek programu na żywo ma czerwone wypełnienie (`live`), archiwum białe.
    val fill = fillColor ?: if (onFocus) NiuniaColor.OnFocus else NiuniaColor.White''')
open(p, "w").write(s)

p = f"{N}/livetv/NiuniaLiveTiles.kt"; s = open(p).read()
a = '''                NiuniaStatusDot(live = true)
                return@Box'''
assert a in s; s = s.replace(a, '''                // Z-1: bez kropek statusu.
                return@Box''')
a = '''            val dot = when (tile.mark) { NiuniaTileMark.LIVE -> true; NiuniaTileMark.ARCHIVE, NiuniaTileMark.RESUME -> false; else -> null }
            if (dot != null) NiuniaStatusDot(live = dot)'''
assert a in s; s = s.replace(a, '''            // Z-1: bez kropek statusu i ikony play; stan mówi mini pasek w podpisie (czerwony na żywo, biały obejrzane archiwum).
            val dot: Boolean? = null''')
a = '''                // Pasek podpisu NMediaCard: ▶ 12, mini postęp 24×4, podpis 12/16.
                if (tile.withChannel) com.nuvio.tv.skin.niunia.components.NiuniaIcon("play-small", size = 12.dp)
                tile.progress?.let { f -> com.nuvio.tv.skin.niunia.components.NiuniaProgress(f) }
                Text(caption(tile), style = com.nuvio.tv.skin.niunia.tokens.NiuniaType.Caption, color = NiuniaColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)'''
assert a in s; s = s.replace(a, '''                // Z-1: [mini pasek 24×4] + „Tytuł · Kanał” (tytuł caption 700, „· kanał” caption-regular 500), maks. 2 linie, kanał zawsze widoczny.
                val live = tile.mark == NiuniaTileMark.LIVE
                val bar = when { live -> tile.progress ?: 0f; (tile.progress ?: 0f) > 0f -> tile.progress; else -> null }
                if (bar != null) com.nuvio.tv.skin.niunia.components.NiuniaProgress(bar, fillColor = if (live) com.nuvio.tv.skin.niunia.tokens.NiuniaColor.Live else null)
                Text(tileCaption(tile), style = com.nuvio.tv.skin.niunia.tokens.NiuniaType.Caption, color = NiuniaColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { contentDescription = caption(tile) })''')
a = '''/** „program · E5 · kanał” (przewodnik) albo „program · Pt 20:15” (Odkrywaj — godzina tylko dla tego, co dopiero będzie). */'''
assert a in s; s = s.replace(a, '''/** Z-1: podpis na kafelku — „**Tytuł** · Kanał” z krótką nazwą kanału; tytuł skracany, żeby kanał został. */
internal fun tileCaption(tile: NiuniaLiveTile): androidx.compose.ui.text.AnnotatedString {
    val p = tile.programme ?: return androidx.compose.ui.text.AnnotatedString(com.nuvio.tv.skin.niunia.components.niuniaShortChannel(tile.channel.name))
    val ch = if (tile.withChannel) com.nuvio.tv.skin.niunia.components.niuniaShortChannel(tile.channel.name) else null
    val title = listOfNotNull(p.title, tile.extra?.takeIf { it.isNotBlank() }).joinToString(" · ")
    return androidx.compose.ui.text.buildAnnotatedString {
        append(com.nuvio.tv.skin.niunia.components.niuniaTileTitle(title, ch))
        if (ch != null) {
            pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
            append(" · $ch")
            pop()
        }
    }
}

/** „program · E5 · kanał” (przewodnik) albo „program · Pt 20:15” (Odkrywaj — godzina tylko dla tego, co dopiero będzie). */''')
if "import androidx.compose.ui.semantics.semantics" not in s:
    s = s.replace("import androidx.compose.ui.Modifier\n", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.semantics.contentDescription\nimport androidx.compose.ui.semantics.semantics\n", 1)
open(p, "w").write(s)
print("Z-1 nałożone")
