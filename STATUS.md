# Stan prac: fork Nuvio Android TV (16.11.2026)

Baza: oficjalny NuvioTV, gałąź `dev`, commit `2c853c1` (w gicie: "Baseline NuvioTV 2c853c1", `85f9010aa`).
Pochodzenie plików: `UPSTREAM_SOURCES.md`. Zasady i preferencje: `../CLAUDE.md`.

## Gałęzie (git, katalog NuvioFork)
| Gałąź | Zawartość | Kompiluje |
|---|---|---|
| `rs-module` | baza + Reshaped jako moduł (759 nowych plików, 74 pliki rdzenia zmienione, bez aktualizatora Reshaped, z DV/HDR10) | tak, APK zbudowany |
| `yt` | rs-module + Nuvio One YouTube (mediaservicecore, pakiet `youtube`, trasa, adaptery) | tak |
| `echo` | yt + Echo `AutoQualityPolicy`, `StreamQuality` (czysta logika, niepodpięta) | tak |
| `ke2` | echo + KE katalog IPTV (pack z GitHuba + iptv-org), most do Reshaped, skan łączy, przełączniki w ustawieniach | tak (aktualna, zbiorcza) |
| `main` | STARY monolit Reshaped nałożony w całości (porzucony, nie spełnia wymogu modułu) | tak |
| `ke` | pierwsza próba KE (cały Live TV KE), porzucona | nie (1391 błędów) |
| `wip-all` | surowe kopie Arc, Echo, KE, One (archiwum) | nie |

## Wdrożone (kompiluje się, nie sprawdzone na telewizorze)
- Reshaped: IPTV (M3U/Xtream/Stalker), EPG, catch-up, szklana nawigacja, synchronizacja napisów z audio, drugi język, rozpoznawanie mowy (model angielski), lokalne miniatury, Dolby Vision, HDR10.
- Nuvio One: YouTube (przeglądanie, odtwarzanie w oficjalnym odtwarzaczu, ustawienia).
- KE: katalog IPTV z kategoriami/krajami/językami (repozytorium `Arshad-Kamal/Nuvio-KE-channels` + metadane iptv-org), dodawanie kraju jako źródła Live TV, filtry kategoria/język, test łączy kanałów.
- Echo: tylko czysta logika Auto Quality (niepodpięta).
- Ustawienia z forków pod jedną kategorią (sekcja Reshaped): KE katalog, YouTube.

## Zaparkowane (za duża ingerencja w rdzeń)
- Echo: wyścig źródeł (`AutoPlayRace`), zamiana w locie (`HotSwap`), przełączanie źródeł, fallback jakości, hedge auto-play, ekran ustawień wydajności. Wymagają ~40 pól stanu w kontrolerze odtwarzacza i selektora Echo.
- KE: testowanie sprzętu i łącza urządzenia (`DeviceAssessmentEngine`, `Applier`, `DeviceWelcome`, `ServiceNetworkTestRunner`, `StreamSpeedTester`). 441 błędów, powiązane z ustawieniami odtwarzacza KE.
- KE: przełączana powłoka, Home, Layout Settings, TV Home (nie zaczęte).
- Arc: kalendarz (2172 błędów), wyszukiwarka ustawień, miniatury, cache przewijania, diagnostyka, Home Catalog Order.
- Arc: pilot (`RemoteButtons`) w całej aplikacji, także w IPTV (w toku: analiza przeniesienia zadań przycisków na zdarzenia odtwarzacza).
- YouTube: miniatury podglądu (storyboard) i raport diagnostyczny zastąpione pustymi adapterami.

## Decyzje użytkownika (16.11, wieczór)
- KE: reszta ZAPARKOWANA (powłoka/Home/Layout/TV Home, testowanie sprzętu). W zakresie KE zostają tylko katalog IPTV, filtry kanałów i skan łączy.
- Arc miniatury: bierzemy implementację z Reshaped (`seekpreview/engine`, `ArcPreviewAdapter`, `local`, `cpp/seekpreview`), nie kopiujemy `core/player/thumbnail` z Arc (60 plików). Mniej ingerencji w rdzeń. Przełącznik: sekcja Reshaped, `seekPreviewSettingsItems`.
- Arc pilot: w toku na gałęzi `arc-remote` (mapa przycisków + hook obok Live TV w PlayerScreen).

- Arc: Home Catalog Order: JUŻ W BAZIE (`ui/screens/addon/CatalogOrder*`, wersja dev jest nowsza od Arc). Nic do dodania.
- Arc: cache przewijania (= cache podglądu przy przewijaniu/seek): pokrywa go silnik podglądu z Reshaped (ta sama decyzja co miniatury).
- Arc: diagnostyka (panel statystyk): fork ma już `PlayerDebugStatsOverlay` (bogatszy niż Arc); przycisk pilota STATS przełącza go (`OnTogglePlayerStatsHud`).

