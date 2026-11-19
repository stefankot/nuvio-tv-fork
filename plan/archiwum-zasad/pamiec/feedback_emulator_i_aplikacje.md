---
name: feedback-emulator-i-aplikacje
description: Zasady emulatora Android: tylko fork com.nuviodebug.com, emulator tylko na testy, nie dotykać cudzych aplikacji
metadata:
  type: feedback
---

Uruchamiaj wyłącznie fork `com.nuviodebug.com` (applicationId z `app/build.gradle.kts:369`, NIE `com.nuvio.tv`). Przed testem sprawdź skrót APK na urządzeniu (`adb pull`, `md5`) względem pliku z buildu. Emulator uruchamiaj tylko na testy i zamykaj po (`adb emu kill`); nigdy razem z Gradle (8 GB RAM).

**Why:** na emulatorze `gtv` były cudze aplikacje z danymi konta; przypadkiem uruchomiłem starą i dodałem pozycję do biblioteki. Użytkownik kazał skasować trzy cudze pakiety (zrobione).

**How to apply:** `pm list packages | grep nuvio` przed startem; start przez `am start -n com.nuviodebug.com/com.nuvio.tv.launcher.AppIconDefault`. Logowanie QR nie działa (Supabase = localhost) — używaj "Continue without account". Pomocnik: scratchpad `lib.sh` (txt/key/tapdesc). Patrz [[feedback-autonomia-bez-czekania]].
