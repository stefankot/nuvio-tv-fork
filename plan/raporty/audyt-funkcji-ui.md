# Audyt interfejsu: co użytkownik widzi, a czego nie (18.11.2026)

Na podstawie kodu, bez uruchamiania. Ułożony według miejsc w aplikacji, tak jak chodzi po niej użytkownik z pilotem.

Oznaczenia: ✅ jest i da się użyć · 👻 funkcja istnieje, ale nie ma do niej drzwi · ⬜ ekran jest, ale pusty · ❌ nie powstało.

---

## 1. Menu boczne
- ✅ Live TV, Kalendarz, Narzędzia.
- 👻 YouTube i katalog kanałów KE nie mają pozycji w menu. Są zagrzebane w Ustawieniach.
- 👻 Nie ma przycisku mikrofonu, czyli nigdzie w aplikacji nie da się zacząć mówić.

## 2. Ekran główny
- 👻 Rzędy „Telewizja: dla Ciebie” i „Odkrywaj” pokazują się tylko w układzie **Classic**. Domyślny układ to **Modern**, więc większość osób ich nie zobaczy.
- ❌ Rzędy nastrojów („Lekkie na dziś”, „Wymagające”, „Nieodkryte perełki”…) nie powstały. Nastroje są tylko jako osobny ekran w Narzędziach.
- ❌ Skórka „Układ KE” (wygląd ekranu głównego z Nuvio KE) nie powstała. Jest tylko pusta rama na przyszłą skórkę.

## 3. Narzędzia (hub)
- ⬜ **Szukaj w telewizji**: znajduje kanały po nazwie, ale nie znajduje programów z przewodnika. Przeszukuje tylko programy z archiwum (catch-up). Brakuje ostatnich wyszukiwań i kafli gatunków z planu.
- ⬜ **Odkrywaj telewizję**: rzędy biorą się tylko z archiwum, więc bez kanałów z catch-up ekran pokazuje sam przycisk „Szukaj”.
- ⬜ **Na co masz ochotę?**: kafle nastrojów są. Po kliknięciu lista może wyjść pusta, bo filmy pobiera z bazy TMDB, a w tej kopii projektu nie ma klucza TMDB. Brakuje ankiety gustu z planu.
- ✅ **Sterowanie głosem: przykłady poleceń**: lista zdań. Tylko że głosu nie da się włączyć (punkt 1).
- ✅/👻 **Aplikacje**: lista była zawsze pusta (aplikacja nie miała zgody na widzenie innych aplikacji). Poprawione dziś, jeszcze niesprawdzone.
- ✅ **Moduły**: długa lista wyłączników. To jedyne miejsce, gdzie widać wszystkie funkcje forka, i nie ma go w zwykłych Ustawieniach.

## 4. Live TV i przewodnik
- ✅ Rząd „Dla Ciebie” nad przewodnikiem, znaczki „obejrzane” w siatce, strona programu, przypomnienia, potwierdzenie przy zmianie kanału w trakcie archiwum, logo kanałów, drugi przewodnik dla Polski (działa w tle).
- ❌ Panel grup kanałów z licznikami (Ulubione / Twoje kanały / Ostatnie / grupy).
- ❌ Mini-przewodnik w trakcie oglądania (OK pokazuje kartę programu, strzałki: kanał i „co dalej”).
- ❌ Pauza na żywo z przewijaniem („−12 min”, „Wróć do na żywo”).
- ❌ Komunikat „N z M kanałów ma program” i lista kanałów bez programu.
- Dla tych czterech przygotowano logikę, ale nie ekrany.

## 5. Ekran filmu (szczegóły)
- ✅ Linia „Gotowe · 1080p · lektor PL” pod przyciskiem Odtwórz.
- ✅ Źródła ładują się zawczasu (szybszy start).

## 6. Odtwarzacz
- 👻 **Lektor**: działa w tle (czyta polskie napisy syntetycznym głosem i ścisza film), ale w menu dźwięku nie ma pozycji „Lektor”. Sam włącza się tylko po zaznaczeniu ukrytej opcji i ustawieniu profilu „po polsku”. Nie zadziała, gdy dźwięk idzie wprost do amplitunera (Dolby). Nie ma wyboru głosu.
- 👻 **Napisy nie pasują**: brak pozycji w menu napisów. Wywołać ją można tylko głosem, a głos nie działa.
- 👻 **Zmiana jakości w locie** (Echo: Auto Quality + hot swap): działa, ale trzeba włączyć trzy osobne przełączniki w ustawieniach, wszystkie domyślnie wyłączone. Zaczyna działać od następnego filmu.
- ✅ Wyścig źródeł, menu języka audio, intro, automatyczna próba innego źródła, sprawdzanie, czy jest polska ścieżka.
- 👻 Pilot Arc: można przypisać przyciskom różne zadania, ale nie ma wśród nich „Sterowanie głosem” ani „Lektor”.

## 7. Sterowanie głosem (całość)
- 👻 Rozumienie poleceń (ok. 60 wzorców: kanały, przewodnik, przewijanie, napisy, lektor, nawigacja) i dymek „Słucham…” są gotowe. Nie ma jednak żadnego sposobu, żeby je wywołać z pilota:
  - brak przycisku mikrofonu,
  - brak zadania w pilocie Arc,
  - Google TV nie pozwala wybrać Nuvio jako asystenta,
  - trzecia droga (specjalna usługa w ustawieniach systemu Android + ukryty przełącznik) wymaga wiedzy technicznej.
- ❌ Rozumienie innych sformułowań („daj mi sport”) bez internetu i wersja przez model AI z kluczem: nie powstały.

## 8. Ustawienia
- ✅ Ekran „Prosty” (do 20 pozycji), tryb nocny z godzinami i ciepłą nakładką, pytanie „polski dźwięk czy jakość”, ekran główny telewizora / start z telewizorem / blokada wyjścia.
- 👻 Funkcje forków mają trzy różne miejsca (sekcja Reshaped w zwykłych Ustawieniach, ekran Prosty, ekran Moduły w Narzędziach). Dla Echo trzeba szukać w sekcji Reshaped.

---

## Dlaczego tak wyszło

Wymóg zgodności z innymi forkami (żeby łatwo aktualizować) **nie był** przyczyną. Każde brakujące „drzwi” to jedna linijka w ekranie, który i tak ma już takie linijki.

Przyczyną był sposób rozliczania pracy. W planie zadanie liczyło się jako zrobione, gdy przeszedł test logiki, a nie gdy użytkownik mógł funkcję kliknąć. Wykonawca odkładał każde dodanie przycisku w ekranach odtwarzacza, ekranu głównego i menu na „później” (lista BLOKADY) i szedł dalej.

## Co trzeba dodać, żeby funkcje były używalne (od najważniejszych)
1. Przycisk mikrofonu w menu bocznym i zadanie „Sterowanie głosem” w pilocie Arc.
2. Pozycja „Lektor” w menu dźwięku odtwarzacza i „Napisy nie pasują” w menu napisów.
3. Rzędy modułów także w układzie Modern ekranu głównego.
4. Szukaj i Odkrywaj oparte na całym przewodniku, nie tylko na archiwum.
5. YouTube i katalog KE w menu bocznym (albo w Narzędziach).
6. Zmiana jakości w locie: jeden przełącznik zamiast trzech, domyślnie włączony.
7. Jedno miejsce na wszystkie ustawienia forka.
8. Panel grup kanałów, mini-przewodnik i pauza na żywo w Live TV.
