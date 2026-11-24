#!/usr/bin/env python3
"""framestats_faza.py <plik dumpsys gfxinfo … framestats> : mediana / p90 czasu klatki w fazach (E1, 24.11).
UI = IntendedVsync→SyncQueued (wejście, animacje, pomiar/układ, zapis rysowania), RT = SyncStart→SwapBuffers
(synchronizacja i wydanie poleceń), GPU = SwapBuffers→GpuCompleted (gdy jest), całość = IntendedVsync→FrameCompleted."""
import sys
rows, head = [], None
for l in open(sys.argv[1], encoding="utf-8", errors="ignore"):
    l = l.strip()
    if l.startswith("Flags,"):
        head = l.rstrip(",").split(","); continue
    if head and l and l[0].isdigit() and l.count(",") >= len(head) - 1:
        v = l.rstrip(",").split(",")
        try:
            r = dict(zip(head, map(int, v)))
        except ValueError:
            continue
        if r.get("Flags", 0) == 0:
            rows.append(r)
def q(xs, p):
    xs = sorted(xs); return xs[min(len(xs) - 1, int(round(p / 100 * (len(xs) - 1))))] if xs else None
ph = {
    "UI (wejście→zapis)": lambda r: r["SyncQueued"] - r["IntendedVsync"],
    "czekanie na RT": lambda r: r["SyncStart"] - r["SyncQueued"],
    "RT (sync+polecenia)": lambda r: r["SwapBuffers"] - r["SyncStart"],
    "dequeueBuffer": lambda r: r.get("DequeueBufferDuration", 0),
    "GPU (swap→gotowe)": lambda r: (r["GpuCompleted"] - r["SwapBuffers"]) if r.get("GpuCompleted", 0) > 0 else None,
    "całość": lambda r: r["FrameCompleted"] - r["IntendedVsync"],
}
print(f"klatek: {len(rows)}")
for k, f in ph.items():
    xs = [x / 1e6 for x in (f(r) for r in rows) if x is not None and x >= 0]
    if xs:
        print(f"{k:22s} mediana {q(xs, 50):6.1f} ms   p90 {q(xs, 90):6.1f} ms")
