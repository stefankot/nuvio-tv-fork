#!/usr/bin/env python3
"""Generator tokenów skórki Niunia z design systemu Nuvio TV (tokens.json, v2).

Użycie (z katalogu NuvioFork):
    python3 fork-tools/niunia/gen_tokens.py [ścieżka/do/tokens.json]

Domyślne źródło: ~/Desktop/nuvio-tv-interface/design-system/tokens.json.
Wynik:
  - app/src/main/java/com/nuvio/tv/skin/niunia/tokens/NiuniaDesignTokens.kt (wygenerowany, nie edytować ręcznie)
  - app/src/test/resources/niunia/tokens.json (kopia źródła dla testu zgodności NiuniaTokensTest)
Zmiana tokenów = jedna komenda.
"""
import json
import os
import re
import shutil
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = sys.argv[1] if len(sys.argv) > 1 else os.path.expanduser("~/Desktop/nuvio-tv-interface/design-system/tokens.json")
OUT = os.path.join(ROOT, "app/src/main/java/com/nuvio/tv/skin/niunia/tokens/NiuniaDesignTokens.kt")
TEST_COPY = os.path.join(ROOT, "app/src/test/resources/niunia/tokens.json")


def camel(name: str) -> str:
    parts = re.split(r"[-_]", name)
    out = "".join(p[:1].upper() + p[1:] for p in parts if p)
    return ("T" + out) if out[:1].isdigit() else out


def parse_color(v: str) -> int:
    """CSS kolor → ARGB (int)."""
    v = v.strip()
    m = re.fullmatch(r"#([0-9a-fA-F]{6})", v)
    if m:
        return 0xFF000000 | int(m.group(1), 16)
    m = re.fullmatch(r"#([0-9a-fA-F]{3})", v)
    if m:
        r, g, b = (int(c * 2, 16) for c in m.group(1))
        return 0xFF000000 | (r << 16) | (g << 8) | b
    m = re.fullmatch(r"rgba?\(([^)]*)\)", v)
    if m:
        nums = [x.strip() for x in m.group(1).split(",")]
        r, g, b = (int(float(x)) for x in nums[:3])
        a = float(nums[3]) if len(nums) > 3 else 1.0
        return (round(a * 255) << 24) | (r << 16) | (g << 8) | b
    if v == "transparent":
        return 0
    raise ValueError(f"nieznany kolor: {v}")


def px(v: str) -> float:
    return float(v.strip().replace("px", ""))


def hexc(argb: int) -> str:
    return f"Color(0x{argb:08X})"


def split_top(s: str):
    """Dzieli po przecinkach na najwyższym poziomie nawiasów."""
    out, depth, cur = [], 0, ""
    for ch in s:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur.strip()); cur = ""
        else:
            cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out


def parse_gradient(v: str):
    """linear-gradient / radial-gradient → (rodzaj, kąt, [(pozycja|None, ARGB)])."""
    m = re.fullmatch(r"(linear|radial)-gradient\((.*)\)", v.strip())
    if not m:
        raise ValueError(f"nieznany gradient: {v}")
    kind, body = m.group(1), m.group(2)
    args = split_top(body)
    angle = 180.0
    if kind == "linear" and re.fullmatch(r"-?[\d.]+deg", args[0]):
        angle = float(args[0][:-3]); args = args[1:]
    elif kind == "radial" and not re.match(r"(#|rgb|transparent)", args[0]):
        args = args[1:]  # kształt i środek: Kotlin rysuje radialny od środka obszaru
    stops = []
    for a in args:
        mm = re.fullmatch(r"(.*?)(?:\s+(-?[\d.]+)%)?", a.strip())
        stops.append((float(mm.group(2)) / 100 if mm.group(2) else None, parse_color(mm.group(1))))
    # Uzupełnij brakujące pozycje równomiernie (jak CSS).
    if stops[0][0] is None:
        stops[0] = (0.0, stops[0][1])
    if stops[-1][0] is None:
        stops[-1] = (1.0, stops[-1][1])
    i = 0
    while i < len(stops):
        if stops[i][0] is None:
            j = i
            while stops[j][0] is None:
                j += 1
            a, b = stops[i - 1][0], stops[j][0]
            for k in range(i, j):
                stops[k] = (a + (b - a) * (k - i + 1) / (j - i + 1), stops[k][1])
            i = j
        i += 1
    return kind, angle, stops


