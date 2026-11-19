# Lektor Gemini — pełne zapytania (stan 20.11, commit po build124)

Lektor „Gemini Cloud” wysyła **dwa rodzaje zapytań**:

1. **Scenariusz** (model tekstowy Gemini, ten sam co AI napisy) — raz na porcję kwestii (pierwsza porcja 40, potem po 150).
   Opisuje sceny, kto mówi, jaka emocja, i skraca za długie kwestie.
2. **Głos** (`gemini-3.8-flash-lite-tts` przez Vertex AI) — osobno dla **każdej kwestii**: tekst + instrukcja stylu.

Do tego pomocniczo:

3. **Skrót za długiej kwestii** (model tekstowy) — gdy gotowe nagranie nie mieści się przed następnym napisem.

Źródła w kodzie: `fork/lektor/LektorScript.kt` (1), `ai-narrator/.../VertexGeminiLektor.kt` + `fork/lektor/Styled.kt` (2), `fork/lektor/Condense.kt` (3).
Klucze API idą w nagłówku `x-goog-api-key` (tu pominięte).

---

## 1. Scenariusz (model tekstowy)

Adres: `POST https://generativelanguage.googleapis.com/v1beta/interactions`
Model: wybrany w ustawieniach AI napisów. Myślenie: `low`.

```json
{
  "model": "<model AI napisów>",
  "system_instruction": "…SYSTEM…",
  "store": false,
  "input": [
    { "type": "text", "text": "…KONTEKST…" },
    { "type": "text", "text": "…ZADANIE…" }
  ],
  "generation_config": { "max_output_tokens": 24000, "thinking_level": "low" },
  "response_format": { "type": "text", "mime_type": "application/json", "schema": { …SCHEMAT… } }
}
```

### SYSTEM (stały)

> Jesteś reżyserem polskiego lektora filmowego. Na podstawie opisu filmu, obsady i pełnego tekstu napisów rozpisujesz kwestie dla lektora: do której sceny należy (krótki opis sceny: miejsce, sytuacja, nastrój), kto mówi (imię postaci z obsady, a gdy nie wiadomo — krótki opis np. „policjant”, „?”), z jaką emocją i jak to podać (2–6 słów, np. „ze złością, podniesionym głosem”, „szeptem, ze strachem”, „ironicznie”). Polski lektor czyta jednym głosem i nie gra ról — emocja ma tylko lekko zabarwić intonację. Jeśli kwestia jest dłuższa niż podany limit znaków, podaj w „t” skrócony tekst lektorski (zwięzłe streszczenie z zachowaniem sensu, imion i pytań, jak robi to lektor); jeśli się mieści — pomiń „t”. Nie dodawaj treści, której nie ma w napisach.

### KONTEKST (z odtwarzacza; cały film, ten sam w każdej porcji)

```
KONTEKST FILMU
Serial: <nazwa serialu, tylko dla odcinków>
Tytuł: <tytuł>
Rok: <rok>
Typ: <movie / series>
Opis: <opis z TMDB/dodatku, do 3000 znaków>
Obsada (aktor jako postać):
- <aktor> jako <postać>
- … (do 30 osób)
PEŁNE NAPISY (numer [czas] tekst):
0 [0:23] <tekst kwestii 0>
1 [0:26] <tekst kwestii 1>
… (wszystkie kwestie filmu, już oczyszczone ze znaczników i opisów dźwięków)
```

### ZADANIE (zmienne, dla każdej porcji)

```
Sceny już opisane (kontynuuj numerację, używaj tych samych numerów dla tych samych scen):
3: nocna kłótnia w kuchni, napięcie
4: …                                    ← tylko od drugiej porcji, do 30 ostatnich scen

Rozpisz te kwestie (numer TAB limit znaków):
120	maks 42 znaków
121	maks 18 znaków
…

Zwróć JSON {"scenes":[{"s":nr,"d":"opis sceny"}],"lines":[{"i":numer,"s":nr sceny,"w":"kto mówi","e":"emocja","t":"tekst skrócony (opcjonalnie)"}]}.
```

Limit znaków = czas do następnego napisu ÷ zmierzone tempo mowy (ok. 65–75 ms na znak).

### SCHEMAT odpowiedzi

