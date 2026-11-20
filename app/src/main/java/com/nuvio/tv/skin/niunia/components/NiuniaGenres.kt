package com.nuvio.tv.skin.niunia.components

/**
 * Test JMGO build152: gatunki z katalogów (Cinemeta, TMDB) przychodzą po angielsku („Action”, „Drama”, „Sci-Fi & Fantasy”).
 * Teksty dla użytkownika po polsku → nazwa do wyświetlenia; do filtrów idzie dalej oryginał. Nieznany gatunek bez zmian.
 */
object NiuniaGenres {
    private val PL = mapOf(
        "action" to "Akcja", "adventure" to "Przygodowy", "action & adventure" to "Akcja i przygoda", "animation" to "Animacja",
        "anime" to "Anime", "biography" to "Biograficzny", "comedy" to "Komedia", "crime" to "Kryminał", "documentary" to "Dokumentalny",
        "drama" to "Dramat", "family" to "Familijny", "fantasy" to "Fantasy", "film-noir" to "Film noir", "game-show" to "Teleturniej",
        "history" to "Historyczny", "horror" to "Horror", "kids" to "Dla dzieci", "music" to "Muzyczny", "musical" to "Musical",
        "mystery" to "Tajemnica", "news" to "Informacje", "reality" to "Reality show", "reality-tv" to "Reality show",
        "romance" to "Romans", "sci-fi" to "Sci-fi", "science fiction" to "Sci-fi", "sci-fi & fantasy" to "Sci-fi i fantasy",
        "short" to "Krótki metraż", "soap" to "Telenowela", "sport" to "Sport", "talk" to "Talk-show", "talk-show" to "Talk-show",
        "thriller" to "Thriller", "tv movie" to "Film telewizyjny", "war" to "Wojenny", "war & politics" to "Wojna i polityka",
        "western" to "Western",
    )

    fun pl(genre: String?): String? {
        val g = genre?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return PL[g.lowercase().replace("&amp;", "&")] ?: g
    }
}
