package com.nuvio.tv.fork.settings

import com.nuvio.tv.fork.night.NightEffects
import com.nuvio.tv.fork.night.Strength
import com.nuvio.tv.fork.registry.ModuleSetting
import com.nuvio.tv.fork.registry.SettingsLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleSettingsTest {
    @Test fun `nie wiecej niz 20 pozycji i 6 grup`() {
        assertTrue(SimpleScreenSpec.all.size <= SimpleScreenSpec.MAX_ITEMS)
        assertEquals(6, SimpleScreenSpec.groups(SimpleScreenSpec.visible(setOf("sources", "night", "tv", "launcher"))).size)
        assertTrue(SimpleScreenSpec.visible(emptySet()).none { it.module != null })
    }

    @Test fun `trzy przejscia poziomu`() {
        assertEquals(LevelSwitch.Change(true, null), LevelSwitch.to(UiLevel.SIMPLE))
        assertEquals(LevelSwitch.Change(false, "ESSENTIAL"), LevelSwitch.to(UiLevel.ESSENTIAL))
        assertEquals(LevelSwitch.Change(false, "ADVANCED"), LevelSwitch.to(UiLevel.ADVANCED))
    }

    @Test fun `filtrowanie pozycji rejestru wedlug poziomu`() {
        val items = listOf(SettingsLevel.SIMPLE, SettingsLevel.ESSENTIAL, SettingsLevel.ADVANCED).map { ModuleSetting("k$it", it, "g", "t") }
        assertEquals(listOf("kSIMPLE"), ModuleSettingsFilter.forLevel(items, UiLevel.SIMPLE).map { it.key })
        assertEquals(listOf("kSIMPLE", "kESSENTIAL"), ModuleSettingsFilter.forLevel(items, UiLevel.ESSENTIAL).map { it.key })
        assertEquals(3, ModuleSettingsFilter.forLevel(items, UiLevel.ADVANCED).size)
    }

    @Test fun `wspolna sila na dwa ustawienia i nakladka`() {
        val s = NightEffects.fromCommon(Strength.MEDIUM)
        assertEquals(Strength.MEDIUM, s.audio); assertEquals(Strength.MEDIUM, s.picture)
        assertEquals(0.14f, NightEffects.overlay(Strength.MEDIUM, s, hdr = false).amber, 0.001f)
        assertEquals(0f, NightEffects.overlay(Strength.MEDIUM, s, hdr = true).amber, 0.001f)
        assertEquals(0f, NightEffects.overlay(Strength.OFF, s, hdr = false).amber, 0.001f)
        assertEquals(0.5f, NightEffects.ramp(1500), 0.001f)
    }

    @Test fun `obowiazujacy poziom dzwieku`() {
        assertEquals("medium", NightEffects.effectiveAudio("off", Strength.MEDIUM, Strength.MEDIUM))
        assertEquals("strong", NightEffects.effectiveAudio("strong", Strength.LIGHT, Strength.LIGHT))
        assertEquals("light", NightEffects.effectiveAudio("light", Strength.OFF, Strength.MEDIUM))
    }
}
