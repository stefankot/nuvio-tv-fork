#!/usr/bin/env python3
"""Dopisuje .logged(tag, "Plik:linia") po blokach runCatching { … } bez obsługi błędu (zasada 19.11: logi wszędzie).
Użycie: add_logged.py [--apply] <katalog_java_com_nuvio_tv>   (bez --apply: tylko lista)"""
import os, re, sys

SKIP_BODY = re.compile(r'requestFocus|freeFocus|scrollTo|animateScroll|Toast|bringIntoView|\bLog\.')
SKIP_PARSE = re.compile(r'^\s*[\w.\[\]()"\'?!: ]*\.(toInt|toLong|toFloat|toDouble|toBoolean|toUri)\(\)\s*$|valueOf\(|Color\.parseColor|\.parse\(\s*[\w.]+\s*\)\s*$')
HANDLED = ('onFailure', 'logged', 'getOrThrow', 'exceptionOrNull', 'fold', 'recover', 'recoverCatching')
WANT = ('getOrNull', 'getOrDefault', 'getOrElse', 'onSuccess', 'map', 'isSuccess', 'isFailure')

def scan_block(s, i):
    """s[i] == '{' -> index of matching '}' (skips strings, chars, comments, templates)."""
    depth = 0; n = len(s); stack = []  # stack of string modes for templates
    while i < n:
        c = s[i]
        if s.startswith('//', i):
            i = s.find('\n', i); i = n if i < 0 else i; continue
        if s.startswith('/*', i):
            i = s.find('*/', i + 2); i = n if i < 0 else i + 2; continue
        if s.startswith('"""', i):
            i = skip_string(s, i + 3, raw=True); continue
        if c == '"':
            i = skip_string(s, i + 1, raw=False); continue
        if c == "'":
            j = i + 1
            if s[j] == '\\': j += 2
            else: j += 1
            while j < n and s[j] != "'": j += 1
            i = j + 1; continue
        if c == '{': depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0: return i
        i += 1
    return -1

def skip_string(s, i, raw):
    n = len(s)
    while i < n:
        if raw and s.startswith('"""', i):
            while s.startswith('""""', i): i += 1
            return i + 3
        c = s[i]
        if not raw and c == '\\': i += 2; continue
        if not raw and c == '"': return i + 1
        if c == '$' and i + 1 < n and s[i + 1] == '{':
            j = scan_block(s, i + 1); i = j + 1; continue
        i += 1
    return n

def tag_for(rel):
    parts = rel.split('/')
    if parts[0] == 'fork':
        sub = parts[1] if len(parts) > 2 else 'Core'
        return 'Fork' + sub[:1].upper() + sub[1:]
    if parts[0] == 'skin' and len(parts) > 1 and parts[1] == 'kicia':
        sub = parts[2] if len(parts) > 3 else 'Core'
        return 'ForkKicia' + sub[:1].upper() + sub[1:]
    return 'Fork'

def process(path, rel, apply):
    s = open(path, encoding='utf-8').read()
    edits = []; report = []
    for m in re.finditer(r'\brunCatching\s*\{', s):
        # skip occurrences inside comments/strings: crude check — line starts with // or *
        ls = s.rfind('\n', 0, m.start()) + 1
        if s[ls:m.start()].lstrip().startswith(('//', '*')): continue
        ob = m.end() - 1
        cb = scan_block(s, ob)
        if cb < 0: continue
        body = s[ob + 1:cb]
        line = s.count('\n', 0, m.start()) + 1
        k = cb + 1
        while k < len(s) and s[k] in ' \t\r\n': k += 1
        nxt = ''
        if s.startswith('?.', k): k2 = k + 2
        elif k < len(s) and s[k] == '.': k2 = k + 1
        else: k2 = -1
        if k2 > 0:
            mm = re.match(r'\w+', s[k2:]); nxt = mm.group(0) if mm else ''
        if nxt in HANDLED: continue
        if nxt and nxt not in WANT: continue
        if SKIP_BODY.search(body): continue
        if '\n' not in body.strip() and SKIP_PARSE.search(body): report.append(f'  pomin(parse) {rel}:{line}: {body.strip()[:70]}'); continue
        fname = os.path.basename(rel)
        ins = f'.logged("{tag_for(rel)}", "{fname}:{line}")'
        edits.append((cb + 1, ins))
        report.append(f'{rel}:{line} [{nxt or "bare"}] {body.strip().splitlines()[0][:70] if body.strip() else ""}')
    if apply and edits:
        for pos, ins in sorted(edits, reverse=True):
            s = s[:pos] + ins + s[pos:]
        pkg = re.search(r'^package\s+([\w.]+)', s, re.M)
        if pkg and pkg.group(1) != 'com.nuvio.tv.fork' and 'import com.nuvio.tv.fork.logged' not in s:
            imps = list(re.finditer(r'^import\s+[\w.*]+.*$', s, re.M))
            at = imps[-1].end() if imps else pkg.end()
            s = s[:at] + '\nimport com.nuvio.tv.fork.logged' + s[at:]
        open(path, 'w', encoding='utf-8').write(s)
    return len(edits), report

def main():
    apply = '--apply' in sys.argv
    root = [a for a in sys.argv[1:] if not a.startswith('--')][0]
    total = 0
    for sub in ('fork', 'skin/kicia'):
        for dp, _, fs in os.walk(os.path.join(root, sub)):
            for f in sorted(fs):
                if not f.endswith('.kt') or f in ('ForkLog.kt', 'FullLog.kt'): continue
                p = os.path.join(dp, f); rel = os.path.relpath(p, root)
                n, rep = process(p, rel, apply); total += n
                for r in rep: print(r)
    print('RAZEM', total)

main()
