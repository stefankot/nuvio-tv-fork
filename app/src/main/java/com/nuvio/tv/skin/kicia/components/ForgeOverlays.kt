package com.nuvio.tv.skin.kicia.components

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeMotion
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/**
 * Moves focus into an overlay when it opens and back to where it came from when it closes
 * (rule: after closing an overlay, focus returns to the element that opened it).
 */
@Composable
private fun OverlayFocus(visible: Boolean, enter: FocusRequester, returnTo: FocusRequester?) {
    var wasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            wasVisible = true
            runCatching { enter.requestFocus() }
        } else if (wasVisible) {
            wasVisible = false
            runCatching { returnTo?.requestFocus() }
        }
    }
}

/**
 * Side panel, 392 dp wide, 16 dp from the screen edge, 24 dp corners (options, sources, subtitles,
 * audio, "more", info, programme actions). Focus stays inside while it is open; Back closes it.
 * The blur behind the glass is a host concern - this draws the translucent surface only.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ForgeSideSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    returnFocusTo: FocusRequester? = null,
    /** v7.4: the element the focus lands on when the panel opens (default: the first focusable one). */
    initialFocus: FocusRequester? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val enter = remember { FocusRequester() }
    OverlayFocus(visible, enter, returnFocusTo)
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(modifier.fillMaxSize()) {
        // makieta 7.7 A9: a curtain under the Panel (API 30 = the JMGO: 60% without blur; the blur needs a host haze)
        AnimatedVisibility(visible = visible, enter = fadeIn(ForgeMotion.fadeSpec()), exit = fadeOut(ForgeMotion.fadeSpec())) {
            Box(Modifier.fillMaxSize().background(ForgeColors.Scrim))
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(ForgeMotion.scrollSpec()) { it } + fadeIn(ForgeMotion.fadeSpec()),
            exit = slideOutHorizontally(ForgeMotion.scrollSpec()) { it } + fadeOut(ForgeMotion.fadeSpec()),
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = ForgeDims.SideSheetMargin, horizontal = ForgeDims.SideSheetMargin)
                    .width(ForgeDims.SideSheetWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = ForgeDims.RadiusSheet, bottomStart = ForgeDims.RadiusSheet)) // v7.4: left corners only
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()) // fork 19.11: dłuższe panele („Więcej”) nie mieściły się na ekranie
                    .background(ForgeColors.GlassFallback) // v7.4: --szklo-zapas (szkło bez rozmycia)
                    .forgeEdge(RoundedCornerShape(topStart = ForgeDims.RadiusSheet, bottomStart = ForgeDims.RadiusSheet)) // makieta 7.7 A8
                    .testTag("panel")
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .focusProperties {
                        exit = { FocusRequester.Cancel }
                        if (initialFocus != null) this.enter = { _ -> initialFocus ?: FocusRequester.Default } // v7.4 (this.: the local val `enter` would shadow the property)
                    }
                    .focusRequester(enter)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp), // v7.4: wiersze panelu co 8
            ) {
                Text(title, style = ForgeText.head)
                if (subtitle != null) Text(subtitle, style = ForgeText.label5)
                Spacer(Modifier.height(0.dp)) // v7.4: tytuł 26, 16, pierwszy wiersz (8 + 0 + 8)
                content()
            }
        }
    }
}

/** v7 Popover: 240 dp wide, rows 44, glass, anchored at (x, y) from the top left; the screen behind is dimmed 60%. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ForgePopover(
    onDismiss: () -> Unit,
    x: androidx.compose.ui.unit.Dp,
    y: androidx.compose.ui.unit.Dp,
    /** v7.4: 240, or 280 for popover-szeroki (10 Biblioteka). */
    width: androidx.compose.ui.unit.Dp = 240.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val enter = remember { FocusRequester() }
    OverlayFocus(true, enter, null)
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        // v7.4 (01): under the avatar menu the screen is dimmed 60% but the bar stays visible
        Box(Modifier.fillMaxSize().padding(top = if (y == 68.dp) 68.dp else 0.dp).background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)))
        Column(
            modifier = Modifier
                .padding(start = x, top = y)
                .width(width)
                .clip(RoundedCornerShape(ForgeDims.RadiusSheet))
                .background(ForgeColors.GlassFallback) // v7.4: --szklo-zapas
                .forgeEdge(RoundedCornerShape(ForgeDims.RadiusSheet)) // makieta 7.7 A8
                .testTag("popover")
                .padding(8.dp)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusRequester(enter)
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(0.dp), // v7.4: rows 44 without gaps
            content = content,
        )
    }
}

