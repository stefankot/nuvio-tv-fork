# Przegląd ekranów z właścicielem — 27.11.2026 (JMGO, fork com.nuviodebug.com)

Notatka ekran po ekranie: co nie działa / co poprawić. Zrzuty w tym katalogu (`NN-nazwa.png`).
Uwagi właściciela zapisane jego słowami (skrót), pod spodem moja obserwacja ze zrzutu/kodu i propozycja poprawki.

## 01 — Karta tytułu (przeglądarka tytułów z rzędu Głównej) — ✅ TV prz04 (jeden przycisk „O filmie”, ▶ = następny tytuł, OK = Szczegóły)
Zrzut: `01-karta-tytulu.png` — „Ruff and Tuff Kanya”, duża karta z obrazem, po bokach brzegi sąsiednich kart, przyciski Odtwórz / + / (szczegóły). Kod: `app/src/main/java/com/nuvio/tv/skin/niunia/home/NiuniaTitlePager.kt`.
1. **Szary gradient zasłania lewą część obrazu.**
   Właściciel: „plakat zasłonięty szarym gradientem po lewej stronie”.
   Obserwacja: na JMGO (Android 11) włącza się wariant „flat” przyciemnienia — pełny szary kolor tła do 45% szerokości (alfa 0.92), zanik dopiero do 70% (`NiuniaTitlePager.kt:135-137`). Na zrzucie lewa połowa karty to jednolita szara plama.
   Poprawka: przyciemnienie tylko pod tekstem — węższe i lżejsze (np. ~0.6 alfa do 25%, zanik do ~50%), bez osobnego „flat” dla Androida 11; obraz ma być widoczny w całości.
2. **Strzałki ←/→ mają od razu przełączać film, a nie chodzić po przyciskach.**
   Właściciel: „strzałkami w prawo i w lewo przechodzić od razu do poprzedniego/następnego filmu, a nie chodzić po przyciskach”.
   Obserwacja: teraz ← przełącza tylko z „Odtwórz”, → tylko z ostatniego przycisku; pomiędzy strzałki chodzą po Odtwórz / + / (szczegóły) (`NiuniaTitlePager.kt:157-163`).
   Poprawka: karta = jeden element z fokusem; ←/→ zawsze poprzedni/następny tytuł. Przyciski „Odtwórz” i „+” nie łapią fokusu na tym ekranie (są w Szczegółach).
3. **OK na filmie ma otwierać Szczegóły, nie odtwarzanie.**
   Właściciel: „po naciśnięciu na film powinno się wchodzić do szczegółów filmu, a nie od razu do odtwarzania”.
   Obserwacja: domyślny fokus stoi na „Odtwórz”, więc OK od razu włącza film (`NiuniaTitlePager.kt:157`, `onPlay`).
   Poprawka: OK → Szczegóły filmu (`onDetails`); odtwarzanie dopiero ze Szczegółów.

## 02 — Szczegóły filmu — ✅ TV prz06 pkt 1 (nagrody w lewej kolumnie: „7 nagród + 3 nominacje · Gale Oscar 3+1 · Kategorie…”, „The Brutalist”)
Zrzut: `02-szczegoly.png` — „Ruff and Tuff Kanya” (tt49112036): plakietka „Obejrzyj z lektorem”, tytuł, opis, ★ 9.9, przyciski Odtwórz / + / (opis) / (zwiastun); po prawej na dole Obsada / Reżyseria / wytwórnie; niżej rząd zwiastunów. Kod: `app/src/main/java/com/nuvio/tv/skin/niunia/detail/NiuniaDetail.kt`.
1. **Zamiast aktorów mają być nagrody (zaprojektowane, czekają na wdrożenie).**
   Właściciel: „po lewej stronie miały być nagrody, a nie aktorzy”.
   Obserwacja: nagrody są w kodzie i włączone (źródło Wikidata, `niunia_awards_wikidata`). Pokazują się tylko wtedy, gdy film ma nagrody — wtedy zastępują blok Obsada (`NiuniaDetail.kt:716`, ten sam róg ekranu, po PRAWEJ). Ten film: Wikidata zwróciła 0 nagród (log `NAwards: tt49112036 (movie): 0 pozycji`), więc wraca Obsada. Inny film dziś (tt8999762) pokazał „7 nagród + 3 nominacje”.
   Poprawka: ustawić nagrody tam, gdzie są w projekcie — po LEWEJ (nie w miejscu obsady po prawej); sprawdzić z projektem, co ma być, gdy film nie ma nagród (ukryć miejsce czy pokazać obsadę).
2. **Na dole strony ma być dłuższa forma nagród.**
   Właściciel: „na tej stronie miały być też nagrody w formie dłuższej na dole”.
   Obserwacja: sekcja „Nagrody i nominacje” jest na samym końcu strony (`NiuniaDetail.kt:411-417`, `NiuniaAwardsSummarySection` — kafelki gal i kategorii zamiast pełnej listy). Dla tego filmu log: „summary: brak nagród, stan pusty”.
   Poprawka: dłuższa forma wg projektu (lista nagród i nominacji, nie tylko kafelki statystyk); do sprawdzenia na filmie z nagrodami, czy w ogóle widać ją na TV (pilot musi dojść na dół strony).
3. **„Rzeczy nagle znikają”.**
   Właściciel: zaprojektowane rzeczy „nagle zniknęły nie wiadomo dlaczego”.
   Poprawka (zadanie osobne): spis „projekt vs to, co jest na TV” dla każdego ekranu Niuni — co zaprojektowane, co wdrożone, co wyłączone przełącznikiem / ukryte, gdy brak danych. Uzupełniać listą rzeczy, które właściciel wskaże w tym przeglądzie.

## 03 — Odtwarzacz: źle wybrane źródło (serial „Ciało”, #1 w „Popular · seriale”) — ✅ TV prz04 (log ForkDurationGuard: verdict=OK dla filmu; klip < 3 min nie trafił się)
Zrzut: `03-zrodlo.png` — zamiast odcinka gra 12-sekundowy klip dodatku TorrentClaw: „Too many requests / Wait a minute and try again”.
1. **Źle wybrane źródło — ma działać kontrola długości: plik różny o więcej niż ±10% od długości filmu → inne źródło.**
   Właściciel: „musi być mechanizm sprawdzania długości filmu — taki już istniał; gdy > ±10% długości filmu, to inne źródło”.
   Obserwacja: log `ForkDiag: stream title=«Ciało» … addon=TorrentClaw (PL, 720p)`, `PlayerViewModel: … durationMs=12000` — plik ma 12 s. Mechanizm istnieje (`fork/sources/StartupChecks.kt`: film ±10%, odcinek 2×; klip < 3 min → `Placeholders`), ale NIKT go nie wywołuje: 22.11 odłączony razem z autoplayem forka (commit d14b50035, `plan/DECYZJE.md:41` „Automatyczny wybór źródła TYLKO natywny … Odłączone: … kontrola pliku Kicii”). To jest jedna z rzeczy, które „zniknęły”.
   Poprawka: podłączyć kontrolę długości z powrotem do natywnego autoplayu (Regex Nuvio): po wczytaniu pliku porównać jego długość z długością z opisu tytułu; film: różnica > ±10% → od razu następne źródło z listy (zły plik odłożyć, żeby nie wracał); klip krótszy niż 3 min (komunikaty „Too many requests”, „not cached”) → zawsze następne źródło. Dla odcinków ustalić tolerancję (właściciel podał ±10% dla filmu; stara reguła dla odcinków była 2×). Zaktualizować `plan/DECYZJE.md:41`.

## 04 — Odtwarzacz: panel „Audio, lektor i napisy”, zakładka Napisy — ✅ TV prz04 (polskie na górze, „Inne języki 45”, ▼ do „Wybierz napisy”, ▲ do zakładek)
Zrzut: `04-lista.png` — „Ciało” S1:01 (Flix-Streams, 4K HEVC HDR, polski dźwięk dołączony z innego wydania); lista: Napisy AI · polski, potem 55 napisów wbudowanych (Brazilian ×4, ar, bg, cs, CC…), dalej pliki z dodatków (70), „Wyłączone”. Kod: `app/src/main/java/com/nuvio/tv/skin/niunia/player/NiuniaPlayerChrome.kt:453-530` (`NiuniaSubtitlesTab`).
1. **Nie da się zejść na dół listy, a potem panel przestaje reagować.**
   Właściciel: „nie da się przejść na dół listy, a w tym momencie ekran w ogóle przestał reagować”.
   Obserwacja: aplikacja się nie zawiesiła — odtwarzanie idzie dalej (log co 10 s), klawisze docierają (`ForkFocus: key=DPAD_DOWN…` o 17:59:41-46). Fokus zgubił się: po tych klawiszach zaznaczony jest cały ekran (`[0,0][1920,1080]`), żaden wiersz listy — więc ▲▼ nic nie robią. Lista ma ~63 wiersze (55 wbudowanych + 6 z dodatków + AI + „Wyłączone” + „Więcej”), rysowane wszystkie naraz (`forEach`, wiersze 506-530).
   Poprawka: przewijana lista, która trzyma fokus do ostatniego wiersza (▼ na ostatnim = zostaje na nim; nigdy fokus poza panel); po zgubieniu fokusu wraca na wiersz z ✓. Sprawdzić na JMGO: ▼ do samego końca i ▲ z powrotem na górę.
