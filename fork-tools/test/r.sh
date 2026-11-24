#!/bin/bash
# r.sh NAZWA [KLAWISZE...] : test ręczny — naciska klawisze (np. DPAD_DOWN, ENTER, BACK, 3xDPAD_RIGHT, L:ENTER = długie OK),
# odczekuje i robi zrzut 960 px do plan/ux/reczne/NAZWA.png. Urządzenie: ANDROID_SERIAL (domyślnie JMGO).
export ANDROID_SERIAL=${ANDROID_SERIAL:-192.168.31.252:5555}
D="$(cd "$(dirname "$0")/../.." && pwd)/plan/ux/reczne"; mkdir -p "$D"; N=$1; shift
for k in "$@"; do
  case $k in
    L:*) adb shell input keyevent --longpress ${k#L:} ;;
    W:*) sleep ${k#W:} ;;
    *x*) c=${k%%x*}; for i in $(seq 1 $c); do adb shell input keyevent ${k#*x}; sleep 0.25; done ;;
    *) adb shell input keyevent $k ;;
  esac; sleep 0.35
done
sleep ${R_WAIT:-1.2}
adb shell screencap -p /sdcard/r.png >/dev/null 2>&1; adb pull /sdcard/r.png "$D/$N.png" >/dev/null && sips -Z 960 "$D/$N.png" >/dev/null 2>&1 && echo "$D/$N.png"
