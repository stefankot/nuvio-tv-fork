#!/bin/bash
# jmgo_czekaj_wolny.sh [min_bez_pilota=10] [max_min=180] : czeka, aż na JMGO nikt nie używa pilota przez N minut
# i nic nie gra (żaden odtwarzacz poza ekranem głównym JMGO). Kod 0 = wolny, 1 = limit czasu. Sprawdza co 60 s.
S=$(cd "$(dirname "$0")" && pwd); idle=$(( ${1:-10} * 60000 )); max=$(( ${2:-180} * 60 ))
eval "$(bash "$S/jmgo_adb.sh" --env)"; start=$(date +%s)
while [ $(( $(date +%s) - start )) -lt $max ]; do
  bash "$S/jmgo_adb.sh" >/dev/null 2>&1
  age=$(adb shell dumpsys input 2>/dev/null | grep -A10 "RecentQueue" | grep -o "age=[0-9]*" | tr -dc '0-9\n' | sort -n | head -1)
  # Grające odtwarzacze bez ekranu głównego JMGO (com.jmgo.arwen trzyma własny AudioTrack — to nie oglądanie; 24.11).
  home=$(adb shell pidof com.jmgo.arwen 2>/dev/null | tr -dc '0-9')
  play=$(adb shell dumpsys audio 2>/dev/null | grep "AudioPlaybackConfiguration piid" | grep "state:started" | grep -v "u/pid:[0-9]*/${home:-brak} " | wc -l | tr -dc 0-9)
  if [ -n "$age" ] && [ "$age" -ge "$idle" ] && [ "${play:-1}" = "0" ]; then echo "WOLNY: pilot nieużywany $((age/60000)) min, nic nie gra"; exit 0; fi
  sleep 60
done; echo "LIMIT: JMGO zajęty przez $(( max / 60 )) min"; exit 1
