import re;x=open('/tmp/u.xml').read();N=re.findall(r'<node [^>]*>',x)
B=lambda n: list(map(int,re.findall(r'\d+',re.search(r'bounds="([^"]*)"',n).group(1))))
F=[n for n in N if 'focused="true"' in n]
f=B(F[0]); inside=[n for n in N if B(n)[0]>=f[0] and B(n)[2]<=f[2] and B(n)[1]>=f[1] and B(n)[3]<=f[3]]
t=[re.search(r' text="([^"]*)"',n).group(1) for n in inside if re.search(r' text="([^"]*)"',n).group(1)]
print(f, ' / '.join(t), '| nodes', len(inside))
