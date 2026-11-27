#!/bin/bash
adb shell rm -f /sdcard/u.xml; rm -f /tmp/u.xml; adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb pull /sdcard/u.xml /tmp/u.xml >/dev/null 2>&1  # 26.11: bez starego zrzutu, gdy nowy się nie uda
python3 - <<'P'
import re;x=open('/tmp/u.xml').read()
nodes=[m.group(0) for m in re.finditer(r'<node [^>]*>',x)]
for i,n in enumerate(nodes):
  if 'focused="true"' in n:
    b=re.search(r'bounds="([^"]*)"',n).group(1)
    # teksty wewnątrz obszaru fokusu
    bb=list(map(int,re.findall(r'\d+',b))); txt=[]
    for m in nodes:
      t=re.search(r' text="([^"]*)"',m).group(1); c=list(map(int,re.findall(r'\d+',re.search(r'bounds="([^"]*)"',m).group(1))))
      if t and c[0]>=bb[0] and c[2]<=bb[2] and c[1]>=bb[1] and c[3]<=bb[3]: txt.append(t)
    print('FOKUS',b,' | '.join(txt)[:200])
P