2. **Polskie napisy (język preferowany) mają być na początku listy, nie na końcu.**
   Właściciel: „polskie napisy — język preferowany powinien być na początku listy, a nie na końcu”.
   Obserwacja: napisy wbudowane są w kolejności z pliku (pt, ar, bg, cs…; polskie gdzieś w środku/na dole 55 pozycji); pliki z dodatków są wprawdzie „najpierw polskie”, ale stoją dopiero PO wszystkich wbudowanych.
   Poprawka: na górze (pod Napisami AI) wszystkie napisy w języku preferowanym — wbudowane i z dodatków razem; potem pozostałe języki. **Decyzja właściciela (27.11): pozostałe języki zwinięte w jeden wiersz „Inne języki”** (OK rozwija listę pozostałych języków) — lista ma być krótka. Dotyczy napisów wbudowanych i z dodatków.

## 05 — Odtwarzacz: panel „Quality” (jakość) — ✅ TV prz04 pkt 1–2 (panel Niuni po polsku, długie OK = źródła 1080p); poprawka prz05: bieżąca jakość i ✓; pkt 3 niesprawdzony
Zrzut: `05-jakosc.png` — 4K ✓, 1080p (56 sources), 720p (13), 480p (7, „Angielski, Hindi +4”), 360p (1).
Kod: okno z forka Echo (teksty `app/src/main/res/values/echo_player_strings.xml:8` `quality_menu_subtitle`; przełączanie `ui/screens/player/PlayerRuntimeControllerSourceSwitch.kt:270-310`).
1. **Okno jakości ma wyglądać jak cała skórka (Niunia).**
   Właściciel: „dialog z listą jakości ma być ostylowany jak cała skórka”.
   Obserwacja: to okno Echo — inne kolory, kształty i czcionki niż panele Niuni (np. „Audio, lektor i napisy”); do tego cały po angielsku („Quality”, „Switches to a source with that resolution and keeps your place.”, „sources”).
   Poprawka: panel jakości w stylu Niuni (jak zakładki panelu „Audio, lektor i napisy”: nagłówek, wiersze, ✓), teksty po polsku („Jakość”, „Przełącza na źródło w tej rozdzielczości i gra od tego samego miejsca”, „56 źródeł”).
2. **Długie OK na jakości → lista źródeł w tej jakości.**
   Właściciel: „po długim naciśnięciu jakości powinna otwierać się lista źródeł”.
   Obserwacja: teraz OK = automatyczne przełączenie; długie OK nic osobnego nie robi.
   Poprawka: długie OK na wierszu (np. 1080p) → lista źródeł tej rozdzielczości (dodatek, nazwa, rozmiar, język dźwięku), OK na źródle = przełącz na nie; pod nagłówkiem podpowiedź „Przytrzymaj OK, aby wybrać źródło”.
3. **Po wybraniu rozdzielczości aplikacja kilka razy zmieniała źródło.**
   Właściciel: „po wybraniu rozdzielczości aplikacja kilka razy wybierała źródło”.
   Obserwacja (log 18:03:07-18:04:10): wybór 1080p → 6 prób w ~63 s: Flix-Streams („Easy Vault”) → TorrentClaw → PenguPlay → Flix-Streams → Flix-Streams (HTTP 416) → Flix-Streams „Usenet Vault 1080p” (gra). Próby 1 i 2 już się wczytały (plik otwarty, polski dźwięk wybrany), a mimo to po 16 s „no playback in time” i skok dalej. Ostatnie źródło nie ma polskiego dźwięku („new source has no pl track”). Diagnostyka podaje `quality=4K` dla pliku 1080p.
   Poprawka: dłużej czekać na źródło, które się wczytuje (nie przerywać, gdy dane płyną); w trakcie jeden komunikat „Przełączam na 1080p…”, a nie widoczne skoki; najpierw źródła z polskim dźwiękiem, gdy obecny ma polski; po nieudanych próbach wrócić do poprzedniego źródła w tym samym miejscu i powiedzieć to na ekranie.

## 06 — Odtwarzacz: panel „Dźwięk” — ✅ TV prz04 (prosta lista ścieżek, „Polski z innego wydania”, reszta zwinięta); ❌ fokus znikał po dołączeniu ścieżki → poprawka prz05; prz06: niesprawdzone — dołączenie polskiej ścieżki nie powiodło się 2× (brak synchronizacji dawców), raz dołączyła przy schowanym panelu
Zrzut: `06-dzwiek.png` — lewa kolumna: „Original · Angielski (In this file)”, „Polski ✓”; prawa: opóźnienie dźwięku, wzmocnienie, downmix, tryb nocny, „Zachowaj między sesjami”.
Kod: okno z forka Echo (teksty `app/src/main/res/values/echo_player_strings.xml:25` `audio_menu_hint` i sąsiednie).
1. **Ścieżkę dźwiękową wybiera się z prostej listy, reszta ustawień zwinięta.**
   Właściciel: „powinno dać się wybrać ścieżkę dźwiękową z prostej listy, a reszta ustawień powinna być zwinięta”.
   Obserwacja: obok listy języków od razu stoją wszystkie ustawienia (opóźnienie z −/+, wzmocnienie, downmix, tryb nocny, „Zachowaj między sesjami”); prawa kolumna leży wprost na obrazie filmu, słabo czytelna (na zrzucie widać przez nią obraz), „In this file” nachodzi na listę.
   Poprawka: panel „Dźwięk” = prosta lista ścieżek (język + krótki opis, ✓ przy bieżącej; OK = wybierz); na końcu jeden wiersz „Więcej ustawień dźwięku” — dopiero OK rozwija opóźnienie, wzmocnienie, tryb nocny itd. Styl Niuni, polskie teksty, bez słów technicznych.
- Moja obserwacja (nie zgłoszona): to też nie styl Niuni, mieszanka języków („Hold OK on a language to pick its source”, „In this file”); techniczne słowa dla użytkownika („PCM”, „Downmix”, „FFmpeg”).

## 07 — Szczegóły serialu (niżej na stronie): Zwiastuny i „Więcej podobnych” — ✅ TV prz04 (zwiastuny = karty poziome Głównej, bez podpisów)
Zrzut: `07-zwiastuny.png` — „Ciało”: logo, rząd „Zwiastuny” (4 karty z podpisami pod spodem: „Zwiastun 1”, „Sneak Peek: So You Wanna …”, „The Lions Break Character | …”, „An Inside Look at New Teen …”), pod nim „Więcej podobnych” (plakaty pionowe). Kod: `skin/niunia/detail/NiuniaDetail.kt:281` (rząd zwiastunów), karta `skin/niunia/components/NiuniaCards.kt:41-74` (preset `TrailerCard`), rozmiary `components/NiuniaModels.kt:42-50`, `tokens/NiuniaDesignTokens.kt:206-208`.
1. **Niepotrzebne opisy pod zwiastunami.**
   Właściciel: „niepotrzebne są opisy do trailerów”.
   Obserwacja: pod każdą kartą tytuł z YouTube (ucięty „…”), na fokusie jako plakietka („Zwiastun 1”) — po angielsku i bez wartości (sam obraz zwiastuna mówi, co to jest).
   Poprawka: karta zwiastuna bez podpisu pod obrazem — sam kadr z ▶.
2. **Karty poziome mają mieć rozmiar i proporcje jak na Głównej (komponent MediaCard).**
   Właściciel: „wielkość plakatów poziomych powinna być taka jak na stronie głównej (inne proporcje komponentu MediaCard)”.
   Obserwacja: zwiastuny używają presetu `TrailerCard` = rozmiar „Still” (~205×115, 16:9), a karty poziome Głównej („Oglądaj dalej”, `ContinueWatchingCard`/`LandscapeCard`) mają `LandscapeW×LandscapeH` = 160×114. Dwa różne rozmiary kart poziomych w jednej skórce.
   Poprawka: zwiastuny (i inne karty poziome na stronie tytułu) na tym samym presecie co karty poziome Głównej — jeden rozmiar i proporcje kart poziomych w całej skórce.

