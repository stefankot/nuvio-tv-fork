#!/bin/bash
# txt.sh : teksty i fokus z ekranu (uiautomator)
export ANDROID_SERIAL=192.168.31.252:5555
adb shell uiautomator dump /sdcard/ke_ui.xml >/dev/null 2>&1
adb exec-out cat /sdcard/ke_ui.xml | python3 -c '
import sys,re
x=sys.stdin.read()
out=[]
for m in re.finditer(r"<node [^>]*>",x):
    n=m.group(0)
    t=(re.search(r"text=\"([^\"]*)\"",n) or [0,""])[1]
    d=(re.search(r"content-desc=\"([^\"]*)\"",n) or [0,""])[1]
    f="focused=\"true\"" in n
    b=(re.search(r"bounds=\"([^\"]*)\"",n) or [0,""])[1]
    if t or d or f:
        out.append(("[F] " if f else "")+(t or "")+((" {"+d+"}") if d else "")+(" "+b if f else ""))
print(" | ".join(out)[:int(sys.argv[1]) if len(sys.argv)>1 else 3000])
' "$@"
