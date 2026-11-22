package com.nuvio.tv.fork.subs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test 22.11: rozpoznanie polskiego języka napisów (stała decyzja: polskie wbudowane przed dodatkami). */
class PolishFirstTest {
    @Test fun `kody polskiego`() {
        listOf("pl", "PL", "pol", "Polish", "pl-PL", "polski").forEach { assertTrue(it, PolishFirst.isPl(it)) }
    }

    @Test fun `inne języki i brak`() {
        listOf(null, "", "en", "eng", "ita", "por", "spa").forEach { assertFalse("$it", PolishFirst.isPl(it)) }
    }
}
