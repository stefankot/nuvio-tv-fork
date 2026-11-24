# Test tv2e na JMGO (24.11, ~20:00–20:15 czasu JMGO), tekstowo + 3 zrzuty

Zainstalowany tv2e (md5 63a75317…, poprawki po tv2c). Wyniki:
- Podgląd na żywo (P-32, zrzut): obraz kanału u góry po prawej (CNBC HD, EuroNews), brzegi w tło, „Na żywo · EuroNews”, po lewej tytuł / LIVE / godziny / kanał — jak makieta LiveTVRow. Odtwarzacz podglądu Reshaped: obraz także na kanałach bez .m3u8.
- Wybierz przewodnik: lista od razu (8 kopii CNBC z pełnymi nazwami „Strong8K · AT: CNBC…”), fokus w panelu; wybór innego → siatka kanału pokazuje jego program; przywrócony „Dobrany automatycznie”. Drobna poprawka (w kodzie, bez builda): „Teraz: CNBC” → pełna nazwa przewodnika.
- Kraj i język: bez „GMT”, „BK”; fokus na zaznaczonej pozycji; przywrócone „Wszystkie kraje” (filtr „Polska” ustawiony w teście tv2c).
- Nagrody (Wikidata): Breaking Bad 42 nagrody + 64 nominacje; hero (zrzut): „42 nagrody + 64 nominacje / Gale Emmy 15+25 · Złoty Glob 1+1 i 5 innych / Kategorie Aktorstwo 17+20 i 10 innych”.
- Tabela ocen (zrzut): etykiety „Sezon N” stoją przy przewijaniu w prawo, sezon z fokusem biały.
- Ekran wczytywania filmu Niuni: pojawił się przy (przypadkowym) starcie S1E1 Breaking Bad — trasa z playOnLoad+manualSelection od razu odtwarza; wyjście Wstecz. Skutek: Breaking Bad S1E1 w „Oglądaj dalej” (~1 min) — do usunięcia przez właściciela (usuwanie przez test zablokowane).
- Okno akcji „Oglądaj dalej” (długie OK): Wznów od…, Szczegóły serialu, Oznacz jako obejrzane, Usuń z „Oglądaj dalej”; Wstecz wraca na kartę.
- Zobacz wszystko: koniec rzędu → „Popular · Cinemeta · 50+”, siatka; Wstecz → karta „Zobacz wszystko”.
- Do wyjaśnienia (nie z tej paczki, był też w tv2c): czarny prostokąt „Gotowe” (android.widget.TextView w oknie aplikacji) w lewym dolnym rogu na stronie tytułu i w tabeli ocen.
- Niesprawdzone: PIN profilu (trasa profili nie otwiera się intencją, brak profilu z PIN do testu), filtry źródeł i „Zmień źródło” (wymagają odtwarzania), ekran błędu i karta następnego odcinka, status „Niedostępny”, rozmycie API 31+.
