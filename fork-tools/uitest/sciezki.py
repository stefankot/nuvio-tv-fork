#!/usr/bin/env python3
"""Ścieżki użytkownika UX/UI (22.11.2026) — uiautomator + klawisze pilota, bez zrzutów ekranu.

Użycie:  DEV=emulator-5554 python3 -I fork-tools/uitest/sciezki.py [--cykl N] [numery ścieżek ...]
Wynik:   plan/ux/cykl<N>/sciezki.md (+ sciezki.json, logcat.txt).

Każda ścieżka = ciąg kroków (trasa, klawisze, OK, DŁUGIE OK, Wstecz) z oceną:
  OK     — ekran/fokus zareagował jak trzeba,
  UWAGA  — działa, ale nieoptymalnie (wolno, fokus nie wrócił, brak akcji długiego OK…),
  BŁĄD   — brak reakcji, wyjście z aplikacji, awaria, ANR.
Reakcja = zmiana zestawu tekstów ekranu albo fokusu. Długie OK: input keyevent --longpress (flaga systemowa długiego naciśnięcia).
"""
import datetime, json, os, re, subprocess, sys, time
import xml.etree.ElementTree as ET

ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
DEV = os.environ.get("DEV", "emulator-5554")
PKG = "com.nuviodebug.com"
ACT = PKG + "/com.nuvio.tv.MainActivity"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SLOW = 5.0  # s: dłużej = UWAGA (właściciel 19.11: > 5 s = błąd ładowania)


def adb(*a, timeout=30):
    try:
        return subprocess.run([ADB, "-s", DEV] + list(a), capture_output=True, text=True, timeout=timeout).stdout
    except subprocess.TimeoutExpired:
        return ""


def key(k, n=1, pause=0.45):
    code = k if k.startswith("KEYCODE_") else ("KEYCODE_DPAD_" + k if k in ("UP", "DOWN", "LEFT", "RIGHT", "CENTER") else "KEYCODE_" + k)
    for _ in range(n):
        adb("shell", "input", "keyevent", code)
        time.sleep(pause)


def hold_ok():
    # API 34 bez --duration; emulator bez roota (sendevent niedostępny). --longpress = wciśnięcie + powtórzenie z flagą
    # FLAG_LONG_PRESS (jak pilot po ~0,5 s trzymania) + puszczenie, bez realnego czekania.
    adb("shell", "input", "keyevent", "--longpress", "KEYCODE_DPAD_CENTER")


def dump():
    for _ in range(3):
        x = adb("exec-out", "uiautomator", "dump", "--compressed", "/dev/tty")
        i, j = x.find("<?xml"), x.rfind("</hierarchy>")
        if i >= 0 and j > i:
            try:
                return ET.fromstring(x[i:j + 12])
            except ET.ParseError:
                pass
        time.sleep(0.5)
    return None


def bounds(n):
    m = list(map(int, re.findall(r"\d+", n.get("bounds", ""))))
    return tuple(m) if len(m) == 4 else (0, 0, 0, 0)


class Screen:
    def __init__(self, root):
        self.root = root
        self.texts, self.focus, self.fb, self.pkg = [], None, None, ""
        if root is None:
            return
        pk = {}
        for n in root.iter("node"):
            pk[n.get("package")] = pk.get(n.get("package"), 0) + 1
            t = (n.get("text") or n.get("content-desc") or "").strip()
            if t and n.get("package") == PKG:
                self.texts.append(t)
        self.pkg = max(pk, key=pk.get) if pk else ""
        foc = [n for n in root.iter("node") if n.get("focused") == "true"]
        if foc:
            f = foc[-1]
            self.fb = bounds(f)
            inner = []
            for n in root.iter("node"):
                b = bounds(n)
                t = (n.get("text") or n.get("content-desc") or "").strip()
                if t and b[0] >= self.fb[0] and b[1] >= self.fb[1] and b[2] <= self.fb[2] and b[3] <= self.fb[3] and t not in inner:
                    inner.append(t)
            self.focus = " | ".join(inner)[:90] or "[bez nazwy %dx%d @%d,%d]" % (self.fb[2] - self.fb[0], self.fb[3] - self.fb[1], self.fb[0], self.fb[1])

    def sig(self):
        return frozenset(self.texts)

    def has(self, *words):
        low = " ".join(self.texts).lower()
        return any(w.lower() in low for w in words)


def screen():
    return Screen(dump())


