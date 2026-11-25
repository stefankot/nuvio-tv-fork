#!/usr/bin/env python3
"""dmtrace_top.py <plik.trace> [wątek=main] [filtr=nuvio] [n=40]: czas łączny (inclusive, ms) metod z `am profile start --sampling`
(format dmtrace v3, zegar dual/wall). Liczy tylko wybrany wątek; rekurencja liczona raz."""
import struct, sys
from collections import defaultdict
path = sys.argv[1]; want = sys.argv[2] if len(sys.argv) > 2 else 'main'
flt = sys.argv[3] if len(sys.argv) > 3 else 'nuvio'; n = int(sys.argv[4]) if len(sys.argv) > 4 else 40
b = open(path, 'rb').read()
end = b.index(b'*end\n') + 5
head = b[:end].decode('utf-8', 'replace').split('\n')
threads, methods, sec, clock = {}, {}, None, 'dual'
for l in head:
    if l.startswith('*'): sec = l[1:]; continue
    if sec == 'version' and l.startswith('clock='): clock = l[6:]
    elif sec == 'threads' and l:
        t, name = l.split('\t', 1); threads[int(t)] = name
    elif sec == 'methods' and l:
        p = l.split('\t'); methods[int(p[0], 16)] = p[1] + '.' + p[2]
magic, ver, off, start = struct.unpack_from('<IHHQ', b, end)
rec = struct.unpack_from('<H', b, end + 16)[0] if ver >= 3 else (10 if clock == 'dual' else 6)
tid_main = [t for t, nm in threads.items() if nm == want] or [min(threads)]
tid = tid_main[0]
i = end + off; stack = []; incl = defaultdict(int); total = 0; t_first = None; t_last = 0
while i + rec <= len(b):
    th, mv = struct.unpack_from('<HI', b, i)
    if clock == 'dual': wall = struct.unpack_from('<I', b, i + 10)[0]
    else: wall = struct.unpack_from('<I', b, i + 6)[0]
    i += rec
    if th != tid: continue
    m, act = mv & ~3, mv & 3
    t_first = wall if t_first is None else t_first; t_last = wall
    if act == 0: stack.append((m, wall))
    else:
        while stack:
            mm, t0 = stack.pop()
            if not any(s[0] == mm for s in stack): incl[mm] += wall - t0
            if mm == m: break
print(f'wątek {threads.get(tid)} ({tid}), okno {(t_last - (t_first or 0)) / 1e6:.1f} s')
rows = sorted(((v, methods.get(k, hex(k))) for k, v in incl.items() if flt in methods.get(k, '').lower() or flt == '*'), reverse=True)
for v, nm in rows[:n]: print(f'{v / 1000:8.0f} ms  {nm.replace("com.nuvio.tv.", "")[:150]}')
