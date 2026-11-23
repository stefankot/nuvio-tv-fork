#!/usr/bin/env python3
"""Prosta analiza zrzutu pamięci Androida (hprof z `am dumpheap`, bez hprof-conv).

  python3 -I hprof_hist.py hist  <plik.hprof> [top]          # histogram klas (sterta app)
  python3 -I hprof_hist.py big   <plik.hprof> [min_len]      # duże tablice obiektów + klasy elementów
  python3 -I hprof_hist.py owners <plik.hprof> <id_hex>...   # kto wskazuje na te obiekty (pole/indeks)
  python3 -I hprof_hist.py chain <plik.hprof> <id_hex> [n]    # łańcuch właścicieli w górę (n poziomów)
  python3 -I hprof_hist.py fields <plik.hprof> <id_hex>       # pola obiektu

Bez zależności (czysty Python, mmap). Plik ~600 MB: jeden przebieg ok. 1–3 min.
"""
import mmap
import struct
import sys
from collections import defaultdict

BASIC = {2: 0, 4: 1, 5: 2, 6: 4, 7: 8, 8: 1, 9: 2, 10: 4, 11: 8}  # 2 = obiekt (rozmiar id)
PRIM_NAME = {4: 'boolean[]', 5: 'char[]', 6: 'float[]', 7: 'double[]', 8: 'byte[]', 9: 'short[]', 10: 'int[]', 11: 'long[]'}


class Hprof:
    def __init__(self, path):
        f = open(path, 'rb')
        self.m = mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ)
        end = self.m.find(b'\0')
        self.idsize = struct.unpack_from('>I', self.m, end + 1)[0]
        assert self.idsize == 4, 'tylko id 4 bajty'
        self.start = end + 1 + 4 + 8
        self.strings = {}
        self.class_name = {}      # class obj id -> name
        self.class_info = {}      # class obj id -> (super, inst_size, [(name, type)])

    def records(self):
        m, pos, n = self.m, self.start, len(self.m)
        while pos + 9 <= n:
            tag = m[pos]
            length = struct.unpack_from('>I', m, pos + 5)[0]
            yield tag, pos + 9, length
            pos += 9 + length

    def load_meta(self):
        """STRING, LOAD CLASS i CLASS DUMP (potrzebne do nazw)."""
        m = self.m
        name_of_class_serial = {}
        for tag, p, ln in self.records():
            if tag == 0x01:
                sid = struct.unpack_from('>I', m, p)[0]
                self.strings[sid] = m[p + 4:p + ln].decode('utf-8', 'replace')
            elif tag == 0x02:
                _, cid, _, nid = struct.unpack_from('>IIII', m, p)
                name_of_class_serial[cid] = nid
        for cid, nid in name_of_class_serial.items():
            self.class_name[cid] = self.strings.get(nid, '?%x' % nid)
        for _ in self.heap_objects(want_class_dumps=True):
            pass

    def heap_objects(self, want_class_dumps=False):
        """Generator po podrekordach sterty: (kind, id, pos, extra, heap). kind: 'I' instancja, 'O' tablica obiektów, 'P' tablica prymitywów."""
        m = self.m
        unp = struct.unpack_from
        heap = 'app'
        heap_names = {}
        for tag, p, ln in self.records():
            if tag not in (0x0C, 0x1C):
                continue
            end = p + ln
            while p < end:
                st = m[p]
                p += 1
                if st == 0xFF or st == 0x05 or st == 0x07 or st == 0x89 or st == 0x8A or st == 0x8B or st == 0x8C or st == 0x8D or st == 0x90:
                    p += 4
                elif st == 0x01:
                    p += 8
                elif st == 0x02 or st == 0x03 or st == 0x08 or st == 0x8E:
                    p += 12
                elif st == 0x04 or st == 0x06:
                    p += 8
                elif st == 0xFE:
                    hid, hn = unp('>II', m, p)
                    heap = self.strings.get(hn, str(hid))
                    p += 8
                elif st == 0xC3:
                    p += 4 + 4 + 4 + 1
                elif st == 0x20:
                    cid = unp('>I', m, p)[0]
                    sup = unp('>I', m, p + 8)[0]
                    inst_size = unp('>I', m, p + 32)[0]
                    q = p + 36
                    ncp = unp('>H', m, q)[0]; q += 2
                    for _ in range(ncp):
                        t = m[q + 2]
                        q += 3 + (BASIC[t] or 4)
                    nst = unp('>H', m, q)[0]; q += 2
                    for _ in range(nst):
                        t = m[q + 4]
                        q += 5 + (BASIC[t] or 4)
                    nif = unp('>H', m, q)[0]; q += 2
                    fields = []
                    for _ in range(nif):
                        fid, t = unp('>IB', m, q)
                        fields.append((fid, t))
                        q += 5
                    if want_class_dumps:
                        self.class_info[cid] = (sup, inst_size, [(self.strings.get(fid, '?'), t) for fid, t in fields])
                    p = q
                elif st == 0x21:
                    oid, _, cid, nb = unp('>IIII', m, p)
                    yield 'I', oid, p + 16, (cid, nb), heap
                    p += 16 + nb
                elif st == 0x22:
                    oid, _, n, acid = unp('>IIII', m, p)
                    yield 'O', oid, p + 16, (acid, n), heap
                    p += 16 + n * 4
                elif st == 0x23:
                    oid, _, n, t = unp('>IIIB', m, p)
                    yield 'P', oid, p + 13, (t, n), heap
                    p += 13 + n * BASIC[t]
                else:
                    raise ValueError('nieznany podrekord %#x w %d' % (st, p))

    def cname(self, cid):
        return self.class_name.get(cid, '?%x' % cid)

    def layout(self, cid):
        """Pola instancji w kolejności zapisu: najpierw klasa, potem nadklasy."""
        out = []
        while cid and cid in self.class_info:
            sup, _, fields = self.class_info[cid]
            out.extend(fields)
            cid = sup
        return out

    def ref_fields(self, cid, pos):
        """(nazwa_pola, id) dla pól obiektowych instancji."""
        m = self.m
        res = []
        if cid not in self.class_info:
            # Klasy systemowe (obraz/zygote) nie mają opisu w zrzucie: pola 4-bajtowe co 4 bajty (bez nazw).
            nb = struct.unpack_from('>I', m, pos - 4)[0]
            for off in range(0, nb - 3, 4):
                v = struct.unpack_from('>I', m, pos + off)[0]
                if v:
                    res.append(('@%d' % off, v))
            return res
        for name, t in self.layout(cid):
            if t == 2:
                v = struct.unpack_from('>I', m, pos)[0]
                if v:
                    res.append((name, v))
                pos += 4
            else:
                pos += BASIC[t]
        return res


