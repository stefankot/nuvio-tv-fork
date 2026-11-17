package com.nuvio.tv.fork.tv.most

/** Dodatkowe dane emisji z przewodnika (XMLTV). */
data class ProgrammeExtra(
    val subTitle: String? = null,
    val episode: String? = null,
    val age: String? = null,
    val year: Int? = null,
    val country: String? = null,
    val categories: List<String> = emptyList(),
    val credits: List<Pair<String, String>> = emptyList(),
) {
    val isEmpty get() = subTitle == null && episode == null && age == null && year == null && country == null && categories.isEmpty() && credits.isEmpty()
}

/** Jedyny punkt, przez który parser przewodnika oddaje dodatkowe dane modułowi TV (B02). */
object ForkTvHooks {
    @Volatile var sink: ((title: String, startEpochMs: Long, extra: ProgrammeExtra) -> Unit)? = null
    fun onProgrammeRead(title: String, startEpochMs: Long, extra: ProgrammeExtra) { sink?.invoke(title, startEpochMs, extra) }
}
