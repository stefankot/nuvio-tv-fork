package com.nuvio.tv.skin.niunia.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.tv.skin.niunia.tokens.NiuniaBlur
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaMotion
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType

/* NCollection, NHero, NNavigation, NOverlay, NInput seek (organizmy design systemu Nuvio TV v2). */

// ================================================================ NCollection

enum class NiuniaCollectionKind { Rail, Actions, Meta, SplitRow, Chips, Tabs, List, Nav, Settings }

/**
 * Nagłówek kolekcji (`.n-col__head`): etykieta 16/22 pogrubiona, szara 50%, biała gdy fokus jest w środku ([active]).
 * Pusty rząd: komunikat zamiast niczego ([emptyText]).
 */
@Composable
fun NiuniaCollectionHeader(title: String, active: Boolean, modifier: Modifier = Modifier, sub: String? = null) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8), verticalAlignment = Alignment.Bottom) {
        Text(title, style = NiuniaType.Label, color = if (active) NiuniaColor.White else NiuniaColor.Text50, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (sub != null) Text(sub, style = NiuniaType.BodySmall, color = NiuniaColor.Text50, maxLines = 1)
    }
}

/** Kolekcja w wierszu: akcje (odstęp 12), meta (8, tekst 14 w 70%), pigułki (4), zakładki (4 w pigułce scrim 30%), wiersz rozdzielony. */
@Composable
fun NiuniaCollection(
    kind: NiuniaCollectionKind,
    modifier: Modifier = Modifier,
    title: String? = null,
    active: Boolean = false,
    empty: Boolean = false,
    emptyText: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
        if (title != null) NiuniaCollectionHeader(title, active)
        if (empty) {
            if (emptyText != null) NiuniaText(emptyText, Modifier.padding(vertical = NiuniaSpacing.Space16), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary)
            return@Column
        }
        val gap = when (kind) {
            NiuniaCollectionKind.Actions -> NiuniaSpacing.Space12
            NiuniaCollectionKind.Meta -> NiuniaSpacing.Space8
            NiuniaCollectionKind.Chips, NiuniaCollectionKind.Tabs -> NiuniaSpacing.Space4
            NiuniaCollectionKind.SplitRow -> NiuniaSpacing.Space24
            else -> NiuniaSpacing.Space20
        }
        val rowMod = when (kind) {
            NiuniaCollectionKind.Tabs -> Modifier.clip(CircleShape).background(NiuniaColor.Scrim30).padding(NiuniaSpacing.Space4)
            NiuniaCollectionKind.SplitRow -> Modifier.fillMaxWidth()
            else -> Modifier
        }
        Row(
            rowMod,
            horizontalArrangement = if (kind == NiuniaCollectionKind.SplitRow) Arrangement.SpaceBetween else Arrangement.spacedBy(gap),
            // Z-9: w hero wiersz akcji i obsada wyrównane do góry (przyciski 16 dp pod opisem, obsada rośnie w dół; bez pustego miejsca nad „Odtwórz”).
            verticalAlignment = if (kind == NiuniaCollectionKind.SplitRow) Alignment.Top else Alignment.CenterVertically,
            content = content,
        )
    }
}

/** Linia meta: tekst 14/20 w 70% i znaczniki, odstęp 8. */
@Composable
fun NiuniaMetaLine(text: String?, modifier: Modifier = Modifier, tone: NiuniaTone = NiuniaTone.Secondary, tags: @Composable RowScope.() -> Unit = {}) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8), verticalAlignment = Alignment.CenterVertically) {
        if (!text.isNullOrBlank()) NiuniaText(text, Modifier.weight(1f, fill = false), variant = NiuniaTextVariant.BodySmall, tone = tone, lines = 1)
        tags()
    }
}

// ================================================================ NHero

enum class NiuniaHeroKind { Immersive, Player, Blurred, Deep, Ambient }

/**
 * Tło hero: obraz na całość + scrim wg rodzaju (immersive: scrim z lewej; player: scrim od dołu; blurred/deep: rozmycie
 * tylko na API 31+, inaczej scrim-dim / przyciemnienie z tokenów). [sidebarScrim] = przyciemnienie z lewej pod otwartym menu.
 * Bez obrazu = tło `bg` (ambient: gradient „ambient-news”).
 */
