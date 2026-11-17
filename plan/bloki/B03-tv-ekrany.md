# B03 Telewizja: ekrany (budżet 4 h)

Cel: przewodnik jest pierwszym ekranem telewizji, ma nad sobą jeden rząd kart i znaczniki obejrzenia; program ma własną stronę jak film.

Skróty: `K` = `../nuvio-tv-kicia/app/src/main/java/com/nuvio/tv`, `J` = `app/src/main/java/com/nuvio/tv`. Ekrany w `J/fork/tv/ekrany`. Rysuj istniejącymi komponentami aplikacji (karty, rzędy, przyciski z ekranu głównego i szczegółów). Z plików `K/ui/forge/livetv/*` bierz stan i obsługę klawiszy, nie wygląd.

Gramatyka pilota (wszędzie): OK odtwarza, długie OK otwiera menu, ▲▼ kanał, ◀▶ czas, Wstecz o jeden poziom. Pilot nigdy nie trafia do natywnych kontrolek odtwarzacza w Live TV.

## Zadania

- [x] **B03-1 Przewodnik z rzędem „Dla Ciebie”.** Wpięcie w `ui/reshaped/livetv/LiveTvScreen.kt` (miejsce nagłówka nad siatką, wzór: `K/ui/forge/livetv/ForgeGuideHeaderA.kt`). Trzy strefy: rząd kart (kolejka z B02-8), linia sterowania `[Dziś ▾] [Kanały: … ▾] [Odkrywaj] [Szukaj]` z opisem programu pod fokusem, siatka Reshaped. Rząd chowa się, gdy fokus schodzi do siatki; ▲ z pierwszego wiersza go przywraca; ▼ z paska nawigacji ląduje na pierwszej karcie. Przyciski Odkrywaj i Szukaj do B08 pokazują się tylko, gdy ich trasy istnieją.
  Ukończone, gdy: test JVM stanu chowania; pozycja na liście TV.

- [x] **B03-2 Karty rzędu.** Karta = okładka (B02-7) albo karta zastępcza (logo kanału + tytuł), znacznik (Wznów, Nowy odcinek, Na żywo), pasek postępu, logo kanału jako mały znaczek. OK: wznawia, gra od początku albo na żywo. Długie OK: menu (Od początku, Obserwuj, Przypomnij, Oznacz jako obejrzane, Strona programu).
  Ukończone, gdy: test JVM mapowania pozycji kolejki na akcję OK.

- [x] **B03-3 Panel grup kanałów.** ◀ z kolumny kanałów albo OK na „Kanały ▾”: lista z licznikami: Ulubione, Twoje kanały, Ostatnie, Wszystkie, potem grupy od największej (dane Reshaped: `groups`, `groupCounts`, `hiddenGroups`, `groupNames`). Siatka pokazuje jedną grupę. Domyślny filtr: Ulubione przy co najmniej 5, inaczej Wszystkie z najczęściej oglądanymi na górze. Kanały bez programu na końcu. Długie OK na kanale: gwiazdka (ulubiony kanał).
  Ukończone, gdy: test JVM wyboru domyślnego filtra i sortowania.

- [x] **B03-4 Znaczniki w siatce.** Wpięcie w `ui/reshaped/livetv/LiveTvGuide.kt` (jedna linia: dekorator komórki): znacznik obejrzenia i cienki pasek postępu z dziennika oglądania.
  Ukończone, gdy: kompiluje się; wpięcie w `WPIECIA.txt`.

- [x] **B03-5 Strona programu.** Trasa modułu. Góra: tło, okładka, tytuł, logo kanału, linia „S24E4308 · wiek 12 · 2026 · serial obyczajowy” (z danych EPG), opis, ocena TMDB z liczbą głosów. Przyciski: Wznów / Oglądaj na żywo / Od początku, Obserwuj, Przypomnij. Odcinki: zakładki sezonów, gdy są numery; inaczej lista według daty; wiersz ma znacznik obejrzenia, postęp, kanał, dostępność (w archiwum do dnia, na żywo, w przyszłości). Obsada: zdjęcia z TMDB albo tekst z EPG. Dla typu Pasmo: tylko „ostatnie wydanie” i najbliższe emisje. Wzór danych: `K/ui/forge/livetv/ForgeProgramDetail.kt`, `ForgeInfoDrawer.kt` (parsowanie etykiet odcinków przenieś z testami `ForgeProgramDetailTest`).
  Ukończone, gdy: testy JVM etykiet odcinków i grupowania w sezony.

- [x] **B03-6 MiniGuide.** Przenieś nawigację z `K/ui/forge/livetv/ForgeMiniGuide.kt` (`ForgeMiniGuideNav`, testy `ForgeMiniGuideTest`) i wpięcia w `LiveTvPlayerOverlay.kt` opisane w diffie (OK pokazuje kartę; ▲▼ kanał; ◀▶ co dalej; przyciski Przewodnik, Info i odcinki, Od początku, Pauza, Gwiazdka). „Info i odcinki” otwiera stronę programu z B03-5 jako warstwę nad obrazem.
  Ukończone, gdy: przeniesione testy zielone; pozycja na liście TV.

- [x] **B03-7 Stan „z opóźnieniem”.** Dwa stany: NA ŻYWO i Z OPÓŹNIENIEM. Pauza albo przewinięcie w tył na kanale z archiwum przełącza bez pytania na strumień archiwalny tej samej emisji w tym samym miejscu (mechanizm „od początku” i `LiveTvScrub` z Reshaped). W stanie Z OPÓŹNIENIEM: przewijanie w obie strony, kroki 10/30/60/120 s przy przytrzymaniu, napis „−12 min”, przycisk „Wróć do na żywo”; po dogonieniu krawędzi powrót do NA ŻYWO. Na kanałach bez archiwum pauza działa jak dotąd.
  Ukończone, gdy: test JVM automatu stanów i kroków przewijania; pozycja na liście TV („pauza na kanale z archiwum i przewinięcie reklam”).

- [x] **B03-8 Przypomnienia i alerty.** Karta przypomnienia nad obrazem (dane z B02-1), przełącznik w ustawieniach modułu. Alert „nowy odcinek” jako pozycja w rzędzie „Dla Ciebie”, bez osobnych powiadomień.
  Ukończone, gdy: test JVM „alert ogłaszany raz”.

- [x] **B03-9 Rząd TV na głównym ekranie aplikacji.** `homeRows()` modułu: rząd „Telewizja: dla Ciebie” z tą samą kolejką. OK na karcie uruchamia kanał przez mechanizm z `K/ui/forge/livetv/ForgePendingPlay.kt` (przenieś z testem).
  Ukończone, gdy: przeniesiony test zielony.
