# B09 Lektor AI: głos czytający polskie napisy (budżet 4 h)

Cel: każdy tytuł z polskimi napisami da się obejrzeć „z lektorem”. Etap pierwszy na systemowym silniku mowy Androida, bez dołączania modelu.

Moduł: `J/fork/lektor` (`J` = `app/src/main/java/com/nuvio/tv`).

Założenia: polski lektor czyta równo i spokojnie, więc celem jest naturalna melodia zdania, nie aktorstwo. Wrażenie lektora daje ściszanie oryginału tylko wtedy, gdy głos mówi. Lektor wymaga dekodowania dźwięku w aplikacji, więc wyłącza przekazywanie Dolby do amplitunera (DECYZJE #13).

## Zadania

- [ ] **B09-1 Plan kwestii.** Czysta logika: z listy napisów (start, koniec, tekst) zrób listę kwestii do przeczytania: usuń znaczniki formatowania, opisy dźwięków w nawiasach, myślniki dialogowe i imiona mówiących; połącz kwestie rozdzielone o mniej niż 300 ms; wylicz dostępny czas do następnej kwestii.
  Ukończone, gdy: testy JVM na próbce 40 napisów (SRT i ASS).

- [ ] **B09-2 Dopasowanie tempa.** Dla każdej kwestii: jeśli mowa jest dłuższa niż dostępny czas, przyspiesz do najwyżej 1,3×; jeśli nadal za długa, pozwól zająć przerwę przed następną kwestią; jeśli i to nie wystarcza, utnij na granicy zdania.
  Ukończone, gdy: testy JVM trzech przypadków.

- [ ] **B09-3 Silnik mowy.** Interfejs `Lektor { suspend fun syntezuj(tekst, tempo): Pcm }`. Implementacja na systemowym `TextToSpeech` (synteza do pliku, język polski, wybór głosu z listy głosów urządzenia). Brak polskiego głosu: moduł zgłasza „niedostępne” i proponuje napisy.
  Ukończone, gdy: test JVM na atrapie silnika; pozycja na liście TV („jakie polskie głosy ma urządzenie i jak brzmią”).

- [ ] **B09-4 Synteza naprzód i pamięć podręczna.** Podczas odtwarzania syntezuj 3 minuty naprzód od pozycji; po przewinięciu przesuń okno. Pamięć na dysku na tytuł i plik napisów, limit 200 MB, najstarsze wypadają. Synteza nie blokuje odtwarzania; kwestia niegotowa na czas jest pomijana, a napis pokazany.
  Ukończone, gdy: testy JVM okna, przewinięcia i limitu pamięci.

- [ ] **B09-5 Miks ze ściszaniem.** Procesor dźwięku w łańcuchu odtwarzacza (wzór budowy: `ui/screens/player/NightModeAudioProcessor.kt`): oryginał ściszany o ok. 10 dB z płynnym zejściem 150 ms i powrotem 300 ms tylko podczas kwestii; głos lektora domiksowany do kanału środkowego (albo obu przy stereo). Przy włączonym lektorze wymuś dekodowanie do PCM. Jedno wpięcie `// fork-hook: lektor` w miejscu budowy procesorów dźwięku.
  Ukończone, gdy: test JVM obwiedni ściszania na syntetycznym sygnale (poziomy przed, w trakcie i po kwestii).

- [ ] **B09-6 Włączanie.** Pozycja w menu dźwięku odtwarzacza: „Lektor (głos syntetyczny)”, widoczna, gdy są polskie napisy i brak polskiej ścieżki. Dla profilu „Zawsze po polsku” włącza się sama jako szczebel przed napisami (B04). Przy pierwszym włączeniu jedno zdanie o braku Dolby. Napisy są wtedy domyślnie ukryte.
  Ukończone, gdy: test JVM warunków widoczności i automatu szczebli.

- [ ] **B09-7 Ustawienia.** ESSENTIAL: głos (lista z urządzenia), głośność lektora względem filmu, siła ściszania. ADVANCED: limit pamięci podręcznej, wyczyść.
  Ukończone, gdy: kompiluje się; pozycje na liście TV („lektor nadąża za dialogiem”, „oryginał słychać w tle”, „przewinięcie nie rozjeżdża głosu”).
