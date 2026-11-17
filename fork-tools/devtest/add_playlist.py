#!/usr/bin/env python3
"""Dodaje playlistę M3U (+EPG) do Live TV na urządzeniu: ekran dodawania listy pokazuje adres serwera konfiguracji z tokenem
(http://IP:PORT/TOKEN/); skrypt czyta go z uiautomator i wysyła POST /TOKEN/api/source.
Użycie: DEV=ip:port python3 add_playlist.py <url_m3u> [url_epg]"""
import json,os,re,subprocess,sys,urllib.request
A=os.path.expanduser("~/Library/Android/sdk/platform-tools/adb"); DEV=os.environ["DEV"]
def sh(*a): return subprocess.run([A,"-s",DEV,"shell",*a],capture_output=True,text=True).stdout
sh("uiautomator","dump","/sdcard/u.xml"); x=sh("cat","/sdcard/u.xml")
m=re.search(r'(https?://\d+\.\d+\.\d+\.\d+:\d+/[A-Za-z0-9_-]+)/?',x)
if not m: sys.exit("brak adresu serwera konfiguracji na ekranie (otwórz okno dodawania listy)")
base=m.group(1); print("serwer:",base.rsplit('/',1)[0]+"/<token>")
body=json.dumps({"type":"m3u","url":sys.argv[1],"epg":sys.argv[2] if len(sys.argv)>2 else "","logo":""}).encode()
req=urllib.request.Request(base+"/api/source",data=body,headers={"Content-Type":"application/json"},method="POST")
try:
    r=urllib.request.urlopen(req,timeout=60); print("odpowiedź:",r.status,r.read().decode()[:300])
except Exception as e: print("błąd:",e)
