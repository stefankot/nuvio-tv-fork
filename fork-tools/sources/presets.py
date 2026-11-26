#!/usr/bin/env python3
"""Generator reguł autoplay (natywny Regex Nuvio) → app/src/main/assets/fork/source_presets.json.
Kawałki wzorców jak w e9a032fbd (tekst źródła StreamRegexRanking.searchableText: dodatek, nazwa, tytuł, opis, plik,
adres, infoHash): (?s), sprawdzanie tylko do adresu linku, flagi 🔊/🗣 = dźwięk, 💬 = napisy, bez „(PL, 720p)”.
Nowe 22.11: polskie napisy wbudowane jako szczebel, zestaw „jakość ważniejsza”, źródła do ściągania (🌐 torrent,
⬇️/⏳ TorrentClaw) za gotowymi, 720p bez gołego „hd” (łapało „DTS-HD”). Poprzednie reguły: source_presets_v2.json.
Nowe 26.11 (test VOD na JMGO, poprzednie reguły: source_presets_v3.json): w obrębie 4K najpierw pliki bez REMUX
(4K Remux 72–77 Mb/s doczytywał się 10× na minutę, 4K BluRay 35 Mb/s grał płynnie), a wpisy „Click to verify & play”
(Flix-Streams SmartPlay: napis „4K”, a gra 1080p) idą na koniec razem z plikami niegotowymi.
Użycie: python3 -I fork-tools/sources/presets.py   Sprawdzenie: python3 -I fork-tools/sources/native_sim.py …"""
import json, os, re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
OUT = os.path.join(ROOT, "app/src/main/assets/fork/source_presets.json")
V2 = os.path.join(ROOT, "app/src/main/assets/fork/source_presets_v2.json")

U = r"(?:(?!https?://).)*?"                       # dowolny tekst, ale nie dalej niż adres linku
FLAGS = r"[\x{1F1E6}-\x{1F1FF}]"
PL_FLAG = r"(?:" + FLAGS + r"{2})*\x{1F1F5}\x{1F1F1}"   # 🇵🇱 na granicy par flag

def res(words): return "(?=" + U + r"(?<!, )\b(?:" + words + r")\b)"
R4K, R1080, R720 = res("2160p|4k|uhd"), res("1080p|fhd"), res("720p")
NO_RES = "(?!" + U + r"(?<!, )\b(?:2160p|4k|uhd|1080p|fhd|720p)\b)"
NOCAM = "(?!" + U + r"\b(?:cam|hdcam|camrip|ts|hdts|telesync|telecine|tc)\b)"

def pla_from_v2():
    """Polski dźwięk: dokładnie ten sam kawałek co w zestawie pl_always z e9a032fbd (reguła 0 = 4K + dźwięk + bez kina)."""
    r0 = json.load(open(V2))["presets"][0]["rules"][0]
    old4k = "(?=" + U + r"(?<!, )\b(?:2160p|4k|uhd)\b)"
    assert r0.startswith("(?s)^" + old4k) and r0.endswith(NOCAM), "zmienił się format reguł v2"
    return r0[len("(?s)^" + old4k):-len(NOCAM)]
PLA = pla_from_v2()
# Polskie napisy wbudowane: 🇵🇱 w wierszu 💬 (TorrentClaw), „sᴜʙ (… ᴘʟ)” (AIOStreams), słowa w nazwie pliku.
PLS = ("(?=" + U + r"(?:\x{1F4AC}[^\n\x{1F1E6}-\x{1F1FF}]*" + PL_FLAG +
       r"|s\x{1D1C}\x{0299}[^\n]*\([^)\n]*\x{1D18}\x{029F}" +
       r"|\b(?:pl[ ._-]?sub(?:s|titles?)?|sub(?:s|titles?)?[ ._-]?pl|napisy(?:[ ._-]?pl)?|plsub)\b))")
NOT_PLA = "(?!" + PLA[3:]                          # „(?=X)” → „(?!X)”
# Do ściągania: torrent bez debrid (tekst kończy się infoHash), TorrentClaw ⬇️ Download / ⏳ caches on play.
SLOW_BODY = r".*?(?:\x{2B07}|\x{23F3}|\bcaches on play\b|\bclick to verify\b|\b[0-9a-f]{40}\s*$)"
FAST, SLOW = "(?!" + SLOW_BODY + ")", "(?=" + SLOW_BODY + ")"

def rule(*parts): return "(?s)^" + "".join(parts) + NOCAM
NOREMUX = "(?!" + U + r"remux)"                   # „Remux”, „BDRemux”, „〈Remux〉”
def k4(*parts): return [rule(FAST, R4K, NOREMUX, *parts), rule(FAST, R4K, *parts)]  # 4K: lżejszy plik przed REMUX

pl_always = [
    *k4(PLA), rule(FAST, R1080, PLA), rule(FAST, R720, PLA), rule(FAST, NO_RES, PLA),
    *k4(PLS), rule(FAST, R1080, PLS), rule(FAST, PLS),
    *k4(), rule(FAST, R1080), rule(FAST, R720), rule(FAST),
    rule(SLOW, PLA), rule(SLOW, PLS), rule(SLOW),
]
quality = []
quality += [*k4(PLA), *k4(PLS), *k4()]
for R in (R1080, R720, NO_RES):
    quality += [rule(FAST, R, PLA), rule(FAST, R, PLS), rule(FAST, R)]
quality += [rule(SLOW, PLA), rule(SLOW)]
original = [*k4(), rule(FAST, R1080), rule(FAST, R720), rule(FAST), rule(SLOW)]

def main():
    presets = [
        {"id": "pl_always", "title": "Polski dźwięk ważniejszy", "rules": pl_always, "audio": ["pol", "eng"], "subs": ["pol", "eng"]},
        {"id": "pl_if_good", "title": "Jakość obrazu ważniejsza", "rules": quality, "audio": ["pol", "eng"], "subs": ["pol", "eng"]},
        {"id": "original_subs", "title": "Oryginał z napisami", "rules": original, "audio": ["eng", "pol"], "subs": ["pol", "eng"]},
    ]
    for p in presets:
        assert 1 <= len(p["rules"]) <= 20 and all(len(r) <= 2048 for r in p["rules"]), (p["id"], max(map(len, p["rules"])))
    json.dump({"presets": presets}, open(OUT, "w"), ensure_ascii=False, indent=1)
    print("zapisano", [(p["id"], len(p["rules"]), max(map(len, p["rules"]))) for p in presets])

if __name__ == "__main__": main()
