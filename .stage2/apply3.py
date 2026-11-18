import shutil
B='app/src/main/java/com/nuvio/tv/'
shutil.copy('.stage2/VoiceModule.kt',B+'fork/voice/VoiceModule.kt')
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
edit(B+'fork/discover/DiscoverModule.kt',[
("    override fun routes() = listOf(","""    private val rowTitles = mapOf("continue" to "Oglądaj dalej", "followed" to "Obserwowane", "tonight" to "Dziś wieczorem", "films" to "Filmy w archiwum", "series" to "Seriale w archiwum")

    override fun settings() = DiscoverRows.DEFAULT_ORDER.map { id ->
        com.nuvio.tv.fork.registry.ModuleSetting("discover_show_$id", com.nuvio.tv.fork.registry.SettingsLevel.ESSENTIAL, "Odkrywaj", "Rząd: " + (rowTitles[id] ?: id.removePrefix("genre_")), "", defaultValue = true)
    }

    override fun routes() = listOf("""),
("        return DiscoverRows.build(input)","        val hidden = DiscoverRows.DEFAULT_ORDER.filterNot { ForkFeatures.enabled(context, \"discover_show_$it\", true) }.toSet()\n        return DiscoverRows.build(input, hiddenRows = hidden)"),
])
print('ok3')
