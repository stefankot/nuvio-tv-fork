#!/usr/bin/env python3
"""Symulacja wyboru źródła (fork/sources) na zapisanych odpowiedziach dodatków.
Użycie: python3 -I sim.py <katalog z plikami *.json {"streams":[...]}> [preset.json]
Wypisuje tabelę plików (rozdzielczość, waga, PL dźwięk, napisy) i kolejność dla zestawów pl_always / pl_if_good."""
import json, re, sys, glob, os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import presets as P  # jedno źródło wzorców (to samo, z czego powstaje source_presets.json)
PLA, PLS, NOCAM = P.PLA, P.PLS, P.NOCAM
R4K, R1080, R720 = P.R["4k"], P.R["1080"], P.R["720"]
RULES = {p["id"]: [re.compile(r, re.I) for r in p["rules"]] for p in json.load(open(P.OUT))["presets"]}
RULES_TXT = {p["id"]: p["rules"] for p in json.load(open(P.OUT))["presets"]}

def rx(p): return re.compile(p, re.I | re.S)

pla, pls, nocam = rx(PLA), rx(PLS), rx("^" + NOCAM)

def text(s):
    bh = s.get("behaviorHints") or {}
    return "\n".join(x for x in [s.get("name"), s.get("title"), s.get("description"), bh.get("filename")] if x)

def res(s):
    t = text(s)
    for lab, p in (("4K", R4K), ("1080p", R1080), ("720p", R720)):
        if rx(p).search(t): return lab
    return "?"

def size_gb(s):
    v = (s.get("behaviorHints") or {}).get("videoSize")
    if v: return v / 1e9
    m = re.search(r"💾\s*([\d.]+)\s*GB", s.get("title", "") or "")
    return float(m.group(1)) if m else None

def subs_line(s):
    t = (s.get("title") or "") + "\n" + (s.get("description") or "")
    m = re.search(r"💬\s*([^\n]*)", t)
    if m: return ("PL" if "🇵🇱" in m.group(1) else "inne") + " " + m.group(1).strip()
    return "PL (nazwa)" if pls.search(text(s)) else "brak danych"

RES_RANK = {"4K": 0, "1080p": 1, "720p": 2, "?": 3}
def key_pl_always(s):   # jak KeRanking.polishTier: szczebel wśród reguł z „lektor”, potem napisy PL, potem jakość (KE)
    t = text(s); lr = [rx(r) for r in RULES_TXT["pl_always"] if "lektor" in r]
    tier = next((i for i, r in enumerate(lr) if r.search(t)), 99)
    return (tier, 0 if pls.search(t) else 1, RES_RANK[res(s)])
def key_quality(s):     # jakość przed wszystkim, w tej samej rozdzielczości: PL dźwięk > PL napisy > reszta
    t = text(s); return (RES_RANK[res(s)], 0 if pla.search(t) else (1 if pls.search(t) else 2))

def playable(s): return ("url" in s or "infoHash" in s) and nocam.search(text(s))

def main():
    d = sys.argv[1]
    for f in sorted(glob.glob(os.path.join(d, "*.json"))):
        st = [s for s in json.load(open(f)).get("streams", []) if playable(s)]
        print(f"\n=== {os.path.basename(f)[:-5]}: {len(st)} źródeł")
        print(f"{'roz.':6}{'GB':>7}  {'PL dźw.':8}{'napisy':28}{'TB':4}nazwa")
        for s in st:
            bh = s.get("behaviorHints") or {}
            gb = size_gb(s)
            print(f"{res(s):6}{(f'{gb:.1f}' if gb else '?'):>7}  {('tak' if pla.search(text(s)) else 'nie'):8}{subs_line(s)[:27]:28}"
                  f"{('tak' if bh.get('cached') else '-'):4}{(bh.get('filename') or s.get('name',''))[:60]}")
        for lab, k in (("PL dźwięk", key_pl_always), ("jakość", key_quality)):
            top = sorted(st, key=k)[:3]
            print(f"  [{lab}] " + " | ".join(f"{res(s)} {('PL' if pla.search(text(s)) else '--')}/{('pl' if pls.search(text(s)) else '--')} {(s.get('behaviorHints') or {}).get('filename','')[:38]}" for s in top))

if __name__ == "__main__": main()
