package com.nuvio.tv.fork.subfonts

import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ModuleSetting
import com.nuvio.tv.fork.registry.SettingsLevel

/** Czcionki napisów Atkinson Hyperlegible i Tiresias dołączone do aplikacji ([BundledSubtitleFonts]). */
object SubtitleFontsModule : ForkModule {
    override val id = "subfonts"
    override val title = "Czcionki napisów Atkinson i Tiresias"
    override val defaultEnabled = true

    override fun settings() = listOf(
        ModuleSetting(
            BundledSubtitleFonts.KEY, SettingsLevel.ESSENTIAL, "Napisy", "Czcionki Atkinson i Tiresias",
            "Dwie bardzo czytelne czcionki do wyboru w wyglądzie napisów. Atkinson jest wybrana od razu",
            defaultValue = true, place = "napisy/napisy",
        ),
    )
}
