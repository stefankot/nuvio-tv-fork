#!/bin/bash
# Test spójności forka. Uruchom z katalogu NuvioFork: bash fork-tools/check.sh
cd "$(dirname "$0")/.." || exit 2
. fork-tools/pins.env
UP=../upstream/reshaped; BASE=9174319d2; fail=0
bad(){ echo "BŁĄD: $*"; fail=1; }
# Seria "kicia" (skórka Kicia): jedna łatka na plik, fork-tools/patches/kicia/<ścieżka z _>.patch, nakładana jako ostatnia.
# unkicia <plik> -> ścieżka do kopii z cofniętą łatką Kicii (albo sam plik, gdy łatki nie ma).
unkicia(){ local f="$1" n pf; n=$(echo "${f#app/src/main/java/com/nuvio/tv/}" | tr / _); pf="fork-tools/patches/kicia/$n.patch"
  if [ -f "$pf" ]; then local tmp; tmp=$(mktemp); cp "$f" "$tmp"; patch -R -s "$tmp" < "$pf" >/dev/null 2>&1 || { echo "BŁĄD: łatka Kicii nie cofa się: $pf" >&2; }; echo "$tmp"; else echo "$f"; fi; }
[ "$(git -C $UP rev-parse --short=9 HEAD)" = "$RESHAPED" ] || bad "klon Reshaped nie jest na $RESHAPED"
# (a) pliki 'override' identyczne z Reshaped
while read -r f; do [ -n "$f" ] && { cmp -s "$(unkicia "$f")" "$UP/$f" || bad "override różni się od Reshaped: $f"; }; done < fork-tools/overrides.txt
# (b) pliki z hookami: wolno tylko DODAWAĆ linie względem Reshaped
while read -r f; do [ -n "$f" ] || continue
  diff "$UP/$f" "$(unkicia "$f")" | grep '^<' | sed 's/^< //' | while IFS= read -r l; do
    t=$(echo "$l" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    awk -F'\t' -v f="$f" -v t="$t" 'NR>1 && $1==f { o=$6; gsub(/^[ \t]+|[ \t]+$/,"",o); if (o==t) found=1 } END{exit found?0:1}' fork-tools/hooks.tsv \
      || { echo "BŁĄD: hook-plik zmienia kod Reshaped ($f): $t"; exit 1; }
  done || fail=1
done < fork-tools/hooked-overrides.txt
# (c) markery z hooks.tsv obecne
tail -n +2 fork-tools/hooks.tsv | while IFS=$'\t' read -r f m a mod p; do grep -qF -- "$m" "$f" || { echo "BŁĄD: brak markera '$m' w $f"; exit 1; }; done || fail=1
# (d2) pliki łatane: po odwróceniu łatek serii (fork-tools/patches/<seria>/NN-*.patch) i usunięciu linii "Fork hook:" plik == Reshaped
PATCHED=${PATCHED_LIST:-fork-tools/patched-overrides.txt}; PUP=${PATCHED_UP:-$UP}
if [ -s "$PATCHED" ]; then
  while IFS=$'\t' read -r f series; do [ -n "$f" ] || continue
    tmp=$(mktemp); cp "$(unkicia "$f")" "$tmp"
    for pf in $(ls -r fork-tools/patches/$series/*.patch 2>/dev/null); do patch -R -s "$tmp" < "$pf" >/dev/null 2>&1 || { echo "BŁĄD: łatka nie cofa się: $pf"; fail=1; }; done
    if grep -q 'Fork hook:' "$tmp"; then grep -v 'Fork hook:' "$tmp" > "$tmp.2"; else cp "$tmp" "$tmp.2"; fi # grep -v dopisałby znak nowej linii na końcu pliku
    cmp -s "$tmp.2" "$PUP/$f" || { echo "BŁĄD: plik łatany różni się od Reshaped poza łatkami: $f"; fail=1; }
    rm -f "$tmp" "$tmp.2"
  done < "$PATCHED"
fi
# (d) limit plików utrzymywanych ręcznie
n=$(cat fork-tools/hooked-overrides.txt fork-tools/base-hooks.txt; cut -f1 "$PATCHED" 2>/dev/null) ; n=$(echo "$n" | grep -c .); [ "$n" -le 65 ] || bad "za dużo plików z hookami: $n"
# (e) zmodyfikowany plik bazy spoza list
for f in $(git diff --diff-filter=M --name-only $BASE; git diff --name-only --diff-filter=M $BASE HEAD); do
  grep -qxF "$f" fork-tools/overrides.txt fork-tools/hooked-overrides.txt fork-tools/base-hooks.txt || cut -f1 "$PATCHED" 2>/dev/null | grep -qxF "$f" || bad "zmodyfikowany plik poza listami: $f"; done
# (f) izolacja skin/kit
for d in app/src/main/java/com/nuvio/tv/skin/kit/tokens app/src/main/java/com/nuvio/tv/skin/kit/components; do
  [ -d "$d" ] && grep -rn "import com\.nuvio\.tv\." "$d" | grep -v "import com\.nuvio\.tv\.skin\." && bad "skin/kit importuje aplikację: $d"; done
[ $fail = 0 ] && echo "check.sh: OK (hand-maintained: $n)" || { echo "check.sh: BŁĘDY"; exit 1; }
