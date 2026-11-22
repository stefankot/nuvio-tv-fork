"""Rozwiązywanie konfliktów scalania: python3 -I resolve.py <tryb> <plik>...
tryb: union (najpierw nasza strona, potem ich), ours, theirs. Działa na znacznikach <<<<<<< ======= >>>>>>> (bez diff3)."""
import sys
mode, files = sys.argv[1], sys.argv[2:]
for path in files:
    out, state, ours, theirs = [], 0, [], []
    for line in open(path, encoding="utf-8").read().split("\n"):
        if state == 0 and line.startswith("<<<<<<< "):
            state, ours, theirs = 1, [], []
        elif state == 1 and line.startswith("||||||| "):
            state = 3
        elif state in (1, 3) and line == "=======":
            state = 2
        elif state == 2 and line.startswith(">>>>>>> "):
            if mode == "union":
                out += ours + [l for l in theirs if l not in ours or not l.strip()]
            elif mode == "ours":
                out += ours
            else:
                out += theirs
            state = 0
        elif state == 1:
            ours.append(line)
        elif state == 2:
            theirs.append(line)
        elif state == 3:
            pass
        else:
            out.append(line)
    assert state == 0, path
    open(path, "w", encoding="utf-8").write("\n".join(out))
    print(mode, path)
