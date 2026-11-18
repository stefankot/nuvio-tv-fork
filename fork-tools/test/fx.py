import re
x=open('/tmp/u.xml').read()
nodes=[m.group(0) for m in re.finditer(r'<node [^>]*>',x)]
def attr(n,a):
    m=re.search(' '+a+r'="([^"]*)"',n); return m.group(1) if m else ''
def box(n): return list(map(int,re.findall(r'\d+',attr(n,'bounds')))) or [0,0,0,0]
fb=None
for n in nodes:
    if 'focused="true"' in n: fb=box(n)
out=''
for n in nodes:
    t=attr(n,'text') or attr(n,'content-desc'); c=box(n)
    if fb and t and c[0]>=fb[0] and c[2]<=fb[2] and c[1]>=fb[1] and c[3]<=fb[3]: out=t
print(out)