def settle(maxs=10.0):
    """Czeka, aż ekran się ustabilizuje (dwa kolejne odczyty te same). -> (Screen, sekundy)."""
    t0, prev = time.time(), None
    while time.time() - t0 < maxs:
        s = screen()
        cur = (s.focus, s.sig())
        if prev is not None and cur == prev and s.texts:
            return s, time.time() - t0
        prev = cur
        time.sleep(0.4)
    return screen(), time.time() - t0


def changed(a, b):
    if a.focus != b.focus:
        return True
    sa, sb = a.sig(), b.sig()
    return len(sa ^ sb) > max(2, len(sa) // 10)


def alive():
    return adb("shell", "pidof", PKG).strip() != ""


def niunia_log():
    return adb("logcat", "-d", "-s", "ForkNiunia:I", "-v", "brief")


class Path:
    def __init__(self, nr, name):
        self.nr, self.name, self.steps, self.verdict, self.notes, self.t0 = nr, name, [], "OK", [], time.time()

    def note(self, level, msg):
        order = {"OK": 0, "UWAGA": 1, "BŁĄD": 2}
        if order[level] > order[self.verdict]:
            self.verdict = level
        self.notes.append(f"{level}: {msg}")

    def step(self, what, s):
        head = " / ".join(t[:30] for t in s.texts[:3])
        self.steps.append(f"{what} → fokus: {s.focus} ⟨{head}⟩")


def open_route(p, route, expect=None):
    adb("logcat", "-c")
    t0 = time.time()
    adb("shell", "am", "start", "-a", "com.nuvio.fork.OPEN", "-n", ACT, "--es", "route", route)
    s, dt = settle(12)
    dt = time.time() - t0
    p.step(f"trasa {route} ({dt:.1f} s)", s)
    if s.pkg != PKG:
        p.note("BŁĄD", f"{route}: na wierzchu {s.pkg}")
    elif dt > SLOW:
        p.note("UWAGA", f"{route}: ładowanie {dt:.1f} s")
    if s.focus is None:
        p.note("UWAGA", f"{route}: brak fokusu po otwarciu")
    if expect and not s.has(*expect):
        p.note("UWAGA", f"{route}: brak tekstu {expect}")
    return s


def toast_shown():
    """Komunikat Toast (osobne okno — uiautomator go nie widzi): ślad okna w logu systemu."""
    return "u0 Toast}" in adb("logcat", "-d", "-s", "CoreBackPreview:D", "NotificationService:W")


def ok(p, what="OK", expect_change=True):
    a = screen()
    adb("logcat", "-c")
    key("CENTER")
    b, dt = settle(8)
    toast = toast_shown()
    p.step(f"{what} na „{a.focus}” ({dt:.1f} s{', komunikat' if toast else ''})", b)
    if not alive():
        p.note("BŁĄD", f"{what}: aplikacja zamknięta")
    elif expect_change and not changed(a, b) and not toast:
        p.note("BŁĄD", f"{what} na „{a.focus}”: brak reakcji")
    return a, b


def long_ok(p, what="długie OK", expect=True):
    a = screen()
    adb("logcat", "-c")
    hold_ok()
    b, dt = settle(8)
    log = niunia_log()
    if toast_shown():
        log += "\nkomunikat"
    fired = "długie OK" in log
    short = re.search(r"ForkNiunia: OK → (?!.*pominięte)", log) is not None
    kind = "długie" if fired else ("jak krótkie OK" if short else "brak zdarzenia Niuni")
    p.step(f"{what} na „{a.focus}” ({kind})", b)
    if not alive():
        p.note("BŁĄD", f"{what}: aplikacja zamknięta")
    elif expect and short and not fired:
        p.note("BŁĄD", f"{what} na „{a.focus}”: zadziałało jak zwykłe OK (brak akcji długiego OK)")
    elif expect and not changed(a, b) and "komunikat" not in log:
        p.note("BŁĄD", f"{what} na „{a.focus}”: brak akcji")
    elif not expect and changed(a, b) and not fired:
        p.note("UWAGA", f"{what} na „{a.focus}”: zachowało się jak zwykłe OK")
    return a, b


def back(p, before=None, what="Wstecz"):
    key("BACK", pause=0.2)
    b, dt = settle(8)
    p.step(f"{what} ({dt:.1f} s)", b)
    if not alive() or b.pkg != PKG:
        p.note("BŁĄD", f"{what}: wyjście z aplikacji ({b.pkg})")
    elif before is not None and before.focus and b.focus != before.focus:
        p.note("UWAGA", f"{what}: fokus „{b.focus}” zamiast „{before.focus}”")
    return b


def move(p, d, n=1):
    key(d, n)
    s = screen()
    p.step(f"{d}×{n}", s)
    return s


def is_poster(s):
    if not s.fb:
        return False
    w, h = s.fb[2] - s.fb[0], s.fb[3] - s.fb[1]
    return h > w * 1.25 and w > 120


def seek(p, pred, d="DOWN", maxn=7, what="plakat"):
    """Strzałka d aż pred(ekran) — np. fokus na plakacie. -> Screen albo None."""
    for i in range(maxn):
        key(d, pause=0.6)
        s = screen()
        if pred(s):
            p.step(f"{d}×{i + 1} → {what}", s)
            return s
    p.note("UWAGA", f"nie znaleziono: {what} ({d}×{maxn})")
    return None


def youtube_live(p):
    """YouTube bez logowania: zakładka „Live” ma filmy (Home jest pusty)."""
    open_route(p, "youtube")
    s = screen()
    for i in range(8):
        if s.focus and s.focus.startswith("Live"):
            break
        key("RIGHT", pause=0.5)
        s = screen()
    key("CENTER", pause=0.3)
    s, _ = settle(10)
    p.step("zakładka Live", s)
    return seek(p, lambda x: x.focus not in (None, "Live") and "Live" not in str(x.focus)[:5], "DOWN", 3, "film")


def run_path(fn, nr, name):
    p = Path(nr, name)
    try:
        fn(p)
    except Exception as e:  # noqa — skrypt testowy: błąd ścieżki zapisany w raporcie
        p.note("BŁĄD", f"wyjątek skryptu {type(e).__name__}: {e}")
    if not alive():
        p.note("BŁĄD", "aplikacja nie działa po ścieżce — restart")
        adb("shell", "am", "start", "-n", ACT)
        time.sleep(8)
    crash = adb("logcat", "-d", "-b", "crash")
    if "FATAL EXCEPTION" in crash and PKG in crash:
        p.note("BŁĄD", "awaria: " + " / ".join(l.strip() for l in crash.splitlines() if "Exception" in l)[:300])
        adb("logcat", "-b", "crash", "-c")
    p.secs = time.time() - p.t0
    return p


# ------------------------------------------------------------------ ścieżki
P = {}


def path(nr, name):
    def deco(fn):
        P[nr] = (name, fn)
        return fn
    return deco


def home_rows(p):
    s = open_route(p, "home")
    return s


@path(1, "Start aplikacji → ekran główny z fokusem")
def _(p):
    adb("shell", "am", "force-stop", PKG)
    t0 = time.time()
    adb("shell", "am", "start", "-n", ACT)
    s, _ = settle(25)
    dt = time.time() - t0
    p.step(f"start ({dt:.1f} s)", s)
    if dt > 12:
        p.note("UWAGA", f"zimny start {dt:.1f} s")
    if s.focus is None:
        p.note("BŁĄD", "po starcie brak fokusu")


@path(2, "Główna: OK „Odtwórz” w banerze")
def _(p):
    s = home_rows(p)
    a, b = ok(p, "OK na banerze")
    back(p, a)


@path(3, "Główna: długie OK w banerze → strona tytułu")
def _(p):
    home_rows(p)
    a, b = long_ok(p)
    back(p, a)


@path(4, "Główna: ▼ rząd, ►×4, OK → tytuł, Wstecz → ten sam plakat")
def _(p):
    home_rows(p)
    move(p, "DOWN")
    move(p, "RIGHT", 4)
    a, b = ok(p)
    back(p, a)


@path(5, "Główna: długie OK na plakacie → menu → „Do biblioteki”")
def _(p):
    home_rows(p)
    if not seek(p, is_poster):
        return
    move(p, "RIGHT", 2)
    a, b = long_ok(p)
    if not b.has("Akcje tytułu", "Title actions"):
        p.note("BŁĄD", "długie OK na plakacie: brak menu „Akcje tytułu”")
        return
    move(p, "DOWN")
    s = screen()
    if s.focus and ("bibliotek" in s.focus.lower() or "library" in s.focus.lower() or "list" in s.focus.lower()):
        ok(p, "OK „Do biblioteki”")
    else:
        p.note("UWAGA", f"menu: drugi przycisk „{s.focus}”")
        back(p)


@path(6, "Główna: długie OK na „Oglądaj dalej”")
def _(p):
    s = home_rows(p)
    move(p, "DOWN")
    s = screen()
    if not s.has("Oglądaj dalej", "Continue"):
        p.note("UWAGA", "brak rzędu „Oglądaj dalej” — pominięte")
        return
    a, b = long_ok(p)
    if changed(a, b):
        back(p, a, "Wstecz z menu")


@path(7, "Główna: szybkie przewijanie ▼×8 i ▲×8 — fokus nie ginie")
def _(p):
    home_rows(p)
    for d in ("DOWN", "UP"):
        key(d, 8, pause=0.15)
        s, dt = settle(6)
        p.step(f"{d}×8 szybko", s)
        if s.focus is None:
            p.note("BŁĄD", f"po {d}×8 brak fokusu")


@path(8, "Główna: ◄ do menu bocznego i z powrotem")
def _(p):
    home_rows(p)
    move(p, "DOWN")
    a = screen()
    move(p, "LEFT", 6)
    s = move(p, "RIGHT")
    if s.focus is None:
        p.note("BŁĄD", "po powrocie z menu brak fokusu")


@path(9, "Menu boczne: każda pozycja otwiera ekran")
def _(p):
    home_rows(p)
    key("BACK", pause=0.6)
    s = screen()
    p.step("Wstecz → menu", s)
    seen = []
    for i in range(10):
        s = screen()
        if s.focus in seen:
            break
        seen.append(s.focus)
        key("DOWN", pause=0.5)
    p.step("pozycje menu: " + " / ".join(str(x) for x in seen), screen())
    if len(seen) < 4:
        p.note("UWAGA", f"menu: tylko {len(seen)} pozycji osiągalnych")


@path(10, "Główna: Wstecz z rzędu → baner/menu, drugi Wstecz → nie zamyka bez pytania")
def _(p):
    home_rows(p)
    move(p, "DOWN", 2)
    back(p)
    b = back(p)
    b2 = back(p)
    if b2.pkg != PKG:
        p.note("BŁĄD", "trzy razy Wstecz zamyka aplikację bez pytania")


def search_type(p, word):
    s = screen()
    # Pole tekstowe (gdy jest) — wpis przez adb, inaczej klawiatura ekranowa (klawisz.py).
    adb("shell", "input", "text", word)
    time.sleep(2.5)
    s2, _ = settle(8)
    p.step(f"wpis „{word}”", s2)
    return s2


def type_osk(p, word):
    """Klawiatura ekranowa Niuni (bez pola tekstowego): fokus do klawisza i OK (fork-tools/test/klawisz.py)."""
    env = dict(os.environ, ANDROID_SERIAL=DEV)
    r = subprocess.run([sys.executable, "-I", os.path.join(ROOT, "fork-tools", "test", "klawisz.py"), word], capture_output=True, text=True, env=env, timeout=240)
    s, _ = settle(8)
    p.step(f"klawiatura „{word}”: {r.stdout.strip()[-80:]}", s)
    if "BRAK" in r.stdout:
        p.note("UWAGA", f"klawiatura: nie wpisano „{word}”")
    return s


@path(11, "Szukaj: wpis „bat” klawiaturą ekranową → wyniki → OK → długie OK")
def _(p):
    s = open_route(p, "search")
    if s.focus is None:
        p.note("BŁĄD", "Szukaj: brak fokusu")
        return
    s = type_osk(p, "bat")
    if not s.has("Batman", "Bat"):
        p.note("UWAGA", "„bat”: brak wyników Batman")
        return
    move(p, "RIGHT", 7)
    a, b = ok(p, "OK na wyniku")
    back(p, a)
    a, b = long_ok(p, "długie OK na wyniku")
    if changed(a, b):
        back(p, a, "Wstecz z menu")


def search_right(p, down=0):
    """Z klawiatury Szukaj do prawej kolumny (Przeglądaj), potem ▼ down razy (Polecane)."""
    open_route(p, "search")
    s = screen()
    for _ in range(8):
        key("RIGHT", pause=0.35)
        s = screen()
        if s.focus and len(s.focus) > 2:
            break
    p.step("► do prawej kolumny", s)
    if down:
        s = move(p, "DOWN", down)
    return s


@path(12, "Szukaj: polecane → OK → tytuł → Wstecz (fokus wraca na plakat)")
def _(p):
    search_right(p, 1)
    a, b = ok(p)
    back(p, a)


@path(13, "Szukaj: długie OK na polecanym plakacie")
def _(p):
    search_right(p, 1)
    a, b = long_ok(p)
    if changed(a, b):
        back(p, a, "Wstecz z menu")


@path(14, "Szukaj: kafelek „przeglądaj” → OK")
def _(p):
    search_right(p)
    a, b = ok(p)
    back(p, a)


@path(15, "Biblioteka: otwarcie, OK na pierwszej pozycji")
def _(p):
    s = open_route(p, "library")
    if s.focus is None:
        p.note("UWAGA", "Biblioteka: brak fokusu (pusta?)")
        return
    a, b = ok(p)
    back(p, a)


@path(16, "Biblioteka: długie OK na pozycji")
def _(p):
    s = open_route(p, "library")
    if s.focus is None:
        return
    move(p, "DOWN")
    a, b = long_ok(p)
    if changed(a, b):
        back(p, a, "Wstecz z menu")


def to_detail(p):
    home_rows(p)
    move(p, "DOWN", 2)
    a, b = ok(p, "OK → strona tytułu")
    return b


@path(17, "Strona tytułu: przyciski ◄► i OK na każdym")
def _(p):
    s = to_detail(p)
    seen = []
    for i in range(6):
        s = screen()
        if s.focus in seen:
            break
        seen.append(s.focus)
        key("RIGHT")
    p.step("przyciski: " + " / ".join(str(x) for x in seen), screen())


@path(18, "Strona tytułu: długie OK na przycisku „Odtwórz”")
def _(p):
    to_detail(p)
    a, b = long_ok(p, expect=False)
    back(p)


@path(19, "Strona tytułu: ▼ do odcinków/podobnych, OK, Wstecz")
def _(p):
    to_detail(p)
    move(p, "DOWN", 2)
    a, b = ok(p)
    back(p, a)


@path(20, "Strona tytułu: długie OK na „Podobne”")
def _(p):
    to_detail(p)
    if not seek(p, is_poster, "DOWN", 6, "plakat „Podobne”"):
        return
    a, b = long_ok(p)
    if not b.has("Akcje tytułu", "Title actions"):
        p.note("BŁĄD", "długie OK na „Podobne”: brak menu")
    if changed(a, b):
        back(p, a, "Wstecz z menu")


@path(21, "Strona tytułu: obsada → osoba → plakat → Wstecz×2")
def _(p):
    to_detail(p)
    s = None
    for i in range(6):
        s = move(p, "DOWN")
        if s.has("Obsada", "Cast"):
            pass
    a, b = ok(p, "OK na osobie")
    if b.has("Filmy", "Seriale", "Biografia"):
        move(p, "DOWN")
        a2, b2 = long_ok(p)
        if changed(a2, b2):
            back(p, a2, "Wstecz z menu")
    back(p)
    back(p)


@path(22, "Ustawienia: otwarcie i przejście ▼ po kategoriach")
def _(p):
    open_route(p, "settings")
    seen = []
    for i in range(14):
        s = screen()
        seen.append(s.focus)
        key("DOWN", pause=0.4)
    p.step("kategorie: " + " / ".join(dict.fromkeys(str(x) for x in seen)), screen())


@path(23, "Ustawienia: ► do treści kategorii, OK, Wstecz")
def _(p):
    open_route(p, "settings")
    move(p, "RIGHT")
    a, b = ok(p)
    back(p, a)


@path(24, "Ustawienia odtwarzania: przełącznik OK dwa razy (stan wraca)")
def _(p):
    open_route(p, "playback_settings")
    move(p, "DOWN")
    a = screen()
    key("CENTER")
    b, _ = settle(4)
    key("CENTER")
    c, _ = settle(4)
    p.step("OK×2", c)
    if b.sig() == a.sig() and b.focus == a.focus:
        p.note("UWAGA", f"przełącznik „{a.focus}”: brak widocznej zmiany po OK")


@path(25, "Ustawienia: długie OK na pozycji (nie powinno nic psuć)")
def _(p):
    open_route(p, "settings")
    move(p, "RIGHT")
    a, b = long_ok(p, expect=False)
    if changed(a, b):
        back(p, a)


@path(26, "Live TV: wejście (stan pusty albo przewodnik)")
def _(p):
    s = open_route(p, "reshaped_live_tv")
    p.step("Live TV", s)


@path(27, "Live TV: OK na kanale → odtwarzanie → Wstecz")
def _(p):
    s = open_route(p, "reshaped_live_tv")
    move(p, "DOWN", 2)
    a, b = ok(p, "OK na kanale")
    time.sleep(4)
    back(p, a, "Wstecz z odtwarzacza")


@path(28, "Live TV: długie OK na kanale")
def _(p):
    open_route(p, "reshaped_live_tv")
    move(p, "DOWN", 2)
    a, b = long_ok(p)
    if changed(a, b):
        back(p, a, "Wstecz z menu")


@path(29, "Live TV: przewijanie listy ▼×15 i ▲×15")
def _(p):
    open_route(p, "reshaped_live_tv")
    for d in ("DOWN", "UP"):
        t0 = time.time()
        key(d, 15, pause=0.12)
        s, dt = settle(6)
        p.step(f"{d}×15 ({time.time() - t0:.1f} s)", s)
        if s.focus is None:
            p.note("BŁĄD", f"Live TV: po {d}×15 brak fokusu")


@path(30, "Live TV: Szukaj kanału")
def _(p):
    open_route(p, "fork_tv_search")
    s = search_type(p, "tvp")
    if not s.has("TVP"):
        p.note("UWAGA", "szukanie „tvp”: brak wyników TVP")


@path(31, "YouTube (Live): OK na filmie → odtwarzacz → Wstecz")
def _(p):
    if not youtube_live(p):
        return
    a, b = ok(p)
    time.sleep(3)
    back(p, a)


@path(32, "YouTube (Live): długie OK na filmie")
def _(p):
    if not youtube_live(p):
        return
    a, b = long_ok(p)
    if changed(a, b):
        back(p, a, "Wstecz z menu")


@path(33, "Kalendarz: otwarcie, ►×3, OK, Wstecz")
def _(p):
    open_route(p, "calendar")
    move(p, "RIGHT", 3)
    a, b = ok(p)
    back(p, a)


@path(34, "Dodatki: lista, OK na dodatku, Wstecz")
def _(p):
    open_route(p, "addon_manager")
    move(p, "DOWN", 2)
    a, b = ok(p)
    back(p, a)


@path(35, "Dodatki: długie OK na dodatku")
def _(p):
    open_route(p, "addon_manager")
    move(p, "DOWN", 2)
    a, b = long_ok(p, expect=False)
    if changed(a, b):
        back(p, a)


@path(36, "Kolejność katalogów: otwarcie, ▼, OK")
def _(p):
    open_route(p, "catalog_order")
    move(p, "DOWN")
    a, b = ok(p)
    back(p, a)


@path(37, "Kolekcje: otwarcie, OK, Wstecz")
def _(p):
    open_route(p, "collections")
    move(p, "DOWN")
    a, b = ok(p)
    back(p, a)


@path(38, "Odkrywaj forka: otwarcie, ▼►, OK, Wstecz")
def _(p):
    open_route(p, "fork_discover")
    move(p, "DOWN")
    move(p, "RIGHT")
    a, b = ok(p)
    back(p, a)


@path(39, "Odkrywaj forka: długie OK na plakacie")
def _(p):
    open_route(p, "fork_discover")
    move(p, "DOWN")
    a, b = long_ok(p)
    if changed(a, b):
        back(p, a, "Wstecz z menu")


@path(40, "Wybrane (fork_picks): otwarcie i OK")
def _(p):
    open_route(p, "fork_picks")
    move(p, "DOWN")
    a, b = ok(p)
    back(p, a)


@path(41, "Moduły forka: przełącznik OK×2")
def _(p):
    open_route(p, "fork_modules")
    move(p, "DOWN")
    a = screen()
    key("CENTER")
    b, _ = settle(4)
    key("CENTER")
    c, _ = settle(4)
    p.step("OK×2", c)
    if not changed(a, b):
        p.note("UWAGA", f"moduł „{a.focus}”: brak widocznej zmiany po OK")


@path(42, "Narzędzia forka: otwarcie, ▼×3, OK, Wstecz")
def _(p):
    open_route(p, "fork_tools")
    move(p, "DOWN", 3)
    a, b = ok(p)
    back(p, a)


@path(43, "Wygląd (layout_settings): ▼×5 i OK")
def _(p):
    open_route(p, "layout_settings")
    move(p, "DOWN", 5)
    a, b = ok(p, expect_change=False)
    back(p)


@path(44, "Profile: ekran wyboru, długie OK na profilu")
def _(p):
    open_route(p, "profiles")
    a, b = long_ok(p)
    if changed(a, b):
        back(p, a, "Wstecz z menu")


@path(45, "Szczegóły → Odtwórz → wybór źródła → Wstecz")
def _(p):
    to_detail(p)
    a, b = ok(p, "OK Odtwórz")
    time.sleep(4)
    s = screen()
    p.step("po 4 s", s)
    back(p)
    back(p)


@path(46, "Główna → Szukaj → Biblioteka → Główna (menu) — fokus wraca")
def _(p):
    for r in ("home", "search", "library", "home"):
        open_route(p, r)


@path(47, "Główna: ►×12 w rzędzie (długi rząd), ◄×12")
def _(p):
    home_rows(p)
    move(p, "DOWN", 2)
    t0 = time.time()
    key("RIGHT", 12, pause=0.15)
    s, dt = settle(5)
    p.step(f"►×12 ({time.time() - t0:.1f} s)", s)
    key("LEFT", 12, pause=0.15)
    s2, _ = settle(5)
    p.step("◄×12", s2)
    if s2.focus is None:
        p.note("BŁĄD", "po ◄×12 brak fokusu")


@path(48, "Strona tytułu: Wstecz z głębi (▼×4) wraca na górę albo wychodzi")
def _(p):
    to_detail(p)
    move(p, "DOWN", 4)
    b = back(p)
    p.step("po Wstecz", b)


@path(49, "Odtwarzacz YouTube: OK pauza, ◄► przewijanie, Wstecz")
def _(p):
    if not youtube_live(p):
        return
    ok(p, "OK film")
    time.sleep(6)
    key("CENTER")
    s, _ = settle(3)
    p.step("OK (nakładka/pauza)", s)
    key("RIGHT", 2)
    s = screen()
    p.step("►×2", s)
    back(p)
    back(p)


@path(50, "Długie OK na menu bocznym (nie powinno psuć)")
def _(p):
    home_rows(p)
    key("BACK", pause=0.6)
    a, b = long_ok(p, expect=False)
    if changed(a, b):
        back(p)


def main():
    args = sys.argv[1:]
    cyc = "1"
    if "--cykl" in args:
        i = args.index("--cykl")
        cyc = args[i + 1]
        del args[i:i + 2]
    nums = [int(a) for a in args] or sorted(P)
    out = os.path.join(ROOT, "plan", "ux", "cykl" + cyc)
    os.makedirs(out, exist_ok=True)
    adb("logcat", "-b", "crash", "-c")
    adb("shell", "settings", "put", "global", "window_animation_scale", "0.5")
    res, t0 = [], time.time()
    for n in nums:
        name, fn = P[n]
        print(f"[{datetime.datetime.now():%H:%M:%S}] ścieżka {n}: {name}", flush=True)
        p = run_path(fn, n, name)
        print(f"   → {p.verdict} ({p.secs:.0f} s) " + "; ".join(p.notes), flush=True)
        res.append(p)
    total = time.time() - t0
    with open(os.path.join(out, "sciezki.json"), "w") as f:
        json.dump([{"nr": p.nr, "nazwa": p.name, "wynik": p.verdict, "uwagi": p.notes, "kroki": p.steps, "s": round(p.secs)} for p in res], f, ensure_ascii=False, indent=1)
    cnt = {v: sum(1 for p in res if p.verdict == v) for v in ("OK", "UWAGA", "BŁĄD")}
    with open(os.path.join(out, "sciezki.md"), "w") as f:
        f.write(f"# Ścieżki UX/UI — cykl {cyc} ({datetime.datetime.now():%d.%m %H:%M}, {DEV}, {total / 60:.1f} min)\n\n")
        f.write(f"OK {cnt['OK']}, UWAGA {cnt['UWAGA']}, BŁĄD {cnt['BŁĄD']}\n\n")
        for p in res:
            f.write(f"## {p.nr}. {p.name} — **{p.verdict}** ({p.secs:.0f} s)\n")
            for nt in p.notes:
                f.write(f"- {nt}\n")
            f.write("<details><summary>kroki</summary>\n\n" + "\n".join("- " + s for s in p.steps) + "\n</details>\n\n")
    print(f"koniec: {total / 60:.1f} min, {cnt}, raport {out}/sciezki.md")


if __name__ == "__main__":
    main()
