package com.nuvio.tv.skin.niunia.components

import android.util.Log
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaGradient
import com.nuvio.tv.skin.niunia.tokens.NiuniaIconPaths
import com.nuvio.tv.skin.niunia.tokens.NiuniaMotion
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaScale
import com.nuvio.tv.skin.niunia.tokens.NiuniaShadow
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val DS_TAG = "ForkNiunia"

// ================================================================ fokus, OK i długie OK (NScreen)

/**
 * Element z fokusem design systemu: OK (puszczenie < 520 ms) = [onClick], długie OK (≥ 520 ms) = [onLongClick]
 * (bez krótkiej akcji), wciśnięcie = skala 0,97 przez 100 ms. Wygląd fokusu rysuje komponent ([focused] w [onFocus]).
 * [name] trafia do logów: `ForkNiunia: OK → …`.
 */
@Composable
fun Modifier.niuniaOk(
    name: String,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    onFocus: (Boolean) -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)?,
): Modifier {
    var downAt by remember { mutableLongStateOf(0L) }
    var longFired by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val press by animateFloatAsState(if (pressed) NiuniaScale.ScalePress else 1f, tween(NiuniaMotion.DurPress.toInt()), label = "niuniaPress")
    return this
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .focusProperties { canFocus = enabled }
        .onFocusChanged { onFocus(it.isFocused) }
        .onPreviewKeyEvent { e ->
            val ok = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
            if (!ok || !enabled) return@onPreviewKeyEvent false
            when (e.type) {
                KeyEventType.KeyDown -> {
                    if (downAt == 0L) {
                        downAt = System.currentTimeMillis(); longFired = false; pressed = true
                        if (onLongClick != null) scope.launch {
                            val mark = downAt
                            delay(NiuniaMotion.LongPress.toLong())
                            if (downAt == mark && !longFired) {
                                longFired = true; pressed = false
                                Log.i(DS_TAG, "długie OK → $name")
                                onLongClick()
                            }
                        }
                    }
                    true
                }
                KeyEventType.KeyUp -> {
                    val wasLong = longFired
                    downAt = 0L; pressed = false
                    if (!wasLong) {
                        if (onClick != null) { Log.i(DS_TAG, "OK → $name"); onClick() } else Log.i(DS_TAG, "OK → $name: brak akcji")
                    }
                    true
                }
                else -> false
            }
        }
        .focusable(enabled)
        .graphicsLayer { scaleX = press; scaleY = press }
}

/** Wygląd fokusu (focus-and-keys.md): Lift = uniesienie 4 dp, skala 1,1, cień i obwódka 3 dp; Button = skala 1,08 i cień; Fill = bez skali. */
@Composable
fun Modifier.niuniaFocusLook(style: NiuniaFocusStyle, focused: Boolean, shape: Shape): Modifier {
    val target = when { !focused -> 1f; style == NiuniaFocusStyle.Lift -> NiuniaScale.ScaleFocus; style == NiuniaFocusStyle.Button -> NiuniaScale.ScaleButton; else -> 1f }
    val s by animateFloatAsState(target, tween(NiuniaMotion.DurFocus.toInt(), easing = NiuniaMotion.Ease), label = "niuniaFocusScale")
    val t = if (target == 1f) 0f else ((s - 1f) / (target - 1f)).coerceIn(0f, 1f)
    val lift = if (style == NiuniaFocusStyle.Lift) NiuniaScale.LiftFocus else 0.dp
    return this
        .zIndex(if (focused) 2f else 0f)
        .graphicsLayer { scaleX = s; scaleY = s; translationY = lift.toPx() * t }
        .then(
            when {
                focused && style == NiuniaFocusStyle.Lift -> Modifier
                    .shadow(NiuniaShadow.ShadowFocus.y, shape, clip = false, ambientColor = NiuniaShadow.ShadowFocus.color, spotColor = NiuniaShadow.ShadowFocus.color)
                    .border(NiuniaShadow.ShadowFocus.ring, NiuniaShadow.ShadowFocus.ringColor, shape)
                focused && style == NiuniaFocusStyle.Button -> Modifier
                    .shadow(NiuniaShadow.ShadowButtonFocus.y, shape, clip = false, ambientColor = NiuniaShadow.ShadowButtonFocus.color, spotColor = NiuniaShadow.ShadowButtonFocus.color)
                style == NiuniaFocusStyle.Lift -> Modifier.shadow(NiuniaShadow.ShadowPoster.y, shape, clip = false, ambientColor = NiuniaShadow.ShadowPoster.color, spotColor = NiuniaShadow.ShadowPoster.color)
                else -> Modifier
            }
        )
}

