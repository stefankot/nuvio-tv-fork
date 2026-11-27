package com.nuvio.tv.skin.niunia.nav

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import coil3.request.transformations
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import com.nuvio.tv.R
import com.nuvio.tv.skin.api.SkinId
import com.nuvio.tv.skin.api.rememberSkinId
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.ui.reshaped.pillnav.PillNavBarState
import com.nuvio.tv.ui.reshaped.pillnav.PillNavEntry
import com.nuvio.tv.ui.reshaped.pillnav.PillNavEntryKind
import kotlinx.coroutines.launch

private const val TAG = "ForkNiunia"

/**
 * Fork 20.11 (właściciel: „po wyborze Główna menu zamyka się na chwilę i wraca; Wstecz i ▶ mają wychodzić z menu”):
 * ekran Niuni podaje tu element, na który wraca fokus po wyjściu z menu (Główna: przycisk hero). Bez niego — ▶ w treść.
 */
object NiuniaContentFocus {
    var requester: FocusRequester? = null
}

/** Stan ekranów Niuni ważny dla paska menu (UX JMGO 24.11). */
object NiuniaNavChrome {
    /** Główna: strona pod hero (rzędy) na wierzchu — pigułka menu schowana, żeby nie zasłaniała tytułu rzędu. */
    val homeRows = androidx.compose.runtime.mutableStateOf(false)
}

/** Przejście do zakładki menu z ekranu Niuni (np. pusta biblioteka → Główna); ustawia [NiuniaNavBar]. */
object NiuniaNavActions {
    @Volatile var openRoute: ((String) -> Boolean)? = null
    fun open(route: String): Boolean = openRoute?.invoke(route) ?: false.also { Log.w(TAG, "menu: brak paska — nie przejdę do $route") }
    /** Fokus na menu (bieżąca zakładka) — dla ekranów, z których ▲ nie trafia w menu samo (np. siatka Biblioteki). */
    @Volatile var focusMenu: (() -> Boolean)? = null
    fun toMenu(why: String): Boolean = (focusMenu?.invoke() ?: false).also { Log.i(TAG, "menu ← $why: ${if (it) "otwarte" else "brak paska"}") }
}

/** Ekran Niuni: [requester] dostaje fokus po wyjściu z menu (dopóki ekran jest na ekranie). */
@Composable
fun NiuniaRegisterContentFocus(requester: FocusRequester?) {
    androidx.compose.runtime.DisposableEffect(requester) {
        NiuniaContentFocus.requester = requester
        onDispose { if (NiuniaContentFocus.requester === requester) NiuniaContentFocus.requester = null }
    }
}

/** Wpięcie (PillNavigationBar): true = aktywna skórka Niunia, pasek rysuje [NiuniaNavBar]. */
@Composable
fun niuniaNavActive(): Boolean = rememberSkinId() == SkinId.NIUNIA

/**
 * Nawigacja Niunii (canvas: `.nav`/`.navpill` + `.side`, ekran Sidebar).
 * Zwinięta: „‹ [ikona] Zakładka” w lewym górnym rogu (20, 28). Fokus na menu (Wstecz albo ▲ z treści — obsługuje
 * PillNavScaffold) otwiera panel boczny 180 dp: profil na górze, potem zakładki; ▲▼ po pozycjach, ▶ / ▼ z ostatniej
 * wraca do treści. Przełączanie tras, Wstecz i powrót fokusu zostają w PillNavScaffold (te same requesterFor/state).
 */
