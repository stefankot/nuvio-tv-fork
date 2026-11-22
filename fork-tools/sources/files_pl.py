#!/usr/bin/env python3
"""Ile PLIKÓW (źródeł) ma polski dźwięk i ile polskie napisy, w całej bibliotece i na dodatek.
Użycie: python3 -I files_pl.py <katalog raw (pliki <nr>_<klucz>.json)> <nazwy dodatków: nr=nazwa ...>
Plik = wpis z adresem albo infoHash; ten sam plik z kilku dodatków liczony raz (nazwa pliku albo rozmiar+tytuł)."""
import json, re, sys, os, glob, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import count_pl as C

def fname(s):
    bh = s.get("behaviorHints") or {}
    f = bh.get("filename") or ""
    if not f:  # Comet/Torrentio: pierwszy wiersz opisu to nazwa pliku
        t = (s.get("description") or s.get("title") or "")
        f = re.sub(r"^📄\s*", "", t.split("\n")[0])
    return re.sub(r"[^a-z0-9]", "", f.lower())

def main():
    raw = sys.argv[1]; names = dict(a.split("=", 1) for a in sys.argv[2:])
    per = collections.defaultdict(collections.Counter); uniq = {}
    for f in glob.glob(os.path.join(raw, "*_L*.json")):
        ad = os.path.basename(f).split("_")[0]
        if ad not in names: continue
        try: st = json.load(open(f)).get("streams", [])
        except Exception: continue
        key_t = os.path.basename(f).split("_", 1)[1]
        for s in st:
            if not (s.get("url") or s.get("infoHash")): continue
            if "rate-limit" in json.dumps(s).lower(): continue
            t = "\n".join(x for x in [s.get("name"), s.get("title"), s.get("description"), (s.get("behaviorHints") or {}).get("filename")] if x)
            a = C.classify(s) == "a"; sub = bool(C.pls.search(t))
            per[names[ad]]["plików"] += 1; per[names[ad]]["PL dźwięk"] += a; per[names[ad]]["PL napisy"] += sub
            k = (key_t, fname(s) or json.dumps(s.get("name")) + str((s.get("behaviorHints") or {}).get("videoSize")))
            u = uniq.setdefault(k, [False, False]); u[0] |= a; u[1] |= sub
    for n, c in per.items(): print(f"{n:22} plików {c['plików']:5} | PL dźwięk {c['PL dźwięk']:5} | PL napisy {c['PL napisy']:5}")
    print(f"{'RAZEM bez powtórek':22} plików {len(uniq):5} | PL dźwięk {sum(u[0] for u in uniq.values()):5} | PL napisy {sum(u[1] for u in uniq.values()):5}"
          f" | oba {sum(u[0] and u[1] for u in uniq.values())} | tylko napisy {sum(u[1] and not u[0] for u in uniq.values())}")

if __name__ == "__main__": main()
