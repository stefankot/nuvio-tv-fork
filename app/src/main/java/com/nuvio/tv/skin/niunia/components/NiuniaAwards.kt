package com.nuvio.tv.skin.niunia.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.skin.niunia.awards.AWARDS_TAG
import com.nuvio.tv.skin.niunia.awards.NiuniaAward
import com.nuvio.tv.skin.niunia.awards.NiuniaAwardBadge
import com.nuvio.tv.skin.niunia.awards.NiuniaAwardBody
import com.nuvio.tv.skin.niunia.awards.NiuniaAwardListGroup
import com.nuvio.tv.skin.niunia.awards.NiuniaAwardResult
import com.nuvio.tv.skin.niunia.awards.NiuniaAwardRowData
import com.nuvio.tv.skin.niunia.awards.NiuniaAwardWords
import com.nuvio.tv.skin.niunia.awards.galas
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsData
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsListGroups
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsSplit
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsStrip
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsStripDescription
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsSummary
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType

/*
 * Z-13 „Nagrody i nominacje” (DS 2.2.0, NAwards + NTag award + NListRow award + NIcon award-*).
 * Dane i kolejność: skin/niunia/awards (NiuniaAwardsModel, NiuniaAwardsSource). Tokeny bez nowych.
 */

/** Cień pod tekstem na obrazie hero / plakatu (`drop-shadow(0 1px 4px shadow-ink)`). */
private val inkShadow = Shadow(NiuniaColor.ShadowInk, Offset(0f, 1f), 4f)
private fun TextStyle.inked() = copy(shadow = inkShadow)

/** Odmiana z resources skórki (testy JVM podają te same słowa wprost). */
@Composable
fun niuniaAwardWords(): NiuniaAwardWords = NiuniaAwardWords(
    stringResource(R.string.niunia_awards_award_one), stringResource(R.string.niunia_awards_award_few), stringResource(R.string.niunia_awards_award_many),
    stringResource(R.string.niunia_awards_nom_one), stringResource(R.string.niunia_awards_nom_few), stringResource(R.string.niunia_awards_nom_many),
    stringResource(R.string.niunia_awards_gala_one), stringResource(R.string.niunia_awards_gala_few), stringResource(R.string.niunia_awards_gala_many),
)

// ================================================================ znak gali (NIcon award-* / znak słowny)

/**
 * Znak gali: biała sylwetka (Oscar, Emmy, Złoty Glob, BAFTA, Saturn, palma Cannes, niedźwiedź Berlinale), dla pozostałych
 * znak słowny w ramce 1 dp w kolorze znaku, promień 6, `caption` 12/16 (przy 28: `label-small`), wysokość = [size].
 */
@Composable
fun NiuniaAwardMark(body: NiuniaAwardBody?, size: Dp, tint: Color, modifier: Modifier = Modifier) {
    val icon = if (body == null) "award-laurel" else body.icon
    if (icon != null) {
        NiuniaIcon(icon, modifier, size = size, tint = tint)
        return
    }
    val big = size >= NiuniaSize.Icon28
    val shape = RoundedCornerShape(NiuniaRadius.Radius6)
    Box(
        modifier.height(size).border(1.dp, tint, shape).padding(horizontal = if (big) NiuniaSpacing.Space8 else NiuniaSpacing.Space4),
        contentAlignment = Alignment.Center,
    ) {
        Text(body!!.short ?: body.name, style = (if (big) NiuniaType.LabelSmall else NiuniaType.Caption), color = tint, maxLines = 1, softWrap = false)
    }
}

// ================================================================ NAwards kind="strip" (hero, nad „Obsada”)

/**
 * Pasek nagród w prawej kolumnie hero: najwyżej [max] znaków z poziomów 1–3 (znak 20 + liczba `label-small`), reszta jako
 * jedna liczba (`caption-regular`, 70%). Wygrane: liczba biała, znak 70%; same nominacje: liczba 70%, znak 50%. Bez fokusu,
 * jedna linia, odstęp 12. Brak nagród = nic (log).
 */
