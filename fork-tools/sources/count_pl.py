#!/usr/bin/env python3
"""Ile tytułów ma polski dźwięk / tylko polskie napisy / nic, na zapisanych odpowiedziach dodatków.
Użycie: python3 -I count_pl.py <katalog raw (pliki <nr dodatku>_<klucz>.json)> <titles.txt (klucz ścieżka)> [library.json]"""
import json, re, sys, os, glob, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import presets as P
pla, pls = re.compile(P.PLA, re.I | re.S), re.compile(P.PLS, re.I | re.S)
# Comet: ostatni wiersz opisu to same flagi języków („🌎/🇵🇱/🇷🇺”), z nazwy pliku — liczony jak polski dźwięk.
comet_line = re.compile(r"^(?:[🌎\U0001F1E6-\U0001F1FF]+/)*[🌎\U0001F1E6-\U0001F1FF]+$", re.M)
AD = {"0": "TorrentClaw", "1": "AIOStreams", "2": "Comet"}

def classify(s):
    t = "\n".join(x for x in [s.get("name"), s.get("title"), s.get("description"), (s.get("behaviorHints") or {}).get("filename")] if x)
    if pla.search(t) or any("🇵🇱" in x for x in comet_line.findall(t)): return "a"
    return "s" if pls.search(t) else "-"

def ready(s):
    n = s.get("name", "") or ""
    return "⚡" in n and "⬇" not in n

def main():
    raw, titles = sys.argv[1], [l.split() for l in open(sys.argv[2]) if l.strip()]
    lib = json.load(open(sys.argv[3])) if len(sys.argv) > 3 else None
    out = []
    for idx, (k, p) in enumerate(titles):
        c = collections.Counter(); r = collections.Counter(); err = 0; by = collections.Counter()
        for f in glob.glob(os.path.join(raw, f"*_{k}.json")):
            try: st = json.load(open(f)).get("streams", [])
            except Exception: err += 1; continue
            for s in st:
                if not (s.get("url") or s.get("infoHash")): continue
                if "rate-limit" in json.dumps(s).lower(): err += 1; continue  # komunikat limitu serwera, nie źródło
                k2 = classify(s); c[k2] += 1
                if k2 == "a": by[AD[os.path.basename(f)[0]]] += 1
                if ready(s): r[k2] += 1
        cat = "PL dźwięk" if c["a"] else ("tylko PL napisy" if c["s"] else ("nic" if sum(c.values()) else "brak źródeł"))
        name = (lib[idx]["name"] + " (" + str(lib[idx]["year"])[:4] + ")") if lib else k
        typ = lib[idx]["type"] if lib else p.split("/")[0]
        out.append(dict(name=name, type=typ, cat=cat, n=sum(c.values()), a=c["a"], s=c["s"], ra=r["a"], rs=r["s"], err=err, by=dict(by)))
    json.dump(out, open(os.path.join(raw, "_wynik.json"), "w"), ensure_ascii=False)
    tot = collections.Counter(o["cat"] for o in out)
    print(dict(tot))
    for typ in ("movie", "series"):
        print(typ, dict(collections.Counter(o["cat"] for o in out if o["type"] == typ)))
    print("PL dźwięk, ale żaden gotowy od razu:", sum(1 for o in out if o["a"] and not o["ra"]))
    print("błędy odpowiedzi:", sum(o["err"] for o in out))

if __name__ == "__main__": main()
