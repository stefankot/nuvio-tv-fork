package com.nuvio.tv.fork.settings.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTreeTest {
    private val allCategories = setOf(
        "ACCOUNT", "PROFILES", "WATCH_PARTY", "APPEARANCE", "NIGHT_MODE", "LAYOUT", "CONTENT_DISCOVERY", "PLAYBACK",
        "NUVIO_RESHAPED", "YOUTUBE", "FORGE", "INTEGRATION", "TRACKING", "ADVANCED", "ABOUT", "DEBUG",
    )

    @Test fun `4 do 8 kategorii i 4 do 8 podkategorii w kazdej`() {
        val s = SettingsTree.sections
        assertTrue(s.size in SettingsTree.MIN..SettingsTree.MAX)
        s.forEach { assertTrue("${it.id}: ${it.leaves.size}", it.leaves.size in SettingsTree.MIN..SettingsTree.MAX) }
    }

    @Test fun `kazda grupa Reshaped jest gdzies w drzewie`() {
        val used = SettingsTree.sections.flatMap { it.leaves }.mapNotNull { it.target as? NavTarget.Parts }.flatMap { it.items }.toSet()
        assertEquals(ItemsGroup.entries.toSet(), used)
    }

    @Test fun `unikalne id podkategorii w sekcji`() {
        SettingsTree.sections.forEach { s -> assertEquals(s.leaves.size, s.leaves.map { it.id }.toSet().size) }
        assertEquals(SettingsTree.sections.size, SettingsTree.sections.map { it.id }.toSet().size)
    }

    @Test fun `ukryte kategorie znikaja z drzewa`() {
        val v = SettingsTree.visible(allCategories - "DEBUG")
        assertTrue(v.flatMap { it.leaves }.none { (it.target as? NavTarget.Category)?.name == "DEBUG" })
        assertEquals(SettingsTree.sections.size, v.size)
    }
}
