package com.nuvio.tv.skin.niunia.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.nuvio.tv.skin.niunia.NiuniaLabels

/**
 * Ikony Niunii (SKIN.md, „Font everywhere = Satoshi”): Satoshi nie ma znaków ★ ▶ ▼ ► ◄ ‹ › ✓ ⌫ — spadały do fontu systemowego.
 * Dlatego gwiazdka oceny, strzałki, odtwarzanie, pauza i znaczniki są zawsze ikonami (wektor), nigdy znakami w tekście.
 */
enum class NiuniaGlyph(val icon: ImageVector) {
    Play(Icons.Filled.PlayArrow),
    Pause(Icons.Filled.Pause),
    Check(Icons.Filled.Check),
    Add(Icons.Filled.Add),
    Back(Icons.AutoMirrored.Filled.KeyboardArrowLeft),
    Next(Icons.AutoMirrored.Filled.KeyboardArrowRight),
    Up(Icons.Filled.KeyboardArrowUp),
    Down(Icons.Filled.KeyboardArrowDown),
    Star(Icons.Filled.Star),
    Backspace(Icons.AutoMirrored.Filled.Backspace),
    Subtitles(Icons.Outlined.ClosedCaption),
    Audio(Icons.Outlined.Headphones),
    Info(Icons.Outlined.Info),
    More(Icons.Filled.MoreHoriz),
    Restart(Icons.Filled.Replay),
}

@Composable
fun NiuniaGlyphIcon(glyph: NiuniaGlyph, tint: Color, size: Dp = 20.dp, modifier: Modifier = Modifier, description: String? = null) {
    Icon(glyph.icon, contentDescription = description, tint = tint, modifier = modifier.size(size))
}

/**
 * Linia „gatunek · ★ ocena” plakatu: gatunek w krótkiej polskiej formie ([NiuniaLabels.shortGenre]), gwiazdka jako ikona w tekście.
 * Przyjmuje tekst w formacie z [NiuniaLabels.meta] (części rozdzielone „ · ”, ocena po znaku ★).
 */
@Composable
fun NiuniaMetaText(meta: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val parts = meta.split(" · ").map { it.trim() }.filter { it.isNotEmpty() }
    val text = buildAnnotatedString {
        parts.forEachIndexed { i, part ->
            if (i > 0) append(" · ")
            if (part.startsWith("★")) {
                appendInlineContent("star", "*")
                append(" " + part.removePrefix("★").trim())
            } else append(if (i == 0) NiuniaLabels.shortGenre(context, part) else part)
        }
    }
    val inline = mapOf("star" to InlineTextContent(Placeholder(0.9.em, 0.9.em, PlaceholderVerticalAlign.TextCenter)) {
        Icon(NiuniaGlyph.Star.icon, contentDescription = null, tint = color)
    })
    Text(text, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, inlineContent = inline, modifier = modifier)
}
