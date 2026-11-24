// WYGENEROWANE przez fork-tools/niunia/gen_tokens.py z design-system/tokens.json (Nuvio TV v2.1.0). Nie edytuj ręcznie.
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.nuvio.tv.skin.niunia.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Plansza 960×540 dp: 1 px projektu = 1 dp = 1 sp (1 dp = 2 px przy 1080p). Tylko ciemny motyw. */
const val NIUNIA_DS_VERSION = "2.1.0"

/** Kolory (`color`). Role (inverse-surface, secondary-container …) to te same wartości co tokeny bazowe. */
object NiuniaColor {
    /** Screen background (960x540 dp stage). Text at 50% on it is 4.6:1. */
    val Bg = Color(0xFF2C2C2E)
    /** Resting fill of rows, EPG cells, cards without image. */
    val Surface = Color(0xFF3A3A3C)
    /** Dialog and sheet container, 96% opaque. */
    val Panel = Color(0xF53A3A3C)
    /** Focus fill: solid white. Content on it is on-focus. */
    val Focus = Color(0xFFFFFFFF)
    /** Content colour on a focused element and on rating fills. */
    val OnFocus = Color(0xFF111111)
    /** Primary text, selected-state text. */
    val White = Color(0xFFFFFFFF)
    /** Primary text, 100%. */
    val Text = Color(0xFFFFFFFF)
    /** Secondary text, key letters, icons at rest. */
    val Text70 = Color(0xB3FFFFFF)
    /** Tertiary text, hints, inactive shelf titles, disabled. */
    val Text50 = Color(0x80FFFFFF)
    /** Secondary button, text field, hairline. */
    val Fill12 = Color(0x1FFFFFFF)
    /** Primary button, selected (not focused) state. */
    val Fill24 = Color(0x3DFFFFFF)
    /** 1 dp divider. */
    val Line = Color(0x1FFFFFFF)
    /** LIVE badge, EPG now-line. */
    val Live = Color(0xFFE5484D)
    /** Archive badge. */
    val Archive = Color(0xFF3DD68C)
    /** Switch track when on. */
    val SwitchOn = Color(0xFF30D158)
    /** Time-shift marker. */
    val Shift = Color(0xFFF5C518)
    /** Slider fill. */
    val SliderFill = Color(0xFF4A7DEB)
    /** Rating <= 4.0. Text on it: on-focus. */
    val Rating1 = Color(0xFFF5473C)
    /** Rating 5.5. */
    val Rating2 = Color(0xFFFF8A1F)
    /** Rating 7.0. */
    val Rating3 = Color(0xFFF5C518)
    /** Rating >= 8.5. Values between are OKLab-interpolated. */
    val Rating4 = Color(0xFF3FD070)
    /** Dim behind a Dialog. */
    val Scrim = Color(0x99000000)
    /** Unfocused 'Next' cards; glass veil in players. */
    val Scrim30 = Color(0x4D000000)
    /** Glass sheet/panel tint (blur 48, saturation 1.6). */
    val Glass = Color(0x8C1E1E22)
    /** Glass tab bar (blur 30, saturation 1.6) and tool tray. */
    val Tray = Color(0x8C28282C)
    /** Badge and caption backing over artwork. */
    val BadgeOnImage = Color(0x99000000)
    /** Focus shadow colour. */
    val ShadowInk = Color(0x8C000000)
    /** Night-mode overlay. */
    val NightTint = Color(0x29FF8C28)
    /** Overscan guide (tooling only). */
    val SafeArea = Color(0xE6E5484D)
    /** Role: variant surface (M3 surface-variant) = white 12% solid equivalent. */
    val SurfaceVariant = Color(0x1FFFFFFF)
    /** Role: content on variant surface. */
    val OnSurfaceVariant = Color(0xB3FFFFFF)
    /** Role: focused fill (M3 inverse-surface). */
    val InverseSurface = Color(0xFFFFFFFF)
    /** Role: content on a focused fill. */
    val InverseOnSurface = Color(0xFF111111)
    /** Role: selected / emphasised fill. */
    val SecondaryContainer = Color(0x3DFFFFFF)
    /** Role: selected switch track. */
    val InversePrimary = Color(0xFF30D158)
    /** Role: archive/tertiary tone. */
    val Tertiary = Color(0xFF3DD68C)
    /** Error text, 5.4:1 on bg. */
    val Error = Color(0xFFFF7B80)
    /** Error fill; text on it #ffd9d9. */
    val ErrorContainer = Color(0xFF5A2A2C)
    /** Role: outline and divider. */
    val Border = Color(0x1FFFFFFF)
    /** Role: weaker divider. */
    val BorderVariant = Color(0x1FFFFFFF)
    /** 3 dp ring on a focused image card. */
    val FocusRing = Color(0xE6FFFFFF)
    /** Top-left sheen on every card (135deg to transparent at 45%). */
    val Gloss = Color(0x24FFFFFF)
    /** Time-shifted live (IPTV delayed). On it: on-delay. */
    val Delay = Color(0xFFF5C542)
    /** Text on delay. */
    val OnDelay = Color(0xFF1B1500)
    /** Buffered part of a track, active toggle. */
    val Fill36 = Color(0x5CFFFFFF)
    /** Tab pill fill (glass). */
    val Chrome = Color(0x9E28282A)
    /** Button at rest on images. */
    val Button = Color(0x8C161618)
    /** Frosted card behind a focused episode text. */
    val Frost = Color(0x1FFFFFFF)
    /** Profile avatar fill. */
    val Avatar = Color(0xFFA5DCB0)
    /** Initial on the avatar. */
    val OnAvatar = Color(0xFF14361F)
    /** Chip on image. */
    val ChipOnImage = Color(0x59000000)
    /** Chip ring on image. */
    val OnImageRing = Color(0x59FFFFFF)
    /** Series stack layer 1 dimming (Z-7: lighter, the layer also has opacity-stack-1 0.7). */
    val Stack1 = Color(0x33000000)
    /** Series stack layer 2 dimming (Z-7: same as layer 1; the depth comes from opacity-stack-2 0.4). */
    val Stack2 = Color(0x33000000)
    /** Secondary text on a focused (white) element; 9.3:1. */
    val OnFocus70 = Color(0xB3111111)
    /** Outline of age box. */
    val LineStrong = Color(0xB3FFFFFF)
    /** Format tag fill (4K, HDR). */
    val Fmt = Color(0xB3FFFFFF)
    /** Text on format tag. */
    val OnFmt = Color(0xFF1C1C1E)
    /** Fill of the LIVE tag; white text 5.2:1 (live #e5484d gave 3.9:1). */
    val LiveBadge = Color(0xFFC8323A)
    /** Z-2 Glass (Haze) button fill at rest over a blurred backdrop (blur-haze). Focus stays solid white. */
    val Haze = Color(0x29FFFFFF)
    /** Z-2 1 dp top highlight of a Haze button (inset). */
    val HazeEdge = Color(0x47FFFFFF)
    /** Z-2 Haze button without blur (Android 11, effects=flat). White text 9.1:1. */
    val HazeFlat = Color(0xFF48484A)
    /** Z-2 Panel/sheet/sidebar without blur (Android 11). */
    val GlassFlat = Color(0xF72C2C2E)
    /** Z-2 Tab pill, tray and resting button without blur (Android 11). White text 11.4:1. */
    val TrayFlat = Color(0xFF3A3A3C)
    /** Z-2 Frosted text block of a focused card without blur (Android 11). */
    val FrostFlat = Color(0xFF48484A)
}

