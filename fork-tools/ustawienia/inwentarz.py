#!/usr/bin/env python3
"""Inwentarz ustawień: wyciąga z kodu wszystkie pozycje ustawień (wiersze bazy/Reshaped/Forge, moduły forka)
i wypisuje tabelę markdown. Użycie: python3 -I fork-tools/ustawienia/inwentarz.py > plan/ustawienia/inwentarz-surowy.md"""
import os, re, sys, glob, html
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
SRC = os.path.join(ROOT, 'app/src/main/java/com/nuvio/tv')
RES = os.path.join(ROOT, 'app/src/main/res')

def load_strings(d):
    out = {}
    for f in glob.glob(os.path.join(RES, d, 'strings*.xml')) + glob.glob(os.path.join(RES, d, '*_strings.xml')):
        txt = open(f, encoding='utf-8').read()
        for m in re.finditer(r'<string name="([^"]+)"[^>]*>(.*?)</string>', txt, re.S):
            out[m.group(1)] = html.unescape(re.sub(r'\\(.)', r'\1', m.group(2))).strip()
    return out
EN = load_strings('values'); PL = load_strings('values-pl')

def res(expr):
    expr = expr.strip()
    m = re.match(r'stringResource\(\s*(?:com\.nuvio\.tv\.)?R\.string\.(\w+)', expr)
    if m:
        k = m.group(1); return PL.get(k) or ('[EN] ' + EN.get(k, k))
    m = re.match(r'"((?:[^"\\]|\\.)*)"', expr)
    if m: return m.group(1)
    return '{' + expr[:60] + '}'

ROWS = ('SettingsToggleRow', 'SettingsActionRow', 'SliderSettingsItem', 'ToggleSettingsItem', 'NavigationSettingsItem', 'ColorSettingsItem', 'LiveTvMenuRow', 'forkSwitch')
call = re.compile(r'\b(' + '|'.join(ROWS) + r'|SettingsGroupCard)\s*\(')

def arg(body, name):
    m = re.search(r'\b' + name + r'\s*=\s*(stringResource\([^)]*\)|"(?:[^"\\]|\\.)*"|[^,\n)]+)', body)
    return res(m.group(1)) if m else ''

def scan_file(path):
    txt = open(path, encoding='utf-8').read()
    rel = os.path.relpath(path, SRC)
    group = ''
    for m in call.finditer(txt):
        kind = m.group(1); body = txt[m.end(): m.end() + 700]
        if kind == 'SettingsGroupCard':
            group = arg(body, 'title'); continue
        if kind == 'forkSwitch':
            p = re.match(r'\s*("?[\w.]+"?)\s*,[^,]+,\s*("(?:[^"\\]|\\.)*")\s*,\s*("(?:[^"\\]|\\.)*")', body)
            if p: yield rel, group, 'przełącznik', res(p.group(2)), p.group(1).strip('"'), res(p.group(3))
            continue
        title = arg(body, 'title') or arg(body, 'label')
        if not title:
            p = re.match(r'\s*(stringResource\([^)]*\)|"(?:[^"\\]|\\.)*")', body)
            title = res(p.group(1)) if p else '?'
        sub = arg(body, 'subtitle') or arg(body, 'description')
        typ = {'SettingsToggleRow': 'przełącznik', 'ToggleSettingsItem': 'przełącznik', 'SliderSettingsItem': 'suwak',
               'NavigationSettingsItem': 'ekran', 'ColorSettingsItem': 'kolor', 'LiveTvMenuRow': 'menu TV'}.get(kind, 'wybór/czynność')
        yield rel, group, typ, title, '', sub

def modules():
    for f in sorted(glob.glob(os.path.join(SRC, 'fork', '**', '*Module.kt'), recursive=True)):
        txt = open(f, encoding='utf-8').read(); rel = os.path.relpath(f, SRC)
        mid = re.search(r'override val id = "(\w+)"', txt)
        for m in re.finditer(r'ModuleSetting\(\s*([^,]+),\s*[\w.]*SettingsLevel\.(\w+),\s*"([^"]*)",\s*("(?:[^"\\]|\\.)*"(?:\s*\+[^,]+)?)\s*,\s*("(?:[^"\\]|\\.)*")?', txt):
            dv = re.search(r'defaultValue\s*=\s*(true|false)', txt[m.end(): m.end() + 300])
            yield rel, m.group(3), 'przełącznik ' + m.group(2), res(m.group(4)), m.group(1).strip().strip('"').split('.')[-1], (res(m.group(5)) if m.group(5) else '') + (' [dom. ' + dv.group(1) + ']' if dv else '')
        for m in re.finditer(r'ModuleAction\(\s*("(?:[^"\\]|\\.)*"|[^,]+),\s*[\w.]*SettingsLevel\.(\w+)(?:,\s*("(?:[^"\\]|\\.)*"))?', txt):
            yield rel, (mid.group(1) if mid else ''), 'czynność ' + m.group(2), res(m.group(1)), '', res(m.group(3)) if m.group(3) else ''

def kotlin(files):
    """Słowa z wnętrza ekranów (nazwy wierszy) dla szukania w drzewie: plik → nazwy. Wynik: fork/settings/tree/SettingsTreeWords.kt."""
    by = {}
    for f in files:
        stem = os.path.splitext(os.path.basename(f))[0]
        for r in scan_file(f):
            t = re.sub(r'^\[EN\] ', '', r[3]).strip()
            if not t or t == '?' or '{' in t or '$' in t or len(t) > 60: continue
            lst = by.setdefault(stem, [])
            if t not in lst: lst.append(t)
    q = lambda t: '"' + t.replace('\\', '\\\\').replace('"', '\\"') + '"'
    out = ['package com.nuvio.tv.fork.settings.tree', '',
           '/** WYGENEROWANE: `python3 -I fork-tools/ustawienia/inwentarz.py --kotlin` — nazwy wierszy z ekranów ustawień (szukanie w środku ekranów). Nie edytuj ręcznie. */',
           'internal object SettingsTreeWords {', '    val byFile: Map<String, List<String>> = mapOf(']
    for k in sorted(by):
        out.append('        %s to listOf(%s),' % (q(k), ', '.join(q(t) for t in by[k])))
    out += ['    )', '}', '']
    open(os.path.join(SRC, 'fork/settings/tree/SettingsTreeWords.kt'), 'w', encoding='utf-8').write('\n'.join(out))
    print('SettingsTreeWords.kt: %d plików, %d nazw' % (len(by), sum(len(v) for v in by.values())), file=sys.stderr)

def main():
    files = sorted(glob.glob(os.path.join(SRC, 'ui/screens/settings/*.kt')) + glob.glob(os.path.join(SRC, 'skin/**/settings/*.kt'), recursive=True)
                   + glob.glob(os.path.join(SRC, 'ui/reshaped/**/*Settings*.kt'), recursive=True) + glob.glob(os.path.join(SRC, 'ui/reshaped/livetv/*.kt')))
    if '--kotlin' in sys.argv: kotlin(files); return
    n = 0
    print('| # | plik | grupa | typ | nazwa | klucz | opis |'); print('|---|---|---|---|---|---|---|')
    for f in files:
        for r in scan_file(f):
            n += 1; print('| %d | %s |' % (n, ' | '.join(x.replace('|', '/').replace('\n', ' ')[:140] for x in r)))
    for r in modules():
        n += 1; print('| %d | %s |' % (n, ' | '.join(x.replace('|', '/').replace('\n', ' ')[:140] for x in r)))
    print('\nRazem: %d pozycji' % n, file=sys.stderr)
main()
