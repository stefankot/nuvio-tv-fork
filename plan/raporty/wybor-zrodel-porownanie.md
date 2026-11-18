# Automatyczny wybór źródła: nowy fork, Echo, KE, Kicia (18.11.2026, z kodu)

## Co nowy fork ma dziś
- Wybór z bazy Nuvio/Reshaped: „pierwsze źródło” albo reguły regex z rankingiem; selektor = plik Reshaped bez zmian.
- Moduł `fork/sources`: trzy odpowiedzi „polski dźwięk czy jakość” (zestawy regex w `source_presets.json`), sprawdzenie, czy plik ma polską ścieżkę, i przejście do następnego źródła.
- Reshaped: automatyczna próba innego źródła (3 próby, 15 s).
- Echo: wyścig źródeł (8 linków naraz, wygrywa pierwszy działający), preferowana rozdzielczość i kodek, zamiana jakości w locie, przejście na następne źródło po błędzie, wczytywanie źródeł zawczasu na ekranie szczegółów.
- Echo NIE przeniesione: „gdy wszystkie preferowane dodatki są puste, graj pierwszy z pozostałych” (`fallbackWhenPreferredSourcesEmpty`).

## KE (nie przeniesione; najdokładniejsza ocena jakości)
- Parser faktów o wydaniu (634 linie): rozdzielczość, źródło (REMUX/BluRay/WEB-DL…), HDR/DV, dźwięk, kodek, grupa wydania, rozmiar, edycje, języki.
- Ranking zgodny z TRaSH Guides: rozdzielczość → jakość źródła → grupa wydania (poziomy TRaSH) → obraz → dźwięk (TrueHD) → kanały → kodek → rozmiar → kontener; wykluczenia.
- Wykrywanie podejrzanych plików (fałszywa jakość, rozmiar nieprawdopodobny dla rozdzielczości).
- Strażnik tożsamości (czy plik to ten tytuł i rok) i pamięć porzuconych wydań („zły plik”).
- Preferowane źródło na serial; wczytywanie i wybór zawczasu (szczegóły, „Kontynuuj oglądanie”, następny odcinek) z linią „gotowe”.
- Limity rozdzielczości i bitrate dla danego telewizora, zapisane filtry listy źródeł.

## Kicia (przeniesione tylko jako pliki na gałąź `kicia-zrodla-wip`, nic nie podpięte)
- Presety (np. Zbalansowany, Najlepsza jakość) i własny, prostszy parser nazw (285 linii). Kicia przejrzała ranking KE i świadomie napisała prostszy (commit „M2b faza A: audyt dawcy KE”).
- Czego nie mają KE ani Echo: aplikacja sama sprawdza dekodery i ekran urządzenia, budżet łącza z pobranych danych, nauka na wynikach (dodatek + usługa), sprawdzanie długości pliku i dźwięku, rozgrzewka połączenia przed Odtwórz, bufor dopasowany do bitrate, ciche P2P, gdy debrid nie da linku, „Spróbuj innego źródła”, AVI/WMV przez mpv.
- Własna kopia Echo (zamiana jakości, wyścig): dubluje Echo z nowego forka, nie przenosić.

## Gdzie się nakładają
Wszystkie trzy mają własny parser nazw i ranking. W aplikacji może działać tylko jeden ranking naraz. Reszta (wyścig Echo, kontrole i rozgrzewka Kicii, prefetch i strażnik tożsamości KE) da się łączyć.

## Propozycja
1. Ranking i parser z **KE** (najdokładniejszy, zgodny z TRaSH), jako moduł z wpięciem w selektor.
2. Z **Kicii** tylko to, czego KE i Echo nie mają: sprawdzanie urządzenia, nauka na wynikach, kontrola długości i dźwięku, rozgrzewka, bufor, P2P, „Spróbuj innego źródła”, AVI/WMV.
3. **Echo** zostaje bez zmian (wyścig, zamiana w locie); dodać jego brakujący fallback.
