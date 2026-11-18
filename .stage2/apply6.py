B='app/src/main/java/com/nuvio/tv/'
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
edit(B+'fork/lektor/LektorEngine.kt',[
("    private val envelope = DuckEnvelope()","    @Volatile var voiceGain = 1f\n    @Volatile private var envelope = DuckEnvelope()\n    fun setDuckDepth(db: Float) { envelope = DuckEnvelope(depthDb = db) }"),
("            val g = LektorEngine.gainAt(t); val v = LektorEngine.voiceAt(t)","            val g = LektorEngine.gainAt(t); val v = LektorEngine.voiceAt(t) * LektorEngine.voiceGain"),
])
edit(B+'fork/lektor/LektorModule.kt',[
("    override fun actions() = listOf(","""    private const val K_VOL = "lektor_volume"; private const val K_DUCK = "lektor_duck"
    private val volumes = listOf("100", "70", "130")
    private val ducks = listOf("-10", "-6", "-14")

    /** Wczytuje głośność lektora i siłę ściszania do silnika (przy włączeniu lektora). */
    fun applySettings(c: android.content.Context) {
        LektorEngine.voiceGain = (com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, K_VOL, "100").toIntOrNull() ?: 100) / 100f
        LektorEngine.setDuckDepth(com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, K_DUCK, "-10").toFloatOrNull() ?: -10f)
    }

    private fun cycle(c: android.content.Context, key: String, values: List<String>) {
        val cur = com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, key, values[0])
        com.nuvio.tv.ui.screens.settings.ForkFeatures.setStr(c, key, values[(values.indexOf(cur) + 1) % values.size]); applySettings(c)
    }

    override fun actions() = listOf(
        ModuleAction("Lektor: głośność względem filmu (100% / 70% / 130%)", SettingsLevel.ESSENTIAL, "Kolejne naciśnięcia przełączają wartość") { c -> cycle(c, K_VOL, volumes) },
        ModuleAction("Lektor: siła ściszania oryginału (−10 / −6 / −14 dB)", SettingsLevel.ESSENTIAL, "Kolejne naciśnięcia przełączają wartość") { c -> cycle(c, K_DUCK, ducks) },"""),
])
edit(B+'fork/lektor/LektorController.kt',[("            LektorEngine.load(cues); LektorEngine.enabled = true","            LektorModule.applySettings(app)\n            LektorEngine.load(cues); LektorEngine.enabled = true")])
print('ok6')
