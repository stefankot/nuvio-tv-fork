B='app/src/main/java/com/nuvio/tv/'
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
edit(B+'fork/night/NightModule.kt',[
('        com.nuvio.tv.fork.registry.ModuleAction("Siła: lekka / średnia / mocna (kolejno)"','''        com.nuvio.tv.fork.registry.ModuleAction("Tryb nocny: początek 21:00 / 22:00 / 23:00 (kolejno)", SettingsLevel.SIMPLE, "Kolejne naciśnięcia zmieniają godzinę rozpoczęcia") { c ->
            val vals = listOf("1260", "1320", "1380"); val cur = ForkFeatures.str(c, K_START, "1320")
            ForkFeatures.setStr(c, K_START, vals[(vals.indexOf(cur) + 1) % vals.size]); load(c)
        },
        com.nuvio.tv.fork.registry.ModuleAction("Tryb nocny: koniec 06:00 / 07:00 / 08:00 (kolejno)", SettingsLevel.SIMPLE, "Kolejne naciśnięcia zmieniają godzinę zakończenia") { c ->
            val vals = listOf("360", "420", "480"); val cur = ForkFeatures.str(c, K_END, "420")
            ForkFeatures.setStr(c, K_END, vals[(vals.indexOf(cur) + 1) % vals.size]); load(c)
        },
        com.nuvio.tv.fork.registry.ModuleAction("Siła: lekka / średnia / mocna (kolejno)"'''),
])
print('ok8')
