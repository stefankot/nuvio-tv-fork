package com.nuvio.tv.fork.tv.most

import com.nuvio.tv.reshaped.livetv.LiveTvProgramme

/**
 * [list] z obrazem, numerem odcinka i opisem, których brakuje, wziętymi z tej samej emisji
 * (start w granicach 10 minut, ten sam tytuł) z [others] (przewodniki, które przegrały ranking).
 */
fun enrichProgrammes(list: List<LiveTvProgramme>, others: List<List<LiveTvProgramme>>): List<LiveTvProgramme> {
    if (list.none { it.image == null || it.episode == null || it.description == null }) return list
    var out: ArrayList<LiveTvProgramme>? = null
    for (other in others) {
        var j = 0
        for (i in list.indices) {
            val p = (out ?: list)[i]
            if (p.image != null && p.episode != null && p.description != null) continue
            while (j < other.size && other[j].startEpochMs < p.startEpochMs - 600_000L) j++
            var k = j
            while (k < other.size && other[k].startEpochMs <= p.startEpochMs + 600_000L) {
                val q = other[k]
                if (q.title.trim().equals(p.title.trim(), ignoreCase = true) && (q.image != null || q.episode != null || q.description != null)) {
                    val merged = p.copy(image = p.image ?: q.image, episode = p.episode ?: q.episode, description = p.description ?: q.description)
                    if (merged != p) {
                        if (out == null) out = ArrayList(list)
                        out[i] = merged
                    }
                    break
                }
                k++
            }
        }
    }
    return out ?: list
}
