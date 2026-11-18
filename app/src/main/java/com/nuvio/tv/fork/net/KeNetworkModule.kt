package com.nuvio.tv.fork.net

import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ModuleAction
import com.nuvio.tv.fork.registry.SettingsLevel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * W1 etap 2: wejście „Test łącza” w ustawieniach forka (poza odtwarzaczem Kicia). Mierzy łącze na adresie
 * ostatnio oglądanego filmu (bazowe `LastPlaybackDiagnostics.streamUrl`/`headersJson`), przez [KeNetwork.test].
 */
object KeNetworkModule : ForkModule {
    override val id = "ke_network"
    override val title = "Test łącza"
    override val defaultEnabled = true

    private fun toast(c: android.content.Context, m: String) =
        android.widget.Toast.makeText(c, m, android.widget.Toast.LENGTH_LONG).show()

    override fun actions() = listOf(
        ModuleAction("Test łącza (ostatni film)", SettingsLevel.SIMPLE,
            "Mierzy prędkość usługi na ostatnio oglądanym filmie i ustawia pobieranie równoległe. " +
                KeNetwork.status.ifEmpty { "Trwa do ok. 1 min." }) { c ->
            val app = c.applicationContext
            CoroutineScope(Dispatchers.Main).launch {
                val d = runCatching {
                    EntryPointAccessors.fromApplication(app, com.nuvio.tv.fork.sources.SourceChoice.Deps::class.java)
                        .player().lastPlaybackDiagnostics.first()
                }.getOrNull()
                val url = d?.streamUrl
                if (url.isNullOrBlank()) { toast(app, "Najpierw obejrzyj chwilę dowolny film, potem uruchom test"); return@launch }
                val headers = runCatching {
                    val o = org.json.JSONObject(d.headersJson ?: "{}")
                    o.keys().asSequence().associateWith { o.getString(it) }
                }.getOrDefault(emptyMap())
                toast(app, "Test łącza: start (ok. 1 min)")
                KeNetwork.test(app, url, headers) { msg -> toast(app, msg) }
            }
        },
    )
}
