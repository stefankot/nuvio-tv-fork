# B04 Źródła i napisy bez tarcia (budżet 4 h)

Cel: użytkownik nietechniczny nigdy nie widzi listy źródeł. Odpowiada raz na jedno pytanie, a aplikacja sama dobiera źródło, dźwięk i napisy.

Moduł: `J/fork/sources` (`J` = `app/src/main/java/com/nuvio/tv`). Korzysta z tego, co już jest w forku. Przed pisaniem przeczytaj: `docs` w Kicia `ranked-regex-autoplay.md` i `automatic-source-recovery.md`, oraz w forku `ui/screens/settings/StreamRegexPresets.kt`, `RankedRegexDialog.kt`, `AutoSourceRecoverySettingsItems.kt`, `core/player/recovery/`.

## Zadania

- [ ] **B04-1 Trzy zestawy reguł.** Dane w `assets/fork/source_presets.json`: dla każdej odpowiedzi lista reguł rankingu (do 20) oraz języki dźwięku i napisów.
  - „Zawsze po polsku”: polski dźwięk w dowolnej jakości (wzorce w nazwie i opisie źródła: PL, POL, POLISH, LEKTOR, DUBBING, DUB PL, MULTI z PL; bez dopasowań do „PL subs”, „napisy”), potem reszta.
  - „Po polsku przy dobrej jakości”: polski dźwięk w 1080p i wyżej, potem najlepsza jakość.
  - „Oryginał z napisami”: najlepsza jakość, bez reguł językowych; dźwięk oryginalny, napisy polskie.
  Każdy zestaw wyklucza CAM/TS (istniejący preset).
  Ukończone, gdy: test JVM na 30 przykładowych nazwach źródeł: właściwy szczebel dla każdej odpowiedzi; „PL subs” nie liczy się jako polski dźwięk.

- [ ] **B04-2 Pytanie w kreatorze i w ustawieniach.** Jedno pytanie na profil: „Co ważniejsze: polski dźwięk czy jakość obrazu?” z trzema odpowiedziami (DECYZJE #11). Odpowiedź zapisuje reguły rankingu, tryb wyboru źródła Regex, preferowany i drugi język dźwięku i napisów przez istniejące DataStore profilu. Gdy użytkownik zmieni reguły ręcznie w Advanced, pozycja pokazuje „Własne”. Pozycja ustawień: poziom PROSTY.
  Ukończone, gdy: test JVM „własne” i zapisu dla każdej odpowiedzi.

- [ ] **B04-3 Automatyczna próba innego źródła.** Włącz istniejącą funkcję (3 próby, 15 s) domyślnie dla poziomu Prostego przez moduł pierwszego uruchomienia. Nie zmieniaj jej logiki.
  Ukończone, gdy: test JVM wartości domyślnej.

- [ ] **B04-4 Sprawdzenie ścieżek po starcie.** Gdy profil chce polskiego dźwięku, po otwarciu strumienia sprawdź rzeczywiste ścieżki dźwiękowe. Brak polskiej: potraktuj jak nieudany start i poproś istniejący mechanizm o następnego kandydata, w ramach tego samego limitu prób. Po wyczerpaniu: graj najlepsze dostępne z polskimi napisami. Wpięcie przez `playerHandlers()` rejestru; jeśli mechanizm odzyskiwania nie ma publicznego wejścia, dodaj jedną funkcję z komentarzem `// fork-hook: sources`.
  Ukończone, gdy: testy JVM decyzji (jest polska / brak, są próby / brak prób).

- [ ] **B04-5 Pamięć serialu.** Źródło i grupa wydania, które zagrały z polskim dźwiękiem, mają pierwszeństwo w następnych odcinkach. Najpierw sprawdź istniejącą „ciągłość binge group” i „ostatni link”; jeśli pokrywają potrzebę, tylko je włącz dla tych profili.
  Ukończone, gdy: opis w `DECYZJE.md`, co wybrano, i test wartości domyślnej albo logiki.

- [ ] **B04-6 Linia „Gotowe”.** Przenieś `forge/ready/ReadyPick.kt`, `ui/forge/detail/ForgeReadyLine.kt`, `ForgeReadyViewModel.kt` z Kicia z testem `ReadyPickTest`. Dodaj do linii informację o języku: „Gotowe · 1080p · lektor PL” (z reguł B04-1). W poziomie Prostym lista źródeł jest dostępna tylko pod długim OK na Odtwórz. Jedno wpięcie w ekranie szczegółów.
  Ukończone, gdy: przeniesiony test zielony; test etykiety języka.

- [ ] **B04-7 Napisy z automatu.** Kolejność: polskie wbudowane, potem z dodatków. Dla napisów z zewnątrz włącz domyślnie istniejącą synchronizację z dźwiękiem (Reshaped). Sprawdź testem, że polskie znaki w kodowaniu windows-1250 i ISO-8859-2 czytają się poprawnie; jeśli nie, napraw w miejscu wykrywania kodowania.
  Ukończone, gdy: test JVM na dwóch próbkach kodowania.

- [ ] **B04-8 Przycisk „Napisy nie pasują”.** W menu napisów odtwarzacza jedna pozycja: bierze następny plik napisów w tym samym języku i uruchamia synchronizację ponownie. Po trzech użyciach proponuje wyłączenie napisów albo lektora AI (gdy B09 jest gotowy).
  Ukończone, gdy: test JVM kolejki napisów.

- [ ] **B04-9 Napisy dla seniorów.** Zestaw „duże napisy na półprzezroczystym tle” jako jedna pozycja poziomu Prostego (mały / średni / duży), zapisująca istniejące ustawienia wyglądu napisów.
  Ukończone, gdy: test JVM mapowania trzech rozmiarów.
