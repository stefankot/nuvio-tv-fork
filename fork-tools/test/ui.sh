#!/bin/bash
# ui.sh: zrzut tekstów ekranu (text, content-desc, focused)
adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb pull /sdcard/u.xml /tmp/u.xml >/dev/null 2>&1
python3 - <<'P'
import re
x=open('/tmp/u.xml').read()
out=[]
for m in re.finditer(r'<node [^>]*>',x):
    n=m.group(0)
    t=re.search(r' text="([^"]*)"',n).group(1); d=re.search(r'content-desc="([^"]*)"',n).group(1)
    f=' focused="true"' in n
    if t or d: out.append(('*' if f else ' ')+(t or '')+((' ['+d+']') if d else ''))
print(' | '.join(out)[:2500])
P