@Composable
fun NiuniaHeroBackdrop(kind: NiuniaHeroKind, image: Any?, modifier: Modifier = Modifier, blurAmount: Float = 1f, sidebarScrim: Boolean = false) {
    val blurOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val density = LocalDensity.current
    Box(modifier.fillMaxSize().background(if (kind == NiuniaHeroKind.Ambient) Color.Transparent else NiuniaColor.Bg)) {
        if (kind == NiuniaHeroKind.Ambient) Box(Modifier.fillMaxSize().background(com.nuvio.tv.skin.niunia.tokens.NiuniaGradient.AmbientNews.brush()))
        if (image != null) {
            val spec = when (kind) { NiuniaHeroKind.Blurred -> NiuniaBlur.BlurBackdrop; NiuniaHeroKind.Deep -> NiuniaBlur.BlurDeep; else -> null }
            val radius = with(density) { (spec?.radius ?: 0.dp).toPx() } * blurAmount
            AsyncImage(
                model = image, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    if (spec != null && blurOk && radius > 0.5f) {
                        renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                        val s = if (kind == NiuniaHeroKind.Deep) 1.2f else 1.15f
                        scaleX = s; scaleY = s
                    }
                },
            )
        }
        when (kind) {
            NiuniaHeroKind.Immersive -> NiuniaSurface(NiuniaSurfaceKind.ScrimHero, Modifier.fillMaxSize())
            NiuniaHeroKind.Player, NiuniaHeroKind.Ambient -> NiuniaSurface(NiuniaSurfaceKind.ScrimPlayer, Modifier.fillMaxSize())
            NiuniaHeroKind.Blurred -> NiuniaSurface(NiuniaSurfaceKind.ScrimDim, Modifier.fillMaxSize())
            NiuniaHeroKind.Deep -> Box(Modifier.fillMaxSize().background(if (blurOk) Color(0x73000000) else NiuniaColor.Bg.copy(alpha = 0.9f)))
        }
        if (sidebarScrim) NiuniaSurface(NiuniaSurfaceKind.ScrimSide, Modifier.fillMaxSize())
    }
}

// ================================================================ NNavigation

/** Zwinięta nawigacja (`tab-pill`): „‹ (ikona) Zakładka” w lewym górnym rogu (x 20, y 24); fokus = biała pigułka. */
@Composable
fun NiuniaTabPill(label: String, icon: String, modifier: Modifier = Modifier, focused: Boolean = false, leading: (@Composable () -> Unit)? = null) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4), verticalAlignment = Alignment.CenterVertically) {
        NiuniaIcon("chevron-left", Modifier.size(NiuniaSpacing.Space12, NiuniaSize.Icon16), size = NiuniaSize.Icon16, tint = NiuniaColor.Text70)
        Row(
            Modifier.height(NiuniaSize.H36).clip(CircleShape).background(if (focused) NiuniaColor.Focus else NiuniaColor.Chrome)
                .border(1.dp, if (focused) Color.Transparent else NiuniaColor.Fill12, CircleShape)
                .padding(start = NiuniaSpacing.Space4, end = NiuniaSpacing.Space16),
            horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8), verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(NiuniaSize.Icon28).clip(CircleShape).background(if (focused) NiuniaColor.Fill12 else NiuniaColor.Fill24), contentAlignment = Alignment.Center) {
                if (leading != null) leading() else NiuniaIcon(icon, size = NiuniaSize.Icon16, tint = if (focused) NiuniaColor.OnFocus else NiuniaColor.White)
            }
            Text(label, style = NiuniaType.LabelSmall, color = if (focused) NiuniaColor.OnFocus else NiuniaColor.White, maxLines = 1)
        }
    }
}

/** Panel menu bocznego (`sidebar`): x 20, y 20, szer. 200, promień 28, szkło (bez rozmycia: `panel`), odstęp pozycji 4. */
@Composable
fun NiuniaSidebarPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(NiuniaRadius.Radius28)
    Column(
        Modifier.padding(start = NiuniaSpacing.Space20, top = NiuniaSpacing.Space20).then(modifier).width(NiuniaSize.SidebarW)
            .clip(shape).background(NiuniaColor.Panel).border(1.dp, NiuniaColor.Fill12, shape)
            .padding(horizontal = NiuniaSpacing.Space8, vertical = NiuniaSpacing.Space12),
        verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4),
        content = content,
    )
}

// ================================================================ NOverlay

/**
 * Panel boczny odtwarzacza (`panel`): przy prawej krawędzi, od y 20, szer. 360, lewe rogi 28, padding 16; pigułki sekcji
 * z odstępem 8 pod spodem, wiersze bez przerw. Szkło bez rozmycia = `panel` 96% (Android 11).
 */
