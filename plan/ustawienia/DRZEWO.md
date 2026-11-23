# Drzewo ustawień (23.11) — projekt i uzasadnienie

Źródło prawdy: `app/src/main/java/com/nuvio/tv/fork/settings/tree/SettingsTreeSpec.kt` (`layout`). Ten plik opisuje, DLACZEGO tak.
Ekran: `skin/niunia/settings/NiuniaSettingsTree.kt` (skórka Niunia, wszystkie poziomy). Wyłącznik: `fork_settings_tree` (System › Ustawienia › Więcej › „Nowy układ ustawień”); wyłączony = dawne ekrany.

## Problem (właściciel 23.11)
„Ustawienia są bardzo trudne w obsłudze, jest ich dużo i są rozmieszczone nieintuicyjnie. Nie wchodzę nawet do ustawień, bo wiem, że niczego nie znajdę.”
Przed zmianą: 3 osobne ekrany (NOOB / Prosty / Zaawansowany) z innym układem każdy; 17 kategorii Nuvio nazwanych technicznie („Nuvio Reshaped”, „Nuvio Forge”, „Integracje”); kategoria Reshaped = 21 grup luzem na jednej liście; Forge = 12 pozycji luzem; te same ustawienia w 2 miejscach (autostart, ekran główny, asystent, karta przypomnień, filtr nocny); szukanie tylko po angielsku (patrz `INWENTARZ.md`).

## Zasady podziału
1. **Grupowanie według celu, nie według pochodzenia kodu.** Kategorie nazwane tym, co użytkownik chce zrobić: Oglądanie, Napisy i dźwięk, Telewizja, Wygląd, Konta i dodatki, Pilot i głos, System. Nie ma „Reshaped”, „Forge”, „Integracji” — ich pozycje są rozłożone tam, gdzie się ich szuka (np. Dolby Vision → Oglądanie › Obraz, AutoSync → Napisy).
2. **7 kategorii** (pamięć robocza, pilot: 7 ruchów ▼ to cała szyna); 2–4 grupy w kategorii, w grupie najwyżej 9 widocznych pozycji (test JVM).
3. **Dwa poziomy w głąb:** szyna kategorii → panel z grupami i pozycjami. Pozycja otwiera ekran/okno albo działa od razu (przełącznik, wybór z wartością). Nagłówki grup nie biorą fokusu.
4. **Stopniowe odkrywanie (progressive disclosure):**
   - Tryb prosty („NOOB”) = **podzbiór tego samego drzewa** (pozycje oznaczone NOOB, 20), nie osobny ekran; na końcu „Pokaż wszystkie ustawienia”.
   - Tryb pełny: rzadsze ustawienia w tej samej grupie pod „Więcej (n)” (rozwija na miejscu, „Mniej” zwija). Zaawansowane są tam, gdzie ich kuzyni, a nie w osobnym „Zaawansowanym”.
   - Przełącznik trybu: System › Ustawienia › „Tryb ustawień: Prosty / Pełny”.
5. **Najczęstsze na górze:** w każdej grupie kolejność wg częstości (np. Napisy: rozmiar, język i wygląd, polskie z filmu, równo z mową, czcionka).
6. **Bez duplikatów:** jedna pozycja na jedno ustawienie (test JVM: id, nazwy, cele). Przy dublach wybrano wersję z modułów forka (launcher/voice), Forge-owe zostały tylko w dawnym ekranie.
7. **Krótkie polskie nazwy bez słów technicznych** + opis jednym zdaniem (na dole panelu dla pozycji z fokusem). Nazwy z modułów nadpisane prostszymi tam, gdzie trzeba („AI napisy: klucz, model, limity, zapisane” → „Napisy AI: tłumaczenie na polski”).
8. **Pilot ▲▼◄► OK Wstecz:** fokus startowy na kategorii (ostatnio używanej); ▲▼ po szynie od razu zmienia panel; ► wchodzi w panel na pierwszą pozycję; ◄ wraca na szynę; OK działa; Wstecz: z panelu → szyna, z szyny → menu Niuni; po powrocie z ekranu/okna fokus wraca na tę samą pozycję.
9. **Wybory z wartością zamiast wielu wierszy:** „Polski dźwięk czy jakość”, „Rozmiar napisów”, „Tryb nocny”, „Po uruchomieniu pokaż”, „Logo kanałów” — jeden wiersz z wartością, kolejne OK = następna wartość (zamiast 3 osobnych czynności).
10. **Szukanie** na górze szyny: cała treść drzewa (też ukryta w prostym i pod Więcej) + nazwy wierszy w środku ekranów; bez polskich znaków; wynik → właściwa kategoria, rozwinięta grupa, fokus na pozycji.

