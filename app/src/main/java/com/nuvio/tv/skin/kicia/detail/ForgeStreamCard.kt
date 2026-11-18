package com.nuvio.tv.skin.kicia.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.fork.sources.ReadyPick
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/**
 * A source as the mockup lists it (b14): "4K · Dolby Vision · REMUX" over "TrueHD Atmos 7.1 · 70,8 GB · AIOStreams",
 * a ✓ on the one Play takes; white when focused. Same inputs as Nuvio's stream card.
 */
@Composable
fun ForgeStreamCard(
    stream: Stream,
    fallbackName: String,
    best: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
    onFocusChanged: ((Boolean) -> Unit)?,
    onUpKey: (() -> Unit)?,
    /** In the player's panel (wide): all five badges; the list before playing fits three. */
    wide: Boolean = false,
) {
    // makieta 7.5 (05, N15): type over "size · bitrate · codec · addon", badges resolution / language / HDR on the right
    val files = androidx.compose.runtime.remember(stream) { forgeFileBadgesFor(stream) }
    val (oldTop, oldBottom) = forgeStreamLines(stream.quality, stream.badges.map { it.name }, stream.behaviorHints?.videoSize, stream.addonName, fallbackName)
    val top = files.type ?: oldTop
    val size = stream.behaviorHints?.videoSize ?: com.nuvio.tv.core.stream.StreamTextSizeParser.effectiveSizeBytes(stream) // nowy fork: rozmiar z parsera KE
    val bottom = when {
        files.type == null -> oldBottom
        wide -> listOfNotNull(size?.takeIf { it > 0 }?.let { ReadyPick.sizeText(it) }, stream.addonName.takeIf { it.isNotBlank() }).joinToString(" · ")
        else -> forgeFileSecondLine(files, size, stream.addonName)
    }
    ForgeFocusBox(
        radius = 16.dp,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp) /* v7.4 (05): Wiersz 56 */.onPreviewKeyEvent { e ->
            if (onUpKey != null && e.type == KeyEventType.KeyDown && e.key == Key.DirectionUp) { onUpKey(); true } else false
        },
        scaleOnFocus = false,
        focusRequester = focusRequester,
        onFocusChange = { onFocusChanged?.invoke(it) },
        onOk = onClick,
    ) { focused ->
        val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (focused) ForgeColors.Focus else androidx.compose.ui.graphics.Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 7.dp), // v7.4 (05): tytuł 16/700 y+7, opis 14
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(top, style = ForgeText.shelfTitle.copy(color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis) // v7.4 (05): 16/700
                if (bottom.isNotBlank()) Text(bottom, style = ForgeText.label5.copy(color = if (focused) ForgeColors.OnFocus.copy(alpha = .75f) else ForgeColors.Text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            (if (wide) files.all else files.narrow).forEach { com.nuvio.tv.skin.kicia.components.ForgeBadge(it, com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Quality, Modifier.padding(start = 8.dp)) }
            if (best) Icon(Icons.Filled.Check, null, tint = if (focused) androidx.compose.ui.graphics.Color(0xFF1E7A4C) else ForgeColors.Ok, modifier = Modifier.padding(start = 12.dp).size(24.dp))
        }
    }
}

/** The two lines of a source: picture (quality and the first two badges), then the rest, the size and the add-on. */
internal fun forgeStreamLines(quality: String?, badges: List<String>, sizeBytes: Long?, addon: String, fallbackName: String): Pair<String, String> {
    val tags = badges.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val topParts = listOfNotNull(quality?.takeIf { it.isNotBlank() }) + tags.take(2)
    val top = topParts.joinToString(" · ").ifBlank { fallbackName.replace(Regex("\\s*\\n\\s*"), " · ") }
    val bottom = (tags.drop(2) + listOfNotNull(sizeBytes?.takeIf { it > 0 }?.let { ReadyPick.sizeText(it) }, addon.takeIf { it.isNotBlank() })).joinToString(" · ")
    return top to bottom
}

/** The file's badges for the source list, with the title's length from the stream screen (for the bitrate). */
internal fun forgeFileBadgesFor(stream: Stream): FileBadges {
    // Nowy fork: tekst jak SmartStreamRanking.candidate w Kicii; rozmiar z parsera KE; długość tytułu nieznana (bez Mb/s);
    // język z odpowiedzi „polski dźwięk czy jakość” (fork/sources/SourceChoice).
    val text = listOf(stream.name, stream.title, stream.description, stream.quality, stream.behaviorHints?.filename, stream.clientResolve?.filename).joinToString("\n") { it.orEmpty() }
    val sizeBytes = stream.behaviorHints?.videoSize ?: com.nuvio.tv.core.stream.StreamTextSizeParser.effectiveSizeBytes(stream)
    val info = com.nuvio.tv.core.streams.smart.ReleaseParser.parse(text, sizeBytes)
    val size = sizeBytes ?: info.textSizeBytes
    val duration: Long? = null
    val lang = com.nuvio.tv.skin.kicia.KiciaSkin.audioLanguage
    return ForgeFileBadges.of(info, size, duration, lang, lang)
}

/** "4,9 GB · 18 Mb/s · HEVC · Torrentio". */
internal fun forgeFileSecondLine(files: FileBadges, sizeBytes: Long?, addon: String): String =
    listOfNotNull(sizeBytes?.takeIf { it > 0 }?.let { ReadyPick.sizeText(it) }, files.bitrate, files.codec, addon.takeIf { it.isNotBlank() }).joinToString(" · ")
