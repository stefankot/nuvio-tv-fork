import shutil
B='app/src/main/java/com/nuvio/tv/'
shutil.copy('.stage2/PlayerBridge.kt',B+'fork/sources/PlayerBridge.kt')
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
edit(B+'ui/screens/player/PlayerScreen.kt',[("    val uiState by viewModel.uiState.collectAsState()\n    val arcRemote","    val uiState by viewModel.uiState.collectAsState()\n    com.nuvio.tv.fork.ForkHooks.playerBridge(uiState) { viewModel.onEvent(it) } // Fork hook: player bridge\n    val arcRemote")])
edit(B+'fork/ForkHooks.kt',[("    // --- Rejestr modułów (B00) ---","""    /** PlayerScreen: udostępnia stan i zdarzenia odtwarzacza modułom (napisy nie pasują, głos). */
    @Composable
    fun playerBridge(uiState: PlayerUiState, onEvent: (PlayerEvent) -> Unit) {
        androidx.compose.runtime.SideEffect { com.nuvio.tv.fork.sources.PlayerBridge.set(uiState, onEvent) }
        androidx.compose.runtime.DisposableEffect(Unit) { onDispose { com.nuvio.tv.fork.sources.PlayerBridge.clear() } }
    }

    // --- Rejestr modułów (B00) ---""")])
edit(B+'fork/voice/VoiceActions.kt',[("        if (call.id == \"channel.play\") {","""        if (call.id == "subs.mismatch") {
            val msg = com.nuvio.tv.fork.sources.PlayerBridge.subtitleMismatch() ?: return false
            lastMessage = msg; return true
        }
        if (call.id == "channel.play") {"""),("    fun run(context: Context","    @Volatile var lastMessage: String? = null\n\n    fun run(context: Context")])
edit(B+'fork/voice/VoiceModule.kt',[('shown = if (call != null) "„$t” → ${call.id}" + (if (done) " ✓" else "")','shown = if (call != null) "„$t” → ${call.id}" + (if (done) " ✓" else "") + (VoiceActions.lastMessage?.let { " · $it" } ?: "")')])
print("ok")
