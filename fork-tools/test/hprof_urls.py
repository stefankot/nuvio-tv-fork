#!/usr/bin/env python3
"""Kształt adresów kanałów ze zrzutu (bez hosta i loginu): ostatnie znaki ścieżki, liczba segmentów.
  python3 -I hprof_urls.py <plik.hprof> <id_tablicy_hex> [co_ile]"""
import re, struct, sys
from collections import Counter
sys.path.insert(0, __import__('os').path.dirname(__file__))
import hprof_hist as H
h = H.Hprof(sys.argv[1]); h.load_meta()
arr = int(sys.argv[2], 16); step = int(sys.argv[3]) if len(sys.argv) > 3 else 1
for kind, oid, pos, extra, heap in h.heap_objects():
    if oid == arr:
        els = set(struct.unpack_from('>I', h.m, pos + 4 * i)[0] for i in range(0, extra[1], step)); break
f = {}
for kind, oid, pos, extra, heap in h.heap_objects():
    if kind == 'I' and oid in els: f[oid] = dict(h.ref_fields(extra[0], pos))
want = {x['streamUrl'] for x in f.values() if 'streamUrl' in x} | {x['group'] for x in f.values() if 'group' in x}
val = {}
for kind, oid, pos, extra, heap in h.heap_objects():
    if kind == 'I' and oid in want: val[oid] = struct.unpack_from('>I', h.m, pos + 8)[0]
inv = {v: k for k, v in val.items()}; txt = {}
for kind, oid, pos, extra, heap in h.heap_objects():
    if kind == 'P' and oid in inv:
        b = h.m[pos:pos + extra[1] * H.BASIC[extra[0]]]
        txt[inv[oid]] = b.decode('latin-1') if extra[0] == 8 else b.decode('utf-16-be', 'replace')
shape = Counter(); live = []
for x in f.values():
    u = txt.get(x.get('streamUrl'), ''); g = txt.get(x.get('group'), '')
    p = re.sub(r'^[a-z]+://[^/]+', '', u.split('?')[0])
    segs = [s for s in p.split('/') if s]
    first = segs[0] if segs else ''
    ext = re.search(r'\.([A-Za-z0-9]{2,5})$', p)
    shape[(first if first in ('live', 'movie', 'series', 'play', 'timeshift') else '*', len(segs), ext.group(1).lower() if ext else '-')] += 1
    if not ext or ext.group(1).lower() in ('ts', 'm3u8'): live.append(g)
print(shape.most_common(12))
print('nie-pliki (ts/m3u8/bez rozszerzenia):', len(live), Counter(live).most_common(8))
