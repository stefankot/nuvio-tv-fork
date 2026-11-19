# B05 Ustawienia: poziom Prosty i tryb nocny (budżet 4 h)

Cel: 20 ustawień na jednym ekranie dla osoby nietechnicznej oraz tryb nocny działający według godzin.

Moduły: `J/fork/settings`, `J/fork/night` (`J` = `app/src/main/java/com/nuvio/tv`). Baza ma `ExperienceMode` (ESSENTIAL, ADVANCED); nie zmieniaj tego wyliczenia. Poziom Prosty to flaga modułu.

## Zadania

- [x] **B05-1 Ekran Prosty.** Trasa modułu. Gdy flaga „Prosty” jest włączona, wejście do Ustawień otwiera ten ekran (jedna linia `// fork-hook: settings` w nawigacji). Jeden ekran, bez szyny, 6 grup, dokładnie te pozycje (każda zapisuje do istniejących danych; pozycje modułów pochodzą z rejestru z poziomem PROSTY):

  | Grupa | Pozycje |
  | --- | --- |
  | Ty | Konto · Profil · Język aplikacji |
  | Oglądanie | Język dźwięku · Napisy · Rozmiar napisów · Następny odcinek automatycznie · Pomijaj czołówki · Polski dźwięk czy jakość (z B04) |
  | Tryb nocny | Tryb nocny · Godziny · Siła |
  | Telewizja | Źródła TV i program · Po uruchomieniu pokaż · Uruchamiaj z telewizorem |
  | Źródła | Dodatki · Połączone usługi |
  | System | Aktualizacje · Poziom ustawień · Pomoc |

  Maksimum 20 pozycji; test pilnuje limitu. Pozycje, których moduł jeszcze nie istnieje, nie są pokazywane.
  Ukończone, gdy: test JVM „nie więcej niż 20” i „pozycja zestawu pokazuje Własne po ręcznej zmianie”.

- [x] **B05-2 Przełącznik poziomu.** Pozycja „Poziom ustawień”: Prosty / Essential / Advanced. Essential i Advanced ustawiają istniejący `ExperienceMode` i wyłączają flagę. Zmiana poziomu niczego nie kasuje.
  Ukończone, gdy: test JVM trzech przejść.

- [x] **B05-3 Kategoria Moduły w Advanced.** Zastąp obecną sekcję forków jedną kategorią „Moduły”: podstrona na każdy moduł z rejestru, na górze główny wyłącznik, niżej pozycje modułu. Istniejące pozycje Reshaped, YouTube, katalogu KE i pilota Arc podłącz jako moduły-opakowania (bez przenoszenia ich kodu).
  Ukończone, gdy: kompiluje się; każda dotychczasowa pozycja jest osiągalna (lista w komentarzu testu).

- [x] **B05-4 Kategorie Telewizja i Tryb nocny w Essential.** Pozycje z rejestru z poziomem ESSENTIAL pokazują się w trybie Essential jako dwie kategorie. Wpięcie w `SettingsScreen` już istnieje z B00-5.
  Ukończone, gdy: test JVM filtrowania po poziomie.

- [x] **B05-5 Sterownik trybu nocnego.** `NightController`: tryby wyłączony / włączony / według godzin (start i koniec, przejście przez północ). Ręczne przełączenie (pilot albo ustawienia) obowiązuje do najbliższej granicy harmonogramu. Sprawdzanie: start aplikacji, start odtwarzania, co minutę gdy aplikacja jest na ekranie. Bez alarmów systemowych.
  Ukończone, gdy: testy JVM: 22:00–07:00 o 23:30 i o 08:00; ręczne wyłączenie o 23:00 wraca o 22:00 następnego dnia.

- [x] **B05-6 Dźwięk nocny według sterownika.** Odtwarzacz czyta „obowiązujący poziom” ze sterownika zamiast surowego `NightModeLevel` (jedna linia `// fork-hook: night` tam, gdzie poziom trafia do `NightModeAudioProcessor`). Zapisane ustawienie użytkownika się nie zmienia. Istniejący przycisk pilota przełącza przez sterownik.
  Ukończone, gdy: test JVM obowiązującego poziomu (harmonogram aktywny + ustawienie wyłączone = poziom z harmonogramu).

- [x] **B05-7 Ciepła nakładka obrazu.** `rootOverlays()` modułu: półprzezroczysta bursztynowa warstwa na całe okno, trzy siły (krycie ok. 8, 14, 20%), opcjonalne przyciemnienie, płynne włączanie przez 3 sekundy. Opcja Advanced „nie stosuj przy HDR i Dolby Vision” (domyślnie włączona: przy HDR nakładka wyłączona). Nakładka nie przechwytuje fokusu ani klawiszy.
  Ukończone, gdy: test JVM stanu nakładki; pozycje na liście TV („nakładka widoczna na filmie SDR”, „brak przy HDR”, „pilot działa normalnie”).

- [x] **B05-8 Pozycje trybu nocnego.** PROSTY: tryb, godziny, siła (jedna wspólna dla dźwięku i obrazu). ESSENTIAL: osobno dźwięk i obraz, siła każdego, przyciemnienie. ADVANCED: wyłączenie przy HDR.
  Ukończone, gdy: test JVM mapowania wspólnej siły na dwa ustawienia.