@Composable
fun NiuniaPanelOverlay(title: String?, modifier: Modifier = Modifier, width: Dp = NiuniaSize.PlayerPanelW, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(topStart = NiuniaRadius.Radius28, bottomStart = NiuniaRadius.Radius28)
    Column(
        modifier.padding(top = NiuniaSpacing.Space20).width(width).clip(shape).background(NiuniaColor.Panel).border(1.dp, NiuniaColor.Fill12, shape)
            .padding(NiuniaSpacing.Space16),
    ) {
        if (title != null) NiuniaText(title, Modifier.padding(start = NiuniaSpacing.Space8, bottom = NiuniaSpacing.Space12), variant = NiuniaTextVariant.TitleSection, lines = 1)
        content()
    }
}

// ================================================================ NInput seek

/**
 * Pasek postępu (jeden we wszystkich odtwarzaczach): czas · tor 8 dp z wypełnieniem i gałką 16 · czas.
 * Ton: biały (główny, archiwum), czerwony (na żywo), żółty (opóźnienie, z buforem i czerwoną kreską „na żywo”).
 * Fokus (Z-5): tor 8 → 12 dp (wysokość, nie skala) i okrągła gałka 20 dp z pierścieniem 5 dp. Fokus i klawisze nadaje [trackModifier] (przewijanie zostaje w odtwarzaczu).
 */
@Composable
fun NiuniaSeekBar(
    state: NiuniaSeekState,
    start: String,
    end: String,
    focused: Boolean,
    modifier: Modifier = Modifier,
    trackModifier: Modifier = Modifier,
    marker: Float? = null,
) {
    val grow by animateFloatAsState(if (focused) 1f else 0f, tween(NiuniaMotion.DurFocus.toInt(), easing = NiuniaMotion.Ease), label = "niuniaSeek")
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12), verticalAlignment = Alignment.CenterVertically) {
        Text(start, style = NiuniaType.Caption, color = NiuniaColor.Text, maxLines = 1)
        Canvas(trackModifier.weight(1f).height(NiuniaSize.Knob + NiuniaSpacing.Space8)) {
            // Z-5: w fokusie tor zmienia wysokość track-h 8 → track-h-focus 12 (nie skalę), gałka 16 → 20 z pierścieniem 5 dp.
            val h = NiuniaSize.TrackH.toPx() + (NiuniaSize.TrackHFocus.toPx() - NiuniaSize.TrackH.toPx()) * grow
            val y = (size.height - h) / 2f
            val w = size.width
            val r = CornerRadius(h / 2f)
            drawRoundRect(NiuniaColor.Fill24, Offset(0f, y), Size(w, h), r)
            state.bufferShown?.let { drawRoundRect(NiuniaColor.Fill36, Offset(0f, y), Size(w * it, h), r) }
            drawRoundRect(state.fillColor, Offset(0f, y), Size(w * state.fill, h), r)
            val gap = NiuniaSize.Stroke3.toPx()
            state.chapterGaps.forEach { c -> drawRect(NiuniaColor.ShadowInk, Offset(w * c - gap / 2, y), Size(gap, h)) }
            state.edgeShown?.let { e ->
                val ext = NiuniaSpacing.Space4.toPx()
                drawRoundRect(NiuniaColor.Live, Offset(w * e - gap / 2, y - ext), Size(gap, h + 2 * ext), CornerRadius(gap / 2))
            }
            val knobX = w * (marker ?: state.fill).coerceIn(0f, 1f)
            val k = NiuniaSize.Knob.toPx() / 2f * (1f + 0.25f * grow)
            if (grow > 0f) drawCircle(NiuniaColor.Fill36, radius = k + 5.dp.toPx() * grow, center = Offset(knobX, size.height / 2f))
            drawCircle(if (state.tone == NiuniaSeekTone.Main || state.tone == NiuniaSeekTone.Archive) NiuniaColor.White else state.fillColor, radius = k, center = Offset(knobX, size.height / 2f))
        }
        Text(end, style = NiuniaType.Caption, color = NiuniaColor.Text70, maxLines = 1)
    }
}

/** Kolumna informacji w odtwarzaczu: etykieta (FILM/SERIAL), logo 300, meta + jakość. */
@Composable
fun NiuniaPlayerInfoBlock(eyebrow: String, logo: String?, title: String, meta: String?, quality: String?, modifier: Modifier = Modifier) {
    NiuniaContentBlock(NiuniaBlockKind.Info, modifier) {
        NiuniaText(eyebrow, variant = NiuniaTextVariant.Eyebrow, tone = NiuniaTone.Secondary)
        NiuniaLogo(logo, title, NiuniaLogoSize.Player)
        NiuniaMetaLine(meta) { if (quality != null) NiuniaTag(quality, NiuniaTagKind.Quality) }
    }
}

/** Pomocnicze: pełnoekranowy kontener z treścią przy dole (layout `player-peek`). */
@Composable
fun NiuniaStage(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize(), content = content)
}
