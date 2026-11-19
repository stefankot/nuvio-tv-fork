#!/usr/bin/env python3
"""Obchód pilotem: dla każdej trasy naciska TYLKO strzałki (nigdy OK), zapisuje, gdzie trafia fokus,
i porównuje to z elementami, które można zaznaczyć. Wynik: elementy nieosiągalne, utrata fokusu,
czas ładowania ekranu, błędy z logcat (obrazki, sieć, awarie).

Użycie:  DEV=<ip:port|emulator-5554> python3 crawl.py [trasa ...] [--steps 120]
Bez tras: lista DEFAULT_ROUTES. Wyniki: fork-tools/uitest/wyniki/<data>/<trasa>.json + raport.md
Tylko build debug (com.nuviodebug.com): trasy otwiera intencja com.nuvio.fork.OPEN (ForkHooks.onDebugIntent).
"""
import collections, datetime, json, os, re, subprocess, sys, time
import xml.etree.ElementTree as ET

ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
DEV = os.environ.get("DEV", "")
PKG = "com.nuviodebug.com"
ACT = PKG + "/com.nuvio.tv.MainActivity"
DIRS = ["DOWN", "RIGHT", "UP", "LEFT"]
REV = {"DOWN": "UP", "UP": "DOWN", "LEFT": "RIGHT", "RIGHT": "LEFT"}
DEFAULT_ROUTES = ["home", "search", "discover", "library", "settings", "playback_settings", "layout_settings",
                  "reshaped_live_tv", "fork_tools", "fork_modules", "fork_tv_search", "fork_discover", "fork_picks",
                  "addon_manager", "catalog_order", "collections", "calendar", "youtube"]
HERE = os.path.dirname(os.path.abspath(__file__))
# Wzorce błędów w logcat warte zgłoszenia (obrazki, sieć, wyjątki, ANR).
LOG_PAT = re.compile(r"(Coil|ImageLoader|HttpException|UnknownHost|SocketTimeout|Exception|FATAL|ANR in|"
                     r"failed to load|Failed to load|404|403|500)", re.I)


def adb(*a, out=True, timeout=30):
    cmd = [ADB] + (["-s", DEV] if DEV else []) + list(a)
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        return r.stdout if out else r.returncode
    except subprocess.TimeoutExpired:
        return ""


def key(k):
    adb("shell", "input", "keyevent", "KEYCODE_DPAD_" + k if k in REV else "KEYCODE_" + k)


def dump(retries=3):
    for _ in range(retries):
        x = adb("exec-out", "uiautomator", "dump", "--compressed", "/dev/tty")
        i = x.find("<?xml")
        j = x.rfind("</hierarchy>")
        if i >= 0 and j > i:
            try:
                return ET.fromstring(x[i:j + 12])
            except ET.ParseError:
                pass
        time.sleep(0.7)
    return None


def bounds(n):
    m = re.findall(r"\d+", n.get("bounds", ""))
    return tuple(map(int, m)) if len(m) == 4 else (0, 0, 0, 0)


def label(n):
    """Nazwa elementu: jego tekst/opis albo teksty potomków (Compose: przycisk = Surface + Text w środku)."""
    own = (n.get("text") or n.get("content-desc") or "").strip()
    if own:
        return own[:60]
    parts = []
    for c in n.iter():
        t = (c.get("text") or c.get("content-desc") or "").strip()
        if t and t not in parts:
            parts.append(t)
        if len(" | ".join(parts)) > 60:
            break
    return " | ".join(parts)[:60]


def ident(n):
    lb = label(n)
    b = bounds(n)
    size = "%dx%d" % (b[2] - b[0], b[3] - b[1])
    # Bez nazwy: rozpoznajemy po rozmiarze i pozycji (przewijanie może je przesunąć — zaznaczone w raporcie).
    return lb if lb else "[bez nazwy %s @%d,%d]" % (size, b[0], b[1])


