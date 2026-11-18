package com.nuvio.tv.skin.kicia.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.NextToWatch
import com.nuvio.tv.fork.sources.ReadyPick
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * "Ready" and the best source's badges under the detail screen's metadata. Hidden until a source is found, and
 * for a title that has none. For a series it follows the episode Play would start.
 */
@Composable
fun ForgeReadyLine(meta: Meta, nextToWatch: NextToWatch?, modifier: Modifier = Modifier) {
    val vm: ForgeReadyViewModel = hiltViewModel()
    val isSeries = meta.apiType.equals("series", ignoreCase = true)
    val videoId = if (isSeries) nextToWatch?.nextVideoId else meta.id
    LaunchedEffect(meta.id, videoId, nextToWatch?.nextSeason, nextToWatch?.nextEpisode) {
        if (videoId != null) vm.load(meta.apiType, videoId, nextToWatch?.nextSeason.takeIf { isSeries }, nextToWatch?.nextEpisode.takeIf { isSeries }, contentId = meta.id)
    }
    val info by vm.info.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()
    // Mockup S03: one plain line under the buttons, never chips: "● Gotowe · 4K · Dolby Vision · TrueHD Atmos 7.1 · REMUX · 70,8 GB".
    val current = info
    if (current == null) {
        if (searching && videoId != null) {
            Text("Szukam źródeł…", style = com.nuvio.tv.skin.kicia.tokens.ForgeText.label5, modifier = modifier.padding(top = 10.dp))
        }
        return
    }
    val ok = com.nuvio.tv.skin.kicia.tokens.ForgeColors.Ok
    val head = stringResource(if (current.ready) R.string.forge_ready_ready else R.string.forge_ready_found)
    val rest = if (current.choices > 0) listOf(stringResource(R.string.forge_ready_choices, current.choices))
    else forgeReadyParts(current.quality, current.tags, current.sizeBytes?.let { ReadyPick.sizeText(it) }, forgeReadyAudio(current.audio))
    Row(modifier = modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (current.ready) ok else Color(0xFFF6C945)))
        Text(
            text = androidx.compose.ui.text.buildAnnotatedString {
                // The source list is not "Ready": only the number of sources is said.
                if (current.choices > 0) append(rest.joinToString(" · "))
                else {
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = if (current.ready) ok else Color(0xFFF6C945)))
                    append(head)
                    pop()
                    if (rest.isNotEmpty()) append(" · " + rest.joinToString(" · "))
                }
            },
            style = com.nuvio.tv.skin.kicia.tokens.ForgeText.body.copy(color = com.nuvio.tv.skin.kicia.tokens.ForgeColors.Text),
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** The best source's parts after "Gotowe", in the mockup's order: quality, HDR / audio / type tags, size, audio kind. */
internal fun forgeReadyParts(quality: String?, tags: List<String>, size: String?, audio: String? = null): List<String> =
    listOfNotNull(quality) + tags + listOfNotNull(size, audio)

/** Switch on when the app translates subtitles itself; until then "napisy AI" is not shown. */
internal const val FORGE_AI_SUBTITLES_READY = false

/** "lektor PL" / "dubbing PL" / "PL" / "napisy PL"; nothing when the name does not say. */
@Composable
internal fun forgeReadyAudio(audio: com.nuvio.tv.fork.sources.ReadyAudio?): String? = audio
    // Owner 13.11: "napisy AI" = subtitles translated by the app, not built yet; the slot stays, the text waits for it.
    ?.takeUnless { it.kind == com.nuvio.tv.fork.sources.ReadyAudioKind.AI_SUBTITLES && !FORGE_AI_SUBTITLES_READY }
    ?.let {
    stringResource(
        when (it.kind) {
            com.nuvio.tv.fork.sources.ReadyAudioKind.LEKTOR -> R.string.forge_ready_audio_lektor
            com.nuvio.tv.fork.sources.ReadyAudioKind.DUBBING -> R.string.forge_ready_audio_dubbing
            com.nuvio.tv.fork.sources.ReadyAudioKind.LANGUAGE -> R.string.forge_ready_audio_language
            com.nuvio.tv.fork.sources.ReadyAudioKind.SUBTITLES -> R.string.forge_ready_audio_subtitles
            com.nuvio.tv.fork.sources.ReadyAudioKind.AI_SUBTITLES -> R.string.forge_ready_audio_ai_subtitles
        },
        it.language,
    )
}

@Composable
private fun Chip(text: String, dot: Color? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
            Box(Modifier.size(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = NuvioTheme.colors.TextPrimary,
            maxLines = 1,
        )
    }
}

/**
 * v7.4 (04) data line, 12/16 at 70%: "2026 · 1 g 45 min  [4K] [HDR]  Gotowe · 9,1 GB". The badges and "Gotowe" appear once a source is found.
 */
@Composable
fun ForgeDetailDataLine(meta: Meta, nextToWatch: NextToWatch?, modifier: Modifier = Modifier) {
    val vm: ForgeReadyViewModel = hiltViewModel()
    val isSeries = meta.apiType.equals("series", ignoreCase = true)
    val videoId = if (isSeries) nextToWatch?.nextVideoId else meta.id
    LaunchedEffect(meta.id, videoId, nextToWatch?.nextSeason, nextToWatch?.nextEpisode) {
        if (videoId != null) vm.load(meta.apiType, videoId, nextToWatch?.nextSeason.takeIf { isSeries }, nextToWatch?.nextEpisode.takeIf { isSeries }, contentId = meta.id)
    }
    val info by vm.info.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()
    val year = Regex("""\d{4}""").find(meta.releaseInfo ?: "")?.value
    val length = if (isSeries) null else forgeRuntime(meta.runtime)
    val base = listOfNotNull(year, length).joinToString(" · ")
    val small = com.nuvio.tv.skin.kicia.tokens.ForgeText.caption.copy(color = com.nuvio.tv.skin.kicia.tokens.ForgeColors.Text2)
    Row(modifier = modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (base.isNotEmpty()) Text(base, style = small, maxLines = 1)
        val current = info
        if (current != null) {
            val badges = (listOfNotNull(current.quality) + current.tags.filter { it.equals("HDR", true) || it.contains("Vision", true) || it.equals("HDR10+", true) }).distinct().take(3)
            badges.forEach { com.nuvio.tv.skin.kicia.components.ForgeBadge(it, com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Quality) }
            val head = stringResource(if (current.ready) R.string.forge_ready_ready else R.string.forge_ready_found)
            val tail = if (current.choices > 0) stringResource(R.string.forge_ready_choices, current.choices) else current.sizeBytes?.let { ReadyPick.sizeText(it) }
            val audio = if (current.choices > 0) null else forgeReadyAudio(current.audio)
            Text(listOfNotNull(head.takeIf { current.choices == 0 }, tail, audio).joinToString(" · "), style = small, maxLines = 1)
        } else if (searching && videoId != null) {
            Text("Szukam źródeł…", style = small, maxLines = 1)
        }
    }
}