/** Odstępy (`spacing`). */
object NiuniaSpacing {
    /** Tight gap, rating bar height. */
    val Space4 = 4.dp
    /** Tray gap, popover gap. */
    val Space8 = 8.dp
    /** Button row gap. */
    val Space12 = 12.dp
    /** Row inner gap, row trailing inset. */
    val Space16 = 16.dp
    /** Card gap in a shelf, field padding. */
    val Space20 = 20.dp
    /** Shelf title to cards. */
    val Space24 = 24.dp
    /** Screen margin left/right, shelf to shelf. */
    val Space40 = 40.dp
}

/** Promienie (`radius`); RadiusFull = pigułka. */
object NiuniaRadius {
    /** Tags. */
    val Radius6 = 6.dp
    /** Cards, rows, keys, EPG cells, day tiles. */
    val Radius16 = 16.dp
    /** Panel, Dialog, Popover (left corners for a side panel). */
    val Radius28 = 28.dp
    /** Buttons, pills, tab bar, fields. */
    val RadiusFull = 999.dp
}

/** Rozmiary (`size`). */
object NiuniaSize {
    /** Small button, pill, tab, round button on video, key. */
    val H36 = 36.dp
    /** Primary/secondary button, tray, field, row, EPG row. */
    val H44 = 44.dp
    /** Two-line row, panel row, episode text block. */
    val H56 = 56.dp
    /** Smallest icon. */
    val Icon16 = 16.dp
    /** Icon in button. */
    val Icon20 = 20.dp
    /** Icon in 36 dp circle. */
    val Icon24 = 24.dp
    /** Icon in 44 dp circle. */
    val Icon28 = 28.dp
    /** Minimum width of main button. */
    val ButtonMinW = 160.dp
    /** Poster card. */
    val PosterW = 130.dp
    /** Poster card height (2:3 of 130). Was 194 (rounding error). */
    val PosterH = 195.dp
    /** Landscape / episode card. */
    val LandscapeW = 160.dp
    /** See SYSTEM.md components table. */
    val LandscapeH = 114.dp
    /** YouTube card. */
    val YoutubeW = 259.dp
    /** See SYSTEM.md components table. */
    val YoutubeH = 146.dp
    /** Featured poster. */
    val FeaturedW = 230.dp
    /** See SYSTEM.md components table. */
    val FeaturedH = 345.dp
    /** Channel card. */
    val ChannelW = 240.dp
    /** See SYSTEM.md components table. */
    val ChannelH = 135.dp
    /** Next-programme card (MiniGuide). */
    val NextW = 160.dp
    /** See SYSTEM.md components table. */
    val NextH = 90.dp
    /** Day card. */
    val DayW = 56.dp
    /** See SYSTEM.md components table. */
    val DayH = 64.dp
    /** Bar avatar. */
    val Avatar44 = 44.dp
    /** Cast avatar. */
    val Avatar72 = 72.dp
    /** Profile avatar. */
    val Avatar120 = 120.dp
    /** Keyboard key 36x36. */
    val KeyW = 36.dp
    /** EPG channel column: number + logo (broadcaster layout). */
    val EpgChannelCol = 166.dp
    /** Switch track. */
    val SwitchW = 44.dp
    /** See SYSTEM.md components table. */
    val SwitchH = 24.dp
    /** See SYSTEM.md components table. */
    val SwitchKnob = 20.dp
    /** Side panel, 360-440. */
    val PanelW = 400.dp
    /** Dialog. */
    val DialogW = 440.dp
    /** Popover. */
    val PopoverW = 240.dp
    /** Hero zone. */
    val HeroH = 440.dp
    /** Stage width in dp (1 dp = 2 px at 1080p). */
    val StageW = 960.dp
    /** Stage height. */
    val StageH = 540.dp
    /** Right content edge (left is space-40). */
    val ContentRight = 920.dp
    /** Edge fade. */
    val FadeEdge = 40.dp
    /** Hero bottom gradient into bg. */
    val HeroFade = 90.dp
    /** Wide card / episode still width (16:9). */
    val StillW = 205.dp
    /** Wide card / episode still height. */
    val StillH = 115.dp
    /** Spotlight (featured) poster width. */
    val SpotlightW = 280.dp
    /** Spotlight poster height. */
    val SpotlightH = 350.dp
    /** Title pager card (Z-12): stage width minus 60 dp each side, so the neighbour cards peek 40 dp (gap space-20). */
    val PagerCardW = 840.dp
    /** Z-13b: max width of the award text in the hero's right column (3 lines, never wrapped; items dropped until each line fits). */
    val AwardsTextW = 336.dp
    /** Cast avatar in player info sheets. */
    val Avatar84 = 84.dp
    /** Search result card width. */
    val ResultW = 280.dp
    /** Search result card height. */
    val ResultH = 80.dp
    /** Poster thumbnail in a result card. */
    val ResultThumbW = 40.dp
    /** Poster thumbnail height in a result card. */
    val ResultThumbH = 60.dp
    /** Width of hero / detail text column. */
    val HeroInfoW = 420.dp
    /** Left column of split screens (6 keys of 36). */
    val SplitCol = 216.dp
    /** Start of the right column on split screens. */
    val SplitRight = 296.dp
    /** Opened sidebar menu width. */
    val SidebarW = 200.dp
    /** Player side panel (audio, voice-over, subtitles). */
    val PlayerPanelW = 360.dp
    /** EPG: 30 minutes of timeline. */
    val EpgSlotW = 220.dp
    /** EPG row height. */
    val EpgCellH = 44.dp
    /** Title logo in hero. */
    val LogoHeroW = 370.dp
    /** Title logo in player. */
    val LogoPlayerW = 300.dp
    /** Title logo, compact. */
    val LogoSmallW = 220.dp
    /** Pinned page logo max height. */
    val LogoPageH = 64.dp
    /** Title logo inside a poster footer. */
    val LogoPosterW = 112.dp
    /** Max height of poster logo. */
    val LogoPosterH = 36.dp
    /** How far the last rail sinks below the stage edge on hero screens. */
    val Peek = 56.dp
    /** Top of the collapsed tab pill and top-right actions. */
    val NavTop = 24.dp
    /** Top of content on main screens (below the top bar). */
    val ContentTop = 76.dp
    /** Mini/progress bar thickness. */
    val ProgressH = 4.dp
    /** Player seek track thickness. */
    val TrackH = 8.dp
    /** Seek knob. */
    val Knob = 16.dp
    /** Status dot (live / archive). */
    val Dot = 10.dp
    /** Text caret. */
    val CaretW = 2.dp
    /** Gap between keys. */
    val KeyboardGap = 4.dp
    /** Small chip on image / EPG NOWY. */
    val ChipH = 20.dp
    /** Tag height. */
    val TagH = 20.dp
    /** Series stack layer offset under a tile. */
    val StackOffset = 5.dp
    /** Profile silhouette illustration (Settings). */
    val Avatar240 = 240.dp
    /** Hairline. */
    val Stroke1 = 1.dp
    /** Now line, caret. */
    val Stroke2 = 2.dp
    /** Chapter gap, live edge, focus ring. */
    val Stroke3 = 3.dp
    /** Visible height of the Live TV category tree (fades out below). */
    val TreeMaxH = 460.dp
    /** Player: how far the bottom rail sinks below the stage (38 dp stays visible). */
    val PlayerPeek = 77.dp
    /** Z-5 Seek track thickness when the seek bar has focus (was scaleY 1.25 on the track, which also stretched the knob into an oval). */
    val TrackHFocus = 12.dp
    /** Z-8 Ratings table: episode cell width (height h-44). */
    val RatingCellW = 56.dp
    /** Z-8 Ratings table: season label column. */
    val RatingLabelW = 80.dp
}

