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
