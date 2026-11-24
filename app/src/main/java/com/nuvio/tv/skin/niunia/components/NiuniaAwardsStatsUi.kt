package com.nuvio.tv.skin.niunia.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.nuvio.tv.R
import com.nuvio.tv.skin.niunia.awards.AWARDS_TAG
import com.nuvio.tv.skin.niunia.awards.NiuniaAward
import com.nuvio.tv.skin.niunia.awards.NiuniaStatTile
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsData
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsSummary
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsSummaryTiles
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsTextItems
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsTextLine
import com.nuvio.tv.skin.niunia.awards.awards
import com.nuvio.tv.skin.niunia.awards.noms
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType

/**
 * Z-13 `NAwards kind="summary"`: „Nagrody i nominacje · 6 nagród · 16 nominacji”, potem „Gale” i „Kategorie” — kafelki po 4 w rzędzie
 * (znak gali albo ikona dziedziny, zdanie i druga linia). Kafelki biorą fokus jak wiersze (pilot dochodzi do końca strony).
 */
@Composable
fun NiuniaAwardsSummarySection(awards: List<NiuniaAward>, modifier: Modifier = Modifier, onFocus: (Boolean) -> Unit = {}) {
    val w = niuniaAwardWords()
    val d = remember(awards) { niuniaAwardsData(awards) }
    val also = stringResource(R.string.niunia_ke_aw_also)
    val oa = Triple(stringResource(R.string.niunia_ke_aw_other_one), stringResource(R.string.niunia_ke_aw_other_few), stringResource(R.string.niunia_ke_aw_other_many))
    val on = Triple(stringResource(R.string.niunia_ke_aw_othernom_one), stringResource(R.string.niunia_ke_aw_othernom_few), stringResource(R.string.niunia_ke_aw_othernom_many))
    val fest = stringResource(R.string.niunia_awards_festivals)
    val (bodies, cats) = remember(d) { niuniaAwardsSummaryTiles(d, w, also, oa, on, fest) }
    LaunchedEffect(d) {
        if (d.isEmpty) Log.i(AWARDS_TAG, "NAwards: summary: brak nagród, stan pusty")
        else Log.i(AWARDS_TAG, "NAwards: summary: ${niuniaAwardsSummary(d.wins, d.noms, w)}; ${bodies.size} gala tiles, ${cats.size} category tiles (${cats.joinToString { it.key + " " + it.supporting }})")
    }
    var active by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().onFocusChanged { if (it.hasFocus != active) { active = it.hasFocus; onFocus(it.hasFocus) } },
        verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
        NiuniaCollectionHeader(stringResource(R.string.niunia_awards_title), active, sub = if (d.isEmpty) null else "· " + niuniaAwardsSummary(d.wins, d.noms, w))
        if (d.isEmpty) {
            NiuniaText(stringResource(R.string.niunia_awards_empty), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary)
            return@Column
        }
        Block(stringResource(R.string.niunia_ke_aw_galas), bodies)
        Block(stringResource(R.string.niunia_ke_aw_categories), cats)
    }
}

@Composable
private fun Block(title: String, tiles: List<NiuniaStatTile>) {
    Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8)) {
        NiuniaText(title, variant = NiuniaTextVariant.Caption, tone = NiuniaTone.Tertiary)
        tiles.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8)) {
                row.forEach { t ->
                    Box(Modifier.weight(1f).semantics { contentDescription = t.description }) {
                        NiuniaListRow(t.label, onClick = { Log.i(AWARDS_TAG, "NAwards: kafelek ${t.key}: ${t.description}") }, kind = NiuniaRowKind.Setting,
                            supporting = t.supporting.ifBlank { null },
                            leading = { focused ->
                                val tint = if (focused) NiuniaColor.OnFocus else NiuniaColor.Text
                                if (t.icon != null && t.body == null) NiuniaIcon(t.icon, size = NiuniaSize.Icon20, tint = tint) else NiuniaAwardMark(t.body, NiuniaSize.Icon28, tint)
                            })
                    }
                }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Z-13b `NAwards kind="text"`: trzy linie w prawej kolumnie hero (zamiast obsady): „6 nagród + 16 nominacji”,
 * „Gale **Emmy 0+7 · BAFTA 1+0 · VES 3+4** i 4 inne”, „Kategorie **Efekty wizualne 4+6** i 9 innych”. Każda linia mieści się
 * w `awards-text-w` (336 dp): ostatnia pozycja przechodzi do „i N innych”, aż linia się zmieści (najpierw gale, potem kategorie).
 */
@Composable
fun NiuniaAwardsText(awards: List<NiuniaAward>, modifier: Modifier = Modifier) {
    val w = niuniaAwardWords()
    val d = remember(awards) { niuniaAwardsData(awards) }
    if (d.isEmpty) return
    val otherLabel = stringResource(R.string.niunia_ke_aw_other_label)
    val (gal, cat) = remember(d) { niuniaAwardsTextItems(d, otherLabel) }
    val rest = Triple(stringResource(R.string.niunia_ke_aw_rest_one), stringResource(R.string.niunia_ke_aw_rest_few), stringResource(R.string.niunia_ke_aw_rest_many))
    val galas = stringResource(R.string.niunia_ke_aw_galas); val cats = stringResource(R.string.niunia_ke_aw_categories)
    val measurer = rememberTextMeasurer()
    val maxPx = with(LocalDensity.current) { NiuniaSize.AwardsTextW.roundToPx() }
    val style = NiuniaType.BodySmall.copy(fontWeight = FontWeight.Bold) // mierzone pogrubione: zapas
    fun fits(line: String) = measurer.measure(line.replace("**", ""), style, maxLines = 1).size.width <= maxPx
    var mg = minOf(3, gal.size); var mc = minOf(2, cat.size)
    while (mg > 1 && !fits(niuniaAwardsTextLine(galas, gal, mg, rest))) mg--
    while (mc > 1 && !fits(niuniaAwardsTextLine(cats, cat, mc, rest))) mc--
    val head = "${w.awards(d.wins)} + ${w.noms(d.noms)}"
    LaunchedEffect(head, mg, mc) { Log.i(AWARDS_TAG, "NAwards: text: $head; galas ${gal.size} ($mg shown), categories ${cat.size} ($mc shown)") }
    Column(modifier.widthIn(max = NiuniaSize.AwardsTextW).semantics { contentDescription = "Nagrody i nominacje: $head" }, horizontalAlignment = Alignment.Start) {
        NiuniaText(head, variant = NiuniaTextVariant.LabelSmall, lines = 1)
        if (gal.isNotEmpty()) NiuniaText(niuniaAwardsTextLine(galas, gal, mg, rest), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 1)
        if (cat.isNotEmpty()) NiuniaText(niuniaAwardsTextLine(cats, cat, mc, rest), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 1)
    }
}
