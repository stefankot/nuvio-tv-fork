#!/bin/bash
# Test klasy "łatany": zmiana poza łatką musi wywrócić sprawdzenie. Uruchom z NuvioFork.
cd "$(dirname "$0")/.." || exit 2
T=$(mktemp -d); mkdir -p $T/up/x fork-tools/patches/selftest
printf 'a\nb\nc\n' > $T/up/x/f.txt; mkdir -p $T/fork/x; printf 'a\nB\nc\n' > $T/fork/x/f.txt
(cd $T && diff -u up/x/f.txt fork/x/f.txt | sed 's|up/x/f.txt|f.txt|;s|fork/x/f.txt|f.txt|' > /tmp/selftest.patch)
cp /tmp/selftest.patch fork-tools/patches/selftest/01-b.patch
printf '%s\tselftest\n' "$T/fork/x/f.txt" > $T/list
run(){ PATCHED_LIST=$T/list PATCHED_UP=/ bash -c '
 . fork-tools/pins.env; f="'"$T"'/fork/x/f.txt"; tmp=$(mktemp); cp "$f" "$tmp"
 patch -R -s "$tmp" < fork-tools/patches/selftest/01-b.patch; cmp -s "$tmp" "'"$T"'/up/x/f.txt"'; }
run && echo "OK: czysty plik przechodzi" || { echo "BŁĄD testu: czysty plik nie przeszedł"; exit 1; }
printf 'a\nB\nc\nx\n' > $T/fork/x/f.txt
run && { echo "BŁĄD testu: ręczna zmiana nie wywróciła"; exit 1; } || echo "OK: ręczna zmiana wywraca"
rm -rf fork-tools/patches/selftest $T
