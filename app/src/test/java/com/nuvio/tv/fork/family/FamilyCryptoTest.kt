package com.nuvio.tv.fork.family

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy 22.11: szyfrowanie rodziny (AES-256-GCM, klucz z kodu parowania). */
class FamilyCryptoTest {
    @Test fun `skrót SHA-256 zgodny ze wzorcem`() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", FamilyCrypto.sha256Hex("abc"))
        assertEquals("00ff10", FamilyCrypto.hex(byteArrayOf(0, -1, 16)))
    }

    @Test fun `kod parowania ma 6 cyfr`() {
        repeat(50) {
            val c = FamilyCrypto.pairCode()
            assertEquals(6, c.length)
            assertTrue(c.all(Char::isDigit))
            assertNotEquals('0', c[0])
        }
    }

    @Test fun `skrót kodu stały i inny niż sam kod`() {
        val a = FamilyCrypto.pairLookup("123456")
        assertEquals(a, FamilyCrypto.pairLookup("123456"))
        assertEquals(64, a.length)
        assertNotEquals(a, FamilyCrypto.pairLookup("123457"))
        assertFalse(a.contains("123456"))
    }

    @Test fun `klucz z kodu - 32 bajty, ten sam kod ten sam klucz`() {
        val k = FamilyCrypto.codeKey("654321")
        assertEquals(32, k.size)
        assertArrayEquals(k, FamilyCrypto.codeKey("654321"))
        assertFalse(k.contentEquals(FamilyCrypto.codeKey("654322")))
    }

    @Test fun `szyfrowanie i odszyfrowanie`() {
        val key = FamilyCrypto.random(32)
        val plain = "profil: Kasia, ulubione 12".toByteArray()
        val a = FamilyCrypto.encrypt(key, plain)
        val b = FamilyCrypto.encrypt(key, plain)
        assertFalse("losowy IV — dwa szyfrogramy różne", a.contentEquals(b))
        assertEquals(12 + plain.size + 16, a.size)
        assertArrayEquals(plain, FamilyCrypto.decrypt(key, a))
        assertArrayEquals(plain, FamilyCrypto.decrypt(key, b))
    }

    @Test fun `zmieniony szyfrogram albo zły klucz nie odszyfrowuje`() {
        val key = FamilyCrypto.random(32)
        val data = FamilyCrypto.encrypt(key, "tajne".toByteArray())
        data[data.size - 1] = (data[data.size - 1].toInt() xor 1).toByte()
        assertTrue(runCatching { FamilyCrypto.decrypt(key, data) }.isFailure)
        val ok = FamilyCrypto.encrypt(key, "tajne".toByteArray())
        assertTrue(runCatching { FamilyCrypto.decrypt(FamilyCrypto.random(32), ok) }.isFailure)
    }
}
