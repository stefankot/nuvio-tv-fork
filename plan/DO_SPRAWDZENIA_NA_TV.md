# DO SPRAWDZENIA NA TELEWIZORZE

Lista dla właściciela. Wykonawca dopisuje tu wszystko, czego nie da się sprawdzić bez prawdziwego urządzenia. Jedna pozycja = co kliknąć i czego się spodziewać. Właściciel odhacza albo dopisuje „nie działa: ...”.

## Baza NuvioTV 1.2.0-beta.4 (aktualizacja 26.11, build baza1126a)

- [ ] Live TV (Niunia): wejście z menu, kanał gra, zmiana kanału ▲▼ bez czarnej przerwy (odtwarzacz zostaje), przewodnik, archiwum programu sprzed godziny od początku i przewijanie jak film; po pauzie kanał wraca do „na żywo”.
- [ ] Menu pigułki u góry (Niunia) widoczne na Głównej; ▲ do menu, Wstecz do menu.
- [ ] Film z „Odtwórz”: automatycznie wybrane źródło zgodne z regułami kreatora (log `ForkRankedRegex wybór:`); gdy źródło nie rusza — samo przechodzi na następne (oficjalne „Fallback to next matching source”).
- [ ] Napisy z dodatku: oficjalny AutoSync — znaczek stanu synchronizacji przy napisach (zamiast dymków Reshaped); przycisk napisów Kici pokazuje kropki/✓/!.
- [ ] Ustawienia › Odtwarzanie › Napisy: czcionki napisów (Atkinson domyślnie, Tiresias) i „Import from Phone”.
- [ ] Dolby Vision / HDR10: film 4K DV gra z obrazem (oficjalna obsługa DV; profil 7 → „Auto”).
- [ ] Ustawienia (drzewo): „Gdy film się zatnie”, „Dźwięk i obraz”, „Wygląd napisów”, „Rozpoznawanie mowy” otwierają ekrany Nuvio; brak pozycji „Przewijanie” i „Szybkość internetu”.

## Na start (stan bazy, przed nowymi funkcjami)

- [ ] Aplikacja się uruchamia i pokazuje ekran główny.
- [ ] Film z dodatku odtwarza się, pauza i przewijanie działają.
- [ ] Live TV: kanał gra, przewodnik pokazuje programy.
- [ ] Catch-up: program sprzed godziny odtwarza się od początku.
- [ ] YouTube: lista i odtwarzanie.

## Dopisane przez wykonawcę

(numer zadania, kroki, oczekiwany wynik)

## B01-2
- Uruchom aplikację, odczekaj ok. minuty, wejdź do TV: program (przewodnik) widoczny od razu, bez długiego ładowania. (Urządzenia 'low memory' pomijają wczytanie w tle.)

## B01-8
- Podczas startu kanału naciśnij OK i strzałki: nie powinny otwierać kontrolek filmu.
- Podczas oglądania programu z archiwum zmień kanał strzałką ▲▼: pojawia się okno „Zmienić kanał?” z fokusem na „Zostań”.

## B01-9/10
- Kanały mają logo z paczki/przewodnika; dla listy z polskimi kanałami pojawia się obraz i numer odcinka z drugiego przewodnika (epg.ovh).

## B03 (ekrany TV)
- TV → przewodnik: nad siatką rząd „Dla Ciebie” (po obejrzeniu czegoś: Wznów; nowe odcinki; Na żywo). OK na karcie gra, długie OK otwiera stronę programu.
- Komórki przewodnika: ✓ przy obejrzanych, cienki zielony pasek przy rozpoczętych.
- Ekran główny: wiersz „Telewizja: dla Ciebie” jest zgłoszony w rejestrze (renderowanie wierszy modułów w HomeScreen: do zrobienia, patrz BLOKADY).
- NIE wykonane w UI (tylko logika + testy JVM): panel grup kanałów (ChannelFilters), MiniGuide z przyciskami (MiniGuideLogic), stan „z opóźnieniem” (DelayedPlayback) w odtwarzaczu.