// ================================================================ NSurface

enum class NiuniaSurfaceKind { ScrimHero, ScrimPlayer, ScrimDim, ScrimSide, ScrimHeroBottom, Panel, Glass, Tray, Frost, Divider, Rule }

/**
 * Powierzchnia. Szkło (Glass/Tray/Frost) bez rozmycia tła: Compose nie rozmywa tego, co pod spodem, a JMGO (Android 11)
 * nie ma RenderEffect — używamy nieprzezroczystego koloru z tokenów (`panel` / `chrome` / `frost`), bez symulacji rozmycia (ZASADY §2).
 */
@Composable
fun NiuniaSurface(kind: NiuniaSurfaceKind, modifier: Modifier = Modifier, shape: Shape? = null, content: @Composable BoxScope.() -> Unit = {}) {
    val m = when (kind) {
        NiuniaSurfaceKind.ScrimHero -> modifier.background(NiuniaGradient.ScrimHero.brush())
        NiuniaSurfaceKind.ScrimPlayer -> modifier.background(NiuniaGradient.ScrimPlayer.brush())
        NiuniaSurfaceKind.ScrimDim -> modifier.background(NiuniaGradient.ScrimDim.brush())
        NiuniaSurfaceKind.ScrimSide -> modifier.background(NiuniaGradient.ScrimSide.brush())
        NiuniaSurfaceKind.ScrimHeroBottom -> modifier.background(NiuniaGradient.ScrimHeroBottom.brush())
        NiuniaSurfaceKind.Panel -> modifier.clip(shape ?: RoundedCornerShape(NiuniaRadius.Radius16)).background(NiuniaColor.Fill12).padding(horizontal = NiuniaSpacing.Space16, vertical = NiuniaSpacing.Space12)
        NiuniaSurfaceKind.Glass -> modifier.clip(shape ?: RoundedCornerShape(NiuniaRadius.Radius28)).background(NiuniaColor.Panel).border(1.dp, NiuniaColor.Fill12, shape ?: RoundedCornerShape(NiuniaRadius.Radius28))
        NiuniaSurfaceKind.Tray -> modifier.clip(shape ?: CircleShape).background(NiuniaColor.Chrome).border(1.dp, NiuniaColor.Fill12, shape ?: CircleShape)
        NiuniaSurfaceKind.Frost -> modifier.clip(shape ?: RoundedCornerShape(NiuniaRadius.Radius16)).background(NiuniaColor.Frost)
        NiuniaSurfaceKind.Divider -> modifier.width(1.dp).height(NiuniaSize.Icon20).background(NiuniaColor.Line)
        NiuniaSurfaceKind.Rule -> modifier.fillMaxWidth().height(1.dp).background(NiuniaColor.Line)
    }
    Box(m, content = content)
}

// ================================================================ NText

enum class NiuniaTextVariant { Caption, CaptionRegular, BodySmall, LabelSmall, Body, Label, TitleShelf, TitleSection, TitleScreen, Display, Eyebrow }
enum class NiuniaTone { Primary, Secondary, Tertiary, Live, Archive, Delay, OnFocus, OnFocus70 }

