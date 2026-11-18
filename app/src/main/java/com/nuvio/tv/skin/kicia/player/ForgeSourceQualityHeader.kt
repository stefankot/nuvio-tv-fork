package com.nuvio.tv.skin.kicia.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.nuvio.tv.core.streams.smart.SmartPreset
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.skin.kicia.components.ForgePillSelect
import com.nuvio.tv.skin.kicia.detail.forgeFileBadgesFor
import com.nuvio.tv.skin.kicia.detail.forgeFileSecondLine
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/**
 * v7.6 (05, owner 13.11): the top of the "Źródło i jakość" panel. Three quality buttons with icons (no word "Jakość"):
 * OK = a new source of that quality at once (the film loads again from the same spot, also for the level that plays now),
 * long OK = the list of sources of that quality. Under them the playing file in two lines: for laymen (quality, language)
 * and for nerds (parameters).
 */
@Composable
fun ForgeSourceQualityHeader(
    preset: SmartPreset,
    current: Stream?,
    onPick: (SmartPreset) -> Unit,
    onList: (SmartPreset) -> Unit,
    showDetails: Boolean,
    modifier: Modifier = Modifier,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
) {
    Column(modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            forgePresetLabels.forEach { (p, label) ->
                ForgePillSelect(
                    text = label, selected = p == preset, icon = forgePresetIcon(p),
                    focusRequester = if (p == preset) focusRequester else null,
                    onLongOk = { onList(p) }, onOk = { onPick(p) },
                )
            }
        }
        if (showDetails && current != null) {
            val badges = forgeFileBadgesFor(current)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Teraz gra: " + forgeLaymanLine(badges.resolution, badges.hdr, badges.language), style = ForgeText.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sizeBytes = com.nuvio.tv.core.streams.smart.SmartStreamRanking.candidate(current).sizeBytes
                val nerd = listOfNotNull(
                    listOfNotNull(badges.resolution, badges.type).joinToString(" ").takeIf { it.isNotBlank() },
                    forgeFileSecondLine(badges, sizeBytes, current.addonName.orEmpty()).takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (nerd.isNotBlank()) Text(nerd, style = ForgeText.caption.copy(color = ForgeColors.Text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun forgePresetIcon(p: SmartPreset) = when (p) {
    SmartPreset.SAVER -> Icons.Filled.SignalCellularAlt1Bar
    SmartPreset.BALANCED -> Icons.Filled.SignalCellularAlt2Bar
    SmartPreset.BEST -> Icons.Filled.SignalCellularAlt
}

/** "Full HD z HDR · polski lektor": the plain-language line. */
internal fun forgeLaymanLine(resolution: String?, hdr: String?, language: String?): String {
    val quality = when (resolution) {
        "2160p" -> "4K"; "1080p" -> "Full HD"; "720p" -> "HD"; null -> "jakość nieznana"; else -> "Standardowa jakość"
    } + if (hdr != null) " z HDR" else ""
    val lang = when {
        language == null -> null
        language.startsWith("lektor") -> "polski lektor"
        language.startsWith("dubbing") -> "polski dubbing"
        language.startsWith("napisy") -> "polskie napisy"
        // mockup 05 (7.6c): the language in words („angielski”), not a code
        else -> forgeLanguageNames[language.trim().uppercase()] ?: language
    }
    return if (lang != null) "$quality · $lang" else quality
}

/** The level a file belongs to (for the list after a long OK): by bitrate, else by height. */
internal fun forgePresetOf(stream: Stream): SmartPreset {
    val b = forgeFileBadgesFor(stream)
    val mbps = b.bitrate?.substringBefore(' ')?.replace(',', '.')?.toDoubleOrNull()
    if (mbps != null) return if (mbps < 6) SmartPreset.SAVER else if (mbps < 20) SmartPreset.BALANCED else SmartPreset.BEST
    val h = b.resolution?.removeSuffix("p")?.toIntOrNull() ?: return SmartPreset.BALANCED
    return if (h <= 720) SmartPreset.SAVER else if (h >= 2160) SmartPreset.BEST else SmartPreset.BALANCED
}

internal val forgePresetLabels = listOf(SmartPreset.SAVER to "Oszczędny", SmartPreset.BALANCED to "Zbalansowany", SmartPreset.BEST to "Najlepsza jakość")

private val forgeLanguageNames = mapOf(
    "EN" to "angielski", "ENG" to "angielski", "PL" to "polski", "DE" to "niemiecki", "FR" to "francuski", "ES" to "hiszpański",
    "IT" to "włoski", "ITA" to "włoski", "RU" to "rosyjski", "UK" to "ukraiński", "JA" to "japoński", "JP" to "japoński",
    "KO" to "koreański", "ZH" to "chiński", "PT" to "portugalski", "NL" to "niderlandzki", "SV" to "szwedzki", "CS" to "czeski",
    "MULTI" to "kilka języków",
)
