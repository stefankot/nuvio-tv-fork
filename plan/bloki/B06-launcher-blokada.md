# B06 Ekran główny telewizora i blokada wyjścia (budżet 3 h)

Cel: Nuvio może być ekranem głównym telewizora, startuje z telewizorem, a przypadkowe wyjście jest niemożliwe.

Moduł: `J/fork/launcher` (`J` = `app/src/main/java/com/nuvio/tv`, `K` = `../nuvio-tv-kicia/app/src/main`).

## Zadania

- [x] **B06-1 Opcje systemowe z Kicia.** Przenieś `K/java/com/nuvio/tv/ui/forge/ForgeSystemOptions.kt` (ekran główny przez alias aktywności, start po włączeniu telewizora, odbiornik BOOT). Wpisy w `AndroidManifest.xml` odtwórz na podstawie manifestu Kicia (alias HOME domyślnie wyłączony, odbiornik BOOT, uprawnienie `RECEIVE_BOOT_COMPLETED`). Alias i odbiornik w pakiecie modułu.
  Ukończone, gdy: kompiluje się; scalony manifest zawiera alias z `CATEGORY_HOME` w stanie wyłączonym.

- [x] **B06-2 Ekran „Aplikacje”.** Trasa modułu: lista zainstalowanych aplikacji z intencją Leanback (ikona-baner, nazwa), OK uruchamia. Na górze skróty: Ustawienia systemu, Wejścia (jeśli system je udostępnia). Bez tego ekranu użytkownik nie uruchomi niczego poza Nuvio, więc alias HOME da się włączyć dopiero, gdy trasa istnieje.
  Ukończone, gdy: test JVM sortowania i filtrowania listy (bez samego Nuvio).

- [x] **B06-3 Reguła wyjścia.** Przenieś `K/java/com/nuvio/tv/ui/forge/nav/ForgeBackPolicy.kt` z testem `ForgeBackPolicyTest`. Zmiana końcówki: na ekranie głównym Wstecz nic nie robi; 5 naciśnięć w 3 sekundy otwiera „Wyjście i ustawienia” (Zamknij aplikację, Aplikacje, Ustawienia systemu), za PIN-em profilu, jeśli jest (DECYZJE #18). Licznik naciśnięć jako czysta klasa.
  Ukończone, gdy: testy JVM: 4 naciśnięcia nic, 5 w 3 s otwiera, 5 w 6 s nic.

- [x] **B06-4 „Po uruchomieniu pokaż”.** Ekran główny / telewizja / ostatni kanał (start od razu na żywo, mechanizm z B03-9). Pozycja poziomu PROSTY.
  Ukończone, gdy: test JVM wyboru trasy startowej.

- [x] **B06-5 Dwa poziomy blokady i ustawienia.** Miękka: reguła wyjścia bez bycia ekranem głównym. Pełna: Nuvio jako ekran główny + reguła wyjścia + start z telewizorem. Pozycje: „Uruchamiaj z telewizorem” (PROSTY), „Nuvio jako ekran główny” i „Blokada wyjścia” (ESSENTIAL). Włączanie ekranu głównego otwiera systemowy wybór; aplikacja nie zmienia ustawień systemu sama.
  Ukończone, gdy: kompiluje się; pozycje na liście TV („po włączeniu telewizora startuje Nuvio”, „Home wraca do Nuvio”, „pięć razy Wstecz otwiera wyjście”, „na tym modelu da się wybrać Nuvio jako ekran główny”).
