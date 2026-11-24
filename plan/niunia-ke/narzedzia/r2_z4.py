#!/usr/bin/env python3
"""Runda 2, Z-4: odtwarzacz programu z archiwum bez przełącznika kanałów; ▲▼ nigdy nie zmienia kanału, ◀ z pierwszego przycisku zostaje."""
N = "/Users/milajovovich/Desktop/nuvio 2/wt-niunia-ke/app/src/main/java/com/nuvio/tv/skin/niunia/live/NiuniaLivePlayer.kt"
s = open(N).read()
def R(a, b):
    global s
    assert a in s, a[:80]
    s = s.replace(a, b, 1)
R('''    fun reset(archive: Boolean) {
        level = 0; slot = if (archive) NiuniaLiveSlot.PAUSE else NiuniaLiveSlot.CHANNEL; rowIndex = 0; section = 0; index = 0
    }''', '''    /** Z-4: program z archiwum — bez przełącznika kanałów (▲▼ tylko przenosi fokus, ◀ z pierwszego przycisku zostaje). */
    var archiveMode = false

    fun reset(archive: Boolean) {
        level = 0; slot = if (archive) NiuniaLiveSlot.PAUSE else NiuniaLiveSlot.CHANNEL; rowIndex = 0; section = 0; index = 0
        archiveMode = archive
        if (archive) Log.i(TAG, "archive: channel switch, channel logo and up/down arrows hidden")
    }''')
R('''                NiuniaLiveSlot.CHANNEL -> { ForgeMiniGuideNav.paused = false; state.zap(-1); Log.i(TAG, "iptv: kanał ▲") }''',
  '''                NiuniaLiveSlot.CHANNEL -> if (archiveMode) Log.i(TAG, "iptv: archiwum — ▲ nie zmienia kanału") else { ForgeMiniGuideNav.paused = false; state.zap(-1); Log.i(TAG, "iptv: kanał ▲") }''')
R('''                NiuniaLiveSlot.CHANNEL -> { ForgeMiniGuideNav.paused = false; state.zap(1); Log.i(TAG, "iptv: kanał ▼") }''',
  '''                NiuniaLiveSlot.CHANNEL -> if (archiveMode) Log.i(TAG, "iptv: archiwum — ▼ nie zmienia kanału") else { ForgeMiniGuideNav.paused = false; state.zap(1); Log.i(TAG, "iptv: kanał ▼") }''')
R('''                else -> { val i = buttons.indexOf(slot); slot = if (i <= 0) NiuniaLiveSlot.CHANNEL else buttons[i - 1] }''',
  '''                else -> { val i = buttons.indexOf(slot); slot = if (i <= 0) (if (archiveMode) slot else NiuniaLiveSlot.CHANNEL) else buttons[i - 1] }''')
R('''                ChannelSwitch(channel, logo, focused = focusSlot == NiuniaLiveSlot.CHANNEL, modifier = Modifier.offset(x = 40.dp, y = 398.dp))
                // Przyciski od x=170 (y 414) i po prawej (right 28).
                Row(Modifier.offset(x = 170.dp, y = 414.dp),''',
  '''                // Z-4: w archiwum bez przełącznika kanałów; przyciski zaczynają się od lewej krawędzi (x 40).
                if (!archive) ChannelSwitch(channel, logo, focused = focusSlot == NiuniaLiveSlot.CHANNEL, modifier = Modifier.offset(x = 40.dp, y = 398.dp))
                // Przyciski od x=170 (y 414) i po prawej (right 28).
                Row(Modifier.offset(x = if (archive) 40.dp else 170.dp, y = 414.dp),''')
open(N, "w").write(s)
print("Z-4 nałożone")
