B='app/src/main/java/com/nuvio/tv/'
p=B+'fork/lektor/LektorController.kt'; s=open(p).read()
a="            LektorEngine.load(cues); LektorEngine.enabled = true\n"
assert a in s
s=s.replace(a,a+"            // Procesor dźwięku włącza się dopiero po flush rurociągu: seek do bieżącej pozycji go wymusza.\n            PlayerBridge.send(PlayerEvent.OnSeekTo(LektorEngine.positionMs()))\n",1)
open(p,'w').write(s)
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
edit(B+'fork/sources/PlayerBridge.kt',[("    fun streamUrl(): String?","    /** Lektor czyta napisy z synchronizacji: wybierz polskie napisy z dodatku, jeśli żadne nie jest wybrane. */\n    fun ensurePolishSubtitle() {\n        val s = state ?: return\n        if (s.selectedAddonSubtitle != null) return\n        s.addonSubtitles.firstOrNull { it.lang.startsWith(\"pl\", true) || it.lang.startsWith(\"pol\", true) }?.let { send?.invoke(PlayerEvent.OnSelectAddonSubtitle(it)) }\n    }\n    fun streamUrl(): String?")])
edit(p,[("        PlayerBridge.send(PlayerEvent.OnReloadSubtitleAutoSyncCues)\n","        PlayerBridge.ensurePolishSubtitle()\n        PlayerBridge.send(PlayerEvent.OnReloadSubtitleAutoSyncCues)\n")])
print('ok14')
