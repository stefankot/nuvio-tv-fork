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
    upstream = [line for line in changes.splitlines() if line and
                not line.split("\t")[-1].startswith(("ai-narrator/", ".github/workflows/ai-narrator-", "app/reshaped.gradle"))]
    print("UPSTREAM MODIFICATION REPORT")
    for line in upstream:
        print(line)
    if len(upstream) > 1 or (upstream and not upstream[0].endswith("app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerScreen.kt")):
        print("FAIL: upstream change budget expanded")
        failed = True
else:
    print("NOTE: origin/main merge-base unavailable; numeric diff not verified")
sys.exit(1 if failed else 0)
