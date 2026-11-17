package com.nuvio.tv.fork.tv.guide

/** Pokrycie przewodnika: ile kanałów ma program i kolejność „kanały bez programu na końcu grupy”. */
object Coverage {
    data class Item(val key: String, val group: String, val hasProgramme: Boolean)

    fun message(withProgramme: Int, total: Int) = "$withProgramme z $total kanałów ma program"

    /** Zachowuje kolejność grup i kanałów, ale w każdej grupie kanały bez programu idą na koniec. */
    fun <T> sortedGroupTail(items: List<T>, group: (T) -> String, hasProgramme: (T) -> Boolean): List<T> {
        val order = LinkedHashMap<String, MutableList<T>>()
        items.forEach { order.getOrPut(group(it)) { ArrayList() }.add(it) }
        return order.values.flatMap { list -> list.filter(hasProgramme) + list.filterNot(hasProgramme) }
    }

    fun withoutProgramme(items: List<Item>) = items.filterNot { it.hasProgramme }
}