/** Skala typografii (`type`), Satoshi 500/700. */
object NiuniaType {
    val Family = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily
    /** Badges, card meta, EPG time axis. */
    val Caption = TextStyle(fontFamily = Family, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = NiuniaColor.Text)
    /** Info line, meta. */
    val CaptionRegular = TextStyle(fontFamily = Family, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = NiuniaColor.Text)
    /** Descriptions, small buttons, pills, card titles, EPG text. */
    val BodySmall = TextStyle(fontFamily = Family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = NiuniaColor.Text)
    /** Small buttons, pills. */
    val LabelSmall = TextStyle(fontFamily = Family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = NiuniaColor.Text)
    /** Row text, dialog text, shelf title. */
    val Body = TextStyle(fontFamily = Family, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, color = NiuniaColor.Text)
    /** Primary buttons, tabs, active shelf title. */
    val Label = TextStyle(fontFamily = Family, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, color = NiuniaColor.Text)
    /** Section header, overlay title, 44 dp avatar initial. */
    val TitleSection = TextStyle(fontFamily = Family, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, color = NiuniaColor.Text)
    /** Screen title, 72 dp avatar initial. */
    val TitleScreen = TextStyle(fontFamily = Family, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, color = NiuniaColor.Text)
    /** Hero title, 120 dp avatar initial. */
    val Display = TextStyle(fontFamily = Family, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, color = NiuniaColor.Text)
    /** Shelf title. */
    val TitleShelf = TextStyle(fontFamily = Family, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = NiuniaColor.Text)
}

