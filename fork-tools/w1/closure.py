# Domknięcie zależności plików KE (W1): które pliki KE trzeba skopiować 1:1, bo forka ich nie ma.
import os,re,sys
K='upstream/ke/app/src/main/java/'; F='NuvioFork/app/src/main/java/'; base='com/nuvio/tv/'
seed=sys.argv[1:]
decl={}
def pkg_of(t):
    m=re.search(r'^package (\S+)',t,re.M); return m.group(1) if m else ''
for root,_,fs in os.walk(K+base):
    for f in fs:
        if not f.endswith('.kt'): continue
        p=os.path.join(root,f); rel=p[len(K):-3]; txt=open(p,errors='ignore').read(); pk=pkg_of(txt)
        for m in re.finditer(r'^(?:(?:internal|private|public|data|sealed|enum|abstract|open|inline|value|annotation)\s+)*(?:class|object|interface|fun|val|var|typealias)\s+(?:<[^>]*>\s*)?(?:[A-Za-z_.]+\.)?([A-Za-z_][A-Za-z0-9_]*)',txt,re.M):
            decl.setdefault((pk,m.group(1)),set()).add(rel)
todo=[base+s for s in seed]; need=set()
while todo:
    r=todo.pop()
    if r in need: continue
    need.add(r); txt=open(K+r+'.kt',errors='ignore').read(); pk=pkg_of(txt); refs=set()
    for m in re.finditer(r'^import (com\.nuvio\.tv\.\S+)\.([A-Za-z_]+)',txt,re.M): refs.add((m.group(1),m.group(2)))
    for w in set(re.findall(r'\b[A-Za-z_][A-Za-z0-9_]+\b',txt)): refs.add((pk,w))
    for k in refs:
        for d in decl.get(k,()):
            if d!=r and not os.path.exists(F+d+'.kt'): todo.append(d)
tot=0
for m in sorted(need):
    n=sum(1 for _ in open(K+m+'.kt',errors='ignore')); tot+=n; print(n,m[len(base):])
print('TOTAL',tot,len(need))
