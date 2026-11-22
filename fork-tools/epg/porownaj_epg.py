#!/usr/bin/env python3
"""porownaj_epg.py A.xml(.gz) B.xml(.gz) [nazwaA nazwaB]

Porównanie dwóch przewodników XMLTV (22.11: FlixNest vs epg.ovh dla polskich kanałów hls.gd).
Czyta strumieniowo (iterparse). Wypisuje: liczby kanałów/programów, zakres dni wstecz/naprzód, jakie pola mają
programy (opis, kategoria, obrazek, odcinek, ...), a dla kanałów dopasowanych po nazwie (bez „PL:”, „HD” itp.)
— ile programów i jakie pola ma każdy z przewodników. Nie wypisuje adresów (mogą zawierać token).
"""
import collections
import gzip
import re
import sys
import time
import unicodedata
import xml.etree.ElementTree as ET


def opener(path):
    with open(path, 'rb') as f:
        magic = f.read(2)
    return gzip.open(path, 'rb') if magic == b'\x1f\x8b' else open(path, 'rb')


QUALITY = re.compile(r'\s+(fhd|uhd|hd|sd|4k|8k|hevc|h26[45]|hdr|raw|backup|alt|vip|50 ?fps|\+1|pl)$')


def norm(name):
    t = unicodedata.normalize('NFD', name.lower().replace('ł', 'l'))
    t = ''.join(c for c in t if unicodedata.category(c) != 'Mn')
    t = re.sub(r'^\[?[a-z]{2,3}\]?\s*[:|]\s*', '', t)
    t = re.sub(r'\s*[(\[][^)\]]*[)\]]', ' ', t)
    t = re.sub(r'\.(pl|tv|com|uk|de|cz|ua)$', '', t)
    t = re.sub(r'[_\-]+', ' ', t)
    t = re.sub(r'\s+', ' ', t).strip()
    prev = None
    while prev != t:
        prev, t = t, QUALITY.sub('', t).strip()
    return t


def ts(v):
    # 20261122153000 +0100
    try:
        return time.mktime(time.strptime(v[:14], '%Y%m%d%H%M%S'))
    except Exception:
        return None


def read(path):
    ch_names = {}
    progs = collections.Counter()
    fields = collections.Counter()
    per_ch = collections.defaultdict(lambda: collections.Counter())
    langs = collections.Counter()
    starts = []
    total = 0
    for ev, el in ET.iterparse(opener(path), events=('end',)):
        if el.tag == 'channel':
            ch_names[el.get('id')] = [d.text or '' for d in el.findall('display-name')]
            el.clear()
        elif el.tag == 'programme':
            total += 1
            cid = el.get('channel')
            c = per_ch[cid]
            c['n'] += 1
            s = ts(el.get('start', ''))
            if s and total % 50 == 0:
                starts.append(s)
            if s:
                c['min'] = min(c['min'] or s, s)
                c['max'] = max(c['max'], s)
            seen = set()
            for k in el:
                seen.add(k.tag)
                if k.get('lang'):
                    langs[k.tag + ':' + k.get('lang')] += 1
                if k.tag == 'desc' and (k.text or '').strip():
                    seen.add('desc(niepusty)')
            for tag in seen:
                fields[tag] += 1
                c[tag] += 1
            el.clear()
    return ch_names, per_ch, fields, langs, starts, total


def summary(label, data):
    ch_names, per_ch, fields, langs, starts, total = data
    now = time.time()
    with_progs = sum(1 for c in per_ch.values() if c['n'])
    print(f'== {label}: kanałów {len(ch_names)}, z programami {with_progs}, programów {total}')
    if starts:
        print(f'   zakres: od {(now - min(starts)) / 86400:.1f} dni wstecz do {(max(starts) - now) / 86400:.1f} dni naprzód')
    print('   pola programów (% programów):', ', '.join(f'{k} {100 * v / max(total, 1):.0f}%' for k, v in fields.most_common()))
    print('   języki:', ', '.join(f'{k} {v}' for k, v in langs.most_common(8)))


def main():
    a, b = sys.argv[1], sys.argv[2]
    la, lb = (sys.argv[3], sys.argv[4]) if len(sys.argv) > 4 else ('A', 'B')
    A, B = read(a), read(b)
    summary(la, A)
    summary(lb, B)
    # dopasowanie kanałów B (np. epg.ovh — polskie) do A po nazwie
    index_a = collections.defaultdict(list)
    for cid, names in A[0].items():
        for n in names + [cid]:
            index_a[norm(n)].append(cid)
    rows = []
    for cid, names in B[0].items():
        cb = B[1].get(cid)
        if not cb or not cb['n']:
            continue
        cands = {x for n in names + [cid] for x in index_a.get(norm(n), [])}
        best = max(cands, key=lambda x: A[1][x]['n'], default=None)
        ca = A[1][best] if best else collections.Counter()
        rows.append((names[0] if names else cid, cb, ca))
    matched = [r for r in rows if r[2]['n']]
    print(f'== kanały {lb} z programami: {len(rows)}; mają odpowiednik z programami w {la}: {len(matched)}')

    def agg(rs, i, f):
        return sum(r[i][f] for r in rs)

    for f in ('n', 'desc(niepusty)', 'category', 'icon', 'episode-num', 'sub-title', 'credits', 'date', 'rating'):
        print(f'   {f:15s} {lb}: {agg(matched, 1, f):8d}   {la}: {agg(matched, 2, f):8d}   (te same kanały)')
    now = time.time()
    ahead_b = [(r[1]['max'] - now) / 86400 for r in matched if r[1]['max']]
    ahead_a = [(r[2]['max'] - now) / 86400 for r in matched if r[2]['max']]
    back_b = [(now - r[1]['min']) / 86400 for r in matched if r[1]['min']]
    back_a = [(now - r[2]['min']) / 86400 for r in matched if r[2]['min']]
    med = lambda v: sorted(v)[len(v) // 2] if v else 0
    print(f'   mediana dni naprzód  {lb}: {med(ahead_b):.1f}   {la}: {med(ahead_a):.1f}')
    print(f'   mediana dni wstecz   {lb}: {med(back_b):.1f}   {la}: {med(back_a):.1f}')
    worse = sorted(matched, key=lambda r: r[2]['n'] - r[1]['n'])[:8]
    print('   najwięcej brakuje w', la + ':', '; '.join(f'{r[0]} {r[2]["n"]}/{r[1]["n"]}' for r in worse))
    missing = [r[0] for r in rows if not r[2]['n']]
    print(f'   brak w {la} ({len(missing)}):', ', '.join(missing[:25]))


if __name__ == '__main__':
    main()
