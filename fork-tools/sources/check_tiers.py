#!/usr/bin/env python3
"""Szczeble reguł dla przykładowych nazw (to samo, co SourcePresetsTest.kt, ale bez Gradle). Użycie: python3 -I check_tiers.py"""
import json, os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from native_sim import comp
P = {p["id"]: [comp(r) for r in p["rules"]] for p in json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/assets/fork/source_presets.json")))["presets"]}
def tier(pid, t): return next((i for i, r in enumerate(P[pid]) if r.search(t)), None)
tcPl = "TorrentClaw (PL, 720p) ⚡️ INSTANT 4K · Web-dl ⭐ 🔵 75/100 · TB\n✅ TrueSpec Verified\n🎞️ hevc · DV+HDR10 · 10-bit\n🔊 ac3 · 5.1 · 🇵🇱🇺🇸\n💬 🇵🇱🇺🇸\n💾 17.1 GB\n🏷️ K83   [DEVIL-TORRENTS.PL] Inside.Out.2.2024.MULTi.2160p.WEB-DL   https://torrentclaw.com/x/ts/sub"
tcSubsOnly = "TorrentClaw (PL, 720p) ⚡️ INSTANT 4K · Bluray 🟢 94/100 · TB\n✅ TrueSpec Verified\n🔊 truehd · 7.1 · 🇺🇸🇪🇸🇫🇷\n💬 🇵🇱🇸🇦🇧🇬🇨🇿 +24\n💾 13.2 GB   Inside Out 2 (2024) (2160p BluRay) [QxR]   https://torrentclaw.com/x"
HASH = " 7714a4a5c3d2e1f0a9b8c7d6e5f4a3b2c1d0e9f8"
cases = [
 ("pl_always","Film 2023 1080p WEB-DL PL Lektor",1),("pl_always","Film.2023.2160p.UHD.DUBBING.PL",0),("pl_always","Film 720p BluRay Dubbing",2),
 ("pl_always","Film 480p POLISH",3),("pl_always","Film 1080p WEB-DL English",8),("pl_always","Film 1080p MULTI PL",1),("pl_always","Film 1080p PL",1),
 ("pl_always","Film 1080p WEB-DL PL subs",5),("pl_always","Film 1080p WEB napisy PL",5),("pl_always","Film 1080p NF WEB-DL ENG + PL Subtitles",5),
 ("pl_if_good","Film 1080p WEB-DL Lektor PL",3),("pl_if_good","Film 4K HDR Dubbing PL",0),("pl_if_good","Film 720p Lektor PL",6),
 ("pl_if_good","Film 2160p English",2),("pl_if_good","Film 1080p English",5),("pl_if_good","Film 720p English",8),("pl_if_good","Film 1080p PL subs",4),
 ("original_subs","Film 2160p PL Lektor",0),("original_subs","Film 1080p English",1),("original_subs","Film 720p",2),("original_subs","Film SD",3),
 ("pl_always",tcPl,0),("pl_always",tcSubsOnly,4),("pl_if_good",tcSubsOnly,1),("pl_if_good",tcPl,0),
 ("pl_always","X 1080p\n🔊 aac · 2.0 · 🇯🇵🇱🇹\n💬 🇬🇧 https://x",8),("pl_always","X 1080p\n🗣️ 🇬🇧🇵🇱 https://x",1),
 ("pl_always","X 1080p Film.2024.PL.1080p\n🔊 ac3 · 🇺🇸\n💬 🇵🇱 https://x",5),
 ("pl_always","Torrentio 4k DV Film.2024.PL.2160p.WEB-DL\n👤 12 💾 20 GB ⚙️ X https://x",0),("pl_always","Torrentio 1080p Film.2024.1080p.Lektor.PL\n👤 3 https://x",1),
 ("pl_always","Torrentio 1080p Film.2024.1080p.PLSUB\n👤 3 https://x",5),
 ("pl_always","TorrentClaw (PL, 720p) ⚡️ INSTANT 1080p · Web-dl\n🔊 eac3 · 5.1\n💾 3 GB Film.2024.1080p.WEB-DL.ENG https://torrentclaw.com/x",8),
 ("pl_always","Addon 1080p [DEVIL-TORRENTS.PL] Film.2024.ENG.1080p https://x",8),("pl_always","Addon Film [PL] 1080p https://x",1),
 ("pl_always","Addon Film.2024.1080p.ENG https://cdn/x/video.ts",8),("pl_always","X 1080p DTS-HD MA English",8),("pl_if_good","X DTS-HD MA English",11),
 ("pl_always","TorrentClaw (PL, 720p) 🌐 4K · Bluray ⭐\n🔊 ac3 · 🇵🇱"+HASH,11),("pl_always","TorrentClaw (PL, 720p) 🌐 4K · Bluray\n🔊 ac3 · 🇺🇸"+HASH,13),
 ("pl_if_good","TorrentClaw (PL, 720p) ⬇️ Download 4K · Bluray\n🔊 ac3 · 🇵🇱 https://x",12),("original_subs","TorrentClaw 🌐 1080p"+HASH,4),
]
bad = [(p, t[:70], e, tier(p, t)) for p, t, e in cases if tier(p, t) != e]
for b in bad: print("ŹLE", b)
print(f"{len(cases)-len(bad)}/{len(cases)} OK")
for t in ["Film 1080p CAM PL","Film HDCAM","Film.TS.2023","Film 1080p TELESYNC Lektor"]:
    for p in P: assert tier(p, t) is None, (p, t)
print("cam/ts odpadają: OK")
