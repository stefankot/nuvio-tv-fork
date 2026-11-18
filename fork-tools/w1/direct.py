# Bezpośrednie (1 poziom) zależności plików KE, których brak w forku.
import os,re,sys
K='upstream/ke/app/src/main/java/'; F='NuvioFork/app/src/main/java/'; base='com/nuvio/tv/'
decl={}
def pkg_of(t):
    m=re.search(r'^package (\S+)',t,re.M); return m.group(1) if m else ''
for root,_,fs in os.walk(K+base):
    for f in fs:
        if not f.endswith('.kt'): continue
        p=os.path.join(root,f); rel=p[len(K)+len(base):-3]; txt=open(p,errors='ignore').read(); pk=pkg_of(txt)
        for m in re.finditer(r'^(?:(?:internal|private|public|data|sealed|enum|abstract|open|inline|value|annotation)\s+)*(?:class|object|interface|fun|val|var|typealias)\s+(?:<[^>]*>\s*)?(?:[A-Za-z_.]+\.)?([A-Za-z_][A-Za-z0-9_]*)',txt,re.M):
            decl.setdefault((pk,m.group(1)),set()).add(rel)
for s in sys.argv[1:]:
    txt=open(K+base+s+'.kt',errors='ignore').read(); pk=pkg_of(txt); refs=set()
    body=re.sub(r'//.*|/\*[\s\S]*?\*/','',txt)
    for m in re.finditer(r'^import (com\.nuvio\.tv\.\S+)\.([A-Za-z_]+)',txt,re.M): refs.add((m.group(1),m.group(2)))
    for w in set(re.findall(r'\b[A-Za-z_][A-Za-z0-9_]+\b',body)): refs.add((pk,w))
    deps=set()
    for k in refs:
        for d in decl.get(k,()):
            if d!=s and not os.path.exists(F+base+d+'.kt'): deps.add(d+':'+k[1])
    print(s,'->',' '.join(sorted(deps)))
