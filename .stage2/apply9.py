p='app/src/test/java/com/nuvio/tv/fork/lektor/LektorEngineTest.kt'
s=open(p).read(); a="    @Test fun `wyłączony lektor nie aktywuje procesora`() {"
assert a in s
s=s.replace(a,open('.stage2/new_test.txt').read()+a,1); open(p,'w').write(s); print('ok9')
