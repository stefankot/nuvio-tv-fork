#!/bin/bash
# k.sh klawisze... : up down left right ok back menu home ; liczby = pauza w s
export ANDROID_SERIAL=192.168.31.252:5555
for a in "$@"; do case $a in
 up) adb shell input keyevent 19;; down) adb shell input keyevent 20;; left) adb shell input keyevent 21;; right) adb shell input keyevent 22;;
 ok) adb shell input keyevent 23;; back) adb shell input keyevent 4;; menu) adb shell input keyevent 82;; home) adb shell input keyevent 3;;
 long) adb shell input keyevent --longpress 23;; [0-9]*) sleep $a;; *) adb shell input keyevent $a;; esac; done
