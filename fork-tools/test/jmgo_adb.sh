#!/bin/bash
# jmgo_adb.sh [polecenie adb...] — JMGO przez osobny serwer adb na porcie 5038.
# Powód (23.11): główny serwer adb (port 5037, uruchamiany razem z emulatorem) potrafi zwracać
# „No route to host” dla 192.168.31.252, choć port 5555 projektora jest otwarty. Osobny serwer nie łączy się
# z emulatorem (ADB_EMU=0, ADB_LOCAL_TRANSPORT_MAX_PORT=5553), więc nie przeszkadza innym sesjom.
# Użycie: bash fork-tools/test/jmgo_adb.sh shell dumpsys window | grep mCurrentFocus
#         eval "$(bash fork-tools/test/jmgo_adb.sh --env)"   # potem zwykłe `adb` i skrypty fork-tools/test/*.sh idą na JMGO
export ANDROID_ADB_SERVER_PORT=5038 ADB_EMU=0 ADB_LOCAL_TRANSPORT_MAX_PORT=5553 ANDROID_SERIAL=192.168.31.252:5555
if [ "${1:-}" = "--env" ]; then
  echo "export ANDROID_ADB_SERVER_PORT=5038 ADB_EMU=0 ADB_LOCAL_TRANSPORT_MAX_PORT=5553 ANDROID_SERIAL=192.168.31.252:5555"
  exit 0
fi
adb devices | grep -q "^192.168.31.252:5555[[:space:]]*device" || adb connect 192.168.31.252:5555 >/dev/null
[ $# -gt 0 ] && exec adb "$@"
adb devices
