# Prompt: panel „Nuvio Rodzina” na serwerze Zenbox

Wklej wszystko poniżej linii do modelu, który ma dostęp do repozytorium `NuvioFork` (gałąź `hybryda-zrodel`) i do panelu Zenbox.

---

## Zadanie

Zbuduj i wdroż na hostingu Zenbox aplikację webową **„Nuvio Rodzina”**: panel, w którym właściciel konfiguruje wszystkie ustawienia aplikacji Nuvio TV (fork) na telewizorach rodziny i znajomych, widzi statystyki i problemy oraz zdalnie zmienia konfigurację, która sama synchronizuje się z telewizorami. Pracuj po polsku; interfejs w całości po polsku.

## Co już istnieje (przeczytaj najpierw)

- `NuvioFork/server/family-sync/api.php` — działający serwer synchronizacji (PHP 8, SQLite w `data/`, `.htaccess` blokuje wszystko poza `api.php`). Wdrożony: `https://nuviosync.caring.design/api.php` (katalog na serwerze: `domains/caring.design/public_html/nuviosync/`).
- `NuvioFork/app/src/main/java/com/nuvio/tv/fork/family/` — klient w aplikacji:
  - `FamilyCrypto.kt`: AES-256-GCM (IV 12 B + szyfrogram z tagiem, Base64 NO_WRAP), PBKDF2-HMAC-SHA256 200 000 rund dla kodu 6 cyfr, skrót kodu `sha256("nuvio-pair:v1:" + kod)`;
  - `FamilySnapshot.kt`: zrzut ustawień = ZIP plików `files/datastore/*.preferences_pb` (protobuf Jetpack DataStore Preferences), `shared_prefs/*.xml` (Android SharedPreferences XML) i `files/live_tv/*` (playlisty); pliki wykluczone są tam wypisane;
  - `FamilySync.kt`: role OWNER/MEMBER, wersje, `put/get/pair/redeem/delete`.
- Projekt i bezpieczeństwo: `NuvioFork/plan/raporty/synchronizacja-rodzina.md`.

**Zasada nadrzędna: serwer nigdy nie widzi ustawień w jawnej postaci** (są w nich klucze API, źródła, sesja konta Nuvio). Panel nie może tego zepsuć.

## Architektura, którą masz zrealizować

1. **Odszyfrowanie tylko w przeglądarce.** Panel pobiera zaszyfrowany blok z `api.php`, odszyfrowuje go Web Crypto API (AES-GCM) kluczem rodziny, pokazuje i edytuje ustawienia, szyfruje z powrotem i wysyła nową wersję (`a=put` z sekretem właściciela). PHP przechowuje i przekazuje tylko szyfrogram.
2. **Połączenie panelu z rodziną bez przepisywania kluczy:** na telewizorze właściciela dodaj w ekranie „Rodzina” przycisk „Połącz panel w przeglądarce”. Pokazuje on kod 6 cyfr (ten sam mechanizm co `pair`/`redeem`, osobny typ wpisu), który panel wymienia na `{id, key, owner}`. Klucz trzymaj w przeglądarce w IndexedDB jako nieeksportowalny `CryptoKey` (sekret właściciela zaszyfrowany tym kluczem). Dodaj „Odłącz tę przeglądarkę”.
3. **Logowanie administratora:** jedno konto, na stałe `brzostkowski@gmail.com` (wpisane w kodzie, bez rejestracji i bez zmiany adresu). Logowanie linkiem e-mail (jednorazowy link ważny 15 min, wysyłany przez `mail()`/SMTP Zenbox), sesja w ciasteczku `HttpOnly; Secure; SameSite=Strict`, limit prób. Żadnych haseł w kodzie ani w bazie. Logowanie chroni panel i statystyki; do ustawień potrzebny jest jeszcze klucz z punktu 2.
4. **Edycja ustawień:** w przeglądarce rozpakuj ZIP (np. fflate z cdnjs), czytaj i zapisuj:
   - `*.preferences_pb`: format `androidx.datastore.preferences.PreferencesProto` (mapa nazwa → wartość: bool, float, int, long, string, string_set, double, bytes) — napisz mały koder/dekoder protobuf bez zależności;
   - `shared_prefs/*.xml`: `<map>` z `<string>`, `<boolean>`, `<int>`, `<long>`, `<float>`, `<set>`;
   - zachowaj nieznane klucze bez zmian (round-trip bajt w bajt dla nieruszanych plików).
   Pokaż ustawienia po ludzku: grupy (Konto i profile, Dodatki, Źródła i Live TV, Odtwarzanie, Napisy i lektor, Wygląd, Moduły forka `fork_features`, Klucze API), nazwy po polsku, przełączniki/pola zamiast surowych kluczy; surowy widok tylko w trybie „Zaawansowane”. Klucze API maskowane, odsłaniane przyciskiem.
