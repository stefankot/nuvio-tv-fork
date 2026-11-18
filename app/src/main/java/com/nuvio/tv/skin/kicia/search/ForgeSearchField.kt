package com.nuvio.tv.skin.kicia.search

import android.view.KeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Up in the one-line search field leaves it for the bar above (mockup: the field is just one stop on the way
 * up). Without this the field keeps Up for its cursor, so with a keyboard attached the screen felt stuck.
 */
fun forgeSearchFieldKeys(): Modifier =
    if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_NAV) Modifier.forgeUpLeavesField() else Modifier

fun Modifier.forgeUpLeavesField(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    onPreviewKeyEvent { event ->
        if (event.nativeKeyEvent.keyCode != KeyEvent.KEYCODE_DPAD_UP) return@onPreviewKeyEvent false
        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
            keyboard?.hide()
            focusManager.moveFocus(FocusDirection.Up)
        }
        true
    }
}