## Sesja 17.11 (wykonawca)
- B04-6: otwórz szczegóły filmu z dodatkami: u dołu po lewej linia „Gotowe · 1080p · lektor PL” (jeśli źródło ma polski dźwięk); znika po wyjściu z ekranu. Sprawdzone na emulatorze (tylko „Gotowe”).
- B04-4: profil „Zawsze po polsku”, źródło bez polskiej ścieżki: po starcie aplikacja sama próbuje następnego źródła (do 3 prób). NIEsprawdzone.
- B07: przycisk asystenta (lub usługa ułatwień) → u dołu „Słucham…”, wypowiedz „pauza” / „następny kanał”; wynik „… → polecenie ✓”. Na emulatorze: nakładka i błąd braku mowy; reszta NIEsprawdzona. Przy pierwszym użyciu system pyta o mikrofon.
- B08: Ustawienia (Prosty) → „Szukaj w telewizji”: wpisz „tvp”, wynik: kanały; OK na kanale otwiera Live TV. Sprawdzone tekstowo.
- B05-3: Ustawienia (Prosty) → „Moduły (wszystkie ustawienia)”: lista modułów z wyłącznikami. Sprawdzone.
- Tryb nocny: nakładka bursztynowa jest teraz nad treścią (wcześniej niewidoczna): „Tryb nocny: włączony” w Prosty → ekran lekko ciepły. NIEsprawdzone wzrokowo.
- Rzędy modułów na ekranie głównym działają tylko w układzie Classic; w Modern ich nie ma.
- Lektor: w odtwarzaczu z polskimi napisami powiedz „włącz lektora” (albo włącz „Włączaj automatycznie” w module Lektor): po chwili głos czyta kwestie, oryginał jest ściszany w trakcie kwestii; pierwsze włączenie pokazuje uwagę o Dolby. Ustawienia: głośność i ściszanie w module Lektor. Wymaga polskiego głosu w systemie (Ustawienia Android → mowa).
- „Napisy nie pasują” (głosem): przełącza na następny plik napisów w tym samym języku; trzecia próba proponuje wyłączenie napisów.
- Whisper offline: Ustawienia → moduł Sterowanie głosem → „Rozpoznawanie mowy offline (Whisper)”; wymaga pobranego modelu mowy (to samo, co synchronizacja napisów z dźwiękiem).
- Tryb nocny: „Tryb nocny: początek/koniec” przełączają godziny harmonogramu (21/22/23 i 06/07/08).
- Nastroje: Ustawienia (Prosty) → „Na co masz ochotę?”: wymaga klucza TMDB w `local.properties`; bez niego komunikat „Brak klucza TMDB”.
- „Sterowanie głosem: przykłady poleceń” — lista przykładów.
- Ustawienia 3 tryby (19.11): Zaawansowany — szyna „Szukaj” + 7 kategorii, ▶ do podkategorii, OK otwiera; Wstecz z panelu/kategorii wraca do drzewa; fokus w panelu grup Reshaped (np. Odtwarzanie → Dźwięk i obraz). Prosty — wiersz „Szukaj” nad kategoriami otwiera wyszukiwarkę. NOOB — nowy wygląd, przełączniki z plakietką, wybór poziomu. Pełne ustawienia (wyłącznik `fork_settings_nav` off): ▲ z pierwszej kategorii → „Szukaj w ustawieniach”.

