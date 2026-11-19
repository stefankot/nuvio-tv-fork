# B06a Skórka z układami Nuvio KE (budżet 4 h + 4 h)

Cel: układ ekranu głównego z Nuvio KE dostępny jako przełączana skórka, bez zmiany rdzenia. Kolejne ekrany dochodzą po jednym.

Źródło: `https://github.com/Arshad-Kamal/Nuvio-KE` (sklonuj do `../nuvio-ke`, tylko odczyt; analiza 17.11 na commicie `16a89f4`; przypięta wersja w `fork-tools/pins.env` to `KE=0a87a08`, użyj przypiętej, chyba że się nie kompiluje). `KE` = `../nuvio-ke/app/src/main/java/com/nuvio/tv`, `J` = `app/src/main/java/com/nuvio/tv`.

Co wiadomo z analizy:

- KE nie ma przełączanych układów. Cały interfejs jest przepisany na własny zestaw komponentów „Marquee” (`KE/ui/components/marquee`, 24 pliki), używany w ok. 100 plikach ekranów. Nie da się go „włączyć”; trzeba go przenieść jako skórkę.
- Fork ma gotowy kontrakt skórki: `J/skin/api` (`Skin.screen(route)`, `Skin.PlayerChrome`, `SkinId`, `SkinRegistry`), wpięcia przez `ForkHooks.skinScreen` i `skinPlayerChrome`, pusty `KitSkin`. Ekran, którego skórka nie ma, wraca do zwykłej aplikacji.
- Ekran główny KE to 5 plików `KE/ui/screens/home/Marquee*.kt` (ok. 2540 linii). Z 56 importów aplikacji 32 nie istnieją w forku: 20 to komponenty Marquee, 5 to `ui/components/trailerpreview`, reszta to pojedyncze symbole (`LocalTopBarFocused`, `TrailerAutoplayMode`, `isPlaceholderItemId`, `catalogPosterImageCacheKey`, `rememberAmbientAccentProvider`, `rememberFolderCoverFallback`). `HomeUiState` i `ModernHomePresentationState` różnią się od wersji w forku, więc potrzebny jest adapter.
- Live TV z KE (`KE/livetv/ui`, 18 plików) jest związane z własnym silnikiem KE: 57 z 85 importów nie istnieje w forku, a poprzednia próba skończyła się 1391 błędami. **Nie przenosić.** Telewizja zostaje na ekranach z B03.

## Zadania

- [ ] **B06a-1 Nowa skórka w kontrakcie.** Dodaj `SkinId.MARQUEE("marquee")` i obiekt `MarqueeSkin` w `J/skin/marquee`, zarejestrowany w `SkinRegistry`. Pozycja „Wygląd” w ustawieniach dostaje czwartą wartość „Układ KE”. Pusta skórka = aplikacja bez zmian.
  Ukończone, gdy: test JVM `SkinId.fromKey`; `fork-tools/check.sh` zielony.

- [ ] **B06a-2 Zestaw komponentów Marquee.** Skopiuj `KE/ui/components/marquee/*` do `J/skin/marquee/components` (zmiana pakietu). Zasada izolacji jak dla `skin/kit`: komponenty nie importują niczego z `com.nuvio.tv.*` poza `com.nuvio.tv.skin.*`. Importy łamiące tę zasadę zastąp parametrami albo małymi kopiami w `J/skin/marquee/shims` (zapisz każdą w `UPSTREAM_SOURCES.md`). Zasoby (napisy, grafiki) do `res/values/marquee_strings.xml` i osobnych plików. Limit: 4 cykle kompilacji; przy ponad 40 błędach zablokuj blok i przejdź dalej.
  Ukończone, gdy: kompiluje się; `check.sh` potwierdza izolację.

- [ ] **B06a-3 Ekran główny.** Skopiuj `Marquee*.kt` do `J/skin/marquee/screens/home`. Adapter `MarqueeHomeAdapter`: z `HomeUiState` i stanu prezentacji forka buduje parametry `MarqueeHomeContent` (lista parametrów w `KE/ui/screens/home/MarqueeHomeContent.kt` od linii 72). Podgląd zwiastunów (`trailerpreview`): przenieś tylko, jeśli fork ma odpowiednik danych; inaczej przekaż puste mapy. `MarqueeSkin.screen("home")` zwraca ten ekran. Rzędy modułów z rejestru (telewizja, polecenia) muszą być widoczne także w tym układzie.
  Ukończone, gdy: test JVM adaptera (rzędy, „oglądaj dalej”, puste stany); pozycja na liście TV z porównaniem do zrzutu `../nuvio-ke/assets/screenshots/hero.jpg`.

- [ ] **B06a-4 Raport dla kolejnych ekranów.** Dla tras `detail`, `streams`, `search`, `library`, `settings`, `profile`, `player` policz jak wyżej: pliki KE, linie, brakujące symbole, różnice stanu. Zapisz w `plan/raporty/skorka-ke.md` z oceną kosztu każdej trasy i kolejnością od najtańszej.
  Ukończone, gdy: raport istnieje.

- [ ] **B06a-5 (dodatkowe 4 h) Dwie najtańsze trasy z raportu.** Przenieś je tym samym sposobem: kopia ekranu, adapter, rejestracja w `MarqueeSkin`.
  Ukończone, gdy: testy JVM adapterów; pozycje na liście TV.

## Uwagi

- Licencja KE: sprawdź plik `LICENSE` w repozytorium KE i dopisz pochodzenie do `UPSTREAM_SOURCES.md`.
- Skórka właściciela (`KitSkin`) ma pierwszeństwo projektowe. Układ KE jest drugą, niezależną skórką i nie zmienia `skin/kit`.
