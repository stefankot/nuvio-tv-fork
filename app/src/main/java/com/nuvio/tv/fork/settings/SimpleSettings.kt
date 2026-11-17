package com.nuvio.tv.fork.settings

/** Poziomy ustawień. PROSTY to flaga modułu; Essential i Advanced odpowiadają `ExperienceMode` bazy. */
enum class UiLevel { SIMPLE, ESSENTIAL, ADVANCED }

object LevelSwitch {
    /** Wynik zmiany poziomu: czy flaga Prosty ma być włączona i jaki `ExperienceMode` (nazwa) zapisać (null = bez zmiany). */
    data class Change(val simpleFlag: Boolean, val experienceMode: String?)

    fun to(level: UiLevel): Change = when (level) {
        UiLevel.SIMPLE -> Change(true, null)
        UiLevel.ESSENTIAL -> Change(false, "ESSENTIAL")
        UiLevel.ADVANCED -> Change(false, "ADVANCED")
    }
}

/** Jedna pozycja ekranu Prostego. */
data class SimpleItem(val group: String, val id: String, val title: String, val module: String? = null)

object SimpleScreenSpec {
    const val MAX_ITEMS = 20

    val all: List<SimpleItem> = listOf(
        SimpleItem("Ty", "account", "Konto"), SimpleItem("Ty", "profile", "Profil"), SimpleItem("Ty", "app_language", "Język aplikacji"),
        SimpleItem("Oglądanie", "audio_language", "Język dźwięku"), SimpleItem("Oglądanie", "subtitles", "Napisy"),
        SimpleItem("Oglądanie", "subtitle_size", "Rozmiar napisów", "sources"), SimpleItem("Oglądanie", "next_episode", "Następny odcinek automatycznie"),
        SimpleItem("Oglądanie", "skip_intro", "Pomijaj czołówki"), SimpleItem("Oglądanie", "pl_or_quality", "Polski dźwięk czy jakość", "sources"),
        SimpleItem("Tryb nocny", "night", "Tryb nocny", "night"), SimpleItem("Tryb nocny", "night_hours", "Godziny", "night"), SimpleItem("Tryb nocny", "night_strength", "Siła", "night"),
        SimpleItem("Telewizja", "tv_sources", "Źródła TV i program", "tv"), SimpleItem("Telewizja", "start_with", "Po uruchomieniu pokaż", "launcher"),
        SimpleItem("Telewizja", "start_with_tv", "Uruchamiaj z telewizorem", "launcher"),
        SimpleItem("Źródła", "addons", "Dodatki"), SimpleItem("Źródła", "services", "Połączone usługi"),
        SimpleItem("System", "updates", "Aktualizacje"), SimpleItem("System", "level", "Poziom ustawień"), SimpleItem("System", "help", "Pomoc"),
    )

    /** Pozycje do pokazania: te bez modułu zawsze, z modułem tylko gdy moduł istnieje i jest włączony; nigdy więcej niż [MAX_ITEMS]. */
    fun visible(enabledModules: Set<String>): List<SimpleItem> =
        all.filter { it.module == null || it.module in enabledModules }.take(MAX_ITEMS)

    fun groups(items: List<SimpleItem>): List<Pair<String, List<SimpleItem>>> = items.groupBy { it.group }.toList()
}

/** Filtr pozycji rejestru wg poziomu: Prosty = tylko SIMPLE, Essential = SIMPLE+ESSENTIAL, Advanced = wszystko. */
object ModuleSettingsFilter {
    fun forLevel(items: List<com.nuvio.tv.fork.registry.ModuleSetting>, level: UiLevel) = items.filter {
        when (level) {
            UiLevel.SIMPLE -> it.level == com.nuvio.tv.fork.registry.SettingsLevel.SIMPLE
            UiLevel.ESSENTIAL -> it.level != com.nuvio.tv.fork.registry.SettingsLevel.ADVANCED
            UiLevel.ADVANCED -> true
        }
    }
}
