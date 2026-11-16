Jesteś architektem oprogramowania Android TV (Kotlin, Jetpack Compose, Hilt, Gradle). Masz dostęp do plików w katalogu `/Users/milajovovich/Desktop/nuvio 2/` i do internetu.

Zadanie: przeanalizuj stan lokalnego forka NuvioTV i napisz PLAN dokończenia prac. Nie pisz kodu i nie zmieniaj plików w `NuvioFork`. Wynik zapisz w `/Users/milajovovich/Desktop/nuvio 2/NuvioFork/ARCHITECT_PLAN.md`.

Najpierw przeczytaj, w tej kolejności:
1. `/Users/milajovovich/Desktop/nuvio 2/NuvioFork/ARCHITECT_BRIEF.md` (pełne zlecenie, wymagania i format wyniku)
2. `/Users/milajovovich/Desktop/nuvio 2/NuvioFork/HANDOFF.md` (co wdrożone, co zaparkowane, przyczyny, ścieżki)
3. `/Users/milajovovich/Desktop/nuvio 2/NuvioFork/STATUS.md`, `UPSTREAM_SOURCES.md`, `/Users/milajovovich/Desktop/nuvio 2/CLAUDE.md`

Kod forków źródłowych (tylko do odczytu) leży w `/Users/milajovovich/Desktop/nuvio 2/upstream/{nuvio,ke,reshaped,arc,echo,one}`.

Co ma zawierać plan:
- Rozwiązanie każdego z pięciu niewdrożonych problemów (A: Echo Auto Quality i przełączanie źródeł, B: KE testowanie łącza i sprzętu, C: KE powłoka/Home/TV Home wg wzorca DaoeZ/notflix, D: Arc kalendarz, E: Arc wyszukiwarka ustawień): jakie pliki, jakie cienkie adaptery, jakie punkty wpięcia, w jakiej kolejności, jakie ryzyko.
- Sprawdzenie w internecie, czy te funkcje istnieją w innych forkach NuvioTV lub nowszych wydaniach Nuvio w formie łatwiejszej do przeniesienia, i czy 1–2 forki pokryją większość brakujących funkcji zamiast pięciu kosztownych portów.
- Strategię modyfikowania plików `main`, żeby dało się dalej scalać nowe wydania bazy NuvioTV i forków (hooki jednolinijkowe, rejestr funkcji, łatki, osobne repozytoria lub podmoduły, testy kontraktowe), oraz redukcję 71 zmodyfikowanych plików rdzenia do ok. 35.
- Plan weryfikacji na emulatorze Android TV oraz uporządkowaną listę maks. 15 kroków z szacunkiem pracy i ryzyka, z wyraźnym wskazaniem funkcji, które warto porzucić.

Wykonawcą planu będzie słabszy model, który pracuje po polsku, autonomicznie i potrzebuje jednoznacznych instrukcji. Nie zakładaj, że istniejący podział na pięć forków jest optymalny.
