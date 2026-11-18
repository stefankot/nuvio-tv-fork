import re;x=open('/tmp/u.xml').read()
nodes=[m.group(0) for m in re.finditer(r'<node [^>]*>',x)]
fb=None
for n in nodes:
  if 'focused="true"' in n: fb=list(map(int,re.findall(r'\d+',re.search(r'bounds="([^"]*)"',n).group(1))))
out=''
for n in nodes:
  t=re.search(r' text="([^"]*)"',n).group(1); c=list(map(int,re.findall(r'\d+',re.search(r'bounds="([^"]*)"',n).group(1))))
  if fb and t and c[0]>=fb[0] and c[2]<=fb[2] and c[1]>=fb[1] and c[3]<=fb[3]: out=t
print(out)
