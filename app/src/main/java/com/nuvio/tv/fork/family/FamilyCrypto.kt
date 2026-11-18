package com.nuvio.tv.fork.family

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Szyfrowanie rodziny: AES-256-GCM; klucz z 6-cyfrowego kodu przez PBKDF2 (tylko do przekazania klucza rodziny). */
internal object FamilyCrypto {
    private val rnd = SecureRandom()

    fun random(bytes: Int): ByteArray = ByteArray(bytes).also { rnd.nextBytes(it) }
    fun hex(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }
    fun sha256Hex(text: String): String = hex(MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)))
    fun b64(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
    fun unb64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)

    /** Kod parowania: 6 cyfr. */
    fun pairCode(): String = (rnd.nextInt(900_000) + 100_000).toString()
    /** Skrót kodu, po którym serwer znajduje parowanie (serwer nie zna samego kodu). */
    fun pairLookup(code: String): String = sha256Hex("nuvio-pair:v1:$code")
    /** Klucz z kodu: wolny (PBKDF2, 200 000 rund), żeby zgadywanie kosztowało. */
    fun codeKey(code: String): ByteArray {
        val spec = PBEKeySpec(code.toCharArray(), "nuvio-pair-key:v1".toByteArray(), 200_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    /** IV (12 B) + szyfrogram z tagiem. */
    fun encrypt(key: ByteArray, plain: ByteArray): ByteArray {
        val iv = random(12)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return iv + c.doFinal(plain)
    }

    fun decrypt(key: ByteArray, data: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, data, 0, 12))
        return c.doFinal(data, 12, data.size - 12)
    }
}
