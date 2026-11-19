#!/usr/bin/env python3
"""Static narrator/upstream contract check; non-mutating and no network access."""
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[2]
checks = {
    "app/reshaped.gradle": [
        "ai-narrator/src/main/kotlin",
        "ai-narrator/src/test/kotlin",
        "ZONOS_ARM32_APK_URL",
        "ZONOS_ARM32_APK_SHA256",
    ],
    "ai-narrator/src/main/kotlin/com/nuvio/tv/ainarrator/ZonosOnDeviceModels.kt": [
        "downloadOnUserRequest", "verifySha256", "applicationContext.filesDir", "ModelPhase.READY",
    ],
    "ai-narrator/src/main/kotlin/com/nuvio/tv/ainarrator/NarratorPreparation.kt": [
        "NarratorPreparationPlan", "PreparationHalf", "NarratorPersistentAudio",
        "filesDir", "15L * 60L * 1000L",
    ],
    "ai-narrator/src/main/kotlin/com/nuvio/tv/ainarrator/ZonosNativeServer.kt": [
        "127.0.0.1", "ZonosArm32Installer", "isPackaged", "FileProvider",
    ],
    "ai-narrator/src/main/kotlin/com/nuvio/tv/ainarrator/ZonosArm32Installer.kt": [
        "downloadOnFeatureEnable", "ZONOS_ARM32_APK_URL", "verifyApkSigner",
        "Arm32GitHubSource", "FileProvider",
    ],
    "ai-narrator/worker-arm32/app/src/main/AndroidManifest.xml": [
        "com.nuvio.tv.zonos32.permission.CONTROL",
    ],
    "ai-narrator/worker-arm32/app/src/main/java/com/nuvio/tv/zonos32/ZonosWorkerService.java": [
        "libzonos3_server.so", "START", "verifiedCopy", "ZONOS3_ACCESS_TOKEN",
    ],
    "app/src/main/res/xml/file_paths.xml": ["zonos_gguf", "zonos3-mini/v1/"],
    "ai-narrator/src/main/kotlin/com/nuvio/tv/ainarrator/NarratorEngine.kt": [
        "NarratorPersistentAudio", "ZONOS3 requires pre-rendered audio",
    ],
    "app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerScreen.kt": [
        "com.nuvio.tv.ainarrator.NarratorOverlay(",
    ],
    "app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeController.kt": [
        "val exoPlayer: ExoPlayer?",
        "val playbackTimeline: StateFlow<PlaybackTimelineState>",
        "internal val subtitleCueTracker = SubtitleCueTracker()",
    ],
    "app/src/main/java/com/nuvio/tv/ui/screens/player/SubtitleCueTracker.kt": [
        "val state: StateFlow<SubtitleCueState>",
    ],
    "ai-narrator/src/main/kotlin/com/nuvio/tv/ainarrator/LocalZonosTts.kt": [
        '"/v2/generate"', '"pl"', '"wav"', 'base.host == "127.0.0.1"',
    ],
    "app/src/main/java/com/nuvio/tv/ui/screens/player/NuvioBaseMpvView.kt": [
        "val mpv: MPV",
    ],
    "app/src/main/java/com/nuvio/tv/ui/screens/player/NuvioMpvSurfaceView.kt": [
        'MPV_SUB_TEXT_PROPERTY = "sub-text"',
    ],
}
failed = False
for path, fragments in checks.items():
    filename = root / path
    value = filename.read_text(encoding="utf8") if filename.exists() else ""
    for fragment in fragments:
        ok = fragment in value
        print(f'{"PASS" if ok else "FAIL"} {path}: {fragment}')
        failed |= not ok
def git(*args):
    result = subprocess.run(["git", *args], cwd=root, text=True, capture_output=True)
    if result.returncode:
        return None
    return result.stdout.strip()
changes = git("diff", "--numstat", "origin/main...HEAD", "--")
if changes is not None:
    allowed = {
        "app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerScreen.kt": 5,
        "app/src/main/AndroidManifest.xml": 5,
        "app/src/main/res/xml/file_paths.xml": 7,
    }
    print("UPSTREAM MODIFICATION REPORT")
    seen = set()
    for line in changes.splitlines():
        columns = line.split("\t")
        if len(columns) != 3:
            continue
        added, removed, path = columns
        if path.startswith(("ai-narrator/", ".github/workflows/ai-narrator-")) or path == "app/reshaped.gradle":
            continue
        seen.add(path)
        print(line)
        if path not in allowed or not added.isdigit() or not removed.isdigit() or int(removed) or int(added) > allowed[path]:
            print("FAIL: unexpected upstream modification or exceeded allowance")
            failed = True
    if "app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerScreen.kt" not in seen:
        print("FAIL: player adapter hook missing")
        failed = True
else:
    print("NOTE: origin/main merge-base unavailable; numeric diff not verified")

sys.exit(1 if failed else 0)
