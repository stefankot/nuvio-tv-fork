#!/bin/bash
# kf.sh KLAWISZ [KLAWISZ...]: naciska klawisze (0,9 s przerwy) i po każdym wypisuje ostatnie logi Fork (fokus, iptv, odtwarzacz) od tego klawisza.
for k in "$@"; do
  t=$(adb shell "date '+%m-%d %H:%M:%S.000'" | tr -d '\r')
  adb shell input keyevent "$k"; sleep "${KF_WAIT:-0.9}"
  echo "== $k"; adb logcat -d -v time -T "$t" 2>/dev/null | grep -E "ForkFocus|ForkNiunia|ForkLive" | grep -v "): key=" | sed -E 's/^[0-9-]+ ([0-9:.]+) [A-Z]\/([A-Za-z]+)\([ 0-9]+\): /\1 \2: /' | cut -c1-170 | tail -4
done
