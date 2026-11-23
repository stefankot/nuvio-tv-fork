#!/bin/bash
# jmgo_instaluj.sh <apk> : instalacja forka na JMGO (ZASADY §5) — w tym samym kroku sprawdza, czy nikt nie ogląda,
# potem md5 (adb pull) i `cmd package compile -m speed -f`. Przez osobny serwer adb (jmgo_adb.sh, port 5038).
# Przerywa (kod 3), gdy cokolwiek teraz gra (wyjście dźwięku nie w uśpieniu). Fork na wierzchu bez dźwięku
# (np. film wstrzymany po teście, wygaszacz) — najpierw HOME, potem instalacja.
set -u
APK="${1:?podaj plik apk}"
S=$(cd "$(dirname "$0")" && pwd)
eval "$(bash "$S/jmgo_adb.sh" --env)"
bash "$S/jmgo_adb.sh" >/dev/null
PKG=com.nuviodebug.com
top=$(adb shell dumpsys window | grep -m1 mCurrentFocus | tr -d '\r')
echo "wierzch: $top"
echo "ostatni dźwięk: $(adb shell dumpsys audio | grep -E "player piid:[0-9]+ state:" | tail -1 | tr -d '\r')"
# tylko czynne wyjścia (wiersze „- …” to historia zamkniętych wyjść)
awake=$(adb shell dumpsys media.audio_flinger | grep -c "^  Standby: no" | tr -d ' \r')
if [ "${awake:-0}" != "0" ]; then echo "PRZERWANE: coś teraz gra (wyjście dźwięku aktywne)"; exit 3; fi
if echo "$top" | grep -q "$PKG"; then echo "fork na wierzchu, nic nie gra — HOME"; adb shell input keyevent HOME; sleep 2; fi
adb install -r "$APK" | tail -1
P=$(adb shell pm path $PKG | sed 's/package://' | tr -d '\r')
tmp=$(mktemp -d); adb pull "$P" "$tmp/i.apk" >/dev/null
a=$(md5 -q "$APK"); b=$(md5 -q "$tmp/i.apk"); rm -rf "$tmp"
echo "md5 apk $a / na TV $b"; [ "$a" = "$b" ] || { echo "md5 NIEZGODNE"; exit 2; }
adb shell cmd package compile -m speed -f $PKG | tail -1
echo "OK: zainstalowane"
