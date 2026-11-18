import shutil
B='app/src/main/java/com/nuvio/tv/'
shutil.copy('.stage2/PlayerBridge.kt',B+'fork/sources/PlayerBridge.kt')
shutil.copy('.stage2/LektorEngine.kt',B+'fork/lektor/LektorEngine.kt')
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
edit(B+'ui/screens/player/PlayerRuntimeControllerInitialization.kt',[(".setAudioProcessors(arrayOf(gainAudioProcessor, nightModeAudioProcessor))",".setAudioProcessors(arrayOf(gainAudioProcessor, nightModeAudioProcessor, com.nuvio.tv.fork.lektor.LektorAudioProcessor())) // Fork hook: lektor")])
edit(B+'fork/sources/PlayerBridge.kt',[("    fun set(s: PlayerUiState, e: (PlayerEvent) -> Unit) { state = s; send = e }","    fun set(s: PlayerUiState, e: (PlayerEvent) -> Unit) { state = s; send = e; com.nuvio.tv.fork.lektor.LektorEngine.updatePosition(s.currentPosition, s.isPlaying) }")])
shutil.copy('.stage2/LektorController.kt',B+'fork/lektor/LektorController.kt')
edit(B+'fork/voice/VoiceActions.kt',[("        if (call.id == \"channel.play\") {","        if (call.id == \"lektor.on\" || call.id == \"lektor.off\") {\n            lastMessage = if (call.id == \"lektor.on\") com.nuvio.tv.fork.lektor.LektorController.start(context) else { com.nuvio.tv.fork.lektor.LektorController.stop(); \"Lektor wyłączony\" }; return true\n        }\n        if (call.id == \"channel.play\") {")])
edit(B+'fork/ForkHooks.kt',[("onDispose { com.nuvio.tv.fork.sources.PlayerBridge.clear() }","onDispose { com.nuvio.tv.fork.sources.PlayerBridge.clear(); com.nuvio.tv.fork.lektor.LektorController.stop() }")])
print('ok')
