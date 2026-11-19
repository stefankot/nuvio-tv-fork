#!/bin/bash
# cat.sh "Kategoria" UP|DOWN [max]: fokus na kategorię ustawień (content-desc), potem OK
t="$1"; d="$2"; max=${3:-20}
for i in $(seq 1 $max); do
  adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb pull /sdcard/u.xml /tmp/u.xml >/dev/null 2>&1
  cur=$(python3 -c "
import re;x=open('/tmp/u.xml').read()
N=re.findall(r'<node [^>]*>',x)
B=lambda n: list(map(int,re.findall(r'\d+',re.search(r'bounds=\"([^\"]*)\"',n).group(1))))
F=[B(n) for n in N if 'focused=\"true\"' in n]
if F:
  f=F[0]
  for n in N:
    c=B(n); l=re.search(r'content-desc=\"([^\"]*)\"',n).group(1) or re.search(r' text=\"([^\"]*)\"',n).group(1)
    if l and c[0]>=f[0] and c[1]>=f[1] and c[2]<=f[2] and c[3]<=f[3]: print(l); break")
  if [ "$cur" = "$t" ]; then adb shell input keyevent DPAD_CENTER; echo "OK: $t"; exit 0; fi
  adb shell input keyevent DPAD_$d; sleep 0.6
done; echo "BRAK: $t (fokus: $cur)"; exit 1