fun NiuniaTextVariant.style(): TextStyle = when (this) {
    NiuniaTextVariant.Caption -> NiuniaType.Caption
    NiuniaTextVariant.CaptionRegular -> NiuniaType.CaptionRegular
    NiuniaTextVariant.BodySmall -> NiuniaType.BodySmall
    NiuniaTextVariant.LabelSmall -> NiuniaType.LabelSmall
    NiuniaTextVariant.Body -> NiuniaType.Body
    NiuniaTextVariant.Label -> NiuniaType.Label
    NiuniaTextVariant.TitleShelf -> NiuniaType.TitleShelf
    NiuniaTextVariant.TitleSection -> NiuniaType.TitleSection
    NiuniaTextVariant.TitleScreen -> NiuniaType.TitleScreen
    NiuniaTextVariant.Display -> NiuniaType.Display
    NiuniaTextVariant.Eyebrow -> NiuniaType.Caption.copy(letterSpacing = NiuniaType.Caption.fontSize * 0.04f)
}

fun NiuniaTone.color(): Color = when (this) {
    NiuniaTone.Primary -> NiuniaColor.Text
    NiuniaTone.Secondary -> NiuniaColor.Text70
    NiuniaTone.Tertiary -> NiuniaColor.Text50
    NiuniaTone.Live -> NiuniaColor.Live
    NiuniaTone.Archive -> NiuniaColor.Archive
    NiuniaTone.Delay -> NiuniaColor.Delay
    NiuniaTone.OnFocus -> NiuniaColor.OnFocus
    NiuniaTone.OnFocus70 -> NiuniaColor.OnFocus70
}

/** `**pogrubienie**` z makiety → pogrubiony fragment w kolorze [strong] (`.n-text__strong`). */
fun niuniaRichText(text: String, strong: Color = NiuniaColor.Text): AnnotatedString = buildAnnotatedString {
    val parts = text.split("**")
    parts.forEachIndexed { i, part ->
        if (i % 2 == 1 && i < parts.size - 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = strong)) { append(part) } else {
            if (i % 2 == 1) append("**")
            append(part)
        }
    }
}

/** Tekst design systemu: wariant ze skali, ton, liczba linii (wielokropek), wersaliki. */
@Composable
fun NiuniaText(
    text: String,
    modifier: Modifier = Modifier,
    variant: NiuniaTextVariant = NiuniaTextVariant.Body,
    tone: NiuniaTone = NiuniaTone.Primary,
    lines: Int = Int.MAX_VALUE,
    upper: Boolean = variant == NiuniaTextVariant.Eyebrow,
    align: TextAlign? = null,
    color: Color? = null,
    strong: Color = NiuniaColor.Text,
) {
    val shown = if (upper) text.uppercase() else text
    Text(
        text = if (shown.contains("**")) niuniaRichText(shown, strong) else AnnotatedString(shown),
        modifier = modifier,
        style = variant.style(),
        color = color ?: tone.color(),
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
    )
}

// ================================================================ NIcon

private val iconCache = HashMap<String, ImageVector>()

