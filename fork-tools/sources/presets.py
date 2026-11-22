#!/usr/bin/env python3
"""Generuje app/src/main/assets/fork/source_presets.json z jednego miejsca (fragmenty = te same co SourceLanguage.kt).
Stare reguły zestawów (z HEAD) trafiają do „legacy” (żeby zapisany wybór nie zmienił się w „Własne”). Użycie: python3 -I presets.py"""
import json, os, re, subprocess

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
OUT = os.path.join(ROOT, "app/src/main/assets/fork/source_presets.json")

NOCAM = r"(?!.*\b(?:cam|hdcam|camrip|ts|hdts|telesync|telecine|tc)\b)"
SUBS_WORD = r"\b(?:pl[ ._-]?subs?|subs?[ ._-]?pl|napisy(?:[ ._-]?pl)?|plsub)\b"
# Polski dźwięk: słowa w nazwie, flaga w wierszu dźwięku (TorrentClaw: 🔊 po „TrueSpec” = sprawdzony plik; 🔊/🗣 bez niego
# = z nazwy, więc nie, gdy nazwa mówi „PL subs”; AIOStreams 🗣),
# AIOStreams SELection „✓ ᴘʟ …” przed nawiasem z napisami, samo „PL/POL/Polish” jako osobne słowo nazwy (nie „PL subs”).
PLA = ("(?:\\blektor\\b|\\bdubbing\\b|\\bdub[ ._-]?pl\\b|\\bpl[ ._-]?dub\\b"
       "|TrueSpec[^🔊]*🔊[^\\n]*🇵🇱"
       "|(?:🔊|🗣)[^\\n]*🇵🇱(?!.*" + SUBS_WORD + ")"
       "|✓[^\\n(]*ᴘʟ"
       "|(?<![\\w(])(?<!sub[ ._-])(?<!subs[ ._-])(?<!napisy[ ._-])(?<!subtitles[ ._-])(?:pl|pol|polish|polski)(?![\\w,)])(?![ ._-]*(?:sub|napis))"
       "|\\bmulti\\b[^\\n]*\\bpl\\b(?![ ._-]?sub))")
# Polskie napisy wbudowane: flaga w wierszu napisów (TorrentClaw 💬), AIOStreams „sᴜʙ (… ᴘʟ)”, słowa w nazwie.
PLS = "(?:💬[^\\n]*🇵🇱|sᴜʙ[^\\n]*\\([^)\\n]*ᴘʟ|" + SUBS_WORD + ")"
R = {"4k": r"(?:(?<!\d)2160p|\b4k\b|\buhd\b)", "1080": r"(?:(?<!\d)1080p|\bfhd\b)", "720": r"(?<!\d)720p"}

def rule(*need):
    return "(?s)^" + NOCAM + "".join("(?=.*" + n + ")" for n in need) + ".*$"

pl_always = [rule(PLA, R["4k"]), rule(PLA, R["1080"]), rule(PLA, R["720"]), rule(PLA),
             rule(PLS, R["4k"]), rule(PLS, R["1080"]), rule(R["4k"]), rule(R["1080"]), rule(R["720"]), rule()]
quality = []
for r in ("4k", "1080", "720"):
    quality += [rule(PLA, R[r]), rule(PLS, R[r]), rule(R[r])]
quality += [rule(PLA), rule(PLS), rule()]
original = [rule(R["4k"]), rule(R["1080"]), rule(R["720"]), rule()]

def main():
    # „legacy” = reguły z wersji w gicie (HEAD) i ich wcześniejsze legacy; bez pośrednich prób z dysku.
    head = subprocess.run(["git", "-C", ROOT, "show", "HEAD:app/src/main/assets/fork/source_presets.json"], capture_output=True, text=True, check=True).stdout
    old = {p["id"]: p for p in json.loads(head)["presets"]}
    def legacy(pid, new):
        prev = [old[pid]["rules"]] + old[pid].get("legacy", []) if pid in old else []
        return [r for r in prev if r != new]
    presets = [
        {"id": "pl_always", "title": "Polski dźwięk ważniejszy", "rules": pl_always, "audio": ["pol", "eng"], "subs": ["pol", "eng"],
         "legacy": legacy("pl_always", pl_always)},
        {"id": "pl_if_good", "title": "Jakość obrazu ważniejsza", "qualityFirst": True, "rules": quality, "audio": ["pol", "eng"], "subs": ["pol", "eng"],
         "legacy": legacy("pl_if_good", quality)},
        {"id": "original_subs", "title": "Oryginał z napisami", "rules": original, "audio": ["eng", "pol"], "subs": ["pol", "eng"],
         "legacy": legacy("original_subs", original)},
    ]
    for p in presets:
        assert len(p["rules"]) <= 20 and all(len(r) <= 2048 for r in p["rules"]), p["id"]
        for r in p["rules"]: re.compile(r, re.I)
    json.dump({"presets": presets}, open(OUT, "w"), ensure_ascii=False, indent=1)
    print("zapisano", OUT, [(p["id"], len(p["rules"]), max(map(len, p["rules"]))) for p in presets])

if __name__ == "__main__": main()
