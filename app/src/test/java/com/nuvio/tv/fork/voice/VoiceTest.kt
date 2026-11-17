package com.nuvio.tv.fork.voice

import com.nuvio.tv.fork.ai.Embedder
import com.nuvio.tv.fork.ai.EmbedderUnavailable
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTest {
    private val channels = EntityIndex(listOf(
        Entity("tvp1", listOf("TVP 1")), Entity("tvp2", listOf("TVP 2")), Entity("polsat", listOf("Polsat")), Entity("tvn", listOf("TVN")),
        Entity("tvn24", listOf("TVN 24")), Entity("polsat_sport2", listOf("Polsat Sport 2")), Entity("hbo", listOf("HBO")), Entity("tvpinfo", listOf("TVP Info")),
        Entity("tvpsport", listOf("TVP Sport")), Entity("tv4", listOf("TV4")), Entity("canal", listOf("Canal+ Premium")), Entity("minimini", listOf("MiniMini+")),
    ))
    private val titles = EntityIndex(listOf(Entity("t1", listOf("Pan Tadeusz")), Entity("t2", listOf("Klan")), Entity("t3", listOf("Wiedźmin")), Entity("t4", listOf("Matrix")), Entity("t5", listOf("Breaking Bad"))))
    private val ctx = VoiceContext(channels = channels, titles = titles)
    private val parser = RuleParser()

    @Test fun `co najmniej 60 wzorcow i po 3 przyklady`() {
        assertTrue(Patterns.all.size >= 60)
        assertTrue(Patterns.all.all { it.examples.size >= 3 })
        assertEquals(Patterns.all.size, Patterns.all.map { it.id }.toSet().size)
    }

    @Test fun `wszystkie przyklady trafiaja we wlasciwy wzorzec z trafnoscia 95 procent`() {
        val misses = ArrayList<String>(); var total = 0
        for (p in Patterns.all) for (e in p.examples) {
            total++
            val got = parser.parseNow(e, ctx)?.id
            if (got != p.id) misses += "'$e' -> $got (oczekiwano ${p.id})"
        }
        assertTrue("trafnosc ${100 - 100 * misses.size / total}%: $misses", misses.size.toDouble() / total <= 0.05)
    }

    @Test fun `poza lista nie ma bledow akcji`() {
        val outside = listOf("jaka jest pogoda jutro", "opowiedz kawał", "ile to dwa plus dwa", "zamów pizzę", "która jest godzina", "kto jest prezydentem", "zadzwoń do mamy",
            "dobranoc", "co słychać", "jak się masz", "ustaw budzik na siódmą", "kup bilety", "czy będzie padać", "przetłumacz to na angielski", "odkurzacz włącz się",
            "jak ugotować jajko", "zapal światło w kuchni", "gdzie jest najbliższa apteka", "pokaż mi zdjęcia kota", "jaki mamy dzień", "napisz wiadomość do taty",
            "co to jest czarna dziura", "otwórz drzwi garażowe", "puść muzykę w kuchni", "wyślij maila", "zamów taksówkę", "ile kosztuje bitcoin", "sprawdź pocztę",
            "podaj przepis na pierogi", "zrób zakupy")
        val wrong = outside.filter { u -> parser.parseNow(u, ctx).let { it != null && it.confidence >= Confidence.SURE && it.id !in setOf("play.title", "channel.play") } }
        assertTrue("błędne akcje: $wrong", wrong.isEmpty())
    }

    @Test fun `rozstrzyganie kanal czy tytul`() {
        assertEquals("channel.play", parser.parseNow("włącz TVP 1", ctx)?.id); assertEquals("tvp1", parser.parseNow("włącz TVP 1", ctx)?.params?.get("kanal_id"))
        assertEquals("play.title", parser.parseNow("włącz Pana Tadeusza", ctx)?.id)
        assertEquals("subs.on", parser.parseNow("włącz napisy", ctx)?.id)
    }

    @Test fun `80 wypowiedzi nazw kanalow i tytulow w tym bledy rozpoznawania`() {
        val ch = listOf(
            "TVP 1" to "tvp1", "tvp jeden" to "tvp1", "jedynka" to "tvp1", "tefałpe jeden" to "tvp1", "tefałpe 1" to "tvp1", "TVP1" to "tvp1", "tvp dwa" to "tvp2", "dwójka" to "tvp2", "tefałpe dwa" to "tvp2",
            "Polsat" to "polsat", "na Polsacie" to "polsat", "polsacie" to "polsat", "polsat sport dwa" to "polsat_sport2", "polsat sport 2" to "polsat_sport2", "Polsat Sport 2" to "polsat_sport2", "polsat sport dwójka" to "polsat_sport2",
            "TVN" to "tvn", "tefałen" to "tvn", "w TVN-ie" to "tvn", "tvn dwadzieścia cztery" to "tvn24", "tvn 24" to "tvn24", "TVN 24" to "tvn24", "tefałen 24" to "tvn24",
            "HBO" to "hbo", "habeo" to "hbo", "na HBO" to "hbo", "tvp info" to "tvpinfo", "tefałpe info" to "tvpinfo", "TVP Info" to "tvpinfo", "tvp sport" to "tvpsport", "tefałpe sport" to "tvpsport", "TVP Sporcie" to "tvpsport",
            "canal plus premium" to "canal", "Canal+ Premium" to "canal", "kanał premium canal" to "canal", "minimini" to "minimini", "mini mini plus" to "minimini", "MiniMini+" to "minimini",
            "polsatu" to "polsat", "polsat sportu 2" to "polsat_sport2", "tvp jedynka" to "tvp1", "tvpi nfo" to "tvpinfo", "tvn dwadziescia cztery" to "tvn24", "polsad" to "polsat", "polsat sport dwa" to "polsat_sport2", "tvp dwójce" to "tvp2",
        )
        val tt = listOf(
            "Pan Tadeusz" to "t1", "pana tadeusza" to "t1", "panu tadeuszowi" to "t1", "pan tadeusz" to "t1", "Klan" to "t2", "klanu" to "t2", "w Klanie" to "t2", "klan" to "t2",
            "Wiedźmin" to "t3", "wiedzmina" to "t3", "wiedźminie" to "t3", "wiedzmin" to "t3", "wiedźmin" to "t3", "Matrix" to "t4", "matriksa" to "t4", "matrixie" to "t4", "matrix" to "t4", "matriks" to "t4",
            "Breaking Bad" to "t5", "breaking bad" to "t5", "brejking bad" to "t5", "breaking badzie" to "t5", "breaking bed" to "t5",
        )
        var ok = 0; var total = 0; val bad = ArrayList<String>()
        for ((q, id) in ch) { total++; if (channels.match(q).firstOrNull()?.id == id) ok++ else bad += "kanał '$q' -> ${channels.match(q).firstOrNull()?.id} (chciano $id)" }
        for ((q, id) in tt) { total++; if (titles.match(q).firstOrNull()?.id == id) ok++ else bad += "tytuł '$q' -> ${titles.match(q).firstOrNull()?.id} (chciano $id)" }
        assertTrue("$total wypowiedzi, trafionych $ok: $bad", total >= 80 && ok.toDouble() / total >= 0.9)
    }

    @Test fun `silnik mowy wg pamieci i klucz klawisza`() {
        assertEquals(SpeechEngine.WHISPER_SMALL, SpeechEngineChoice.auto(4000, true)); assertEquals(SpeechEngine.SYSTEM, SpeechEngineChoice.auto(2000, true))
        assertEquals(SpeechEngine.WHISPER_BASE, SpeechEngineChoice.auto(2000, false, whisperBaseRequested = true)); assertEquals(SpeechEngine.SYSTEM, SpeechEngineChoice.auto(4000, false))
        assertEquals(SpeechEngine.API, SpeechEngineChoice.auto(2000, false, hasApiKey = true)); assertEquals(SpeechEngine.SYSTEM, SpeechEngineChoice.auto(4000, true, SpeechEngine.SYSTEM))
        assertTrue(KeyFilter.shouldIntercept(KeyFilter.KEYCODE_ASSIST)); assertFalse(KeyFilter.shouldIntercept(23)); assertFalse(KeyFilter.shouldIntercept(KeyFilter.KEYCODE_ASSIST, serviceEnabled = false))
        assertTrue(KeyFilter.shouldIntercept(231, chosen = 231))
    }

    @Test fun `progi pewnosci i potwierdzenia`() {
        fun c(id: String, conf: Float, confirm: Boolean = false) = VoiceCommandCall(id, emptyMap(), conf, confirm)
        assertTrue(Confidence.decide(listOf(c("a", 0.95f))) is Confidence.Decision.Run)
        assertTrue(Confidence.decide(listOf(c("a", 0.6f))) is Confidence.Decision.Suggest)
        assertTrue(Confidence.decide(listOf(c("a", 0.9f), c("b", 0.88f))) is Confidence.Decision.Suggest)
        assertTrue(Confidence.decide(listOf(c("profile.switch", 0.95f, true))) is Confidence.Decision.Confirm)
        assertEquals(3, (Confidence.decide(listOf(c("a", .6f), c("b", .5f), c("c", .4f), c("d", .3f))) as Confidence.Decision.Suggest).options.size)
        assertTrue(Confidence.decide(emptyList()) is Confidence.Decision.Nothing)
    }

    private class BagEmbedder : Embedder {
        var down = false
        override suspend fun embed(texts: List<String>): List<FloatArray> {
            if (down) throw EmbedderUnavailable()
            return texts.map { t -> FloatArray(256).also { v -> Text.norm(t).split(' ').forEach { w -> v[(w.hashCode() and 0x7fffffff) % 256] += 1f } } }
        }
    }

    @Test fun `dopasowanie znaczeniowe na atrapie embeddera`() = runBlocking {
        val emb = BagEmbedder(); val sem = SemanticParser(emb, threshold = 0.5f)
        assertEquals("subs.on", sem.parse("włącz napisy", ctx)?.id)
        assertNull(sem.parse("zzz qqq xxx", ctx))
        emb.down = true; assertNull(sem.parse("włącz napisy", ctx))
    }

    @Test fun `parser modelowy: poprawne narzedzie, nieznane odrzucone, limit czasu`() = runBlocking {
        val ok = object : LlmClient { override suspend fun choose(utterance: String, tools: List<LlmTool>, context: List<String>) = LlmChoice.Tool("mood.light", mapOf("x" to "y")) }
        assertEquals("mood.light", LlmParser(ok).parse("coś lekkiego ale nie komedię", ctx)?.id)
        val unknown = object : LlmClient { override suspend fun choose(utterance: String, tools: List<LlmTool>, context: List<String>) = LlmChoice.Tool("format.disk") }
        assertNull(LlmParser(unknown).parse("x", ctx))
        val slow = object : LlmClient { override suspend fun choose(utterance: String, tools: List<LlmTool>, context: List<String>): LlmChoice { delay(500); return LlmChoice.Tool("mood.light") } }
        assertNull(LlmParser(slow, timeoutMs = 50).parse("x", ctx))
        val ask = object : LlmClient { override suspend fun choose(utterance: String, tools: List<LlmTool>, context: List<String>) = LlmChoice.Ask("Jaki gatunek?") }
        val p = LlmParser(ask); assertNull(p.parse("coś", ctx)); assertEquals("Jaki gatunek?", p.lastQuestion)
        assertNull(LlmParser(null).parse("coś", ctx)) // bez klucza
    }

    @Test fun `szyna: reguly przed znaczeniem`() = runBlocking {
        val chain = ParserChain(listOf(parser, SemanticParser(BagEmbedder(), threshold = 0.5f)))
        assertEquals("pause", chain.parse("pauza", ctx)?.id); assertNotNull(chain.parse("zatrzymaj odtwarzanie", ctx))
    }
}
