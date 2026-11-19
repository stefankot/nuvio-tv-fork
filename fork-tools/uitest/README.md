# Testy interfejsu (19.11.2026) — przez uiautomator, bez zrzutów ekranu

Urządzenie: JMGO (`DEV=192.168.31.252:5555`, APK armeabi-v7a) — tylko gdy nikt nie ogląda
(`adb shell dumpsys window | grep mCurrentFocus`: na wierzchu ma być `com.nuviodebug.com` i nic nie odtwarzane).
Emulator NIE: ma stary build97 arm64; bieżące APK są tylko arm32 (właściciel 19.11).

## 1. Obchód pilotem — `crawl.py`
`DEV=192.168.31.252:5555 python3 fork-tools/uitest/crawl.py [trasa ...] [--steps 120]`
- Otwiera trasę intencją debug `com.nuvio.fork.OPEN`, naciska tylko strzałki (nigdy OK), buduje mapę przejść fokusu.
- Raport `wyniki/<data>-<urządzenie>/raport.md`: czas ładowania (>25 s = nie załadowało się), elementy nieosiągalne,
  utrata fokusu, pułapki, wyjście z aplikacji, ostrzeżenia/błędy z logcat (obrazki, sieć, wyjątki).
- Próba na emulatorze przerwana (stary build); odczyt i rozpoznanie fokusu działa (ekran główny: fokus „Odtwórz”, 9 elementów).

## 2. Diagnostyka odtwarzania — `playdiag.py` + `fork/diag/PlaybackDiag.kt`
- `PlaybackDiag` (wołane z `PlayerBridge.set`, zero zmian w bazie) loguje „ForkDiag”: ścieżki audio/napisy oferowane i wybrane,
  HDR, jakość, opis źródła, preferencja (`SourceChoice`), `mismatch=` gdy preferencja niespełniona mimo dostępnej ścieżki.
- `python3 playdiag.py --clear` → odtwórz film → `python3 playdiag.py` (+ ForkPerf: czas startu; dekoder z logów ExoPlayera).
- NIESKOMPILOWANE — wchodzi do najbliższej dużej paczki.

## 3. Ścieżki użytkownika — do napisania po pierwszym obchodzie (scenariusze z pomiarem czasu i logami).

## Ustalenia z kodu: postęp oglądania programów TV (`fork/tv`)
Zapis działa (`LiveTvPlayerOverlay` co 10 s → `TvBridge.onPosition` → `TvData.onPosition`), wyświetlanie ma błędy:
1. `CellMark` (`fork/tv/ekrany/CellMark.kt`): `remember(title, startMs)` liczy znacznik raz — jeśli komórka powstała,
   zanim dane się wczytały albo zanim obejrzano, pasek nie pojawi się do ponownego otwarcia przewodnika.
2. `TvData.recSnap` wypełnia się dopiero w `ensureLoaded()`, wołanym z `onGuide` (tylko gdy jakiś kanał ma archiwum),
   `onPlay` albo `onPosition`. Bez archiwum: po starcie aplikacji przewodnik nie ma znaczników, dopóki nie włączy się kanału.
3. Znacznik jest tylko w siatce przewodnika (`LiveTvGuide.kt:924`); nie ma go na kartach Kicii (półka „Dla Ciebie”,
   mini‑przewodnik, karta programu). Pasek ma 2 dp — na TV z daleka prawie niewidoczny.
4. „W trakcie” dopiero po 2 min oglądania (`WatchRules.inProgressMinutes`); wcześniej brak śladu.
Poprawki NIE wykonane — czekają na decyzję właściciela.
