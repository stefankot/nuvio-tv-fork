# B00 Fundament (budżet 4 h)

Cel: każda następna funkcja wchodzi jako moduł bez nowych zmian w rdzeniu, a build i testy działają bez udziału właściciela.

Skróty: `K` = `../nuvio-tv-kicia`, `J` = `app/src/main/java/com/nuvio/tv`.

## Zadania

- [x] **B00-1 Gałąź i stan środowiska.** Przejdź na `final`, przeczytaj `ARCHITECT_PLAN.md` (sekcje 2.0, 2.1, 2.7, 4) i `STATUS.md`, utwórz `auto/wdrozenie`. Uruchom `fork-tools/check.sh` i zapisz wynik. Uzupełnij sekcję „Środowisko” w `STAN.md`. Sklonuj Kicia, jeśli brak.
  Ukończone, gdy: gałąź istnieje, `STAN.md` ma wypełnione środowisko.

- [ ] **B00-2 Build bazowy.** Uruchom build i testy w tle według `KONTYNUUJ.md` sekcja 4. Nie czekaj; przejdź do B00-3. Gdy wynik będzie znany, zapisz w `STAN.md` liczbę testów i ewentualne błędy bazy. Błędy kompilacji bazy napraw najmniejszą możliwą zmianą.
  Ukończone, gdy: w `STAN.md` jest wynik builda bazowego (zielony albo lista naprawionych błędów).

- [x] **B00-3 CI na GitHubie.** Dodaj workflow `fork-verify.yml`: na push do `auto/**` kompilacja `assembleFullDebug -PforgeArm64Only=true`, testy JVM, artefakt APK. Wzór: `K/.github/workflows/forge-verify.yml` i `K/scripts/forge/ci-gradle-env.sh`, `run-tests.sh`. Istniejących workflowów bazy nie zmieniaj.
  Ukończone, gdy: workflow jest w repozytorium i (jeśli push możliwy) pierwszy przebieg jest widoczny w `gh run list`.

- [ ] **B00-4 Rejestr modułów.** Pakiet `J/fork/registry`, wołany wyłącznie z `J/fork/ForkHooks.kt` (istnieje; ma już `onGlobalKey`, `skinScreen`, `skinPlayerChrome`, wpięcia Echo). Przełączniki przez istniejące `ForkFeatures` (`enabled(c, key, default)`); nie twórz drugiego mechanizmu. Interfejs `ForkModule`: `id`, `title`, `enabled` (przełącznik w prefs `fork_features`), oraz opcjonalne dostawcy:
  1. `settings()`: lista pozycji z poziomem (PROSTY, ESSENTIAL, ADVANCED), grupą, tytułem, opisem i kontrolką;
  2. `routes()`: trasy nawigacji;
  3. `homeRows()`: rzędy ekranu głównego;
  4. `rootOverlays()`: nakładki na całe okno;
  5. `playerHandlers()`: klawisze i zdarzenia odtwarzacza (start, pozycja, koniec);
  6. `voiceCommands()`: polecenia głosowe (używane od B07).
  Obiekt `ForkRegistry` trzyma listę modułów wpisaną jawnie w jednym pliku (bez refleksji).
  Ukończone, gdy: testy JVM rejestru (włączanie, wyłączony moduł nic nie zgłasza) są zielone.

- [ ] **B00-5 Wpięcia rejestru przez `ForkHooks`.** Dodaj do `ForkHooks` funkcje: `moduleRoutes`, `moduleSettings`, `moduleHomeRows`, `moduleOverlays`, `modulePlayer`. Po jednej instrukcji z komentarzem `// Fork hook: registry` w: `NuvioNavHost`, `SettingsScreen`, `HomeScreen`, `MainActivity`, `PlayerScreen` (wszystkie pięć są już plikami z wpięciami, więc limit 35 się nie zmienia). Każde wpięcie dopisz do `fork-tools/hooks.tsv` według zasady R2. Istniejących wpięć nie przepisuj.
  Ukończone, gdy: aplikacja się kompiluje, a przy pustym rejestrze zachowanie jest bez zmian.

- [ ] **B00-6 Rozszerzenie `fork-tools` o łatki.** Test kontraktowy już istnieje: `fork-tools/check.sh` z `hooks.tsv`, `overrides.txt`, `hooked-overrides.txt`. Dodaj czwartą klasę plików: „łatany” (`fork-tools/patched-overrides.txt` + pliki w `fork-tools/patches/<seria>/NN-opis.patch`). `check.sh` dla takiego pliku: po odwróceniu łatek serii i usunięciu linii z `hooks.tsv` plik musi być identyczny z Reshaped na przypiętym commicie. Pliki łatane liczą się do limitu 35. Tam, gdzie w dalszych blokach jest mowa o `plan/WPIECIA.txt` albo `scripts/fork/hooks.sh`, używaj `fork-tools/hooks.tsv` i `check.sh`.
  Ukończone, gdy: `check.sh` jest zielony, a ręczna zmiana jednej linii w pliku łatanym poza łatką go wywraca (sprawdź i przywróć).

- [ ] **B00-7 Moduł „pierwsze uruchomienie”.** `J/fork/settings/FirstRunDefaults`: przy pierwszym starcie profilu ustawia przez istniejące DataStore: TMDB włączone, język TMDB i aplikacji polski, poziom Prosty (flaga modułu, używana od B05). Nie zmienia wartości, które użytkownik już zapisał. Żadnych edycji domyślnych wartości w plikach bazy.
  Ukończone, gdy: test JVM potwierdza „nie nadpisuje zapisanych”.

- [ ] **B00-8 Autoaktualizacja z forków.** Przenieś `K/scripts/forge/autoupdate.sh`, `watch-upstream.sh` i workflow `forge-autoupdate.yml` jako `scripts/fork/` i `fork-autoupdate.yml`, dostosowane do źródeł z `UPSTREAM_SOURCES.md`. Workflow tylko na ręczne uruchomienie (`workflow_dispatch`), bez harmonogramu i bez wydawania APK: otwiera gałąź `sync/auto-*` i uruchamia `fork-verify`. Nic nie scala do `main`.
  Ukończone, gdy: skrypt przechodzi `bash -n`, workflow jest poprawnym YAML-em.

## Po bloku

Zaktualizuj `STATUS.md` (sekcja „Wdrożone”) jedną linią o rejestrze i CI.
