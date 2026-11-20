package com.nuvio.tv.fork.echo

import android.content.Context
import android.util.Log
import com.nuvio.tv.core.streams.AutoQualityPolicy

/**
 * Właściciel 20.11: „źródło przełącza się na gorsze, bez napisów, mimo że wszystko było w porządku”.
 * Log JMGO: AUTO_HOTSWAP + AUTO_QUALITY down 4K → 1080p przy 14 s bufora, bez żadnego zacięcia — to „wczesne
 * schodzenie” (AutoQualityPolicy.SENSITIVE, przy szybkiej podmianie): łącze chwilę wolniejsze niż plik REMUX.
 * Nowe źródło = inny plik, napisy dobrane do starego przepadają. Dlatego: w dół TYLKO po prawdziwym zacięciu
 * (reszta szybkiej podmiany zostaje: szybsze przejście w górę). Wyłącznik: ForkFeatures `fork_aq_only_on_stall`.
 */
object EchoAutoQualityGuard {
    private const val TAG = "ForkEcho"
    @Volatile private var lastLogged: String? = null

    fun config(context: Context, hotSwap: Boolean): AutoQualityPolicy.Config {
        val guard = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(context, "fork_aq_only_on_stall", default = true)
        val cfg = when {
            !hotSwap -> AutoQualityPolicy.DEFAULT
            guard -> AutoQualityPolicy.SENSITIVE.copy(earlyDowngrade = false)
            else -> AutoQualityPolicy.SENSITIVE
        }
        val desc = "szybka podmiana=$hotSwap, w dół tylko po zacięciu=${guard || !hotSwap}"
        if (desc != lastLogged) { lastLogged = desc; Log.i(TAG, "auto jakość: $desc") }
        return cfg
    }
}
