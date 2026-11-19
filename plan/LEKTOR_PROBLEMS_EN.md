# Voice-over narrator ("lektor") module — known problems (19.11.2026, build122)

The module reads Polish subtitles aloud over the film (classic Polish single-voice "lektor"), mixing synthesized speech into the player's audio. Code: `app/src/main/java/com/nuvio/tv/fork/lektor/` (controller, planner, mixer, scenes, tempo, condense) and `ai-narrator/src/main/kotlin/com/nuvio/tv/ainarrator/` (TTS engines: Chirp, Gemini, system). Test device: JMGO projector (Android 11, 32-bit armeabi-v7a, 4 slow cores).

## 1. Voice quality (main complaint, unresolved)
- **Google Cloud Chirp 3 HD (default engine) sounds flat and monotone**: the same intonation for every line, no emotion. Chirp 3 HD accepts no SSML and no style/emotion instructions; only `[pause]` markup tags and speaking rate. Longer context (whole scenes in one request) was expected to help prosody; not confirmed.
- **No speaker awareness**: one voice for all characters, the engine does not know who speaks, the mood of the scene, the genre or the plot.
- **Gemini TTS** (supports style prompts, so emotions are possible) is blocked by account quota: Tier 1 = 10 requests/min and 100 requests/day for TTS models; HTTP 429 after ~15 minutes of film. Tier 2 needs $100 paid + 3 days, or an approved quota-increase form (not sent yet).
- **Planned next step (owner's request):** two Google Cloud TTS voice types that support SSML (Neural2 / WaveNet / Standard; Studio has partial SSML; check which exist for pl-PL), with Gemini (text model) converting each scene to SSML (`<prosody>`, `<emphasis>`, `<break>`) using the full film context (title, plot, characters/cast, the whole subtitle text), plus an engine switch in settings.

## 2. Lines cut off when there is a lot of text (fixed in code, not yet confirmed on device)
- A line whose speech is longer than the time until the next subtitle was cut off by the next line (the mixer lets the newer line win, so the voice never drifts behind the picture).
- Lines synthesized as part of a scene were never time-fitted, and the time-fitting for single lines allowed 1.5 s of overlap into the next line.
- Fixes: every line is fitted to the time before the next one (speed up to 1.3×, then cut at a sentence boundary, no overlap); overly long lines are condensed by Gemini (text model) before synthesis, like a human Polish lektor who summarizes rather than reads verbatim. Untested.

## 3. Scene synthesis (one request per scene, then cut into lines)
- A scene is sent as one Chirp request (lines separated by `[pause long]`), and the recording is cut back into lines at the longest silences (`SilenceSplit`). The model may ignore pause tags, so cuts can land in the wrong place; then the scene is retried in smaller pieces and finally line by line.
- **Too-large scenes broke the narrator (build121):** a 53-line scene (~4700 bytes, the Cloud TTS limit is 5000 bytes/request) produced a WAV > 8 MB, which the on-disk cache rejected ("Invalid WAV cache payload"), aborting the scene; the request took 68 s and the ~15 MB base64 response caused GC pressure and video stutter on the 32-bit TV. Scenes are now capped at ~1500 bytes (~60–90 s of speech). Untested.
- One request mutex paces all Chirp calls (200 requests/min quota), so a long scene request blocks single-line requests behind it.

## 4. Error recovery (fixed in code, untested)
- After any synthesis error the loop stopped attempting synthesis at all (it waited for a success that could never come), so the narrator went silent for the rest of the film. Now it retries one line per cycle with back-off, and a failed scene is split into smaller scenes, then single lines.

## 5. No pauses between different speakers (fixed in code, untested)
- Dialogue subtitles ("- Line A / - Line B") had their dashes removed and lines joined without punctuation, so two people's lines were read as one long sentence. A full stop is now inserted before a dash line. Subtitles closer than 300 ms are still merged into one line (needed for timing), which can also join two speakers.

## 6. Audio pipeline
- Dolby/E-AC-3 passthrough bypasses Android audio processors, so the narrator forces PCM decoding while it is on. Switching re-initialises the audio track: the first seconds after enabling the narrator stutter (AudioTrack underruns), and decoding 5.1 E-AC-3 in software is heavy for the JMGO.
- Switching the decode path empties the buffer, which previously triggered Echo's automatic quality downgrade and a source switch (fixed: the narrator cancels those decisions for a few seconds).
- Earlier mixer bugs (per-sample search over all lines, millisecond timing → crackling) are fixed.

## 7. Text sources and timing
- The narrator needs Polish text: addon subtitles, an embedded Polish track, or AI translation. Live AI translation of embedded English subtitles only ran ~50 s ahead (player buffer); a new MKV reader now fetches subtitle text ~10 minutes ahead (untested).
- Lines are read from the cues currently on screen (after AutoSync shifts them); an earlier version read them a minute early.

## 8. Logging
- All errors are logged with type and message (no dialogue text, no keys). Log tags: `ForkLektor`, `ForkAiMkv`, `ForkAiLive`.

## Status summary
| Problem | State |
|---|---|
| Monotone, emotionless voice | Open — needs a different engine (SSML voices + Gemini interpretation, or Gemini TTS Tier 2) |
| Lines cut off | Fixed in build122, untested |
| "Invalid WAV payload", stutter with big scenes | Fixed in build122, untested |
| Narrator never recovers after an error | Fixed in build122, untested |
| No pause between speakers | Fixed in build122, untested |
| Stutter right after enabling (PCM switch) | Open (inherent to forcing PCM; may be reduced but not removed) |
| Gemini TTS quota (100/day) | Open — owner decision (Tier 2) |
