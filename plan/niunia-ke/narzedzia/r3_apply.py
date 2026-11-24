#!/usr/bin/env python3
"""Runda 3: ekran źródeł Niuni (Z-14 Streams) — plik, teksty, trasa skórki, wpięcie w StreamScreen (jedna dopisana linia), wiersz hooks.tsv."""
import os, shutil
W = "/Users/milajovovich/Desktop/nuvio 2/wt-niunia-ke"
S = "/private/tmp/claude-501/-Users-milajovovich-Desktop-nuvio-tv-interface/ba68948d-78b5-4bdb-8bc1-f4f288f6f583/scratchpad/r3"
J = f"{W}/app/src/main/java/com/nuvio/tv"
os.makedirs(f"{J}/skin/niunia/stream", exist_ok=True)
shutil.copy(f"{S}/NiuniaStreams.kt", f"{J}/skin/niunia/stream/NiuniaStreams.kt")

p = f"{W}/app/src/main/res/values/niunia_ke_strings.xml"; s = open(p).read()
if "niunia_ke_src_all" not in s:
    s = s.replace("</resources>", '''    <string name="niunia_ke_src_all">Wszystkie</string>
    <string name="niunia_ke_src_lektor">Lektor PL</string>
    <string name="niunia_ke_src_subs">Napisy PL</string>
    <string name="niunia_ke_src_4k">4K</string>
    <string name="niunia_ke_src_1080">1080p</string>
    <string name="niunia_ke_src_dub">Dubbing PL</string>
    <string name="niunia_ke_src_lektor_pl">Lektor PL</string>
    <string name="niunia_ke_src_subs_pl">Napisy PL</string>
    <string name="niunia_ke_src_all_addons">Wszystkie dodatki</string>
    <string name="niunia_ke_src_loading">Szukam źródeł…</string>
    <string name="niunia_ke_src_error">Nie udało się pobrać źródeł</string>
    <string name="niunia_ke_src_none">Brak źródeł</string>
    <string name="niunia_ke_src_none_hint">Żaden dodatek nie zwrócił źródła dla tego tytułu.</string>
    <string name="niunia_ke_src_retry">Spróbuj ponownie</string>
    <string name="niunia_ke_src_count">Źródła: %1$d · dodatki: %2$d</string>
    <string name="niunia_ke_src_more">· szukam dalej…</string>
    <string name="niunia_ke_src_cached">W pamięci</string>
</resources>''')
    open(p, "w").write(s)

p = f"{J}/skin/niunia/NiuniaSkin.kt"; s = open(p).read()
a = '''        "livetv" -> SkinScreen { a ->'''
assert a in s
if '"streams" ->' not in s:
    s = s.replace(a, '''        // Z-14 (makieta Streams): lista źródeł w stylu Niuni; odtwarzanie, autoodtwarzanie i okna zostają w StreamScreen bazy.
        "streams" -> SkinScreen { a ->
            com.nuvio.tv.skin.niunia.stream.NiuniaStreams(
                uiState = a["uiState"] as com.nuvio.tv.ui.screens.stream.StreamScreenUiState,
                resumeText = a["resumeText"] as String?,
                onAddonFilter = a["onFilter"] as (String?) -> Unit,
                onRetry = a["onRetry"] as () -> Unit,
                onPlay = a["onPlay"] as (com.nuvio.tv.domain.model.Stream) -> Unit,
            ); true
        }
''' + a, 1)
    open(p, "w").write(s)

p = f"{J}/ui/screens/stream/StreamScreen.kt"; s = open(p).read()
a = '''            // Content overlay
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Left side - Title/Logo (centered vertically)'''
assert a in s
hook = '''            if (com.nuvio.tv.fork.ForkHooks.skinScreen("streams", mapOf("uiState" to uiState, "resumeText" to resumeText, "onFilter" to { f: String? -> viewModel.onEvent(StreamScreenEvent.OnAddonFilterSelected(f)) }, "onRetry" to { viewModel.onEvent(StreamScreenEvent.OnRetry) }, "onPlay" to { stream: com.nuvio.tv.domain.model.Stream -> scope.coroutineLaunch { val info = viewModel.resolveStreamForPlayback(stream); if (info != null) { viewModel.persistBingeGroupForStream(stream); pendingRestoreOnResume = true; routePlayback(info); viewModel.onEvent(StreamScreenEvent.OnAutoPlayConsumed) } } }))) Unit else // Fork hook: skin (ekran źródeł skórki)
'''
if "ekran źródeł skórki" not in s:
    s = s.replace(a, a.replace("            Row(\n", hook + "            Row(\n", 1), 1)
    open(p, "w").write(s)

p = f"{W}/fork-tools/hooks.tsv"; s = open(p).read()
if "ekran źródeł skórki" not in s:
    if not s.endswith("\n"): s += "\n"
    s += "app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreen.kt\tFork hook: skin (ekran źródeł skórki)\t// Content overlay\tskin\tskin: źródła (Niunia, Z-14)\n"
    open(p, "w").write(s)
print("runda 3 (źródła) nałożona")