5. **Zdalna konfiguracja, która się synchronizuje:** zapis w panelu = nowa wersja bloku. W aplikacji dodaj pobieranie co 15 minut, gdy aplikacja jest na ekranie, a nie tylko przy starcie. Ustawienia, które działają bez restartu, wgrywaj na żywo; resztę przy następnym starcie, z małym komunikatem „Nowe ustawienia od rodziny — uruchom ponownie”. Konflikt: właściciel na TV i panel zmienili to samo → wygrywa nowszy zapis; pokaż w panelu „Zmienione na telewizorze o 18:42”.
6. **Statystyki i problemy:** aplikacja wysyła (osobny endpoint `a=report`, tylko dla urządzeń w rodzinie) lekkie, nieszyfrowane, ale anonimowe dane: model urządzenia, Android, wersja aplikacji, ostatnio widziane, liczba startów odtwarzania, czasy startu (log `ForkPerf`), błędy odtwarzania i awarie (kod błędu, bez adresów strumieni, bez tytułów, jeśli właściciel tego nie włączy), odłożone złe źródła (`ForkStartupChecks`), wynik testu dekoderów. W panelu: lista telewizorów (nazwa nadawana w panelu), zdrowie każdego, wykres czasów startu, lista problemów z podpowiedzią, co zrobić („Źródło X nie startuje na 3 telewizorach — wyłącz dodatek?”). Retencja 30 dni, kasowanie przyciskiem.
7. **Zarządzanie urządzeniami:** nazwij, usuń z rodziny (zmiana klucza rodziny i ponowne zaszyfrowanie bloku w przeglądarce), kod dla nowego telewizora generowany też z panelu.

## Ograniczenia hostingu

- Zenbox: PHP 8 + SQLite (PDO), bez Node na serwerze; build frontendu lokalnie, na serwer tylko pliki statyczne + PHP. Wdrożenie przez menedżer plików DirectAdmin (`https://panel-s7.zenbox.pl/CMD_FILE_MANAGER/...`) w przeglądarce; nie wpisuj żadnych haseł (logowanie robi właściciel).
- Panel pod `https://nuviosync.caring.design/panel/`; `.htaccess` w katalogu głównym trzeba rozszerzyć tak, by wpuszczał `panel/` (i dalej blokował `data/`). Domena jest za Cloudflare, ale rekord `nuviosync` jest bez proxy — nie włączaj proxy (odnawianie Let's Encrypt).
- Nagłówki: CSP bez `unsafe-inline` dla skryptów, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`.

## Projekt interfejsu (użyj umiejętności frontend-design)

Odbiorca: właściciel, osoba nietechniczna, konfiguruje najczęściej z telefonu, czasem z laptopa; rodzina panelu nie widzi. Główne zadanie: „zmień coś na telewizorze mamy bez chodzenia do mamy” i „zobacz, co się psuje”.

Kierunek (do rozwinięcia w planie tokenów zgodnie z umiejętnością):
- Język wizualny z salonu i pilota, nie z panelu SaaS: ekran główny to rząd „telewizorów” rodziny (nazwa, pokój, stan: działa / ma problem / dawno offline). Wejście w telewizor pokazuje jego ustawienia i zdrowie. To jedyny mocny, zapamiętywalny element. Reszta ma być cicha.
- Paleta: ciepły, przyciemniony wieczór w salonie zamiast bieli biura, z jednym kolorem „sygnału” dla stanu (działa / problem). Unikaj kremowego tła z terakotą, czerni z kwasową zielenią, kart z jednakowym cieniem i gradientów jako dekoracji. Pełny tryb jasny i ciemny.
- Typografia: jedna rodzina z charakterem (polskie znaki, czytelne cyfry tabelaryczne do statystyk), skala według The Elements of Typographic Style; bez etykiet wersalikami nad każdym nagłówkiem.
- Teksty: proste słowa („Klucze do serwisów”, nie „API credentials”), przyciski mówią, co się stanie („Wyślij na telewizory”, potem komunikat „Wysłano na 3 telewizory”), błędy mówią, co zrobić.
- Jakość: od 360 px szerokości, cele dotyku ≥ 44 px, widoczny fokus z klawiatury, `prefers-reduced-motion`, kontrast WCAG AA, bez przewijania w poziomie, szybki pierwszy ekran (< 150 KB JS).
- Przed kodem: plan tokenów (kolory hex, kroje, siatka, szkice ASCII ekranu głównego, telewizora i ustawień), przegląd planu pod kątem banału, dopiero potem budowa. Na końcu zrzuty ekranu telefon/laptop i poprawki.

## Kolejność pracy i odbiór

1. Endpointy PHP (`report`, logowanie linkiem, sparowanie przeglądarki, nazwy urządzeń) + testy `curl`.
2. Dekoder/koder ZIP + protobuf DataStore + XML z testem round-trip na prawdziwym zrzucie (pobierz z telewizora testowego w rodzinie).
3. Frontend według planu projektu.
4. Zmiany w aplikacji (Kotlin, moduł `fork/family`, cienkie wpięcia, `bash fork-tools/check.sh`): sparowanie przeglądarki, pobieranie co 15 min, `report`. Build: tylko `-PforkAbi=armeabi-v7a` (JMGO) albo `arm64-v8a` (emulator), w zakładce terminala z `--console=rich`.
5. Wdrożenie na Zenbox, test pełnego obiegu: zmiana w panelu → telewizor rodziny ją dostaje; problem na telewizorze → widać go w panelu.
6. Dokumentacja dla właściciela (prosta, po polsku): jak zalogować się, połączyć przeglądarkę, zmienić ustawienie, odczytać problem.

Pytaj właściciela tylko o rzeczy z jego konta (logowanie do Zenbox, potwierdzenie e-maila); resztę rób samodzielnie.