def fmt(b):
    return '%.1f MB' % (b / 1048576)


def cmd_hist(h, top=60):
    cnt = defaultdict(int)
    size = defaultdict(int)
    heaps = defaultdict(int)
    for kind, oid, pos, extra, heap in h.heap_objects():
        if kind == 'I':
            b = extra[1] + 8
            key = h.cname(extra[0])
        elif kind == 'O':
            b = extra[1] * 4 + 12
            key = h.cname(extra[0])
        else:
            b = extra[1] * BASIC[extra[0]] + 12
            key = PRIM_NAME[extra[0]]
        heaps[heap] += b
        if heap != 'app':
            continue
        cnt[key] += 1
        size[key] += b
    print('sterty:', ', '.join('%s %s' % (k, fmt(v)) for k, v in heaps.items()))
    print('%-70s %10s %12s' % ('klasa (sterta app)', 'liczba', 'płytko'))
    for k in sorted(size, key=size.get, reverse=True)[:top]:
        print('%-70s %10d %12s' % (k[:70], cnt[k], fmt(size[k])))


def cmd_big(h, min_len=20000):
    big = []
    for kind, oid, pos, extra, heap in h.heap_objects():
        if kind == 'O' and extra[1] >= min_len and heap == 'app':
            ids = [struct.unpack_from('>I', h.m, pos + 4 * i)[0] for i in range(0, extra[1], max(1, extra[1] // 40))]
            big.append((oid, h.cname(extra[0]), extra[1], [i for i in ids if i]))
    want = {i for b in big for i in b[3]}
    cls = {}
    for kind, oid, pos, extra, heap in h.heap_objects():
        if oid in want:
            cls[oid] = h.cname(extra[0]) if kind != 'P' else PRIM_NAME[extra[0]]
    for oid, cn, n, ids in sorted(big, key=lambda b: -b[2]):
        kinds = defaultdict(int)
        for i in ids:
            kinds[cls.get(i, '?')] += 1
        print('%08x %-28s n=%-8d elementy: %s' % (oid, cn, n, dict(kinds)))


def owners_of(h, targets):
    res = defaultdict(list)
    m = h.m
    for kind, oid, pos, extra, heap in h.heap_objects():
        if kind == 'I':
            for name, v in h.ref_fields(extra[0], pos):
                if v in targets:
                    res[v].append((oid, h.cname(extra[0]), name))
        elif kind == 'O':
            n = extra[1]
            if n == 0:
                continue
            arr = struct.unpack_from('>%dI' % n, m, pos)
            hit = targets.intersection(arr)
            for v in hit:
                res[v].append((oid, h.cname(extra[0]), '[%d]' % arr.index(v)))
    return res


def cmd_owners(h, ids):
    t = {int(x, 16) for x in ids}
    res = owners_of(h, t)
    for v in t:
        print('%08x <-' % v)
        for oid, cn, f in res.get(v, [])[:20]:
            print('    %08x %s .%s' % (oid, cn, f))


def cmd_chain(h, idhex, levels=8):
    cur = {int(idhex, 16)}
    seen = set(cur)
    for lv in range(levels):
        res = owners_of(h, cur)
        nxt = set()
        for v in cur:
            for oid, cn, f in res.get(v, [])[:6]:
                print('  ' * lv + '%08x <- %08x %s .%s' % (v, oid, cn, f))
                if oid not in seen:
                    nxt.add(oid)
                    seen.add(oid)
        if not nxt:
            break
        cur = set(list(nxt)[:12])


def cmd_chains(h, ids, levels=7):
    """Łańcuchy właścicieli dla wielu obiektów naraz (jeden przebieg na poziom); pomija pośrednie tablice/węzły."""
    roots = [int(x, 16) for x in ids]
    paths = {r: [] for r in roots}
    front = {r: r for r in roots}  # root -> bieżący obiekt
    for _ in range(levels):
        cur = set(front.values())
        if not cur:
            break
        res = owners_of(h, cur)
        for r in list(front):
            own = res.get(front[r], [])
            if not own:
                paths[r].append('(brak właściciela — korzeń GC?)')
                del front[r]
                continue
            oid, cn, f = own[0]
            paths[r].append('%s.%s%s' % (cn.split('.')[-1], f, '' if len(own) == 1 else ' (+%d)' % (len(own) - 1)))
            front[r] = oid
    for r in roots:
        print('%08x: %s' % (r, ' <- '.join(paths[r])))


def cmd_fields(h, idhex):
    t = int(idhex, 16)
    for kind, oid, pos, extra, heap in h.heap_objects():
        if oid == t:
            if kind == 'I':
                print(h.cname(extra[0]))
                pp = pos
                for name, ty in h.layout(extra[0]):
                    sz = BASIC[ty] or 4
                    raw = h.m[pp:pp + sz]
                    v = int.from_bytes(raw, 'big')
                    print('  %-30s %s' % (name, ('%08x' % v) if ty == 2 else v))
                    pp += sz
            else:
                print(kind, extra)
            return


if __name__ == '__main__':
    cmd, path = sys.argv[1], sys.argv[2]
    h = Hprof(path)
    h.load_meta()
    if cmd == 'hist':
        cmd_hist(h, int(sys.argv[3]) if len(sys.argv) > 3 else 60)
    elif cmd == 'big':
        cmd_big(h, int(sys.argv[3]) if len(sys.argv) > 3 else 20000)
    elif cmd == 'owners':
        cmd_owners(h, sys.argv[3:])
    elif cmd == 'chain':
        cmd_chain(h, sys.argv[3], int(sys.argv[4]) if len(sys.argv) > 4 else 8)
    elif cmd == 'chains':
        cmd_chains(h, sys.argv[3:])
    elif cmd == 'fields':
        cmd_fields(h, sys.argv[3])