/** Czas i krzywa (`time`). */
object NiuniaMotion {
    /** Focus, screen entry, appear. */
    const val DurFocus = 200
    /** Scroll, panels. */
    const val DurScroll = 320
    /** Background, hero content. */
    const val DurBg = 500
    /** Pressed scale 0.97. */
    const val DurPress = 100
    /** Long OK = secondary action. */
    const val LongPress = 520
    /** Single curve, no overshoot. */
    val Ease = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)
}

/** Skale i uniesienie (`scale`). */
object NiuniaScale {
    /** Focused image card, with translateY(-4px). */
    const val ScaleFocus = 1.1f
    /** Pressed. */
    const val ScalePress = 0.97f
    /** Progress bar thickens 4 to 8 dp in focus. */
    const val ScaleProgress = 2.0f
    /** Focused button. */
    const val ScaleButton = 1.08f
    /** Vertical lift of a focused card. */
    val LiftFocus = (-4).dp
}

/** Przezroczystości (`opacity`). */
object NiuniaOpacity {
    /** Disabled / unavailable (skipped by focus). */
    const val OpacityDisabled = 0.5f
    /** Unfocused Next card dimming. */
    const val OpacityDim = 0.3f
    /** Z-7 Series stack: first layer under a programme tile. */
    const val OpacityStack1 = 0.7f
    /** Z-7 Series stack: second (bottom) layer under a programme tile. */
    const val OpacityStack2 = 0.4f
}

/** Cień: przesunięcie Y, rozmycie, kolor; [ring] = obwódka (0 = brak). */
data class NiuniaShadowSpec(val y: Dp, val blur: Dp, val color: Color, val ring: Dp = 0.dp, val ringColor: Color = Color.Transparent, val inset: Boolean = false)