/** Ikona z zestawu design systemu (viewBox 24, kreska 2). Nieznana nazwa = „info” + błąd w logu. */
fun niuniaIconVector(name: String): ImageVector = iconCache.getOrPut(name) {
    val paths = NiuniaIconPaths.all[name] ?: run {
        Log.e(DS_TAG, "NIcon: UnknownIcon: brak ikony $name")
        NiuniaIconPaths.all.getValue("info")
    }
    val b = ImageVector.Builder(name = "niunia-$name", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
    paths.forEach { (d, filled) ->
        b.addPath(
            pathData = addPathNodes(d),
            fill = if (filled) SolidColor(Color.White) else null,
            stroke = if (filled) null else SolidColor(Color.White),
            strokeLineWidth = if (filled) 0f else 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    b.build()
}

@Composable
fun NiuniaIcon(name: String, modifier: Modifier = Modifier, size: Dp = NiuniaSize.Icon20, tint: Color = NiuniaColor.Text, description: String? = null) {
    Icon(niuniaIconVector(name), contentDescription = description, tint = tint, modifier = modifier.size(size))
}

// ================================================================ NMedia

enum class NiuniaLogoSize(val width: Dp) { Hero(NiuniaSize.LogoHeroW), Player(NiuniaSize.LogoPlayerW), Small(NiuniaSize.LogoSmallW), Page(NiuniaSize.LogoHeroW), Poster(NiuniaSize.LogoPosterW), Spotlight(NiuniaSize.LogoPosterW * 2) }

/** Obraz wypełniający; błąd ładowania = podpisany zapas (`.n-media__fallback`) i `ImageLoadError` w logu. */
@Composable
fun NiuniaMedia(src: Any?, modifier: Modifier = Modifier, alt: String? = null, fallbackLabel: String? = alt, contentScale: ContentScale = ContentScale.Crop) {
    var failed by remember(src) { mutableStateOf(src == null) }
    Box(modifier) {
        if (!failed) AsyncImage(
            model = src, contentDescription = alt, contentScale = contentScale, modifier = Modifier.fillMaxSize(),
            onError = { st -> failed = true; Log.w(DS_TAG, "NMedia: ImageLoadError: ${st.result.throwable.javaClass.simpleName}: ${st.result.throwable.message}") },
        )
        if (failed) Box(Modifier.fillMaxSize().background(NiuniaColor.Surface).padding(NiuniaSpacing.Space12), contentAlignment = Alignment.Center) {
            if (!fallbackLabel.isNullOrBlank()) NiuniaText(fallbackLabel, variant = NiuniaTextVariant.LabelSmall, tone = NiuniaTone.Secondary, lines = 3, align = TextAlign.Center)
        }
    }
}

/** Logo tytułu: szerokość z tokenu, wys. do 2 × logo-page-h, do lewej i do dołu; bez logo — tytuł tekstem. */
@Composable
fun NiuniaLogo(src: String?, alt: String, size: NiuniaLogoSize, modifier: Modifier = Modifier, center: Boolean = false) {
    var failed by remember(src) { mutableStateOf(src.isNullOrBlank()) }
    if (!failed) {
        AsyncImage(
            model = src, contentDescription = alt, contentScale = ContentScale.Fit,
            alignment = if (center) Alignment.Center else Alignment.BottomStart,
            onError = { st -> failed = true; Log.w(DS_TAG, "NMedia logo: ImageLoadError: ${st.result.throwable.javaClass.simpleName}: ${st.result.throwable.message}") },
            modifier = modifier.widthIn(max = size.width).heightIn(max = if (size == NiuniaLogoSize.Poster) NiuniaSize.LogoPosterH else if (size == NiuniaLogoSize.Page) NiuniaSize.LogoPageH else NiuniaSize.LogoPageH * 2),
        )
    } else {
        val v = when (size) { NiuniaLogoSize.Hero, NiuniaLogoSize.Page -> NiuniaTextVariant.Display; NiuniaLogoSize.Player -> NiuniaTextVariant.TitleScreen; else -> NiuniaTextVariant.Label }
        NiuniaText(alt, modifier.widthIn(max = size.width), variant = v, lines = 2, align = if (center) TextAlign.Center else null)
    }
}

/** Awatar z inicjałem (konto w menu, obsada bez zdjęcia). [me] = kolor profilu. */
@Composable
fun NiuniaAvatar(initials: String, size: Dp, modifier: Modifier = Modifier, fill: Color? = null, photo: String? = null) {
    Box(
        modifier.size(size).clip(CircleShape).then(if (fill != null) Modifier.background(fill) else Modifier.background(NiuniaGradient.InitialsFill.brush())),
        contentAlignment = Alignment.Center,
    ) {
        val style = when { size >= NiuniaSize.Avatar120 -> NiuniaType.TitleScreen; size >= NiuniaSize.Avatar44 -> NiuniaType.TitleSection; else -> NiuniaType.LabelSmall }
        Text(initials, style = style, color = if (fill != null) NiuniaColor.OnAvatar else NiuniaColor.Text50, maxLines = 1)
        if (photo != null) AsyncImage(model = photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

// ================================================================ NIndicator

/** Mini pasek postępu 24×4 (albo na całą szerokość [bar]); na białym przycisku ciemny. */
@Composable
fun NiuniaProgress(value: Float, modifier: Modifier = Modifier, bar: Boolean = false, onFocus: Boolean = false) {
    val track = if (onFocus) NiuniaColor.OnFocus70 else NiuniaColor.Fill36
    val fill = if (onFocus) NiuniaColor.OnFocus else NiuniaColor.White
    Box((if (bar) modifier.fillMaxWidth() else modifier.width(NiuniaSize.Icon24)).height(NiuniaSize.ProgressH).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(NiuniaSize.ProgressH).background(fill))
    }
}

/** Kropki karuzeli: aktywna 12×4 biała, pozostałe 4×4 w 50%, na tle scrim 30% w pigułce. */
@Composable
fun NiuniaDots(count: Int, index: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(NiuniaColor.Scrim30).padding(horizontal = NiuniaSpacing.Space8, vertical = NiuniaSpacing.Space4)
            .semantics { contentDescription = "${index + 1} / $count" },
        horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4), verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            Box(Modifier.size(width = if (i == index) NiuniaSpacing.Space12 else NiuniaSpacing.Space4, height = NiuniaSpacing.Space4).clip(CircleShape)
                .background(if (i == index) NiuniaColor.White else NiuniaColor.Text50))
        }
    }
}

/** Kropka statusu 10 dp (na żywo / archiwum / opóźnienie). */
@Composable
fun NiuniaDot(status: NiuniaCardStatus, modifier: Modifier = Modifier) {
    Box(modifier.size(NiuniaSize.Dot).clip(CircleShape).background(status.color))
}

/** Migotanie ładowania (`state="loading"`). */
@Composable
fun BoxScope.NiuniaShimmer() {
    val t by rememberInfiniteTransition(label = "niuniaShimmer").animateFloat(0f, 1f, infiniteRepeatable(tween(1200)), label = "niuniaShimmerT")
    Canvas(Modifier.matchParentSize()) {
        val w = size.width
        drawRect(Brush.horizontalGradient(listOf(NiuniaColor.Fill12, NiuniaColor.Fill24, NiuniaColor.Fill12), startX = -w + 2 * w * t, endX = w * t * 2))
    }
}

// ================================================================ NControl

/** Przełącznik 44×24, gałka 20; włączony = zielony. */
@Composable
fun NiuniaSwitch(on: Boolean, modifier: Modifier = Modifier) {
    val x by animateFloatAsState(if (on) 1f else 0f, tween(NiuniaMotion.DurFocus.toInt(), easing = NiuniaMotion.Ease), label = "niuniaSwitch")
    Canvas(modifier.size(NiuniaSize.SwitchW, NiuniaSize.SwitchH)) {
        drawRoundRect(if (on) NiuniaColor.SwitchOn else NiuniaColor.Fill24, cornerRadius = CornerRadius(size.height / 2))
        val k = NiuniaSize.SwitchKnob.toPx(); val pad = 2.dp.toPx()
        drawCircle(NiuniaColor.White, radius = k / 2, center = Offset(pad + k / 2 + x * k, size.height / 2))
    }
}

/** Pasek „teraz” EPG / tor (pomocniczo): prostokąt z zaokrągleniem. */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.niuniaBar(color: Color, x: Float, y: Float, w: Float, h: Float) {
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(h / 2f))
}
