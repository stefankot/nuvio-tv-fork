package com.nuvio.tv.ainarrator

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.time.YearMonth
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** App-local configuration; credential ciphertext is encrypted with the Android Keystore. */
internal class NarratorPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ai_narrator_v1", Context.MODE_PRIVATE)
    private val alias = "nuvio_ai_narrator_v1"

    fun load(): NarratorOptions = NarratorOptions(
        enabled = prefs.getBoolean("enabled", false),
        backend = runCatching { NarratorBackend.valueOf(prefs.getString("backend", "GEMINI")!!) }
            .getOrDefault(NarratorBackend.GEMINI),
        localEndpoint = prefs.getString("local_endpoint", "") ?: "",
        localSpeakerId = prefs.getString("local_speaker", "") ?: "",
        model = runCatching { NarratorModel.valueOf(prefs.getString("model", "FLASH_LITE")!!) }
            .getOrDefault(NarratorModel.FLASH_LITE),
        voice = prefs.getString("voice", "Kore") ?: "Kore",
        narratorVolume = prefs.getFloat("volume", 0.65f).coerceIn(0f, 1f),
        monthlyUsdLimit = prefs.getFloat("limit", 2f).toDouble().coerceIn(0.1, 100.0)
    )

    fun save(options: NarratorOptions) {
        prefs.edit()
            .putBoolean("enabled", options.enabled)
            .putString("backend", options.backend.name)
            .putString("local_endpoint", options.localEndpoint)
            .putString("local_speaker", options.localSpeakerId)
            .putString("model", options.model.name)
            .putString("voice", options.voice)
            .putFloat("volume", options.narratorVolume.coerceIn(0f, 1f))
            .putFloat("limit", options.monthlyUsdLimit.coerceIn(0.1, 100.0).toFloat())
            .apply()
    }

    fun apiKey(): String = secret("api_key")
    fun saveApiKey(value: String) = saveSecret("api_key", value)
    fun localToken(): String = secret("local_token")
    fun saveLocalToken(value: String) = saveSecret("local_token", value)

    private fun secret(field: String): String {
        val encrypted = prefs.getString("${field}_ciphertext", null) ?: return ""
        val iv = prefs.getString("${field}_nonce", null) ?: return ""
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun saveSecret(field: String, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            prefs.edit().remove("${field}_ciphertext").remove("${field}_nonce").apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString("${field}_ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString("${field}_nonce", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    @Synchronized fun reserve(usd: Double, limitUsd: Double): Boolean {
        val period = YearMonth.now().toString()
        val spent = if (prefs.getString("period", "") == period) prefs.getFloat("spent", 0f).toDouble() else 0.0
        if (spent + usd > limitUsd) return false
        // Conservative local reservation: cannot prevent charges from other devices or accounts.
        prefs.edit().putString("period", period).putFloat("spent", (spent + usd).toFloat()).commit()
        return true
    }

    fun spentThisMonth(): Double =
        if (prefs.getString("period", "") == YearMonth.now().toString())
            prefs.getFloat("spent", 0f).toDouble() else 0.0
}
