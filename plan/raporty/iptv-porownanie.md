# IPTV: Kicia, obecny fork i plan (18.11.2026, sprawdzone w kodzie)

Kicia = `~/Desktop/nuvio-tv-kicia` @ 34c80c6. Fork = gałąź `hybryda-zrodel`. Plan = `plan/bloki/B01–B03, B08`.

## Jest w forku i działa w interfejsie (tak samo jak w Kicii albo z planu)
- Przewodnik z drugiego źródła dla Polski, logo kanałów, szczegóły programów z EPG, okno 7 dni wstecz / 8 naprzód (B01).
- Rząd „Dla Ciebie” nad przewodnikiem i na ekranie głównym, znaczki „obejrzane”, strona programu, przypomnienia, alert nowego odcinka (B02/B03).
- Pamięć postępu programów i obserwowane seriale (B02-4, B02-6) — tego Kicia NIE miała.
- Skórka Kicia (gdy wybrana): przeglądanie kategorii EPG (`ForgeEpgCategories`, `ForgeTvBrowse`), wyszukiwarka TV Kicii, karta programu, mini-przewodnik podczas oglądania, propozycja oglądania od początku z archiwum (`ForgeArchiveOffer`), karta końca archiwum, wyścig źródeł emisji (`ForgeAiringRace`: wybiera działające źródło odcinka), podglądy kanałów na kartach.
- Szukaj w telewizji i Odkrywaj: od dziś na całym przewodniku, nie tylko na archiwum (B08).
- Start HLS od dowolnej klatki I (kanały, które wisiały na „Buffering…”).

## Jest w Kicii, brak w forku
- **Szybkie EPG na starcie**: Kicia zapisuje osobno wycinek „teraz” (3 h wstecz .. 12 h naprzód, `guide_now.bin.gz` w `LiveTvGuideCache`) i pokazuje go przed pełnym przewodnikiem (na JMGO pełny czytał się 3,4–3,8 s). W forku przewodnik pokazuje się dopiero po odczycie całości. Wymaga zmiany w plikach danych Live TV (Reshaped), które fork już łata pod dostawcę — trzeba scalić ręcznie.
- **Kalendarz przewodnika** (`ForgeGuideCalendar`, wybór dnia): plik skopiowany, nigdzie nie użyty.

## Z planu, nie zrobione
- B08-4: okładki w tle dla obserwowanych i pierwszych rzędów Odkrywaj.

## W skórce Nuvio (domyślny wygląd bazy)
Funkcje Kicii z listy wyżej są widoczne tylko przy skórce Kicia. W wyglądzie Nuvio: przewodnik Reshaped + moduły forka (Dla Ciebie, strona programu, Szukaj/Odkrywaj w Narzędziach).

## Propozycja
1. Przenieść szybkie EPG „teraz” z Kicii (łatka na `LiveTvGuideCache` + odczyt w `LiveTvRepository`), po kompilacji.
2. Podpiąć kalendarz dnia w nagłówku przewodnika skórki Kicia.
