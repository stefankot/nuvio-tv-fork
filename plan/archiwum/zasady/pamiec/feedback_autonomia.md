---
name: feedback-autonomia-bez-czekania
description: "Użytkownik wymaga pracy w pełni autonomicznej - bez pytań, bez zatrzymywania się, bez czekania na odpowiedź"
metadata:
  node_type: memory
  type: feedback
  originSessionId: d8643607-81cc-4d89-a978-f18cce929dff
  modified: 2026-11-16T19:52:51.351Z
---

Nigdy nie czekaj na użytkownika ani nie zatrzymuj się z pytaniem w trakcie "kontynuuj". Pracuj autonomicznie do końca listy kroków. Użytkownik wybuchł (powtarza się "setny raz").

**Why:** w CLAUDE.md jest to już zapisane ("Nie pytaj o potwierdzenie ani o kontynuację"); mimo to kończyłem tury pytaniami i przerwami.

**How to apply:** nie używaj AskUserQuestion ani końcowych pytań w wrap_up z `blocked`; przy niejasności wybierz rozsądną opcję, napisz ją w jednym zdaniu i działaj dalej. Wyjątek: czynności nieodwracalne na cudzych danych (odinstalowanie nie-swoich aplikacji, kasowanie danych) tylko na wyraźne polecenie. Testuj tekstem (uiautomator dump), bez zrzutów ekranu; wygląd pomijaj. Uruchamiaj wyłącznie `com.nuviodebug.com` (fork), emulator tylko na testy. Patrz [[feedback-emulator-i-aplikacje]].
