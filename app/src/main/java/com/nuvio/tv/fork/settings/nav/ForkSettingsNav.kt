package com.nuvio.tv.fork.settings.nav

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Nawigacja ustawień w 3 trybach (19.11): NOOB (`fork/settings/SimpleSettingsScreen`), Prosty (`ForgeBasicSettings` skórki)
 * i Zaawansowany ([ForkAdvancedSettings]: drzewo [SettingsTree]). Wyłącznik: klucz [KEY] (wyłączony = stary pełny ekran).
 */
object ForkSettingsNav {
    const val TAG = "ForkSettingsNav"
    const val KEY = "fork_settings_nav"
    const val SEARCH_KEY = "fork_settings_search"
    /** Umowny cel objazdu „otwórz pełne ustawienia z wyszukiwarką”. */
    const val SEARCH = "SEARCH"

    private var pendingSearch = false

    fun enabled(c: Context) = ForkFeatures.enabled(c, KEY, true)

    /** Hub prosi o wyszukiwarkę: pełne ustawienia otworzą ją od razu (czytane raz w `showSettingsSearch`). */
    fun requestSearch() { pendingSearch = true; Log.i(TAG, "wyszukiwarka: otwarta z menu") }

    /** Hook: początkowa wartość `showSettingsSearch` w pełnych ustawieniach. */
    fun takeSearch(): Boolean = pendingSearch.also { pendingSearch = false }

    /** Hook szyny: czy pokazać wiersz „Szukaj w ustawieniach” (od niego ▲ idzie do paska, nie od pierwszej kategorii). */
    fun railSearch(c: Context): Boolean = ForkFeatures.enabled(c, SEARCH_KEY, true).also { railSearchShown = it }

    /** Wiersz wyszukiwania jest pierwszy w szynie: pierwsza kategoria nie jest wtedy „pierwszym wierszem” dla ▲. */
    @Volatile var railSearchShown = false

    /** Gdzie był użytkownik w drzewie przed otwarciem pełnej kategorii (powrót Wstecz na ten sam wiersz). */
    var lastSection = 0
    var lastRow = 0
    var backFromCategory = false

    /**
     * Hook w `SettingsScreen` (przed ustawieniami podstawowymi). Tryb Zaawansowany: rysuje drzewo i zwraca true.
     * Gdy otwarty jest objazd do pełnej kategorii ([detour] != null) — Wstecz wraca do drzewa, zwraca false.
     */
    @Composable
    fun gate(isEssentialMode: Boolean, detour: String?, setDetour: (String?) -> Unit): Boolean {
        // 23.11: kategoria Nuvio otwarta z drzewa ustawień Niuni — ustawia kategorię i Wstecz wraca do drzewa.
        com.nuvio.tv.fork.settings.tree.SettingsTreeRuntime.detourGate(isEssentialMode, detour, setDetour)?.let { return it }
        val c = LocalContext.current
        val on = remember { enabled(c) }
        if (isEssentialMode || !on) return false
        if (detour != null) {
            BackHandler { Log.i(TAG, "Wstecz z kategorii $detour → drzewo"); backFromCategory = true; setDetour(null) }
            return false
        }
        // Ktoś prosi o konkretną kategorię (np. długie OK na Trybie nocnym): od razu objazd do niej.
        val wanted = com.nuvio.tv.skin.kicia.nav.ForgeNavState.settingsCategory
        if (wanted != null) { SideEffect { setDetour(wanted) }; return true }
        ForkAdvancedSettings(
            onOpenCategory = { cat -> Log.i(TAG, "otwieram kategorię $cat"); com.nuvio.tv.skin.kicia.nav.ForgeNavState.settingsCategory = cat; setDetour(cat) },
            onSearch = { requestSearch(); setDetour(SEARCH) },
        )
        return true
    }
}
