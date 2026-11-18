package com.nuvio.tv.skin.kicia.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design tokens of the TV mockup (docs/makieta-tv/LAYOUT_SPEC.md §1).
 * Plain objects on purpose: they are constants of the design, not a theme the user can change.
 * The board is 960x540 dp (1080p at 320 dpi), so 1 mockup px = 1 dp = 1 sp.
 */
object ForgeColors {
    val Bg = Color(0xFF2C2C2E) // v6: flat Apple TV grey
    val Text = Color(0xFFFFFFFF)
    val Text2 = Color(0xB3FFFFFF) // v7.4: tekst-70
    val Text3 = Color(0x80FFFFFF) // .50 (v6)
    val Focus = Color(0xFFEBEBE8) // v7
    val OnFocus = Color(0xFF14161A)
    val Surface = Color(0x1FFFFFFF) // v7.4: wyp-12 (przycisk w spoczynku)
    val Surface2 = Color(0xFF3A3A3C) // v7.4: surface (karta, wiersz, szkielet)
    val Surface3 = Color(0x3DFFFFFF) // .24
    val Sheet = Color(0xF53A3A3C) // v7.4: panel rgba(58,58,60,.96); the blur behind it is added by the host
    val Ok = Color(0xFF3DD68C) // v7 archiwum
    val Live = Color(0xFFE5484D) // v7
    val Gold = Color(0xFFF6D365)
    val Accent = Color(0xFFA8C7FA)
    val AccentSoft = Color(0x4DA8C7FA) // .30, history pill of the guide
    val CardBase = Color(0xFF3A3A3C) // v7.4: karta bez obrazu = --surface
    val ShadowFocus = Color(0x8C000000) // .55
    val Switch = Color(0xFF30D158) // v7.4: --przelacznik
    val Line = Color(0x1FFFFFFF) // v7.4: --linia-kolor
    val Scrim = Color(0x99000000) // v7.4: --zaslona .6
    val GlassBg = Color(0x61000000) // v7.4: --szklo-tlo .38
    val GlassFallback = Color(0xE0000000) // v7.4: --szklo-zapas .88 (bez rozmycia)
    val NightTint = Color(0x29FF8C28) // v7.4: --tryb-nocny-barwa rgba(255,140,40,.16)
    val Rating1 = Color(0xFFF5473C); val Rating2 = Color(0xFFFF8A1F); val Rating3 = Color(0xFFF5C518); val Rating4 = Color(0xFF3FD070) // v7.4: --ocena-1..4 (progi 4 / 5,5 / 7 / 8,5)
    val Tray = Color(0xB81C1C1E) // v7.4: --taca rgba(28,28,30,.72)

    private val ScrimBase = Color(0xFF2C2C2E) // v6: scrims fade into the flat Apple TV grey

    /** Hero scrim on Home / TV (v6): light, only the left and the bottom edge fade into the background. */
    // v7.4: .tlo-tytulu-lewe = linear-gradient(to right, rgba(0,0,0,.72) 40%, transparent 75%)
    val HeroScrimHorizontal: Brush = Brush.horizontalGradient(
        0.00f to Color.Black.copy(alpha = .72f),
        0.40f to Color.Black.copy(alpha = .72f),
        0.75f to Color.Transparent,
        1.00f to Color.Transparent,
    )
    val HeroScrimVertical: Brush = Brush.verticalGradient(
        0.00f to Color.Transparent,
        0.55f to ScrimBase.copy(alpha = 0f),
        0.82f to ScrimBase.copy(alpha = .75f),
        1.00f to ScrimBase.copy(alpha = 1f),
    )

    /** Player scrims: top 170 dp (.78 -> 0) and bottom 300 dp (.90 -> .72 at 45% -> 0). */
    val PlayerScrimTop: Brush = Brush.verticalGradient(
        0f to Color.Black.copy(alpha = .78f),
        1f to Color.Transparent,
    )
    val PlayerScrimBottom: Brush = Brush.verticalGradient(
        0f to Color.Transparent,
        0.55f to Color.Black.copy(alpha = .72f),
        1f to Color.Black.copy(alpha = .90f),
    )
}

object ForgeDims {
    // Safe area: 48 dp left/right, 27 dp top/bottom. Content edge x = 58 dp.
    val SafeH = 48.dp
    val SafeV = 27.dp
    val ContentStart = 40.dp // v6 (nav bar unchanged)
    val ContentEnd = 40.dp
    val Gap = 20.dp
    val ShelfHeaderToCards = 24.dp // v7
    val ShelfGap = 40.dp // v7