object NiuniaShadow {
    /** Focused image card: lifts 4 dp, scale 1.1, white ring included. */
    val ShadowFocus = NiuniaShadowSpec(22.dp, 44.dp, Color(0x99000000), 3.dp, Color(0xE6FFFFFF), false)
    /** Poster at rest. */
    val ShadowPoster = NiuniaShadowSpec(6.dp, 18.dp, Color(0x59000000), 0.dp, Color(0x00000000), false)
    /** 1 dp hairline. */
    val OutlineHair = NiuniaShadowSpec(0.dp, 0.dp, Color(0x00000000), 1.dp, Color(0x1FFFFFFF), true)
    /** Focused button (white fill, scale 1.08). */
    val ShadowButtonFocus = NiuniaShadowSpec(10.dp, 28.dp, Color(0x73000000), 0.dp, Color(0x00000000), false)
}

/** Rozmycie (`blur`): tylko natywnie na API 31+ (RenderEffect). Na Androidzie 11 (JMGO) — zwykłe tło z tokenów, bez symulacji. */
data class NiuniaBlurSpec(val radius: Dp, val saturate: Float = 1f, val brightness: Float = 1f)

object NiuniaBlur {
    /** Tab pill, top bar. */
    val BlurTray = NiuniaBlurSpec(30.dp, 1.5f, 1.0f)
    /** Overlays, panels, sidebar. */
    val BlurGlass = NiuniaBlurSpec(48.dp, 1.6f, 1.0f)
    /** Blurred screen backdrop (Episodes, Cast). */
    val BlurBackdrop = NiuniaBlurSpec(40.dp, 1.2f, 1.0f)
    /** Page background under full pages and player info. */
    val BlurDeep = NiuniaBlurSpec(64.dp, 1.2f, 0.45f)
    /** Poster progressive blur, layer 1. */
    val BlurPoster1 = NiuniaBlurSpec(2.dp, 1.0f, 1.0f)
    /** Poster progressive blur, layer 2. */
    val BlurPoster2 = NiuniaBlurSpec(6.dp, 1.0f, 1.0f)
    /** Poster progressive blur, layer 3. */
    val BlurPoster3 = NiuniaBlurSpec(14.dp, 1.0f, 1.0f)
    /** Z-2 Glass (Haze) button backdrop. */
    val BlurHaze = NiuniaBlurSpec(20.dp, 1.8f, 1.0f)
    /** Z-2 Blurred left part of a detail hero (film, series, TV programme). */
    val BlurDetail = NiuniaBlurSpec(36.dp, 1.4f, 1.0f)
}

/** Gradient: [angle] w stopniach CSS (180 = z góry na dół, 90 = od lewej), przystanki (pozycja 0..1, kolor). */
data class NiuniaGradientSpec(val radial: Boolean, val angle: Float, val stops: List<Pair<Float, Color>>) {
    fun brush(): Brush = when {
        radial -> Brush.radialGradient(*stops.toTypedArray())
        angle == 90f -> Brush.horizontalGradient(*stops.toTypedArray())
        angle == 270f -> Brush.horizontalGradient(*stops.map { (p, c) -> (1f - p) to c }.reversed().toTypedArray())
        angle == 180f -> Brush.verticalGradient(*stops.toTypedArray())
        else -> Brush.linearGradient(*stops.toTypedArray(), start = Offset.Zero, end = Offset.Infinite)
    }
}

