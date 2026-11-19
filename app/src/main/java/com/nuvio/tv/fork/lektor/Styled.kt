package com.nuvio.tv.fork.lektor

import android.content.Context
import android.util.Log
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Fork 19.11 (strategia `plan/LEKTOR_GEMINI_STRATEGIA.md`): silnik, który nagrywa kwestię z poleceniem stylu
 * (opis sceny, kto mówi, emocja). Kontroler używa go zamiast scen: kwestia po kwestii, każda w chwili swojego napisu.
 */
interface StyledLektor {
    /** [style] — pełne polecenie stylu (lektor + scena + mówiący + emocja). */
    suspend fun synthesizeStyled(text: String, style: String): Pcm
}

/** Ustawienia lektora Gemini (styl, tryb scenariusza, limit wydatków) — wspólne dla silnika i kontrolera. */
object LektorPrefs {
    const val K_STYLE = "lektor_style"           // "classic" | "acting"
    const val K_SCRIPT = "lektor_script_mode"    // "chunks" | "whole"
    const val K_LIMIT = "lektor_spend_limit"     // USD na tytuł
    val STYLES = listOf("classic", "acting")
    val SCRIPT_MODES = listOf("chunks", "whole")
    val LIMITS = listOf("2", "1", "5", "0")      // 0 = bez limitu

    fun style(c: Context) = ForkFeatures.str(c, K_STYLE, "classic").takeIf { it in STYLES } ?: "classic"
    fun scriptMode(c: Context) = ForkFeatures.str(c, K_SCRIPT, "chunks").takeIf { it in SCRIPT_MODES } ?: "chunks"
    fun limitUsd(c: Context) = ForkFeatures.str(c, K_LIMIT, "2").toDoubleOrNull() ?: 2.0

    fun styleLabel(s: String) = if (s == "acting") "aktorski (wyraźne emocje)" else "klasyczny polski lektor"
    fun scriptLabel(s: String) = if (s == "whole") "cały film przed startem" else "porcjami po 10 min"

    /** Podstawa polecenia stylu dla wybranego stylu lektora. */
    fun baseStyle(c: Context): String = if (style(c) == "acting")
        "Polski lektor filmowy czytający z ekspresją aktora: wyraźne emocje zgodne ze sceną (złość, strach, czułość, ironia, szept, krzyk), " +
            "naturalne tempo, jeden głos dla wszystkich postaci."
    else
        "Klasyczny polski lektor filmowy (jak w polskiej telewizji): jeden spokojny, rzeczowy męski głos dla wszystkich postaci, " +
            "lekka intonacja zgodna z sensem i nastrojem sceny (pytanie, napięcie, żart), bez aktorskiego grania i bez przesady, " +
            "wyraźnie, w naturalnym tempie."
}

/**
 * Wydatki lektora na tytuł (USD), z tokenów zwróconych przez Google (`usageMetadata`). Po przekroczeniu limitu
 * [LektorPrefs.limitUsd] nowe nagrania są wstrzymywane (komunikat raz). Zerowane przy zmianie tytułu.
 */
object LektorSpend {
    @Volatile private var title = ""
    @Volatile var usd = 0.0; private set
    @Volatile private var warned = false

    fun begin(titleKey: String) { if (titleKey != title) { title = titleKey; usd = 0.0; warned = false } }

    fun add(what: String, cost: Double) {
        usd += cost
        if ((usd * 100).toInt() != ((usd - cost) * 100).toInt()) Log.i("ForkLektor", "koszt lektora w tym tytule: ${"%.2f".format(java.util.Locale.ROOT, usd)} USD (ostatnio: $what)")
    }

    /** true = wolno wydawać dalej. */
    fun allowed(c: Context): Boolean {
        val limit = LektorPrefs.limitUsd(c)
        if (limit <= 0 || usd < limit) return true
        if (!warned) { warned = true; Log.w("ForkLektor", "limit wydatków lektora $limit USD na tytuł osiągnięty"); LektorController.say(c, "Lektor: osiągnięto limit $limit USD na ten tytuł (Ustawienia → Lektor)") }
        return false
    }
}
