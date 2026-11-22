#!/bin/bash
# jmgo_sesja.sh [minuty=10] : test jednej kompilacji na JMGO (ZASADY §5, TESTY_PLAN): sprawdza, czy nikt nie ogląda,
# czyści logi, startuje fork, obchodzi trasy pilotem (crawl.py, tylko strzałki) aż minie co najmniej N minut,
# potem zbiera: awarie (bufor crash), ANR, ostrzeżenia/błędy Fork*, wyjątki. Wynik: .stage/jmgo/<data>/raport.txt
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
export DEV="${DEV:-192.168.31.252:5555}"; export ANDROID_SERIAL="$DEV"
PKG=com.nuviodebug.com; ACT=$PKG/com.nuvio.tv.launcher.AppIconDefault
MIN=${1:-10}; OUT="$ROOT/.stage/jmgo/$(date +%Y%m%d-%H%M)"; mkdir -p "$OUT"; R="$OUT/raport.txt"
log() { echo "[$(date +%H:%M:%S)] $*" | tee -a "$R"; }
top=$(adb shell dumpsys activity activities | grep -m1 -E "mResumedActivity|topResumedActivity")
log "na wierzchu: $top"
if echo "$top" | grep -qiE "player|video|netflix|youtube|tvplayer|launcher.*Player" && ! echo "$top" | grep -q "$PKG"; then
  log "STOP: ktoś coś ogląda — test odłożony"; exit 3
fi
# 22.11: logcat się przewija (build169c: raport „ANR: 0”, a system zapisał ANR) — liczymy też wpisy ANR w dropboxie.
anr0=$(adb shell dumpsys dropbox 2>/dev/null | grep -c "data_app_anr")
adb logcat -c; adb logcat -b crash -c
adb shell am force-stop $PKG; adb shell am start -n $ACT >/dev/null; sleep 20
start=$(date +%s); round=0
while [ $(( $(date +%s) - start )) -lt $(( MIN * 60 )) ]; do
  round=$((round+1)); log "obchód $round"
  python3 "$ROOT/fork-tools/uitest/crawl.py" --steps 40 >> "$OUT/crawl.txt" 2>&1
  adb shell pidof $PKG >/dev/null || { log "!!! proces nie żyje po obchodzie $round — restart"; adb shell am start -n $ACT >/dev/null; sleep 20; }
done
log "czas testu: $(( ($(date +%s) - start) / 60 )) min"
adb logcat -b crash -d > "$OUT/crash.txt"; adb logcat -d > "$OUT/logcat.txt"
log "awarie (FATAL): $(grep -c 'FATAL EXCEPTION' "$OUT/crash.txt")"
log "ANR: $(grep -c 'ANR in' "$OUT/logcat.txt") w logcat, $(( $(adb shell dumpsys dropbox 2>/dev/null | grep -c "data_app_anr") - anr0 )) nowych w dropboxie (wszystkie aplikacje)"
adb shell dumpsys activity exit-info $PKG 2>/dev/null | grep -E "timestamp|reason=" | paste - - | head -5 > "$OUT/exit-info.txt"
grep -aE " [WE] Fork[A-Za-z]*" "$OUT/logcat.txt" | sed -E 's/^.{19}//' | sort | uniq -c | sort -rn | head -60 > "$OUT/fork_we.txt"
log "ostrzeżenia/błędy Fork* (unikalne): $(wc -l < "$OUT/fork_we.txt")"
grep -aE "FATAL|Caused by|AndroidRuntime" "$OUT/crash.txt" | head -20 >> "$R"
log "raport: $OUT"
