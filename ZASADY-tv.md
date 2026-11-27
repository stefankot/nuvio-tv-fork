# ZASADY — testy na urządzeniu (JMGO, emulator) + ściąga poleceń (czytaj przed testami)

Część jedynego źródła zasad (rdzeń: `ZASADY.md`). Nowe zasady o testach dopisuj tutaj, z datą. Ściąga na końcu to wiedza praktyczna, nie zasady — poprawiaj ją swobodnie.

## Zasady
- Ustawienia urządzeń testowych (24.11, właściciel; nadrzędne): zmieniaj ustawienia JMGO i emulatora tak, żeby praca była wydajna (np. perfetto, opcje programisty), bez pytania i bez przywracania; każdą zmianę zapisz jednym zdaniem w `plan/DECYZJE.md`. Zmiany na profilu właściciela w aplikacji (obejrzane odcinki, pozycje, ustawienia) wymień w raporcie.
- Tylko fork `com.nuviodebug.com` (nie `com.nuvio.tv`), start: `am start -n com.nuviodebug.com/com.nuvio.tv.launcher.AppIconDefault`. Nie ruszaj cudzych aplikacji i danych.
- Po instalacji: md5 z urządzenia (`adb pull` ścieżki z `pm path`) = md5 pliku z buildu, potem `cmd package compile -m speed -f com.nuviodebug.com`.
- JMGO: `adb connect 192.168.31.252:5555` (Android 11, armeabi-v7a, profil właściciela zalogowany). Ostrożnie — ktoś może oglądać.
- Czekanie na wolny JMGO (24.11, właściciel: „godzina czekania na nic”): dźwięk ekranu głównego JMGO (`com.jmgo.arwen`) to NIE oglądanie (`jmgo_czekaj_wolny.sh` go pomija). Wystarczy 1 min spoczynku pilota. Czekanie > 10 min → od razu sprawdź, co blokuje (wierzch, odtwarzacz w `dumpsys audio`); gdy na wierzchu jest ekran główny JMGO albo fork, a pilot nieużywany — testuj (`--bez-czekania`). Gdy właściciel mówi, że jest przy TV albo pyta, czemu nic się nie dzieje — testuj od razu. Nigdy nie czekaj w ciszy.
- Projektor dzielony między wątki: `.stage/jmgo_zajety.txt` (kto używa) — wpisz się przed testami, usuń po.
- Testy tekstowo (`fork-tools/test/{ui,tap,go,focus,fx2,kf}.sh`, uiautomator); zrzuty ekranu tylko wyjątkowo. OK tylko, gdy fokus jest na celu, nigdy na ślepo — przed KAŻDYM OK sprawdź fokus (`fx2.sh` / log `ForkFocus`). Po starcie aplikacji Główna wczytuje się kilka sekund (26.11 OK na ślepo włączyło filmy na profilu właściciela).
- Emulator tylko na testy, nigdy razem z Gradle (8 GB RAM); po testach `adb emu kill`. Logowanie QR na emulatorze nie działa → „Continue without account”.
- Czego nie da się sprawdzić bez TV → dopisz na końcu `plan/DO_SPRAWDZENIA_NA_TV.md`.

## Pętle testów (22.11, 24.11)
- Pętla: testy ≥ 10 min → lista błędów APLIKACJI (z logów, bez błędów testera) w ciągu 15 min → poprawki w kodzie → jeden build → przebieg. Jeden przebieg na jeden build; bez zmiany w `app/` nie ma kolejnego przebiegu.
- Po KAŻDEJ pętli zapisz stan w katalogu raportu (`plan/ux/<temat>/WYNIKI.md`), wyślij go właścicielowi (SendUserFile), commit + push.
- Nowe lub zmienione narzędzie testowe najpierw na JEDNEJ ścieżce, porównane z poprzednim wynikiem tej ścieżki; pełny przebieg dopiero, gdy się zgadza. Poprawiony tester → najpierw ponowna ocena istniejących logów.
- Logcat przed zapisem do repo: usuń adresy `http(s)://…` (login i hasło dostawcy IPTV).

## Ściąga
- adb „No route to host”, a `nc -z 192.168.31.252 5555` działa: `adb kill-server; adb start-server` i połącz ponownie; jeśli serwer adb należy do sesji z emulatorem — osobny serwer: `ANDROID_ADB_SERVER_PORT=5038 ADB_EMU=0 ADB_LOCAL_TRANSPORT_MAX_PORT=5553 adb connect 192.168.31.252:5555`. Instalacja: `fork-tools/test/jmgo_adb.sh` / `jmgo_instaluj.sh` (`adb install -r`).
- Klawisze: `fork-tools/test/kf.sh KLAWISZ...`; fokus: `fork-tools/test/fx2.sh [n]`. Szybki odczyt ekranu: stały czytnik `fork-tools/uitest/fastdump/` (`FASTDUMP=0` wyłącza).
- Zrzut (nakładka/siatka — uiautomator ich nie widzi): `fork-tools/test/r.sh NAZWA [klawisze…]` albo `adb shell screencap -p /sdcard/s.png` + `adb pull` + `sips -Z 960` → `plan/ux/reczne/NNN-*.png`.
- Ustawienia aplikacji z Maca (build debug): `adb forward tcp:8765 tcp:8765`, `fork-tools/prefs.sh get|set|find|pull|push`.
- Deep linki (z Głównej): `nuvio://detail/movie/ttID`, `nuvio://detail/series/ttID`.
- Odtwarzacz Niuni: ▼ = przyciski, potem `DPAD_UP DPAD_RIGHT×3 DPAD_CENTER` = Więcej; ▲ z paska = Dźwięk. Przewinięcie odcinka: `adb shell 'for i in $(seq 1 250); do input keyevent DPAD_RIGHT; done'` (~10 s na klik).
- Narzędzia VOD (`.stage/vod/`, poza gitem): `zrodla.py <movie|series> <id[:s:e]>` (oczekiwany plik wg reguł z projektora; wymaga `adb forward tcp:8765 tcp:8765`), `napisy.py series <id:s:e>…` (polskie napisy w dodatkach), `porownaj.py` (stare vs nowe reguły na zapisanych listach).
- Czas metod na wątku UI: `adb shell am profile start --sampling 2000 com.nuviodebug.com /data/local/tmp/x.trace` → przejście → `am profile stop` → `adb pull` → `python3 -I fork-tools/uitest/dmtrace_top.py x.trace main com.nuvio 40` (przy profilowaniu kanał IPTV może dostać „Niedostępny”).
- Perfetto na JMGO (usługa śledzenia włączona 24.11, `persist.traced.enable=1`): ślad `fork-tools/uitest/perf_jmgo.sh perfetto`, odczyt `fork-tools/uitest/perfetto_plasterki.py <ślad> <pid>`; klatki: `dumpsys gfxinfo … framestats` + `framestats_faza.py`. Budżety czasu JMGO: `plan/DECYZJE.md`, sekcja „Wydajność”.
- JMGO: strefa czasowa CST (+7 h względem Polski); bufor logcat 16 MB. Emulator Google TV API 34 (arm64, bez roota): długie OK `input keyevent --longpress`.
