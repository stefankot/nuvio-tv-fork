#!/bin/bash
# smoke.sh [skórki...] : test dymny na emulatorze (ANDROID_SERIAL, domyślnie emulator-5554). Dla każdej skórki: start,
# każda zakładka menu (Wstecz z treści → menu, ▼ do kolejnej pozycji, OK), na każdym ekranie ▶ do treści, OK, Wstecz.
# Wynik: .stage/smoke/<data>.txt (crashe, wyjątki Fork*, fokus po każdym kroku). Kod 1 = był crash.
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"; S="$ROOT/fork-tools/test"
export ANDROID_SERIAL="${ANDROID_SERIAL:-emulator-5554}"
PKG=com.nuviodebug.com; ACT=$PKG/com.nuvio.tv.launcher.AppIconDefault
OUT="$ROOT/.stage/smoke/$(date +%Y%m%d-%H%M).txt"; mkdir -p "$(dirname "$OUT")"
SKINS=("${@:-niunia kicia nuvio}"); [ $# -eq 0 ] && SKINS=(niunia kicia nuvio)
crash=0
log() { echo "$*" | tee -a "$OUT"; }
alive() { adb shell pidof $PKG >/dev/null 2>&1; }
key() { adb shell input keyevent "$1"; sleep "${2:-1.5}"; }
foc() { bash "$S/focus.sh" 2>/dev/null | head -1 | cut -c1-80; }
check() { if ! alive || adb logcat -b crash -d | grep -q "FATAL EXCEPTION"; then log "  !!! CRASH po: $1"; adb logcat -b crash -d | grep -aE "FATAL|Caused by|at com.nuvio" | head -8 >> "$OUT"; adb logcat -b crash -c; crash=1; adb shell am start -n $ACT >/dev/null; sleep 15; fi; }
for skin in "${SKINS[@]}"; do
  log "=== skórka: $skin"
  bash "$ROOT/fork-tools/prefs.sh" set sp:fork_skin skin "$skin" >/dev/null 2>&1
  adb shell am force-stop $PKG; adb logcat -c; adb logcat -b crash -c
  adb shell am start -n $ACT >/dev/null; sleep 20; check "start"
  log "  start: fokus = $(foc)"
  for i in $(seq 1 9); do
    key BACK 2; check "Wstecz (menu) $i"
    for j in $(seq 1 $i); do key DPAD_DOWN 0.8; done
    log "  menu $i: $(foc)"
    key DPAD_CENTER 4; check "OK zakładka $i"
    key DPAD_RIGHT 1.5; log "    treść: $(foc)"
    key DPAD_CENTER 4; check "OK w treści zakładki $i"; log "    po OK: $(foc)"
    key BACK 3; check "Wstecz z pozycji $i"
  done
  adb logcat -d | grep -aE " [WE] Fork[A-Za-z]*:.*(Exception|Error)" | sort | uniq -c | sort -rn | head -15 >> "$OUT"
done
log "=== koniec, crash=$crash, wynik: $OUT"; exit $crash
