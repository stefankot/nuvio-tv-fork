#!/bin/bash
# fx2.sh: fokus + teksty ekranu (odporne na „>” w tekstach — ElementTree). Użycie: fx2.sh [ile_znakow]
adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb pull /sdcard/u.xml /tmp/u.xml >/dev/null 2>&1
python3 - "${1:-0}" <<'P'
import sys,xml.etree.ElementTree as E
r=E.parse('/tmp/u.xml').getroot(); ts=[]; f=None
for n in r.iter('node'):
  t=n.get('text') or ''
  if n.get('focused')=='true': f=(n.get('bounds'),t or n.get('content-desc'))
  if t: ts.append(t)
print('FOKUS',f)
k=int(sys.argv[1])
if k: print(' | '.join(ts)[:k])
P
