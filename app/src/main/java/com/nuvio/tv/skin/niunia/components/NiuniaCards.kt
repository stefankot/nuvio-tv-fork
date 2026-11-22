package com.nuvio.tv.skin.niunia.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType

/* NMediaCard: presety z tekstem pod obrazem i karty bez obrazu (paczka 2: EpisodeCard, TrailerCard, CastCard, ProfileCard, ResultCard). */

/**
 * EpisodeCard / TrailerCard (`.n-cardwrap--episode`, szer. 205): kadr 205×115 z paskiem podpisu (▶, postęp, czas), pod nim
 * nadtytuł (ODCINEK 1), tytuł 1 linia, opis do 4 linii, meta. Fokus: kadr unosi się z obwódką, tekst na matowym tle
 * (`frost`) przesunięty o 8 dp; [current] = to samo tło bez fokusu („Teraz”).
 */
@Composable
fun NiuniaEpisodeCard(
    image: Any?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    title: String? = null,
    description: String? = null,
    meta: String? = null,
    caption: String? = null,
    progress: Int? = null,
    play: Boolean = false,
    badge: String? = null,
    current: Boolean = false,
    trailer: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocus: (Boolean) -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(NiuniaRadius.Radius16)
    val name = listOfNotNull(eyebrow, title).joinToString(", ").ifBlank { "odcinek" }
    Column(
        modifier.width(NiuniaSize.StillW)
            .niuniaOk(name, focusRequester, onFocus = { focused = it; onFocus(it) }, onLongClick = onLongClick, onClick = onClick)
            .semantics { contentDescription = name },
    ) {
        // Kadr jako zwykła karta (bez własnego fokusu), wygląd fokusu z rodzica.
        NiuniaMediaCard(
            preset = if (trailer) NiuniaCardPreset.TrailerCard else NiuniaCardPreset.EpisodeCard, image = image, onClick = null,
            caption = caption, progress = progress, play = play, badge = badge, focusable = false,
            modifier = Modifier.niuniaFocusLook(NiuniaFocusStyle.Lift, focused, shape), description = title,
        )
        val textBg = if (focused || current) NiuniaColor.Frost else Color.Transparent
        Column(
            Modifier.padding(top = if (focused) NiuniaSpacing.Space16 else NiuniaSpacing.Space8).fillMaxWidth()
                .clip(RoundedCornerShape(NiuniaRadius.Radius16)).background(textBg)
                .padding(horizontal = if (trailer) NiuniaSpacing.Space4 else NiuniaSpacing.Space12, vertical = NiuniaSpacing.Space8),
            verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4),
        ) {
            if (eyebrow != null) NiuniaText(eyebrow, variant = NiuniaTextVariant.Caption, tone = NiuniaTone.Tertiary, upper = true, lines = 1)
            if (title != null) NiuniaText(title, variant = NiuniaTextVariant.LabelSmall, lines = 1)
            if (!description.isNullOrBlank()) NiuniaText(description, variant = NiuniaTextVariant.CaptionRegular, tone = NiuniaTone.Secondary, lines = 4)
            if (!meta.isNullOrBlank()) NiuniaText(meta, variant = NiuniaTextVariant.CaptionRegular, tone = NiuniaTone.Tertiary, lines = 1)
        }
    }
}

/**
 * CastCard (`.n-cardwrap--person`): okrągłe zdjęcie 130 (albo 84 przy [small]) z inicjałami, gdy nie ma zdjęcia; pod nim imię
 * (14/20) i rola (12/16, 50%). Fokus: koło unosi się (skala 1,1, obwódka), tekst bez tła.
 */
