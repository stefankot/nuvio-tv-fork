package com.nuvio.tv.fork.settings.tree

import com.nuvio.tv.fork.registry.ForkModules
import com.nuvio.tv.fork.registry.SettingsLevel
import com.nuvio.tv.fork.settings.nav.ItemsGroup
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.screens.settings.PlaybackSection
import com.nuvio.tv.ui.screens.settings.SettingsCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Drzewo ustawień (23.11): kształt wg `plan/ustawienia/DRZEWO.md` i kompletność względem rejestru modułów. */
class SettingsTreeSpecTest {
    companion object {
        private fun lvl(l: SettingsLevel) = when (l) { SettingsLevel.SIMPLE -> TreeLevel.NOOB; SettingsLevel.ESSENTIAL -> TreeLevel.MAIN; SettingsLevel.ADVANCED -> TreeLevel.MORE }

        /** Pozycje WSZYSTKICH modułów (jak `SettingsTreeRuntime.entries`, bez filtra włączonych). */
        val entries: List<ModuleEntry> by lazy {
            ForkModules.all.flatMap { m ->
                m.settings().map { s -> ModuleEntry(m.id, s.key, s.title, s.description, lvl(s.level), TreeTarget.Toggle(s.key, s.defaultValue), s.place) } +
                    m.actions().map { a ->
                        val t = a.title.removePrefix("✓ ").trim()
                        ModuleEntry(m.id, "a:$t", t, a.description, lvl(a.level), TreeTarget.Action(m.id, t), a.place)
                    }
            }
        }
        val full: List<TreeCategory> by lazy { SettingsTreeSpec.build(entries) }
        val items: List<TreeItem> by lazy { full.flatMap { c -> c.groups.flatMap { it.items } } }
    }

    @Test fun `siedem kategorii nazwanych celem, każda z grupami`() {
        assertEquals(listOf("Oglądanie", "Napisy i dźwięk", "Telewizja", "Wygląd", "Konta i dodatki", "Pilot i głos", "System"), full.map { it.title })
        assertTrue(full.size <= SettingsTreeSpec.MAX_CATEGORIES)
        full.forEach { c -> assertTrue("${c.title}: grupy", c.groups.size in 1..5) }
    }

    @Test fun `każdy slot modułu wskazuje istniejącą pozycję`() {
        val refs = entries.map { it.ref }.toSet()
        val missing = SettingsTreeSpec.moduleRefs.filter { r -> if (r.endsWith("*")) refs.none { it.startsWith(r.dropLast(1)) } else r !in refs } +
            SettingsTreeSpec.COVERED.filter { it !in refs }
        assertTrue("sloty bez pozycji w modułach: $missing", missing.isEmpty())
    }

    @Test fun `każda pozycja modułu ma miejsce w drzewie (grupa Inne pusta)`() {
        val inne = full.flatMap { it.groups }.firstOrNull { it.id == SettingsTreeSpec.FALLBACK_GROUP }
        assertTrue("bez miejsca: ${inne?.items?.map { it.title }}", inne == null)
    }

    @Test fun `bez duplikatów id, nazw i celów`() {
        assertEquals(items.size, items.map { it.id }.toSet().size)
        val titles = items.groupBy { it.title.lowercase() }.filter { it.value.size > 1 }.keys
        assertTrue("powtórzone nazwy: $titles", titles.isEmpty())
        val targets = items.map { it.target }.filter { it !is TreeTarget.Special && it !is TreeTarget.Action }
            .groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("powtórzone cele: $targets", targets.isEmpty())
    }

    @Test fun `tryb prosty to podzbiór pełnego, najwyżej 20 pozycji`() {
        val simple = SettingsTreeSpec.forMode(full, TreeMode.SIMPLE).flatMap { c -> c.groups.flatMap { g -> SettingsTreeSpec.itemsFor(g, TreeMode.SIMPLE, false) } }
        assertTrue("prosty: ${simple.size}", simple.size in 10..20)
        assertTrue(simple.all { it.level == TreeLevel.NOOB })
        assertTrue(items.map { it.id }.containsAll(simple.map { it.id }))
        assertTrue("prosty ma pozycję Tryb ustawień", simple.any { it.target == TreeTarget.Special("level") })
    }

