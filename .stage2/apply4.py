import shutil
B='app/src/main/java/com/nuvio/tv/'
shutil.copy('.stage2/WhisperListen.kt',B+'fork/voice/WhisperListen.kt')
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
edit(B+'fork/voice/VoiceModule.kt',[
("    private suspend fun listen(c: android.content.Context, onError: (Int) -> Unit): String? = kotlinx.coroutines.suspendCancellableCoroutine { cont ->",
 "    private suspend fun listen(c: android.content.Context, onError: (Int) -> Unit): String? =\n        if (com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, \"voice_whisper\", false) && WhisperListen.available(c)) WhisperListen.listen(c) else systemListen(c, onError)\n\n    private suspend fun systemListen(c: android.content.Context, onError: (Int) -> Unit): String? = kotlinx.coroutines.suspendCancellableCoroutine { cont ->"),
("            if (!SpeechRecognizer.isRecognitionAvailable(c)) {","            if (!(com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, \"voice_whisper\", false) && WhisperListen.available(c)) && !SpeechRecognizer.isRecognitionAvailable(c)) {"),
("        ModuleSetting(\"voice_key_service\"","        ModuleSetting(\"voice_whisper\", SettingsLevel.ESSENTIAL, \"Sterowanie głosem\", \"Rozpoznawanie mowy offline (Whisper)\", \"Wymaga pobranego modelu mowy (ustawienia napisów: model mowy); działa bez internetu\", defaultValue = false),\n        ModuleSetting(\"voice_key_service\""),
])
print('ok4')
