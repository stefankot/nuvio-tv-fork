#!/usr/bin/env python3
"""perfetto_plasterki.py <ślad.perfetto-trace> <pid aplikacji> [od_s do_s] : plasterki atrace (B/E z ftrace print) procesu,
bez trace_processor (E1, 24.11). Wynik: na wątek (główny / RenderThread / inne) — liczba, mediana, p90, suma ms dla nazw
z największą sumą czasu. Dekoduje tylko: Trace.packet(1) → TracePacket.ftrace_events(1) → FtraceEventBundle.event(2) →
FtraceEvent{timestamp(1), pid(2), print(3) → buf(2)}; sched_switch(4) tylko do nazw wątków (comm)."""
import re, sys
from collections import defaultdict


def varint(b, i):
    r = s = 0
    while True:
        c = b[i]; i += 1; r |= (c & 0x7F) << s; s += 7
        if c < 0x80:
            return r, i


def fields(b, i=0, n=None):
    n = len(b) if n is None else n
    while i < n:
        k, i = varint(b, i); f, w = k >> 3, k & 7
        if w == 0:
            v, i = varint(b, i); yield f, v
        elif w == 2:
            l, i = varint(b, i); yield f, b[i:i + l]; i += l
        elif w == 1:
            yield f, b[i:i + 8]; i += 8
        elif w == 5:
            yield f, b[i:i + 4]; i += 4
        else:
            return


def events(data):
    for f, pkt in fields(data):
        if f != 1 or not isinstance(pkt, bytes):
            continue
        for pf, bundle in fields(pkt):
            if pf != 1 or not isinstance(bundle, bytes):
                continue
            for bf, ev in fields(bundle):
                if bf != 2 or not isinstance(ev, bytes):
                    continue
                ts = tid = None; buf = None; comm = None
                for ef, v in fields(ev):
                    if ef == 1: ts = v
                    elif ef == 2: tid = v
                    elif ef == 3 and isinstance(v, bytes):
                        for xf, xv in fields(v):
                            if xf == 2 and isinstance(xv, bytes): buf = xv.decode("utf-8", "replace")
                    elif ef == 4 and isinstance(v, bytes):  # sched_switch: prev_comm(1), prev_pid(2)
                        pc = pp = None
                        for xf, xv in fields(v):
                            if xf == 1 and isinstance(xv, bytes): pc = xv.decode("utf-8", "replace")
                            elif xf == 2: pp = xv
                        if pc is not None and pp is not None: comm = (pp, pc)
                if buf is not None and ts is not None:
                    yield ts, tid, buf, None
                elif comm is not None:
                    yield ts, None, None, comm


def q(xs, p):
    xs = sorted(xs); return xs[min(len(xs) - 1, int(round(p / 100 * (len(xs) - 1))))] if xs else 0


def main():
    path, pid = sys.argv[1], int(sys.argv[2])
    lo = float(sys.argv[3]) if len(sys.argv) > 3 else None; hi = float(sys.argv[4]) if len(sys.argv) > 4 else None
    data = open(path, "rb").read()
    names, stacks, durs, t0 = {}, defaultdict(list), defaultdict(list), None
    for ts, tid, buf, comm in events(data):
        if comm:
            names[comm[0]] = comm[1]; continue
        t0 = ts if t0 is None else t0
        rel = (ts - t0) / 1e9
        if (lo is not None and rel < lo) or (hi is not None and rel > hi):
            continue
        for line in buf.split("\n"):
            m = re.match(r"B\|(\d+)\|(.*)", line)
            if m:
                if int(m.group(1)) == pid:  # id i numery na końcu → „…” / „#”, żeby te same czynności sumowały się razem
                    n = re.sub(r" \d+$", " #", re.sub(r"id=0x[0-9a-f]+", "id=…", m.group(2)[:60]))
                    stacks[tid].append((ts, n))
                continue
            if line.startswith("E") and stacks.get(tid):
                s, n = stacks[tid].pop(); durs[(tid, n)].append((ts - s) / 1e6)
    per = defaultdict(list)
    for (tid, n), xs in durs.items():
        per[tid].append((sum(xs), n, xs))
    for tid in sorted(per, key=lambda t: -sum(x[0] for x in per[t])):
        who = "główny" if tid == pid else names.get(tid, "?")
        print(f"\n== wątek {tid} ({who})")
        for tot, n, xs in sorted(per[tid], reverse=True)[:14]:
            print(f"  {n[:48]:48s} n={len(xs):5d}  med {q(xs, 50):7.2f}  p90 {q(xs, 90):7.2f}  suma {tot:8.0f} ms")


if __name__ == "__main__":
    main()
