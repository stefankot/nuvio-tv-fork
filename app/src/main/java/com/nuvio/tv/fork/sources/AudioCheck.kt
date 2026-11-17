package com.nuvio.tv.fork.sources

/** Sprawdzenie ścieżek po starcie (B04-4): profil chce polskiego dźwięku, ale strumień go nie ma. */
object AudioCheck {
    enum class Decision { KEEP, TRY_NEXT, FALLBACK_WITH_SUBS }

    private val polish = setOf("pl", "pol", "polish", "polski")

    fun hasPolish(trackLanguages: List<String?>): Boolean =
        trackLanguages.any { l -> l?.trim()?.lowercase()?.substringBefore('-')?.let { it in polish } == true }

    /**
     * [wantsPolish] — profil wybrał „po polsku”; [attemptsUsed]/[maxAttempts] — limit prób istniejącego mechanizmu odzyskiwania.
     * Brak polskiej ścieżki i są próby = następny kandydat; po wyczerpaniu = najlepsze dostępne z polskimi napisami.
     */
    fun decide(wantsPolish: Boolean, trackLanguages: List<String?>, attemptsUsed: Int, maxAttempts: Int): Decision = when {
        !wantsPolish || trackLanguages.isEmpty() || hasPolish(trackLanguages) -> Decision.KEEP
        attemptsUsed < maxAttempts -> Decision.TRY_NEXT
        else -> Decision.FALLBACK_WITH_SUBS
    }
}
