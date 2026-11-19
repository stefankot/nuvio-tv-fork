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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.nuvio.tv.ui.reshaped.pillnav.PillNavBarState
import com.nuvio.tv.ui.reshaped.pillnav.PillNavEntry
import com.nuvio.tv.ui.reshaped.pillnav.PillNavEntryKind

private const val TAG = "ForkNiunia"

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
    val pillAlpha by animateFloatAsState(if (open || hidden) 0f else 1f, tween(200), label = "niuniaNavPill")
    // Kolejność panelu jak w projekcie: profil, potem zakładki (Szukaj, Główna, …), na końcu Ustawienia.
    val profile = entries.firstOrNull { it.kind == PillNavEntryKind.Profile }
    val items = entries.filter { it.kind != PillNavEntryKind.Profile }
    val ordered = listOfNotNull(profile) + items
    val selected = entries.firstOrNull { it.key == selectedKey } ?: items.firstOrNull()

    Box(modifier.fillMaxSize()) {
        // Przyciemnienie od lewej, gdy panel jest otwarty (.scrim-side).
        if (openFraction > 0f) Box(
            Modifier.fillMaxSize().alpha(openFraction)
                .background(Brush.horizontalGradient(0f to Color(0x80000000), .45f to Color.Transparent))
        )
        // Zwinięta pigułka: tylko obraz (nie łapie fokusu), znika przy otwartym panelu i przy przewijaniu w dół.
        if (selected != null && pillAlpha > 0f) Row(
            Modifier.padding(start = 20.dp, top = 28.dp).alpha(pillAlpha),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("‹", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NiuniaColors.Text70))
            Row(
                Modifier.height(34.dp).shadow(6.dp, CircleShape, clip = false).clip(CircleShape).background(NiuniaColors.Chrome)
                    .border(1.dp, Color(0x14FFFFFF), CircleShape).padding(start = 4.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EntryIcon(selected, size = 26, bg = Color(0x33FFFFFF), tint = NiuniaColors.Text, activeProfileColorHex)
                Text(selected.label, style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = NiuniaColors.Text), maxLines = 1)
            }
        }
        // Panel boczny. Zawsze w drzewie, żeby PillNavScaffold mógł dać fokus pozycji (requesterFor); zamknięty = przezroczysty.
        Column(
            Modifier
                .padding(start = 18.dp, top = 18.dp)
                .width(180.dp)
                .graphicsLayer { alpha = openFraction; translationX = (1f - openFraction) * -24.dp.toPx() }
                .shadow(if (open) 20.dp else 0.dp, RoundedCornerShape(22.dp), clip = false)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xBD2C2C2E))
                .border(1.dp, NiuniaColors.Line, RoundedCornerShape(22.dp))
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .onFocusChanged { if (state.hasFocus != it.hasFocus) { state.hasFocus = it.hasFocus; Log.i(TAG, "menu: " + if (it.hasFocus) "otwarte" else "zamknięte") } }
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val i = ordered.indexOfFirst { it.key == state.focusedKey }
                    when (e.key) {
                        Key.DirectionUp -> { ordered.getOrNull(i - 1)?.let { runCatching { requesterFor(it.key).requestFocus() }.onFailure { t -> Log.w(TAG, "menu ▲: ${t.javaClass.simpleName}: ${t.message}") } }; true }
                        Key.DirectionDown -> { val n = ordered.getOrNull(i + 1); if (n != null) runCatching { requesterFor(n.key).requestFocus() }.onFailure { t -> Log.w(TAG, "menu ▼: ${t.javaClass.simpleName}: ${t.message}") } else onExitDown(); true }
                        Key.DirectionRight -> { onExitDown(); true }
                        Key.DirectionLeft -> true
                        else -> false
                    }
                },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ordered.forEach { entry ->
                if (entry.kind == PillNavEntryKind.Profile) ProfileRow(entry, activeProfileColorHex, interactive && open || interactive && entry.key == selected?.key, requesterFor(entry.key), state, onEntryClick)
                else SideItem(entry, entry.key == selectedKey, interactive && (open || entry.key == selected?.key), requesterFor(entry.key), state, activeProfileColorHex, onEntryClick)
            }
        }
    }
}

@Composable
private fun SideItem(
    entry: PillNavEntry, isSelected: Boolean, focusable: Boolean, requester: FocusRequester,
    state: PillNavBarState, profileHex: String, onClick: (PillNavEntry) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, tween(160), label = "niuniaSideScale")
    val bg = when { focused -> NiuniaColors.Focus; isSelected -> Color(0x29FFFFFF); else -> Color.Transparent }
    val fg = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text
    Row(
        Modifier.fillMaxWidth().height(36.dp)
            .focusRequester(requester)
            .focusProperties { canFocus = focusable }
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) state.focusedKey = entry.key }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 10.dp else 0.dp, CircleShape, clip = false)
            .clip(CircleShape).background(bg)
            .clickable(remember { MutableInteractionSource() }, indication = null) { onClick(entry) }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EntryIcon(entry, size = 22, bg = if (focused) Color(0x1A000000) else Color(0x29FFFFFF), tint = fg, profileHex)
        Text(entry.label, style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProfileRow(
    entry: PillNavEntry, hex: String, focusable: Boolean, requester: FocusRequester,
    state: PillNavBarState, onClick: (PillNavEntry) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .focusRequester(requester)
            .focusProperties { canFocus = focusable }
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) state.focusedKey = entry.key }
            .clip(RoundedCornerShape(18.dp)).background(if (focused) NiuniaColors.Focus else Color.Transparent)
            .clickable(remember { MutableInteractionSource() }, indication = null) { onClick(entry) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Avatar(entry.label, hex, 30)
        Column {
            val fg = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text
            Text(entry.label, style = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.niunia_nav_switch_profile), style = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, color = if (focused) Color(0x99111111) else NiuniaColors.Text50))
        }
    }
}

@Composable
private fun Avatar(name: String, hex: String, size: Int) {
    val color = remember(hex) {
        runCatching { Color(android.graphics.Color.parseColor(hex)) }
            .onFailure { Log.w(TAG, "kolor profilu: ${it.javaClass.simpleName}: ${it.message}") }
            .getOrDefault(Color(0xFFA5DCB0))
    }
    Box(Modifier.size(size.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14361F)))
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