## 08 — Szczegóły serialu (góra): przycisk „O serialu” (ikona kartki) — ✅ TV prz04 („O filmie” przewija do sekcji informacji)
Zrzut: `08-przycisk.png` — „Ciało”: przyciski Wznów (39m) / ✓ / **(ikona kartki — zaznaczona)** / (zwiastun); po prawej Obsada i Twórcy. Kod: `skin/niunia/detail/NiuniaDetail.kt:180-186` (`focusAbout`), przycisk `:708`.
1. **Zaznaczony przycisk nic nie robi i nie wiadomo, do czego służy.**
   Właściciel: „przycisk teraz zaznaczony nic nie robi i nie wiem, co miałby robić”.
   Obserwacja: to „O serialu” — ma przenieść niżej na stronę do części „Obsada i opis” (pełny opis, informacje, języki, obsada). Log: OK naciśnięte 3 razy (18:08:51, :52, :56) → „szczegóły: O serialu → Obsada i opis”, ale fokus się nie przenosi (brak zmiany fokusu w `ForkFocus`, brak błędu w logu) — cel leży w liście niżej, jeszcze nienarysowany, więc prośba o fokus przepada po cichu. Przycisk ma tylko ikonę, bez podpisu.
   Poprawka: (a) OK najpierw przewija stronę do „Obsada i opis”, potem stawia tam fokus (a gdy się nie uda — log + komunikat na ekranie, nie cisza); (b) przy zaznaczeniu pokazać podpis przycisku („O serialu” / „O filmie”), tak jak „Odtwórz” ma tekst. Do decyzji właściciela: czy ten przycisk w ogóle potrzebny (to samo daje ▼ na stronie).

## Wszystkie ekrany: napis „Gotowe” w lewym dolnym rogu — ✅ TV prz04 (na żadnym ekranie ani zrzucie)
Zrzuty: `03-zrodlo.png`, `04-lista.png`, `05-jakosc.png`, `06-dzwiek.png`, `07-zwiastuny.png` (na plakacie), `08-przycisk.png`; na `09-gotowe.png` (Główna, 18:10) już go nie ma.
1. **Ciągle widoczny napis „Gotowe” — nie wiadomo, co to i po co.**
   Właściciel: „ciągle na ekranie widnieje napis »Gotowe« w rogu — nie wiem, co to i po co”.
   Obserwacja: czarny prostokąt z białym „Gotowe”, zawsze w tym samym miejscu, nad treścią (zasłania plakat na 07). Pojawił się razem z pierwszym odtwarzaniem (17:56, klip TorrentClaw) i trwał na odtwarzaczu i stronach serialu „Ciało” do ~18:09; na Głównej o 18:10 zniknął. To nie okno innej aplikacji (lista okien: tylko fork). W logu brak komunikatu „ekran: Gotowe” (`ForkLog.say`), więc to nie zwykły komunikat forka. Kandydaci w kodzie z tym tekstem: `forge_ready_ready` (skórka Kicia, linia „Gotowe” pod przyciskami), `fork/sources/SourceLanguage.kt:22` („Gotowe · jakość”), „Gotowe” w `sync_*_done` / `collections_editor_done`. Źródła nie ustaliłem w przeglądzie.
   Uzupełnienie 18:41 (`25-przegladaj.png`, wyszukiwarka): napis znów widoczny. Zrzut drzewa widoku: element tekstowy aplikacji `text="Gotowe"` w granicach [120,987][216,1020] px, w TEJ SAMEJ warstwie co zegar w prawym górnym rogu ([1746,64][1840,104]) — czyli nakładka całej aplikacji (warstwa zegara Niuni, `skin/niunia/NiuniaSkin.kt:~142` `PlayerClock` / `NiuniaClockCorner`), nie ekran. Klawiatura systemowa schowana (`mInputShown=false`) — to nie przycisk klawiatury. Polskie „Gotowe” to tłumaczenie m.in. `forge_ready_ready` (ang. „Ready”, Kicia `skin/kicia/detail/ForgeReadyLine.kt:54,148`), `sync_*_done`, `collections_editor_done` („Done”).
   Poprawka: znaleźć, co go rysuje (zrzut drzewa widoku przy widocznym napisie), i usunąć — żaden stały napis bez znaczenia dla użytkownika; jeśli to stan źródła („gotowe do odtworzenia”), to w stylu Niuni i tylko tam, gdzie ma sens, z pełnym zdaniem.

## 10 — Live TV: górny rząd kart (to, co leci teraz) — ✅ TV prz04 pkt 3 (okładki TMDB na kafelkach); poprawka prz05: okładka filmu tylko dla programów ≥ 70 min („Kropka nad i”); pkt 1 (stos) niesprawdzony — brak kafla serialu; pkt 2 (MediaCard) — nie robione
Zrzut: `10-mediacard.png` — karty: 13 Ulica „Bez śladu 6…”, TVN 24 BiS „Kropka nad i”, Polsat Cafe „Hell's Kitchen…” (fokus), Polsat Cafe „Gry małżeńskie”, Comedy Central Extra „Graczykowie”; w każdej logo stacji zamiast obrazu programu; pod kartami wystają kolorowe, zniekształcone kawałki logo. Kod: `skin/niunia/livetv/NiuniaLiveTiles.kt:76-112`, warstwy pod kartą `skin/niunia/components/NiuniaSeriesStack.kt:25-45`.
1. **Pod kartami widać zniekształcony fragment logo.**
   Właściciel: „komponenty MediaCard mają błąd — pokazują pod spodem zniekształcony fragment logo”.
   Obserwacja: to „stos serialu” (decyzja 20.11, canvas `.stack`) — dwie warstwy pod kartą z TYM SAMYM obrazkiem, jedna odwrócona do góry nogami, druga obrócona o 90°. Gdy program nie ma obrazu, w warstwy trafia logo stacji (`NiuniaLiveTiles.kt:85`: `image ?: logo`) → wychodzą kolorowe, obrócone strzępy logo.
   Poprawka: warstwy stosu bez obrazka (gładkie, w kolorze powierzchni karty, jak w makiecie), nigdy z logo; sprawdzić z makietą `.stack`.
2. **Karty nie są komponentami design systemu.**
   Właściciel: „mam duże wątpliwości, czy używają one natywnych komponentów design systemu”.
   Obserwacja: słusznie — kafelek Live TV to własny `Box` z ręcznie dobranym rozmiarem, fokusem i podpisem (`NiuniaLiveTiles.kt:86-112`, komentarz „DS v2 (NowCard / ChannelCard)”), a nie `NiuniaMediaCard` z presetem `NowCard` / `ChannelCard` (`components/NiuniaMolecules.kt:156`, presety `components/NiuniaModels.kt:31-50`).
   Poprawka: kafelki Live TV przez `NiuniaMediaCard` (preset `NowCard` dla programu, `ChannelCard` dla kanału) — jeden komponent kart w całej skórce; brakujące elementy (mini pasek postępu, stos serialu) dodać do komponentu, nie do ekranu.
3. **Karty mają pokazywać okładki programów, nie logo stacji.**
   Właściciel: „powinny wyświetlać okładki programów, a nie logo stacji TV”.
   Obserwacja: przewodnik (FlixNest) dla polskich kanałów nie ma obrazków — tylko tytuł i opis; karta spada wtedy na logo (`NiuniaLiveTiles.kt:107-110`). Okładki z TMDB są już napisane, ale NIEPODŁĄCZONE: `fork/tv/ProgramCard.kt`, `fork/tv/TvTmdb.kt` (`plan/iptv/STAN.md:127-129`) — kolejna „rzecz, która jest, a jej nie widać”.
   Poprawka: podłączyć okładki TMDB do kart Live TV (tytuł programu → plakat/kadr serialu lub filmu); logo stacji tylko jako mały znaczek w rogu albo w podpisie, gdy okładki nie ma — a nie jako obraz całej karty.
   **Uzupełnienie właściciela (18:18): „wszędzie powinny być okładki programu, nie logo stacji w MediaCards”** — dotyczy WSZYSTKICH kart programów: rzędy Live TV (Wszystkie, Ulubione, Oglądaj dalej, kategorie), Odkrywaj, Szukaj, Dziś, rzędy IPTV na Głównej. Logo stacji jako obraz karty tylko na karcie samego kanału (bez programu).

## 11 — Live TV: podgląd kanału nad przewodnikiem (EPG) — ❌ TV prz04: obraz podglądu rysował się NAD gradientem (przycięcie pola) → poprawka prz05
Zrzuty: `11-pasek-epg.png` (cały ekran: „Bez śladu 6: Zakręt (14)”, podgląd 13 Ulica w prawym górnym rogu, fokus w przewodniku), `11-pasek-zoom.png` (powiększenie paska). Kod: `skin/niunia/livetv/NiuniaLivePreview.kt:82-93`.
1. **Nad przewodnikiem pasek bez szarego przyciemnienia.**
   Właściciel: „nad EPG jest pasek bez overlaya (szarego gradientu)”.
   Obserwacja: tuż nad osią godzin (18:00 / 18:30 / 19:00…) widać cienki, kolorowy pasek obrazu z podglądu kanału — dolna krawędź wideo wystaje poza przyciemnienie. Podgląd to pole 520×288 z obrazem (`TextureView`) i dwoma gradientami na wierzchu; pole nie jest przycięte do swoich granic, więc obraz (skalowany „na wypełnienie”) wychodzi kilka pikseli niżej niż gradient, który kończy się dokładnie na krawędzi pola.
   Poprawka: przyciąć podgląd do jego pola (obraz nie może wyjść poza gradient) i dolny gradient zakończyć pełnym tłem trochę wyżej (np. od 90% pełne tło), żeby krawędź wideo nigdy nie była widoczna; sprawdzić na kanałach o różnych proporcjach obrazu (4:3, 16:9).
