#!/usr/bin/env python3
"""klawisz.py TEKST : wpisuje TEKST na klawiaturze ekranowej (Niunia Live TV → Szukaj) — fokus strzałkami do klawisza, potem OK.
Klawisze rozpoznaje po tekście węzła (uiautomator). Urządzenie z ANDROID_SERIAL."""
import re
import subprocess
import sys


def dump():
    subprocess.run("adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb pull /sdcard/u.xml /tmp/u.xml >/dev/null 2>&1", shell=True)
    return open('/tmp/u.xml').read()


def box(n):
    b = list(map(int, re.findall(r'\d+', re.search(r'bounds="([^"]*)"', n).group(1))))
    return ((b[0] + b[2]) / 2, (b[1] + b[3]) / 2, b)


def key(code):
    subprocess.run(['adb', 'shell', 'input', 'keyevent', code])


def press(letter):
    for _ in range(25):
        x = dump()
        nodes = re.findall(r'<node [^>]*>', x)
        foc = [n for n in nodes if 'focused="true"' in n]
        tgt = [n for n in nodes if re.search(r' text="%s"' % re.escape(letter), n)]
        if not foc or not tgt:
            print('BRAK fokusu albo klawisza', letter); return False
        fx, fy, fb = box(foc[-1])
        tx, ty, _ = box(tgt[0])
        if fb[0] <= tx <= fb[2] and fb[1] <= ty <= fb[3]:
            key('DPAD_CENTER'); return True
        dx, dy = tx - fx, ty - fy
        key(('DPAD_RIGHT' if dx > 0 else 'DPAD_LEFT') if abs(dx) > abs(dy) else ('DPAD_DOWN' if dy > 0 else 'DPAD_UP'))
    print('nie doszedłem do', letter); return False


if __name__ == '__main__':
    for ch in sys.argv[1]:
        if not press(ch):
            sys.exit(1)
    print('wpisane:', sys.argv[1])