## 22.11 (build163, gałąź claude/nuvio-tv-handoff-batch-1-4537d7)
- Lektor: film z polskimi napisami bez polskiego dźwięku, lektor Gemini — tło (muzyka, efekty) słychać lepiej niż aktorów, lektor wyraźny; log `mikser: ściszanie pasmowe`. Z włączonym trybem nocnym: log z „tryb nocny: medium — podbija dialogi”.
- Lektor: drugi seans tego samego filmu → log `pamięć: … z Dysku` / brak nowych zapytań Gemini; folder „Nuvio Lektor” na Dysku (tylko gdy TV zalogowany w synchronizacji Google).
- Napisy AI: film japoński/koreański → w powodach „Film spoza Europy … rosyjski i angielski”, AUX_WESTERN = en.
- Rozdziały: plik MKV z rozdziałami → kreski na pasku Niuni i panel rozdziałów.
- Omijanie reklam: program na żywo, który leciał wcześniej (powtórka) → po ok. 15 s karta „Możesz ominąć reklamy”, OK → archiwum + komunikat.
- Głos: komenda „następny odcinek” / „włącz Netflix”; cisza po naciśnięciu → log `bez mowy … Whisper pominięty` (bez zmyślonego tekstu).
- Catch-up (po buildzie z 598c92946): log `ForkCatchup … naprawdę N dni` dla TV Team.
- Autoplay tylko natywny (22.11): po pierwszym starcie Ustawienia → Odtwarzanie → Auto-play: tryb „Regex”, lista priorytetów = reguły odpowiedzi z kreatora (6 albo 8 pozycji, zaczynają się od `(?s)^`). Film z polskim dubbingiem (np. „W głowie się nie mieści 2”, TorrentClaw) → sam startuje plik z 🇵🇱 przy 🔊, nie ten z 🇵🇱 tylko przy 💬; w logu `ForkSettings` „autoplay Regex: profil … → <zestaw>”. Duża lista źródeł (kilkaset) na JMGO: autoplay ma ruszyć, a nie pokazać listę do wyboru (Nuvio daje regułom łącznie 2 s; na Macu 900 źródeł = 0,11 s). W ustawieniach Echo nie ma już: wyścigu źródeł, kontroli otwartego pliku, oszczędzania łącza, limitu łącza KE, Auto-play: rozdzielczość/kodek.
## Paczka „oczywiste” (21.11, gałąź `claude/paczka-oczywiste`)
- Pkt 1: archiwum IPTV — pasek pokazuje długość programu z EPG (nie 24 h); log `ForkNiunia: odtwarzacz: archiwum — długość z EPG`.
- Pkt 2: w archiwum Wstecz wychodzi bez pytania (`ForkTv: archiwum: Wstecz → wyjście bez pytania`), ▲▼ zmienia kanał od razu; po powrocie fokus na kaflu/kanale (`live tv: fokus wraca na …`).
- Pkt 3: kanał IPTV bez okna „Brak polskich napisów” (`okno napisów AI: pominięte (IPTV)`).
- Pkt 4: film — ◀▶ na pasku pokazuje miniatury nad paskiem (wymaga wygenerowanych klatek; pierwszy seans może ich nie mieć).
- Pkt 5: kanały bez logo pokazują nazwę (`logo kanału …: … → nazwa`).
- Pkt 6: filtr kategorii w Live TV — np. „TVN”, „Polsat” w „Ogólne” (`ForkKiciaLivetv: kategoria kanału …`, poziom debug).
- Pkt 7: „Beverly Hills Chihuahua” — na starcie polskie napisy (`ForkSubs: polskie napisy: …`), lektor czyta jedną polską ścieżkę (`ForkLektor: źródło: …`). Wymaga tego filmu i dodatku z włoskimi napisami.
- Pkt 8: SŁUCHOWO (tylko właściciel): lektor 100% = dawne 30%; „bez ściszania” — film tak głośny jak bez lektora (`PCM (lektor wł.): normalizacja downmiksu=false`).
- Pkt 9: typografia Satoshi na szczegółach programu archiwum i ekranach Nuvio (ocena wzrokowa).
- Pkt 10–11: plakaty w „Więcej podobnych” pionowe, karty stosu przezroczyste (ocena wzrokowa).
- Pkt 12: wyszukiwarka Niuni → „Powiedz” → „Słucham…”; log `ForkVoice: przycisk głosu (wyszukiwarka): systemowe=…`. Mowa do mikrofonu — tylko właściciel.

## build171 (22.11, Live TV z listami FlixNest — razem ok. 253 tys. pozycji)
- [ ] Wejście do Live TV: ekran nie zawiesza się (wcześniej komunikat „aplikacja nie odpowiada”); po kilku–kilkunastu sekundach kanały polskie są na górze, pojawiają się kategorie.
- [ ] Przewodnik: przy polskich kanałach z hls.gd widać programy (nie „Brak programu”); rząd kafelków u góry nie jest pusty.
- [ ] Po wyjściu z kanału (Wstecz) fokus wraca na ten sam kanał od razu.
- [ ] Reszta aplikacji (Główna, Kalendarz, YouTube) ładuje się szybciej niż w build169c (6–8 s).

