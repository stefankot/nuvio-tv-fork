# B11-4: tłumaczenie napisów bez chmury (17.11.2026)

**Wynik: kryterium (zrozumiały polski, poniżej 2 min na film) NIEZWERYFIKOWANE pomiarem; kod NIE dodany.** Raport oparty na źródłach publicznych, nie na teście na urządzeniu.

## Opcje
1. **ML Kit Translate** (Google, na urządzeniu): modele z tej samej rodziny co tryb offline Tłumacza, para angielski→polski bezpośrednia, ok. 30 MB na język (liczba z opisu opakowania, nie z oficjalnej dokumentacji). Pobranie modelu i działanie wymagają Usług Google Play; na części telewizorów/boxów ich brak = opcja odpada. Najmniejszy koszt integracji (jedna zależność).
2. **OPUS-MT przez CTranslate2 (INT8)**: ok. 75–80 MB na parę (szacunek z konwersji podobnych par), licencja CC-BY 4.0; ok. 100 ms na zdanie na CPU (inny sprzęt niż telewizor). Wymaga własnej natywnej biblioteki i tokenizera (SentencePiece) w aplikacji: duża praca.
3. **Bergamot (Firefox)**: 21–35 MB na parę, szybki na CPU; integracja natywna, brak gotowej biblioteki Androida w forku.

## Szacunek czasu
Film ≈ 1500 linii napisów. Przy 100 ms na linię = ok. 150 s sekwencyjnie (powyżej 2 min), ale tłumaczenie można prowadzić z wyprzedzeniem w tle, wsadami i równolegle, więc przy odtwarzaniu liczy się tylko opóźnienie pierwszych linii. Prawdziwy czas na telewizorze (słaby CPU) NIE zmierzony.

## Rekomendacja
Jeśli telewizor właściciela ma Usługi Google Play: próba ML Kit jako moduł wyłączony domyślnie (wymaga zgody na dodanie zależności i pomiaru na urządzeniu). Bez GMS: nie opłaca się. Do ustalenia z właścicielem, nie przesądzam.
Źródła: https://developers.google.com/ml-kit/language/translation , https://huggingface.co/beshkenadze/murmur-translation-ct2 , https://qvac.tether.io/?p=75
