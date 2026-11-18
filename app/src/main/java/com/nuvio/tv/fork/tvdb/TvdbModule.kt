package com.nuvio.tv.fork.tvdb

import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ModuleAction
import com.nuvio.tv.fork.registry.SettingsLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** W8: przełącznik „TVDB: dane o serialach w IPTV” (moduł `module_tvdb`, domyślnie wł.) i sprawdzenie połączenia. */
object TvdbModule : ForkModule {
    override val id = "tvdb"
    override val title = "TVDB: dane o serialach w IPTV"
    override val defaultEnabled = true

    override fun actions() = listOf(
        ModuleAction("TVDB: sprawdź połączenie", SettingsLevel.ESSENTIAL,
            "Gdy TMDB nie zna programu z przewodnika, opis, plakat i obsada przychodzą z TheTVDB. Stan: " + Tvdb.status) { c ->
            val app = c.applicationContext
            CoroutineScope(Dispatchers.Main).launch {
                val msg = Tvdb.check(app)
                android.widget.Toast.makeText(app, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        },
    )
}
