# ZASADY — kod, pliki bazy, wpięcia (czytaj przed zmianą kodu, o której mówi ZASADY.md §0)

Część jedynego źródła zasad (rdzeń: `ZASADY.md`). Nowe zasady o kodzie dopisuj tutaj, z datą.

## Pliki bazy NuvioTV (26.11, właściciel: „nie zmieniaj plików bazy, tylko kopiuj je 1:1. masz zakaz ich edycji, chyba że wystąpi krytyczny błąd bez zmiany. ogranicz sobie na początku ilość poprawek”)
- Przy aktualizacji bazy każdy plik NuvioTV = kopia 1:1 z wydania (`vendor/nuvio`); wersje Reshaped plików bazy już nie obowiązują. Nie podmieniaj plików bazy wersją z forka.
- W pliku bazy wolno tylko:
  1. nałożyć istniejące wpięcia i łatki forka (`fork-tools/hooks.tsv`, `fork-tools/patches`);
  2. jednoliniowe wpięcia Reshaped, od których zależą IPTV, menu pigułki (Niunia) i strona ustawień Reshaped — najwyżej 50 linii na aktualizację;
  3. poprawkę krytycznego błędu (brak kompilacji, awaria, działająca funkcja przestaje działać, a w kodzie forka nie da się tego obejść) — najwyżej 10 linii na aktualizację.
- Każda linia z (2) i (3) trafia do `plan/DECYZJE.md` z powodem.

## Wpięcia i łatki
- Wpięcie w bazę = jedna instrukcja z komentarzem `// Fork hook: <moduł>`, wiersz w `fork-tools/hooks.tsv`, zmiany jako łatki w `fork-tools/patches/<seria>/<plik>/NN-opis.patch` (łatki jednego pliku nie dotykają innych).
- Przed commitem `bash fork-tools/check.sh` (limit plików bazy z wpięciami: 65; check.sh dopuszcza w nich tylko dopisywanie linii).
- Limit jest praktycznie wyczerpany — nowe rzeczy przez istniejące wpięcia: `ForkHooks.moduleOverlays` (nakładki, start usług), `ForkHooks.screensWrap` (korzeń ekranów), `ForkHooks.playerBridge` (ten sam `PlayerViewModel`), `ForkHooks.onGlobalKey` (klawisze), wpięcia skórki (`skinScreen`, `PlayerOverlay`, `detail`, `home`).
- Zmienione wartości domyślne bazy: przez moduł pierwszego uruchomienia (`FirstRunDefaults`), nie przez edycję plików bazy.
- Poprawka istniejącej funkcji (nie nowa funkcja): wyłącznik tylko kluczem `ForkFeatures`, bez pozycji w ustawieniach.
- Testy bazy sprzeczne z forkiem: nie zmieniaj ani nie usuwaj — dopisz do `fork-tools/test/wykluczone-testy-bazy.txt` (wzorzec + powód).

## Kopie z forków
- Kopie z forków (KE, Echo, Reshaped, Arc, One) 1:1 z przypiętym SHA w `UPSTREAM_SOURCES.md`; upstream tylko do odczytu: `../upstream/{nuvio,ke,reshaped,arc,echo,one}` (27.11 jest tylko `reshaped`).
- Kicia (`~/Desktop/nuvio-tv-kicia`) jest zamrożona: jej kod można dowolnie dopasować.
- Reshaped wymaga ffmpeg z `app/libs/*.aar`. Stringi Arc są w osobnych plikach `values/strings_*.xml`.

## Wygląd i efekty
- Wygląd: istniejące komponenty; skórki przez `skin/` (właściciel robi własny projekt).
- Efekty (rozmycie itp.) tylko natywnie (RenderEffect API 31+, RuntimeShader API 33+); na starszych (JMGO, Android 11) zwykłe tło z tokenów skórki, bez symulacji.
- Wyjątek (22.11, właściciel: „Tak, włącz”): rozmycie OBRAZKÓW (tło hero, plakaty) liczone raz przy wczytaniu i trzymane w pamięci Coil (`fork/images/NativeBlur`) działa też na Androidzie 11 — procesorem, na obrazie pomniejszonym 4×. Rozmycie na żywo w każdej klatce na Androidzie 11 nadal zakazane.

## Wydajność
- Ciężkie obliczenia (indeksy, kategorie, kolejności list, szukanie) nigdy przy każdym zapytaniu ani na wątku UI: liczone w tle, przyrostowo (tylko nowe/zmienione dane), zapisane na dysku; po restarcie tylko odczyt, pełne przeliczenie tylko przy większej zmianie (22.11). Docelowo SQLite (zgoda właściciela 22.11).

## Pułapki
- KSP ukrywa prawdziwe błędy („could not be resolved”): szukaj brakującej klasy w konstruktorze `@Inject`.
- Nie edytuj plików `app/` w trakcie buildu — zmiany do `.stage/`, potem łatka.
