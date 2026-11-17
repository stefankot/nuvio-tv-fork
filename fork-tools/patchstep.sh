#!/bin/bash
# Użycie: patchstep.sh begin <plik>   (zapamiętuje stan)
#         patchstep.sh end <plik> <seria> <NN-opis>  (zapisuje łatkę, rejestruje plik jako łatany, zdejmuje z overrides.txt)
cd "$(dirname "$0")/.." || exit 2
S=/tmp/patchstep; mkdir -p $S; k=$(echo "$2" | tr '/' '_')
if [ "$1" = begin ]; then cp "$2" "$S/$k"; exit 0; fi
mkdir -p fork-tools/patches/$3
diff -u "$S/$k" "$2" | sed "1s|.*|--- a|;2s|.*|+++ b|" > fork-tools/patches/$3/$4.patch
grep -q "^$2	" fork-tools/patched-overrides.txt || printf '%s\t%s\n' "$2" "$3" >> fork-tools/patched-overrides.txt
grep -vxF "$2" fork-tools/overrides.txt > /tmp/ov && cp /tmp/ov fork-tools/overrides.txt
