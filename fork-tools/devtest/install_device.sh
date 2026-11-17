#!/bin/bash
# Użycie: jmgo_install.sh <ip[:port]|serial>   (instalacja forka na JMGO i test tekstowy od instalacji)
A=~/Library/Android/sdk/platform-tools/adb
D="$1"; case "$D" in *.*.*.*) $A connect "$D" ;; esac
S="-s $D"
echo "model: $($A $S shell getprop ro.product.model) / $($A $S shell getprop ro.product.manufacturer) / android $($A $S shell getprop ro.build.version.release) sdk $($A $S shell getprop ro.build.version.sdk)"
ABIS=$($A $S shell getprop ro.product.cpu.abilist); echo "abi: $ABIS"
case "$ABIS" in *arm64-v8a*) APK=app-full-arm64-v8a-debug.apk;; *armeabi-v7a*) APK=app-full-armeabi-v7a-debug.apk;; *x86_64*) APK=app-full-x86_64-debug.apk;; *) APK=app-full-universal-debug.apk;; esac
cd "/Users/milajovovich/Desktop/nuvio 2/NuvioFork/app/build/outputs/apk/full/debug" && ls -la $APK
echo "wolne miejsce: $($A $S shell df /data | tail -1)"
$A $S install -r -d $APK && echo INSTALL_OK
P=$($A $S shell pm path com.nuviodebug.com | head -1 | cut -d: -f2); echo "hash lokalny=$(md5 -q $APK) urzadzenie=$($A $S shell md5sum $P | cut -c1-32)"
$A $S shell dumpsys package com.nuviodebug.com | grep -E "versionName|firstInstallTime|lastUpdateTime|requested permissions" | head -4
