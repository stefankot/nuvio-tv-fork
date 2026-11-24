package com.nuvio.tv.skin.niunia.components

/**
 * Pamięć fokusu po kluczu (UX JMGO 24.11, reguły F3/F5/F6; konwencja w `plan/DECYZJE.md`): powrót do rzędu, listy albo ekranu
 * przywraca element o zapamiętanym kluczu (id), nie o indeksie — przetrwa dociągnięcie danych i zmianę kolejności
 * (np. „Oglądaj dalej” po obejrzeniu). Usunięty element → najbliższy sąsiad na jego dawnym miejscu. Testy: NiuniaFocusKeysTest.
 */
object NiuniaFocusKeys {
    /** Indeks do przywrócenia w [keys]: [savedKey], gdy jest; inaczej [savedIndex] przycięty do listy; -1 = lista pusta. */
    fun restoreIndex(keys: List<String>, savedKey: String?, savedIndex: Int): Int {
        if (keys.isEmpty()) return -1
        if (savedKey != null) {
            val i = keys.indexOf(savedKey)
            if (i >= 0) return i
        }
        return savedIndex.coerceIn(0, keys.size - 1)
    }

    /**
     * Szukaj (F5): który zapisany element dostaje fokus po powrocie — tylko gdy zapis dotyczy tego samego zapytania
     * ([savedQuery] = [currentQuery]); inne zapytanie albo brak zapisu → fokus domyślny ekranu (klawisz „a”). Klucz może być
     * identyfikatorem wyniku albo znacznikiem elementu, który otworzył ekran (np. [OPENER_DISCOVER]).
     */
    fun searchReturn(savedId: String?, savedQuery: String, currentQuery: String): String? =
        savedId?.takeIf { savedQuery == currentQuery }

    /** Znacznik chipu „Odkrywaj” w Szukaj jako elementu, który otworzył ekran Odkrywaj (powrót Wstecz → ten chip). */
    const val OPENER_DISCOVER = "@odkrywaj"

    /**
     * Klucz elementu do przywrócenia na ekranie z rzędami ([rows] = prefiks rzędu → klucze jego elementów, każdy klucz zaczyna się
     * od prefiksu). Rząd szukany po prefiksie [savedKey]; brak rzędu (zniknął cały) → null (fokus domyślny ekranu).
     */
    fun resolve(rows: List<Pair<String, List<String>>>, savedKey: String?, savedIndex: Int): String? {
        if (savedKey == null) return null
        val row = rows.firstOrNull { (prefix, _) -> savedKey.startsWith(prefix) } ?: return null
        val i = restoreIndex(row.second, savedKey, savedIndex)
        return if (i >= 0) row.second[i] else null
    }
}