@Composable
internal fun NiuniaNavBar(
    entries: List<PillNavEntry>,
    selectedKey: String?,
    state: PillNavBarState,
    requesterFor: (String) -> FocusRequester,
    interactive: Boolean,
    hidden: Boolean,
    activeProfileColorHex: String,
    onEntryClick: (PillNavEntry) -> Unit,
    onExitDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val open = state.hasFocus
    val openFraction by animateFloatAsState(if (open) 1f else 0f, tween(220), label = "niuniaNavOpen")
    // UX JMGO (ręczne testy 24.11): Główna wracała od razu w rzędach (po odtwarzaczu, profilu) z widoczną pigułką na tytule
    // „Oglądaj dalej” — pigułka chowa się, dopóki Główna jest pod hero (nie tylko po ▼).
    val homeRows = NiuniaNavChrome.homeRows.value && entries.firstOrNull { it.route == "home" }?.key == selectedKey
    val pillAlpha by animateFloatAsState(if (open || hidden || homeRows) 0f else 1f, tween(200), label = "niuniaNavPill")
    // Kolejność panelu jak w projekcie: profil, potem zakładki (Szukaj, Główna, …), na końcu Ustawienia.
    val profile = entries.firstOrNull { it.kind == PillNavEntryKind.Profile }
    val items = entries.filter { it.kind != PillNavEntryKind.Profile }
    val ordered = listOfNotNull(profile) + items
    val selected = entries.firstOrNull { it.key == selectedKey } ?: items.firstOrNull()
    val focusManager = LocalFocusManager.current
    // Wyjście z panelu do treści ekranu. Fork 20.11: na czas wyjścia pozycje menu nie przyjmują fokusu — wcześniej, gdy
    // w prawo nic nie było, onExitDown robił „fokus w dół”, który w pionowym panelu trafiał w następną pozycję menu
    // (menu zamykało się na chwilę i wracało). Kolejność: element ekranu Niuni → w prawo → requester treści.
    var leaving by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val ctx = LocalContext.current
    fun exitToContent(why: String) {
        scope.launch {
            leaving = true
            withFrameNanos { }
            val target = NiuniaContentFocus.requester
            var how = "element ekranu"
            val ok = target != null && try { target.requestFocus(); true } catch (e: IllegalStateException) { Log.w(TAG, "menu → treść (element ekranu): ${e.javaClass.simpleName}: ${e.message}"); false }
            if (!ok) {
                how = "w prawo"
                val moved = try { focusManager.moveFocus(FocusDirection.Right) } catch (e: IllegalStateException) { Log.w(TAG, "menu → treść: ${e.javaClass.simpleName}: ${e.message}"); false }
                if (!moved || state.hasFocus) { how = "requester treści"; onExitDown() }
            }
            withFrameNanos { }
            withFrameNanos { }
            leaving = false
            Log.i(TAG, "menu → treść ($why): $how, menu otwarte=${state.hasFocus}")
            if (state.hasFocus) com.nuvio.tv.fork.ForkLog.say(ctx, TAG, "Nie udało się zamknąć menu")
        }
    }
    // OK na zakładce: po przejściu na nowy ekran fokus idzie do jego treści (nie zostaje w menu).
    var exitAfterSwitch by remember { mutableStateOf(false) }
    LaunchedEffect(exitAfterSwitch, selectedKey) {
        if (!exitAfterSwitch) return@LaunchedEffect
        repeat(4) { withFrameNanos { } }
        if (state.hasFocus) exitToContent("po wyborze zakładki")
        exitAfterSwitch = false
    }
    val homeKey = items.firstOrNull { it.route == "home" }?.key
    androidx.compose.runtime.SideEffect {
        NiuniaNavActions.openRoute = { route ->
            val e = items.firstOrNull { it.route == route }
            if (e == null) false else { Log.i(TAG, "menu: przejście z ekranu → $route"); onEntryClick(e); true }
        }
        NiuniaNavActions.focusMenu = {
            val k = selected?.key
            k != null && try { requesterFor(k).requestFocus(); true } catch (e: IllegalStateException) { Log.w(TAG, "menu ←: ${e.javaClass.simpleName}: ${e.message}"); false }
        }
    }

    // Szkło (fork/glass): menu leży poza treścią ekranów — ten sam manager, próbka warstwy ekranu.
    com.nuvio.tv.fork.glass.ForkGlass.Provide {
    Box(modifier.fillMaxSize()) {
        // Przyciemnienie od lewej, gdy panel jest otwarty (.scrim-side).
        // Przegląd 27.11 (CAŁA APLIKACJA): pod otwartym menu rozmyta kopia tła bieżącego ekranu z lewej (gotowe rozmycie, maska do 45 %)
        // zamiast przyciemnienia ScrimSide; ekran bez obrazu tła (Live TV, ustawienia) — dawne przyciemnienie.
        if (openFraction > 0f) {
            val ctxNav = androidx.compose.ui.platform.LocalContext.current
            val px = with(androidx.compose.ui.platform.LocalDensity.current) { 60.dp.toPx() }
            val behind = com.nuvio.tv.skin.niunia.components.NiuniaBackdropNow.current
            val req = remember(behind) {
                if (behind == null || !com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(ctxNav, "niunia_blur_scrims", true) || !com.nuvio.tv.fork.images.NativeBlur.enabled(ctxNav)) null
                else coil3.request.ImageRequest.Builder(ctxNav).data(behind).transformations(com.nuvio.tv.fork.images.NativeBlur(px, diskKey = behind)).build()
                    .also { Log.i(TAG, "menu: pod panelem rozmyta kopia tła ekranu zamiast przyciemnienia") }
            }
            if (req != null) coil3.compose.AsyncImage(
                model = req, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(androidx.compose.ui.graphics.Color(0x4D000000), androidx.compose.ui.graphics.BlendMode.SrcAtop),
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = openFraction; compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(androidx.compose.ui.graphics.Brush.horizontalGradient(0f to androidx.compose.ui.graphics.Color.Black, 0.3f to androidx.compose.ui.graphics.Color.Black, 0.5f to androidx.compose.ui.graphics.Color.Transparent),
                            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
                    },
            ) else Box(
                Modifier.fillMaxSize().alpha(openFraction)
                    .background(com.nuvio.tv.skin.niunia.tokens.NiuniaGradient.ScrimSide.brush())
            )
        }
        // Zwinięta pigułka (NNavigation tab-pill, x 20, y 24): tylko obraz (nie łapie fokusu), znika przy otwartym panelu i przy przewijaniu w dół.
        if (selected != null && pillAlpha > 0f) com.nuvio.tv.skin.niunia.components.NiuniaTabPill(
            label = selected.label, icon = niuniaNavIcon(selected) ?: "home",
            modifier = Modifier.padding(start = NiuniaSpacing.Space20, top = NiuniaSize.NavTop).alpha(pillAlpha),
            leading = if (niuniaNavIcon(selected) == null) ({ EntryIcon(selected, size = 28, bg = Color.Transparent, tint = NiuniaColor.White, activeProfileColorHex) }) else null,
        )
        // Z-15: zegar w prawym górnym rogu ekranów z menu; Live TV i YouTube mają go na końcu własnego paska akcji.
        if (selected?.route != com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE && selected?.route != "youtube" && selected?.route != "search") com.nuvio.tv.skin.niunia.components.NiuniaClockCorner() // Szukaj: zegar obok „Odkrywaj” (NiuniaSearch)
        // Panel boczny. Zawsze w drzewie, żeby PillNavScaffold mógł dać fokus pozycji (requesterFor); zamknięty = przezroczysty.
        com.nuvio.tv.skin.niunia.components.NiuniaSidebarPanel(
            Modifier
                // Test JMGO build152: zamknięty panel leżał w swoim miejscu (przezroczysty), a wybrana pozycja przyjmuje fokus —
                // ▼ z kafli Live TV i ▲ z pigułek trafiały w nią w środku ekranu. Zamknięty = zaparkowany nad ekranem:
                // ▲ ze szczytu treści dalej otwiera menu, ze środka ekranu fokus idzie do najbliższego elementu treści.
                .offset { if (!open && openFraction == 0f) androidx.compose.ui.unit.IntOffset(0, -20_000) else androidx.compose.ui.unit.IntOffset.Zero }
                .graphicsLayer { alpha = openFraction; translationX = (1f - openFraction) * -24.dp.toPx() }
                .shadow(if (open) 20.dp else 0.dp, RoundedCornerShape(NiuniaRadius.Radius28), clip = false)
                .onFocusChanged { if (state.hasFocus != it.hasFocus) { state.hasFocus = it.hasFocus; Log.i(TAG, "menu: " + if (it.hasFocus) "otwarte" else "zamknięte") } }
                .onPreviewKeyEvent { e ->
                    // Wstecz w otwartym menu: zamyka je (fokus do treści) — także na Głównej (właściciel 20.11).
                    if (e.key == Key.Back) {
                        if (e.type == KeyEventType.KeyUp) exitToContent("Wstecz")
                        return@onPreviewKeyEvent true
                    }
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val i = ordered.indexOfFirst { it.key == state.focusedKey }
                    when (e.key) {
                        Key.DirectionUp -> { ordered.getOrNull(i - 1)?.let { runCatching { requesterFor(it.key).requestFocus() }.onFailure { t -> Log.w(TAG, "menu ▲: ${t.javaClass.simpleName}: ${t.message}") } }; true }
                        Key.DirectionDown -> { val n = ordered.getOrNull(i + 1); if (n != null) runCatching { requesterFor(n.key).requestFocus() }.onFailure { t -> Log.w(TAG, "menu ▼: ${t.javaClass.simpleName}: ${t.message}") } else exitToContent("▼ z ostatniej"); true }
                        Key.DirectionRight -> { exitToContent("▶"); true }
                        Key.DirectionLeft -> true
                        else -> false
                    }
                },
        ) {
            ordered.forEach { entry ->
                if (entry.kind == PillNavEntryKind.Profile) ProfileRow(entry, activeProfileColorHex, !leaving && (interactive && open || interactive && entry.key == selected?.key), requesterFor(entry.key), state, onEntryClick)
                else SideItem(entry, entry.key == selectedKey, !leaving && interactive && (open || entry.key == selected?.key), requesterFor(entry.key), state, activeProfileColorHex) {
                    if (it.key == selectedKey) {
                        // Ta sama zakładka (np. Główna na Głównej): bez ponownego przejścia — tylko wyjście do treści
                        // (ponowne wejście PillNavScaffold oddawało fokus treści i „w dół” z powrotem do menu).
                        Log.i(TAG, "menu: ta sama zakładka ${it.key} → treść")
                        exitToContent("ta sama zakładka")
                    } else {
                        onEntryClick(it)
                        exitAfterSwitch = true
                    }
                }
            }
        }
    }
    }
}

