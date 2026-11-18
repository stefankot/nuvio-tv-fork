package com.nuvio.tv.fork.registry

/** Jedyna jawna lista modułów (bez refleksji). Nowy moduł dopisuj tutaj. */
object ForkModules {
    val all: List<ForkModule> = listOf(com.nuvio.tv.fork.tv.TvModule, com.nuvio.tv.fork.sources.SourcesModule, com.nuvio.tv.fork.night.NightModule, com.nuvio.tv.fork.launcher.LauncherModule, com.nuvio.tv.fork.voice.VoiceModule, com.nuvio.tv.fork.discover.DiscoverModule, com.nuvio.tv.fork.lektor.LektorModule, com.nuvio.tv.fork.picks.PicksModule, com.nuvio.tv.fork.family.FamilyModule)
}