object NiuniaGradient {
    /** Left scrim on hero images; text sits on it. */
    val ScrimHero = NiuniaGradientSpec(false, 90.0f, listOf(0.0f to Color(0xE618181A), 0.38f to Color(0x8C18181A), 0.68f to Color(0x0018181A)))
    /** Player controls scrim. */
    val ScrimPlayer = NiuniaGradientSpec(false, 180.0f, listOf(0.3f to Color(0x00000000), 0.55f to Color(0x99000000), 1.0f to Color(0xE6000000)))
    /** Over a blurred backdrop. */
    val ScrimDim = NiuniaGradientSpec(false, 180.0f, listOf(0.0f to Color(0x991E1E20), 0.6f to Color(0xD61E1E20), 1.0f to Color(0xFF2C2C2E)))
    /** Behind the opened sidebar. */
    val ScrimSide = NiuniaGradientSpec(false, 90.0f, listOf(0.0f to Color(0x80000000), 0.45f to Color(0x00000000)))
    /** Caption band at the bottom of a card (text on image). */
    val ScrimCaption = NiuniaGradientSpec(false, 180.0f, listOf(0.0f to Color(0x00000000), 1.0f to Color(0xB8000000)))
    /** Bottom fade of a hero into bg. */
    val ScrimHeroBottom = NiuniaGradientSpec(false, 180.0f, listOf(0.0f to Color(0x002C2C2E), 1.0f to Color(0xFF2C2C2E)))
    /** Card gloss, top-left. */
    val Gloss = NiuniaGradientSpec(false, 135.0f, listOf(0.0f to Color(0x14FFFFFF), 0.45f to Color(0x00FFFFFF)))
    /** Mask: list continues to the right. */
    val FadeRight = NiuniaGradientSpec(false, 90.0f, listOf(0.88f to Color(0xFF000000), 1.0f to Color(0x00000000)))
    /** Mask: list continues downwards. */
    val FadeBottom = NiuniaGradientSpec(false, 180.0f, listOf(0.88f to Color(0xFF000000), 1.0f to Color(0x00000000)))
    /** Selected EPG row, left edge. */
    val EpgFadeLeft = NiuniaGradientSpec(false, 90.0f, listOf(0.4f to Color(0xFF2C2C2E), 1.0f to Color(0x002C2C2E)))
    /** Selected EPG row, right edge. */
    val EpgFadeRight = NiuniaGradientSpec(false, 270.0f, listOf(0.4f to Color(0xFF2C2C2E), 1.0f to Color(0x002C2C2E)))
    /** Avatar without a photo. */
    val InitialsFill = NiuniaGradientSpec(false, 160.0f, listOf(0.0f to Color(0xFF4A4A4C), 1.0f to Color(0xFF343436)))
    /** Player with no picture (channel feed placeholder). */
    val AmbientNews = NiuniaGradientSpec(true, 180.0f, listOf(0.0f to Color(0xFF2B4C7E), 0.55f to Color(0xFF0F1D33), 1.0f to Color(0xFF06090F)))
    /** Programme tile without art, tone 1. */
    val Tile1 = NiuniaGradientSpec(false, 150.0f, listOf(0.0f to Color(0xFF3A4A66), 1.0f to Color(0xFF1C2333)))
    /** Tone 2. */
    val Tile2 = NiuniaGradientSpec(false, 150.0f, listOf(0.0f to Color(0xFF4A3A3A), 1.0f to Color(0xFF231B1B)))
    /** Tone 3. */
    val Tile3 = NiuniaGradientSpec(false, 150.0f, listOf(0.0f to Color(0xFF3A4A44), 1.0f to Color(0xFF1B2320)))
    /** Tone 4. */
    val Tile4 = NiuniaGradientSpec(false, 150.0f, listOf(0.0f to Color(0xFF46405A), 1.0f to Color(0xFF201D2A)))
    /** Tone 5. */
    val Tile5 = NiuniaGradientSpec(false, 150.0f, listOf(0.0f to Color(0xFF3A3A4A), 1.0f to Color(0xFF1B1B23)))
    /** Z-2 Tint over the blurred (haze) left part of a detail hero; text sits on it. */
    val ScrimDetail = NiuniaGradientSpec(false, 90.0f, listOf(0.0f to Color(0x9E18181A), 0.4f to Color(0x5C18181A), 0.68f to Color(0x0018181A)))
    /** Z-2 Mask: where the detail hero image is blurred (left, under the text). */
    val MaskDetail = NiuniaGradientSpec(false, 90.0f, listOf(0.32f to Color(0xFF000000), 0.64f to Color(0x00000000)))
    /** Z-2 Detail hero without blur (Android 11): solid skin background fading into the sharp image. */
    val ScrimDetailFlat = NiuniaGradientSpec(false, 90.0f, listOf(0.0f to Color(0xFF2C2C2E), 0.3f to Color(0xF02C2C2E), 0.52f to Color(0x8C2C2C2E), 0.76f to Color(0x002C2C2E)))
    /** Z-2 Replaces blurred/deep backdrops without blur (Android 11): skin colours only. */
    val BackdropFlat = NiuniaGradientSpec(true, 180.0f, listOf(0.0f to Color(0xFF48484A), 0.35f to Color(0xFF3A3A3C), 0.75f to Color(0xFF2C2C2E)))
    /** Z-2 Bottom of a detail hero (blur and flat): from 58% of the height text-50 stays >= 4.8:1 even on the brightest pixel (measured on the Odyssey, Rings of Power and TV backdrops). */
    val ScrimDetailBottom = NiuniaGradientSpec(false, 180.0f, listOf(0.3f to Color(0x0018181A), 0.58f to Color(0xE618181A), 1.0f to Color(0xF018181A)))
}

