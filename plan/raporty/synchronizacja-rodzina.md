# Synchronizacja ustawień dla rodziny — wariant A na serwerze Zenbox (projekt, 18.11.2026)

## Cel
Członek rodziny wpisuje na swoim telewizorze 6-cyfrowy kod i dostaje WSZYSTKIE ustawienia właściciela (dodatki, źródła Live TV, klucze API, wygląd, ustawienia odtwarzania). Potem zmiany same przychodzą. Nikt spoza rodziny, także ktoś, kto zobaczy serwer, nie odczyta ustawień.

## Jak to wygląda dla ludzi
- **Właściciel**: Ustawienia → Rodzina → „Dodaj urządzenie”. Telewizor pokazuje kod, np. `482 913`, ważny 10 minut, jednorazowy.
- **Rodzina**: przy pierwszym uruchomieniu (kreator) albo w Ustawienia → Rodzina → „Dołącz do rodziny”: 6 cyfr pilotem (duże przyciski cyfr), OK. Po kilku sekundach aplikacja ma ustawienia właściciela.
- **Potem**: każdy telewizor sprawdza zmiany przy starcie i co kilka godzin; zmiana ustawień u właściciela wysyła się sama po chwili.
- **Lista urządzeń** u właściciela: nazwa, ostatnio widziane, „Usuń” (odcina urządzenie od razu).
- Opcja „Tylko właściciel zmienia ustawienia” (domyślnie włączona): urządzenia rodziny tylko pobierają, nie nadpisują.

## Bezpieczeństwo (dlaczego 6 cyfr wystarczy)
- Ustawienia na serwerze są **zaszyfrowane kluczem rodzinnym** (256 bitów, AES-256-GCM). Klucz powstaje na telewizorze właściciela i nigdy nie trafia na serwer w jawnej postaci. Serwer przechowuje tylko nieczytelny blok.
- 6-cyfrowy kod służy **tylko do jednorazowego przekazania klucza** nowemu urządzeniu:
  1. telewizor właściciela szyfruje klucz rodzinny kodem i wysyła na serwer na 10 minut;
  2. serwer pozwala na **5 prób** wpisania kodu, potem kod przepada (zgadnięcie: 5 na milion = 0,0005%), dodatkowo limit prób z jednego adresu IP;
  3. po udanym użyciu kod i zaszyfrowany klucz są od razu kasowane.
- Każde urządzenie dostaje własny **token urządzenia** (serwer trzyma tylko jego skrót). Bez tokenu serwer nic nie wydaje.
- Usunięcie urządzenia: kasuje token + zmienia klucz rodzinny (pozostałe urządzenia dostają nowy klucz zaszyfrowany starym).
- Klucz na telewizorze: w magazynie kluczy Androida (Keystore), wyłączony z kopii zapasowej Androida.
- Połączenie tylko HTTPS (certyfikat Let's Encrypt z panelu Zenbox).
- Słabe miejsce: przez 10 minut ważności kodu właściciel serwera (Ty) mógłby teoretycznie złamać kod offline. Serwer jest Twój, więc to akceptowalne. Gdyby nie był, potrzebny jest protokół PAKE (bardziej złożony).

## Serwer Zenbox (PHP + MySQL, które Zenbox ma w każdym pakiecie)
- Jeden plik `api.php` (ok. 200 linii) + `config.php` (dane bazy wpisujesz Ty w panelu, nie ja).
- Trzy tabele MySQL:
  - `families` — id rodziny, zaszyfrowany blok ustawień, numer wersji, data;
  - `devices` — skrót tokenu, rodzina, nazwa urządzenia, czy właściciel, ostatnio widziane;
  - `pair_codes` — skrót kodu, rodzina, zaszyfrowany klucz, wygasa, liczba prób.
- Operacje: `pair/start` (właściciel), `pair/redeem` (nowe urządzenie), `blob/get`, `blob/put` (z numerem wersji, żeby dwa zapisy się nie nadpisały), `devices/list`, `devices/remove`, `key/rotate`.
- Limit rozmiaru bloku (np. 5 MB), limit zapytań z IP.

## Co jest synchronizowane
- Wszystko, co dziś zbiera synchronizacja Reshaped (ustawienia, Live TV: źródła, ulubione, grupy, listy) + przełączniki forka (`fork_features`) + dodatki + klucze API (TMDB, MDBList, debrid itp.) + skórka i odpowiedź „po polsku / jakość”.
- **Nie** synchronizowane (zależą od urządzenia): wynik testu dekoderów i ekranu, dopasowanie klatek, pobrane modele mowy, pamięć złych źródeł, historia oglądania (każdy ma swoją).
- Do decyzji: sesje logowania (Trakt, konto Nuvio) — to konta osobiste, proponuję NIE synchronizować.

## W aplikacji (moduł forka `fork/family`, cienkie wpięcia)
- Zbieranie i zapis ustawień: istniejące `reshaped/sync` (SyncDoc, SyncSections) + dopisane sekcje forka; transport zamiast Google Drive → serwer Zenbox.
- Ekran „Rodzina” w ustawieniach forka + krok w kreatorze pierwszego uruchomienia („Masz kod od rodziny?”).
- Szacunek: aplikacja ok. 600–900 linii, serwer ok. 200 linii.

## Co będzie potrzebne od Ciebie
1. W panelu Zenbox: subdomena (np. `sync.twojadomena.pl`) z włączonym SSL i pusta baza MySQL (nazwa, użytkownik, hasło — zostają u Ciebie).
2. Wgranie dwóch plików, które przygotuję (menedżer plików w panelu albo FTP), i wpisanie danych bazy do `config.php`.
3. Adres subdomeny (sam adres, bez haseł) — wpiszę go w aplikacji.

## Alternatywa do rozważenia
Zamiast kodu dla każdego urządzenia: jeden stały kod rodzinny (np. 8 znaków) do wpisywania bez udziału właściciela. Wygodniejsze, ale kod może wyciec, a wtedy trzeba go zmienić u wszystkich. Polecam kody jednorazowe.