## Drzewo (N = tryb prosty, W = pod „Więcej”)
**Oglądanie** — Włączanie filmu: Polski dźwięk czy jakość (N), Wybór źródła, Gdy film się zatnie, Szybszy start filmu, Dopasowanie do internetu (W), Torrenty (W) · Seriale: Następny odcinek (N), Pomijanie czołówek (N) · Obraz i odtwarzacz: Obraz, Dolby Vision i dźwięk przestrzenny, Wygląd odtwarzacza, Przewijanie, Odtwarzacz (W), Pamięć na film (W).

**Napisy i dźwięk** — Napisy: Rozmiar napisów (N), Język i wygląd napisów (N), Najpierw polskie napisy z filmu, Napisy równo z mową, Czcionka napisów · Napisy AI: tłumaczenie (N), gdy brak dobrych polskich, powiadomienie w rogu, recenzja (W), oryginał gdzie brak (W) · Dźwięk i lektor: Język i dźwięk (N), Lektor (N), lektor sam, głośność lektora, ściszanie filmu, ściszaj aktorów, nagrania na Dysku (W), małe pliki (W), wyczyść nagrania (W), Rozpoznawanie mowy (W).

**Telewizja** — Kanały i program: Listy kanałów i program (N), Szukaj w telewizji, Najpierw / Tylko kanały z mojego kraju, Kraj kanałów, Bez filmów i seriali, Same nazwy kanałów, Logo kanałów, Własne logo i kanały bez programu (W), przewodnik uzupełniający (W) · Oglądanie telewizji: przypomnienia, archiwum bez pytania, rząd „Dla Ciebie”, Podgląd kanałów, obserwowane programy, surowe liczenie (W), wyczyść historię (W) · Filmy i seriale z list IPTV: Katalogi IPTV, TVDB (W) · Pamięć telewizji (cała W): tylko na ekranie TV, przewodnik w bazie, szybki start przewodnika, podgląd po minucie, karta przypomnień i układ (skórka Kicia).

**Wygląd** — Skórka i język: Skórka (N), Kolory, czcionka i język (N), Menu i styl (W) · Ekran główny: Układ ekranu głównego, Odkrywaj telewizję, Na co masz ochotę?, YouTube na ekranie głównym, tła w pełnej jakości, gotowe rozmycia (W), rzędy Odkrywaj (W), Kalendarz w menu (W) · Tryb nocny: Tryb nocny (N), początek, koniec, siła, ciepła nakładka, przyciemnienie, nie przy HDR (W), dźwięk nocą i filtr (W).

**Konta i dodatki** — Ty i domownicy: Profile (N), Konto Nuvio (N), Rodzina, Wspólne oglądanie · Skąd są filmy: Dodatki (N), Usługi i dane filmów, Serwery mediów i wtyczki, YouTube, YouTube w aplikacji (W) · Obejrzane i kopia: Trakt i Simkl, Kopia ustawień, statystyki używania (W).

**Pilot i głos** — Pilot: Przyciski pilota, Przyciski pilota w całej aplikacji, Blokada wyjścia · Głos: Co mogę powiedzieć (N), przycisk „Powiedz”, usługa mikrofonu, naucz przycisk, Nuvio jako asystent, przycisk asystenta, Whisper (W).

**System** — Start z telewizorem: Uruchamiaj z telewizorem (N), Po uruchomieniu pokaż (N), Nuvio jako ekran główny, zgoda na start (W) · Pomoc: Pomoc i dziennik błędów (N), Test łącza, test sam, Test telewizora i łącza (W), O aplikacji i aktualizacje · Ustawienia: Tryb ustawień (N), Pamięć i wydajność (W), Wszystkie moduły (W), Nowy układ ustawień (W), dawne menu/szukanie (W), Diagnostyka (W, wersja testowa).

## Jak dodać ustawienie
Moduł forka: `ModuleSetting(..., place = "kategoria/grupa")` (np. `"tv/kanaly"`) albo slot `m("klucz")` w `SettingsTreeSpec.layout`. Bez miejsca trafia do System › Inne (pod Więcej) i test `każda pozycja modułu ma miejsce` nie przechodzi. Po zmianie nazw wierszy w ekranach: `python3 -I fork-tools/ustawienia/inwentarz.py --kotlin` (słowa do szukania).