/** Indeks nazwa z tokens.json → wartość z obiektów powyżej (test NiuniaTokensTest porównuje z JSON-em). */
object NiuniaTokenIndex {
    val colors: Map<String, Color> = mapOf(
        "bg" to NiuniaColor.Bg,
        "surface" to NiuniaColor.Surface,
        "panel" to NiuniaColor.Panel,
        "focus" to NiuniaColor.Focus,
        "on-focus" to NiuniaColor.OnFocus,
        "white" to NiuniaColor.White,
        "text" to NiuniaColor.Text,
        "text-70" to NiuniaColor.Text70,
        "text-50" to NiuniaColor.Text50,
        "fill-12" to NiuniaColor.Fill12,
        "fill-24" to NiuniaColor.Fill24,
        "line" to NiuniaColor.Line,
        "live" to NiuniaColor.Live,
        "archive" to NiuniaColor.Archive,
        "switch-on" to NiuniaColor.SwitchOn,
        "shift" to NiuniaColor.Shift,
        "slider-fill" to NiuniaColor.SliderFill,
        "rating-1" to NiuniaColor.Rating1,
        "rating-2" to NiuniaColor.Rating2,
        "rating-3" to NiuniaColor.Rating3,
        "rating-4" to NiuniaColor.Rating4,
        "scrim" to NiuniaColor.Scrim,
        "scrim-30" to NiuniaColor.Scrim30,
        "glass" to NiuniaColor.Glass,
        "tray" to NiuniaColor.Tray,
        "badge-on-image" to NiuniaColor.BadgeOnImage,
        "shadow-ink" to NiuniaColor.ShadowInk,
        "night-tint" to NiuniaColor.NightTint,
        "safe-area" to NiuniaColor.SafeArea,
        "surface-variant" to NiuniaColor.SurfaceVariant,
        "on-surface-variant" to NiuniaColor.OnSurfaceVariant,
        "inverse-surface" to NiuniaColor.InverseSurface,
        "inverse-on-surface" to NiuniaColor.InverseOnSurface,
        "secondary-container" to NiuniaColor.SecondaryContainer,
        "inverse-primary" to NiuniaColor.InversePrimary,
        "tertiary" to NiuniaColor.Tertiary,
        "error" to NiuniaColor.Error,
        "error-container" to NiuniaColor.ErrorContainer,
        "border" to NiuniaColor.Border,
        "border-variant" to NiuniaColor.BorderVariant,
        "focus-ring" to NiuniaColor.FocusRing,
        "gloss" to NiuniaColor.Gloss,
        "delay" to NiuniaColor.Delay,
        "on-delay" to NiuniaColor.OnDelay,
        "fill-36" to NiuniaColor.Fill36,
        "chrome" to NiuniaColor.Chrome,
        "button" to NiuniaColor.Button,
        "frost" to NiuniaColor.Frost,
        "avatar" to NiuniaColor.Avatar,
        "on-avatar" to NiuniaColor.OnAvatar,
        "chip-on-image" to NiuniaColor.ChipOnImage,
        "on-image-ring" to NiuniaColor.OnImageRing,
        "stack-1" to NiuniaColor.Stack1,
        "stack-2" to NiuniaColor.Stack2,
        "on-focus-70" to NiuniaColor.OnFocus70,
        "line-strong" to NiuniaColor.LineStrong,
        "fmt" to NiuniaColor.Fmt,
        "on-fmt" to NiuniaColor.OnFmt,
        "live-badge" to NiuniaColor.LiveBadge,
        "haze" to NiuniaColor.Haze,
        "haze-edge" to NiuniaColor.HazeEdge,
        "haze-flat" to NiuniaColor.HazeFlat,
        "glass-flat" to NiuniaColor.GlassFlat,
        "tray-flat" to NiuniaColor.TrayFlat,
        "frost-flat" to NiuniaColor.FrostFlat,
    )
    val spacing: Map<String, Dp> = mapOf(
        "space-4" to NiuniaSpacing.Space4,
        "space-8" to NiuniaSpacing.Space8,
        "space-12" to NiuniaSpacing.Space12,
        "space-16" to NiuniaSpacing.Space16,
        "space-20" to NiuniaSpacing.Space20,
        "space-24" to NiuniaSpacing.Space24,
        "space-40" to NiuniaSpacing.Space40,
    )
    val radius: Map<String, Dp> = mapOf(
        "radius-6" to NiuniaRadius.Radius6,
        "radius-16" to NiuniaRadius.Radius16,
        "radius-28" to NiuniaRadius.Radius28,
        "radius-full" to NiuniaRadius.RadiusFull,
    )
    val size: Map<String, Dp> = mapOf(
        "h-36" to NiuniaSize.H36,
        "h-44" to NiuniaSize.H44,
        "h-56" to NiuniaSize.H56,
        "icon-16" to NiuniaSize.Icon16,
        "icon-20" to NiuniaSize.Icon20,
        "icon-24" to NiuniaSize.Icon24,
        "icon-28" to NiuniaSize.Icon28,
        "button-min-w" to NiuniaSize.ButtonMinW,
        "poster-w" to NiuniaSize.PosterW,
        "poster-h" to NiuniaSize.PosterH,
        "landscape-w" to NiuniaSize.LandscapeW,
        "landscape-h" to NiuniaSize.LandscapeH,
        "youtube-w" to NiuniaSize.YoutubeW,
        "youtube-h" to NiuniaSize.YoutubeH,
        "featured-w" to NiuniaSize.FeaturedW,
        "featured-h" to NiuniaSize.FeaturedH,
        "channel-w" to NiuniaSize.ChannelW,
        "channel-h" to NiuniaSize.ChannelH,
        "next-w" to NiuniaSize.NextW,
        "next-h" to NiuniaSize.NextH,
        "day-w" to NiuniaSize.DayW,
        "day-h" to NiuniaSize.DayH,
        "avatar-44" to NiuniaSize.Avatar44,
        "avatar-72" to NiuniaSize.Avatar72,
        "avatar-120" to NiuniaSize.Avatar120,
        "key-w" to NiuniaSize.KeyW,
        "epg-channel-col" to NiuniaSize.EpgChannelCol,
        "switch-w" to NiuniaSize.SwitchW,
        "switch-h" to NiuniaSize.SwitchH,
        "switch-knob" to NiuniaSize.SwitchKnob,
        "panel-w" to NiuniaSize.PanelW,
        "dialog-w" to NiuniaSize.DialogW,
        "popover-w" to NiuniaSize.PopoverW,
        "hero-h" to NiuniaSize.HeroH,
        "stage-w" to NiuniaSize.StageW,
        "stage-h" to NiuniaSize.StageH,
        "content-right" to NiuniaSize.ContentRight,
        "fade-edge" to NiuniaSize.FadeEdge,
        "hero-fade" to NiuniaSize.HeroFade,
        "still-w" to NiuniaSize.StillW,
        "still-h" to NiuniaSize.StillH,
        "spotlight-w" to NiuniaSize.SpotlightW,
        "spotlight-h" to NiuniaSize.SpotlightH,
        "pager-card-w" to NiuniaSize.PagerCardW,
        "awards-text-w" to NiuniaSize.AwardsTextW,
        "avatar-84" to NiuniaSize.Avatar84,
        "result-w" to NiuniaSize.ResultW,
        "result-h" to NiuniaSize.ResultH,
        "result-thumb-w" to NiuniaSize.ResultThumbW,
        "result-thumb-h" to NiuniaSize.ResultThumbH,
        "hero-info-w" to NiuniaSize.HeroInfoW,
        "split-col" to NiuniaSize.SplitCol,
        "split-right" to NiuniaSize.SplitRight,
        "sidebar-w" to NiuniaSize.SidebarW,
        "player-panel-w" to NiuniaSize.PlayerPanelW,
        "epg-slot-w" to NiuniaSize.EpgSlotW,
        "epg-cell-h" to NiuniaSize.EpgCellH,
        "logo-hero-w" to NiuniaSize.LogoHeroW,
        "logo-player-w" to NiuniaSize.LogoPlayerW,
        "logo-small-w" to NiuniaSize.LogoSmallW,
        "logo-page-h" to NiuniaSize.LogoPageH,
        "logo-poster-w" to NiuniaSize.LogoPosterW,
        "logo-poster-h" to NiuniaSize.LogoPosterH,
        "peek" to NiuniaSize.Peek,
        "nav-top" to NiuniaSize.NavTop,
        "content-top" to NiuniaSize.ContentTop,
        "progress-h" to NiuniaSize.ProgressH,
        "track-h" to NiuniaSize.TrackH,
        "knob" to NiuniaSize.Knob,
        "dot" to NiuniaSize.Dot,
        "caret-w" to NiuniaSize.CaretW,
        "keyboard-gap" to NiuniaSize.KeyboardGap,
        "chip-h" to NiuniaSize.ChipH,
        "tag-h" to NiuniaSize.TagH,
        "stack-offset" to NiuniaSize.StackOffset,
        "avatar-240" to NiuniaSize.Avatar240,
        "stroke-1" to NiuniaSize.Stroke1,
        "stroke-2" to NiuniaSize.Stroke2,
        "stroke-3" to NiuniaSize.Stroke3,
        "tree-max-h" to NiuniaSize.TreeMaxH,
        "player-peek" to NiuniaSize.PlayerPeek,
        "track-h-focus" to NiuniaSize.TrackHFocus,
        "rating-cell-w" to NiuniaSize.RatingCellW,
        "rating-label-w" to NiuniaSize.RatingLabelW,
    )
    val type: Map<String, TextStyle> = mapOf(
        "caption" to NiuniaType.Caption,
        "caption-regular" to NiuniaType.CaptionRegular,
        "body-small" to NiuniaType.BodySmall,
        "label-small" to NiuniaType.LabelSmall,
        "body" to NiuniaType.Body,
        "label" to NiuniaType.Label,
        "title-section" to NiuniaType.TitleSection,
        "title-screen" to NiuniaType.TitleScreen,
        "display" to NiuniaType.Display,
        "title-shelf" to NiuniaType.TitleShelf,
    )
}
