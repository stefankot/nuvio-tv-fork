# Wydajność: pomiar bazowy forka (wstępny, 17.11.2026)

Urządzenie: Xiaomi TV Box 3rd gen `MiTV-AFMU0` (Android 14, armeabi-v7a, 2 GB RAM, swap ok. 515-545 MB w użyciu), build debug b96 (`com.nuviodebug.com`, 102 MB). Sterowanie `adb input` samo obciąża procesor, liczby to górna granica. Playlista: 321 kanałów (`hls.gd/pl/...`), EPG `epg.ovh/pl.gz` (8,5 MB gz).

## Live TV (po załadowaniu listy i przewodnika)
| Test | Wynik |
|---|---|
| 20 × ▼ po liście/siatce Live TV (co 150 ms) | 179 klatek, **47% zacinających się** (84), p50 40 ms, p90 200 ms, p95 250 ms, p99 350 ms; pominięte vsync 59; „slow UI thread” 84; „high input latency” 190 |
| 20 × ▶ w przewodniku (co 150 ms) | 37 klatek, 11% zacinających się (4), p50 11 ms, p90 200 ms, p99 250 ms; „high input latency” 34 |
| Start kanału (OK na liście, potem 7 × ▼ w odtwarzaczu co 7 s) | `ForkPerf first_play start_to_play_ms` = 608 / 707 / 629 / 691 / 550 / 605 / 643 ms (adres strumienia → odtwarzanie; do tego ok. 0,3 s od klawisza do `startInitialPlayback`); host 12.hls.gd; **kanały startują szybko, bez opóźnienia ponad 1 s** |
| Bezczynność Live TV 30 s | 2 klatki/30 s, CPU aplikacji 0% (OK; w przeciwieństwie do Kicii WY-406) |
| Home (Modern): 20 × ▼ (co 150 ms) | 229 klatek, **47% zacinających się** (108), p50 38 ms, p90 150 ms, p99 300 ms; „slow UI thread” 107; GPU p50/p90/p99 = 8/10/14 ms, więc wąskie gardło to wątek UI/CPU, nie GPU |
| Pamięć | PSS 260-330 MB, wolne ok. 150 MB (RAM) przy 545 MB swapu |
| EPG | `LiveTvEpg: begin generation=2 guides=2 channels=321`; „saved programmes: miss (no file)” przy pierwszym wczytaniu |

## Wnioski wstępne
- 47% zacinających się klatek przy przewijaniu listy kanałów to wartość porównywalna z Live TV 42% w pomiarze Kicii na JMGO (p50/p90/p99 = 7/34/73 ms); na Xiaomi p90 jest 6× gorsze (2 GB RAM + swap).
- Podejrzenia do sprawdzenia w pierwszej kolejności: podgląd kanału odtwarzany w liście (przełącznik „Channel previews” domyślnie włączony), nakładka modułów na całe okno, rozgrzewka przewodnika (`TvModule`), obciążenie pamięci/swap.
- Znacznik pomiaru: `adb logcat -s ForkPerf` (`first_play start_to_play_ms=… key_to_play_ms=…`) od buildu b97.
