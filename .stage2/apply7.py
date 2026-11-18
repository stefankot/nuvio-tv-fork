B='app/src/main/java/com/nuvio/tv/'
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
# PlayerBridge: automatyczny lektor (B09-6) - raz na odtwarzanie, gdy brak polskiej ścieżki, są polskie napisy i włączone „lektor_auto”
edit(B+'fork/sources/PlayerBridge.kt',[
("    fun set(s: PlayerUiState, e: (PlayerEvent) -> Unit) { state = s; send = e }",
"""    private var autoTried = false
    fun set(s: PlayerUiState, e: (PlayerEvent) -> Unit) { state = s; send = e }

    /** B09-6: lektor sam, gdy profil chce polskiego, brak polskiej ścieżki dźwięku, są polskie napisy i włączono „lektor_auto”. Raz na odtwarzanie. */
    fun maybeAutoLektor(c: android.content.Context) {
        val s = state ?: return
        if (autoTried || s.audioTracks.isEmpty() || !s.isPlaying) return
        if (!com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, "lektor_auto", false)) return
        autoTried = true
        val hasPlAudio = AudioCheck.hasPolish(s.audioTracks.map { it.language })
        val hasPlSubs = s.addonSubtitles.any { it.lang.startsWith("pl", true) || it.lang.startsWith("pol", true) }
        val wantsPl = SourceChoice.answer(c).let { it == "pl_always" || it == "pl_if_good" }
        if (com.nuvio.tv.fork.lektor.LektorRules.step(wantsPl, hasPlAudio, hasPlSubs, true) == com.nuvio.tv.fork.lektor.LektorRules.Step.LEKTOR) com.nuvio.tv.fork.lektor.LektorController.start(c)
    }"""),
("    fun clear() { state = null; send = null; queue = null }","    fun clear() { state = null; send = null; queue = null; autoTried = false }"),
])
edit(B+'fork/ForkHooks.kt',[("        androidx.compose.runtime.SideEffect { com.nuvio.tv.fork.sources.PlayerBridge.set(uiState, onEvent) }","        val ctx = androidx.compose.ui.platform.LocalContext.current\n        androidx.compose.runtime.SideEffect { com.nuvio.tv.fork.sources.PlayerBridge.set(uiState, onEvent) }\n        androidx.compose.runtime.LaunchedEffect(uiState.audioTracks.size, uiState.isPlaying) { com.nuvio.tv.fork.sources.PlayerBridge.maybeAutoLektor(ctx) }")])
edit(B+'fork/lektor/LektorController.kt',[('        return "Włączam lektora…"','        val first = !com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(app, "lektor_dolby_note", false)\n        if (first) com.nuvio.tv.ui.screens.settings.ForkFeatures.set(app, "lektor_dolby_note", true)\n        return "Włączam lektora…" + if (first) " " + LektorRules.DOLBY_NOTE else ""')])
print('ok7')
