# Wydajność: pomiar bazowy forka (wstępny, 17.11.2026)

Urządzenie: Xiaomi TV Box 3rd gen `MiTV-AFMU0` (Android 14, armeabi-v7a, 2 GB RAM, swap ok. 515-545 MB w użyciu), build debug b96 (`com.nuviodebug.com`, 102 MB). Sterowanie `adb input` samo obciąża procesor, liczby to górna granica. Playlista: 321 kanałów (`hls.gd/pl/...`), EPG `epg.ovh/pl.gz` (8,5 MB gz).

## Live TV (po załadowaniu listy i przewodnika)
| Test | Wynik |
|---|---|
| 20 × ▼ po liście/siatce Live TV (co 150 ms) | 179 klatek, **47% zacinających się** (84), p50 40 ms, p90 200 ms, p95 250 ms, p99 350 ms; pominięte vsync 59; „slow UI thread” 84; „high input latency” 190 |
| 20 × ▶ w przewodniku (co 150 ms) | 37 klatek, 11% zacinających się (4), p50 11 ms, p90 200 ms, p99 250 ms; „high input latency” 34 |
| Start kanału z listy (OK) | `startInitialPlayback` +0,3 s po klawiszu; ExoPlayer Init +0,9 s; dekoder wideo (c2.amlogic.avc) utworzony +1,4 s; czas do pierwszej klatki: dodano znacznik `ForkPerf` (patrz niżej), pomiar w następnym buildzie |
| Pamięć | PSS 260-330 MB, wolne ok. 150 MB (RAM) przy 545 MB swapu |
| EPG | `LiveTvEpg: begin generation=2 guides=2 channels=321`; „saved programmes: miss (no file)” przy pierwszym wczytaniu |

## Wnioski wstępne
- 47% zacinających się klatek przy przewijaniu listy kanałów to wartość porównywalna z Live TV 42% w pomiarze Kicii na JMGO (p50/p90/p99 = 7/34/73 ms); na Xiaomi p90 jest 6× gorsze (2 GB RAM + swap).
- Podejrzenia do sprawdzenia w pierwszej kolejności: podgląd kanału odtwarzany w liście (przełącznik „Channel previews” domyślnie włączony), nakładka modułów na całe okno, rozgrzewka przewodnika (`TvModule`), obciążenie pamięci/swap.
- Znacznik pomiaru: `adb logcat -s ForkPerf` (`first_play start_to_play_ms=… key_to_play_ms=…`) od buildu b97.
