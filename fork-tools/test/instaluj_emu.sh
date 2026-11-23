#!/bin/bash
# Instalacja APK ux4 na emulatorze (arm64) z porównaniem md5 i kompilacją speed. Użycie: bash fork-tools/test/instaluj_emu.sh <apk> (urządzenie: ANDROID_SERIAL, domyślnie emulator-5554)
set -u
export ANDROID_SERIAL=${ANDROID_SERIAL:-emulator-5554}
APK="${1:?podaj apk}"
PKG=com.nuviodebug.com
adb install -r "$APK" | tail -1
path=$(adb shell pm path $PKG | head -1 | sed 's/package://' | tr -d '\r')
adb pull "$path" /tmp/ux_pulled.apk >/dev/null
a=$(md5 -q "$APK"); b=$(md5 -q /tmp/ux_pulled.apk)
echo "md5 build:  $a"; echo "md5 urządz: $b"
[ "$a" = "$b" ] && echo "md5 ZGODNY" || echo "md5 RÓŻNY"
adb shell cmd package compile -m speed -f $PKG | tail -1
