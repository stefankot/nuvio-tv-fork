#!/usr/bin/env python3
"""Próbka kanałów Live TV ze zrzutu pamięci: grupa, nazwa, rodzaj adresu (bez loginu i hosta).
  python3 -I hprof_channels.py <plik.hprof> <id_tablicy_hex> [co_ile]"""
import re, struct, sys
from collections import Counter
sys.path.insert(0, __import__('os').path.dirname(__file__))
import hprof_hist as H

h = H.Hprof(sys.argv[1]); h.load_meta()
arr = int(sys.argv[2], 16); step = int(sys.argv[3]) if len(sys.argv) > 3 else 1
els = None
for kind, oid, pos, extra, heap in h.heap_objects():
    if oid == arr:
        els = [struct.unpack_from('>I', h.m, pos + 4 * i)[0] for i in range(0, extra[1], step)]
        break
els = set(e for e in els if e)
fields = {}
for kind, oid, pos, extra, heap in h.heap_objects():
    if kind == 'I' and oid in els:
        fields[oid] = dict(h.ref_fields(extra[0], pos))
want = set()
for f in fields.values():
    for k in ('group', 'name', 'streamUrl', 'logoUrl', 'id', 'guideKey', 'tvgId'):
        if k in f: want.add(f[k])
val = {}
for kind, oid, pos, extra, heap in h.heap_objects():
    if kind == 'I' and oid in want:
        val[oid] = struct.unpack_from('>I', h.m, pos + 8)[0]
arrs = {v: k for k, v in val.items()}
txt = {}
for kind, oid, pos, extra, heap in h.heap_objects():
    if kind == 'P' and oid in arrs:
        b = h.m[pos:pos + extra[1] * H.BASIC[extra[0]]]
        txt[arrs[oid]] = b.decode('latin-1') if extra[0] == 8 else b.decode('utf-16-be', 'replace')
def kind(u):
    if not u: return '-'
    m = re.search(r'/(live|movie|series|timeshift)/', u)
    if m: return m.group(1)
    return re.sub(r'[^.]+$', '', u.split('?')[0])[-6:] or 'inne'
groups, kinds = Counter(), Counter()
lens = Counter()
for oid, f in fields.items():
    g = txt.get(f.get('group'), ''); u = txt.get(f.get('streamUrl'), '')
    groups[g] += 1; kinds[kind(u)] += 1
    for k in ('name', 'streamUrl', 'logoUrl', 'id', 'guideKey', 'tvgId'):
        lens[k] += len(txt.get(f.get(k), '') or '')
n = len(fields)
print('kanałów w próbce', n)
print('rodzaje adresów', kinds.most_common(8))
print('grupy', groups.most_common(15))
print('średnie długości', {k: round(v / max(n, 1)) for k, v in lens.items()})
