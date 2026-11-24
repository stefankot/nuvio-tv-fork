#!/usr/bin/env python3
"""50 ścieżek UX / fokusu na JMGO (24.11.2026, prompt `plan/ux/jmgo/PROMPT.md`).

Użycie:  python3 -I fork-tools/uitest/sciezki_jmgo.py --cykl N [--bez-czekania] [numery ścieżek ...]
Wynik:   plan/ux/jmgo/cykl<N>/{sciezki.md, sciezki.json, fokus.csv, logcat.txt, gfx/*.txt}

- Odczyt ekranu: stały czytnik `fastdump/FastDump.java` (jedno połączenie UiAutomation, klawisze bez `input`, odczyt po ~350 ms ciszy:
  ~0,1–0,6 s, ten sam XML); gdy nie działa albo `FASTDUMP=0` → `uiautomator dump --compressed` (JMGO: ~2,2 s i czeka ≥1 s na ciszę). Gdy aplikacja ma ForkFocusTrace
  (log `ForkFocus`, przełącznik w Narzędziach forka), ruch fokusu i czasy (dtFocus/dtFrame) bierzemy z logu, a `dump` tylko
  w punktach kontrolnych reguł (granice, widoczność, teksty).
- Przytrzymanie: prawdziwe (`sendevent` na urządzeniu pilota /dev/input/event12: wciśnij, czekaj, puść) — system sam
  generuje powtórzenia jak przy pilocie. Długie OK = przytrzymanie OK 700 ms.
- Bezpieczeństwo (prompt „JMGO: bezpieczeństwo”): przed każdą ścieżką sprawdzamy wierzch; dwa razy z rzędu cudza aplikacja =
  ktoś używa TV → czekamy na „WOLNY” (jmgo_czekaj_wolny.sh). Żadnych `pm clear`, PIN-ów, tłumaczenia AI (ustawienia systemu: ZASADY §5 — wolno, gdy przyspiesza pracę).
  Ustawienia zmieniane w ścieżce wracają w tej samej ścieżce (przełącznik ×2). Odtwarzanie najwyżej ok. 10 s.

Wynik ścieżki: OK / UWAGA (budżet „cel” przekroczony, reguła spełniona) / BŁĄD (reguła złamana, budżet „błąd”, awaria) /
POMINIĘTA (ekranu brak — z powodem).
"""
import atexit, csv, datetime, json, os, re, select, subprocess, sys, time
import xml.etree.ElementTree as ET

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
DEV = os.environ.get("DEV", "192.168.31.252:5555")
if DEV.startswith("192.168.31.252"):  # JMGO przez osobny serwer adb (fork-tools/test/jmgo_adb.sh)
    os.environ.update(ANDROID_ADB_SERVER_PORT="5038", ADB_EMU="0", ADB_LOCAL_TRANSPORT_MAX_PORT="5553", ANDROID_SERIAL=DEV)
PKG = "com.nuviodebug.com"
ACT = PKG + "/com.nuvio.tv.MainActivity"
LAUNCH = PKG + "/com.nuvio.tv.launcher.AppIconDefault"
W, H = 1920, 1080
MARGIN = 8  # px = 4 dp (F7)
REMOTE = os.environ.get("REMOTE_DEV", "/dev/input/event12")
SCAN = {"UP": 103, "DOWN": 108, "LEFT": 105, "RIGHT": 106, "CENTER": 28, "BACK": 158}
MENU_WORDS = ("Główna", "Szukaj", "Biblioteka", "Live TV", "Telewizja", "YouTube", "Ustawienia", "Odkrywaj", "Profil")
# Budżety (plan/DECYZJE.md, 24.11 UX JMGO): (cel, błąd) w sekundach
B_OPEN = (0.6, 1.5)      # OK → pierwszy fokus na nowym ekranie
B_DATA = (3.0, 5.0)      # pełne dane ekranu (z sieci)
B_BACK = (0.3, 0.8)      # Wstecz → fokus na elemencie źródłowym
B_COLD = (3.5, 5.0)
B_WARM = (1.0, 2.0)


def adb(*a, timeout=40):
    try:
        return subprocess.run([ADB, "-s", DEV] + list(a), capture_output=True, text=True, timeout=timeout).stdout
    except subprocess.TimeoutExpired:
        return ""


def sh(cmd, timeout=40):
    return adb("shell", cmd, timeout=timeout)


# ------------------------------------------------------------------ odczyt ekranu
def bounds(n):
    m = list(map(int, re.findall(r"-?\d+", n.get("bounds", ""))))
    return tuple(m) if len(m) == 4 else (0, 0, 0, 0)


class Screen:
    def __init__(self, root, t=None):
        self.t = t or time.time()
        self.root, self.texts, self.focus, self.fb, self.rid, self.pkg, self.nfocus = root, [], None, None, "", "", 0
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
        self.nfocus = len(foc)
        if foc:
            f = foc[-1]
            self.fb, self.rid = bounds(f), (f.get("resource-id") or "")
            own = (f.get("text") or f.get("content-desc") or "").strip()
            inner = [own] if own else []
            for n in root.iter("node"):
                b = bounds(n)
                t = (n.get("text") or n.get("content-desc") or "").strip()
                if t and b[0] >= self.fb[0] and b[1] >= self.fb[1] and b[2] <= self.fb[2] and b[3] <= self.fb[3] and t not in inner:
                    inner.append(t)
            name = " | ".join(inner)[:90]
            if self.rid:
                name = (self.rid + " " + name).strip()
            self.focus = name or "[bez nazwy %dx%d @%d,%d]" % (self.fb[2] - self.fb[0], self.fb[3] - self.fb[1], self.fb[0], self.fb[1])

    def sig(self):
        return frozenset(self.texts)

    def has(self, *words):
        low = " ".join(self.texts).lower()
        return any(w.lower() in low for w in words)

    def key(self):
        return (self.focus, self.fb)

    def cx(self):
        return (self.fb[0] + self.fb[2]) / 2 if self.fb else -1

    def cy(self):
        return (self.fb[1] + self.fb[3]) / 2 if self.fb else -1

    def w(self):
        return self.fb[2] - self.fb[0] if self.fb else 0

    def short(self):
        return f"{self.focus} {list(self.fb) if self.fb else ''}"


FASTJAR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fastdump", "fastdump.jar")
KEY_SETTLE = float(os.environ.get("FD_KEY_SETTLE", "0.7"))  # s po klawiszu przed odczytem (czytnik)
QUIET = int(os.environ.get("FD_QUIET", "350"))  # ms ciszy na ekranie przed odczytem (zwykły dump: na sztywno 1000 ms, do 10 s)
DUMPST = {"n": 0, "s": 0.0, "fast": 0, "legacy": 0, "fail": 0, "keys": 0}  # statystyka odczytów ekranu (koszt czasu testu)
KEYNUM = {"KEYCODE_DPAD_UP": 19, "KEYCODE_DPAD_DOWN": 20, "KEYCODE_DPAD_LEFT": 21, "KEYCODE_DPAD_RIGHT": 22,
          "KEYCODE_DPAD_CENTER": 23, "KEYCODE_BACK": 4, "KEYCODE_HOME": 3}


class Fast:
    """Stały czytnik ekranu na JMGO (fastdump/FastDump.java): jedno połączenie z UiAutomation na cały test, klawisze bez
    uruchamiania `input`, odczyt po krótkiej ciszy (QUIET) zamiast `waitForIdle(1000, 10000)` zwykłego `uiautomator dump`
    (24.11, właściciel: testy połowę czasu stały bezczynnie). Ten sam XML. Błąd → zwykły dump (FASTDUMP=0 wyłącza)."""

    def __init__(self):
        self.proc, self.buf, self.fails = None, b"", 0
        self.dead = os.environ.get("FASTDUMP", "1") == "0"

    def _read(self, pred, timeout):
        fd, end = self.proc.stdout.fileno(), time.time() + timeout
        while True:
            m = pred(self.buf)
            if m is not None:
                return m
            left = end - time.time()
            if left <= 0:
                return None
            if select.select([fd], [], [], min(0.5, left))[0]:
                chunk = os.read(fd, 1 << 16)
                if not chunk:
                    return None
                self.buf += chunk

    def start(self):
        if self.proc is not None:
            return True
        if self.dead:
            return False
        try:
            r = subprocess.run([ADB, "-s", DEV, "push", FASTJAR, "/data/local/tmp/fastdump.jar"], capture_output=True, text=True, timeout=40)
            if r.returncode != 0:
                raise RuntimeError("push: " + (r.stderr or r.stdout).strip()[:120])
            os.makedirs(os.path.join(ROOT, ".stage"), exist_ok=True)
            self.err = open(os.path.join(ROOT, ".stage", "fastdump_err.log"), "ab")
            self.proc = subprocess.Popen([ADB, "-s", DEV, "shell", "-T", "app_process", "-Djava.class.path=/data/local/tmp/fastdump.jar:/system/framework/uiautomator.jar",
                                          "/system/bin", "FastDump", str(W), str(H)], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=self.err, bufsize=0)
            self.buf = b""

            def ready(b):
                i = b.find(b"#READY\n")
                return i + 7 if i >= 0 else None
            n = self._read(ready, 25)
            if n is None:
                raise RuntimeError("brak #READY: " + self.buf[:120].decode("utf-8", "replace"))
            self.buf = self.buf[n:]
            print("FastDump: czytnik ekranu działa")
            return True
        except Exception as e:
            print(f"UWAGA: FastDump nie działa ({type(e).__name__}: {e}) — zwykły uiautomator dump")
            self.stop()
            self.dead = True
            return False

    def stop(self):
        p, self.proc = self.proc, None
        if p is None:
            return
        try:
            p.stdin.write(b"Q\n")
            p.stdin.close()
            p.wait(timeout=4)
        except Exception:
            p.kill()

    def cmd(self, line, timeout):
        """→ (xml_bytes, idle_ms, dump_ms) albo None (błąd; czytnik wyłączony po 3 błędach)."""
        try:
            self.proc.stdin.write((line + "\n").encode())

            def end(b):
                m = re.search(rb"#END (\d+) (\d+)\n", b)
                return m if m else None
            m = self._read(end, timeout)
            if m is None:
                raise RuntimeError("brak odpowiedzi na: " + line)
            x, self.buf = self.buf[:m.start()], self.buf[m.end():]
            if b"#ERR" in x:
                raise RuntimeError(x.decode("utf-8", "replace").strip()[:120])
            return x, int(m.group(1)), int(m.group(2))
        except Exception as e:
            print(f"UWAGA: FastDump błąd ({type(e).__name__}: {e})")
            DUMPST["fail"] += 1
            self.stop()
            self.fails += 1
            if self.fails >= 3:
                self.dead = True
                print("UWAGA: FastDump wyłączony po 3 błędach — dalej zwykły uiautomator dump")
            return None

    def press(self, code, n, pause_ms):
        r = self.cmd(f"X {code} {n} {pause_ms}", 8 + n * (pause_ms / 1000 + 0.5))
        return r is not None