def analyse(root):
    """-> (pakiet, fokus:str|None, zbiór elementów do zaznaczenia, podpis ekranu)."""
    pkgs = collections.Counter(n.get("package") for n in root.iter("node"))
    pkg = pkgs.most_common(1)[0][0] if pkgs else ""
    foc, focusables, texts = None, {}, set()
    for n in root.iter("node"):
        if n.get("package") != PKG:
            continue
        b = bounds(n)
        if b[2] - b[0] < 4 or b[3] - b[1] < 4:
            continue
        t = n.get("text")
        if t:
            texts.add(t)
        if n.get("focused") == "true":
            foc = ident(n)
        if n.get("focusable") == "true" and n.get("enabled") == "true":
            focusables[ident(n)] = b
    return pkg, foc, focusables, texts


def app_pid():
    return adb("shell", "pidof", PKG).strip().split(" ")[0]


def open_route(route):
    adb("logcat", "-c")
    t0 = time.time()
    adb("shell", "am", "start", "-a", "com.nuvio.fork.OPEN", "-n", ACT, "--es", "route", route)
    # Ekran gotowy = dwa kolejne odczyty z tym samym zestawem tekstów i jest fokus (max 25 s).
    prev, stable_at, root = None, None, None
    while time.time() - t0 < 25:
        root = dump()
        if root is None:
            continue
        pkg, foc, fs, texts = analyse(root)
        sig = (foc, frozenset(texts))
        if sig == prev and pkg == PKG:
            stable_at = stable_at or time.time()
            break
        prev = sig
    return root, (round(time.time() - t0, 1) if stable_at else None)


def crawl(route, steps):
    res = {"trasa": route, "czas_ladowania_s": None, "kroki": 0, "elementy": {}, "odwiedzone": [],
           "utrata_fokusu": [], "wyjscie_z_ekranu": [], "pulapki": [], "krawedzie": []}
    root, t = open_route(route)
    res["czas_ladowania_s"] = t
    if root is None:
        res["blad"] = "brak odczytu ekranu"
        return res
    pkg, foc, fs, texts = analyse(root)
    if pkg != PKG:
        res["blad"] = "na wierzchu inna aplikacja: " + pkg
        return res
    elements = dict(fs)
    res["teksty_startowe"] = sorted(texts)[:80]
    if foc is None:
        res["utrata_fokusu"].append({"po": "start"})
    graph = collections.defaultdict(dict)  # stan -> kierunek -> stan
    visited = {foc} if foc else set()
    cur = foc
    n = 0
    stuck_paths = 0
    while n < steps:
        todo = [d for d in DIRS if d not in graph[cur]]
        if not todo:
            # Najbliższy znany stan z niewypróbowanym kierunkiem (BFS po krawędziach).
            q, seen, path = collections.deque([(cur, [])]), {cur}, None
            while q:
                s, p = q.popleft()
                if any(d not in graph[s] for d in DIRS):
                    path = p
                    break
                for d, s2 in graph[s].items():
                    if s2 not in seen and s2 is not None:
                        seen.add(s2)
                        q.append((s2, p + [d]))
            if not path:
                break
            ok = True
            for d in path:
                key(d)
                n += 1
                r = dump()
                if r is None:
                    ok = False
                    break
                _, f2, fs2, _ = analyse(r)
                elements.update(fs2)
                cur = f2
            if not ok:
                break
            stuck_paths += 1
            if stuck_paths > 25:
                break
            continue
        d = todo[0]
        key(d)
        n += 1
        r = dump()
        if r is None:
            graph[cur][d] = cur
            continue
        pkg2, f2, fs2, _ = analyse(r)
        if pkg2 != PKG:
            res["wyjscie_z_ekranu"].append({"z": cur, "klawisz": d, "do": pkg2})
            key("BACK")
            graph[cur][d] = cur
            continue
        elements.update(fs2)
        if f2 is None:
            res["utrata_fokusu"].append({"z": cur, "klawisz": d})
        graph[cur][d] = f2
        res["krawedzie"].append([cur, d, f2])
        if f2:
            visited.add(f2)
        cur = f2
    res["kroki"] = n
    # Pułapka: stan, z którego żadna strzałka nie prowadzi gdzie indziej, a są jeszcze inne elementy.
    for s, e in graph.items():
        if s and len(e) == 4 and all(v == s for v in e.values()) and len(elements) > 1:
            res["pulapki"].append(s)
    res["elementy"] = {k: list(v) for k, v in elements.items()}
    res["odwiedzone"] = sorted(v for v in visited if v)
    res["nieosiagalne"] = sorted(k for k in elements if k not in visited)
    res["logcat"] = logcat_issues()
    return res


