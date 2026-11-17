# B08 Telewizja: Odkrywaj i Szukaj (budżet 4 h)

Cel: archiwum 300 kanałów wygląda jak biblioteka VOD, a każdy program da się znaleźć jednym polem.

Moduł: `J/fork/tv` (ciąg dalszy B02 i B03). `K` = `../nuvio-tv-kicia/app/src/main/java/com/nuvio/tv`.

## Zadania

- [ ] **B08-1 Koszyki gatunków.** Czysta funkcja: kategorie z EPG (tekst po polsku i angielsku) na 8 koszyków: Film, Serial, Sport, Dla dzieci, Dokument, Rozrywka, Informacje, Muzyka. Gdy EPG nie ma kategorii: koszyk z nazwy grupy kanału. Słownik słów kluczowych w `assets/fork/genres.json`.
  Ukończone, gdy: test JVM na 40 przykładowych kategoriach.

- [ ] **B08-2 Widok Odkrywaj.** Trasa modułu, otwierana przyciskiem z linii sterowania przewodnika. Rzędy z istniejących komponentów ekranu głównego: Oglądaj dalej · Obserwowane (licznik nieobejrzanych w archiwum) · Dziś wieczorem (20:00–23:00 na twoich kanałach, OK = przypomnienie) · Filmy w archiwum · Seriale w archiwum (emisje scalone w jedną kartę programu) · po jednym rzędzie na koszyk gatunku. Dane z dziennika odcinków (B02-3) i typów (B02-5). Puste rzędy znikają. Rzędy ładują się leniwie, maksymalnie 30 kart na rząd.
  Ukończone, gdy: test JVM budowania rzędów z próbki dziennika (scalanie, brak pasm w „Seriale”).

- [ ] **B08-3 Szukanie.** Trasa modułu. Przed wpisaniem: ostatnie wyszukiwania i 8 kafli koszyków. Wpisywanie: istniejąca klawiatura ekranowa aplikacji; mikrofon przez `SpeechToText` z B07, jeśli jest. Wyniki w trzech rzędach: Kanały · Teraz i wkrótce · W archiwum; emisje jednego programu scalone. Dopasowanie bez znaków diakrytycznych, od 2 liter, po początku słów. Wzór: `K/ui/forge/livetv/ForgeTvSearch.kt` (przenieś test `ForgeTvSearchTest`).
  Ukończone, gdy: testy JVM wyszukiwania na próbce indeksu (300 kanałów, 5000 emisji) w czasie poniżej 50 ms na zapytanie w teście.

- [ ] **B08-4 Okładki w tle.** Dla obserwowanych programów i kart pierwszych dwóch rzędów Odkrywaj pobieraj karty TMDB w tle po odczycie przewodnika: najwyżej 40 zapytań na odświeżenie, z przerwami, tylko poza odtwarzaniem. Karta zastępcza: logo kanału i tytuł na jednolitym tle.
  Ukończone, gdy: test JVM limitu zapytań i kolejki.

- [ ] **B08-5 Ustawienia rzędów.** ESSENTIAL: które rzędy Odkrywaj są widoczne i ich kolejność; ukryte grupy kanałów (istniejące dane Reshaped) pomijane w rzędach.
  Ukończone, gdy: test JVM kolejności i ukrywania.