```json
{
  "type": "object",
  "properties": {
    "scenes": { "type": "array", "items": { "type": "object",
      "properties": { "s": {"type":"integer"}, "d": {"type":"string"} }, "required": ["s","d"] } },
    "lines": { "type": "array", "items": { "type": "object",
      "properties": { "i": {"type":"integer"}, "s": {"type":"integer"}, "w": {"type":"string"}, "e": {"type":"string"}, "t": {"type":"string"} },
      "required": ["i","s","w","e"] } }
  },
  "required": ["scenes","lines"]
}
```

Wynik zapisywany w `filesDir/lektor-script/<tytuł>.json` (drugi raz ten sam film = bez zapytań).

---

## 2. Głos (Gemini TTS przez Vertex AI) — jedna kwestia

Adres: `POST https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-3.8-flash-lite-tts:generateContent`
Głos: wybrany w Ustawienia → Lektor (domyślnie `Charon`). Do 3 zapytań naraz.

```json
{
  "contents": [ { "role": "user", "parts": [ {
    "text": "<tekst kwestii — skrócony „t” ze scenariusza, jeśli był>",
    "speechMetadata": { "style": "…INSTRUKCJA STYLU…" }
  } ] } ],
  "generationConfig": {
    "responseModalities": ["AUDIO"],
    "speechConfig": {
      "voiceConfig": { "prebuiltVoiceConfig": { "voiceName": "Charon" } },
      "languageCode": "pl-PL"
    }
  }
}
```

### INSTRUKCJA STYLU = podstawa + scena + mówiący + emocja

**Podstawa — styl „klasyczny polski lektor”** (domyślny):

> Klasyczny polski lektor filmowy (jak w polskiej telewizji): jeden spokojny, rzeczowy męski głos dla wszystkich postaci, lekka intonacja zgodna z sensem i nastrojem sceny (pytanie, napięcie, żart), bez aktorskiego grania i bez przesady, wyraźnie, w naturalnym tempie.

**Podstawa — styl „aktorski”** (do wyboru w ustawieniach):

> Polski lektor filmowy czytający z ekspresją aktora: wyraźne emocje zgodne ze sceną (złość, strach, czułość, ironia, szept, krzyk), naturalne tempo, jeden głos dla wszystkich postaci.

**Dopisywane ze scenariusza** (jeśli są):

> ` Scena: <opis sceny>. Mówi: <kto>. Nastrój i sposób podania: <emocja>.`

**Przykład całej instrukcji:**

> Klasyczny polski lektor filmowy (jak w polskiej telewizji): jeden spokojny, rzeczowy męski głos dla wszystkich postaci, lekka intonacja zgodna z sensem i nastrojem sceny (pytanie, napięcie, żart), bez aktorskiego grania i bez przesady, wyraźnie, w naturalnym tempie. Scena: nocna kłótnia w kuchni, para rozmawia podniesionym głosem. Mówi: Tommy (mąż). Nastrój i sposób podania: zirytowany, ostro, z wyrzutem.

Gdy scenariusz jeszcze nie gotowy albo nieudany — sama podstawa.

---

## 3. Za długa kwestia → skrót (od 20.11, zamiast „mów szybciej”)

Gdy nagranie jest dłuższe niż czas do następnego napisu + 0,5 s:

1. zapytanie do modelu tekstowego (jak w p. 1, myślenie `low`, `max_output_tokens` 4000):
   - SYSTEM: *„Jesteś polskim lektorem filmowym. Lektor nie czyta napisów słowo w słowo, tylko je zwięźle streszcza, tak żeby zdążyć przed następną kwestią. Skróć każdą kwestię do podanej maksymalnej liczby znaków, zachowując sens, ton i najważniejsze informacje (imiona, liczby, pytania). Nie dodawaj niczego od siebie, bez komentarzy i cudzysłowów.”*
   - KONTEKST: `Kwestie (numer TAB maks. znaków TAB tekst):` + `0<TAB>35<TAB><tekst>`
   - ZADANIE: `Zwróć JSON {"items":[{"i":numer,"t":"skrócony tekst"}]} dla każdej kwestii.`
2. skrót nagrywany ponownie **z tą samą instrukcją stylu, w zwykłym tempie**;
3. nadal za długi → czytane pierwsze zdania; nadal za długi → mikser czeka na koniec (do 1,2 s), potem płynnie wycisza.

Do 19.11 zamiast tego szło drugie nagranie z dopiskiem „Mów wyraźnie szybciej niż zwykle — cała kwestia musi zmieścić się w X s.” — usunięte (właściciel: brzmiało dziwnie).
