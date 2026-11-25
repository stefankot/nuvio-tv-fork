#!/bin/bash
# Buduje skrot-betternuvio.apk bez Gradle (aapt2 + javac + d8 + apksigner), kilka sekund.
set -e
cd "$(dirname "$0")"
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
SDK=~/Library/Android/sdk; BT=$SDK/build-tools/36.0.0; JAR=$SDK/platforms/android-36/android.jar
O=build; rm -rf $O; mkdir -p $O/classes $O/dex
$BT/aapt2 compile --dir res -o $O/res.zip
$BT/aapt2 link -I $JAR --manifest AndroidManifest.xml -o $O/base.apk $O/res.zip
$JAVA_HOME/bin/javac --release 11 -classpath $JAR -d $O/classes $(find src -name '*.java')
$BT/d8 --release --lib $JAR --min-api 26 --output $O/dex $(find $O/classes -name '*.class')
(cd $O/dex && zip -q ../base.apk classes.dex)
$BT/zipalign -f 4 $O/base.apk $O/aligned.apk
$BT/apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android --out skrot-betternuvio.apk $O/aligned.apk
rm -rf $O
ls -l skrot-betternuvio.apk
