package com.nuvio.tv.fork.settings

/** Czysta logika "pierwszego uruchomienia": co ustawić, nie nadpisując zapisanych wartości. */
object FirstRunPlan {
    data class Stored(val tmdbEnabled: Boolean?, val tmdbLanguage: String?, val appLocale: String?, val level: String?)
    data class Result(val tmdbEnabled: Boolean?, val tmdbLanguage: String?, val appLocale: String?, val level: String?)

    const val LEVEL_SIMPLE = "SIMPLE"

    /** null w polu wyniku = nie zmieniaj. */
    fun decide(s: Stored) = Result(
        tmdbEnabled = if (s.tmdbEnabled == null) true else null,
        tmdbLanguage = if (s.tmdbLanguage == null) "pl" else null,
        appLocale = if (s.appLocale == null) "pl" else null,
        level = if (s.level == null) LEVEL_SIMPLE else null,
    )
}
