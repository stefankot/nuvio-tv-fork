#!/bin/bash
# Buduje fastdump.jar (czytnik ekranu dla testów UX na JMGO): javac + d8, bez Gradle. Wynik obok: fastdump.jar.
set -eu
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
D="$(cd "$(dirname "$0")" && pwd)"; SDK="$HOME/Library/Android/sdk"
AJ="$SDK/platforms/android-35/android.jar"; D8="$SDK/build-tools/35.0.0/d8"
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
"$JAVA_HOME/bin/javac" --release 8 -Xlint:-options -cp "$AJ" -d "$T/cls" "$D/FastDump.java"
"$D8" --min-api 30 --lib "$AJ" --output "$T/out.jar" $(find "$T/cls" -name '*.class')
cp "$T/out.jar" "$D/fastdump.jar"; ls -l "$D/fastdump.jar"