def main():
    with open(SRC, encoding="utf-8") as f:
        t = json.load(f)
    L = []
    w = L.append
    w("// WYGENEROWANE przez fork-tools/niunia/gen_tokens.py z design-system/tokens.json (Nuvio TV v%s). Nie edytuj ręcznie." % t.get("version", "?"))
    w("@file:Suppress(\"unused\", \"MemberVisibilityCanBePrivate\")")
    w("")
    w("package com.nuvio.tv.skin.niunia.tokens")
    w("")
    for imp in ["androidx.compose.animation.core.CubicBezierEasing", "androidx.compose.ui.graphics.Brush", "androidx.compose.ui.graphics.Color",
                "androidx.compose.ui.geometry.Offset", "androidx.compose.ui.text.TextStyle", "androidx.compose.ui.text.font.FontWeight",
                "androidx.compose.ui.unit.Dp", "androidx.compose.ui.unit.dp", "androidx.compose.ui.unit.sp"]:
        w("import " + imp)
    w("")
    w("/** Plansza 960×540 dp: 1 px projektu = 1 dp = 1 sp (1 dp = 2 px przy 1080p). Tylko ciemny motyw. */")
    w("const val NIUNIA_DS_VERSION = \"%s\"" % t.get("version", "?"))
    w("")

    # --- kolory (z rozwiązaniem odwołań {nazwa}) ---
    raw = {c["name"]: c["value"] for c in t["color"]["tokens"]}

    def resolve(n, seen=()):
        v = raw[n]
        m = re.fullmatch(r"\{(.+)\}", v)
        if m:
            if m.group(1) in seen:
                raise ValueError("pętla odwołań: " + n)
            return resolve(m.group(1), seen + (n,))
        return parse_color(v)

    colors = {n: resolve(n) for n in raw}
    w("/** Kolory (`color`). Role (inverse-surface, secondary-container …) to te same wartości co tokeny bazowe. */")
    w("object NiuniaColor {")
    for c in t["color"]["tokens"]:
        w("    /** %s */" % c.get("usage", "").replace("*/", "* /"))
        w("    val %s = %s" % (camel(c["name"]), hexc(colors[c["name"]])))
    w("}")
    w("")

    def dims(obj, group, unit="dp"):
        w("object %s {" % obj)
        for x in t[group]["tokens"]:
            w("    /** %s */" % x.get("usage", "").replace("*/", "* /"))
            w("    val %s = %s.%s" % (camel(x["name"]), fmt(px(x["value"])), unit))
        w("}")
        w("")

    def fmt(f):
        return str(int(f)) if f == int(f) else repr(f)

    w("/** Odstępy (`spacing`). */"); dims("NiuniaSpacing", "spacing")
    w("/** Promienie (`radius`); RadiusFull = pigułka. */"); dims("NiuniaRadius", "radius")
    w("/** Rozmiary (`size`). */"); dims("NiuniaSize", "size")

    # --- typografia ---
    w("/** Skala typografii (`type`), Satoshi 500/700. */")
    w("object NiuniaType {")
    w("    val Family = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily")
    for g in t["type"]["groups"]:
        for s in g["styles"]:
            wt = "FontWeight.Bold" if int(s["fontWeight"]) >= 700 else "FontWeight.Medium"
            w("    /** %s */" % s.get("usage", ""))
            w("    val %s = TextStyle(fontFamily = Family, fontSize = %s.sp, lineHeight = %s.sp, fontWeight = %s, color = NiuniaColor.Text)"
              % (camel(s["name"]), fmt(px(s["fontSize"])), fmt(px(s["lineHeight"])), wt))
    w("}")
    w("")

    # --- ruch, skala, przezroczystość ---
    w("/** Czas i krzywa (`time`). */")
    w("object NiuniaMotion {")
    for x in t["time"]["tokens"]:
        v = x["value"]
        if v.endswith("ms"):
            w("    /** %s */" % x.get("usage", ""))
            w("    const val %s = %s" % (camel(x["name"]), fmt(float(v[:-2]))))
        else:
            m = re.fullmatch(r"cubic-bezier\(([^)]*)\)", v)
            a, b, c, d = (float(z) for z in m.group(1).split(","))
            w("    /** %s */" % x.get("usage", ""))
            w("    val %s = CubicBezierEasing(%sf, %sf, %sf, %sf)" % (camel(x["name"]), a, b, c, d))
    w("}")
    w("")
    w("/** Skale i uniesienie (`scale`). */")
    w("object NiuniaScale {")
    for x in t["scale"]["tokens"]:
        v = x["value"]
        w("    /** %s */" % x.get("usage", ""))
        if v.endswith("px"):
            w("    val %s = (%s).dp" % (camel(x["name"]), fmt(px(v))))
        else:
            w("    const val %s = %sf" % (camel(x["name"]), float(v)))
    w("}")
    w("")
    w("/** Przezroczystości (`opacity`). */")
    w("object NiuniaOpacity {")
    for x in t["opacity"]["tokens"]:
        w("    /** %s */" % x.get("usage", ""))
        w("    const val %s = %sf" % (camel(x["name"]), float(x["value"])))
    w("}")
    w("")

    # --- cienie ---
    w("/** Cień: przesunięcie Y, rozmycie, kolor; [ring] = obwódka (0 = brak). */")
    w("data class NiuniaShadowSpec(val y: Dp, val blur: Dp, val color: Color, val ring: Dp = 0.dp, val ringColor: Color = Color.Transparent, val inset: Boolean = false)")
    w("")
    w("object NiuniaShadow {")
    for x in t["shadow"]["tokens"]:
        parts = split_top(x["value"])
        y = blur = 0.0; color = 0; ring = 0.0; ringc = 0; inset = False
        for p in parts:
            pin = p.startswith("inset")
            q = p.replace("inset", "").strip()
            cm = re.search(r"(rgba?\([^)]*\)|#[0-9a-fA-F]{3,6})", q)
            col = parse_color(cm.group(1))
            nums = [px(n) for n in re.findall(r"-?[\d.]+px|\b0\b", q[:cm.start()])]
            nums += [0.0] * (4 - len(nums))
            _, ny, nb, ns = nums[:4]
            if nb == 0 and ns > 0:
                ring, ringc, inset = ns, col, pin
            else:
                y, blur, color = ny, nb, col
        w("    /** %s */" % x.get("usage", ""))
        w("    val %s = NiuniaShadowSpec(%s.dp, %s.dp, %s, %s.dp, %s, %s)" % (camel(x["name"]), fmt(y), fmt(blur), hexc(color), fmt(ring), hexc(ringc), "true" if inset else "false"))
    w("}")
    w("")

    # --- rozmycie ---
    w("/** Rozmycie (`blur`): tylko natywnie na API 31+ (RenderEffect). Na Androidzie 11 (JMGO) — zwykłe tło z tokenów, bez symulacji. */")
    w("data class NiuniaBlurSpec(val radius: Dp, val saturate: Float = 1f, val brightness: Float = 1f)")
    w("")
    w("object NiuniaBlur {")
    for x in t["blur"]["tokens"]:
        v = x["value"]
        r = px(re.search(r"blur\(([^)]*)\)", v).group(1))
        sm = re.search(r"saturate\(([^)]*)\)", v); bm = re.search(r"brightness\(([^)]*)\)", v)
        w("    /** %s */" % x.get("usage", ""))
        w("    val %s = NiuniaBlurSpec(%s.dp, %sf, %sf)" % (camel(x["name"]), fmt(r), float(sm.group(1)) if sm else 1.0, float(bm.group(1)) if bm else 1.0))
    w("}")
    w("")

    # --- gradienty ---
    w("/** Gradient: [angle] w stopniach CSS (180 = z góry na dół, 90 = od lewej), przystanki (pozycja 0..1, kolor). */")
    w("data class NiuniaGradientSpec(val radial: Boolean, val angle: Float, val stops: List<Pair<Float, Color>>) {")
    w("    fun brush(): Brush = when {")
    w("        radial -> Brush.radialGradient(*stops.toTypedArray())")
    w("        angle == 90f -> Brush.horizontalGradient(*stops.toTypedArray())")
    w("        angle == 270f -> Brush.horizontalGradient(*stops.map { (p, c) -> (1f - p) to c }.reversed().toTypedArray())")
    w("        angle == 180f -> Brush.verticalGradient(*stops.toTypedArray())")
    w("        else -> Brush.linearGradient(*stops.toTypedArray(), start = Offset.Zero, end = Offset.Infinite)")
    w("    }")
    w("}")
    w("")
    w("object NiuniaGradient {")
    for x in t["gradient"]["tokens"]:
        kind, angle, stops = parse_gradient(x["value"])
        st = ", ".join("%sf to %s" % (round(p, 4), hexc(c)) for p, c in stops)
        w("    /** %s */" % x.get("usage", ""))
        w("    val %s = NiuniaGradientSpec(%s, %sf, listOf(%s))" % (camel(x["name"]), "true" if kind == "radial" else "false", angle, st))
    w("}")
    w("")

    # --- indeks do testu zgodności ---
    w("/** Indeks nazwa z tokens.json → wartość z obiektów powyżej (test NiuniaTokensTest porównuje z JSON-em). */")
    w("object NiuniaTokenIndex {")
    w("    val colors: Map<String, Color> = mapOf(")
    for c in t["color"]["tokens"]:
        w("        \"%s\" to NiuniaColor.%s," % (c["name"], camel(c["name"])))
    w("    )")
    for obj, group in [("NiuniaSpacing", "spacing"), ("NiuniaRadius", "radius"), ("NiuniaSize", "size")]:
        w("    val %s: Map<String, Dp> = mapOf(" % group)
        for x in t[group]["tokens"]:
            w("        \"%s\" to %s.%s," % (x["name"], obj, camel(x["name"])))
        w("    )")
    w("    val type: Map<String, TextStyle> = mapOf(")
    for g in t["type"]["groups"]:
        for s in g["styles"]:
            w("        \"%s\" to NiuniaType.%s," % (s["name"], camel(s["name"])))
    w("    )")
    w("}")
    w("")

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(L))
    os.makedirs(os.path.dirname(TEST_COPY), exist_ok=True)
    shutil.copyfile(SRC, TEST_COPY)
    print("zapisano %s (%d linii) i %s" % (os.path.relpath(OUT, ROOT), len(L), os.path.relpath(TEST_COPY, ROOT)))


