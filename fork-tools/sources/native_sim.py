#!/usr/bin/env python3
"""Symulacja natywnego Regex Nuvio (StreamRegexRanking + RankedRegexRules) na zapisanych odpowiedziach dodatków.
Użycie: python3 -I native_sim.py <presets.json> <katalog z *.json> [nazwa dodatku]
Tekst źródła jak StreamRegexRanking.searchableText: dodatek, name, title, description, filename, adres (albo infoHash).
Reguła wygrywa pierwsza pasująca; źródła bez żadnej pasującej reguły odpadają z autoplay."""
import json, re, sys, glob, os

def py(p):  # \x{1F50A} (Java) -> \U0001F50A (Python)
    return re.sub(r"\\x\{([0-9A-Fa-f]+)\}", lambda m: "\\U%08X" % int(m.group(1), 16), p)

def comp(p): return re.compile(py(p.strip()), re.I)

def text(s, addon):
    bh = s.get("behaviorHints") or {}
    url = s.get("url") or ("magnet:?xt=urn:btih:" + s["infoHash"] if s.get("infoHash") else "")
    parts = [addon, s.get("name"), s.get("title"), s.get("description"), bh.get("filename"), url]
    if s.get("infoHash"): parts.append(s["infoHash"])
    return " ".join(x or "" for x in parts)

def rank(rules, streams, addon):
    tiers = [[] for _ in rules]
    for s in streams:
        t = text(s, addon)
        for i, r in enumerate(rules):
            if r.search(t): tiers[i].append(s); break
    return [(i, s) for i, tier in enumerate(tiers) for s in tier]

def main():
    presets = json.load(open(sys.argv[1]))["presets"]
    addon = sys.argv[3] if len(sys.argv) > 3 else "TorrentClaw (PL, 720p)"
    for f in sorted(glob.glob(os.path.join(sys.argv[2], "*.json"))):
        st = [s for s in json.load(open(f)).get("streams", []) if s.get("url") or s.get("infoHash")]
        print(f"\n=== {os.path.basename(f)[:-5]} ({len(st)} źródeł)")
        for p in presets:
            rules = [comp(r) for r in p["rules"]]
            out = rank(rules, st, addon)
            print(f"  [{p['id']}] pasuje {len(out)}/{len(st)}; pierwsze 3:")
            for i, s in out[:3]:
                print(f"     reguła {i}: {s['name'][:26]:26} | {(s.get('behaviorHints') or {}).get('filename','')[:60]}")

if __name__ == "__main__": main()
