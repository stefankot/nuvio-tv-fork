# B07 Sterowanie głosem (budżet 4 h + 4 h)

Cel: przycisk na pilocie uruchamia sterowanie głosem całej aplikacji. Priorytetem jest jakość rozumienia, nie lokalność (DECYZJE #14–16).

Moduł: `J/fork/voice` (`J` = `app/src/main/java/com/nuvio/tv`, `K` = `../nuvio-tv-kicia/app/src/main`).

Urządzenia docelowe (DECYZJE #19): Xiaomi TV Box S 3. generacji (2 GB RAM), Onn 4K Pro, projektor JMGO (4 GB RAM). Wszystkie z Google TV, gdzie system zwykle nie pozwala wybrać innego asystenta w ustawieniach. Dlatego są dwa sposoby wywołania (B07-1 i B07-2).

Ważne rozróżnienie: **wzorzec polecenia** to zdanie z miejscem na parametr, np. „włącz kanał {kanał}”. Jeden wzorzec obsługuje wszystkie 300 kanałów, bo parametr jest dopasowywany do listy nazw. Wymagane minimum to 60 wzorców, a nie 60 zdań.

## Zadania

- [ ] **B07-1 Asystent z Kicia.** Przenieś `K/java/com/nuvio/tv/ui/forge/assist/ForgeAssist.kt` (usługa asystenta, sesja, aktywność dla `ACTION_ASSIST`) z wpisami manifestu i plikiem XML usługi. Sesja uruchamia nasłuch poleceń zamiast wyszukiwania. Przenieś opcję „Nuvio jako asystent” z `ForgeSystemOptions`.
  Ukończone, gdy: kompiluje się; scalony manifest zawiera usługę z uprawnieniem `BIND_VOICE_INTERACTION`.

- [ ] **B07-2 Przechwycenie przycisku na Google TV.** Dwie drogi, obie wyłączane w ustawieniach:
  1. akcja „Sterowanie głosem” do przypisania w module pilota Arc (`RemoteButtons`), działa, gdy Nuvio jest na ekranie;
  2. usługa ułatwień dostępu modułu, która przechwytuje wybrany przycisk (domyślnie przycisk asystenta; do wyboru także długie naciśnięcie innego) i przekazuje go do Nuvio. Usługa obsługuje tylko ten jeden klawisz, niczego nie czyta z ekranu, a resztę zdarzeń przepuszcza bez opóźnienia. Włączenie wymaga jednorazowej zgody użytkownika na ekranie systemowym; aplikacja tylko go otwiera i tłumaczy jednym zdaniem.
  Ukończone, gdy: test JVM filtra klawiszy (tylko wybrany klawisz jest przejmowany); pozycje na liście TV dla każdego z trzech urządzeń („przycisk asystenta otwiera Słucham”, „pilot nie zwalnia po włączeniu usługi”).

- [ ] **B07-3 Szyna poleceń.** Typ `VoiceCommand` (identyfikator + parametry) i interfejs `CommandParser { suspend fun parse(tekst, kontekst): VoiceCommand? }`. Kontekst: bieżący ekran, bieżący kanał i program, listy encji. Moduły zgłaszają wzorce przez `voiceCommands()` rejestru: identyfikator, wzorce z parametrami, przykłady, wykonanie. Wszystkie parsery zwracają ten sam typ.
  Ukończone, gdy: test JVM rejestracji i wywołania.

- [ ] **B07-4 Encje.** Słowniki do dopasowania parametrów, odświeżane w tle: nazwy kanałów (z wariantami: „TVP 1”, „jedynka”, „tefałpe jeden”, „Polsat Sport 2”), grupy kanałów, tytuły z przewodnika, archiwum i biblioteki, gatunki, nazwy profili, nazwy ustawień, liczebniki i czas („pięć minut”, „pół godziny”, „dwudziesta”), dni („wczoraj”, „w sobotę”). Dopasowanie przybliżone bez znaków diakrytycznych, z odmianą („na Polsacie”).
  Ukończone, gdy: test JVM na 80 wypowiedziach nazw kanałów i tytułów, w tym błędach typowych dla rozpoznawania mowy.

- [ ] **B07-5 Parser regułowy: minimum 60 wzorców.** Grupy:
  - **Kanały (10):** włącz {kanał} · następny / poprzedni kanał · poprzednio oglądany · kanał numer {n} · dodaj / usuń z ulubionych · pokaż ulubione · pokaż kanały {grupa} · co leci na {kanał} · co będzie potem na {kanał} · co leci teraz (lista)
  - **Przeglądanie przewodnika (10):** pokaż przewodnik · przewodnik na {dzień} · co jest o {godzina} · co jest dziś wieczorem · filmy / seriale / sport teraz · filmy w archiwum · pokaż {gatunek} · w dół / w górę / następna strona · wybierz {numer na ekranie} · wróć
  - **Program i archiwum (10):** od początku · wróć do na żywo · cofnij / przewiń o {czas} · przewiń reklamy · pauza / graj · włącz {tytuł} · następny / poprzedni odcinek · odcinek {n} · obserwuj / przestań obserwować · przypomnij mi o tym
  - **Dźwięk i napisy (10):** włącz / wyłącz napisy · napisy po {język} · większe / mniejsze napisy · napisy nie pasują · dźwięk po {język} · włącz / wyłącz lektora · głośniej / ciszej dialogi · tryb nocny włącz / wyłącz · tryb nocny do rana · inne źródło
  - **Aplikacja (10):** ekran główny · telewizja · biblioteka · szukaj {tekst} · ustawienia · otwórz ustawienie {nazwa} · zmień profil na {profil} · aplikacje · otwórz {aplikacja} · pomoc (lista przykładów)
  - **Odkrywanie (10):** coś lekkiego / wymagającego / perełki · coś na {czas} · coś jak {tytuł} · kryminał / komedia / {gatunek} · oglądaj dalej · co nowego w obserwowanych · losowy odcinek {tytuł} · pokaż obsadę · kto tu gra · zagraj zwiastun
  Na listach wyników każda pozycja dostaje widoczny numer, żeby działało „wybierz trzy”.
  Nierozpoznany tekst trafia do parsera modelowego (B07-8), a gdy go nie ma, do wyszukiwania.
  Ukończone, gdy: test JVM na co najmniej 180 wypowiedziach (po 3 na wzorzec) z trafnością 95% i zero błędnych akcji na 30 wypowiedziach spoza listy.

- [ ] **B07-5b Dopasowanie znaczeniowe (lokalnie, bez klucza).** Trzecia implementacja `CommandParser` na `Embedder` z B06b: wypowiedź zamieniana na wektor i porównywana z wektorami przykładów każdego wzorca (po 5 do 10 przykładów na wzorzec, liczone raz i trzymane w pliku). Wybrany wzorzec dostaje parametry z dopasowania encji (B07-4). Poniżej progu podobieństwa albo przy małej różnicy między dwoma najlepszymi: dymek z propozycjami (B07-7). Kolejność rozumienia: reguły, potem dopasowanie znaczeniowe, na końcu model po API, jeśli jest klucz. Dzięki temu inne sformułowania („przełącz na jedynkę”, „daj mi sport”) działają u każdego bez sieci.
  Ukończone, gdy: test JVM na atrapie `Embedder`; zestaw 120 parafraz (po 2 na wzorzec, inne słowa niż w regułach) do pomiaru na prawdziwym modelu, z wynikiem zapisanym w `plan/raporty/glos-znaczenia.md` albo, bez modelu w środowisku, z pozycją na liście TV.

- [ ] **B07-6 Mowa na tekst: trzy silniki.** Interfejs `SpeechToText`. Implementacje:
  1. **Whisper lokalnie** przez bibliotekę sherpa już obecną w forku (`ui/screens/player/audiosync/asr/`: `SharedRecognizer`, `AsrModelChoice` z modelami tiny, base, small). Użyj wspólnego rozpoznawacza, nie ładuj drugiego modelu. Nasłuch do 8 sekund z wykrywaniem ciszy.
  2. **Systemowe** rozpoznawanie mowy Androida (wzór: `ui/screens/search/SearchScreen.kt`).
  3. **Przez API** (B07-8), gdy użytkownik podał klucz.
  Wybór automatyczny, do zmiany w ustawieniach: urządzenie z co najmniej 3 GB RAM i pobranym modelem: Whisper small; 2 GB RAM: systemowe, a Whisper base tylko na wyraźne życzenie. Podpowiedź dla Whispera: lista nazw kanałów i tytułów z kontekstu jako tekst wstępny, jeśli biblioteka to umożliwia.
  Ukończone, gdy: test JVM wyboru silnika według pamięci; pozycje na liście TV („czas od końca wypowiedzi do akcji” dla każdego silnika i urządzenia).

- [ ] **B07-7 Dymek i potwierdzenia.** Nakładka z rejestru: „Słucham…”, rozpoznany tekst, wynik („Włączam TVP 1”). Gdy pewność dopasowania jest niska albo pasują dwie encje: lista do trzech propozycji z numerami zamiast zgadywania. Polecenia o skutkach trudnych do cofnięcia (zmiana profilu, zamknięcie aplikacji) wymagają „tak”.
  Ukończone, gdy: test JVM progów pewności.

- [ ] **B07-8 Parser modelowy przez API (jakość).** Druga implementacja `CommandParser` dla próśb swobodnych („włącz coś lekkiego, ale nie komedię”, „to, co wczoraj oglądałem na Polsacie”). Model dostaje listę wzorców jako narzędzia oraz skrócony kontekst (ekran, kanał, do 50 najbliższych encji) i zwraca jedno polecenie albo pytanie doprecyzowujące. Dostawca wymienny za interfejsem `LlmClient` (pierwsza implementacja: Gemini). Klucz API podaje użytkownik przez stronę konfiguracji z telefonu; przechowywany w zaszyfrowanych preferencjach, nigdy w repozytorium ani w logach. Bez klucza funkcja jest niewidoczna. Wysyłany jest tylko tekst wypowiedzi i nazwy z kontekstu, nigdy dźwięk ani historia oglądania.
  Kolejność rozumienia: reguły, a model tylko wtedy, gdy reguły nie trafią albo pewność jest niska.
  Ukończone, gdy: test JVM na atrapie klienta (poprawne narzędzie, odmowa nieznanego narzędzia, limit czasu 4 s z powrotem do wyszukiwania).

- [ ] **B07-9 Polecenia modułów i ustawienia.** Zgłoś wzorce w modułach TV, źródeł i napisów, trybu nocnego, launchera, poleceń. Moduł nieobecny: wzorzec pominięty. Ustawienia „Sterowanie głosem” (ESSENTIAL): włącz, sposób wywołania, silnik mowy, klucz API (przez telefon), lista przykładów.
  Ukończone, gdy: test JVM: każdy wzorzec ma wykonawcę albo znacznik „moduł nieobecny”.
