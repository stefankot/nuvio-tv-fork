#!/bin/bash
# jmgo_pamiec.sh [sekundy=180] [trasa] : pamięć aplikacji po starcie (23.11 — zawieszanie przez pełną stertę 512 MB).
# Startuje fork, co 10 s zapisuje stertę Javy (dumpsys meminfo) i ostatnią linię GC z logcat; po 40 s opcjonalnie
# otwiera [trasa] (np. reshaped_live_tv) intencją debug com.nuvio.fork.OPEN (wysyłana 2×: pierwsza po zimnym starcie ginie).
# Wynik: .stage/jmgo/pamiec-<data>/ (pamiec.txt, logcat.txt). Nie sprawdza, czy ktoś ogląda — zrób to przed wywołaniem.
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
export ANDROID_SERIAL="${DEV:-192.168.31.252:5555}"
PKG=com.nuviodebug.com; ACT=$PKG/com.nuvio.tv.launcher.AppIconDefault
SEC=${1:-180}; ROUTE=${2:-}
OUT="$ROOT/.stage/jmgo/pamiec-$(date +%Y%m%d-%H%M)"; mkdir -p "$OUT"; R="$OUT/pamiec.txt"
anr0=$(adb shell dumpsys dropbox 2>/dev/null | grep -c "data_app_anr")
adb logcat -c
adb shell am force-stop $PKG; adb shell am start -n $ACT >/dev/null
t0=$(date +%s)
while [ $(( $(date +%s) - t0 )) -lt "$SEC" ]; do
  sleep 10
  el=$(( $(date +%s) - t0 ))
  if [ -n "$ROUTE" ] && [ $el -ge 40 ]; then
    ROUTE_DONE=$ROUTE; ROUTE=
    adb shell am start -a com.nuvio.fork.OPEN -n $ACT --es route "$ROUTE_DONE" >/dev/null; sleep 3
    adb shell am start -a com.nuvio.fork.OPEN -n $ACT --es route "$ROUTE_DONE" >/dev/null
    echo "t=${el}s otwarta trasa $ROUTE_DONE" | tee -a "$R"
  fi
  pid=$(adb shell pidof $PKG)
  heap=$(perl -e 'alarm shift; exec @ARGV' 8 adb shell "dumpsys meminfo $PKG | grep -E 'Java Heap:|TOTAL PSS'" 2>/dev/null | tr -s ' ' | tr '\n' ' ')
  gc=$(adb logcat -d 2>/dev/null | grep -E "GC freed" | tail -1 | grep -oE "[0-9]+% free, [0-9A-Z]+/[0-9A-Z]+")
  echo "t=${el}s pid=${pid:-BRAK} ${heap:-meminfo nie odpowiada} | GC: ${gc:--}" | tee -a "$R"
done
adb logcat -d > "$OUT/logcat.txt"
echo "nowe ANR w dropboxie: $(( $(adb shell dumpsys dropbox 2>/dev/null | grep -c "data_app_anr") - anr0 ))" | tee -a "$R"
grep -aE "ForkTvVod|ForkTvIndex|LiveTvEpg.*begin|live tv:" "$OUT/logcat.txt" | cut -c1-220 | tee -a "$R"
echo "wynik: $OUT"