## Zredukowane względem Reshaped
- Aktualizator Reshaped usunięty (polecenie użytkownika), wraca natywny.
- Pominięte ~125 zmian Reshaped poza odtwarzaczem (debrid, Trakt, biblioteka, serwer addonów itp.).
- Limit ok. 35 plików rdzenia przekroczony (74).

## Do dodania (lista użytkownika, kolejność)
1. Arc pilot w całej aplikacji.
2. Arc: miniatury, cache przewijania, diagnostyka, Home Catalog Order, wyszukiwarka ustawień, kalendarz.
3. KE: testowanie sprzętu i łącza, powłoka/Home.
4. Echo: wpięcie w odtwarzacz.
5. Model rozpoznawania mowy wielojęzyczny (wybór w ustawieniach: Whisper wielojęzyczny, Parakeet TDT 0.6B v3; pliki hostowane u nas). Obecny model: zipformer GigaSpeech 2023-12-12, tylko angielski, 74 MB.

## Budowanie
```
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :app:assembleFullDebug -PforgeArm64Only=true --console=rich
```
APK: `app/build/outputs/apk/full/debug/app-full-arm64-v8a-debug.apk`. Gradle ma `-Xmx3072m` (Mac 8 GB RAM).

## Koniec wątku 1
- `arc-remote`: pilot Arc + wybór modelu mowy (Whisper) zacommitowane jako WIP; ostatnia kompilacja (build39) w toku, wynik nieznany. Szczegóły i procedura: `../CLAUDE.md`.

## 16.11.2026 – wątek 2
- Krok 1 planu architekta wykonany: `arc-remote` kompiluje się (build40), APK zbudowany (arm64, 105 MB). Pakowanie za pierwszym razem padło bez winy kodu, powtórka przeszła.
- Nic nie uruchomiono na emulatorze. Następny: krok 2 (pierwsze uruchomienie, checks 1–5, 7), potem krok 3 (gałąź `final`, `fork-tools/`).

## Krok 2 planu: pierwszy test na emulatorze (16.11.2026, fork `com.nuviodebug.com`, skrót APK sprawdzony)
- 1 Start: OK (onboarding, Home z katalogami, 0 crashy).
- 2 Kategoria „Nuvio Reshaped": OK (AutoSync, Arc, Live TV, KE, YouTube, pilot, model mowy).
- 3 Live TV: OK (M3U iptv-org PL załadowane, lista kanałów, kanał przechodzi w PLAYING).
- 4 KE: przełącznik działa, bez crasha; zawartość katalogu NIEZWERYFIKOWANA.
- 5 YouTube: crash po włączeniu przełącznika (zagnieżdżone przewijanie w `KeCatalogSettingsItems.kt`), NAPRAWIONY (Box height 900.dp), OneTube się otwiera; zawartość wymaga logowania.
- 7 Model mowy: wybór działa (cykl modeli, Whisper tiny); trwałość po restarcie i pobieranie NIEZWERYFIKOWANE.
- Logowanie QR nie działa: build łączy się z Supabase pod localhost:443 (brak konfiguracji kluczy).
- Następny krok: 3 (gałąź `final`, `fork-tools/`), potem 4–11.

## Krok 3 planu (16.11.2026): gałąź `final`, `vendor/nuvio`, `fork-tools/`
- `vendor/nuvio` = 85f9010aa (czysta baza). `final` = praca od teraz.
- `fork-tools/`: pins.env, overrides.txt (68 plików identycznych z Reshaped), hooked-overrides.txt (4), base-hooks.txt (3), hooks.tsv, check.sh. `bash fork-tools/check.sh` → OK (ręcznie utrzymywane: 7, limit 35).

## Kroki 4–5 (16.11.2026): ForkHooks + pilot w całej aplikacji
- `fork/ForkHooks.kt`, `fork/arc/RemoteGlobalHook.kt`; hook w `MainActivity.dispatchKeyEvent` (1 linia). `ForkFeatures.enabled(c,key,default)`.
- Poza odtwarzaczem pilot wycisza tylko przyciski przypisane do „Nic"; reszta zadań nie ma odpowiednika poza odtwarzaczem. W odtwarzaczu i Live TV działa hook w PlayerScreen.
- Kompiluje się (build45), check.sh OK (ręcznie utrzymywane: 8). Niezweryfikowane na urządzeniu.

## Kroki 6–7 (16.11.2026)
- B: wiersz „Test łącza i raport sprzętu" w kategorii forka (`DeviceTestSettingsItems.kt`); sam test i raport są w bazie (Ustawienia → Advanced). Kompiluje się.
- E: wyszukiwarka ustawień z Nuvio One (`SettingsSearch.kt`, `SettingsSearchOverlay.kt`) + 3 hooki w `SettingsScreen.kt` (pasek, szyna, overlay). Wynik „Playback → sekcja" otwiera tylko kategorię (fork nie ma `initialPlaybackSection`). Kompiluje się (build47), niezweryfikowane na urządzeniu. Zmiany w kopii: `UPSTREAM_SOURCES.md`.