@Composable
fun NiuniaAwardsStrip(awards: List<NiuniaAward>, modifier: Modifier = Modifier, max: Int = 3) {
    val w = niuniaAwardWords()
    val d = remember(awards) { niuniaAwardsData(awards) }
    val plan = remember(d, w, max) { niuniaAwardsStrip(d, w, max) }
    LaunchedEffect(d) {
        if (d.isEmpty) Log.i(AWARDS_TAG, "NAwards: strip: brak nagród, nic nie rysuję")
        else Log.i(AWARDS_TAG, "NAwards: strip: ${d.groups.size} gal (${plan.shown.size} znaki: ${plan.shown.joinToString { it.body.label }}), ${niuniaAwardsSummary(d.wins, d.noms, w)}, reszta: ${plan.rest ?: "–"}")
    }
    if (d.isEmpty) return
    val descr = stringResource(R.string.niunia_awards_strip_descr, niuniaAwardsStripDescription(d, w))
    Row(
        modifier.clearAndSetSemantics { contentDescription = descr },
        horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        plan.shown.forEach { g ->
            val won = g.wins.isNotEmpty()
            Row(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4), verticalAlignment = Alignment.CenterVertically) {
                NiuniaAwardMark(g.body, NiuniaSize.Icon20, if (won) NiuniaColor.Text70 else NiuniaColor.Text50)
                Text("${if (won) g.wins.size else g.noms.size}", style = NiuniaType.LabelSmall.inked(), color = if (won) NiuniaColor.Text else NiuniaColor.Text70, maxLines = 1, softWrap = false)
            }
        }
        plan.rest?.let { Text(it, style = NiuniaType.CaptionRegular.inked(), color = NiuniaColor.Text70, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip) }
    }
}

// ================================================================ NAwards kind="list" (dół pełnej strony)

/**
 * Sekcja „Nagrody i nominacje” + podsumowanie „6 nagród · 16 nominacji”, dwie zrównoważone kolumny (odstęp 40), grupy nie
 * łamią się między kolumnami, 24 pod grupą. Gale z poziomu 4 w jednej grupie „Festiwale i inne gale”. Wiersze mają fokus
 * (białe tło jak ustawienia), „Wygrana” odwrócona na fokusie, gala przed rozdaniem = chip „Gala <data>”. Brak = stan pusty.
 */
@Composable
fun NiuniaAwardsList(awards: List<NiuniaAward>, modifier: Modifier = Modifier, onFocus: (Boolean) -> Unit = {}) {
    val w = niuniaAwardWords()
    val d = remember(awards) { niuniaAwardsData(awards) }
    val groups = remember(d) { niuniaAwardsListGroups(d) }
    val split = remember(groups) { niuniaAwardsSplit(groups) }
    LaunchedEffect(d) {
        if (d.isEmpty) Log.i(AWARDS_TAG, "NAwards: list: brak nagród, stan pusty")
        else Log.i(AWARDS_TAG, "NAwards: list: ${d.all.size} pozycji w ${d.groups.size} galach → ${groups.size} grup (lewa kolumna: $split)")
    }
    var active by remember { mutableStateOf(false) }
    Column(
        modifier.fillMaxWidth().onFocusChanged { if (it.hasFocus != active) { active = it.hasFocus; onFocus(it.hasFocus) } },
        verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12),
    ) {
        NiuniaCollectionHeader(stringResource(R.string.niunia_awards_title), active, sub = if (d.isEmpty) null else niuniaAwardsSummary(d.wins, d.noms, w))
        if (d.isEmpty) {
            NiuniaText(stringResource(R.string.niunia_awards_empty), Modifier.padding(vertical = NiuniaSpacing.Space16), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary)
            return@Column
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space40), verticalAlignment = Alignment.Top) {
            listOf(groups.take(split), groups.drop(split)).forEach { col ->
                Column(Modifier.weight(1f)) { col.forEach { AwardGroup(it, w) } }
            }
        }
    }
}

@Composable
private fun AwardGroup(g: NiuniaAwardListGroup, w: NiuniaAwardWords) {
    val name = g.body?.name ?: stringResource(R.string.niunia_awards_festivals)
    Column(Modifier.fillMaxWidth().padding(bottom = NiuniaSpacing.Space24), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4)) {
        Row(
            Modifier.padding(start = NiuniaSpacing.Space16, end = NiuniaSpacing.Space16, bottom = NiuniaSpacing.Space8),
            horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12), verticalAlignment = Alignment.CenterVertically,
        ) {
            NiuniaAwardMark(g.body, NiuniaSize.Icon28, NiuniaColor.Text70)
            Column {
                Text(name, style = NiuniaType.Label, color = NiuniaColor.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val counts = niuniaAwardsSummary(g.wins, g.noms, w)
                Text(if (g.body == null) w.galas(g.galas) + " · " + counts else counts, style = NiuniaType.CaptionRegular, color = NiuniaColor.Text50, maxLines = 1)
            }
        }
        g.rows.forEach { AwardRow(it, g.body?.name ?: it.via ?: name) }
    }
}

