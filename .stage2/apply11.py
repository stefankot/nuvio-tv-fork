import shutil
B='app/src/main/java/com/nuvio/tv/'
shutil.copy('.stage2/ToolsHub.kt',B+'fork/settings/ToolsHub.kt')
def edit(p,pairs):
    s=open(p).read()
    for a,b in pairs:
        assert a in s,(p,a); s=s.replace(a,b,1)
    open(p,'w').write(s)
edit(B+'MainActivity.kt',[
("                    val calendarMenuItem = com.nuvio.tv.fork.echo.rememberCalendarMenuItem() // Fork hook: echo-calendar\n","                    val calendarMenuItem = com.nuvio.tv.fork.echo.rememberCalendarMenuItem() // Fork hook: echo-calendar\n                    val toolsMenuItem = com.nuvio.tv.fork.settings.rememberToolsMenuItem() // Fork hook: tools menu\n"),
("                            calendarMenuItem?.let { add(it.route) } // Fork hook: echo-calendar\n","                            calendarMenuItem?.let { add(it.route) } // Fork hook: echo-calendar\n                            toolsMenuItem?.let { add(it.route) } // Fork hook: tools menu\n"),
("                            calendarMenuItem?.let(::add) // Fork hook: echo-calendar\n","                            calendarMenuItem?.let(::add) // Fork hook: echo-calendar\n                            toolsMenuItem?.let(::add) // Fork hook: tools menu\n"),
])
edit(B+'fork/ForkHooks.kt',[("        builder.composable(com.nuvio.tv.fork.settings.MODULES_ROUTE) { com.nuvio.tv.fork.settings.ModulesScreen() }","        builder.composable(com.nuvio.tv.fork.settings.MODULES_ROUTE) { com.nuvio.tv.fork.settings.ModulesScreen() }\n        builder.composable(com.nuvio.tv.fork.settings.TOOLS_ROUTE) { com.nuvio.tv.fork.settings.ToolsHubScreen() }")])
print('ok11')