if __name__ == "__main__":
    main()


# ---------------------------------------------------------------------------
# Ikony: ścieżki SVG z bundle/nuvio.js (ICONS, viewBox 24, kreska 2) → NiuniaIconPaths.kt
ICONS_JS = os.path.join(os.path.dirname(SRC), "bundle", "nuvio.js")
ICONS_OUT = os.path.join(ROOT, "app/src/main/java/com/nuvio/tv/skin/niunia/tokens/NiuniaIconPaths.kt")


def _attrs(tag):
    return dict(re.findall(r'([\w-]+)="([^"]*)"', tag))


def _svg_to_paths(svg):
    out = []
    for tag in re.findall(r"<(path|circle|rect)\b([^>]*)/>", svg):
        kind, rest = tag
        a = _attrs(rest)
        fill = "fill" in a.get("class", "")
        if kind == "path":
            d = a["d"]
        elif kind == "circle":
            cx, cy, r = float(a["cx"]), float(a["cy"]), float(a["r"])
            d = f"M{cx - r},{cy}a{r},{r} 0 1,0 {2 * r},0a{r},{r} 0 1,0 {-2 * r},0z"
        else:
            x, y, wd, ht = float(a["x"]), float(a["y"]), float(a["width"]), float(a["height"])
            rx = float(a.get("rx", 0))
            d = (f"M{x + rx},{y}h{wd - 2 * rx}a{rx},{rx} 0 0 1 {rx},{rx}v{ht - 2 * rx}a{rx},{rx} 0 0 1 {-rx},{rx}"
                 f"h{-(wd - 2 * rx)}a{rx},{rx} 0 0 1 {-rx},{-rx}v{-(ht - 2 * rx)}a{rx},{rx} 0 0 1 {rx},{-rx}z")
        out.append((d, fill))
    return out


