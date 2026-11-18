import shutil
B='app/src/main/java/com/nuvio/tv/'
shutil.copy('.stage2/VoiceHelp.kt',B+'fork/voice/VoiceHelp.kt')
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
edit(B+'fork/voice/VoiceModule.kt',[
("    private val parser = RuleParser()","""    override fun routes() = listOf(com.nuvio.tv.fork.registry.ModuleRoute(VOICE_HELP_ROUTE) { composable(VOICE_HELP_ROUTE) { VoiceHelpScreen() } })

    override fun actions() = listOf(
        com.nuvio.tv.fork.registry.ModuleAction("Sterowanie głosem: przykłady poleceń", SettingsLevel.SIMPLE, "Lista tego, co można powiedzieć") { com.nuvio.tv.ui.screens.settings.ForkFeatures.open?.invoke(VOICE_HELP_ROUTE) },
    )

    private val parser = RuleParser()"""),
("import androidx.compose.runtime.Composable","import androidx.compose.runtime.Composable\nimport androidx.navigation.compose.composable"),
])
print('ok10')
