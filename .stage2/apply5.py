B='app/src/main/java/com/nuvio/tv/'
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
edit(B+'fork/voice/VoiceActions.kt',[
('        "guide.show" to com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE,','        "genre.show" to "fork_discover", "archive.movies" to "fork_discover", "now.kind" to "fork_discover", "followed.news" to "fork_discover", "continue" to "fork_discover",\n        "mood.light" to "fork_picks", "mood.heavy" to "fork_picks", "mood.gems" to "fork_picks",\n        "guide.show" to com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE,'),
('        if (call.id == "subs.mismatch") {','''        if (call.id == "subs.bigger" || call.id == "subs.smaller") {
            val size = if (call.id == "subs.bigger") com.nuvio.tv.fork.sources.SubtitleSizeChoice.LARGE else com.nuvio.tv.fork.sources.SubtitleSizeChoice.SMALL
            scope.launch { com.nuvio.tv.fork.sources.SourceChoice.applySubtitleSize(context, size) }
            lastMessage = "Napisy: " + size.title.lowercase(); return true
        }
        if (call.id == "subs.mismatch") {'''),
('    @Volatile var lastMessage: String? = null','    @Volatile var lastMessage: String? = null\n    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)'),
])
print('ok5')
