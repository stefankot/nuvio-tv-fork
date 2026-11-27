#!/bin/bash
# zrzut.sh NAZWA : zrzut ekranu JMGO (960 px) do plan/ux/przeglad-ekranow/NAZWA.png, bez naciskania klawiszy
export ANDROID_SERIAL=${ANDROID_SERIAL:-192.168.31.252:5555}
D="$(cd "$(dirname "$0")" && pwd)"; N=${1:-$(date +%H%M%S)}
adb shell screencap -p /sdcard/pe.png >/dev/null 2>&1 && adb pull /sdcard/pe.png "$D/$N.png" >/dev/null && sips -Z 960 "$D/$N.png" >/dev/null 2>&1 && echo "$D/$N.png"
