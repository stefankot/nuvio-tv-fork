# B02 Telewizja: rdzeń danych (budżet 4 h)

Cel: aplikacja wie, który program jest serialem, który odcinek jest w trakcie i który obejrzany. Bez ekranów; same dane i testy.

Skróty: `K` = `../nuvio-tv-kicia/app/src`, `J` = `app/src/main/java/com/nuvio/tv`. Moduł: `J/fork/tv` z pakietami `rdzen` (czysty Kotlin), `dane`, `most`, `ekrany`.

Zasada: stan należy do programu i odcinka, nie do kanału i godziny. Pojęcia: Kanał (adres strumienia), Emisja (kanał + start), Program (znormalizowany tytuł), Odcinek (znacznik S01E02 / „odc. 5” albo skrót opisu).

## Zadania

- [ ] **B02-1 Kopia logiki z Kicia.** Z `K/main/java/com/nuvio/tv/forge/tv/`: `ShowIdentity.kt`, `RecentlyViewed.kt`, `RecentChannels.kt`, `EpisodeAlerts.kt`, `ForgeEpgExtras.kt`, `ForgeReminders.kt`, `ForgeTv.kt`, `ForgeTvTmdb.kt` do `J/fork/tv` (zmiana pakietu; nazwy bez przedrostka Forge). Testy z `K/test/java/com/nuvio/tv/forge/tv/` i `ForgeEpgExtrasTest`. Brakujące wywołanie TMDB `searchMulti` dodaj do `TmdbApi` jedną metodą (`// fork-hook: tv`).
  Ukończone, gdy: przeniesione testy są zielone.

- [ ] **B02-2 Most do Reshaped.** `fork/tv/most`: jedyne miejsce importujące `reshaped.livetv.*`. Implementacja `ForkTvHooks` z B01-6: `onProgrammeRead`, `onGuideRead(channels, schedule)`, `onPlay(channel, programme)`, `onPosition(channel, emisja, pozycjaMs)`. Wpięcia: `LiveTvRepository.kt` (2 linie: po odczycie przewodnika, przy starcie odtwarzania), `LiveTvPlayerOverlay.kt` (1 linia: pozycja co 10 s, na żywo i z archiwum; źródło: aktualizacje pozycji używane przez `LiveTvReplayFollow`).
  Ukończone, gdy: test JVM mostu na sztucznych kanałach; wpięcia w `WPIECIA.txt`.

- [ ] **B02-3 Dziennik odcinków i indeks tytułów.** Archiwum Reshaped nie trzyma opisów, więc identyfikator odcinka zapisuj w chwili czytania EPG. Plik na profil w `files/fork_tv/`: dla każdej emisji kanałów z archiwum z ostatnich 7 dni i następnych 8: klucz programu, kanał, start, długość, identyfikator odcinka. Zwarty format, limit 200 000 wpisów, najstarsze wypadają. Służy też jako indeks do szukania (B08).
  Ukończone, gdy: testy JVM zapisu, odczytu, limitu i uszkodzonego pliku (zwraca pusty, nie wywraca aplikacji).

- [ ] **B02-4 Dziennik oglądania.** Dla odcinka (program + odcinek; gdy brak identyfikatora: kanał + start): pozycja, długość, obejrzane minuty (zbiór minut emisji), data, stan, emisja do wznowienia. Reguły z DECYZJE #1. Ręczne oznaczanie: obejrzany, nieobejrzany, wszystkie wcześniejsze. Wznowienie: jeśli emisja jest w archiwum, od zapisanej pozycji; jeśli nie, wskaż najbliższą powtórkę z dziennika odcinków.
  Ukończone, gdy: testy JVM: przeskok na koniec nie zalicza; 90% z przewijaniem reklam zalicza; oglądanie na żywo od połowy do końca zalicza przy 50%.

- [ ] **B02-5 Typ programu.** Serial (znaczniki albo co najmniej 2 różne identyfikatory odcinków), Film (TMDB dopasował film albo emisja ponad 75 min bez odcinków), Pasmo (ten sam tytuł co najmniej 4 razy w 7 dni, bez znaczników, jeden opis), Jednorazowy. Pasmo nie ma licznika nieobejrzanych (DECYZJE #3).
  Ukończone, gdy: testy JVM na próbkach: serial codzienny, wiadomości, film, program jednorazowy.

- [ ] **B02-6 Obserwowane i statystyka kanałów.** Obserwowane programy (lista + automatyczne po 2 obejrzanych odcinkach, z flagą „automatyczne” i możliwością cofnięcia). Statystyka kanałów: minuty oglądania z połową wagi po 14 dniach, osobno dla 4 pór dnia; „twoje kanały” = co najmniej 3 oglądania w 14 dni (DECYZJE #7). Ulubione kanały zostają w Reshaped (`favoriteUrls`).
  Ukończone, gdy: testy JVM wygaszania i progu.

- [ ] **B02-7 Karta programu z trwałym cache.** Okładka, tło, opis, obsada, ocena, typ. Kolejność okładki: obraz z EPG, plakat TMDB przy ścisłej zgodności tytułu (logika z `ForgeTvTmdb`), brak (ekran narysuje kartę zastępczą). Cache na dysku na klucz programu; brak dopasowania pamiętany 7 dni. Zapytania TMDB tylko dla kart widocznych i obserwowanych.
  Ukończone, gdy: testy JVM cache (trafienie, wygaśnięcie braku).

- [ ] **B02-8 Kolejka „Dla Ciebie”.** Czysta funkcja zwracająca do 12 pozycji w kolejności: w trakcie, nowy odcinek obserwowanego (na żywo albo w archiwum), na żywo na ulubionych, na żywo na kanałach zwykle oglądanych o tej porze. Każda pozycja ma znacznik: Wznów, Nowy odcinek, Na żywo. Nigdy pusta, jeśli jest choć jeden kanał z programem.
  Ukończone, gdy: testy JVM kolejności i braku duplikatów programu.

- [ ] **B02-9 Rejestracja modułu.** `TvModule : ForkModule` z wyłącznikiem. Ustawienia modułu: próg „obejrzane” (Essential), automatyczne ulubione i obserwowane (Essential), wyczyść historię oglądania TV (Advanced).
  Ukończone, gdy: wyłączony moduł nie zapisuje niczego (test JVM).
