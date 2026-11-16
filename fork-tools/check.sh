#!/bin/bash
# Test spójności forka. Uruchom z katalogu NuvioFork: bash fork-tools/check.sh
cd "$(dirname "$0")/.." || exit 2
. fork-tools/pins.env
UP=../upstream/reshaped; BASE=85f9010aa; fail=0
bad(){ echo "BŁĄD: $*"; fail=1; }
[ "$(git -C $UP rev-parse --short=9 HEAD)" = "$RESHAPED" ] || bad "klon Reshaped nie jest na $RESHAPED"
# (a) pliki 'override' identyczne z Reshaped
while read -r f; do [ -n "$f" ] && { cmp -s "$f" "$UP/$f" || bad "override różni się od Reshaped: $f"; }; done < fork-tools/overrides.txt
# (b) pliki z hookami: wolno tylko DODAWAĆ linie względem Reshaped
while read -r f; do [ -n "$f" ] || continue
  diff "$UP/$f" "$f" | grep -q '^<' && bad "hook-plik usuwa/zmienia kod Reshaped: $f"; done < fork-tools/hooked-overrides.txt
# (c) markery z hooks.tsv obecne
tail -n +2 fork-tools/hooks.tsv | while IFS=$'\t' read -r f m a mod p; do grep -qF -- "$m" "$f" || { echo "BŁĄD: brak markera '$m' w $f"; exit 1; }; done || fail=1
# (d) limit plików utrzymywanych ręcznie
n=$(cat fork-tools/hooked-overrides.txt fork-tools/base-hooks.txt | grep -c .); [ "$n" -le 35 ] || bad "za dużo plików z hookami: $n"
# (e) zmodyfikowany plik bazy spoza list
for f in $(git diff --diff-filter=M --name-only $BASE; git diff --name-only --diff-filter=M $BASE HEAD); do
  grep -qxF "$f" fork-tools/overrides.txt fork-tools/hooked-overrides.txt fork-tools/base-hooks.txt || bad "zmodyfikowany plik poza listami: $f"; done
# (f) izolacja skin/kit
for d in app/src/main/java/com/nuvio/tv/skin/kit/tokens app/src/main/java/com/nuvio/tv/skin/kit/components; do
  [ -d "$d" ] && grep -rn "import com\.nuvio\.tv\." "$d" | grep -v "import com\.nuvio\.tv\.skin\." && bad "skin/kit importuje aplikację: $d"; done
[ $fail = 0 ] && echo "check.sh: OK (hand-maintained: $n)" || { echo "check.sh: BŁĘDY"; exit 1; }
