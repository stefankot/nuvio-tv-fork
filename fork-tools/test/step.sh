#!/bin/bash
# step.sh PAKIET KIERUNEK N : po każdym kroku fokus + 90/99 percentyl klatek tego kroku
P=$1; D=$2; N=${3:-5}; S=$(cd "$(dirname "$0")" && pwd)
for i in $(seq 1 $N); do adb shell dumpsys gfxinfo $P reset >/dev/null; adb shell input keyevent DPAD_$D; sleep 3
  g=$(adb shell dumpsys gfxinfo $P | grep -E "Total frames|^90th|^99th" | awk -F: '{printf "%s ", $2}'); echo "$(bash "$S/focus.sh" | cut -c1-60) | klatki/p90/p99: $g"; done
