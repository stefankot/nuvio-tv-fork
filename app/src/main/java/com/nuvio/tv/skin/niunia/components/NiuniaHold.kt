package com.nuvio.tv.skin.niunia.components

/**
 * Przytrzymanie strzałki (UX JMGO 24.11, reguła F9): kosztowna praca po zmianie fokusu (wzbogacenie danych, hero, podgląd)
 * rusza dopiero po [SETTLE_MS] bez ruchu — przy przytrzymaniu (powtórzenia co ~50 ms) tylko raz, na końcu. Testy: NiuniaHoldTest.
 */
object NiuniaHold {
    const val SETTLE_MS = 250L

    /** Powtórzenie klawisza (przytrzymanie) — bez kosztownej pracy od razu. */
    fun isRepeat(repeatCount: Int): Boolean = repeatCount > 0

    /**
     * Które ruchy fokusu (chwile w ms, rosnąco) uruchomią pracę przy odroczeniu o [settle] z anulowaniem poprzedniej
     * (tak działa `itemFocusLater` na Głównej): ruch, po którym następny przyszedł wcześniej niż [settle], nic nie uruchamia.
     */
    fun fired(moves: List<Long>, settle: Long = SETTLE_MS): List<Int> =
        moves.indices.filter { i -> i == moves.lastIndex || moves[i + 1] - moves[i] >= settle }
}