## UX długie OK (22.11, build z paczki UX — po instalacji na JMGO)
- Pilot: przytrzymanie OK ~0,6 s na plakacie Głównej / Biblioteki / Szukaj / „Podobne” → okno „Akcje tytułu”; puszczenie OK NIE wybiera od razu „Szczegóły”.
- „Oglądaj dalej”: przytrzymanie → menu (Przejdź do szczegółów, Od początku, Odtwórz ręcznie, Obejrzane, Usuń); „Usuń” — fokus zostaje w rzędzie.
- Live TV: przytrzymanie na kanale w przewodniku i na kafelku → „Dodano do ulubionych: …”, drugi raz „Usunięto…”; filtr „Ulubione” pokazuje kanał.
- Strona serialu: przytrzymanie na „Sezon N” → „Sezon N: oznaczony jako obejrzany”, znaczniki odcinków.
- Po przytrzymaniu, które otworzyło okno, i zamknięciu go Wstecz: krótkie OK na tym samym plakacie działa od razu (wcześniej ginęło).
- (23.11, ux4/ux5) Strona tytułu: OK na osobie w „Obsada i twórcy” → strona osoby → Wstecz: fokus na tej samej osobie (na emulatorze obsada bez id TMDB — strona się nie otwiera, komunikat „Nie mamy strony tej osoby”). To samo dla plakatu z „Więcej podobnych”.
- (23.11) Przytrzymanie OK na odcinku → okno „Akcje odcinka” (Oznacz jako obejrzane, sezon, poprzednie, Odtwórz); puszczenie nic nie wybiera; Wstecz → fokus na odcinku.
- (23.11) Live TV, kanał, który nie działa → okno „Kanał nie odpowiada”: ◄► zmienia przycisk, OK „Następny kanał” włącza następny, „Spróbuj ponownie” — ten sam; jeden Wstecz wraca do listy (fokus na kanale).

## Szkło (fork/glass, 24.11)
- Telewizor z Androidem 13+ (API 33+): płynność przewijania rzędów pod otwartym menu i animacji hero ze szkłem (emulator liczy GPU programowo — nie mówi nic o wydajności TV); ewentualnie skala próbki 0,5 → 0,4.
- Wspólne okno bazy `NuvioDialog` (okno nie na cały ekran): czy szkło próbkuje właściwe miejsce ekranu (biblioteka liczy położenie względem okna; przy oknie wyśrodkowanym może brać lewy górny róg ekranu — przy ciemnym odcieniu 72 % mało widoczne).
- Panele odtwarzacza nad filmem: biblioteka nie widzi obrazu wideo (SurfaceView) — powinien być dawny wygląd panelu z obwódką szkła, bez przezroczystości.
- Kontrast przycisków hero przy bardzo jasnym obrazie (Czyste szkło: odcień biały 6 % na scrimie hero).
- JMGO (Android 11): pozycja „Szkło” wyszarzona, „Niedostępne na tym telewizorze”; wygląd jak przed zmianą; w logu „szkło wyłączone: API 30 < 33”.

## Czcionki napisów Atkinson i Tiresias (24.11)
- [ ] Po instalacji włącz dowolny film z napisami: napisy są czcionką Atkinson Hyperlegible (okrągłe, szerokie litery), polskie znaki (ą, ę, ł, ż) wyglądają dobrze.
- [ ] W odtwarzaczu: panel napisów → Czcionka: do wyboru Domyślna, Szeryfowa, Stała szerokość, Atkinson Hyperlegible, Tiresias. Tiresias zmienia wygląd napisów od razu.
- [ ] Ustawienia › Napisy i dźwięk › „Czcionki Atkinson i Tiresias”: wyłączenie usuwa obie z listy (napisy wracają do domyślnej), ponowne włączenie dodaje je i wybiera Atkinson.

