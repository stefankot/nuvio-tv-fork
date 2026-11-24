#!/bin/bash
# perf_jmgo.sh <krok> <katalog_wyniku> [arg] — pomiary wydajności forka na JMGO (prompt plan/ux/jmgo/PROMPT.md, pkt 4).
#   starty <kat> [n=10]   zimny start ×n (force-stop → am start -W): TotalTime, Displayed, Fully drawn, firstFocus (ForkFocus);
#                         ciepły ×n (HOME → am start -W). Wynik: <kat>/starty.txt + podsumowanie (mediana, min, max).
#   pamiec <kat> <etykieta>  dumpsys meminfo (PSS, sterta Java, natywna, grafika) → <kat>/pamiec.txt
#   perfetto <kat> <etykieta> [s=10]  ślad perfetto (sched gfx view am wm dalvik) w tle, w tym czasie wykonaj przejście;
#                         plik <kat>/<etykieta>.perfetto-trace (otwierać w ui.perfetto.dev).
# Tylko com.nuviodebug.com; nic nie czyści i nie zmienia ustawień systemu. Przed użyciem: jmgo_czekaj_wolny.sh.
set -u
S=$(cd "$(dirname "$0")/../test" && pwd)
eval "$(bash "$S/jmgo_adb.sh" --env)"; bash "$S/jmgo_adb.sh" >/dev/null
PKG=com.nuviodebug.com; LAUNCH=$PKG/com.nuvio.tv.launcher.AppIconDefault
cmd=${1:?krok}; OUT=${2:?katalog}; mkdir -p "$OUT"
med() { sort -n | awk '{a[NR]=$1} END{if(NR==0){print "-";exit} m=(NR%2)?a[(NR+1)/2]:(a[NR/2]+a[NR/2+1])/2; printf "mediana %s, min %s, max %s (n=%d)", m, a[1], a[NR], NR}'; }
ms() { # „+3s255ms” → 3255
  python3 -c "import re,sys;s=sys.argv[1];m=re.search(r'\+(?:(\d+)s)?(\d+)ms',s);print(int(m.group(1) or 0)*1000+int(m.group(2)) if m else '')" "$1"; }
case $cmd in
  starty)
    n=${3:-10}; F="$OUT/starty.txt"; : > "$F"
    for i in $(seq 1 "$n"); do
      adb shell am force-stop $PKG; sleep 3; adb logcat -c
      tt=$(adb shell am start -W -n $LAUNCH | grep -o "TotalTime: [0-9]*" | tr -dc 0-9)
      sleep 12
      L=$(adb logcat -d -v time -s ActivityTaskManager:I ForkFocus:I)
      disp=$(ms "$(echo "$L" | grep -m1 "Displayed $PKG" | grep -o '+[0-9a-z]*ms')")
      full=$(ms "$(echo "$L" | grep -m1 "Fully drawn $PKG" | grep -o '+[0-9a-z]*ms')")
      ff=$(echo "$L" | grep -m1 -o "firstFocus=[0-9]*" | tr -dc 0-9)
      echo "zimny $i TotalTime=$tt Displayed=$disp FullyDrawn=$full firstFocus=$ff" | tee -a "$F"
    done
    for i in $(seq 1 "$n"); do
      adb shell input keyevent KEYCODE_HOME; sleep 3
      tt=$(adb shell am start -W -n $LAUNCH | grep -o "TotalTime: [0-9]*" | tr -dc 0-9)
      echo "ciepły $i TotalTime=$tt" | tee -a "$F"; sleep 2
    done
    for k in TotalTime Displayed FullyDrawn firstFocus; do
      echo "zimny $k: $(grep '^zimny' "$F" | grep -o "$k=[0-9]*" | tr -dc '0-9\n' | grep . | med)" | tee -a "$F"
    done
    echo "ciepły TotalTime: $(grep '^ciepły' "$F" | grep -o "TotalTime=[0-9]*" | tr -dc '0-9\n' | grep . | med)" | tee -a "$F" ;;
  pamiec)
    lab=${3:-?}
    m=$(adb shell dumpsys meminfo $PKG)
    pss=$(echo "$m" | grep -m1 "TOTAL PSS:" | awk '{print $3}'); [ -z "$pss" ] && pss=$(echo "$m" | grep -m1 -E "^ *TOTAL " | awk '{print $2}')
    jh=$(echo "$m" | grep -m1 "Java Heap:" | awk '{print $3}'); nh=$(echo "$m" | grep -m1 "Native Heap:" | awk '{print $3}'); gr=$(echo "$m" | grep -m1 "Graphics:" | awk '{print $2}')
    echo "$(date +%H:%M) $lab: PSS ${pss} KB, Java ${jh} KB, natywna ${nh} KB, grafika ${gr} KB" | tee -a "$OUT/pamiec.txt" ;;
  perfetto)
    lab=${3:-slad}; s=${4:-10}
    adb shell "perfetto -o /data/misc/perfetto-traces/$lab.perfetto-trace -t ${s}s sched gfx view am wm dalvik input" >/dev/null 2>&1 &
    echo "perfetto $lab: $s s — wykonaj przejście"; sleep $((s + 3)); wait
    adb pull /data/misc/perfetto-traces/$lab.perfetto-trace "$OUT/$lab.perfetto-trace" >/dev/null && adb shell rm /data/misc/perfetto-traces/$lab.perfetto-trace
    ls -la "$OUT/$lab.perfetto-trace" ;;
  *) echo "nieznany krok $cmd"; exit 1 ;;
esac
