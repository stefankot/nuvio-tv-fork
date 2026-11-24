#!/bin/bash
export ANDROID_SERIAL=192.168.31.252:5555
D="/Users/milajovovich/Desktop/nuvio 2/wt-niunia-ke/plan/niunia-ke/zrzuty"
adb shell screencap -p /sdcard/ke_s.png && adb pull -q /sdcard/ke_s.png "$D/$1.png" >/dev/null && sips -Z ${2:-800} "$D/$1.png" --out "$D/$1.jpg" -s format jpeg -s formatOptions 55 >/dev/null 2>&1 && rm "$D/$1.png" && echo "$D/$1.jpg"
