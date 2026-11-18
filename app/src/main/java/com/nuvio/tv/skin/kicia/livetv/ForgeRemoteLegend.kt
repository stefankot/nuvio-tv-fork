package com.nuvio.tv.skin.kicia.livetv

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.skin.kicia.components.ForgeDialog
import com.nuvio.tv.skin.kicia.components.ForgeDialogButton

/**
 * Makieta 7.7b H9: the „Przytrzymaj przycisk” legend — only after a long press (never when the MiniGuide shows), with
 * the pressed direction named. „Nie pokazuj więcej” turns it off for good (Odtwarzanie → „Podpowiedź przytrzymania”).
 * Not after a long ▲ (Home) or a long Back (leaving).
 */
object ForgeRemoteLegend {
    private const val PREFS = "forge_remote_legend"
    private const val KEY_OFF = "off"
    /** The key code of the long press the legend explains; -1 = hidden. */
    var pressed by mutableIntStateOf(-1)
        internal set
    private var off: Boolean? = null

    fun offer(code: Int) { if (off != true) pressed = code }
    fun dismiss() { pressed = -1 }

    internal fun isOff(context: Context): Boolean =
        off ?: context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_OFF, false).also { off = it }

    internal fun turnOff(context: Context) {
        off = true
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_OFF, true).apply()
    }
}

@Composable
internal fun ForgeRemoteLegendDialog() {
    val context = LocalContext.current
    val code = ForgeRemoteLegend.pressed
    if (code != -1 && ForgeRemoteLegend.isOff(context)) { ForgeRemoteLegend.pressed = -1; return }
    fun mark(c: Int, text: String) = if (c == code) "● $text" else text
    ForgeDialog(
        visible = code != -1,
        title = "Przytrzymaj przycisk",
        message = listOf(
            mark(android.view.KeyEvent.KEYCODE_DPAD_UP, "▲ Menu"),
            mark(android.view.KeyEvent.KEYCODE_DPAD_LEFT, "◀ Od początku"),
            mark(android.view.KeyEvent.KEYCODE_DPAD_RIGHT, "▶ Następny odcinek"),
            mark(android.view.KeyEvent.KEYCODE_DPAD_DOWN, "▼ Szczegóły"),
        ).joinToString("   ·   "),
        note = "Wstecz: wyjście   ·   OK: szczegóły",
        buttons = listOf(
            ForgeDialogButton("OK", primary = true) { ForgeRemoteLegend.pressed = -1 },
            ForgeDialogButton("Nie pokazuj więcej") { ForgeRemoteLegend.turnOff(context); ForgeRemoteLegend.pressed = -1 },
        ),
        onDismiss = { ForgeRemoteLegend.pressed = -1 },
    )
}
