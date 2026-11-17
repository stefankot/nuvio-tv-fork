package com.nuvio.tv.fork.tv

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A programme the viewer asked to be reminded of (mockup S07/S09 "Przypomnij mi"). */
data class Reminder(val channelUrl: String, val startMs: Long, val title: String, val channelName: String) {
    val key: String get() = "$channelUrl|$startMs"
}

/** Reminders kept on the device (app preferences), past ones dropped on load. */
object Reminders {
    private const val PREFS = "fork_reminders"
    private const val KEY = "list"
    private val _list = MutableStateFlow<List<Reminder>>(emptyList())
    val list: StateFlow<List<Reminder>> = _list.asStateFlow()
    private var loaded = false

    fun load(context: Context, nowMs: Long = System.currentTimeMillis()) {
        if (loaded) return
        loaded = true
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null).orEmpty()
        _list.value = decode(raw).filter { it.startMs > nowMs - 3_600_000 }
    }

    fun has(channelUrl: String, startMs: Long): Boolean = _list.value.any { it.channelUrl == channelUrl && it.startMs == startMs }

    fun toggle(context: Context, reminder: Reminder) {
        val now = _list.value
        _list.value = if (now.any { it.key == reminder.key }) now.filter { it.key != reminder.key } else (now + reminder).sortedBy { it.startMs }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, encode(_list.value)).apply()
    }

    internal fun encode(list: List<Reminder>): String =
        list.joinToString("\n") { listOf(it.channelUrl, it.startMs.toString(), it.title, it.channelName).joinToString("\t") { f -> f.replace('\t', ' ').replace('\n', ' ') } }

    internal fun decode(raw: String): List<Reminder> = raw.lineSequence().mapNotNull { line ->
        val f = line.split('\t')
        if (f.size < 4) null else f[1].toLongOrNull()?.let { Reminder(f[0], it, f[2], f[3]) }
    }.toList()
}