/** One row of a side sheet: 52 dp, 14 dp corners, white when focused. */
@Composable
fun ForgeSheetRow(
    text: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    enabled: Boolean = true,
    onLongOk: (() -> Unit)? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null, // v7.4: leading icon 16
    tile: Boolean = false, // v7.4: row on the surface (#3A3A3C) at rest, as in panels
    focusRequester: FocusRequester? = null,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null, // chevron or check
    description: String? = null, // v7.4: Wiersz 56 (tytuł 16/22 + opis 14/20)
    rail: Boolean = false, // v7.4: Wiersz 36 (szyna), tytuł 14/20
    selected: Boolean = false, // v7.4: wybrany = 24%
    switch: Boolean? = null, // v7.4: przełącznik po prawej
    onOk: () -> Unit,
) {
    val rowHeight = if (rail) 36.dp else if (description != null) 56.dp else ForgeDims.RowHeight
    ForgeFocusBox(
        radius = ForgeDims.RadiusRow,
        modifier = modifier.fillMaxWidth().height(rowHeight).alpha(if (enabled) 1f else .5f), // v7.4: 44 / 56 / 36; wyłączony = krycie .5
        scaleOnFocus = false,
        enabled = enabled,
        focusRequester = focusRequester,
        onLongOk = onLongOk,
        onOk = onOk,
    ) { focused ->
        val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .clip(RoundedCornerShape(ForgeDims.RadiusRow))
                .background(if (focused) ForgeColors.Focus else if (selected) ForgeColors.Surface3 else if (tile) ForgeColors.Surface2 else Color.Transparent)
                .padding(start = 16.dp, end = if (trailingIcon != null) 12.dp else 16.dp), // v7.4: szewron kończy się 12 od krawędzi
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp), // v7.4: gap od-12
        ) {
            if (icon != null) androidx.compose.material3.Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp)) // v7.4: ikona 20
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text, style = (if (rail) ForgeText.label else ForgeText.button).copy(color = fg), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (description != null) Text(description, style = ForgeText.label5.copy(color = if (focused) fg else ForgeColors.Text2), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            if (detail != null) Text(detail, style = ForgeText.body.copy(color = if (focused) fg else ForgeColors.Text2), maxLines = 1) // v7.4: wartość 16/22, 70%
            if (switch != null) ForgeSwitch(switch)
            if (trailingIcon != null) androidx.compose.material3.Icon(trailingIcon, null, tint = fg, modifier = Modifier.size(16.dp))
        }
    }
}

data class ForgeDialogButton(val text: String, val primary: Boolean = false, val onOk: () -> Unit)

/**
 * Centered dialog, 440 dp (exit, PIN). Focus starts on [initialFocusIndex] (for "Close the app?" that is
 * "Zostań"). Back dismisses. No key hints inside, on purpose.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ForgeDialog(
    visible: Boolean,
    title: String,
    buttons: List<ForgeDialogButton>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    initialFocusIndex: Int = 0,
    returnFocusTo: FocusRequester? = null,
    note: String? = null, // v7.6: explanation under the message, smaller
    topContent: (@Composable () -> Unit)? = null, // v7.6: e.g. the channel logo above the title
) {
    val requesters = remember(buttons.size) { List(buttons.size) { FocusRequester() } }
    val group = remember { FocusRequester() }
    var wasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            wasVisible = true
            runCatching { requesters.getOrNull(initialFocusIndex)?.requestFocus() }
        } else if (wasVisible) {
            wasVisible = false
            runCatching { returnFocusTo?.requestFocus() }
        }
    }
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // makieta 7.7 A9: a curtain under the Okno (API 30 = the JMGO: 60% without blur)
        AnimatedVisibility(visible = visible, enter = fadeIn(ForgeMotion.fadeSpec()), exit = fadeOut(ForgeMotion.fadeSpec())) {
            Box(Modifier.fillMaxSize().background(ForgeColors.Scrim))
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(ForgeMotion.fadeSpec()),
            exit = fadeOut(ForgeMotion.fadeSpec()),
        ) {
            Column(
                modifier = Modifier
                    .width(ForgeDims.DialogWidth)
                    .clip(RoundedCornerShape(ForgeDims.RadiusSheet))
                    .background(ForgeColors.GlassFallback) // v7.4: --szklo-zapas
                    .forgeEdge(RoundedCornerShape(ForgeDims.RadiusSheet)) // makieta 7.7 A8
                    .testTag("okno")
                    .padding(24.dp) // v7.4: okno 440, padding 24
                    .focusProperties { exit = { FocusRequester.Cancel } }
                    .focusRequester(group)
                    .focusGroup(),
            ) {
                if (topContent != null) { topContent(); Spacer(Modifier.height(8.dp)) }
                Text(title, style = ForgeText.head)
                if (message != null) { Spacer(Modifier.height(12.dp)); Text(message, style = ForgeText.body) }
                if (note != null) { Spacer(Modifier.height(8.dp)); Text(note, style = ForgeText.caption.copy(color = ForgeColors.Text3)) }
                Spacer(Modifier.height(24.dp)) // v7.4: title 26, 12, text 22, 24, buttons 44
                // v7.4: przyciski do prawej, odstęp 12, sąsiad odsuwa się przy fokusie
                ForgeSpreadRow(Modifier.fillMaxWidth(), gap = 12.dp, alignEnd = true) {
                    buttons.forEachIndexed { i, b ->
                        Item(i) {
                            ForgeTagged("przycisk.$i") {
                                ForgeButton(text = b.text, primary = b.primary, focusRequester = requesters[i], onOk = b.onOk)
                            }
                        }
                    }
                }
            }
        }
    }
}
