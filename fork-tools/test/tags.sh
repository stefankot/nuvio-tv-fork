#!/bin/bash
# tags.sh: elementy z testTag (resource-id) i tekstem, z pozycją
adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb pull /sdcard/u.xml /tmp/u.xml >/dev/null 2>&1
python3 - <<'P'
import re
x=open('/tmp/u.xml').read()
for m in re.finditer(r'<node [^>]*>',x):
    n=m.group(0); t=re.search(r' text="([^"]*)"',n).group(1); d=re.search(r'content-desc="([^"]*)"',n).group(1); tag=re.search(r'resource-id="([^"]*)"',n).group(1); b=re.search(r'bounds="([^"]*)"',n).group(1)
    if t or d or tag: print(b, tag, t[:40], d[:30])
P