def logcat_issues():
    pid = app_pid()
    raw = adb("logcat", "-d", "-v", "brief", "*:W") if not pid else adb("logcat", "-d", "-v", "brief", "--pid", pid, "*:W")
    out, seen = [], set()
    for line in raw.splitlines():
        if not LOG_PAT.search(line):
            continue
        k = re.sub(r"\d+", "#", line)[:140]
        if k in seen:
            continue
        seen.add(k)
        out.append(line[:300])
        if len(out) >= 40:
            break
    return out


def report(results, outdir):
    L = ["# Obchód pilotem — %s, urządzenie %s\n" % (datetime.datetime.now().strftime("%Y-%m-%d %H:%M"), DEV or "domyślne"),
         "Tylko strzałki (bez OK). „Nieosiągalne” = element do zaznaczenia widoczny na ekranie, do którego fokus nie doszedł "
         "w limicie kroków (w długich listach część to po prostu elementy dalej w liście — sprawdzić ręcznie).\n",
         "| Ekran | Ładowanie [s] | Kroki | Elementy | Odwiedzone | Nieosiągalne | Utrata fokusu | Błędy w logach |",
         "|---|---|---|---|---|---|---|---|"]
    for r in results:
        L.append("| %s | %s | %d | %d | %d | %d | %d | %d |" % (
            r["trasa"], r.get("czas_ladowania_s") if r.get("czas_ladowania_s") is not None else "**>25**",
            r.get("kroki", 0), len(r.get("elementy", {})), len(r.get("odwiedzone", [])),
            len(r.get("nieosiagalne", [])), len(r.get("utrata_fokusu", [])), len(r.get("logcat", []))))
    for r in results:
        L.append("\n## %s\n" % r["trasa"])
        if r.get("blad"):
            L.append("- BŁĄD: " + r["blad"])
            continue
        for k in r.get("nieosiagalne", [])[:30]:
            L.append("- nieosiągalne: `%s`" % k)
        for u in r.get("utrata_fokusu", [])[:10]:
            L.append("- utrata fokusu: %s" % u)
        for p in r.get("pulapki", []):
            L.append("- pułapka (strzałki nic nie zmieniają): `%s`" % p)
        for w in r.get("wyjscie_z_ekranu", []):
            L.append("- strzałka wyprowadziła z aplikacji: %s" % w)
        for l in r.get("logcat", [])[:15]:
            L.append("- log: `%s`" % l.replace("`", "'"))
    open(os.path.join(outdir, "raport.md"), "w").write("\n".join(L) + "\n")


def main():
    args = sys.argv[1:]
    steps = 120
    if "--steps" in args:
        i = args.index("--steps")
        steps = int(args[i + 1])
        del args[i:i + 2]
    routes = args or DEFAULT_ROUTES
    outdir = os.path.join(HERE, "wyniki", datetime.datetime.now().strftime("%Y%m%d-%H%M") + "-" + (DEV.split(":")[0] or "dev"))
    os.makedirs(outdir, exist_ok=True)
    results = []
    for route in routes:
        t = time.time()
        r = crawl(route, steps)
        r["czas_obchodu_s"] = round(time.time() - t)
        results.append(r)
        json.dump(r, open(os.path.join(outdir, route + ".json"), "w"), ensure_ascii=False, indent=1)
        print("%-20s ładowanie=%s kroki=%d elementy=%d odwiedzone=%d nieosiągalne=%d utrata=%d logi=%d %s" % (
            route, r.get("czas_ladowania_s"), r.get("kroki", 0), len(r.get("elementy", {})), len(r.get("odwiedzone", [])),
            len(r.get("nieosiagalne", [])), len(r.get("utrata_fokusu", [])), len(r.get("logcat", [])), r.get("blad", "")), flush=True)
        report(results, outdir)
    print("raport:", os.path.join(outdir, "raport.md"))


if __name__ == "__main__":
    main()
