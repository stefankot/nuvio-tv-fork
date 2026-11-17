import re,subprocess,time
A="/Users/milajovovich/Library/Android/sdk/platform-tools/adb"
import os
DEV=os.environ.get("DEV","")
def sh(*a): return subprocess.run([A]+(["-s",DEV] if DEV else [])+["shell",*a],capture_output=True,text=True).stdout
def dump():
    sh("uiautomator","dump","/sdcard/u.xml"); return sh("cat","/sdcard/u.xml")
def texts(x=None):
    x=x or dump(); return re.findall(r'text="([^"]{1,80})"',x)
def key(k,n=1):
    for _ in range(n): sh("input","keyevent","KEYCODE_"+k)
def tap_text(name,x=None):
    x=x or dump(); m=re.search(r'text="%s"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'%re.escape(name),x)
    if not m: return False
    a,b,c,d=map(int,m.groups()); sh("input","tap",str((a+c)//2),str((b+d)//2)); return True
def focus_y(x):
    for n in re.findall(r'<node [^>]*>',x):
        if 'focused="true"' in n:
            b=re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',n)
            if b: a,bb,c,d=map(int,b.groups()); return (bb+d)//2
def text_y(x,name):
    m=re.search(r'text="%s"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'%re.escape(name),x)
    if m: a,b,c,d=map(int,m.groups()); return (b+d)//2
def focus_to(name,step=96,maxit=6):
    for _ in range(maxit):
        x=dump(); fy=focus_y(x); ty=text_y(x,name)
        if fy is None or ty is None: return False
        n=round((ty-fy)/step)
        if n==0: return True
        key("DPAD_DOWN" if n>0 else "DPAD_UP",abs(n))
    return False
def start_app():
    sh("am","force-stop","com.nuviodebug.com"); sh("am","start","-n","com.nuviodebug.com/com.nuvio.tv.launcher.AppIconDefault"); time.sleep(12); key("DPAD_CENTER"); time.sleep(10)

def tap_desc(name):
    x=dump(); m=re.search(r'content-desc="%s"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'%re.escape(name),x)
    if not m: return False
    a,b,c,d=map(int,m.groups()); sh("input","tap",str((a+c)//2),str((b+d)//2)); return True

def label_at_focus(x=None):
    x=x or dump(); nodes=re.findall(r'<node [^>]*>',x); fb=None
    for n in nodes:
        if 'focused="true"' in n:
            b=re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',n); fb=tuple(map(int,b.groups())); break
    if not fb: return None
    best=None
    for n in nodes:
        b=re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',n)
        if not b: continue
        bb=tuple(map(int,b.groups()))
        if bb[0]>=fb[0] and bb[1]>=fb[1] and bb[2]<=fb[2] and bb[3]<=fb[3]:
            d=re.search(r'content-desc="([^"]+)"',n) or re.search(r' text="([^"]+)"',n)
            if d: return d.group(1)
    return "?"
def nav(label,dirs=("DPAD_DOWN","DPAD_UP"),maxn=10):
    for d in dirs:
        for _ in range(maxn):
            if label_at_focus()==label: return True
            key(d); time.sleep(0.5)
        for _ in range(maxn): key("DPAD_UP" if d=="DPAD_DOWN" else "DPAD_DOWN")
    return label_at_focus()==label
def sidebar_open(label):
    key("DPAD_LEFT",6); time.sleep(1.5); return nav(label)
