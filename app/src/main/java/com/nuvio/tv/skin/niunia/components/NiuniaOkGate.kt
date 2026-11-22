package com.nuvio.tv.skin.niunia.components

/**
 * Czysta logika OK / długiego OK jednego elementu Niuni ([niuniaOk]); testy: NiuniaOkGateTest.
 * UX 22.11 (właściciel: „w większości elementów nie działa długie naciśnięcie”):
 * - długie OK = własny zegar ([timer]) albo systemowa flaga długiego naciśnięcia (pilot po ~0,5 s, adb `--longpress`);
 * - puszczenie bez wciśnięcia na tym elemencie (np. po długim OK, które otworzyło okno) NIE jest kliknięciem;
 * - powtórzenia OK bez własnego wciśnięcia (OK trzymane od poprzedniego ekranu) są pomijane;
 * - utrata fokusu w trakcie wciśnięcia zeruje stan (wcześniej następne krótkie OK ginęło).
 */
internal class NiuniaOkGate {
    enum class Down { IGNORE, PRESS, HELD, LONG }
    enum class Up { IGNORE, CLICK, AFTER_LONG }

    /** Chwila wciśnięcia (0 = nie wciśnięty) — znacznik dla [timer]. */
    var downAt = 0L
        private set
    private var longFired = false

    fun down(now: Long, repeatCount: Int, systemLong: Boolean, hasLong: Boolean): Down {
        if (downAt == 0L) {
            if (repeatCount > 0) return Down.IGNORE
            downAt = now.coerceAtLeast(1L); longFired = false
            return if (hasLong && systemLong) { longFired = true; Down.LONG } else Down.PRESS
        }
        if (hasLong && systemLong && !longFired) { longFired = true; return Down.LONG }
        return Down.HELD
    }

    /** Zegar długiego OK minął: true = wywołaj długie OK (tylko gdy to wciąż to samo wciśnięcie). */
    fun timer(mark: Long): Boolean {
        if (downAt == 0L || downAt != mark || longFired) return false
        longFired = true
        return true
    }

    fun up(): Up {
        if (downAt == 0L) return Up.IGNORE
        val wasLong = longFired
        reset()
        return if (wasLong) Up.AFTER_LONG else Up.CLICK
    }

    fun reset() { downAt = 0L; longFired = false }
}
