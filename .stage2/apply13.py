import shutil
B='app/src/main/java/com/nuvio/tv/'
shutil.copy('.stage2/Duration.kt',B+'fork/voice/Duration.kt')
import os; os.makedirs('app/src/test/java/com/nuvio/tv/fork/voice',exist_ok=True)
shutil.copy('.stage2/test/DurationParserTest.kt','app/src/test/java/com/nuvio/tv/fork/voice/DurationParserTest.kt')
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
edit(B+'fork/sources/PlayerBridge.kt',[("    fun streamUrl(): String? = state?.currentStreamUrl","    fun streamUrl(): String? = state?.currentStreamUrl\n    fun positionMs(): Long = com.nuvio.tv.fork.lektor.LektorEngine.positionMs()")])
edit(B+'fork/voice/VoiceActions.kt',[('        if (call.id == "subs.mismatch") {','''        if (call.id == "seek.back" || call.id == "seek.forward") {
            if (!com.nuvio.tv.fork.sources.PlayerBridge.active) return false
            val ms = DurationParser.millis(call.params["czas"].orEmpty()) ?: 60_000L
            val to = (com.nuvio.tv.fork.sources.PlayerBridge.positionMs() + if (call.id == "seek.back") -ms else ms).coerceAtLeast(0L)
            com.nuvio.tv.fork.sources.PlayerBridge.send(com.nuvio.tv.ui.screens.player.PlayerEvent.OnSeekTo(to))
            lastMessage = (if (call.id == "seek.back") "Cofam o " else "Przewijam o ") + (ms / 1000) + " s"; return true
        }
        if (call.id == "channel.number") {
            val n = call.params["n"]?.toIntOrNull() ?: return false
            val ch = com.nuvio.tv.reshaped.livetv.LiveTvRepository.uiState.value.channels.getOrNull(n - 1) ?: return false
            com.nuvio.tv.fork.tv.ekrany.PendingPlay.ask(ch.streamUrl)
            ForkFeatures.open?.invoke(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE); lastMessage = ch.name; return true
        }
        if (call.id == "subs.mismatch") {''')])
print('ok13')
