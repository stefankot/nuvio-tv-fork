# Funkcje starego forka (Kicia) a nowy fork (18.11.2026)

Źródło: `nuvio-tv-kicia-poprawki`, gałąź `fix/zgloszenia-jmgo` (`429d322`), 727 commitów. Na podstawie kodu i opisów w plikach.

## A. Przyjdą razem ze skórką Kicia (kod skopiowany do `skin/kicia`, wpięcia etapu 1–2 gotowe, niekompilowane)
- Ekran główny Kicii: hero z opisem karty pod fokusem, półki, rząd „YouTube dla Ciebie”, kafle kanałów TV, „Nowe odcinki” z przewodnika, szkielet przy pierwszym starcie, wczytywanie innych zakładek w tle.
- Szukaj: mikrofon, pole w pigułce, polska klawiatura ekranowa 12 kolumn, „Ostatnio szukane”, wyniki jako półki, Odkrywaj (Typ/Katalog/Gatunek) pod spodem.
- Biblioteka z podzakładkami i licznikiem nieobejrzanych; osobny ekran Historia.
- „Kto ogląda?”: okrągłe awatary, PIN jako cztery kółka.
- Kreator „Skonfiguruj dla tego TV” (krok 1; krok 2 bez wpięcia: limit 35 plików).
- YouTube: szyna z kanałami/feedami po polsku, własne sterowanie odtwarzacza YouTube (rozdziały, odcinki SponsorBlock na pasku).
- Odtwarzacz: sterowanie Kicii (zegar, „Koniec o”, przewijanie pilotem z przyspieszaniem), przycisk napisów z postępem AutoSync, ratunek fokusu, gdy kontrolki znikną.
- Wygaszacz z dużym zegarem; znaczek wersji builda w rogu; komunikaty (toast); rozmycie pod paskiem (także Android 11); kolory ocen.

## B. Są w skórce, ale wymagają etapu 3 (łatki w plikach Reshaped)
- Górny pasek zakładek Kicii (Szukaj, Home, Live TV, YouTube, Biblioteka) zamiast menu bocznego; reguła Wstecz.
- Live TV: nagłówek przewodnika (rząd ostatnio oglądanych programów + wybór dnia), drzewo kategorii z EPG (13 rodzin), widok programów kategorii, strona główna zakładki TV, szukanie w TV, MiniGuide, szuflada Info i odcinki.
- Archiwum: wyścig emisji (pierwsza działająca kopia wygrywa), propozycja wcześniejszej emisji z archiwum podczas oglądania na żywo, „Wstecz w archiwum pyta przed powrotem na żywo”.
- Zrzuty kanałów (miniatury tego, co leci) i podgląd wideo na kartach.
- Szczegóły filmu w wyglądzie Kicii (57 zmian w 5 plikach Nuvio), ustawienia z szyną (19 zmian).

## C. Nie ma ich w nowym forku ani w skórce (logika Kicii poza wyglądem)
- **Inteligentne auto-odtwarzanie** (M1–M2): trzy presety (np. Zbalansowany, Najlepsza jakość), czytanie nazw wydań (rozdzielczość, HDR/DV, kodek, rozmiar), aplikacja sama sprawdza dekodery i ekran, zgodność tytułu, budżet łącza, tryb dla słabych urządzeń, stara synteza mowy (IVONA/TTS) na końcu rankingu, lektor AI rozpoznawany.
- **Inteligentny ratunek** (M3): pamięć złych źródeł, „zły plik” (ten sam plik z innego dodatku nie jest kolejną próbą), sprawdzanie długości pliku i czy ma dźwięk, kontrola wolnego startu, przycisk „Spróbuj innego źródła”, wznowienie tym samym plikiem.
- **Mózg jakości** (M4): przyczyny problemów, poziomy, drabina następców, pamięć seansu (nowy fork ma tylko politykę Echo).
- Podglądy przewijania z TorBox i dopasowanie kolorów podglądu.
- YouTube: pamięć miniatur przewijania, przewijanie stron.

## D. Wspólne szukanie wszystkiego naraz (filmy, seriale, TV, YouTube)
- **W Kicii też go nie było.** Ekran Szukaj Kicii szuka filmów i seriali z dodatków; w kodzie jest notatka, że półki „Telewizja” i „YouTube” wymagają osobnych źródeł (`TODO(scalanie)` w `ForgeSearchScreen.kt`). Szukanie w TV było osobnym ekranem w Live TV, YouTube miał własne. Prawdopodobnie było w makiecie. Do zrobienia jako nowa funkcja: półki TV (z przewodnika) i YouTube dodane do ekranu Szukaj skórki.