## tv1 (24.11, Niunia Live TV: kraj i język, opcje kanału, okno błędu kanału)
- Live TV: ► po filtrach do końca → chip „Kraj i język” (ikona globusa) → OK → panel po prawej, fokus na zaznaczonej pozycji („Wszystkie kraje” z ✓); lista krajów z liczbą kanałów; „Polska” → panel znika, fokus na chipie (wybrany), w rzędzie i przewodniku tylko polskie kanały; chip „Język” → „Polski”; „Wszystkie …” zdejmuje filtr; po restarcie aplikacji wybór zostaje. Log: `live tv: kraj i język → …`, `… z … kanałów (… ms)`.
- Długie OK na kanale w przewodniku → „Opcje kanału · <kanał>”, fokus na „Dodaj do ulubionych”; Wstecz → fokus wraca na ten kanał. Długie OK na kafelku → te same opcje.
- „Wybierz przewodnik” → „Przewodnik dla: …”: ✓ na „Dobrany automatycznie”, inne z „teraz: …”; wybór innego → siatka tego kanału pokazuje jego program; „Bez przewodnika” → „Brak programu”; powrót „Dobrany automatycznie”.
- „Ramówka kanału” → lista dziś i jutro, fokus na „Teraz · …”; OK na trwającym → odtwarzacz, na minionym → archiwum albo komunikat „nie ma w archiwum”.
- „Ukryj kanał” → znika z listy, komunikat; w opcjach innego kanału „Pokaż ukryte kanały (1)” → wraca.
- Kanał, który nie rusza (dwie próby) → ekran „<kanał> nie odpowiada”, fokus na „Następny kanał”; ◄► przesuwa fokus; „Spróbuj ponownie” → ponowny start; „Lista kanałów” → przewodnik; Wstecz jak dotąd.

## tv2 (24.11, Niunia: reszta Z-2…Z-21)
- Odtwarzacz TV (Z-3): przyciski nad paskiem w jednym rzędzie (Poprzedni, Od początku, Pauza | Dodaj, Informacje, Więcej), pasek pod nimi, rząd programów wystaje niżej; ▼ z przycisków → pasek → rząd.
- Nagrody (Z-13): strona tytułu z nagrodami (np. Oppenheimer, Avengers: Endgame) → kafelki gal i trzy linie nagród w hero; log `NAwards: Wikidata … nagród`; drugie wejście `z dysku`.
- Odtwarzacz filmu (Z-14): wczytywanie „Buforuję… N%” z Inne źródło / Anuluj; zły link → „Nie udało się odtworzyć” z przyczyną, Inne źródło / Spróbuj ponownie / Wróć; Więcej → Zmień źródło: panel z ✓ przy obecnym, chipy dodatków; koniec odcinka → karta „Następny odcinek za N s”, Odtwórz następny / Oglądaj napisy końcowe.
- Serial z ocenami odcinków (Z-8): ★ w hero → tabela sezon × odcinek, chip „Tylko najlepsze 20%” (szare komórki pomijane), OK odtwarza, długie OK = strona odcinka; w „O serialu” kolorowe kapsułki ocen.
- Główna (Z-12): OK na plakacie w rzędzie → karta tytułu, fokus na Odtwórz; ► z (i) → następna karta; ◄ z Odtwórz → poprzednia; Wstecz → ten sam plakat.
- Biblioteka: zakładka „Historia” (Dziś / W tym tygodniu / Wcześniej; długie OK → Usuń z historii). Profil z PIN-em: klawisze Anuluj / 0 / Usuń, fokus na 5, cyfry z pilota.
- Źródła: chip „Filtry” → panel (Rozdzielczość, Dźwięk, HDR, Kodek, Dodatek), „Zapisz jako filtr” zostaje dla następnych tytułów, „Wyczyść”.
- Zobacz wszystko (Główna) → siatka Niuni po 6; strona tytułu → studio → Niunia „Studio”; Szukaj → Odkrywaj → Niunia „Odkrywaj”.
- Live TV (P-28): kanał, który nie ruszył → w przewodniku „Niedostępny” pod logo przez 24 h; po udanym włączeniu znika.
- API 31+ (emulator, Z-2): lewa część obrazu hero strony tytułu rozmyta. JMGO (Android 11): bez zmian (płaskie).
- Live TV (P-32): fokus w przewodniku → po ~1 s u góry po prawej obraz kanału na żywo (bez dźwięku) z „Na żywo · kanał”, po lewej tytuł, godziny i opis programu; ▲ do filtrów → wraca rząd kafelków; OK → pełny ekran bez zacięcia (log `podgląd zwolniony (pełny ekran)` przed startem odtwarzacza); szybkie przewijanie kanałów nie zawiesza przewodnika; HOME w trakcie → `podgląd zwolniony (aplikacja w tle)`.

## UX ręczne R14 (25.11, APK `uxr7`) — niesprawdzone na TV
- Szukaj i Live TV › Szukaj: klawiatura tylko a–z; „kuchenne”/„lodz” znajduje polskie tytuły.
- Ustawienia › System: pozycja „Narzędzia” (zamiast „Pomoc i dziennik błędów”).
- Ustawienia › Wygląd: wyłączony przełącznik widoczny na zaznaczonym wierszu.
- „Na co masz ochotę?”: od razu lista „Lekkie na dziś”.

