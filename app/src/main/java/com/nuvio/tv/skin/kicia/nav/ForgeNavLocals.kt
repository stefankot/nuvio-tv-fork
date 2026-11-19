package com.nuvio.tv.skin.kicia.nav

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/** v7.4: the bar's "take the focus" action, handed down to the tab screens (null outside the bar's scaffold). */
val LocalForgeBarFocus = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * v7.4 (ZACHOWANIE, kolejność W): Back in the content of a tab screen goes to the bar (the current tab), not to Home.
 * Put it first in the tab's screen: the screen's own BackHandlers, composed after it, still win.
 */
@Composable
fun ForgeTabBackToBar() {
    val focusBar = LocalForgeBarFocus.current
    BackHandler(enabled = focusBar != null) { focusBar?.invoke() }
}

/** v7.4: one-shot requests between the bar and the screens it opens. */
object ForgeNavState {
    /** Historia was opened from the avatar menu: Back from it shows the menu again (1.13). */
    var reopenAvatarMenu by mutableStateOf(false)
    /** Settings should open on this category (name of SettingsCategory), e.g. long OK on Tryb nocny (1.15). */
    var settingsCategory by mutableStateOf<String?>(null)
    /** Live TV (6.2): focus is in the tools row or the grid, the content moved up and the bar tucks away. */
    var liveTvLifted by mutableStateOf(false)
    /** Back from a tab on the bar went to Home (1.19/1.23): Home keeps the focus on its tab instead of taking it for the hero. */
    var homeKeepsBarFocus = false
    /** Debug builds (pomiar 15): "essential" / "advanced" asked by `am start --es forge_mode`; applied by Settings on the active profile. */
    var debugExperienceMode: String? = null
    /** Debug builds: show the basic settings for this process (pomiar 15), whatever the stored mode. */
    var debugBasicSettings by mutableStateOf(false)
    /** Debug builds: the source list never auto-plays for this process (pomiar 05). */
    var debugManualSources = false
}

/** v7.4: the name of the element in the mockup (its CSS class, `class.N` = N-th by y, x), read by narzedzia/roznice.py. */
fun Modifier.forgeTag(name: String): Modifier = if (name.isEmpty()) this else this.testTag(name)

/** v7.4: put on the root of a window so the tags above show up as resource-id in `uiautomator dump`. */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.forgeTagRoot(): Modifier = this.semantics { testTagsAsResourceId = true }

/**
 * Fork 19.11 (Calendar z Echo): ekran zakładki bez własnej obsługi paska Kicii — Wstecz idzie na pasek, a ▲, gdy
 * wyżej nie ma już nic do fokusu, też na pasek. Poza paskiem Kicii (null) nic nie zmienia.
 */
@Composable
fun ForgeTabScreenToBar(content: @Composable () -> Unit) {
    ForgeTabBackToBar()
    val focusBar = LocalForgeBarFocus.current
    val fm = androidx.compose.ui.platform.LocalFocusManager.current
    androidx.compose.foundation.layout.Box(Modifier.forgeUpToBar(focusBar, fm)) { content() }
}

private fun Modifier.forgeUpToBar(focusBar: (() -> Unit)?, fm: androidx.compose.ui.focus.FocusManager): Modifier =
    if (focusBar == null) this else this.then(Modifier.onPreviewKeyEvent { e ->
        if (e.type != androidx.compose.ui.input.key.KeyEventType.KeyDown || e.key != androidx.compose.ui.input.key.Key.DirectionUp) return@onPreviewKeyEvent false
        if (!fm.moveFocus(androidx.compose.ui.focus.FocusDirection.Up)) focusBar()
        true
    })