@Composable
fun NiuniaCastCard(
    name: String,
    role: String?,
    photo: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocus: (Boolean) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val d: Dp = if (small) NiuniaSize.Avatar84 else NiuniaSize.PosterW
    Column(
        modifier.width(if (small) NiuniaSize.Avatar84 + NiuniaSpacing.Space16 else NiuniaSize.PosterW)
            .niuniaOk(name, focusRequester, onFocus = { focused = it; onFocus(it) }, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(if (small) NiuniaSpacing.Space8 else NiuniaSpacing.Space12),
    ) {
        NiuniaAvatar(niuniaInitials(name), d, Modifier.niuniaFocusLook(NiuniaFocusStyle.Lift, focused, CircleShape), photo = photo)
        Column {
            Text(name, style = NiuniaType.LabelSmall, color = NiuniaColor.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!role.isNullOrBlank()) Text(niuniaRoleLabel(role), style = NiuniaType.CaptionRegular, color = NiuniaColor.Text50, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Test JMGO build159: TMDB podaje funkcje twórców po angielsku („Director”) → po polsku; rola postaci zostaje bez zmian. */
@androidx.compose.runtime.Composable
internal fun niuniaRoleLabel(role: String): String = when (role.trim().lowercase()) {
    "director" -> androidx.compose.ui.res.stringResource(com.nuvio.tv.R.string.niunia_role_director)
    "creator" -> androidx.compose.ui.res.stringResource(com.nuvio.tv.R.string.niunia_role_creator)
    "writer", "screenplay", "novel", "story" -> androidx.compose.ui.res.stringResource(com.nuvio.tv.R.string.niunia_role_writer)
    "producer", "executive producer" -> androidx.compose.ui.res.stringResource(com.nuvio.tv.R.string.niunia_role_producer)
    else -> role
}

/** Inicjały: pierwsze litery dwóch pierwszych słów („Morfydd Clark” → „MC”). */
fun niuniaInitials(name: String): String = name.split(' ', '-').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/**
 * ProfileCard (`.n-cardwrap--profile`): koło 72 (wypełnienie 24%, bieżący profil w kolorze awatara), inicjał albo ikona,
 * zielony znacznik ✓ przy bieżącym, podpis 12/16. Fokus: skala 1,08 i cień, podpis biały.
 */
@Composable
fun NiuniaProfileCard(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    initials: String? = null,
    icon: String? = null,
    me: Boolean = false,
    current: Boolean = false,
    color: Color? = null,
    photo: String? = null,
    focusRequester: FocusRequester? = null,
    onFocus: (Boolean) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier.width(NiuniaSize.Avatar72 + NiuniaSpacing.Space16)
            .niuniaOk(title, focusRequester, onFocus = { focused = it; onFocus(it) }, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8),
    ) {
        Box {
            Box(
                Modifier.size(NiuniaSize.Avatar72).niuniaFocusLook(NiuniaFocusStyle.Button, focused, CircleShape).clip(CircleShape)
                    .background(if (me) color ?: NiuniaColor.Avatar else NiuniaColor.Fill24),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    icon != null -> NiuniaIcon(icon, size = NiuniaSize.Icon28, tint = NiuniaColor.White)
                    else -> Text(initials ?: niuniaInitials(title).take(1), style = NiuniaType.TitleScreen, color = if (me) NiuniaColor.OnAvatar else NiuniaColor.White, maxLines = 1)
                }
                if (photo != null) NiuniaMedia(photo, Modifier.fillMaxSize(), fallbackLabel = null)
            }
            if (current) Box(
                Modifier.align(Alignment.TopEnd).size(NiuniaSize.Icon20).clip(CircleShape).background(NiuniaColor.SwitchOn),
                contentAlignment = Alignment.Center,
            ) { NiuniaIcon("check", size = NiuniaSpacing.Space12, tint = NiuniaColor.White) }
        }
        Text(title, style = NiuniaType.CaptionRegular, color = if (focused) NiuniaColor.White else NiuniaColor.Text70, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Miniatura w karcie wyniku: pionowa 40×60, pozioma 60×40 albo awatar z inicjałami (osoba). */
enum class NiuniaResultThumb { Poster, Wide, Person }

/**
 * ResultCard (`.n-result`, 280×80): miniatura, tytuł 16/22 w 1 linii, podtytuł 12/16 w 50% (z kropką statusu przy TV na żywo).
 * Fokus: białe tło, ciemny tekst, skala 1,08 i cień.
 */
@Composable
fun NiuniaResultCard(
    title: String,
    subtitle: String?,
    image: Any?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    thumb: NiuniaResultThumb = NiuniaResultThumb.Poster,
    status: NiuniaCardStatus? = null,
    focusRequester: FocusRequester? = null,
    onFocus: (Boolean) -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(NiuniaRadius.Radius16)
    Row(
        modifier.size(NiuniaSize.ResultW, NiuniaSize.ResultH)
            .niuniaOk(title, focusRequester, onFocus = { focused = it; onFocus(it) }, onLongClick = onLongClick, onClick = onClick)
            .niuniaFocusLook(NiuniaFocusStyle.Button, focused, shape)
            .clip(shape).background(if (focused) NiuniaColor.Focus else NiuniaColor.Fill12)
            .padding(horizontal = NiuniaSpacing.Space12),
        horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val thumbShape = RoundedCornerShape(NiuniaRadius.Radius6)
        when (thumb) {
            NiuniaResultThumb.Person -> NiuniaAvatar(niuniaInitials(title), NiuniaSize.Avatar44, fill = NiuniaColor.Avatar, photo = image as? String)
            NiuniaResultThumb.Wide -> NiuniaMedia(image, Modifier.size(NiuniaSize.ResultThumbH, NiuniaSize.ResultThumbW).clip(thumbShape).background(NiuniaColor.Surface), fallbackLabel = null)
            NiuniaResultThumb.Poster -> NiuniaMedia(image, Modifier.size(NiuniaSize.ResultThumbW, NiuniaSize.ResultThumbH).clip(thumbShape).background(NiuniaColor.Surface), fallbackLabel = null)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = NiuniaType.Body, color = if (focused) NiuniaColor.OnFocus else NiuniaColor.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4), verticalAlignment = Alignment.CenterVertically) {
                if (status != null) NiuniaDot(status)
                if (subtitle != null) Text(subtitle, style = NiuniaType.CaptionRegular, color = if (focused) NiuniaColor.OnFocus70 else NiuniaColor.Text50, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Fakt (NContentBlock `fact`): nagłówek 14/20 pogrubiony i linie 14/20 w 50%. */
@Composable
fun NiuniaFact(title: String, lines: List<String>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4)) {
        NiuniaText(title, variant = NiuniaTextVariant.LabelSmall, lines = 1)
        lines.filter { it.isNotBlank() }.forEach { NiuniaText(it, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 2) }
    }
}

