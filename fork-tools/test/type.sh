#!/bin/bash
# type.sh słowo: klawiatura Kicii 6 kolumn a..z; liczy wiersz/kolumnę i naciska strzałki
cur(){ bash "/Users/milajovovich/Desktop/nuvio 2/NuvioFork/fork-tools/test/focus.sh" | awk '{print $NF}'; }
w="$1"
for i in $(seq 1 8); do c=$(cur); [[ "$c" =~ ^[a-z]$ ]] && break; adb shell input keyevent DPAD_LEFT; sleep 0.5; done
for ((k=0;k<${#w};k++)); do t=${w:$k:1}; c=$(cur)
  ci=$(( $(printf %d "'$c") - 97 )); ti=$(( $(printf %d "'$t") - 97 ))
  dr=$(( ti/6 - ci/6 )); dc=$(( ti%6 - ci%6 ))
  while [ $dr -gt 0 ]; do adb shell input keyevent DPAD_DOWN; dr=$((dr-1)); sleep 0.3; done
  while [ $dr -lt 0 ]; do adb shell input keyevent DPAD_UP; dr=$((dr+1)); sleep 0.3; done
  while [ $dc -gt 0 ]; do adb shell input keyevent DPAD_RIGHT; dc=$((dc-1)); sleep 0.3; done
  while [ $dc -lt 0 ]; do adb shell input keyevent DPAD_LEFT; dc=$((dc+1)); sleep 0.3; done
  sleep 0.3; c=$(cur); [ "$c" = "$t" ] && { adb shell input keyevent DPAD_CENTER; sleep 0.5; echo "ok $t"; } || { echo "zle: $c zamiast $t"; exit 1; }
done
