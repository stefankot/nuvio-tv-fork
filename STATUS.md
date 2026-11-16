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