FAST = Fast()
atexit.register(FAST.stop)


def dump(quiet=None):
    t0 = time.time()
    if FAST.start():
        r = FAST.cmd(f"D {QUIET if quiet is None else quiet} 3000", 20)
        if r is not None:
            x = r[0].decode("utf-8", "replace")
            i, j = x.find("<?xml"), x.rfind("</hierarchy>")
            if i >= 0 and j > i:
                try:
                    root = ET.fromstring(x[i:j + 12])
                    DUMPST["n"] += 1; DUMPST["fast"] += 1; DUMPST["s"] += time.time() - t0
                    return root
                except ET.ParseError as e:
                    print(f"UWAGA: FastDump zły XML ({e})")
                    DUMPST["fail"] += 1
    for _ in range(3):
        x = adb("exec-out", "uiautomator", "dump", "--compressed", "/dev/tty", timeout=25)
        i, j = x.find("<?xml"), x.rfind("</hierarchy>")
        if i >= 0 and j > i:
            try:
                root = ET.fromstring(x[i:j + 12])
                DUMPST["n"] += 1; DUMPST["legacy"] += 1; DUMPST["s"] += time.time() - t0
                return root
            except ET.ParseError:
                pass
        time.sleep(0.4)
    return None


def look(quiet=None):
    return Screen(dump(quiet))


def settle(maxs=10.0, need_focus=True):
    """Dwa kolejne odczyty z tym samym fokusem i tekstami → (Screen, s od wywołania)."""
    t0, prev, tprev = time.time(), None, 0.0
    while time.time() - t0 < maxs:
        s = look()
        cur = (s.focus, s.fb, s.sig())
        if prev is not None and cur == prev and s.texts and (s.focus or not need_focus):
            return s, tprev  # czas pierwszego z dwóch zgodnych odczytów
        prev, tprev = cur, time.time() - t0
        time.sleep(0.2)
    return look(), time.time() - t0


def first_focus(maxs=12.0):
    """Pierwszy odczyt z fokusem (czas od wywołania, rozdzielczość ~2 s bez ForkFocus)."""
    t0 = time.time()
    while time.time() - t0 < maxs:
        s = look()
        if s.focus and s.pkg == PKG:
            return s, time.time() - t0
    return look(), time.time() - t0


# ------------------------------------------------------------------ klawisze
CODES = {"UP": "KEYCODE_DPAD_UP", "DOWN": "KEYCODE_DPAD_DOWN", "LEFT": "KEYCODE_DPAD_LEFT", "RIGHT": "KEYCODE_DPAD_RIGHT",
         "CENTER": "KEYCODE_DPAD_CENTER", "BACK": "KEYCODE_BACK", "HOME": "KEYCODE_HOME"}
ARROW = {"UP": "▲", "DOWN": "▼", "LEFT": "◄", "RIGHT": "►", "CENTER": "OK", "BACK": "Wstecz", "HOME": "HOME"}


def key(k, n=1, pause=0.25):
    code = CODES.get(k, k)
    if code in KEYNUM and FAST.start() and FAST.press(KEYNUM[code], n, int(pause * 1000)):
        DUMPST["keys"] += n
        # Odczyt drzewa dostępności (Compose) spóźnia się o jeden klawisz, gdy czytamy < ~0,7 s po naciśnięciu (pomiar 24.11: pauza 0,4 s →
        # przestarzały fokus, 0,7 s → zgodny z logiem ForkFocus); pętle „▲ aż do …” przestrzeliwały. Zwykły dump miał to w czekaniu na ciszę 1 s.
        time.sleep(max(pause, KEY_SETTLE))
        return
    if n > 1:
        sh(";".join([f"input keyevent {code}; sleep {pause}"] * n))
    else:
        sh(f"input keyevent {code}")
    time.sleep(pause)


def hold(k, secs):
    """Prawdziwe przytrzymanie klawisza pilota (sendevent). False = brak uprawnień → seria keyevent."""
    sc = SCAN[k]
    out = sh(f"sendevent {REMOTE} 1 {sc} 1 && sendevent {REMOTE} 0 0 0 && sleep {secs} && sendevent {REMOTE} 1 {sc} 0 && sendevent {REMOTE} 0 0 0 && echo HOLD_OK",
             timeout=int(secs) + 20)
    if "HOLD_OK" in out:
        return True
    n = max(1, int(secs / 0.05))
    sh(f"for i in $(seq 1 {n}); do input keyevent {CODES[k]}; done", timeout=int(secs) + 60)
    return False


def long_ok_press():
    if not hold("CENTER", 0.7):
        sh("input keyevent --longpress KEYCODE_DPAD_CENTER")


# ------------------------------------------------------------------ logi
def logcat_clear():
    adb("logcat", "-c")


def log_lines(tags=("ForkFocus:I",)):
    return adb("logcat", "-d", "-v", "time", "-s", *tags).splitlines()


TRACE = {"on": None}
STATS = {"dtFrame": [], "dtFocus": [], "firstFocus": [], "back": []}


def trace_after(p, label):
    """Wpisy ForkFocus od ostatniego logcat -c: zbiera dtFrame (strzałki) i firstFocus; zwraca opis do CSV."""
    if not TRACE["on"]:
        return ""
    ls = log_lines()
    st = trace_stats(ls)
    if st["dtFrame"]:
        if label.startswith(("▲", "▼", "◄", "►")):
            STATS["dtFrame"].extend(st["dtFrame"][-1:]); STATS["dtFocus"].extend([x for x in st["dtFocus"][-1:] if x >= 0])
        if label.startswith("Wstecz"):
            STATS["back"].extend(st["dtFrame"][-1:])
    if st["firstFocus"]:
        STATS["firstFocus"].append((label, st["firstFocus"][-1]))
    out = []
    if st["dtFrame"]:
        out.append(f"dtFocus {st['dtFocus'][-1] if st['dtFocus'] else '-'} / dtFrame {st['dtFrame'][-1]} ms")  # E1: dtFrame bez dtFocus → był IndexError
    if st["firstFocus"]:
        out.append(f"firstFocus {st['firstFocus'][-1]} ms")
    return "; ".join(out)


def pct(xs, q):
    xs = sorted(xs)
    if not xs:
        return None
    return xs[min(len(xs) - 1, int(round(q / 100 * (len(xs) - 1))))]


def focus_trace():
    """Czy aplikacja ma ForkFocusTrace (log ForkFocus)."""
    if TRACE["on"] is None:
        TRACE["on"] = any("ForkFocus" in l for l in log_lines())
    return TRACE["on"]


def trace_stats(lines):
    """dtFocus/dtFrame/firstFocus z logu ForkFocus."""
    out = {"dtFocus": [], "dtFrame": [], "firstFocus": [], "screen": []}
    for l in lines:
        for k in ("dtFocus", "dtFrame", "firstFocus"):
            m = re.search(k + r"=(\d+)", l)
            if m:
                out[k].append(int(m.group(1)))
        m = re.search(r"screen=(\S+)", l)
        if m:
            out["screen"].append(m.group(1))
    return out


# ------------------------------------------------------------------ ścieżka i reguły
CSV_ROWS = []


class Path:
    ORDER = {"OK": 0, "UWAGA": 1, "BŁĄD": 2, "POMINIĘTA": -1}

    def __init__(self, nr, name):
        self.nr, self.name, self.steps, self.verdict, self.notes, self.t0, self.n = nr, name, [], "OK", [], time.time(), 0
        self.metrics = {}

    def note(self, level, msg):
        if level == "POMINIĘTA":
            self.verdict = "POMINIĘTA"
        elif self.verdict != "POMINIĘTA" and self.ORDER[level] > self.ORDER[self.verdict]:
            self.verdict = level
        self.notes.append(f"{level}: {msg}")

    def rec(self, k, a, b, dt, rule, res, msg=""):
        self.n += 1
        CSV_ROWS.append([self.nr, self.n, k, a.short() if a else "", b.short() if b else "", f"{dt:.2f}", rule, res, msg])
        self.steps.append(f"{k} [{rule}] {res}: {a.focus if a else ''} → {b.focus if b else ''} ({dt:.1f} s) {msg}".strip())
        if res in ("UWAGA", "BŁĄD"):
            self.note(res, f"{rule} {k}: {msg or (b.focus if b else '')}")

    def metric(self, k, v):
        self.metrics[k] = v


def vis(s):
    """F7: fokus w całości na ekranie, ≥ 8 px od krawędzi."""
    if not s.fb:
        return False
    l, t, r, b = s.fb
    if l <= 0 and r >= W:  # wiersz listy na całą szerokość (Live TV): krawędzie boczne są jego projektem — sprawdzamy tylko pion
        return t >= MARGIN and b <= H - MARGIN
    return l >= MARGIN and t >= MARGIN and r <= W - MARGIN and b <= H - MARGIN


def check_vis(p, k, a, b, dt):
    if b.fb is None:
        p.rec(k, a, b, dt, "F7", "BŁĄD", "brak fokusu")
    elif not vis(b):
        p.rec(k, a, b, dt, "F7", "BŁĄD", f"fokus przy krawędzi/poza ekranem {list(b.fb)}")
    else:
        p.rec(k, a, b, dt, "F7", "OK")