def gen_icons():
    if not os.path.exists(ICONS_JS):
        print("brak %s — ikony bez zmian" % ICONS_JS)
        return
    js = open(ICONS_JS, encoding="utf-8").read()
    block = re.search(r"var ICONS = \{(.*?)\n  \};", js, re.S).group(1)
    icons = re.findall(r"""(?:"([\w-]+)"|([\w-]+)):\s*'([^']*)'""", block)
    L = ["// WYGENEROWANE przez fork-tools/niunia/gen_tokens.py z design-system/bundle/nuvio.js (ICONS). Nie edytuj ręcznie.",
         "package com.nuvio.tv.skin.niunia.tokens", "",
         "/** Ikona design systemu: ścieżki SVG w polu 24×24, kreska 2 dp; `true` = wypełniona. */",
         "object NiuniaIconPaths {", "    val all: Map<String, List<Pair<String, Boolean>>> = mapOf("]
    for q, b, svg in icons:
        name = q or b
        paths = ", ".join('"%s" to %s' % (d, "true" if f else "false") for d, f in _svg_to_paths(svg))
        L.append('        "%s" to listOf(%s),' % (name, paths))
    L += ["    )", "}", ""]
    with open(ICONS_OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(L))
    print("zapisano %s (%d ikon)" % (os.path.relpath(ICONS_OUT, ROOT), len(icons)))


if __name__ == "__main__":
    gen_icons()
