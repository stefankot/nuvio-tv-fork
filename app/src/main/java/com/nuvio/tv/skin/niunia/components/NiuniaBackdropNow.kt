package com.nuvio.tv.skin.niunia.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf

/**
 * Przegląd 27.11 (CAŁA APLIKACJA): adres obrazu tła bieżącego ekranu (hero Głównej, Szczegółów, źródeł, odcinka). Menu boczne
 * i okna nad ekranem rysują pod sobą jego gotową rozmytą kopię zamiast szarego/czarnego przyciemnienia. Stos: ekran na wierzchu
 * (np. Szczegóły nad Główną) wygrywa, po jego zamknięciu wraca poprzedni.
 */
object NiuniaBackdropNow {
    private val stack = mutableStateListOf<String>()
    val current: String? get() = stack.lastOrNull()
    internal fun push(url: String) { stack.add(url) }
    internal fun pop(url: String) { val i = stack.lastIndexOf(url); if (i >= 0) stack.removeAt(i) }
}

/** Zgłasza [url] jako tło bieżącego ekranu, dopóki wołający jest na ekranie. */
@Composable
fun NiuniaBackdropNowEffect(url: Any?) {
    val u = (url as? String)?.takeIf { it.isNotBlank() } ?: return
    DisposableEffect(u) { NiuniaBackdropNow.push(u); onDispose { NiuniaBackdropNow.pop(u) } }
}
