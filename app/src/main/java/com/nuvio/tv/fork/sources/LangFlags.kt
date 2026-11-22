package com.nuvio.tv.fork.sources

/**
 * Fork 22.11 (właściciel: „odróżnij polski dźwięk od napisów”, dodatek TorrentClaw): języki w opisie źródła jako flagi,
 * osobno w linii 🔊 (dźwięk) i 💬 (napisy), np. „🔊 ac3 · 5.1 · 🇵🇱🇺🇸” / „💬 🇵🇱🇺🇸”. Flaga to para znaków, więc ciąg
 * liczony parami od początku (🇯🇵🇱🇹 = Japonia + Litwa, nie 🇵🇱). Sprawdzone na 60 źródłach TorrentClaw i ffprobe 2 plików.
 */
object LangFlags {
    private const val FLAG = """[\x{1F1E6}-\x{1F1FF}]"""
    private const val NOT_FLAG = """[^\n\x{1F1E6}-\x{1F1FF}]*"""
    private const val PL = """\x{1F1F5}\x{1F1F1}"""
    private val AUDIO_PL = Regex("""\x{1F50A}$NOT_FLAG(?:$FLAG{2})*$PL""")
    private val SUBS_PL = Regex("""\x{1F4AC}$NOT_FLAG(?:$FLAG{2})*$PL""")

    /** Dodatek podaje polski dźwięk (flaga 🇵🇱 w linii 🔊). */
    fun polishAudio(text: String): Boolean = AUDIO_PL.containsMatchIn(text)

    /** Dodatek podaje polskie napisy w pliku (flaga 🇵🇱 w linii 💬). */
    fun polishSubs(text: String): Boolean = SUBS_PL.containsMatchIn(text)

    /**
     * Tekst źródła dla reguł zestawów (`source_presets.json`): reguły są jednoliniowe (`^…$` bez `(?s)`), więc opis
     * w kilku liniach (TorrentClaw, Torrentio) nie trafiał w żaden szczebel. Polski dźwięk z flag → znacznik „pldub”,
     * żeby szczebel dalej zależał od rozdzielczości z reguł (np. „po polsku przy dobrej jakości”).
     */
    fun presetText(raw: String): String = raw.replace('\n', ' ') + if (polishAudio(raw)) " pldub" else ""
}
