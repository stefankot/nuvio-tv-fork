# DO SPRAWDZENIA NA TELEWIZORZE

Lista dla właściciela. Wykonawca dopisuje tu wszystko, czego nie da się sprawdzić bez prawdziwego urządzenia. Jedna pozycja = co kliknąć i czego się spodziewać. Właściciel odhacza albo dopisuje „nie działa: ...”.

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
- Autoplay tylko natywny (22.11): po pierwszym starcie Ustawienia → Odtwarzanie → Auto-play: tryb „Regex”, lista priorytetów = reguły odpowiedzi z kreatora (6 albo 8 pozycji, zaczynają się od `(?s)^`). Film z polskim dubbingiem (np. „W głowie się nie mieści 2”, TorrentClaw) → sam startuje plik z 🇵🇱 przy 🔊, nie ten z 🇵🇱 tylko przy 💬; w logu `ForkSettings` „autoplay Regex: profil … → <zestaw>”. Duża lista źródeł (kilkaset) na JMGO: autoplay ma ruszyć, a nie pokazać listę do wyboru (Nuvio daje regułom łącznie 2 s; na Macu 900 źródeł = 0,11 s). W ustawieniach Echo nie ma już: wyścigu źródeł, kontroli otwartego pliku, oszczędzania łącza, limitu łącza KE, Auto-play: rozdzielczość/kodek.