def scrolled(a, b):
    """Ekran przewinął się / zmienił układ (hero wyjeżdża, lista przesunięta) — położenie nieporównywalne."""
    sa, sb = a.sig(), b.sig()
    return len(sa ^ sb) > max(3, len(sa) // 5)


def moved_dir(a, b, d):
    """F2: nowy fokus w stronę strzałki (albo ten sam punkt ekranu przy przewinięciu listy — inny element)."""
    if not a.fb or not b.fb:
        return False
    if a.key() == b.key():
        return False
    dx, dy = b.cx() - a.cx(), b.cy() - a.cy()
    tol = 12
    return {"RIGHT": dx >= -tol, "LEFT": dx <= tol, "DOWN": dy >= -tol, "UP": dy <= tol}[d]


def step(p, d, rule="F2", expect_move=True, n=1, pause=0.25):
    """Strzałka(i) + odczyt. Reguła F2 (kierunek) albo F4 (krawędź: brak ruchu)."""
    a = p.last if getattr(p, "last", None) else look()
    if TRACE["on"]:
        logcat_clear()
    t0 = time.time()
    key(d, n, pause)
    b = look()
    dt = time.time() - t0
    lab = f"{ARROW[d]}×{n}" if n > 1 else ARROW[d]
    tr = trace_after(p, lab)
    if tr:
        p.steps.append(f"  ForkFocus: {tr}")
    if b.fb is None:
        p.rec(lab, a, b, dt, rule, "BŁĄD", "fokus zgubiony")
    elif rule == "F4":
        p.rec(lab, a, b, dt, "F4", "OK" if a.key() == b.key() else "BŁĄD", "" if a.key() == b.key() else "ruch na krawędzi (zawijanie/skok)")
    elif expect_move:
        ok = moved_dir(a, b, d) if n == 1 else a.key() != b.key()
        if not ok and a.focus != b.focus and scrolled(a, b):
            p.rec(lab, a, b, dt, rule, "OK", "przewinięcie/zmiana układu")
        else:
            p.rec(lab, a, b, dt, rule, "OK" if ok else "BŁĄD", "" if ok else "fokus nie ruszył się w stronę strzałki")
    else:
        p.rec(lab, a, b, dt, rule, "OK")
    p.last = b
    return b


def open_route(p, route, expect=None, rule="F1", own_menu=False):
    logcat_clear()
    t0 = time.time()
    sh(f"am start -a com.nuvio.fork.OPEN -n {ACT} --es route '{route}'")
    s, dt = first_focus(14)
    s2, dt2 = settle(8)
    total = dt + dt2 if s2.key() != s.key() else dt
    p.last = s2
    if s2.pkg != PKG:
        p.rec(f"trasa {route}", None, s2, total, rule, "BŁĄD", f"na wierzchu {s2.pkg}")
        return s2
    # E3 (24.11): trasa „home” na już otwartą Główną otwiera menu z fokusem na „Główna” (zdarzenie testowe, nie ścieżka
    # użytkownika) — wchodzimy ► w treść, jak użytkownik; bez tego kolejne kroki oceniałyby menu zamiast treści (pętla 1: 22 fałszywe BŁĘDY).
    via_menu = False
    if route == "home" and in_menu(s2) and not own_menu:
        for _ in range(3):  # ► z menu → treść; fokus przechodzi asynchronicznie (menu zamyka się), więc czekamy aż się ustali
            key("RIGHT", pause=0.4)
            s3, _t = settle(6)
            if s3.fb is not None and not in_menu(s3):
                s2, via_menu, p.last = s3, True, s3
                break
    res, msg = "OK", f"fokus {dt:.1f} s, stabilnie {total:.1f} s"
    if s2.focus is None:
        res, msg = "BŁĄD", "brak fokusu po otwarciu"
    elif s2.nfocus > 1:
        res, msg = "BŁĄD", f"{s2.nfocus} elementy z fokusem"
    elif in_menu(s2) and rule == "F1" and not own_menu:
        res, msg = "BŁĄD", f"fokus w menu („{s2.focus}”), nie w treści"
    elif not vis(s2):
        res, msg = "BŁĄD", f"fokus poza ekranem {list(s2.fb)}"
    elif dt > B_OPEN[1] + 2.3 or total > B_DATA[1] + 2.3:  # rozdzielczość odczytu ~2,2 s (bez ForkFocus)
        res = "UWAGA"
        msg += f" (budżet {B_OPEN[0]}/{B_DATA[0]} s)"
    if expect and not s2.has(*expect):
        res, msg = ("UWAGA" if res == "OK" else res), msg + f"; brak tekstu {expect}"
    if via_menu:
        msg += "; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści"
    tr = trace_after(p, f"trasa {route}")
    if tr:
        msg += "; " + tr
    p.rec(f"trasa {route}", None, s2, total, rule, res, msg)
    p.metric(f"open:{route}", round(total, 1))
    return s2


def in_menu(s):
    if not s.fb or not s.focus:
        return False
    # Pętla 0 (E2): pozycje menu mają ≤ 450 px szerokości; pole „Szukaj kanału lub programu” (560 px) i wiersze list to nie menu.
    if s.fb[0] >= 120 or (s.fb[2] - s.fb[0]) > 450:
        return False
    return "Zmień profil" in s.focus or any(s.focus.startswith(w) or s.focus.endswith(w) for w in MENU_WORDS)


def ok(p, what="OK", rule="F1", expect_change=True):
    a = p.last if getattr(p, "last", None) else look()
    logcat_clear()
    t0 = time.time()
    key("CENTER", pause=0.1)
    b, dtf = first_focus(10)
    b2, _ = settle(6, need_focus=False)
    dt = time.time() - t0
    toast = "Toast" in adb("logcat", "-d", "-s", "NotificationService:W", "CoreBackPreview:D")
    tr = trace_after(p, what)
    if tr:
        p.steps.append(f"  ForkFocus: {tr}")
    if not alive():
        p.rec(what, a, b2, dt, rule, "BŁĄD", "aplikacja zamknięta")
    elif expect_change and a.key() == b2.key() and a.sig() == b2.sig() and not toast:
        p.rec(what, a, b2, dt, rule, "BŁĄD", "brak reakcji")
    elif b2.focus is None:
        p.rec(what, a, b2, dt, rule, "BŁĄD", "brak fokusu po OK")
    else:
        res = "OK"
        if dtf > B_OPEN[1] + 2.3:  # rozdzielczość odczytu ~2,2 s
            res = "UWAGA"
        p.rec(what, a, b2, dt, rule, res, f"pierwszy fokus {dtf:.1f} s{', komunikat' if toast else ''}")
    p.last = b2
    return a, b2


def long_ok(p, what="długie OK", expect=True):
    """F8: przytrzymanie 700 ms; puszczenie nic nie klika (sprawdzamy log ForkNiunia „OK → …” po długim)."""
    a = p.last if getattr(p, "last", None) else look()
    logcat_clear()
    t0 = time.time()
    long_ok_press()
    b, _ = settle(6, need_focus=False)
    dt = time.time() - t0
    log = adb("logcat", "-d", "-s", "ForkNiunia:I")
    fired = "długie OK" in log
    clicked = re.search(r"ForkNiunia: OK → (?!.*(pominięte|brak akcji))", log) is not None
    if not alive():
        p.rec(what, a, b, dt, "F8", "BŁĄD", "aplikacja zamknięta")
    elif fired and clicked:
        p.rec(what, a, b, dt, "F8", "BŁĄD", "po długim OK puszczenie kliknęło")
    elif expect and not fired and a.key() == b.key():
        p.rec(what, a, b, dt, "F8", "BŁĄD", "brak akcji długiego OK")
    elif expect and not fired:
        p.rec(what, a, b, dt, "F8", "UWAGA", "zmiana ekranu bez logu „długie OK” (element spoza Niuni?)")
    elif not expect and a.key() != b.key() and not fired:
        p.rec(what, a, b, dt, "F10", "OK", "pozycja bez długiego OK: puszczenie = zwykłe OK (nic się nie psuje)")
    else:
        p.rec(what, a, b, dt, "F8" if expect else "F10", "OK", "długie OK" if fired else "")
    p.last = b
    return a, b


def back(p, src=None, what="Wstecz", rule="F5"):
    """F5/F12: Wstecz → fokus na element źródłowy, ten sam przewinięty stan (bounds), nie wyjście z aplikacji."""
    a = p.last if getattr(p, "last", None) else look()
    if TRACE["on"]:
        logcat_clear()
    t0 = time.time()
    key("BACK", pause=0.1)
    b, dtf = first_focus(8)
    b2, _ = settle(5)
    dt = time.time() - t0
    tr = trace_after(p, "Wstecz")
    if tr:
        p.steps.append(f"  ForkFocus: {tr}")
        m = re.search(r"dtFrame (\d+)", tr)
        if m and int(m.group(1)) > B_BACK[1] * 1000:
            p.note("UWAGA", f"F12 Wstecz → fokus {m.group(1)} ms (budżet {int(B_BACK[0] * 1000)} ms)")
    if not alive() or b2.pkg != PKG:
        p.rec(what, a, b2, dt, "F12", "BŁĄD", f"wyjście z aplikacji ({b2.pkg})")
    elif b2.focus is None:
        p.rec(what, a, b2, dt, "F12", "BŁĄD", "brak fokusu po Wstecz")
    elif src is not None and src.focus:
        same = b2.focus == src.focus
        same_b = same and b2.fb is not None and src.fb is not None and all(abs(x - y) <= 4 for x, y in zip(b2.fb, src.fb))
        if not same:
            p.rec(what, a, b2, dt, rule, "BŁĄD", f"fokus „{b2.focus}” zamiast „{src.focus}”")
        elif not same_b:
            p.rec(what, a, b2, dt, rule, "UWAGA", f"ten sam element, inne położenie {list(b2.fb)} vs {list(src.fb)}")
        else:
            p.rec(what, a, b2, dt, rule, "OK", f"pierwszy fokus {dtf:.1f} s")
    else:
        p.rec(what, a, b2, dt, "F12", "OK", f"pierwszy fokus {dtf:.1f} s")
    p.last = b2
    return b2


def alive():
    return sh(f"pidof {PKG}").strip() != ""


def top_pkg():
    m = re.search(r"mCurrentFocus=Window\{\S+ \S+ ([^/\s}]+)", sh("dumpsys window | grep -m1 mCurrentFocus"))
    return m.group(1) if m else ""


def is_poster(s):
    if not s.fb:
        return False
    w, h = s.fb[2] - s.fb[0], s.fb[3] - s.fb[1]
    return h > w * 1.2 and w > 160


def seek(p, pred, d="DOWN", maxn=8, what="element"):
    for i in range(maxn):
        key(d, pause=0.3)
        s = look()
        if pred(s):
            p.rec(f"{ARROW[d]}×{i + 1}", None, s, 0, "-", "OK", f"→ {what}")
            p.last = s
            return s
    p.note("UWAGA", f"nie znaleziono: {what} ({ARROW[d]}×{maxn})")
    return None


def seek_text(p, words, d="DOWN", maxn=10):
    return seek(p, lambda s: s.focus is not None and any(w.lower() in s.focus.lower() for w in words), d, maxn, "/".join(words))


def walk_to_edge(p, d, maxn=12):
    """Strzałka aż fokus przestanie się ruszać; ostatnia = sprawdzenie F4."""
    prev = p.last if getattr(p, "last", None) else look()
    for i in range(maxn):
        key(d, pause=0.3)
        s = look()
        if s.key() == prev.key():
            p.rec(f"{ARROW[d]} na krawędzi (po {i})", prev, s, 0, "F4", "OK")
            p.last = s
            return s, i
        if s.fb is None:
            p.rec(f"{ARROW[d]}", prev, s, 0, "F4", "BŁĄD", "fokus zgubiony")
            p.last = s
            return s, i
        prev = s
    p.rec(f"{ARROW[d]}×{maxn}", None, prev, 0, "F4", "UWAGA", "nie doszedł do krawędzi")
    p.last = prev
    return prev, maxn


# ------------------------------------------------------------------ wydajność w ścieżce
GFX_DIR = None


def gfx_reset():
    sh(f"dumpsys gfxinfo {PKG} reset")


def gfx_take(p, label):
    out = sh(f"dumpsys gfxinfo {PKG}")
    keep = [l.strip() for l in out.splitlines() if re.search(r"Total frames|Janky frames|50th|90th|95th|99th|Number (Missed|High input|Slow UI|Slow bitmap|Slow issue|Frame deadline)", l)]
    with open(os.path.join(GFX_DIR, f"p{p.nr:02d}_{label}.txt"), "w") as f:
        f.write("\n".join(keep[:14]) + "\n")
    m = re.search(r"Janky frames: (\d+) \(([\d.]+)%\)", out)
    p95 = re.search(r"95th percentile: (\d+)ms", out)
    tot = re.search(r"Total frames rendered: (\d+)", out)
    slow_ui = re.search(r"Number Slow UI thread: (\d+)", out)
    slow_bmp = re.search(r"Number Slow bitmap uploads: (\d+)", out)
    v = {"frames": int(tot.group(1)) if tot else 0, "janky%": float(m.group(2)) if m else None, "p95": int(p95.group(1)) if p95 else None,
         "slowUI": int(slow_ui.group(1)) if slow_ui else None, "slowBmp": int(slow_bmp.group(1)) if slow_bmp else None}
    for q in ("50th", "90th", "99th"):
        mm = re.search(q + r" percentile: (\d+)ms", out)
        v[q] = int(mm.group(1)) if mm else None
    p.metric(f"gfx:{label}", v)
    if v["janky%"] is not None:
        res = "OK" if v["janky%"] <= 5 and (v["p95"] or 0) <= 34 else ("BŁĄD" if v["janky%"] > 15 else "UWAGA")
        p.rec(f"gfx {label}", None, None, 0, "klatki", res, f"Janky {v['janky%']}%, p95 {v['p95']} ms, klatek {v['frames']}")
    return v


def jank_100(p, since_clear=True):
    n = sum(1 for l in adb("logcat", "-d", "-s", "ForkJank:W", "ForkJank:I", "ForkPerf:W").splitlines() if re.search(r"\b(\d{3,})\s*ms", l))
    p.metric("klatki>=100ms(log)", n)
    return n


# ------------------------------------------------------------------ ścieżki
P = {}


def path(nr, name):
    def deco(fn):
        P[nr] = (name, fn)
        return fn
    return deco


def home(p):
    s = open_route(p, "home", rule="F1")
    # Baner: fokus na „Odtwórz” (makieta Main). Gdy fokus wrócił w rzędy (pamięć), ▲ do banera.
    for _ in range(6):
        if s.focus and "Odtwórz" in s.focus:
            break
        key("UP", pause=0.4)
        s = look()
    p.last = s
    return s


def to_row(p, row=1):
    s = home(p)
    for _ in range(row):
        s = step(p, "DOWN")
    return s


@path(1, "Zimny start: fokus na „Odtwórz” w banerze; po dociągnięciu rzędów fokus nie skacze [F1 F6, czas]")
def _(p):
    sh(f"am force-stop {PKG}")
    time.sleep(2)
    logcat_clear()
    t0 = time.time()
    out = sh(f"am start -W -n {LAUNCH}", timeout=60)
    s, dtf = first_focus(20)
    tf = time.time() - t0
    m = re.search(r"TotalTime: (\d+)", out)
    p.metric("zimny TotalTime ms", int(m.group(1)) if m else None)
    p.metric("zimny fokus s (dump)", round(tf, 1))
    res = "OK" if s.focus and "Odtwórz" in s.focus else "BŁĄD"
    p.rec("zimny start", None, s, tf, "F1", res, f"TotalTime {m.group(1) if m else '?'} ms; fokus po {tf:.1f} s")
    if tf > B_COLD[1] + 2.3:
        p.note("UWAGA", f"zimny start do fokusu {tf:.1f} s (budżet {B_COLD[0]} s, rozdzielczość odczytu ~2 s)")
    time.sleep(6)
    s2 = look()
    p.rec("po 6 s", s, s2, 6, "F6", "OK" if s2.key() == s.key() else "BŁĄD", "" if s2.key() == s.key() else "fokus przeskoczył po dociągnięciu danych")


@path(2, "Ciepły start (HOME → ikona): fokus tam, gdzie był, albo na banerze [F1]")
def _(p):
    s = home(p)
    s = step(p, "DOWN")
    key("HOME", pause=2.0)
    t0 = time.time()
    out = sh(f"am start -W -n {LAUNCH}", timeout=60)
    b, dtf = first_focus(10)
    m = re.search(r"TotalTime: (\d+)", out)
    p.metric("ciepły TotalTime ms", int(m.group(1)) if m else None)
    where = "tam, gdzie był" if b.key() == s.key() else ("baner" if b.focus and "Odtwórz" in b.focus else f"inaczej: {b.focus}")
    p.rec("HOME → ikona", s, b, time.time() - t0, "F1", "OK" if where != f"inaczej: {b.focus}" else "BŁĄD", f"TotalTime {m.group(1) if m else '?'} ms; fokus: {where}")
    p.metric("ciepły: fokus", where)


@path(3, "Baner: ◄► po przyciskach, skrajne bez zawijania, ▲ nie gubi fokusu [F4 F10]")
def _(p):
    s = home(p)
    walk_to_edge(p, "RIGHT", 8)
    for _ in range(8):
        b = look()
        if b.focus and "Odtwórz" in b.focus:
            break
        key("LEFT", pause=0.3)
    p.last = look()
    a = p.last
    key("LEFT", pause=0.5)
    b = look()
    if b.key() == a.key():
        p.rec("◄ na pierwszym", a, b, 0, "F4", "OK", "bez ruchu")
    elif in_menu(b):
        p.rec("◄ na pierwszym", a, b, 0, "F11", "OK", "otwiera menu")
        key("RIGHT", pause=0.5)
    else:
        p.rec("◄ na pierwszym", a, b, 0, "F4", "BŁĄD", "skok/zawijanie")
    p.last = look()
    step(p, "UP", rule="F10", expect_move=False)


@path(4, "Baner ▼ → pierwszy rząd: plakat w całości widoczny, hero bez skoku [F2 F7, gfxinfo]")
def _(p):
    home(p)
    gfx_reset()
    b = step(p, "DOWN")
    check_vis(p, "▼ do rzędu", None, b, 0)
    step(p, "RIGHT")
    step(p, "RIGHT")
    gfx_take(p, "baner_rzad")


@path(5, "Rząd ►×6 → ▼ → ▲: ten sam plakat [F3]")
def _(p):
    to_row(p, 1)
    for _ in range(6):
        step(p, "RIGHT")
    a = p.last
    step(p, "DOWN")
    b = step(p, "UP")
    p.rec("powrót ▲", a, b, 0, "F3", "OK" if b.focus == a.focus else "BŁĄD", "" if b.focus == a.focus else f"„{b.focus}” zamiast „{a.focus}”")


@path(6, "Rząd 1, 4. plakat ▼ → rząd 2 (konwencja F3/F2) [F2 F3]")
def _(p):
    to_row(p, 1)
    for _ in range(3):
        step(p, "RIGHT")
    a = p.last
    b = step(p, "DOWN")
    col = abs(b.cx() - a.cx()) <= max(a.w(), b.w()) * 0.75
    p.rec("kolumna ▼", a, b, 0, "F2", "OK" if col else "UWAGA", f"Δx={b.cx() - a.cx():.0f} px")
    c = step(p, "UP")
    p.rec("▲ wraca", a, c, 0, "F3", "OK" if c.focus == a.focus else "BŁĄD", "" if c.focus == a.focus else f"„{c.focus}” zamiast „{a.focus}”")


@path(7, "Przytrzymana ▼ 3 s przez rzędy i ▲ do banera [F9, gfxinfo]")
def _(p):
    home(p)
    gfx_reset()
    logcat_clear()
    real = hold("DOWN", 3.0)
    b = look()
    p.rec("▼ 3 s" + ("" if real else " (keyevent)"), None, b, 3, "F9", "OK" if b.focus and vis(b) else "BŁĄD", "")
    hold("UP", 3.0)
    time.sleep(0.6)
    c = look()
    # E2: ▲ z banera otwiera menu (projekt Niuni), więc po 3 s fokus jest na „Odtwórz” albo na górze menu („Zmień profil”) — oba poprawne.
    p.rec("▲ 3 s", b, c, 3, "F9", "OK" if c.focus and ("Odtwórz" in c.focus or in_menu(c)) else "UWAGA", "")
    gfx_take(p, "przytrzymanie_v")
    jank_100(p)


@path(8, "Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku [F4 F7 F9, gfxinfo]")
def _(p):
    to_row(p, 2)
    gfx_reset()
    hold("RIGHT", 3.0)
    time.sleep(0.4)
    b = look()
    check_vis(p, "► 3 s", None, b, 3)
    p.last = b
    walk_to_edge(p, "RIGHT", 25)
    hold("LEFT", 4.0)
    time.sleep(0.4)
    c = look()
    p.rec("◄ 4 s", b, c, 4, "F9", "OK" if c.focus and vis(c) and not in_menu(c) else "UWAGA", "menu" if in_menu(c) else "")
    gfx_take(p, "przytrzymanie_h")
    jank_100(p)


@path(9, "Plakat OK → tytuł → Wstecz: ten sam plakat, to samo przewinięcie [F5 F12, czas]")
def _(p):
    to_row(p, 2)
    step(p, "RIGHT")
    step(p, "RIGHT")
    a = p.last
    ok(p, "OK na plakacie")
    back(p, a)


@path(10, "Długie OK na plakacie → menu → Wstecz: fokus na plakacie, puszczenie nic nie klika [F8 F5]")
def _(p):
    to_row(p, 2)
    step(p, "RIGHT")
    a = p.last
    long_ok(p)
    back(p, a)
    ok(p, "krótkie OK po długim")  # F8: następne krótkie OK działa
    back(p, a)


def open_menu(p, maxn=14):
    """Menu Niuni otwiera ▲ z góry ekranu albo Wstecz, nie ◄ (NiuniaNav.kt; decyzja 24.11 w DECYZJE.md). Zwraca (element, z którego
    wyszliśmy, pierwszy element menu); menu = None, gdy się nie otworzyło."""
    before = p.last if getattr(p, "last", None) else look()
    for _ in range(maxn):
        key("UP", pause=0.3)
        s = look()
        if in_menu(s):
            p.last = s
            return before, s
        if s.fb is not None:
            before = s
    p.note("UWAGA", f"menu nie otwarte ▲×{maxn} (fokus „{before.focus}”)")
    return before, None


@path(11, "Pierwszy plakat: ◄ bez zawijania; menu otwiera ▲ z góry ekranu, fokus na „Główna” [F4 F11]")
def _(p):
    to_row(p, 2)
    walk_to_edge(p, "LEFT", 12)  # F4: ◄ na krawędzi nic nie robi (menu otwiera ▲ i Wstecz — projekt Niuni)
    before, m = open_menu(p)
    p.rec("▲ do menu", before, m or before, 0, "F11", "OK" if m and m.focus and "Główna" in m.focus else "BŁĄD", f"fokus „{m.focus if m else None}”")
    if m:
        p.last = m


@path(12, "Menu ▼ do końca i ▲ bez zawijania, ► → element, z którego wyszło [F4 F11]")
def _(p):
    to_row(p, 1)
    before, m = open_menu(p)
    if m is None:
        return
    walk_to_edge(p, "DOWN", 12)
    walk_to_edge(p, "UP", 12)
    seek_text(p, ["Główna"], "DOWN", 10)
    b = step(p, "RIGHT", expect_move=False)
    # „element, z którego wyszło” = ostatni element treści przed ▲ do menu (E2: wcześniej błędnie pierwszy plakat po starcie ścieżki)
    p.rec("► z menu", before, b, 0, "F11", "OK" if b.focus == before.focus else "BŁĄD", "" if b.focus == before.focus else f"„{b.focus}” zamiast „{before.focus}”")


def menu_go(p, word):
    if in_menu(p.last):
        s = p.last
    else:
        before, s = open_menu(p)
        if getattr(p, "menu_from", None) is None:
            p.menu_from = before
        if s is None:
            return False
    for d in ("UP", "DOWN"):
        for _ in range(10):
            if s.focus and word in s.focus:
                break
            key(d, pause=0.3)
            s = look()
        if s.focus and word in s.focus:
            break
    if not (s.focus and word in s.focus):
        p.note("UWAGA", f"menu: brak pozycji „{word}”")
        return False
    p.last = s
    a, b = ok(p, f"menu → {word}")
    if in_menu(b):
        p.rec(f"{word}: pierwszy fokus", a, b, 0, "F1", "BŁĄD", f"fokus w menu („{b.focus}”)")
    return True


@path(13, "Menu → Szukaj → Biblioteka → Live TV → Główna: pierwszy fokus w treści, Główna przywraca fokus [F1 F5, czas]")
def _(p):
    to_row(p, 1)
    step(p, "RIGHT")
    p.menu_from = None
    for w in ("Szukaj", "Biblioteka", "Live TV", "Główna"):
        menu_go(p, w)
    src = p.menu_from or p.last  # element, z którego wyszliśmy z Głównej (ostatni przed ▲ do menu)
    b = p.last
    p.rec("Główna: powrót", src, b, 0, "F5", "OK" if b.focus == src.focus else "UWAGA", "" if b.focus == src.focus else f"„{b.focus}” zamiast „{src.focus}”")


@path(14, "Wstecz z głębi rzędu: baner/menu, drugi Wstecz nie zamyka bez pytania [F12]")
def _(p):
    to_row(p, 3)
    back(p, None, "Wstecz 1")
    back(p, None, "Wstecz 2")
    s = p.last
    if s.has("Zamknąć", "Wyjść", "Exit", "Zamknij"):
        p.rec("pytanie o wyjście", None, s, 0, "F12", "OK", "jest pytanie")
        key("BACK", pause=0.8)  # anuluj pytanie, nie OK
    p.last = look()


@path(15, "Długie OK na pozycji menu: nic się nie psuje [F10]")
def _(p):
    to_row(p, 1)
    open_menu(p)
    long_ok(p, "długie OK w menu", expect=False)
    s = look()
    p.rec("po długim OK", None, s, 0, "F10", "OK" if s.focus and alive() else "BŁĄD", "")
    if not in_menu(s):
        p.last = s


def type_osk(p, word):
    t0 = time.time()
    r = subprocess.run([sys.executable, "-I", os.path.join(ROOT, "fork-tools", "test", "klawisz.py"), word], capture_output=True, text=True, timeout=300)
    s, _ = settle(8)
    p.rec(f"klawiatura „{word}”", None, s, time.time() - t0, "F6", "OK" if "wpisane" in r.stdout else "UWAGA", r.stdout.strip()[-60:])
    p.last = s
    return s


@path(16, "Szukaj: „bat” klawiaturą ekranową; fokus na klawiszu, wyniki bez kradzieży fokusu [F1 F6]")
def _(p):
    s = open_route(p, "search")
    s = type_osk(p, "bat")
    time.sleep(3)
    b = look()
    p.rec("wyniki po 3 s", s, b, 3, "F6", "OK" if b.key() == s.key() else "BŁĄD", "wyniki: " + ("tak" if b.has("Batman", "Bat") else "brak"))


@path(17, "Szukaj: z klawiatury do wyników (liczba ►), ◄ wraca na ten sam klawisz [F2 F3, gfxinfo]")
def _(p):
    open_route(p, "search")
    type_osk(p, "bat")
    gfx_reset()
    n, prev = 0, p.last
    for i in range(12):
        key("RIGHT", pause=0.3)
        s = look()
        n += 1
        if s.fb and s.w() > 150:  # wynik (karta), nie klawisz
            p.rec(f"►×{n} do wyników", None, s, 0, "F2", "OK" if n <= 6 else "UWAGA", f"{n} naciśnięć" + (" (> 6 — propozycja skrótu)" if n > 6 else ""))
            p.metric("► do wyników", n)
            p.last = s
            c = step(p, "LEFT")
            p.rec("◄ z wyników", prev, c, 0, "F3", "OK" if c.focus == prev.focus else "BŁĄD", "" if c.focus == prev.focus else f"„{c.focus}” zamiast „{prev.focus}”")
            break
        prev = s
    else:
        p.note("UWAGA", "nie doszedł do wyników ►×12")
    gfx_take(p, "szukaj")


@path(18, "Szukaj: wynik OK → tytuł → Wstecz: fokus na wyniku [F5]")
def _(p):
    open_route(p, "search")
    type_osk(p, "bat")
    s = seek(p, lambda x: x.fb is not None and x.w() > 150, "RIGHT", 12, "wynik")
    if not s:
        return
    ok(p, "OK na wyniku")
    back(p, s)


@path(19, "Szukaj: polecane — długie OK → menu → Wstecz [F8 F5]")
def _(p):
    open_route(p, "search")
    # E2: wcześniej ►×6 trafiało w chip „Odkrywaj” (szeroki, nie plakat); plakat polecany = pionowy kształt
    s = seek(p, is_poster, "DOWN", 6, "polecany plakat") or seek(p, is_poster, "RIGHT", 12, "polecany plakat")
    if not s:
        return
    long_ok(p)
    back(p, s)


@path(20, "Szukaj: kasowanie liter do pustego pola, fokus nie spada w niewidoczne miejsce [F6 F7]")
def _(p):
    open_route(p, "search")
    type_osk(p, "bat")
    s = look()
    root = s.root
    # klawisz kasowania: tekst „⌫”/„Usuń”/„Kasuj” albo opis
    words = ("⌫", "Usuń", "Kasuj", "Backspace", "Delete")
    if not any(w in " ".join(s.texts) for w in words):
        p.note("UWAGA", "brak klawisza kasowania w odczycie")
        return
    s = seek_text(p, list(words), "DOWN", 8) or seek_text(p, list(words), "RIGHT", 8)
    if not s:
        return
    for i in range(4):
        key("CENTER", pause=0.6)
    b, _ = settle(5)
    check_vis(p, "po skasowaniu", s, b, 0)
    p.rec("wyniki znikają", s, b, 0, "F6", "OK" if not b.has("Batman") else "UWAGA", "")


@path(21, "Biblioteka: chipy ▼ do treści, ▲ na wybrany chip [F3]")
def _(p):
    s = open_route(p, "library")
    step(p, "RIGHT")
    chip = p.last
    step(p, "DOWN")
    b = step(p, "UP")
    p.rec("▲ na chip", chip, b, 0, "F3", "OK" if b.focus == chip.focus else "BŁĄD", "" if b.focus == chip.focus else f"„{b.focus}” zamiast „{chip.focus}”")


@path(22, "Biblioteka: Historia — OK → tytuł → Wstecz; długie OK na pozycji [F5 F8]")
def _(p):
    open_route(p, "library")
    if not seek_text(p, ["Historia"], "RIGHT", 8):
        p.note("POMINIĘTA", "brak zakładki „Historia”")
        return
    key("CENTER", pause=1.5)
    s = step(p, "DOWN")
    if not s.fb:
        return
    ok(p, "OK na pozycji historii")
    back(p, s)
    long_ok(p)
    back(p, s)


@path(23, "Biblioteka: pusta lista — fokus na przycisku stanu pustego [F1 F10]")
def _(p):
    open_route(p, "library")
    for i in range(8):
        key("RIGHT", pause=0.3)
        key("CENTER", pause=1.2)
        s = look()
        if s.has("Nic tu", "pusta", "Brak", "Przeglądaj"):
            key("DOWN", pause=0.5)
            b = look()
            p.rec("pusta lista ▼", s, b, 0, "F1", "OK" if b.fb and b.w() > 100 else "UWAGA", f"fokus „{b.focus}”")
            return
    p.note("POMINIĘTA", "brak pustej listy w Bibliotece")


DETAIL_MOVIE = "detail/tt15398776/movie"     # Oppenheimer
DETAIL_SERIES = "detail/tt0903747/series"    # Breaking Bad


def detail(p, route=DETAIL_MOVIE):
    s = open_route(p, route, rule="F1")
    if s.focus and not re.sub(r"^n:", "", s.focus).startswith(("Odtwórz", "Wznów")):
        p.rec("pierwszy fokus tytułu", None, s, 0, "F1", "UWAGA", f"„{s.focus}” zamiast „Odtwórz”")
    return s


@path(24, "Film: fokus na „Odtwórz” w budżecie; ◄► po przyciskach bez zawijania [F1 F4, czas]")
def _(p):
    detail(p)
    walk_to_edge(p, "RIGHT", 8)
    walk_to_edge(p, "LEFT", 8)


@path(25, "„Więcej” → „Mniej”: fokus zostaje na przycisku po zmianie układu [F6]")
def _(p):
    detail(p)
    s = seek_text(p, ["Więcej"], "DOWN", 6) or seek_text(p, ["Więcej"], "RIGHT", 6)
    if not s:
        return
    key("CENTER", pause=1.0)
    b = look()
    p.rec("OK „Więcej”", s, b, 0, "F6", "OK" if b.focus and "Mniej" in b.focus else "BŁĄD", f"fokus „{b.focus}”")
    key("CENTER", pause=1.0)
    c = look()
    p.rec("OK „Mniej”", b, c, 0, "F6", "OK" if c.focus and "Więcej" in c.focus else "BŁĄD", f"fokus „{c.focus}”")


@path(26, "Serial: ▼ pigułki sezonów, ► sezon, ▼ odcinki, ▲ na wybraną pigułkę [F3 F6]")
def _(p):
    detail(p, DETAIL_SERIES)
    s = seek_text(p, ["Sezon 1", "Sezon"], "DOWN", 6)
    if not s:
        return
    b = step(p, "RIGHT")
    key("CENTER", pause=1.5)
    pill = look()
    p.last = pill
    step(p, "DOWN")
    c = step(p, "UP")
    p.rec("▲ na pigułkę", pill, c, 0, "F3", "OK" if c.focus == pill.focus else "BŁĄD", "" if c.focus == pill.focus else f"„{c.focus}” zamiast „{pill.focus}”")
    # przywróć sezon 1 (stan widoku, nie ustawienie)
    p.last = c


@path(27, "Tabela sezon × odcinek (Z-8): ◄►▲▼ po komórkach, przewijanie trzyma fokus [F2 F7, gfxinfo]")
def _(p):
    detail(p, DETAIL_SERIES)
    s = seek_text(p, ["★"], "RIGHT", 6)
    if not s:
        p.note("POMINIĘTA", "brak ★ (tabela ocen) w hero")
        return
    ok(p, "OK ★ → tabela")
    gfx_reset()
    for d in ("RIGHT", "RIGHT", "DOWN", "DOWN", "DOWN", "LEFT", "UP"):
        b = step(p, d)
        check_vis(p, ARROW[d], None, b, 0)
    gfx_take(p, "tabela")
    back(p, s)


@path(28, "Odcinek OK → wybór źródła → Wstecz: ten odcinek; długie OK = opcje odcinka [F5 F8]")
def _(p):
    detail(p, DETAIL_SERIES)
    s = seek_text(p, ["Odcinek", "Pilot", "E1"], "DOWN", 8)
    if not s:
        return
    ok(p, "OK na odcinku")
    back(p, s)
    long_ok(p)
    back(p, s)


@path(29, "Obsada → Osoba → plakat → tytuł → Wstecz ×3: za każdym razem element źródłowy [F5]")
def _(p):
    detail(p, DETAIL_MOVIE)
    s = None
    for i in range(10):
        key("DOWN", pause=0.4)
        x = look()
        if x.has("Obsada") and x.fb and x.w() < 300 and not is_poster(x):
            s = x
            break
    if not s:
        p.note("UWAGA", "nie znaleziono osoby w obsadzie")
        return
    p.last = s
    ok(p, "OK na osobie")
    person_ok = p.last.has("Filmografia", "Filmy", "Biografia", "Znana")
    if not person_ok:
        back(p, s)
        return
    pp = seek(p, is_poster, "DOWN", 6, "plakat osoby")
    if pp:
        ok(p, "OK na plakacie")
        back(p, pp)
    back(p, s)


@path(30, "Zwiastun: OK → od razu „Wczytuję zwiastun…” → Wstecz na kartę [F5 F12, czas]")
def _(p):
    detail(p, DETAIL_MOVIE)
    s = seek_text(p, ["Zwiastun"], "DOWN", 10)
    if not s:
        p.note("POMINIĘTA", "brak kart zwiastunów")
        return
    t0 = time.time()
    key("CENTER", pause=0.2)
    b = look()
    msg = b.has("Wczytuję")
    p.rec("OK zwiastun", s, b, time.time() - t0, "F12", "OK" if msg or b.key() != s.key() else "UWAGA", "komunikat" if msg else "")
    time.sleep(6)
    p.last = look()
    back(p, s)


@path(31, "Wstecz z głębi strony (▼×6): góra albo wyjście, fokus zawsze widoczny [F7 F12]")
def _(p):
    detail(p, DETAIL_MOVIE)
    key("DOWN", 6, pause=0.35)
    p.last = look()
    b = back(p, None)
    check_vis(p, "po Wstecz", None, b, 0)


def play_cw(p):
    """Odtwarzanie z „Oglądaj dalej” (pozycja już tam jest — nie dokładamy nowych)."""
    s = home(p)
    s = step(p, "DOWN")
    if not s.has("Oglądaj dalej"):
        p.note("POMINIĘTA", "brak „Oglądaj dalej”")
        return None
    return s


@path(32, "Wybór źródła: pierwszy fokus na pierwszym źródle; filtry ▲▼ wracają na wybrany filtr [F1 F3]")
def _(p):
    detail(p, DETAIL_MOVIE)
    a, b = ok(p, "OK „Odtwórz” → źródła")
    if b.has("Buforuję", "Wczytywanie") or not b.has("Źródła", "Filtry", "1080", "720", "4K"):
        p.note("UWAGA", "zamiast listy źródeł: odtwarzanie albo brak listy")
        key("BACK", pause=1.0)
        return
    src = b
    up = step(p, "UP")
    dn = step(p, "DOWN")
    p.rec("▼ z filtrów", src, dn, 0, "F3", "OK" if dn.focus == src.focus else "UWAGA", "")
    back(p, None)


@path(33, "Odtwarzacz: OK nakładka, ▲ do przycisków nad paskiem, ◄►, ▼ na pasek [F3 F4]")
def _(p):
    s = play_cw(p)
    if not s:
        return
    ok(p, "OK „Oglądaj dalej” → odtwarzacz")
    time.sleep(4)
    key("CENTER", pause=1.0)
    b = look()
    p.last = b
    p.rec("nakładka", None, b, 0, "F1", "OK" if b.focus else "UWAGA", f"fokus „{b.focus}”")
    step(p, "UP", expect_move=False)
    step(p, "LEFT", expect_move=False)
    step(p, "RIGHT", expect_move=False)
    step(p, "DOWN", expect_move=False)
    back(p, None)
    back(p, None)


@path(34, "Przewijanie ◄► pojedyncze i przytrzymane; po schowaniu nakładki klawisz nie gubi fokusu [F9 F6]")
def _(p):
    s = play_cw(p)
    if not s:
        return
    ok(p, "OK → odtwarzacz")
    time.sleep(3)
    key("RIGHT", pause=0.6)
    key("LEFT", pause=0.6)
    hold("RIGHT", 1.5)
    time.sleep(5)
    key("DOWN", pause=0.8)
    b = look()
    p.rec("klawisz po schowaniu nakładki", None, b, 0, "F6", "OK" if b.focus else "UWAGA", f"fokus „{b.focus}”")
    p.last = b
    back(p, None)
    back(p, None)


@path(35, "„Zmień źródło” → lista → Wstecz na ten przycisk [F5]")
def _(p):
    s = play_cw(p)
    if not s:
        return
    ok(p, "OK → odtwarzacz")
    time.sleep(3)
    key("CENTER", pause=1.0)
    p.last = look()
    m = seek_text(p, ["Więcej"], "UP", 3) or seek_text(p, ["Więcej"], "RIGHT", 6)
    if m:
        key("CENTER", pause=1.0)
        p.last = look()
    z = seek_text(p, ["Zmień źródło"], "DOWN", 6)
    if not z:
        p.note("UWAGA", "brak „Zmień źródło”")
    else:
        ok(p, "OK „Zmień źródło”")
        back(p, z)
    back(p, None)
    back(p, None)


@path(36, "Wstecz z odtwarzacza → strona/Główna, fokus na źródle; czas pierwszej klatki [F5 F12, czas]")
def _(p):
    s = play_cw(p)
    if not s:
        return
    logcat_clear()
    ok(p, "OK → odtwarzacz")
    time.sleep(6)
    first = [l for l in adb("logcat", "-d", "-v", "time", "-s", "ForkPerf:I", "ForkDiag:I", "ExoPlayerImpl:I").splitlines() if "first" in l.lower() or "pierwsza" in l.lower()]
    p.metric("pierwsza klatka (log)", first[-1][-80:] if first else "brak wpisu")
    back(p, None)
    back(p, s, "Wstecz do Głównej")


def live(p):
    s = open_route(p, "reshaped_live_tv", rule="F1")
    return s


@path(37, "Live TV: wejście — fokus na ostatnim lub pierwszym kanale; czas listy z dysku [F1, czas]")
def _(p):
    s = live(p)
    p.rec("pierwszy fokus Live TV", None, s, 0, "F1", "OK" if s.focus else "BŁĄD", f"„{s.focus}”")


@path(38, "Chip „Kraj i język” → panel → Wstecz: fokus na chipie (bez zmiany wyboru) [F5 F6]")
def _(p):
    live(p)
    key("UP", 3, pause=0.3)
    p.last = look()
    s = seek_text(p, ["Kraj", "Język", "kraje"], "RIGHT", 14)
    if not s:
        p.note("POMINIĘTA", "brak chipa „Kraj i język”")
        return
    a, b = ok(p, "OK chip → panel")
    p.rec("panel: fokus na zaznaczonej", None, b, 0, "F1", "OK" if b.focus and ("✓" in b.focus or "Wszystkie" in b.focus or "Polska" in b.focus) else "UWAGA", f"„{b.focus}”")
    back(p, s)


@path(39, "Lista kanałów: przytrzymana ▼ 5 s i ▲ [F9, gfxinfo]")
def _(p):
    live(p)
    seek(p, lambda x: x.fb is not None and x.w() > 400, "DOWN", 4, "wiersz kanału")
    gfx_reset()
    logcat_clear()
    hold("DOWN", 5.0)
    time.sleep(0.5)
    b = look()
    check_vis(p, "▼ 5 s", None, b, 5)
    hold("UP", 5.0)
    time.sleep(0.5)
    c = look()
    p.rec("▲ 5 s", b, c, 5, "F9", "OK" if c.focus else "BŁĄD", f"„{c.focus}”")
    gfx_take(p, "kanaly")
    jank_100(p)


@path(40, "Kanał OK → odtwarzanie → Wstecz: ten sam kanał (okno „nie odpowiada”: przyciski i Wstecz) [F5 F1 F12]")
def _(p):
    live(p)
    s = seek(p, lambda x: x.fb is not None and x.w() > 400, "DOWN", 4, "wiersz kanału")
    if not s:
        return
    ok(p, "OK kanał")
    time.sleep(7)
    b = look()
    if b.has("nie odpowiada"):
        p.rec("okno błędu", None, b, 0, "F1", "OK" if b.focus and "Następny" in b.focus else "UWAGA", f"„{b.focus}”")
    p.last = b
    back(p, s, "Wstecz z kanału")


@path(41, "Długie OK na kanale → „Opcje kanału” → ▼ po opcjach → Wstecz na kanał [F8 F5]")
def _(p):
    live(p)
    s = seek(p, lambda x: x.fb is not None and x.w() > 400, "DOWN", 4, "wiersz kanału")
    if not s:
        return
    a, b = long_ok(p)
    if not b.has("Opcje kanału"):
        p.rec("panel opcji", None, b, 0, "F8", "BŁĄD", "brak „Opcje kanału”")
    else:
        walk_to_edge(p, "DOWN", 8)
    back(p, s)


@path(42, "Przewodnik EPG: ►◄ w czasie, ▼▲ trzyma czas; podgląd nie kradnie fokusu [F2 F6, gfxinfo]")
def _(p):
    live(p)
    s = seek(p, lambda x: x.fb is not None and x.w() > 400, "DOWN", 4, "wiersz kanału")
    if not s:
        return
    gfx_reset()
    # E2: przewodnik Niuni to wiersze kanałów na całą szerokość (jeden element = kanał + bieżący program, bez osi czasu) — ► w prawo
    # nie ma dokąd iść i nic nie robi (F4); ruch ►◄ po komórkach sprawdzamy tylko, gdy fokus ma szerokość komórki (< 1800 px).
    r = step(p, "RIGHT", expect_move=bool(s.fb and s.w() < 1800))
    d = step(p, "DOWN")
    col = abs(d.fb[0] - r.fb[0]) < 300 if d.fb and r.fb else False
    p.rec("▼ trzyma czas", r, d, 0, "F2", "OK" if col else "UWAGA", f"x {r.fb[0] if r.fb else '?'} → {d.fb[0] if d.fb else '?'}")
    time.sleep(2.5)
    e = look()
    p.rec("podgląd po 2,5 s", d, e, 2.5, "F6", "OK" if e.key() == d.key() else "BŁĄD", "")
    step(p, "UP")
    step(p, "LEFT", expect_move=False)
    gfx_take(p, "epg")


@path(43, "„Szukaj w telewizji”: fokus w polu, wynik OK → Wstecz [F1 F5]")
def _(p):
    s = open_route(p, "fork_tv_search")
    p.rec("fokus w polu", None, s, 0, "F1", "OK" if s.focus and "Szukaj" in s.focus else "UWAGA", f"„{s.focus}”")
    type_osk(p, "tvp")
    r = seek(p, lambda x: x.has("TVP") and x.fb is not None and x.w() > 150 and "TVP" in (x.focus or ""), "DOWN", 8, "wynik TVP")
    if not r:
        return
    ok(p, "OK wynik")
    time.sleep(5)
    p.last = look()
    back(p, r)


@path(44, "Ustawienia: ▼ kategorie, ► treść, ◄ ta sama kategoria; Wygląd → „Szkło” niedostępne [F3 F10]")
def _(p):
    s = open_route(p, "settings")
    step(p, "DOWN")
    cat = p.last
    step(p, "RIGHT", expect_move=False)
    b = step(p, "LEFT", expect_move=False)
    p.rec("◄ na kategorię", cat, b, 0, "F3", "OK" if b.focus == cat.focus else "BŁĄD", "" if b.focus == cat.focus else f"„{b.focus}” zamiast „{cat.focus}”")
    w = seek_text(p, ["Wygląd"], "DOWN", 8)
    if not w:
        return
    step(p, "RIGHT", expect_move=False)
    g = seek_text(p, ["Szkło"], "DOWN", 12)
    if g:
        p.rec("Szkło", None, g, 0, "F10", "OK" if g.has("Niedostępne") else "UWAGA", "„Niedostępne na tym telewizorze”" if g.has("Niedostępne") else "brak napisu „Niedostępne”")
        key("CENTER", pause=0.8)
        x = look()
        p.rec("OK na „Szkło”", g, x, 0, "F10", "OK" if x.focus and not x.has("Mróz", "Czyste szkło") else "UWAGA", "")
        if x.key() != g.key():
            key("BACK", pause=0.8)


@path(45, "Przełącznik OK ×2: stan wraca, fokus zostaje [F6]")
def _(p):
    open_route(p, "playback_settings")
    s = step(p, "DOWN", expect_move=False)
    a = look()
    key("CENTER", pause=1.0)
    b = look()
    key("CENTER", pause=1.0)
    c = look()
    p.rec("OK×2", a, c, 0, "F6", "OK" if c.focus == a.focus and c.sig() == a.sig() else "BŁĄD", "stan/fokus " + ("wrócił" if c.sig() == a.sig() else "INNY — sprawdzić ręcznie"))
    if c.sig() != a.sig() and b.sig() != a.sig():
        p.note("UWAGA", "przełącznik mógł zostać zmieniony — sprawdzić")


@path(46, "Napisy → wygląd: Atkinson Hyperlegible domyślna, na liście Tiresias; lista → Wstecz bez zmiany [F1 F5]")
def _(p):
    s = open_route(p, "playback_settings")
    f = seek_text(p, ["Czcionka"], "DOWN", 25)
    if not f:
        p.note("UWAGA", "brak pozycji „Czcionka”")
        return
    p.rec("czcionka domyślna", None, f, 0, "F1", "OK" if f.has("Atkinson") else "BŁĄD", "Atkinson" if f.has("Atkinson") else "brak Atkinson")
    a, b = ok(p, "OK „Czcionka” → lista")
    p.rec("Tiresias na liście", None, b, 0, "F1", "OK" if b.has("Tiresias") else "BŁĄD", "")
    back(p, f)


@path(47, "YouTube: czas do fokusu w treści, OK → odtwarzacz → Wstecz (kod obcy: pomiar) [F1 F5, czas]")
def _(p):
    s = open_route(p, "youtube", own_menu=True)  # YouTube ma własny pasek „Główna / Subskrypcje…” (kod obcy, [i]) — nie menu Niuni
    m = s
    if not s.focus:
        p.note("UWAGA", "YouTube: brak fokusu po otwarciu (kod obcy — [i])")
    f = seek(p, lambda x: x.fb is not None and x.w() > 200 and x.fb[1] > 150, "DOWN", 4, "film")
    if not f:
        return
    ok(p, "OK film")
    time.sleep(6)
    p.last = look()
    back(p, f)


@path(48, "Profile: ◄► po profilach, (PIN: wejście i Wstecz bez wpisywania) [F1 F5]")
def _(p):
    s = open_route(p, "profiles")
    walk_to_edge(p, "RIGHT", 8)
    walk_to_edge(p, "LEFT", 8)
    # bez OK na profilu bez PIN-u (przełączyłoby profil właściciela)
    s = look()
    for i in range(6):
        if s.has("PIN") or (s.focus and "🔒" in s.focus):
            break
        key("RIGHT", pause=0.3)
        s = look()
    if s.focus and ("🔒" in s.focus or "PIN" in s.focus):
        p.last = s
        ok(p, "OK profil z PIN")
        back(p, s)
    else:
        p.rec("PIN", None, s, 0, "-", "OK", "brak profilu z PIN-em — bez OK (nie przełączamy profilu)")


@path(49, "Odkrywaj / Zobacz wszystko: siatka ▼►, przytrzymana ▼, OK → tytuł → Wstecz na ten sam kafelek [F2 F5 F7 F9, gfxinfo]")
def _(p):
    s = open_route(p, "discover")
    g = seek(p, is_poster, "DOWN", 5, "kafelek siatki")
    if not g:
        return
    gfx_reset()
    step(p, "RIGHT")
    d = step(p, "DOWN")
    hold("DOWN", 2.0)
    time.sleep(0.4)
    h = look()
    check_vis(p, "▼ 2 s", d, h, 2)
    p.last = h
    step(p, "RIGHT")
    t = p.last
    ok(p, "OK kafelek")
    back(p, t)
    gfx_take(p, "siatka")


@path(50, "Kalendarz, Dodatki, Kolejność katalogów, Kolekcje: pierwszy fokus, Wstecz z pozycji na źródło [F1 F5]")
def _(p):
    for r in ("calendar", "addon_manager", "catalog_order", "collections"):
        s = open_route(p, r)
        step(p, "DOWN", expect_move=False)
        src = p.last
        a, b = ok(p, f"{r}: OK", expect_change=False)
        if b.key() != src.key():
            back(p, src, f"{r}: Wstecz")


# ------------------------------------------------------------------ przebieg
def wait_free():
    r = subprocess.run(["bash", os.path.join(ROOT, "fork-tools", "test", "jmgo_czekaj_wolny.sh"), "1", "180"], capture_output=True, text=True)
    print(r.stdout.strip(), flush=True)
    return r.returncode == 0


def guard(state):
    """Przed ścieżką: na wierzchu fork? Dwa razy z rzędu cudza aplikacja = ktoś używa TV."""
    t = top_pkg()
    if t == PKG or t == "":
        state["foreign"] = 0
        return True
    state["foreign"] += 1
    print(f"   ! na wierzchu {t} ({state['foreign']}×)", flush=True)
    if state["foreign"] >= 2:
        print("   ! ktoś używa TV — czekam na WOLNY", flush=True)
        if not wait_free():
            return False
        state["foreign"] = 0
        sh(f"am start -n {LAUNCH}")
        time.sleep(6)
        return True
    time.sleep(20)
    return guard(state)


def main():
    global GFX_DIR
    args = sys.argv[1:]
    cyc, nowait = "0", False
    if "--cykl" in args:
        i = args.index("--cykl"); cyc = args[i + 1]; del args[i:i + 2]
    if "--bez-czekania" in args:
        args.remove("--bez-czekania"); nowait = True
    nums = [int(a) for a in args] or sorted(P)
    out = os.path.join(ROOT, "plan", "ux", "jmgo", "cykl" + cyc)
    GFX_DIR = os.path.join(out, "gfx")
    os.makedirs(GFX_DIR, exist_ok=True)
    if not nowait and not wait_free():
        print("JMGO zajęty — koniec"); return
    adb("logcat", "-b", "crash", "-c")
    adb("logcat", "-G", "8M")
    # Pomiar fokusu (ForkFocusTrace) na czas pętli — intencja debug; na końcu wyłączony (domyślny stan).
    logcat_clear()
    sh(f"am start -a com.nuvio.fork.OPEN -n {ACT} --ez focus_trace true")
    time.sleep(3)
    TRACE["on"] = "pomiar fokusu: włączony" in adb("logcat", "-d", "-s", "ForkFocus:I")
    print(f"ForkFocus: {'jest' if TRACE['on'] else 'brak (APK bez pomiaru — tylko odczyty uiautomator)'}", flush=True)
    res, t0, state = [], time.time(), {"foreign": 0}
    full_log = open(os.path.join(out, "logcat.txt"), "a")
    for n in nums:
        name, fn = P[n]
        if not guard(state):
            print("przerwane: TV zajęty"); break
        print(f"[{datetime.datetime.now():%H:%M:%S}] ścieżka {n}: {name}", flush=True)
        p = Path(n, name)
        try:
            fn(p)
        except Exception as e:  # noqa — błąd skryptu zapisany w raporcie
            p.note("UWAGA", f"wyjątek skryptu {type(e).__name__}: {e}")
        full_log.write(f"\n===== ścieżka {n}\n" + adb("logcat", "-d", "-v", "time", "-s", "ForkFocus:*", "ForkNiunia:*", "ForkJank:*", "ForkPerf:*", "AndroidRuntime:E", "ActivityTaskManager:I"))
        if not alive():
            p.note("BŁĄD", "aplikacja nie działa po ścieżce — restart")
            sh(f"am start -n {LAUNCH}"); time.sleep(8)
        crash = adb("logcat", "-d", "-b", "crash")
        if "FATAL EXCEPTION" in crash and PKG in crash:
            p.note("BŁĄD", "awaria: " + " / ".join(l.strip() for l in crash.splitlines() if "Exception" in l)[:300])
            adb("logcat", "-b", "crash", "-c")
        p.secs = time.time() - p.t0
        print(f"   → {p.verdict} ({p.secs:.0f} s) " + "; ".join(p.notes)[:400], flush=True)
        res.append(p)
        with open(os.path.join(out, "fokus.csv"), "w", newline="") as f:
            w = csv.writer(f); w.writerow(["sciezka", "krok", "klawisz", "fokus_przed", "fokus_po", "czas_s", "regula", "wynik", "uwaga"]); w.writerows(CSV_ROWS)
    full_log.close()
    if TRACE["on"]:
        sh(f"am start -a com.nuvio.fork.OPEN -n {ACT} --ez focus_trace false")
    total = time.time() - t0
    with open(os.path.join(out, "sciezki.json"), "w") as f:
        json.dump([{"nr": p.nr, "nazwa": p.name, "wynik": p.verdict, "uwagi": p.notes, "kroki": p.steps, "miary": p.metrics, "s": round(p.secs)} for p in res], f, ensure_ascii=False, indent=1)
    cnt = {v: sum(1 for p in res if p.verdict == v) for v in ("OK", "UWAGA", "BŁĄD", "POMINIĘTA")}
    with open(os.path.join(out, "sciezki.md"), "w") as f:
        f.write(f"# Ścieżki UX JMGO — cykl {cyc} ({datetime.datetime.now():%d.%m %H:%M}, {DEV}, {total / 60:.1f} min)\n\n")
        f.write(f"OK {cnt['OK']}, UWAGA {cnt['UWAGA']}, BŁĄD {cnt['BŁĄD']}, POMINIĘTA {cnt['POMINIĘTA']}\n\n")
        f.write("| # | Ścieżka | Wynik | s | Uwagi |\n|---|---|---|---|---|\n")
        for p in res:
            f.write(f"| {p.nr} | {p.name.split('[')[0].strip()} | **{p.verdict}** | {p.secs:.0f} | {'; '.join(p.notes)[:300].replace('|', '/')} |\n")
        f.write("\n## Kroki\n\n")
        for p in res:
            f.write(f"### {p.nr}. {p.name} — {p.verdict}\n")
            if p.metrics:
                f.write(f"- miary: `{json.dumps(p.metrics, ensure_ascii=False)}`\n")
            f.write("\n".join("- " + s for s in p.steps) + "\n\n")
        if TRACE["on"]:
            df, dfo, bk = STATS["dtFrame"], STATS["dtFocus"], STATS["back"]
            f.write("\n## Pomiar fokusu (ForkFocus)\n\n")
            f.write(f"- strzałka → klatka (dtFrame): n={len(df)}, mediana {pct(df, 50)} ms, p95 {pct(df, 95)} ms, max {max(df) if df else None} ms (cel ≤ 50 / ≤ 100)\n")
            f.write(f"- strzałka → onFocusChanged (dtFocus): mediana {pct(dfo, 50)} ms, p95 {pct(dfo, 95)} ms\n")
            f.write(f"- Wstecz → fokus (dtFrame): n={len(bk)}, mediana {pct(bk, 50)} ms, p95 {pct(bk, 95)} ms (cel ≤ 300)\n")
            ff = [v for _, v in STATS["firstFocus"]]
            f.write(f"- pierwszy fokus na ekranie (firstFocus): n={len(ff)}, mediana {pct(ff, 50)} ms, p95 {pct(ff, 95)} ms, max {max(ff) if ff else None} ms (cel ≤ 600)\n")
            for lab, v in sorted(STATS["firstFocus"], key=lambda x: -x[1])[:8]:
                f.write(f"  - {lab}: {v} ms\n")
            json.dump(STATS, open(os.path.join(out, "forkfocus.json"), "w"), ensure_ascii=False)
    d = DUMPST
    odczyty = (f"odczyty ekranu: {d['n']} (czytnik {d['fast']}, zwykły {d['legacy']}, błędy czytnika {d['fail']}), "
               f"razem {d['s'] / 60:.1f} min = {100 * d['s'] / max(total, 1):.0f}% czasu, średnio {d['s'] / max(d['n'], 1):.2f} s")
    with open(os.path.join(out, "sciezki.md"), "a") as f:
        f.write(f"\n## Czas testu\n\n- {odczyty}; klawisze przez czytnik: {d['keys']}\n")
    print(odczyty)
    print(f"koniec: {total / 60:.1f} min, {cnt}, raport {out}/sciezki.md")


if __name__ == "__main__":
    main()
