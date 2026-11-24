#!/bin/bash
# str.sh prefix [maxchars]: wartości stringów KE o prefiksie, skrócone
F="/Users/milajovovich/Desktop/nuvio 2/upstream/ke-1.2.2/app/src/main/res/values/strings.xml"
grep -o "<string name=\"$1[^\"]*\"[^>]*>[^<]*" "$F" | sed 's/<string name="\([^"]*\)"[^>]*>/\1=/' | sed "s/^$1//" | cut -c1-90 | tr '\n' ';' | head -c ${2:-4000}; echo