2. **Podgląd kanału większy, z zachowaniem przyciemnienia.**
   Właściciel: „podgląd kanału zrób większy, zachowaj pokrycie gradientem”.
   Obserwacja: podgląd ma stałe 520×288 (`NiuniaLivePreview.kt:82`, ok. połowy szerokości ekranu), od góry do paska kategorii; nad nim przyciski Szukaj / Dziś / Odkrywaj.
   Poprawka: większe pole podglądu (np. ~640×360 w proporcji 16:9, od prawej krawędzi i od samej góry ekranu, pod przyciskami i zegarem); gradienty z lewej i od dołu zostają (pełne tło pod tytułem programu i nad paskiem kategorii) — rozmiar gradientu liczony od nowego pola, nie od starych 520×288. Tytuł, „LIVE” i plakietka „Na żywo · kanał” bez zmian położenia względem przewodnika.

## 12 — Live TV: podpis zaznaczonej karty (rząd „Ulubione”) — ✅ TV prz04 (czyste nazwy kanałów w podpisach i przewodniku)
Zrzut: `12-karta-podpis.png` — zaznaczona „Gry małżeńskie (2… · Polsat Cafe”; obok „Świat według Kiepskich · Strong…” (×2), „MUHAMMAD KHALI…”. Kod: podpis `skin/niunia/livetv/NiuniaLiveTiles.kt:136-142` i `tileCaption` `:150-162` (skracanie tytułu `components/niuniaTileTitle`, nazwa kanału `components/niuniaShortChannel`).
1. **Nie ucinać nazwy programu — przenosić do drugiej linii (tam, gdzie nazwa kanału).**
   Właściciel: „nie ucinaj nazwy programu, tylko przenoś do drugiej linii, tam gdzie jest nazwa kanału”.
   Obserwacja: `tileCaption` celowo skraca tytuł („tytuł skracany, żeby kanał został”) do jednej linii, a druga linia jest zarezerwowana na „· kanał” → „Gry małżeńskie (2…”.
   Poprawka: tytuł płynie normalnie przez obie linie, „· kanał” dopisany za nim; skracać („…”) dopiero, gdy całość nie mieści się w 2 liniach — wtedy najpierw skraca się nazwę kanału, nie tytuł.
2. **Zamiast „Strong…” sama nazwa kanału.**
   Właściciel: „nie wyświetlaj »Strong…«, tylko samą nazwę kanału”.
   Obserwacja: nazwy kanałów od dostawcy mają przedrostek paczki i kraju: „Strong8K · PL: POLSAT FILM ᴴᴰ”, „Strong8K · PL VIP: POLSAT FILM ᴿᴬᵂ”, „Strong8K · AR: MUHAMMAD KHALIL …” (log `ForkFocus`). Skrót nazwy (`niuniaShortChannel`) zostawia „Strong8K”, więc w podpisie zostaje „Strong…”.
   Poprawka: w podpisach kart (i wszędzie na ekranie) czysta nazwa kanału: bez nazwy paczki („Strong8K ·”), bez kraju/grupy („PL:”, „PL VIP:”, „AR:”), bez znaczków jakości („ᴴᴰ”, „ᴿᴬᵂ”, „FHD”, „4K”), z normalną wielkością liter → „Polsat Film”. Najlepiej jedna funkcja nazw (`fork/tv/names/ChannelNames.shown`) użyta też przez `niuniaShortChannel`.

## 13 — Live TV: pasek postępu na karcie (rząd „Oglądaj dalej”) — ✅ TV prz04 (pasek nad podpisem, na całą szerokość)
Zrzut: `13-pasek-postepu.png` — karty „Wznów”: „Gry małżeńskie (1… · Polsat Cafe”, „Czarna Żmija · Polsat Comedy Ce…”; mały pasek postępu stoi z lewej, obok podpisu. Kod: `skin/niunia/livetv/NiuniaLiveTiles.kt:131-142` (`Row`: [pasek 24×4] + tekst).
1. **Pasek postępu nad podpisem, nie z lewej.**
   Właściciel: „przenieś pasek postępu oglądania od razu ponad napisy, a nie z lewej, tak żeby napisy miały więcej miejsca”.
   Obserwacja: podpis i pasek są w jednym wierszu (`Row`, odstęp 8) — pasek 24 szer. + 8 odstępu zabiera ~32 z ~160 szerokości tekstu, stąd szybsze ucinanie tytułu i kanału.
   Poprawka: `Column` zamiast `Row` — pasek na całą szerokość (albo wg makiety) tuż nad pierwszą linią podpisu, tekst na pełną szerokość karty. Ta sama zmiana dla paska „na żywo” (czerwony) na kartach programów — jeden układ podpisu we wszystkich kartach (najlepiej w komponencie MediaCard, zob. ekran 10 pkt 2).

## 14 — Live TV: kategorie nad przewodnikiem (Informacje, Filmy i seriale…) — ✅ TV prz04 (filtr od samego przejścia na pigułkę, polskie pierwsze w „Informacje”)
Zrzut: `14-kategorie.png` — wybrana „Informacje” (szara), fokus na „Filmy i seriale” (biała); kafelki: wPolsce (podpis „Strong8K · PL: WPOLSCE”), CNBC, EuroNews („Ринг”), BBC World News, CNN; przewodnik: CNBC, EuroNews po rosyjsku, BBC, CNN. Kod: `skin/niunia/livetv/NiuniaLiveTv.kt:179-190` (kolejność wg kraju), `skin/niunia/livetv/NiuniaLiveData.kt:238-252` (kategorie).
1. **Kategorie mają filtrować kafelki.**
   Właściciel: „kategorie nad EPG powinny filtrować kafelki”.
   Obserwacja: kafelki zmieniają się dopiero po OK na kategorii (log „filtr=Filmy i seriale, kafelki=20”); samo przejście po kategoriach (fokus na „Filmy i seriale”) zostawia kafelki i przewodnik z poprzedniej („Informacje”) — na ekranie wygląda to tak, jakby kafelki nie słuchały kategorii.
   Poprawka: kafelki (i przewodnik) zmieniają się od razu przy przejściu na kategorię (z krótkim opóźnieniem ~300 ms, żeby szybkie przewijanie nie liczyło każdej), bez OK; zaznaczona = wybrana (jeden stan, nie dwa: szary i biały).
2. **Najpierw polskie programy — ustawione w ustawieniach, a pokazują się np. arabskie.**
   Właściciel: „najpierw polskie programy — tak jest ustawione w ustawieniach, a pokazują się na przykład programy arabskie”.
   Obserwacja: ustawienie działa przy wejściu (`kraj IPTV Mode(prefer=true, country=pl)`), ale log pokazuje je tylko dla pierwszych list (205 i 324 kanały, 18:11:11-12); po dociągnięciu pełnej listy (59 573 kanały, 18:11:20) kolejności wg kraju już nie ma w logu. W kategorii „Informacje” na górze CNBC, EuroNews (rosyjski), BBC, CNN — polskie (TVN24, Polsat News…) nie są pierwsze.
   Poprawka: kolejność „najpierw Polska” stosować do KAŻDEJ wersji listy (także po dociągnięciu kolejnych źródeł) i wewnątrz każdej kategorii: najpierw kanały/programy z Polski, potem reszta; to samo dla kafelków. Sprawdzić: kategoria „Informacje” zaczyna się od polskich kanałów informacyjnych.

## 15 — Live TV: panel „Kraj i język” — ✅ TV prz04 („Kraje arabskie 3470”, bez fałszywej Argentyny)
Zrzut: `15-kraj.png` — zakładki Kraj / Język; „Wszystkie kraje ✓”, Polska (1158 kanałów), Stany Zjednoczone (11 103), Argentyna (3470), Wielka Brytania (2680), Szwecja (2247)…; brak Indii. Kod: `skin/niunia/livetv/NiuniaLiveRegion.kt:123-144` (liczenie), kraj z nazwy `fork/tv/country/TvCountry.kt:63-78`, `fork/tv/dupes/ChannelDupes.kt:38-48`.
1. **Polska (wybrany kraj) pierwsza — dobrze; ale brakuje krajów, np. Indii (~10 000 kanałów).**
   Właściciel: „Polska jako wybrany preferowany kraj powinna być pierwsza, ale potem brakuje krajów, jak Indie, które mają 10 000 kanałów”.
   Obserwacja: Polska jest pierwsza (na sztywno „pl” na górze, potem wg liczby kanałów, maks. 30 pozycji). Kraj czytany jest z przedrostka nazwy kanału lub grupy (2-3 litery przed „:”, „|”, „-”), a potem przepuszczane są TYLKO dwuliterowe kody ISO (`NiuniaLiveRegion.kt:129`). Prawdopodobna przyczyna: kanały indyjskie mają przedrostki trzyliterowe lub słowne („IND:”, „INDIA |”, „HINDI”, „TAMIL”…) — odpadają jako nieznany kraj. Do potwierdzenia na liście kanałów (bez zapisywania adresów).
   Poprawka: rozpoznawać też kody trzyliterowe (IND, USA, GBR, ARG…) i pełne nazwy krajów oraz języków w grupach („INDIA”, „HINDI”, „TAMIL” → Indie); sprawdzić, ile kanałów zostaje „bez kraju” i pokazać największe takie przedrostki w logu; preferowany kraj z ustawień (nie na sztywno „pl”) zawsze pierwszy.

## 16 — Live TV: filtr języka „Hiszpański” — ✅ TV prz04 („Hiszpański” = ES/ARG/M+, bez AR:; złączone kopie jakości 2379 → 1840)
Zrzut: `16-hiszpania.png` — chip „Hiszpański” wybrany, kategoria „Filmy i seriale”; kafelki: beIN Drama, Mamlakat al-Fajr, CBC Drama, „Lama Kona Soghayareen” (podpisy „Strong8K · AR: …”); przewodnik: El Fadjer (×2, „Brak programu”), beIN „The American”. Log 18:23: „filtr=Filmy i seriale, kafelki=20, kanały=229 (lista 5706)”, podgląd „Strong8K · AR: EL Fadjer Drama DZ”, „AR: BEIN STAR FILMS”, „AR: AL HAYAT DRAMA”. Kod: `skin/niunia/livetv/NiuniaLiveRegion.kt:129-133` (kraj → język), `fork/tv/country/TvCountry.kt:63-78`.
1. **Wybrane hiszpańskie kanały, a pokazują się arabskie — w kafelkach i w przewodniku.**
   Właściciel: „wybrane hiszpańskie kanały, a pokazują się arabskie, i na górze, i na dole”.
   Obserwacja (przyczyna): w listach IPTV przedrostek „AR:” znaczy „kanały arabskie”, a aplikacja czyta go jak kod ISO kraju „AR” = **Argentyna**, a Argentyna → język hiszpański. Stąd w panelu „Kraj i język” „Argentyna 3470 kanałów” (ekran 15) — to w rzeczywistości kanały arabskie, a filtr „Hiszpański” pokazuje arabskie.
   Poprawka: słownik przedrostków IPTV ważniejszy niż kody ISO: „AR” / „ARAB” / „ARABIC” → region arabski, język arabski (nie Argentyna); podobne pułapki sprawdzić: „EX-YU”, „AF” (Afryka vs Afganistan), „LAT”/„LATAM” (Ameryka Łacińska → hiszpański), „UK”, „IN”/„IND”. Argentyna tylko z wyraźnego „ARG” / „ARGENTINA”. Po poprawce: „Hiszpański” = Hiszpania + Ameryka Łacińska, zero kanałów „AR:”.
2. **Przy okazji (z tego samego zrzutu):** przewodnik pokazuje kilka razy ten sam kanał z tym samym numerem („40” ×3, El Fadjer w wersjach „DZ” i „DZ LQ”) i wszystkie z „Brak programu” — w filtrach języka duplikaty jakości tego samego kanału powinny być złączone jak na liście głównej.

## 17 — Live TV: Szukaj (klawiatura, Kategorie, wyniki) — ✅ TV prz04 (bez „Rzędy”, siatka z obrazami programów, „Ostatnio wyszukiwane”, długie OK = Usuń z historii)
Zrzut: `17-rzedy.png` — pole „Program, kanał, kategoria” (50 programów · 0 kanałów · 0 kategorii), klawiatura abc/123, lista Kategorie (Filmy → Komedia 78 wybrana), wyniki „Programy · 50” z przełącznikiem **Rzędy** (zaznaczony) / Siatka. Kod: `skin/niunia/livetv/NiuniaLiveSearch.kt:225-232` (przełącznik), teksty `res/values/niunia_tv_strings.xml:36` (`niunia_tv_view_rows`) i sąsiedni `niunia_tv_view_grid`.
1. **Usunąć opcję „Rzędy”.**
   Właściciel: „usuń opcję zaznaczoną — Rzędy”.
   Obserwacja: przełącznik Rzędy / Siatka nad wynikami; „Rzędy” = jeden poziomy rząd kart, który na tym ekranie mieści ~2,5 karty (pierwsza ucięta z lewej: „…BO”), reszta wyników niewidoczna.
   Poprawka: usunąć przełącznik i tryb „Rzędy”; wyniki zawsze jako siatka (bez wyboru). Zapamiętany wybór „Rzędy” z ustawień (`NiuniaLiveFilters`) zignorować / wyczyścić. Nieużywany tekst `niunia_tv_view_rows` usunąć.
2. **Pod klawiaturą ostatnie wyszukiwania; długie OK = usuń z historii.**
   Właściciel: „pod klawiaturą mają być ostatnie wyszukiwania z opcją usunięcia z historii po długim naciśnięciu”.
   Obserwacja: pod klawiaturą Live TV jest tylko SPACJA / ⌫ / WYCZYŚĆ, pusto niżej; Szukaj Live TV nie ma historii. Wyszukiwarka filmów (Niunia) ma już historię bazy (`state.recentSearches`, `NiuniaSearch.kt:208` — 3 ostatnie), a model bazy umie usuwać pojedyncze wpisy (`ui/screens/search/SearchViewModel.kt:495-501`: `removeRecentSearch`, `clearRecentSearches`).
   Poprawka: pod klawiaturą lista „Ostatnie wyszukiwania” (np. 5 wpisów, najnowsze na górze); OK = wpisz i szukaj; długie OK = małe okno „Usuń z historii” (potwierdzenie OK). Historia Live TV osobna od historii filmów (albo wspólna — do decyzji właściciela); to samo zachowanie (długie OK = usuń) także w wyszukiwarce filmów.
3. **Przy okazji (z tego samego zrzutu):** karty wyników znowu z logo stacji (Canal+, HBO) zamiast okładek — zob. decyzja z ekranu 10/12 (okładki wszędzie).

## 18 — Odtwarzacz IPTV: rząd na dole nakładki — ✅ TV prz04 (rząd = poprzednie emisje z archiwum)
Zrzut: `18-na-dole.png` — kanał TLC (TR), „Karayip Evleri” 18:00-18:30, opóźnienie −1:06; na dole 4 karty: „Za 1 min” (18:30), „Za 30 min” (19:00), „Wkrótce” (19:30), „Wkrótce” (20:00) — same PRZYSZŁE emisje. Kod: `skin/niunia/live/NiuniaLivePlayer.kt:346-375` (`buildRow`).
1. **Na dole mają być poprzednie emisje (z archiwum) programu, który teraz leci; gdy ich nie ma — programy z archiwum z tej samej kategorii (np. Polska, informacyjne).**
   Właściciel: „na dole mają się wyświetlać poprzednie emisje (z archiwum) tego programu, który teraz leci; jeśli nie ma, to programy z archiwum w tej samej kategorii — czyli np. Polska, programy informacyjne”.
   Obserwacja: rząd = inne emisje tego samego programu z przewodnika kanału: minione TYLKO, gdy są w archiwum, plus do 4 następnych („Za 1 min”, „Wkrótce”). TLC nie ma archiwum, więc zostały same przyszłe — nic z nich nie da się obejrzeć. Gdy emisji jest mniej niż 2, rząd bierze programy z innych kanałów z tej samej kategorii przewodnika — ale nie sprawdza archiwum ani kraju.
   Poprawka: (a) najpierw poprzednie emisje tego programu z archiwum (najnowsze pierwsze), bez przyszłych; (b) gdy ich nie ma — programy z archiwum z tej samej kategorii i tego samego kraju co oglądany kanał (np. polskie informacyjne), tylko takie, które da się od razu włączyć; (c) gdy nic z archiwum — rząd ukryty (bez kart „Wkrótce”). Nagłówek rzędu mówi, co to jest („Poprzednie odcinki” / „Z archiwum: informacyjne”).

## 19 — Odtwarzacz IPTV: kafel przełączania kanałów (▲ logo ▼) — ✅ TV prz04 (logo na szerokość kafla)
Zrzut: `19-logo.png` — kafel z lewej (zaznaczony): ▲, małe logo „TLC HD”, ▼; obok „Karayip Evleri”, przyciski „Poprzedni: TLC”, ▶, „Na żywo”, +. Kod: `skin/niunia/live/NiuniaLivePlayer.kt:654-670` (`ChannelSwitch`).
1. **Logo stacji większe — na całą szerokość kafla.**
   Właściciel: „logo stacji większe — na całą szerokość kafla ze skakaniem po programach”.
   Obserwacja: kafel ma 110 szer., logo ma stałą wysokość 32 (pole 36), wyrównane do lewej; logo prawie kwadratowe (TLC) zajmuje ~1/3 szerokości kafla, reszta pusta.
   Poprawka: logo wypełnia szerokość kafla (110 minus marginesy), wysokość dopasowana do proporcji logo (maks. ~60, żeby kafel nie urósł za bardzo), wyśrodkowane; strzałki ▲▼ wyśrodkowane nad i pod logo. Gdy brak logo — nazwa kanału dużą czcionką na całą szerokość (2 linie).

## 20 — Live TV: kategoria „Oglądaj dalej” — ✅ TV prz04 (Ulubione / Oglądaj dalej bez filtra kraju)
Zrzut: `20-ogladaj-dalej.png` — „Oglądaj dalej” wybrane, w miejscu kafelków tylko „Nic tu jeszcze nie ma.”; przewodnik: tureckie kanały (Planet, BBC Earth, Discovery…) wszystkie z numerem 39, „No Data”, „Brak programu”. Kod: `skin/niunia/livetv/NiuniaLiveData.kt:214-228` (kafelki „Oglądaj dalej”), filtr kraju/języka `skin/niunia/livetv/NiuniaLiveRegion.kt`, kolejność/przygotowanie listy `skin/niunia/livetv/NiuniaLiveTv.kt:179-190`.
1. **„Oglądaj dalej” ładuje się bardzo wolno i pokazuje „Nic tu jeszcze nie ma”.**
   Właściciel: „oglądaj dalej ładuje się bardzo wolno, a do tego jest komunikat błędu »Nic tu jeszcze nie ma«”.
   Obserwacja (przyczyna pustki): włączony był filtr języka „Turecki” (log 18:29:52 „język Turecki: 1234 z 59573 kanałów”, zostawiony po wcześniejszych próbach) → „filtr=oglądaj dalej, kafelki=0”. Rozpoczęte programy są na polskich kanałach, więc filtr je ukrył. Po wyborze „Polska” (18:32:54) od razu „kafelki=3”. Przewodnik przy tym filtrze: wszystkie kanały z numerem 39 i „No Data”.
   Poprawka: (a) „Oglądaj dalej” i „Ulubione” to listy osobiste — NIE podlegają filtrowi kraju/języka (zawsze pokazują wszystko, co rozpocząłem/polubiłem); (b) komunikat pustej listy mówi prawdę i co zrobić, np. „Nie masz rozpoczętych programów w wybranym języku (Turecki). Pokaż wszystkie?” zamiast „Nic tu jeszcze nie ma”; (c) wybór kraju/języka widoczny na chipie (był „Kraj i język”, a działał „Turecki”) — zob. też ekran 15.
2. **Sekcje mają być w pamięci podręcznej i pojawiać się natychmiast.**
   Właściciel: „sekcje muszą być w cache i pojawiać się natychmiast”.
   Obserwacja: przy wejściu do Live TV lista budowana jest na nowo z ~59 573 kanałów (log 18:11:20 „wszystkie: 59573 z 59573 kanałów (4128 ms)”), a kafelki rosną w kilku krokach (8 → 12 → …, „próbka startowa”); każda zmiana filtra kraju/języka to kolejne ~250 ms.
   Poprawka: zapisywać gotowe sekcje (Ulubione, Oglądaj dalej, kafelki każdej kategorii i przewodnik pierwszego ekranu) na dysku i pokazywać je od razu przy wejściu; odświeżać w tle i podmieniać po cichu. Cel na JMGO: kafelki i przewodnik widoczne < 300 ms od wejścia, bez „próbki” i bez przeskakiwania kafelków. Budżety: `plan/DECYZJE.md`, sekcja „Wydajność”.

## 21 — Live TV: numery kanałów w przewodniku — ❌ TV prz04: w filtrze „Hiszpański” wszystkie kanały miały numer 42 → poprawka prz05 (klucz z adresem)
Zrzuty (przewodnik z numerami): `10-mediacard.png` (12, 13, 14, 15), `12-karta-podpis.png` (Ulubione: 115, 56, 961, 58), `16-hiszpania.png` (40, 40, 40, 43), `20-ogladaj-dalej.png` (39, 39, 39, 39 — różne kanały tureckie). `21-numery.png` — już odtwarzanie (Polsat Cafe), przewodnik zamknięty. Kod: `skin/niunia/livetv/NiuniaLiveData.kt:380-381` (`numbersOf`), użycie `skin/niunia/livetv/NiuniaLiveTv.kt:644`, rysowanie `:806-811`.
1. **Przewodnik ma złe numery kanałów.**
   Właściciel: „przewodnik ma złe numery kanałów”.
   Obserwacja: numer to NIE numer kanału od dostawcy, tylko pozycja kanału na całej posortowanej liście (~59 tys.), liczona po adresie strumienia (`putIfAbsent(streamUrl, i + 1)`). Skutki: (a) w filtrach i Ulubionych numery „skaczą” (115, 56, 961, 58); (b) różne kanały mają ten sam numer (39 ×4, 40 ×3) — najpewniej mają wspólny klucz adresu (kanały, dla których link tworzy się dopiero przy odtwarzaniu, np. portal Stalker), więc wszystkie dostają numer pierwszego z nich; (c) numer zmienia się po każdej zmianie kolejności (kraj, nowe źródło).
   Poprawka: numer z listy dostawcy (numer kanału z playlisty / portalu), gdy jest; gdy go nie ma — stały numer kolejny w pełnej liście ustalony raz (zapisany), unikalny dla kanału (klucz = identyfikator kanału, nie adres strumienia). Numer nie zmienia się przy filtrach — „TVN 24” ma zawsze ten sam numer, także w Ulubionych. Do decyzji właściciela: czy polskie kanały mają mieć numery „jak w telewizji” (1 TVP1, 2 TVP2, 3 Polsat, 4 TVN…).

## 22 — Odtwarzacz IPTV: program z archiwum — ✅ TV prz04 pkt 1, 3 (ARCHIWUM bez kafla kanału i „Na żywo”, ▲ tylko nakładka, ◀▶ przewija); ❌ pkt 2 puste karty odcinków → okładki TMDB w prz05
Zrzut: `22-archiwum.png` — „ARCHIWUM 07:00-08:05 · Polsat Cafe”, „Gry małżeńskie (197)”, pasek 0:38 / −1:04:21, przyciski ↻ ▶ **● Na żywo** ✓; rząd odcinków (192, 193, 194, 195…) z zieloną kropką, bez obrazków. Kod: `skin/niunia/live/NiuniaLivePlayer.kt:250-300` (klawisze, poziomy), `:482-483` (przycisk „Na żywo” w archiwum), `:253-259` (pasek przewijania), `:616-646` (`LiveBar`).
1. **W archiwum żadnego przełączania kanałów ani przycisku „Na żywo”.**
   Właściciel: „przy odtwarzaniu programu z archiwum ma nie być przełączania kanałów żadnego ani przycisku na żywo”.
   Obserwacja: kafel kanału (▲ logo ▼) jest w archiwum już schowany (log 18:34:28 „archive: channel switch, channel logo and up/down arrows hidden”), ale „Na żywo” dodano celowo 26.11 (UX #72, `:482-483`: „w archiwum nie było jak wrócić do transmisji”) — właściciel teraz to odwraca. Logo stacji w prawym górnym rogu też zostaje.
   Poprawka: w archiwum bez „Na żywo” i bez jakiejkolwiek zmiany kanału (▲▼, CH+/CH−, cyfry, „Poprzedni: kanał”); wyjście z archiwum tylko Wstecz (do przewodnika / miejsca, z którego wszedłem). Zaktualizować komentarz/decyzję #72.
2. **Brak okładek odcinków programu.**
   Właściciel: „brakuje też okładek odcinków programu”.
   Obserwacja: karty odcinków (192-195) to puste, granatowe prostokąty z tytułem i godziną — przewodnik (Polsat Cafe) nie ma obrazków; okładki z TMDB są w kodzie niepodłączone (zob. ekran 10 pkt 3).
   Poprawka: okładka/kadr odcinka z TMDB (serial „Gry małżeńskie” → kadr odcinka albo plakat serialu, gdy odcinka nie ma); w ostateczności kadr z samego nagrania archiwum. Ta sama zasada co „okładki wszędzie” (DECYZJE 27.11).
3. **Przewijanie programu z archiwum musi działać.**
   Właściciel: „przewijanie programu z archiwum musi działać”.
   Obserwacja: log 18:35:15-18:35:18 — 13 naciśnięć ◀/▶ trafiło w rząd odcinków (poziom 1), nie w pasek; pasek przewijania jest dostępny tylko przez ▲ z przycisków (`:253-259`, slot BAR, gdy kanał ma archiwum) — tego nie widać i nie da się zgadnąć; w logu brak jakiegokolwiek przewinięcia.
   Poprawka: w archiwum ◀/▶ ZAWSZE przewija (przy schowanej nakładce i na przyciskach: ±10 s, przytrzymanie = coraz szybciej, jak w odtwarzaczu filmów); po zaznaczeniu paska — skoki z podglądem godziny; klawisze przewijania pilota (⏪ ⏩) też. Rząd odcinków dopiero ▼. Sprawdzić na JMGO: ◀ cofa, ▶ przesuwa, czas na pasku się zmienia, obraz rusza od nowego miejsca.

## 23 — YouTube (Niunia): strona główna — ✅ TV prz04 pkt 1a (fokus na 1. filmie); Mix niesprawdzony (brak Mixu na ekranie); pusta playlista — poprawka prz03, niesprawdzona
Zrzut: `23-ekran.png` — szyna z lewej (Główna zaznaczona), siatka filmów po 3 (Wiedźmin 4, Wonderboom, „Mermaid Chunky — céilí” z plakietką **Mix**…). Kod: `skin/niunia/youtube/NiuniaYouTube.kt:150-156` (fokus startowy).
1. **Po wejściu fokus na pierwszym filmie, a ten sam się odtwarza z dźwiękiem.**
   Właściciel: „focus po wejściu w ekran ma być na pierwszym filmie i ten ma się odtwarzać sam z dźwiękiem”.
   Obserwacja: fokus startowy wybierany raz, gdy kart jeszcze nie ma (log 18:37:36 „HOME — kart 0, wczytuję” → „fokus startowy → szyna”); karty dochodzą 2,3 s później (20 → 109), ale fokus zostaje na szynie („Główna”). Podglądu filmu z dźwiękiem na tym ekranie nie ma.
   Poprawka: (a) fokus startowy na pierwszej karcie — gdy kart jeszcze nie ma, przenieść go na pierwszą, gdy tylko się pojawią (chyba że użytkownik zdążył już coś zaznaczyć); karty z pamięci podręcznej, żeby były od razu (jak w ekranie 20); (b) zaznaczony film po ~1 s sam odtwarza się w swojej karcie (albo w dużym podglądzie u góry) Z DŹWIĘKIEM; zejście z karty zatrzymuje; OK = pełny ekran od tego samego miejsca. Do sprawdzenia na JMGO: zużycie pamięci i płynność przewijania siatki przy podglądzie.
2. **Nie działają Mixy (playlisty YouTube).**
   Właściciel: „nie działają Mixy (playlisty YouTube)”.
   Obserwacja: log 18:38:30 — OK na „Mermaid Chunky — céilí (Mix)” → „youtube: OK → PLAYLIST …” → „kart 0, wczytuję” → „kart 0”; pusta lista, fokus spada na „Główna”, po Wstecz powrót do HOME. Mix to playlista generowana przez YouTube (identyfikator zaczyna się od „RD…”), której nie da się wczytać jak zwykłej playlisty.
   Uzupełnienie z logu (18:39:05): drugie OK na Mixie — `YouTubeVM: couldn't load Mermaid Chunky … IllegalStateException: fromNullable result is null` (plik filmu był gotowy po 1374 ms, wywaliło się dopiero składanie listy Mixu).
   Poprawka: OK na Mixie = od razu odtwarzanie pierwszego filmu Mixu, a kolejne filmy z Mixu jako „Następne” w odtwarzaczu (wczytywane przy odtwarzaniu, jak robi YouTube); zwykłe playlisty dalej jako lista. Gdy lista wróci pusta — komunikat na ekranie i fokus zostaje na karcie (nie skok na szynę).

## 24 — YouTube: Szukaj — ✅ TV prz04 (własne okno „Szukaj w YouTube”, podpowiedzi i wyniki z YouTube); ❌ fokus po wyszukaniu na szynie → poprawka prz05
Zrzut: `24-yt-szukaj.png` — po „Szukaj” w YouTube otwiera się OGÓLNA wyszukiwarka („Filmy, seriale, TV, YouTube i osoby”, klawiatura abc/123/Powiedz, „Odkrywaj”, „Przeglądaj”: Filmy / Seriale / Live TV / Akcja — puste szare kafle bez obrazków, „Polecane” z filmami i serialami). Log 18:39:40 i 18:40:04: „youtube: Szukaj → ekran Szukaj (zakładka YouTube)” → „menu: przejście z ekranu → search”.
1. **Wyszukiwarka YouTube nie działa.**
   Właściciel: „wyszukiwarka YouTube nie działa”.
   Obserwacja: przycisk Szukaj w YouTube przenosi do wspólnej wyszukiwarki (filmy, seriale, TV), a nie do wyszukiwania YouTube — log mówi „zakładka YouTube”, ale na ekranie żadnej zakładki ani wyników YouTube; podpowiedzi i „Polecane” to filmy/seriale z katalogu. W logu brak jakiegokolwiek zapytania do YouTube po wejściu.
   Poprawka: Szukaj w YouTube = wyszukiwarka tylko YouTube (pole „Szukaj w YouTube”, podpowiedzi z YouTube, wyniki jako karty YouTube: filmy, kanały, playlisty/Mixy), Wstecz wraca do YouTube; ostatnie wyszukiwania pod klawiaturą z usuwaniem na długie OK (jak ekran 17). Do potwierdzenia z właścicielem przy teście: co wpisane i co się stało po OK.
2. **Przy okazji (z tego samego zrzutu):** kafle „Przeglądaj” (Filmy, Seriale, Live TV, Akcja) są puste — bez obrazków.

## 25 — Szukaj (ogólna): kafle „Przeglądaj” — ✅ TV prz04 („Przeglądaj” = pigułki)
Zrzut: `25-przegladaj.png` — po prawej od klawiatury: „Odkrywaj: gatunek, rok, katalog”, „Przeglądaj” = 4,5 dużych pustych kafli (Filmy, Seriale, Live TV, Akcja, P…) na ~połowę wysokości ekranu, niżej „Polecane” ucięte u dołu. Kod: `skin/niunia/search/NiuniaSearch.kt:301-330` (`Browse`).
1. **Kafle „Przeglądaj” zajmują większość ekranu — mają być rzędem przycisków.**
   Właściciel: „większość ekranu zajmują kafle »Przeglądaj« — one mają być zredukowane do rzędu przycisków”.
   Obserwacja: kafle w rozmiarze plakatów (bez obrazków, ekran 24 pkt 2) zajmują ~200 z 540 wysokości; przez to „Polecane” (jedyne obrazy) widać tylko w 1/3.
   Poprawka: „Przeglądaj” jako jeden rząd przycisków-pigułek (jak kategorie w Live TV: „Filmy”, „Seriale”, „Live TV”, „YouTube”, „Akcja”, „Komedia”…), przewijany ◀▶; zwolnione miejsce dla „Polecanych” (pełne plakaty) i ostatnich wyszukiwań. „Odkrywaj: gatunek, rok, katalog” może być pierwszym przyciskiem tego rzędu.

## 26 — Odtwarzacz: ekran wczytywania (buforowanie przed filmem) — ✅ TV prz04 (◀▶ Inne źródło ↔ Anuluj; linia „1080p • HEVC • 1,5 GB • polski dźwięk”); OK na „Anuluj” niesprawdzone
Zrzut: `26-buforowanie.png` — zrobiony już po starcie filmu („The Brutalist” z „Oglądaj dalej”, napisy PL; w rogu znów „Gotowe”). Ekran wczytywania trwał 18:42:16 → 18:42:28 (12 s). Kod: `skin/niunia/player/NiuniaPlayerOverlays.kt:85-112` (`PlayerLoading`: „Buforuję… 42%”, etap, źródło; przyciski „Inne źródło” (fokus) · „Anuluj”).
1. **Na ekranie buforowania nie działają przyciski.**
   Właściciel: „ekran buforowania przed odtworzeniem filmu: nie działają przyciski”.
   Obserwacja: fokus startowy na „Inne źródło” jest (log 18:42:16 „focus=Inne źródło”), ale ▶ (18:42:19), ◀ (18:42:19) i ▼ (18:42:20) nie przesunęły fokusu (brak zmiany w `ForkFocus`), a w logu brak „wczytywanie → Inne źródło” / „→ Anuluj” — klawisze dochodzą do aplikacji, ale przechwytuje je odtwarzacz pod spodem (obsługa klawiszy odtwarzacza w czasie wczytywania), nie przyciski nakładki.
   Poprawka: na czas ekranu wczytywania klawisze ◀▶ i OK należą do nakładki (Inne źródło ↔ Anuluj, OK = wykonaj), Wstecz = Anuluj; sprawdzić na JMGO: ▶ przenosi na „Anuluj”, OK na „Anuluj” wraca, OK na „Inne źródło” otwiera listę źródeł / bierze następne.
2. **Na ekranie buforowania informacja o parametrach pliku: rozdzielczość, bitrate i inne.**
   Właściciel: „na ekranie buforowania powinna być dostępna informacja o parametrach buforowanego pliku, czyli rozdzielczość, bitrate i inne”.
   Obserwacja: ekran pokazuje logo, kółko, „Buforuję… NN%”, etap (`loadingMessage`) i jedną linię z nazwą źródła (`currentStreamName`, `NiuniaPlayerOverlays.kt:98-105`) — bez parametrów pliku.
   Poprawka: pod nazwą źródła krótki wiersz parametrów (z opisu źródła zanim plik się otworzy, potem z samego pliku): rozdzielczość (4K / 1080p), HDR / Dolby Vision, kodek obrazu, bitrate (Mb/s), rozmiar pliku, dźwięk (język, kanały 5.1, kodek), napisy PL tak/nie, dodatek; do tego prędkość pobierania na żywo (Mb/s) obok procentu buforowania. Prosto dla użytkownika („4K · HDR · 18 Mb/s · 12,4 GB · Polski 5.1 · napisy PL”).


## 27 — Szczegóły filmu (hero): szare przyciemnienie pod tekstem — ✅ TV prz04 (hero bez szarego gradientu z lewej)
Zrzut: `27-gradient.png` — „Daniel” (HBO, dokument): lewa ~45% obrazu pod ciężką szarą mgłą (logo, opis, przyciski), po prawej ostry obraz; w rogu znów „Gotowe”. Kod: `skin/niunia/detail/NiuniaDetail.kt:216-221` i `:663-665` (na Androidzie 11 / JMGO zamiast rozmycia „scrim-detail-flat” = gradient `NiuniaGradient.ScrimDetailFlat`; rozmycie `detailBlur` tylko na API 31+), statyczne rozmycie obrazów: `fork/images` (log „rozmycie gotowe raz (procesor)”, `NativeBlur`, pamięć Coil).
1. **Szary gradient pod tekstem zbyt agresywny — zastąpić gradientem przechodzącym w rozmycie (obraz z rozmyciem policzonym statycznie).**
   Właściciel: „nakładka szary gradient pod tekstem jest zbyt agresywna; zastąp ją gradientem przechodzącym w blur — masz obraz z nałożonym blurem policzony statycznie”.
   Obserwacja: na JMGO (Android 11) nie ma rozmycia na żywo, więc kod rysuje zamiast niego prawie pełną szarą warstwę od lewej. Aplikacja JUŻ liczy raz rozmytą kopię obrazu na procesorze (używaną pod hero przy ▼: log „tło pod hero: sam rozmyty obraz z przyciemnieniem”, „rozmycie gotowe”).
   Poprawka: dwie warstwy obrazu: ostry + statycznie rozmyta kopia (ta sama, co pod hero), rozmyta widoczna przez maskę-gradient od lewej (pełna pod tekstem → zanika do ~55% szerokości); BEZ szarego przyciemnienia od lewej (zob. pkt 2 — właściciel: usunąć kompletnie). Ten sam sposób na Androidzie 11 i nowszych (bez osobnego „flat”). Dotyczy też karty tytułu (ekran 01 pkt 1) i baneru Głównej — jeden komponent tła hero.
2. **Na hero są aż dwie nakładki z gradientem — ta od lewej ma zniknąć całkowicie.**
   Właściciel (18:51, „Fool Night”, zrzut `28-dwie-nakladki.png`): „są obecne na hero nawet dwie nakładki z gradientem — ta idąca od lewej ma być usunięta kompletnie”.
   Obserwacja: log 18:50:18 „szczegóły: effects: flat (scrim-detail-flat + scrim-detail-bottom, przyciski haze)” — dwie warstwy: od lewej `ScrimDetailFlat` (`NiuniaDetail.kt:220`) i od dołu `ScrimDetailBottom` (`:221`); do tego tło strony (fork/glass) rysuje je drugi raz w `:663-665`.
   Poprawka: usunąć gradient od lewej (`ScrimDetail` / `ScrimDetailFlat`) we wszystkich miejscach hero (Szczegóły, karta tytułu `NiuniaTitlePager.kt:137`, baner Głównej `NiuniaHeroPage.kt:124`); zostaje tylko gradient od dołu (przejście w stronę pod hero). Czytelność tekstu z lewej — przejście w rozmytą kopię obrazu (pkt 1). Sprawdzić na jasnych obrazach (śnieg, „Guarding Stars”), czy biały tekst jest czytelny; jeśli nie — cień pod literami, nie szara warstwa.

## CAŁA APLIKACJA: szare gradienty i szare tła → rozmyty obraz spod spodu
Właściciel (18:52): „wszędzie — w całej aplikacji poza podglądem kanału live na górze ekranu głównego IPTV — zastąp szare gradienty i szare tła tymi, które pochodzą z policzonych już wcześniej rozmyć obrazów pod spodem”.
- Zakres: każde szare przyciemnienie i szare tło nad obrazem (hero, karty i ich podpisy, panele i okna nad odtwarzaczem, nakładki odtwarzacza, ekran wczytywania, menu boczne itd.). **Wyjątek:** podgląd kanału na górze ekranu głównego Live TV (`skin/niunia/livetv/NiuniaLivePreview.kt` — ekran 11) zostaje z gradientem.
- Sposób: zamiast szarej warstwy — rozmyta kopia obrazu, który leży pod spodem, policzona raz statycznie (jak dziś „tło pod hero”, `fork/images`: rozmycie na procesorze, pamięć Coil), widoczna przez maskę-gradient tam, gdzie był szary gradient, albo jako całe tło panelu (panel = wycinek rozmytego obrazu za nim). Działa na Androidzie 11 (JMGO) bez rozmycia na żywo.
- Do przejścia (tokeny i pliki z szarymi gradientami/tłami w skórce Niuni): `NiuniaGradient.ScrimDetail`, `ScrimDetailFlat`, `ScrimDetailBottom`, `ScrimHero`, `ScrimHeroBottom`, `ScrimSide`, `ScrimPlayer`, `ScrimDim`, `ScrimCaption`, `NiuniaColor.Scrim`, `Scrim30` (`tokens/NiuniaDesignTokens.kt`); pliki: `home/NiuniaTitlePager.kt`, `components/NiuniaHeroPage.kt`, `detail/NiuniaDetail.kt`, `livetv/NiuniaLiveTiles.kt`, `livetv/NiuniaLiveTv.kt`, `components/NiuniaAtoms.kt`, `components/NiuniaDialog.kt`, `components/NiuniaMolecules.kt`, `components/NiuniaOrganisms.kt`, `components/NiuniaPoster.kt`, `nav/NiuniaNav.kt`, `player/NiuniaPlayerParts.kt`, `player/NiuniaPlayerOverlays.kt` (wszystkie w `app/src/main/java/com/nuvio/tv/skin/niunia/`); do tego panele z forka Echo nad odtwarzaczem (jakość, dźwięk — ekrany 05, 06).
- Wydajność: rozmycie liczone raz na obraz i trzymane w pamięci; budżety JMGO z `plan/DECYZJE.md` („Wydajność”). Sprawdzić płynność przewijania rzędów (podpisy kart z rozmyciem) — gdy za ciężkie na kartach, rozmycie raz na kartę przy wczytaniu obrazu.

## CAŁA APLIKACJA: plakaty — dół plakatu przechodzi w rozmycie, nie w czarny gradient — ✅ TV prz04 pkt a (bez czarnego gradientu); pkt b ✅ TV prz06 (log „ForkImages: rozmycie z dysku” po restarcie aplikacji)
Zrzut: `29-plakaty.png` — rzędy „Popular · filmy / seriale”: na każdym plakacie od ~60% wysokości ciemny, prawie czarny gradient pod podpisem („Akcja · ★ 7.2”). Kod: `app/src/main/java/com/nuvio/tv/skin/niunia/components/NiuniaPoster.kt:88-95` (obraz + `BlurLayer` + `Tint`), `:129-165` (`BlurLayer`), `:177-189` (`Tint`), rozmycie `app/src/main/java/com/nuvio/tv/fork/images/NativeBlur.kt:20-40`.
1. **To samo co wyżej dla plakatów: zamiast czarnego gradientu od dołu — gradient wchodzący w rozmycie plakatu; rozmycia liczyć statycznie i zachowywać.**
   Właściciel (18:54): „to samo zrób dla plakatów; miały wyglądać inaczej — jest teraz na każdym czarny gradient od dołu, a ma być gradient wchodzący w rozmycie plakatu. Rozmycia plakatów licz statycznie i zachowuj”.
   Obserwacja: rozmycie dołu plakatu JUŻ jest (`BlurLayer`, 1-2 warstwy, liczone raz procesorem na Androidzie 11), ale przykrywa je `Tint`: dwie plamy koloru dominującego w dolnych rogach + pionowy gradient od 60% (72% dla plakatów z wtopionym tytułem) do koloru „deep” = kolor dominujący × 0,3, czyli prawie czarny. Rozmycia trzymane tylko w pamięci Coil (RAM) — po restarcie aplikacji liczone od nowa.
   Poprawka: (a) usunąć ciemny pionowy gradient z `Tint` (i ocenić plamy w rogach — zostawić tylko, jeśli makieta ich chce); dół plakatu = płynne przejście ostry obraz → rozmyta kopia tego samego plakatu, podpis na rozmyciu (dla czytelności co najwyżej delikatny cień liter); (b) rozmyte plakaty zapisywać na dysku (pamięć dyskowa Coil albo własny katalog, klucz = adres obrazu + promień) i czytać od razu przy kolejnych uruchomieniach; liczyć w tle przy wczytaniu rzędu, nie przy rysowaniu. Sprawdzić płynność przewijania na JMGO.

## Obserwacje przy okazji (nie zgłoszone przez właściciela)
