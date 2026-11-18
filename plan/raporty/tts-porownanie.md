# Lektor: porównanie głosów w chmurze (18.11.2026)

Po co: na JMGO nie ma polskiego głosu w systemie, a Piper odrzucony (brzmi jak robot). Te usługi czytają tekst w internecie i odsyłają nagranie.

**Ile tekstu ma jeden film:** napisy filmu to ok. 60–90 tys. znaków. Serial (odcinek 45 min) ok. 30 tys.

| Usługa | Polskie głosy | Za darmo co miesiąc | Potem | Ile filmów za darmo |
|---|---|---|---|---|
| Google Cloud TTS, WaveNet | kilka pl-PL | 1 mln znaków (4 mln głosy „Standard”) | ok. 4 USD za 1 mln znaków | ok. 12 |
| Google Cloud TTS, Chirp 3 HD (najnowsze, najbardziej naturalne) | pl-PL (wiele głosów) | 1 mln znaków | 30 USD za 1 mln (ok. 2,5 USD za film) | ok. 12 |
| Microsoft Azure Speech (neural) | Agnieszka, Marek, Zofia | 0,5 mln znaków (plan F0) | ok. 15 USD za 1 mln | ok. 6 |
| OpenAI tts-1 / gpt-4o-mini-tts | głosy wielojęzyczne (mówią po polsku z lekkim obcym akcentem) | brak stałego darmowego limitu | ok. 15 USD za 1 mln znaków (ok. 1 USD za film) | 0 |
| ElevenLabs | bardzo naturalny polski | mały | najdroższy | 1–2 |

**Warunki:**
- Google i Microsoft: użycie prywatne w aplikacji na własnym telewizorze jest dozwolone; trzeba założyć konto z kartą (darmowy limit odnawia się co miesiąc). Microsoft: darmowy plan F0 nie daje praw komercyjnych, ale do prywatnego oglądania to bez znaczenia.
- OpenAI: zasady wymagają informacji, że głos jest wygenerowany przez AI (w aplikacji wystarczy napis „Lektor AI”).
- Klucz API wpisuje właściciel w ustawieniach (nie ja).
- Opóźnienie: zdanie wraca w ok. 0,2–1 s; aplikacja czyta z wyprzedzeniem, więc nie będzie słychać opóźnienia. Wymaga internetu.

**Próbki do odsłuchu (bez konta):**
- Google: https://cloud.google.com/text-to-speech (pole „Try it”, wybierz Polish, typ Chirp 3 HD i WaveNet)
- Microsoft: https://speech.microsoft.com/portal/voicegallery (Polish: Agnieszka, Marek, Zofia)
- OpenAI: https://www.openai.fm (wpisz polskie zdanie)

Próbki zrobione przeze mnie z tego samego zdania wymagają klucza API (patrz wyżej) albo dopiero po Twojej decyzji.

**Moja rekomendacja:** Google Chirp 3 HD — najbardziej naturalny polski z dużym darmowym limitem (ok. 12 filmów miesięcznie); gdy limit się skończy, przełączyć na WaveNet (4 razy taniej). Najpierw jednak test systemowego głosu na Xiaomi — jeśli brzmi dobrze, chmura niepotrzebna.