## UX ręczne (24–25.11, APK `uxr6`) — niesprawdzone na TV
- Live TV: kanał nie działa → „Następny kanał” → Wstecz → fokus na ostatnio oglądanym kanale (nie na pierwszym).
- Główna: OK na plakacie → karta tytułu → ► na następny tytuł → Wstecz → fokus na tym tytule (rząd przewinięty).
- Live TV: ◄ z „Szukaj” (górny pasek) otwiera menu; Wstecz z Odkrywaj/Szukaj → fokus na tym przycisku.
- Live TV › Odkrywaj: bez powtórzonych „Kuchennych rewolucji”, bez CNBC/RT („Niedostępny”).
- Szukaj: klawiatura z ą ć ę ł ń ó ś ź ż; dwie podpowiedzi mieszczą się na ekranie.
- Zmień profil: „Stef+Patryk (Kaptain)” i „… Copy” w dwóch liniach, do odróżnienia.
- Główna › „Oglądaj dalej” › film YouTube → Wstecz z odtwarzacza → Główna (nie zakładka YouTube).

## Odtwarzacz VOD, wersja uxr16 (26.11; wyniki: `plan/ux/vod/WYNIKI.md`)

- [x] Film 4K: automat wybiera 4K bez REMUX — „F1”: 4K BluRay 22 Mb/s, płynnie (26.11 pętla 2).
- [ ] Gdy film ruszy tuż przed upływem 15 s, nie przełącza się sam na inny plik (log `ForkSources: start: film już gra`) — w pętli 2 nie wystąpiło.
- [ ] Więcej → Jakość: strzałki ▲▼ chodzą po liście jakości, OK na „1080p” przełącza plik i zostaje w tym samym miejscu filmu — ✘ w uxr16 (fokus na całym ekranie), poprawka V2b w uxr17.
- [x] Dźwięk → Ścieżka dźwiękowa: okno trzyma zaznaczenie (uxr16).
- [x] Serial bez polskich napisów: komunikat w lewym górnym rogu, Wstecz anuluje, film gra dalej („Taskmaster” S18 O1, uxr16).
- [x] Koniec odcinka: następny włącza się sam, z tego samego wydania (S18 O1 → O2, uxr16).

## Odtwarzacz VOD, wersja uxr17 (26.11 wieczorem; wyniki: `plan/ux/vod/WYNIKI.md`)

- [x] Po instalacji w logu `ForkSettings: reguły autoplay v5: profil 1, zestaw original_subs: 6 → 6 reguł (stare w kopii)`.
- [ ] V2b+V2c (uxr18): Więcej → Jakość — zaznaczenie na bieżącej jakości, ▼ przechodzi na 1080p (także po schowaniu się przycisków), OK przełącza plik w tym samym miejscu; po zamknięciu okna fokus na przyciskach.
- [x] V6: Dźwięk → Ścieżka dźwiękowa — wiersze: Oryginał, Polski (z liczbą źródeł), Angielski; bez japońskiego i koreańskiego.
- [ ] V7: „F1” (tt16311594) → Odtwórz kilka razy — ekran źródeł bez awarii.
- [ ] V8: „Taskmaster” S18 O3 (tt4934214:18:3, bez postępu — Wznów używa zapisanego pliku) — wybiera 1080p WEB-DL z TorBox, nie „Dream 4K VOD” (360p).
- [ ] V2c (uxr18): okno Ścieżka dźwiękowa i Napisy z panelu Niuni — zaznaczenie w oknie za każdym razem (3 próby).
- [ ] Okładki Live TV ze szczegółów AIOMetadata (TvCovers, wyłącznik `tv_covers_aio_meta`; log z prz11 sprawdzony 27.11: 6/12 trafień, wszystkie z kadrem ze szczegółów, 0 błędów — zostaje obejrzenie kart) — Live TV / archiwum IPTV, kilka kanałów; log `ForkTvCovers`: linie „kadr ze szczegółów wtyczki”, trafienia vs dawne 33 %, błędów 0; karty z poziomym kadrem zamiast przyciętego plakatu (np. „Świat według Kiepskich”, „Zabójcza broń”, „Piotruś Królik”).
