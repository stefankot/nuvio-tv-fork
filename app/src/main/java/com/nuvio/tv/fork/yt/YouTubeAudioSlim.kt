package com.nuvio.tv.fork.yt

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Fork 19.11 (właściciel: „nie da się chodzić po panelu dźwięku” w YouTube). Przed YouTubeAudioTracks.tidy (One):
 * (1) kod języka z myślnikiem U+2010/U+2011 („en‐US”) → zwykły „-” (inaczej oryginał trafiał do „unknown”);
 * (2) jeden zestaw na język i rodzaj (AAC/mp4 przed Opus/webm) z jedną, najlepszą jakością — zamiast ~100 wierszy
 * (21 języków × 2 kodeki × kilka bitrate'ów) panel ma po wierszu na język. Obraz i napisy bez zmian.
 */
object YouTubeAudioSlim {
    fun prepare(manifest: ByteArray): ByteArray = runCatching {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(manifest))
        val period = doc.getElementsByTagName("Period").item(0) ?: return@runCatching manifest
        fun kids(n: Node, name: String) = (0 until n.childNodes.length).map { n.childNodes.item(it) }.filter { it.nodeName == name }
        fun attr(n: Node, a: String) = (n as? Element)?.getAttribute(a).orEmpty()
        fun isAudio(s: Node) = attr(s, "mimeType").startsWith("audio") || kids(s, "Representation").any { attr(it, "mimeType").startsWith("audio") }
        val sets = kids(period, "AdaptationSet").filter(::isAudio)
        if (sets.size <= 1) return@runCatching manifest
        sets.forEach { (it as Element).setAttribute("lang", normalize(attr(it, "lang"))) }
        val before = sets.sumOf { kids(it, "Representation").size }
        val keep = sets.groupBy { attr(it, "lang").lowercase() }.values.map { group ->
            group.firstOrNull { attr(it, "mimeType").contains("mp4") || kids(it, "Representation").any { r -> attr(r, "mimeType").contains("mp4") } } ?: group.first()
        }.toSet()
        sets.filter { it !in keep }.forEach { period.removeChild(it) }
        keep.forEach { set ->
            val reps = kids(set, "Representation")
            val best = reps.maxByOrNull { attr(it, "bandwidth").toLongOrNull() ?: 0L } ?: return@forEach
            reps.filter { it !== best }.forEach { set.removeChild(it) }
        }
        android.util.Log.i("ForkYtAudio", "audio sets ${sets.size} -> ${keep.size}, tracks $before -> ${keep.size}")
        val out = StringWriter()
        TransformerFactory.newInstance().newTransformer().transform(DOMSource(doc), StreamResult(out))
        out.toString().toByteArray(Charsets.UTF_8)
    }.getOrDefault(manifest)

    /** „en‐US (original)” → „en-US (original)”. */
    fun normalize(lang: String): String = lang.replace('‐', '-').replace('‑', '-').replace('‒', '-').replace('–', '-')
}