/** Pozycja menu = NListRow `nav`: pigułka 36, ikona w kółku 24, wybrana 24%, fokus białe tło (bez skali). */
@Composable
private fun SideItem(
    entry: PillNavEntry, isSelected: Boolean, focusable: Boolean, requester: FocusRequester,
    state: PillNavBarState, profileHex: String, onClick: (PillNavEntry) -> Unit,
) {
    val ds = niuniaNavIcon(entry)
    com.nuvio.tv.skin.niunia.components.NiuniaListRow(
        label = entry.label,
        onClick = { onClick(entry) },
        kind = com.nuvio.tv.skin.niunia.components.NiuniaRowKind.Nav,
        icon = ds,
        leading = if (ds == null) ({ f -> EntryIcon(entry, size = 24, bg = if (f) NiuniaColor.Fill12 else NiuniaColor.Fill24, tint = if (f) NiuniaColor.OnFocus else NiuniaColor.White, profileHex) }) else null,
        selected = isSelected,
        enabled = focusable,
        dimDisabled = false,
        focusRequester = requester,
        onFocus = { if (it) state.focusedKey = entry.key },
    )
}

/** Konto = NListRow `account`: awatar w kolorze profilu, imię i „Zmień profil”. */
@Composable
private fun ProfileRow(
    entry: PillNavEntry, hex: String, focusable: Boolean, requester: FocusRequester,
    state: PillNavBarState, onClick: (PillNavEntry) -> Unit,
) {
    com.nuvio.tv.skin.niunia.components.NiuniaListRow(
        label = entry.label,
        onClick = { com.nuvio.tv.skin.niunia.profile.NiuniaProfileSwitch.fromMenu = true; onClick(entry) },
        kind = com.nuvio.tv.skin.niunia.components.NiuniaRowKind.Account,
        supporting = stringResource(R.string.niunia_nav_switch_profile),
        leading = { Avatar(entry.label, hex, 32) },
        enabled = focusable,
        dimDisabled = false,
        focusRequester = requester,
        onFocus = { if (it) state.focusedKey = entry.key },
    )
}