    @Test fun `grupa bez rozwinięcia Więcej ma najwyżej 9 pozycji, Więcej tylko w pełnym`() {
        full.forEach { c -> c.groups.forEach { g ->
            val shown = SettingsTreeSpec.itemsFor(g, TreeMode.FULL, expanded = false)
            assertTrue("${c.title} › ${g.title}: ${shown.size}", shown.size <= 9)
            assertEquals(0, SettingsTreeSpec.moreCount(g, TreeMode.SIMPLE))
            assertEquals(g.items.size, SettingsTreeSpec.itemsFor(g, TreeMode.FULL, expanded = true).size)
        } }
    }

    @Test fun `cele istnieją — sekcje Odtwarzania, kategorie Nuvio, trasy, grupy Reshaped, pozycje specjalne`() {
        val routes = setOf(Screen.ThemeSettings.route, Screen.LayoutSettings.route, Screen.ManageProfiles.route, Screen.Account.route,
            Screen.AddonManager.route, Screen.Tracking.route, Screen.About.route, com.nuvio.tv.fork.settings.TOOLS_ROUTE, com.nuvio.tv.fork.settings.MODULES_ROUTE)
        items.forEach { i ->
            when (val t = i.target) {
                is TreeTarget.Playback -> assertTrue(t.section, PlaybackSection.entries.any { it.name == t.section })
                is TreeTarget.Base -> assertTrue(t.category, SettingsCategory.entries.any { it.name == t.category })
                is TreeTarget.Route -> assertTrue(t.route, t.route in routes)
                is TreeTarget.Special -> assertTrue(t.id, t.id in SettingsTreeSpec.SPECIALS)
                else -> Unit
            }
            i.words.forEach { w -> assertTrue("${i.id}: słowa $w", SettingsTreeWords.byFile.containsKey(w)) }
            i.needsCategory?.let { n -> assertTrue(n, SettingsCategory.entries.any { it.name == n }) }
        }
        val used = items.mapNotNull { it.target as? TreeTarget.Parts }.flatMap { it.groups }.toSet()
        // FORK_SWITCHES zastąpione pojedynczymi przełącznikami (kalendarz, pilot Arc, dawne ustawienia).
        assertEquals(ItemsGroup.entries.toSet() - ItemsGroup.FORK_SWITCHES, used)
        assertEquals(SettingsTreeSpec.SPECIALS, items.mapNotNull { (it.target as? TreeTarget.Special)?.id }.toSet())
    }

    @Test fun `wyłączony moduł i niewidoczna kategoria Nuvio znikają z drzewa`() {
        val noLektor = SettingsTreeSpec.build(entries.filter { it.module != "lektor" }, setOf("ACCOUNT", "PROFILES"), ForkModules.all.map { it.id }.toSet() - "lektor")
        val ids = noLektor.flatMap { c -> c.groups.flatMap { g -> g.items.map { it.id } } }
        assertTrue(ids.none { it.startsWith("m:lektor:") })
        assertTrue("wyłącznik Lektora zostaje", "lektor" in ids)
        assertTrue("Diagnostyka tylko w wersji testowej", "debug" !in ids)
        assertTrue("wspólne oglądanie tylko, gdy dostępne", "party" !in ids)
        val guest = SettingsTreeSpec.build(entries, setOf("APPEARANCE"), null).flatMap { c -> c.groups.flatMap { g -> g.items.map { it.id } } }
        assertTrue("profil dodatkowy: bez Konta i Profili", "account" !in guest && "profiles" !in guest)
    }

    @Test fun `miejsce podane przez moduł (place) wygrywa z grupą Inne`() {
        val e = ModuleEntry("x", "nowy_klucz", "Nowe ustawienie", "", TreeLevel.MAIN, TreeTarget.Toggle("nowy_klucz", true), place = "tv/kanaly")
        val t = SettingsTreeSpec.build(entries + e)
        assertTrue(t.first { it.id == "tv" }.groups.first { it.id == "kanaly" }.items.any { it.title == "Nowe ustawienie" })
        val bez = SettingsTreeSpec.build(entries + e.copy(place = null))
        val inne = bez.first { it.id == "system" }.groups.first { it.id == SettingsTreeSpec.FALLBACK_GROUP }.items
        assertEquals(listOf(TreeLevel.MORE), inne.filter { it.title == "Nowe ustawienie" }.map { it.level })
    }
}
