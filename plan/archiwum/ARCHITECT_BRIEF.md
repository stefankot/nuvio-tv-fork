# Zlecenie dla architekta (innego, mocniejszego modelu AI)

Przeczytaj najpierw: `HANDOFF.md` (stan i problemy ze ścieżkami), `STATUS.md`, `UPSTREAM_SOURCES.md`, `../CLAUDE.md` (zasady i preferencje użytkownika).
Katalog projektu: `/Users/milajovovich/Desktop/nuvio 2/` (`ROOT`). Fork: `ROOT/NuvioFork` (git). Klony upstream do odczytu: `ROOT/upstream/{nuvio,ke,reshaped,arc,echo,one}`.
Nie zmieniaj plików w `NuvioFork`. Twoim wynikiem ma być PLAN w pliku `ROOT/NuvioFork/ARCHITECT_PLAN.md` (nie kod).

## Cel projektu
Lokalny fork Android TV na bazie NuvioTV `dev` (commit `2c853c1`), do którego dokładamy funkcje z pięciu forków (KE, Reshaped, Arc, Echo, Nuvio One). Wymagania użytkownika:
- Każdy fork jako osobny moduł (katalogi, bez przepisywania kodu; wyjątek: YouTube wymaga modułów Gradle) z cienkimi adapterami.
- Maksymalnie ok. 35 plików rdzenia (`main`) zmodyfikowanych punktami wpięcia, każde wpięcie wyłączalne w ustawieniach (jedna wspólna kategoria ustawień).
- Kod 1:1 z forków, oficjalny odtwarzacz zostaje, nic nie wychodzi poza lokalny komputer.
- Zakres użytkownika: IPTV/EPG tylko z Reshaped; z KE tylko katalogi IPTV, filtry kanałów w EPG oraz testowanie łącza i sprzętu; pilot Arc w całej aplikacji; Dolby Vision i HDR10 z Reshaped; aktualizator Reshaped wyrzucony; rozpoznawanie mowy wielojęzyczne.

## Co zostało zrobione
Szczegóły i ścieżki: `HANDOFF.md`, sekcja 1. Skrót: baza kompiluje się; Reshaped (759 nowych plików + 71 plików rdzenia zmienionych), YouTube, katalog IPTV z KE (most do Reshaped przez M3U), przełączniki w ustawieniach, w toku pilot Arc i wybór modelu mowy (Whisper).

## Czego nie udało się zrobić (problemy z przyczynami: `HANDOFF.md`, sekcja 2)
A. Echo: Auto Quality, wyścig i przełączanie źródeł.
B. KE: testowanie sprzętu i łącza urządzenia.
C. KE: przełączana powłoka, Home, Layout Settings, TV Home (wzorzec `DaoeZ/notflix`); nie analizowane.
D. Arc: kalendarz.
E. Arc: wyszukiwarka ustawień.
Oraz problem strukturalny G: Reshaped zmienia 71 plików rdzenia (limit ok. 35), część plików wzięta w całości.

## Zadania dla ciebie
1. **Rozwiąż każdy problem A–E**: dla każdego wskaż jasne rozwiązanie z listą plików do skopiowania, nowych adapterów (nazwa, odpowiedzialność, sygnatury), punktów wpięcia w rdzeniu (plik, miejsce, ile linii) i kolejnością kroków. Nie pisz kodu; opisz tak, żeby słabszy model mógł to wykonać bez zgadywania. Oszacuj ryzyko i koszt (liczba plików rdzenia).
2. **Szukaj łatwiejszych źródeł.** Czy te same funkcje istnieją w innych forkach NuvioTV (lub w samym Nuvio w nowszej wersji) w formie, która mniej koliduje z naszą bazą? Przeszukaj sieć (GitHub: forki `NuvioMedia/NuvioTV`, `tapframe/NuvioTV` i pochodne; wyszukaj Auto Quality, source race, calendar, settings search, device assessment, speed test, IPTV catalog). Dla każdego kandydata podaj: URL, bazę (jaka wersja Nuvio), jak blisko jest naszej bazy `dev`, czy kod jest samodzielnym modułem. Sprawdź, czy dwa forki (np. Arc i Echo, albo Reshaped i KE) pokrywają większość brakujących funkcji, tak aby zastąpić kilka kosztownych portów jednym. Nie zakładaj, że obecny podział (5 forków) jest optymalny; zaproponuj najtańszy zestaw źródeł.
3. **Strategia scalania (najważniejsze długoterminowo).** Zaproponuj, jak zmodyfikować pliki rdzenia (`main`), aby można było dalej scalać nowe wydania bazy (NuvioTV `dev`) i nowe wydania forków bez ręcznych konfliktów. Rozważ m.in.: punkty wpięcia jako jednolinijkowe wywołania do interfejsów w osobnym pakiecie (`ForkHooks`); rejestr funkcji (feature registry) z `ServiceLoader`/Hilt multibindings zamiast edycji `NuvioNavHost`, `SettingsScreen`, `PlayerScreen`; pliki zastępcze zamiast wklejania całych plików Reshaped; zestaw łatek (`git format-patch` na zmianach rdzenia) trzymany obok modułów, z automatycznym skryptem `rebase`; osobne repozytoria lub podmoduły git dla każdego modułu z oznaczeniem wersji upstream; testy kontraktowe sprawdzające, że punkty wpięcia nadal istnieją po aktualizacji. Wskaż konkretnie, które z 71 zmodyfikowanych plików da się zredukować do jednolinijkowych hooków, a które wymagają osobnej strategii (np. rozdzielenie `PlayerRuntimeController` na część upstream i część fork).
4. **Zredukuj liczbę zmodyfikowanych plików rdzenia** do ok. 35: zaproponuj, które pliki Reshaped (lista w `HANDOFF.md`) zastąpić hookami lub przenieść do rozszerzeń (extension functions, delegaci), a które trzeba zostawić.
5. **Plan weryfikacji.** Proponuj minimalny zestaw kontroli na emulatorze Android TV lub telewizorze po każdej funkcji (co kliknąć, czego się spodziewać), bo dotąd nic nie było uruchamiane.
6. **Rekomendacja końcowa.** Jedna uporządkowana lista kroków (maks. 15) z szacunkiem pracy i ryzyka; zaznacz funkcje, które radzisz porzucić jako nieopłacalne, z uzasadnieniem.

## Ograniczenia środowiska (ważne dla planu)
- Mac z 8 GB RAM; kompilacja Kotlin ~4 min, pełne APK ~10 min; Gradle z `-Xmx3072m`.
- Wykonawca planu to słabszy model, który pracuje autonomicznie, krótko i po polsku; potrzebuje jednoznacznych instrukcji.
- Użytkownik nie jest techniczny; nie wolno go prosić o uruchamianie skryptów.
- Nie zajmować się tłumaczeniami (poza poprawkami duplikatów zasobów).

## Format wyniku
Plik `ROOT/NuvioFork/ARCHITECT_PLAN.md` z sekcjami: 1) Podsumowanie i rekomendacja, 2) Rozwiązania A–E, 3) Alternatywne źródła (tabela), 4) Strategia scalania, 5) Redukcja plików rdzenia, 6) Plan weryfikacji, 7) Kroki do wykonania (max 15). Na końcu krótka lista pytań do użytkownika, jeśli jakaś decyzja należy do niego.