/** Ikona design systemu dla trasy zakładki (makieta Sidebar); null = ikona zakładki z aplikacji. */
internal fun niuniaNavIcon(entry: PillNavEntry): String? = when (entry.route) {
    "search" -> "search"
    "home" -> "home"
    com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE -> "tv"
    "youtube" -> "youtube"
    "library" -> "library"
    "settings" -> "settings"
    "discover" -> "discover"
    else -> if (entry.kind == PillNavEntryKind.Settings) "settings" else null
}

@Composable
private fun Avatar(name: String, hex: String, size: Int) {
    val color = remember(hex) {
        runCatching { Color(android.graphics.Color.parseColor(hex)) }
            .onFailure { Log.w(TAG, "kolor profilu: ${it.javaClass.simpleName}: ${it.message}") }
            .getOrDefault(NiuniaColor.Avatar)
    }
    Box(Modifier.size(size.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), style = com.nuvio.tv.skin.niunia.tokens.NiuniaType.LabelSmall, color = NiuniaColor.OnAvatar)
    }
}

@Composable
private fun EntryIcon(entry: PillNavEntry, size: Int, bg: Color, tint: Color, profileHex: String) {
    if (entry.kind == PillNavEntryKind.Profile) { Avatar(entry.label, profileHex, size); return }
    Box(Modifier.size(size.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        val icon = (size * 0.55f).dp
        when {
            entry.icon != null -> Icon(entry.icon, contentDescription = null, tint = tint, modifier = Modifier.size(icon))
            entry.iconRes != null -> Icon(rememberRawSvgPainter(entry.iconRes, icon), contentDescription = null, tint = tint, modifier = Modifier.size(icon))
            else -> Text(entry.label.take(1), style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tint))
        }
    }
}

/** Ikony zakładek to pliki SVG z res/raw (jak w PillNavigationBar) — painterResource ich nie czyta (crash na starcie). */
@Composable
private fun rememberRawSvgPainter(rawIconRes: Int, size: Dp): Painter {
    val px = with(LocalDensity.current) { size.roundToPx() }
    return rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current).data(rawIconRes).size(px).build()
    )
}
