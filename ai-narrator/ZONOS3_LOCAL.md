# ZONOS3-Mini ARM32: automatyczne pobieranie wewnątrz NuvioTV

**Stan: implementacja instalatora i osobnego projektu ARM32; NIE MA jeszcze zbudowanego ani opublikowanego binarnego wydania.** Synteza lokalna nie została uruchomiona na urządzeniu. Nie mylić implementacji mechanizmu pobierania z dostarczeniem silnika.

## Proces dla użytkownika

1. Użytkownik włącza **Lektor AI → ZONOS3 (lokalnie)**. NuvioTV sprawdza obecność podpisanego dodatkowego pakietu **armeabi-v7a**, odrębnego od podstawowego APK.
2. Jeśli pakietu brakuje, aplikacja **automatycznie pobiera go z Twojego GitHub Releases** (po skonfigurowaniu adresu wydania). Pasek postępu działa w panelu. Sprawdzana jest suma SHA-256 i podpis: pakiet musi być podpisany tym samym kluczem co NuvioTV.
3. Android **wymaga zatwierdzenia instalacji** osobnego APK. Aplikacja otwiera systemowy instalator przyciskiem; instalacji nie można wykonać po cichu.
4. Po instalacji i weryfikacji aplikacja automatycznie pobiera model GGUF (~1,12 GB) z Hugging Face, z wznawianiem i SHA-256. Model nie jest częścią żadnego APK.
5. Po wstrzymaniu filmu użytkownik przygotowuje pierwszą albo drugą połowę z pełnej osi czasowej napisów. Audio jest zapisywane w trwałej pamięci. W trakcie projekcji nie następuje synteza.
6. Gdy już istnieją WAV, ponowne przygotowanie pomija nagrania obecne na dysku. Ręczne usuwanie audio wymaga potwierdzenia.

## Skąd plik ARM32

Źródła i eksperymentalny skrypt: `ai-narrator/worker-arm32/`, `ai-narrator/tools/build_android_zonos3.sh`. Skrypt używa przypiętego commita Zyphra i NDK dla `armeabi-v7a`, produkując oddzielny **podpisany APK** `com.nuvio.tv.zonos32`. Bazowe APK nie zawiera silnika ani wag.

Workflow `.github/workflows/zonos32-companion-release.yml` umożliwia zbudowanie pliku, zapisanie artefaktu do testów i opcjonalne wypchnięcie podpisanego APK na publiczne GitHub Releases z konta `stefankot`. Wymaga istniejącego **publicznego** repozytorium dla dystrybucji, `ZONOS_PUBLIC_RELEASE_TOKEN` uprawnionego do publikacji oraz sekretów podpisu `AUTOSYNC_KEYSTORE_BASE64`, `AUTOSYNC_STORE_PASSWORD`, `AUTOSYNC_KEY_ALIAS`, `AUTOSYNC_KEY_PASSWORD`. Nie publikować przed sprawdzeniem zgodności ELF/PIE, głosu i RAM na urządzeniach.

Repozytorium `stefankot/nuvio-tv-fork` jest **prywatne**. Jego release assets nie będą anonimowo dostępne dla aplikacji na TV bez dodatkowego uwierzytelnienia. Nie wdrożono wprowadzania tokena GitHub na telewizorze. Potrzebne jest osobne publiczne repozytorium w tym samym koncie albo świadoma decyzja o publicznym wydaniu.

Po opublikowaniu i pomiarze SHA-256 zbudowanego APK należy ustawić w `local.dev.properties` przed budową NuvioTV dwa klucze:
- `ZONOS_ARM32_APK_URL` — rzeczywisty adres `https://github.com/stefankot/<repo-publiczne>/releases/download/<tag>/zonos3-worker-armeabi-v7a.apk`.
- `ZONOS_ARM32_APK_SHA256` — dokładna suma SHA-256 tego APK.

Po uzupełnieniu tych wartości aplikacja sama pobiera wydanie po aktywacji funkcji; użytkownik nie wpisuje adresu serwera. Brak któregokolwiek parametru powoduje komunikat **Brak opublikowanego pakietu ARM32** i zakaz pobierania z nieznanych źródeł.

## Bezpieczeństwo i architektura

- NuvioTV weryfikuje APK przez SHA-256 oraz rzeczywiste certyfikaty podpisu pakietu. Sprawdza też podpis zainstalowanej aplikacji `com.nuvio.tv.zonos32`.
- Komponent ma eksportowaną usługę chronioną **uprawnieniem signature**, więc inne aplikacje nie mogą jej uruchamiać.
- Model jest przekazywany z NuvioTV do komponentu przez czasowo udostępnione `content://` URIs z uprawnieniem odczytu; komponent kopiuje i sprawdza GGUF w swojej prywatnej pamięci. Uwaga: wymaga drugiej kopii ~1,12 GB na dysku.
- Natywny proces pracuje tylko na `127.0.0.1`, z losowym tokenem sesyjnym i limitami RAM. Wbudowany strażnik zatrzymuje proces przy małej pamięci lub długiej sesji.
- **Nigdy** nie uruchamia się pobranego pliku ELF przez `exec` z `filesDir`; natywny plik znajduje się w katalogu zarządzanym przez Android Package Manager. Nie jest dołączany do głównego NuvioTV APK.
- Jeśli Android nie zgłasza ABI `armeabi-v7a`, aplikacja blokuje instalację. Sama nominalna wielkość RAM nie dowodzi zgodności architektury.

## Pamięć i przygotowanie

Model + dekoder: **1 115 476 064 bajty**. Kopia w Nuvio + kopia w komponencie to około **2,23 GB** na dysku, nie licząc gotowych WAV (limit 2 GiB i rezerwa 500 MiB). Wymagane progi wolnej pamięci RAM przed przygotowaniem: 1 250 MiB (klasa 2 GB), 1 500 MiB (3 GB), 1 750 MiB (4 GB). To tylko wstępna, konserwatywna polityka; nie ma pomiarów szczytowego zużycia procesora ARM32.

Przygotowanie ma limit **15 minut na sesję**, ale nie potwierdzono możliwości wygenerowania połowy filmu w tym czasie. Sesję można wznowić bez utraty gotowych nagrań. Model uruchamiany jest tylko przy wstrzymanym filmie; sam dekoder obrazu może wówczas nadal zajmować RAM.

**Funkcja aktualnie czyta istniejące polskie napisy — nie tłumaczy obcych dialogów.** Pełne napisy SRT/VTT nie zawsze są dostępne przez obecny adapter odtwarzacza. Jeśli nie ma pełnej osi czasowej, przygotowanie jest zablokowane.

## Warunki odbioru i brakujące elementy

BRAK DANYCH: poprawna kompilacja C++ ARM32 z upstream, instalacja podpisanego APK, uruchomienie serwera na realnym Android TV, zgodność ABI Xiaomi/Onn/projektora, jakość polskiej wymowy, szczytowa RAM, realny czas generacji, obsługa pełnych ścieżek napisów. Dotychczasowe GitHub Actions kończyły się błędami bez dowodu poprawnej kompilacji.

Źródła: https://github.com/Zyphra/ZONOS3-Mini/tree/main/servers/ggml i https://huggingface.co/mlx-community/ZONOS3-Mini/blob/main/conversion.json (model revision i SHA-256).
