#!/bin/bash
# go.sh "cel" KIERUNEK [max] : przesuwa fokus (DPAD_<KIERUNEK>) aż fokus będzie na celu, wtedy OK. Bez celu = BRAK, nic nie naciska.
S=$(cd "$(dirname "$0")" && pwd); t="$1"; d="$2"; max=${3:-15}
for i in $(seq 1 $max); do
  adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb pull /sdcard/u.xml /tmp/u.xml >/dev/null 2>&1
  cur=$(python3 "$S/fx.py")
  if [ "$cur" = "$t" ]; then adb shell input keyevent DPAD_CENTER; echo "OK: $t"; exit 0; fi
  adb shell input keyevent DPAD_$d; sleep 0.7
done; echo "BRAK: $t (ostatni fokus: $cur)"; exit 1