/** NListRow kind="award": kategoria (`label-small`, zawija się) + „rok · festiwal · nominowany”, min. 56, padding 8. */
@Composable
private fun AwardRow(r: NiuniaAwardRowData, bodyName: String) {
    val a = r.award
    var focused by remember { mutableStateOf(false) }
    val won = a.result == NiuniaAwardResult.Won
    val pending = a.result == NiuniaAwardResult.Pending
    val year = a.year?.toString() ?: ""
    val tag = when {
        won -> stringResource(R.string.niunia_awards_won)
        pending -> stringResource(R.string.niunia_awards_gala, a.date ?: year)
        else -> null
    }
    val descr = when {
        won -> stringResource(R.string.niunia_awards_row_won, bodyName, a.category, year)
        pending -> stringResource(R.string.niunia_awards_row_pending, bodyName, a.category, year, a.date ?: year)
        else -> stringResource(R.string.niunia_awards_row_nom, bodyName, a.category, year)
    }
    val shape = RoundedCornerShape(NiuniaRadius.Radius16)
    Row(
        Modifier.fillMaxWidth().heightIn(min = NiuniaSize.H56)
            .niuniaOk("nagroda ${a.body} ${a.year}", onFocus = { focused = it }, onClick = null)
            .semantics { contentDescription = descr }
            .clip(shape).background(if (focused) NiuniaColor.Focus else Color.Transparent)
            .padding(horizontal = NiuniaSpacing.Space16, vertical = NiuniaSpacing.Space8),
        horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12), verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4)) {
            Text(a.category, style = NiuniaType.LabelSmall, color = if (focused) NiuniaColor.OnFocus else NiuniaColor.Text)
            val sub = listOfNotNull(a.year?.toString(), r.via, a.nominee?.takeIf { it.isNotBlank() }).joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, style = NiuniaType.CaptionRegular, color = if (focused) NiuniaColor.OnFocus70 else NiuniaColor.Text50)
        }
        if (tag != null) AwardRowTag(tag, strong = won, focused = focused)
    }
}

/** „Wygrana” = `badge-strong` (białe), na fokusie odwrócona; gala przed rozdaniem = chip. */
@Composable
private fun AwardRowTag(text: String, strong: Boolean, focused: Boolean) {
    val (bg, fg) = when {
        strong && focused -> NiuniaColor.OnFocus to NiuniaColor.White
        strong -> NiuniaColor.Focus to NiuniaColor.OnFocus
        focused -> Color.Transparent to NiuniaColor.OnFocus
        else -> NiuniaColor.ChipOnImage to NiuniaColor.Text
    }
    val shape = if (strong) CircleShape else RoundedCornerShape(NiuniaRadius.Radius6)
    Box(
        Modifier.height(if (strong) NiuniaSize.Icon16 else NiuniaSize.TagH).clip(shape).background(bg)
            .then(if (strong) Modifier else Modifier.border(1.dp, if (focused) NiuniaColor.OnFocus70 else NiuniaColor.OnImageRing, shape))
            .padding(horizontal = NiuniaSpacing.Space8),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = NiuniaType.Caption, color = fg, maxLines = 1, softWrap = false) }
}

// ================================================================ NTag kind="award" (plakietka na plakacie)

/** Plakietka nagrody na plakacie: znak 16 nad tekstem (najwyżej 2 linie, wyśrodkowany, `caption`, cień); nominacja = 70%. */
@Composable
fun NiuniaAwardTag(badge: NiuniaAwardBadge, modifier: Modifier = Modifier) {
    val color = if (badge.nomination) NiuniaColor.Text70 else NiuniaColor.Text
    Column(
        modifier.clearAndSetSemantics { contentDescription = badge.text },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4),
    ) {
        if (badge.body != null) NiuniaAwardMark(badge.body, NiuniaSize.Icon16, color)
        Text(badge.text, style = NiuniaType.Caption.inked(), color = color, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/** Plakietka dla plakatu tytułu [id] z nagród już wczytanych (plakaty nic same nie pobierają); null = brak plakietki. */
@Composable
fun niuniaPosterAward(id: String): NiuniaAwardBadge? {
    val list = com.nuvio.tv.skin.niunia.awards.NiuniaAwardsRepo.cached(id)?.takeIf { it.isNotEmpty() } ?: return null
    val w = niuniaAwardWords()
    return remember(list, w) { com.nuvio.tv.skin.niunia.awards.niuniaAwardBadge(niuniaAwardsData(list), w) }
}
