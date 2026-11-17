package com.nuvio.tv.fork.sources

/** Kolejka plików napisów w jednym języku dla przycisku „Napisy nie pasują” (B04-8). */
class SubtitleQueue(private val candidates: List<String>) {
    private var index = 0
    var uses = 0
        private set

    val current: String? get() = candidates.getOrNull(index)

    /** Następny plik w tym samym języku; po ostatnim wraca do pierwszego. */
    fun next(): String? {
        if (candidates.isEmpty()) return null
        uses++
        index = (index + 1) % candidates.size
        return current
    }

    /** Po trzech użyciach proponuje wyłączenie napisów albo lektora AI (gdy gotowy). */
    fun suggestion(lektorReady: Boolean): Suggestion? = if (uses >= 3) (if (lektorReady) Suggestion.LEKTOR else Suggestion.SUBTITLES_OFF) else null

    enum class Suggestion { SUBTITLES_OFF, LEKTOR }
}

/** Napisy dla seniorów (B04-9): rozmiar → ustawienia wyglądu napisów. */
enum class SubtitleSizeChoice(val title: String, val sizePercent: Int) {
    SMALL("Małe", 80), MEDIUM("Średnie", 120), LARGE("Duże", 170);

    companion object {
        /** Półprzezroczyste czarne tło (ARGB). */
        const val BACKGROUND_ARGB: Int = 0x99000000.toInt()
        fun fromSize(size: Int) = entries.minByOrNull { kotlin.math.abs(it.sizePercent - size) }!!
    }
}
