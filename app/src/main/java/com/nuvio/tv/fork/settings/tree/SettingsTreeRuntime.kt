package com.nuvio.tv.fork.settings.tree

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import com.nuvio.tv.fork.registry.ForkRegistry
import com.nuvio.tv.fork.registry.SettingsLevel
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/** Drzewo ustawień w aplikacji: pozycje modułów z rejestru, otwieranie celów, objazd do kategorii Nuvio. */
object SettingsTreeRuntime {
    const val TAG = "ForkSettingsTree"

    fun enabled(c: Context) = ForkFeatures.enabled(c, SettingsTreeSpec.KEY, true)

    private fun level(l: SettingsLevel) = when (l) {
        SettingsLevel.SIMPLE -> TreeLevel.NOOB
        SettingsLevel.ESSENTIAL -> TreeLevel.MAIN
        SettingsLevel.ADVANCED -> TreeLevel.MORE
    }

    fun plainTitle(t: String) = t.removePrefix("✓ ").trim()

    /** Pozycje włączonych modułów z rejestru (wyłączony moduł nic nie zgłasza). */
    fun entries(c: Context): List<ModuleEntry> {
        val reg = ForkRegistry.forContext(c)
        val out = ArrayList<ModuleEntry>()
        reg.settings().forEach { (m, s) ->
            out += ModuleEntry(m, s.key, s.title, s.description, level(s.level), TreeTarget.Toggle(s.key, s.defaultValue), s.place)
        }
        reg.actions().forEach { (m, a) ->
            val t = plainTitle(a.title)
            out += ModuleEntry(m, "a:$t", t, a.description, level(a.level), TreeTarget.Action(m, t), a.place)
        }
        return out
    }

    fun enabledModules(c: Context): Set<String> {
        val reg = ForkRegistry.forContext(c)
        return reg.allModules().filter { ForkFeatures.enabled(c, ForkRegistry.switchKey(it.id), it.defaultEnabled) }.map { it.id }.toSet()
    }

    fun tree(c: Context, visibleCategories: Set<String>): List<TreeCategory> {
        val t0 = System.currentTimeMillis()
        val e = entries(c)
        val t = SettingsTreeSpec.build(e, visibleCategories, enabledModules(c))
        val fallback = t.flatMap { it.groups }.firstOrNull { it.id == SettingsTreeSpec.FALLBACK_GROUP }
        Log.i(TAG, "drzewo: ${t.size} kategorii, ${t.sumOf { k -> k.groups.sumOf { it.items.size } }} pozycji (modułów ${e.size})" +
            (fallback?.let { ", bez miejsca: " + it.items.joinToString { i -> i.title } } ?: "") + " w ${System.currentTimeMillis() - t0} ms")
        return t
    }

    /** Uruchamia czynność modułu po tytule (bez „✓ ”). false = brak (moduł wyłączony albo tytuł zmieniony). */
    fun runAction(c: Context, module: String, title: String): Boolean {
        val a = ForkRegistry.forContext(c).actions().firstOrNull { it.first == module && plainTitle(it.second.title) == title }?.second
        if (a == null) { Log.w(TAG, "brak czynności $module: $title"); return false }
        com.nuvio.tv.fork.family.FamilyUsage.feature("akcja:" + a.title)
        a.run(c)
        return true
    }

    fun open(c: Context, route: String) {
        val nav = ForkFeatures.open
        Log.i(TAG, "otwieram ekran $route")
        if (nav == null) { Log.w(TAG, "brak nawigacji dla $route"); com.nuvio.tv.fork.ForkLog.say(c, TAG, "Nie udało się otworzyć"); return }
        try { nav(route) } catch (e: IllegalArgumentException) {
            Log.w(TAG, "trasa $route: ${e.javaClass.simpleName}: ${e.message}"); com.nuvio.tv.fork.ForkLog.say(c, TAG, "Nie udało się otworzyć")
        }
    }

    // --- Objazd do kategorii pełnych ustawień Nuvio (Base): SimpleSettingsGate oddaje ekran SettingsScreen, Wstecz wraca. ---

    /** Kategoria Nuvio otwarta z drzewa (null = drzewo na ekranie). Czytane w `SimpleSettingsGate` (stan Compose). */
    var detour by mutableStateOf<String?>(null)
        private set

    /** Gdzie był fokus w drzewie (powrót z kategorii, ekranu, okna). */
    var lastCategory = 0
    var lastItem: String? = null
    var lastSearch: String? = null

    fun openBase(category: String) { Log.i(TAG, "objazd: kategoria Nuvio $category"); detour = category }

    /**
     * Hook w `ForkSettingsNav.gate` (SettingsScreen): przy objeździe z drzewa ustawia kategorię i odpowiada za Wstecz.
     * true = SettingsScreen nic nie rysuje w tej klatce; null = brak objazdu (zwykła logika).
     */
    @Composable
    fun detourGate(isEssentialMode: Boolean, current: String?, setCurrent: (String?) -> Unit): Boolean? {
        val target = detour ?: return null
        var opened by remember { mutableStateOf(false) }
        if (current == null) {
            if (!opened) SideEffect {
                opened = true; setCurrent(target); com.nuvio.tv.skin.kicia.nav.ForgeNavState.settingsCategory = target
            } else SideEffect { Log.i(TAG, "Wstecz z kategorii $target → drzewo"); detour = null }
            return true
        }
        // Tryb podstawowy Nuvio ma własne Wstecz (zeruje kategorię); w pełnym — nasze (ekrany wewnątrz kategorii wygrywają, bo są złożone później).
        if (!isEssentialMode) BackHandler { setCurrent(null) }
        return false
    }
}

/** Skok do sekcji ekranu Odtwarzanie (hook w `PlaybackSettingsSections`): rozwinięta sekcja z fokusem. */
object SettingsTreeJump {
    @Volatile private var pending: String? = null
    private var active: String? = null
    private var requester: FocusRequester? = null

    fun set(section: String) { pending = section; active = section; requester = FocusRequester() }

    /** Początkowo rozwinięte sekcje (czytane raz, przy pierwszym złożeniu). */
    fun expanded(): List<String> = pending?.let { listOf(it) } ?: emptyList()

    fun focus(section: String): FocusRequester? = if (section == active) requester else null

    @Composable
    fun Effect(listState: LazyListState, sections: List<String>) {
        DisposableEffect(Unit) { onDispose { active = null; requester = null } }
        LaunchedEffect(Unit) {
            val s = pending ?: return@LaunchedEffect
            pending = null
            val i = sections.indexOf(s)
            if (i < 0) { Log.w(SettingsTreeRuntime.TAG, "sekcja $s niewidoczna"); return@LaunchedEffect }
            try { listState.scrollToItem(i) } catch (e: IllegalStateException) { Log.w(SettingsTreeRuntime.TAG, "przewinięcie do $s: ${e.javaClass.simpleName}: ${e.message}") }
            var ok = false
            repeat(30) {
                if (!ok) {
                    withFrameNanos { }
                    ok = try { requester?.requestFocus(); true } catch (e: IllegalStateException) { false }
                }
            }
            Log.i(SettingsTreeRuntime.TAG, "sekcja odtwarzania $s: rozwinięta, fokus ${if (ok) "na niej" else "BRAK"}")
        }
    }
}
