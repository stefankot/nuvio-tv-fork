"""Wyniki testów JVM: python3 -I fork-tools/test/junit_wyniki.py [katalog]
Czyta app/build/test-results/testFullDebugUnitTest/*.xml, wypisuje liczby i nieudane testy z pierwszą linią błędu."""
import glob, os, sys
import xml.etree.ElementTree as ET
root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
d = sys.argv[1] if len(sys.argv) > 1 else os.path.join(root, "app/build/test-results/testFullDebugUnitTest")
tests = fails = skipped = 0
bad = []
for f in sorted(glob.glob(os.path.join(d, "*.xml"))):
    s = ET.parse(f).getroot()
    tests += int(s.get("tests", 0)); skipped += int(s.get("skipped", 0))
    for c in s.iter("testcase"):
        for e in list(c.findall("failure")) + list(c.findall("error")):
            fails += 1
            msg = (e.get("message") or e.text or "").strip().splitlines()
            bad.append(f"{c.get('classname')} > {c.get('name')}: {(msg[0] if msg else '')[:220]}")
print(f"testy: {tests}, nieudane: {fails}, pominięte: {skipped}")
for b in bad: print("  NIE:", b)
