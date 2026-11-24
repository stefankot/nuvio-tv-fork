package com.nuvio.tv.skin.niunia.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay

private const val TAG = "ForkNiunia"

/**
 * Pamięć fokusu ekranu po kluczu (UX JMGO 24.11, reguły F5/F6; konwencja w `plan/DECYZJE.md`): jeden zapis na ekran, tylko w pamięci
 * procesu. Ekran zapisuje ostatnio zaznaczony element razem z przewinięciem (listy zewnętrznej i rzędu); po powrocie (Wstecz ze strony
 * tytułu) przewija się tak samo i oddaje fokus temu samemu elementowi — po kluczu, a gdy zniknął, jego sąsiadowi ([NiuniaFocusKeys]).
 * Czysta logika bez Compose; testy: NiuniaFocusMemoryTest.
 */
internal object NiuniaFocusMemory {
    /** [key] elementu, [index] w swoim rzędzie/liście, przewinięcie listy zewnętrznej ([outer]) i rzędu ([inner]). */
    data class Entry(
        val key: String, val index: Int,
        val outer: Int = 0, val outerOffset: Int = 0, val inner: Int = 0, val innerOffset: Int = 0,
    )

    private val entries = HashMap<String, Entry>()

    @Synchronized fun save(screen: String, entry: Entry) { entries[screen] = entry }
    @Synchronized fun entry(screen: String): Entry? = entries[screen]
    @Synchronized fun clear(screen: String) { entries.remove(screen) }

    /** Klucz do przywrócenia w [rows] (prefiks rzędu → klucze; patrz [NiuniaFocusKeys.resolve]); null = nie ma czego przywracać. */
    fun target(entry: Entry?, rows: List<Pair<String, List<String>>>): String? =
        if (entry == null) null else NiuniaFocusKeys.resolve(rows, entry.key, entry.index)

    /** Przewinięcie rzędu o prefiksie [prefix] do odtworzenia: tylko gdy zapisany element jest z tego rzędu ([target] ≠ null). */
    fun innerStart(entry: Entry?, target: String?, prefix: String): Pair<Int, Int> =
        if (entry != null && target != null && entry.key.startsWith(prefix)) entry.inner to entry.innerOffset else 0 to 0

    /** Przewinięcie listy zewnętrznej do odtworzenia (0, 0, gdy nie ma czego przywracać). */
    fun outerStart(entry: Entry?, target: String?): Pair<Int, Int> =
        if (entry != null && target != null) entry.outer to entry.outerOffset else 0 to 0
}

/**
 * Powrót fokusu na ekran z listą: [entry] i [target] ustalane raz, przy pierwszej kompozycji (dane, które przyszły później, nie
 * kradną fokusu — F6). Element o kluczu [target] dostaje [requester] przez [modifier]; fokus idzie tam po chwili (patrz [rememberNiuniaFocusRestore]).
 */
internal class NiuniaFocusRestore(private val screen: String, rows: List<Pair<String, List<String>>>) {
    val entry: NiuniaFocusMemory.Entry? = NiuniaFocusMemory.entry(screen)
    val target: String? = NiuniaFocusMemory.target(entry, rows)
    val requester = FocusRequester()
    val active: Boolean get() = target != null

    fun modifier(key: String): Modifier = if (key == target) Modifier.focusRequester(requester) else Modifier
    fun outerStart(): Pair<Int, Int> = NiuniaFocusMemory.outerStart(entry, target)
    fun innerStart(prefix: String): Pair<Int, Int> = NiuniaFocusMemory.innerStart(entry, target, prefix)

    fun save(key: String, index: Int, outer: Int = 0, outerOffset: Int = 0, inner: Int = 0, innerOffset: Int = 0) {
        NiuniaFocusMemory.save(screen, NiuniaFocusMemory.Entry(key, index, outer, outerOffset, inner, innerOffset))
    }
}

/**
 * Tworzy [NiuniaFocusRestore] dla ekranu [screen] i po 120 ms (po ułożeniu listy) oddaje fokus zapamiętanemu elementowi.
 * [rows] liczone tylko raz, przy pierwszej kompozycji.
 */
@Composable
internal fun rememberNiuniaFocusRestore(screen: String, rows: () -> List<Pair<String, List<String>>>): NiuniaFocusRestore {
    val r = remember { NiuniaFocusRestore(screen, rows()) }
    LaunchedEffect(Unit) {
        val t = r.target ?: return@LaunchedEffect
        delay(120)
        try {
            r.requester.requestFocus()
            Log.i(TAG, "$screen: powrót — fokus na $t")
        } catch (e: IllegalStateException) {
            Log.w(TAG, "$screen: powrót na $t: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
    return r
}
