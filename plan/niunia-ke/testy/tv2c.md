# Test tv2c na JMGO (24.11, ~19:30–19:50 czasu JMGO), tekstowo (uiautomator) + 3 zrzuty

Zainstalowany tv2c (md5 2db9e9a8…). Wyniki:
- Live TV, kraj i język: chip na końcu filtrów (15. pozycja), panel z fokusem na „Wszystkie kraje” ✓, liczby kanałów z odmianą; „Polska” → 348 z 58 622 kanałów (209 ms w tle), fokus wraca na chip. BŁĄD: na liście „GMT”, „BK” (kody z nazw, nie kraje) → poprawione (tylko ISO).
- Opcje kanału (długie OK): panel z fokusem na „Dodaj do ulubionych”, wszystkie pozycje; Wstecz → fokus na tym samym kanale. Ramówka: dziś i jutro, fokus na „Teraz”. Wybierz przewodnik: BŁĘDY — szukanie kilka sekund (shown() dla 58 tys. kanałów), w tym czasie fokus wypadał spod panelu, wszystkie kopie pokazane jako „CNBC” → poprawione (tani test nazwy, wiersz z fokusem, pełne nazwy).
- Podgląd na żywo: pierwsza klatka na RT HD; BŁĄD na kanale bez .m3u8 (PARSING_CONTAINER_UNSUPPORTED) → zamiana na odtwarzacz podglądu Reshaped (HLS bez .m3u8, klatki bez IDR). Przed pełnym ekranem „podgląd zwolniony (pełny ekran)”.
- Odtwarzacz TV (Z-3, zrzut): przełącznik kanałów, info, przyciski nad paskiem z separatorem, pasek z „LIVE” — zgodnie z makietą. BŁĄD: ekran wczytywania filmu („Buforuję…”, Inne źródło / Anuluj) nakładał się na Live TV → poprawione (Live TV bez nakładek filmu).
- Główna, karta tytułu (Z-12, zrzut): OK na plakacie → karta 1/50, fokus Odtwórz; ► z (i) → karta 2, ◄ z Odtwórz → karta 1; Wstecz → ten sam plakat.
- Breaking Bad: przycisk „Oceny odcinków”, średnia 9,2 z 5 serwisów; tabela 62 odcinki, próg 8,1, filtr 20% (zrzut: szare komórki pomijane). BŁĄD: przewijanie w prawo chowało etykiety „Sezon N” → poprawione. Nagrody z Wikidata: SocketTimeoutException na JMGO (z Maca 1,7 s, 144 wiersze) → limit 25 s i jedna ponowna próba.
- Biblioteka: zakładka Historia, 45 pozycji w 3 rzędach. Odkrywaj: chipy typów i katalogów, 50 wyników (fokus startowy na niewybranym typie → poprawione). Studio (Marvel Studios): nagłówek i 6 rzędów (podwójna spacja → poprawione).
- Niesprawdzone: PIN profilu, filtry źródeł, ekrany wczytywania / błędu / następnego odcinka filmu, „Zobacz wszystko”, status „Niedostępny”, rozmycie API 31+.
