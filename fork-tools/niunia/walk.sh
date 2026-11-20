#!/bin/bash
# walk.sh — szybkie przejście pilotem po ekranach Niuni na JMGO (test paczek design systemu).
# Użycie:  source fork-tools/niunia/walk.sh   potem np.:  k DOWN; f; shot b145-detail
#   k KLAWISZ [ile]   naciśnij klawisz (DPAD_*, BACK, DPAD_CENTER…), domyślnie 1 raz, 0,8 s przerwy
#   kk "K1 K2 …"     sekwencja wykonana na urządzeniu bez przerw na adb (szybka, np. przy znikającej nakładce odtwarzacza)
#   f                 bieżący fokus (granice + teksty w środku)
#   ui                teksty ekranu (fork-tools/test/ui.sh)
#   shot NAZWA        zrzut ekranu do .stage/niunia/NAZWA.png (+ wersja 960 px NAZWA-s.png)
#   log [N]           ostatnie N wpisów ForkNiunia (domyślnie 8)
#   long              długie OK (przytrzymanie ok. 0,8 s)
export ANDROID_SERIAL="${ANDROID_SERIAL:-192.168.31.252:5555}"
WALK_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")/../.." && pwd)"
WALK_STAGE="${WALK_STAGE:-/Users/milajovovich/Desktop/nuvio 2/NuvioFork/.stage/niunia}"
k() { local n=${2:-1}; for _ in $(seq 1 "$n"); do adb shell input keyevent "$1"; sleep 0.8; done; }
kk() { local cmd=""; for x in $1; do cmd="$cmd input keyevent $x; sleep 0.5;"; done; adb shell "$cmd" >/dev/null 2>&1; }
f() { bash "$WALK_ROOT/fork-tools/test/focus.sh" | head -1; }
ui() { bash "$WALK_ROOT/fork-tools/test/ui.sh"; }
shot() { adb shell screencap -p /sdcard/walk.png >/dev/null 2>&1; adb pull /sdcard/walk.png "$WALK_STAGE/$1.png" >/dev/null 2>&1; sips -Z 960 "$WALK_STAGE/$1.png" --out "$WALK_STAGE/$1-s.png" >/dev/null 2>&1; echo "$WALK_STAGE/$1-s.png"; }
log() { adb logcat -d -v time | grep -E "ForkNiunia|FATAL|AndroidRuntime: (FATAL|Caused)" | cut -c19-220 | tail -"${1:-8}"; }
long() { adb shell input keyevent --longpress DPAD_CENTER; sleep 1; }
