B='app/src/main/java/com/nuvio/tv/'
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
edit(B+'fork/sources/PlayerBridge.kt',[("    fun cues() =","    fun streamUrl(): String? = state?.currentStreamUrl\n    fun cues() =")])
edit(B+'fork/voice/VoiceActions.kt',[('        if (call.id == "subs.mismatch") {','''        if (call.id == "fav.add" || call.id == "fav.remove") {
            val url = com.nuvio.tv.fork.sources.PlayerBridge.streamUrl() ?: return false
            val repo = com.nuvio.tv.reshaped.livetv.LiveTvRepository
            val ch = repo.uiState.value.channels.firstOrNull { it.streamUrl == url } ?: return false
            val isFav = url in repo.uiState.value.favoriteUrls
            if ((call.id == "fav.add") != isFav) repo.toggleFavorite(ch)
            lastMessage = if (call.id == "fav.add") "Dodano do ulubionych: ${ch.name}" else "Usunięto z ulubionych: ${ch.name}"; return true
        }
        if (call.id == "subs.mismatch") {''')])
print('ok12')