    // Shapes.
    val RadiusCard = 8.dp
    val RadiusButton = 22.dp // v7
    val RadiusNavPill = 22.dp // v7.4: pełna kapsuła taca 44
    val RadiusPillSelect = 18.dp // v7.4: pigułka 36
    val RadiusSheet = 16.dp
    val RadiusRow = 8.dp
    val RadiusGuideCell = 8.dp

    // Controls.
    val ButtonHeight = 44.dp // v7
    val ButtonSmHeight = 36.dp // v7
    val IconButton = 44.dp // v7
    val IconButtonOnVideo = 36.dp // v7
    const val IconRatio = 0.66f
    val RowHeight = 44.dp // v7
    val BadgeHeight = 22.dp
    val IconButtonLarge = 56.dp
    val PillSelectHeight = 36.dp // v7
    val NavPillHeight = 44.dp // v7

    // Cards (grid columns 124 / 196 / 268 / 412 / 844; YouTube exception 242).
    val Poster = 130.dp
    val PosterHeight = 194.dp
    val BigCard = 230.dp // v7
    val BigCardHeight = 345.dp // v7
    val RadiusBigCard = 8.dp // v7
    val Wide = 160.dp
    val WideHeight = 114.dp
    val ChannelTile = 96.dp
    val YouTubeCard = 259.dp // v7

    // Focus.
    const val PressScale = 0.97f // v7.4: --skala-wcisniecia
    const val FocusScale = 1.125f // v6: focus = scale + shadow, no outline
    val FocusBorder = 0.dp
    val FocusShadow = 14.dp

    // Panels.
    val SideSheetWidth = 400.dp // v7.4: panel 400, flush right
    val SideSheetMargin = 0.dp // v7.4
    val DialogWidth = 440.dp
    val PopoverWidth = 240.dp // v7.4
    val PopoverWideWidth = 280.dp
    val SideSheetNarrow = 360.dp
    val SourcesSheetWidth = 440.dp
    val SettingsRailWidth = 216.dp
    val ChannelCard = 240.dp; val ChannelCardHeight = 135.dp // v7.4: --kanal
    val YouTubeCardHeight = 146.dp
    val NextCard = 160.dp; val NextCardHeight = 90.dp // v7.4: --dalej
    val DayCard = 56.dp; val DayCardHeight = 64.dp
    val KeyboardKey = 36.dp; val KeyboardGap = 2.dp
    val SwitchWidth = 44.dp; val SwitchHeight = 24.dp; val SwitchKnob = 20.dp
    val ContentTop = 76.dp; val ContentStartY = 84.dp // v7.4: --tresc-gora / --tresc-start
    val HeroHeight = 440.dp; val HeroRowY = 316.dp; val HeroDotsY = 366.dp; val HeroTextWidth = 300.dp; val HeroLogoHeight = 63.dp

    // Player.
    val ScrubTrack = 4.dp
    val ScrubTrackFocused = 8.dp
    val ScrubMarkerWidth = 4.dp
    val ScrubMarkerHeight = 22.dp
    val PreviewCardWidth = 150.dp
    val PreviewCardHeight = 85.dp
    val PlayerScrimTopHeight = 170.dp
    val PlayerScrimBottomHeight = 300.dp
}

object ForgeMotion {
    // v7.4: tokens.css --ruch-300 / 450 / 600, --krzywa cubic-bezier(.4,0,.2,1), --czas-wcisniecia 100
    const val FocusMs = 300
    const val ScrollMs = 450
    const val FadeMs = 300
    const val BackgroundMs = 600
    const val HeroTextMs = 500
    const val PressMs = 100
    const val NightFadeMs = 5000
    const val TabDwellMs = 250L
    const val ToastMs = 3000L
    const val TrailerFadeMs = 700

    /** Long OK threshold (a decision of the owner): OK acts at once, long OK opens more options. */
    const val LongPressMs = 520L

    /** Holding an arrow moves focus no faster than this. */
    const val MinFocusRepeatMs = 110L

    val FocusEasing = CubicBezierEasing(.4f, 0f, .2f, 1f) // v7.4: --krzywa
    val ScrollEasing = CubicBezierEasing(.4f, 0f, .2f, 1f) // v7.4: --krzywa

    fun <T> focusSpec() = tween<T>(durationMillis = FocusMs, easing = FocusEasing)
    fun <T> scrollSpec() = tween<T>(durationMillis = ScrollMs, easing = ScrollEasing)
    fun <T> fadeSpec() = tween<T>(durationMillis = FadeMs)
}
