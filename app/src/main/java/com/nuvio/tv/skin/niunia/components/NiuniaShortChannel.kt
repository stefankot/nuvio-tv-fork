package com.nuvio.tv.skin.niunia.components

/**
 * Krótka nazwa kanału (Z-1, makieta `Nuvio.shortChannel`): bez prefiksu kraju („PL:”, „PL |”, „[PL]”, „UK -”), bez końcówek jakości
 * (HD, FHD, UHD, SD, 4K, HEVC, H265, RAW, +1) i bez „ TV” na końcu, jeśli zostają ≥ 3 znaki. „PL: Trwam TV” → „Trwam”,
 * „PL | TVN 24 HD” → „TVN 24”, „[PL] Polsat FHD” → „Polsat”, „TV Puls” → bez zmian. Pełna nazwa zostaje w opisie dostępności.
 */
fun niuniaShortChannel(name: String): String {
    // Przegląd 27.11, ekran 12: najpierw wspólne czyszczenie nazw (bez „Strong8K ·”, „PL VIP:”, „ᴴᴰ”, normalna wielkość liter).
    val orig = com.nuvio.tv.fork.tv.names.ChannelNames.shown(name).trim()
    var s = orig.replace(Regex("""^(\[[A-Za-z]{2,3}\]|[A-Za-z]{2,3}\s*[:|]|[A-Za-z]{2,3}\s+-)\s*"""), "")
    val q = Regex("""\s+(FHD|UHD|HD|SD|4K|HEVC|H\.?265|RAW|\+1)$""", RegexOption.IGNORE_CASE)
    while (q.containsMatchIn(s)) s = s.replace(q, "")
    val tv = Regex("""\s+TV$""", RegexOption.IGNORE_CASE)
    if (tv.containsMatchIn(s) && s.replace(tv, "").length >= 3) s = s.replace(tv, "")
    return s.trim().ifEmpty { orig }
}

/** Podpis kafelka (Z-1): tytuł skrócony tak, żeby „ · kanał” zawsze się zmieścił w budżecie [chars] znaków na dwie linie. */
fun niuniaTileTitle(title: String, channel: String?, chars: Int = 32): String {
    if (channel.isNullOrBlank()) return title
    val room = chars - channel.length - 3
    return if (title.length <= room || room < 6) title else title.take(room - 1).trimEnd() + "…"
}
