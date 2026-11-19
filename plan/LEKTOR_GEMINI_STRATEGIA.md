# Lektor Gemini przez Google Cloud (Vertex AI) — strategia (ustalona z właścicielem 19.11.2026)

## Decyzje właściciela
- Model głosu: **Gemini 3.8 Flash-Lite TTS** (`gemini-3.8-flash-lite-tts`) przez **Vertex AI** (Google Cloud), NIE przez AI Studio (tam Tier 1 = 100 zapytań/dzień).
- Nagrywanie: **kwestia po kwestii + kontekst sceny** w poleceniu stylu (bez cięcia nagrań scen).
- Styl lektora: **wybór w aplikacji** (klasyczny polski lektor / aktorski).
- Przygotowanie scenariusza: **wybór w aplikacji** (porcjami po 10 min / cały film przed startem).
- Wybór silnika lektora w aplikacji: Chirp 3 HD / Gemini Cloud / Gemini AI Studio / systemowy.

## Sprawdzone 19.11 (test z Maca, klucz `GOOGLE_VERTEX_API_KEY` z `local.properties`)
- `POST https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-3.8-flash-lite-tts:generateContent`, nagłówek `x-goog-api-key` (tryb express, bez projektu w adresie) → HTTP 200 w 1,9 s, `audio/wav` 24 kHz mono 16 bit, `trafficType: ON_DEMAND` (płatność za użycie, bez limitu dziennego AI Studio).
- Polski działa (`speechConfig.languageCode = "pl-PL"`, głos `prebuiltVoiceConfig.voiceName = "Charon"`), styl: `parts[].speechMetadata.style`. Próbka: `plan/probki/gemini-3.8-flash-lite-tts-charon.wav` (5,5 s).
- Zdanie 60 znaków: 36 tokenów wejścia, 138 tokenów audio (~25 tokenów/s).
- Limity modelu: wejście 8192 tokeny, wyjście 16 384 tokeny (~10 min audio).

## Koszt (cennik: wejście 0,50 USD / 1 mln tokenów, wyjście audio 6 USD / 1 mln tokenów)
Film 2 h ≈ 50 min mowy lektora ≈ 800 kwestii:
- audio: 3000 s × 25 = 75 tys. tokenów → **~0,45 USD**
- tekst + kontekst sceny w każdym zapytaniu (~600 tokenów × 800) → **~0,25 USD**
- scenariusz (model tekstowy Gemini: opis, bohaterowie, kto mówi, emocje, skrócony tekst lektorski; pełny kontekst przy każdej porcji, pamięć podręczna promptu) → **~0,1–0,3 USD**
- **Razem ok. 0,8–1 USD za film**, odcinek 45 min ~0,3–0,4 USD. Limit wydatków na tytuł w ustawieniach (jak AI napisy). Nagrania i scenariusz w pamięci podręcznej na dysku (ponowne oglądanie = 0 USD).

## Etap 1 — Scenariusz (model tekstowy Gemini, raz na film/porcję, zapis na dysku)
Wejście (cały kontekst w każdym zapytaniu):
- tytuł, rok, gatunek, opis filmu / odcinka i sezonu (TMDB/metadane aplikacji),
- obsada z nazwami postaci (TMDB credits), główni bohaterowie,
- opisy scen, jeśli są dostępne (np. opis odcinka, napisy SDH z opisami dźwięków i nazwami mówiących — angielskie SDH, gdy są),
- cały tekst napisów PL z czasami (i oryginalny EN, gdy dostępny — pomaga rozpoznać mówiącego).
Wyjście (JSON na kwestię): scena (nr + krótki opis), kto mówi (postać), emocja/sposób podania (krótko, dla polecenia stylu), tekst lektorski (skrócony, jeśli nie mieści się w czasie — jak prawdziwy lektor), pauza przed.
Tryby: porcje po 10 min z wyprzedzeniem (start po ~20–30 s) albo cały film przed startem (1–3 min czekania).

## Etap 2 — Nagranie (Gemini 3.8 Flash-Lite TTS, Vertex AI)
- Każda kwestia osobnym zapytaniem; `speechMetadata.style` = styl lektora (klasyczny/aktorski) + opis sceny + kto mówi + emocja; jeden stały głos lektora (wybór głosu w ustawieniach, domyślnie Charon).
- Długość nagrania sprawdzana z czasem do następnej kwestii; za długie → ponowne nagranie z poleceniem „szybciej” albo krótszy tekst ze scenariusza.
- Pamięć podręczna (klucz: model + głos + styl + tekst), licznik kosztu (tokeny z `usageMetadata`), limit na tytuł.

## Do naprawienia przy okazji (test build122)
- Cisza w trakcie filmu mimo nagranych kwestii (61 OK): podejrzenie — sklejanie napisów <0,3 s w długie kwestie (843 → 298) + ucinanie za długich do pierwszych zdań + mikser odcinający starszą kwestię. Dodać log miksera (która kwestia gra, ile, czy ucięta) i nie sklejać napisów różnych osób.
