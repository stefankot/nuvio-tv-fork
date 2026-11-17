package com.nuvio.tv.fork.registry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForkRegistryTest {
    private class Fake(override val id: String, override val defaultEnabled: Boolean = false) : ForkModule {
        override val title = id
        override fun settings() = listOf(ModuleSetting("k_$id", SettingsLevel.SIMPLE, "g", "t"))
        override fun voiceCommands() = listOf(VoiceCommand("v_$id", listOf("p")))
        override fun playerHandlers() = listOf(object : PlayerHandler {})
        override fun homeRows() = listOf(ModuleHomeRow("r_$id", "r", order = if (id == "b") 1 else 2) {})
    }

    @Test fun `wylaczony modul nic nie zglasza`() {
        val r = ForkRegistry(listOf(Fake("a"))) { _, d -> d }
        assertTrue(r.settings().isEmpty() && r.voiceCommands().isEmpty() && r.playerHandlers().isEmpty() && r.homeRows().isEmpty())
    }

    @Test fun `wlaczony przelacznikiem zglasza`() {
        val on = setOf("module_a")
        val r = ForkRegistry(listOf(Fake("a"), Fake("b"))) { k, _ -> k in on }
        assertEquals(listOf("a" to "k_a"), r.settings().map { it.first to it.second.key })
        assertEquals(listOf("v_a"), r.voiceCommands().map { it.id })
    }

    @Test fun `domyslnie wlaczony i wylaczenie przelacznikiem`() {
        val r = ForkRegistry(listOf(Fake("a", defaultEnabled = true))) { _, d -> d }
        assertEquals(1, r.settings().size)
        val off = ForkRegistry(listOf(Fake("a", defaultEnabled = true))) { _, _ -> false }
        assertTrue(off.settings().isEmpty())
    }

    @Test fun `rzedy sortowane`() {
        val r = ForkRegistry(listOf(Fake("a", true), Fake("b", true))) { _, d -> d }
        assertEquals(listOf("r_b", "r_a"), r.homeRows().map { it.id })
    }
}
