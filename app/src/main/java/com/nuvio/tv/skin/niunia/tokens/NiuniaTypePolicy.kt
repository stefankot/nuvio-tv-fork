package com.nuvio.tv.skin.niunia.tokens

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Typography

/**
 * Paczka „oczywiste” pkt 9 (21.11): typografia w całej aplikacji przy skórce Niunia — wyłącznie Satoshi (500 i 700),
 * waga ≥ 500, odstęp liter ≤ 0 (dodatni zostaje tylko w napisach wersalikami, w kodzie Niuni). Typografia Material
 * (ekrany Nuvio, których makieta nie rysuje) idzie przez [typography] — wpięcie w `ui/theme/Type.kt`.
 */
object NiuniaTypePolicy {
    val active: Boolean get() = com.nuvio.tv.skin.api.SkinPreferences.skin.value == com.nuvio.tv.skin.api.SkinId.NIUNIA

    /** Styl zgodny z zasadą: Satoshi, 500/700, odstęp liter ≤ 0. */
    fun enforce(s: TextStyle): TextStyle {
        val w = s.fontWeight
        val weight = when {
            w == null -> FontWeight.Medium
            w >= FontWeight.SemiBold -> FontWeight.Bold
            else -> FontWeight.Medium
        }
        val ls = s.letterSpacing
        val spacing = if (ls != TextUnit.Unspecified && ls.value > 0f) 0.sp else ls
        return s.copy(fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontWeight = weight, letterSpacing = spacing)
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    fun typography(t: Typography): Typography {
        if (!active) return t
        return t.copy(
            displayLarge = enforce(t.displayLarge), displayMedium = enforce(t.displayMedium), displaySmall = enforce(t.displaySmall),
            headlineLarge = enforce(t.headlineLarge), headlineMedium = enforce(t.headlineMedium), headlineSmall = enforce(t.headlineSmall),
            titleLarge = enforce(t.titleLarge), titleMedium = enforce(t.titleMedium), titleSmall = enforce(t.titleSmall),
            bodyLarge = enforce(t.bodyLarge), bodyMedium = enforce(t.bodyMedium), bodySmall = enforce(t.bodySmall),
            labelLarge = enforce(t.labelLarge), labelMedium = enforce(t.labelMedium), labelSmall = enforce(t.labelSmall),
        ).also { if (!logged) { logged = true; android.util.Log.i("ForkNiunia", "typografia: Satoshi 500/700, odstęp liter ≤ 0 (ekrany Nuvio)") } }
    }
    private var logged = false
}
