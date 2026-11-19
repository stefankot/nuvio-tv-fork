#!/usr/bin/env python3
"""Podsumowanie odtwarzania z logcat: co źródło oferowało, co wybrano, czy preferencja języka spełniona,
czas startu, faktyczny dekoder i rozdzielczość obrazu.
Użycie: DEV=<ip:port> python3 playdiag.py [--clear]   (--clear czyści logcat przed testem; potem odtwórz film i uruchom bez flagi)
Wymaga APK z fork/diag/PlaybackDiag.kt (znacznik ForkDiag)."""
import os, re, subprocess, sys

ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
DEV = os.environ.get("DEV", "")


def adb(*a):
    return subprocess.run([ADB] + (["-s", DEV] if DEV else []) + list(a), capture_output=True, text=True).stdout


if "--clear" in sys.argv:
    adb("logcat", "-c")
    print("logcat wyczyszczony — odtwórz film, potem uruchom bez --clear")
    sys.exit()

log = adb("logcat", "-d", "-v", "time")
PAT = re.compile(r"ForkDiag|ForkPerf|MediaCodec.*(init|configure)|CodecInfo|DolbyVision|video.*(\d{3,4}x\d{3,4})|"
                 r"ExoPlayerImpl.*Init|PlaybackException|ERROR_CODE|Unsupported|DecoderInitializationException", re.I)
issues = []
for line in log.splitlines():
    if PAT.search(line):
        print(line[:400])
        m = re.search(r"mismatch=(\S+)", line)
        if m and m.group(1) != "none":
            issues.append(m.group(1))
print("\nPODSUMOWANIE:", "preferencje spełnione" if not issues else "NIESPEŁNIONE: " + ", ".join(sorted(set(issues))))
