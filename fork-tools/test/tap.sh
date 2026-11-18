#!/bin/bash
# tap.sh "tekst" : dotknij pierwszego węzła o tym tekście/opisie (z ostatniego zrzutu odświeżonego teraz)
adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb pull /sdcard/u.xml /tmp/u.xml >/dev/null 2>&1
python3 - "$1" <<'P'
import re,sys,subprocess
x=open('/tmp/u.xml').read(); q=sys.argv[1]
for m in re.finditer(r'<node [^>]*>',x):
    n=m.group(0)
    t=re.search(r' text="([^"]*)"',n).group(1); d=re.search(r'content-desc="([^"]*)"',n).group(1)
    if q==t or q==d:
        b=list(map(int,re.findall(r'\d+',re.search(r'bounds="([^"]*)"',n).group(1))))
        subprocess.run(['adb','shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2)]); print('tap',q,b); break
else: print('BRAK',q)
P
