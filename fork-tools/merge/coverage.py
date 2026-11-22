"""Które pliki forka nie mają żadnego testu JVM (test nie wspomina żadnej nazwy z pliku). python3 -I coverage.py"""
import os, re, sys
ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
src = os.path.join(ROOT, "app/src/main/java/com/nuvio/tv")
tst = os.path.join(ROOT, "app/src/test/java")
tests = ""
for d, _, fs in os.walk(tst):
    for f in fs:
        if f.endswith(".kt"): tests += open(os.path.join(d, f), encoding="utf-8", errors="ignore").read()
rows = []
for part in ["fork", "skin/niunia", "ainarrator", "skin/api"]:
    for d, _, fs in os.walk(os.path.join(src, part)):
        for f in sorted(fs):
            if not f.endswith(".kt"): continue
            p = os.path.join(d, f); s = open(p, encoding="utf-8", errors="ignore").read()
            names = set(re.findall(r"^(?:internal |public )?(?:data |enum |sealed )?(?:object|class|interface) (\w+)", s, re.M))
            names |= set(re.findall(r"^(?:internal |public )?fun (?:[\w.<>]+\.)?(\w+)\(", s, re.M))
            comp = len(re.findall(r"@Composable", s))
            hit = [n for n in names if re.search(r"\b" + re.escape(n) + r"\b", tests)]
            rows.append((os.path.relpath(p, src), len(s.splitlines()), comp, bool(hit)))
cov = sum(1 for r in rows if r[3]); print(f"pliki: {len(rows)}, z testem: {cov}")
for r in rows:
    if not r[3]: print(f"BRAK {r[0]} linie={r[1]} composable={r[2]}")
