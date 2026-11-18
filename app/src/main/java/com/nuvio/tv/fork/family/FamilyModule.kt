package com.nuvio.tv.fork.family

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.composable
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ModuleAction
import com.nuvio.tv.fork.registry.ModuleRoute
import com.nuvio.tv.fork.registry.SettingsLevel
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/** Moduł „Rodzina”: wspólne ustawienia dla rodziny i znajomych (FamilySync). Wejście: Ustawienia (proste) i Narzędzia → Moduły. */
object FamilyModule : ForkModule {
    const val ROUTE = "fork_family"
    override val id = "family"
    override val title = "Rodzina: wspólne ustawienia"
    override val defaultEnabled = true
    override fun actions() = listOf(
        ModuleAction("Rodzina: wspólne ustawienia", SettingsLevel.SIMPLE, "Udostępnij ustawienia albo dołącz kodem 6 cyfr") { ForkFeatures.open?.invoke(ROUTE) },
    )
    override fun settings() = listOf(
        com.nuvio.tv.fork.registry.ModuleSetting(FamilyUsage.KEY, SettingsLevel.SIMPLE, "Rodzina", "Statystyki używania dla panelu",
            "Anonimowe liczniki ekranów, przycisków i funkcji (bez tytułów) — pomagają ulepszać wygląd i obsługę", defaultValue = true),
    )
    override fun routes() = listOf(ModuleRoute(ROUTE) { composable(ROUTE) { FamilyScreen() } })
}

@Composable
fun FamilyScreen() {
    val c = LocalContext.current
    val st by FamilySync.state.collectAsState()
    var code by remember { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(st.code) { while (st.code != null) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(1_000) } }
    LaunchedEffect(Unit) { c.findActivity()?.let { FamilySync.onAppVisible(it) } }
    LazyColumn(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Rodzina: wspólne ustawienia", style = MaterialTheme.typography.headlineMedium) }
        item {
            Text(
                when (st.role) {
                    FamilySync.Role.OWNER -> "Udostępniasz swoje ustawienia rodzinie (wersja ${st.version}). Zmiany wysyłają się same."
                    FamilySync.Role.MEMBER -> "Ten telewizor ma ustawienia rodziny (wersja ${st.version}). Zmiany przychodzą same przy starcie."
                    FamilySync.Role.NONE -> "Wszystkie ustawienia (dodatki, źródła, klucze, konto, profile) z jednego telewizora na telewizorach rodziny. Zaszyfrowane: serwer ich nie odczyta."
                },
            )
        }
        if (st.message.isNotEmpty()) item { Text(st.message, style = MaterialTheme.typography.titleMedium) }
        if (st.role != FamilySync.Role.MEMBER) {
            item { Text("Twój telewizor", style = MaterialTheme.typography.titleMedium) }
            if (st.role == FamilySync.Role.NONE) item { Button(onClick = { FamilySync.share(c) }) { Text("Udostępnij moje ustawienia rodzinie") } }
            if (st.role == FamilySync.Role.OWNER) {
                item { Button(onClick = { FamilySync.newDeviceCode(c) }) { Text("Dodaj telewizor rodziny (pokaż kod)") } }
                val left = ((st.codeUntilMs - now) / 1000).coerceAtLeast(0)
                if (st.code != null && left > 0) item {
                    Column {
                        Text(st.code!!.chunked(3).joinToString(" "), fontSize = 64.sp)
                        Text((if (st.panelCode) "Wpisz ten kod w panelu nuviosync.caring.design/panel." else "Wpisz ten kod na telewizorze rodziny.") + " Ważny jeszcze ${left / 60}:${"%02d".format(left % 60)}, jeden raz.")
                    }
                }
                item { Button(onClick = { FamilySync.newPanelCode(c) }) { Text("Połącz panel w przeglądarce (pokaż kod)") } }
                item { Button(onClick = { FamilySync.share(c) }) { Text("Wyślij zmiany teraz") } }
                item { Button(onClick = { FamilySync.stopSharing(c) }) { Text("Przestań udostępniać (rodzina zachowa obecne ustawienia)") } }
            }
        }
        if (st.role != FamilySync.Role.OWNER) {
            item { Text(if (st.role == FamilySync.Role.MEMBER) "Połącz ponownie" else "Masz kod od rodziny?", style = MaterialTheme.typography.titleMedium) }
            item {
                OutlinedTextField(
                    value = code, onValueChange = { v -> code = v.filter { it.isDigit() }.take(6) }, singleLine = true,
                    label = { androidx.compose.material3.Text("Kod 6 cyfr") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.width(280.dp),
                )
            }
            item { Button(onClick = { c.findActivity()?.let { FamilySync.join(it, code) } }, enabled = code.length == 6) { Text("Pobierz ustawienia rodziny") } }
            if (st.role == FamilySync.Role.MEMBER) item { Button(onClick = { FamilySync.stopSharing(c) }) { Text("Odłącz od rodziny") } }
        }
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Przycisk na ekranie startowym + okno z kodem (nie zmienia układu ekranu: jeden przycisk w rzędzie, okno nad nim). */
@Composable
fun FamilyStartButton() {
    val c = LocalContext.current
    var open by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))
    Button(onClick = { open = true }) { Text("Mam kod od rodziny") }
    if (!open) return
    val st by FamilySync.state.collectAsState()
    var code by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = { open = false }) {
        androidx.compose.foundation.layout.Box(
            Modifier.androidx_background().padding(32.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ustawienia od rodziny", style = MaterialTheme.typography.headlineSmall)
                Text("Na telewizorze, który udostępnia ustawienia: Ustawienia → Rodzina → „Dodaj telewizor rodziny”. Wpisz pokazany kod.")
                OutlinedTextField(
                    value = code, onValueChange = { v -> code = v.filter { it.isDigit() }.take(6) }, singleLine = true,
                    label = { androidx.compose.material3.Text("Kod 6 cyfr") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.width(280.dp),
                )
                if (st.message.isNotEmpty()) Text(st.message)
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { c.findActivity()?.let { FamilySync.join(it, code) } }, enabled = code.length == 6) { Text("Pobierz ustawienia") }
                    Button(onClick = { open = false }) { Text("Anuluj") }
                }
            }
        }
    }
}

private fun Modifier.androidx_background(): Modifier =
    background(androidx.compose.ui.graphics.Color(0xFF1C1C1E), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
