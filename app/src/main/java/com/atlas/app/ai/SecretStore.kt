package com.atlas.app.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * API anahtarı ve yapay zekâ ayarları. Anahtar, Android Keystore'daki AES-GCM anahtarıyla
 * şifrelenip yalnızca bu uygulamanın özel depolamasında tutulur. Günlüklere yazılmaz.
 * Not: Cihazda çalışan bir uygulamanın anahtarı, cihaz ele geçirilirse okunabilir; bu yüzden
 * yalnızca kısıtlı/ücretsiz katman anahtarları kullanılmalıdır.
 */
class SecretStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("atlas_ai", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    fun saveApiKey(plain: String) {
        if (plain.isBlank()) {
            prefs.edit().remove(K_KEY).apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val enc = cipher.doFinal(plain.trim().toByteArray(Charsets.UTF_8))
        val packed = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(enc, Base64.NO_WRAP)
        prefs.edit().putString(K_KEY, packed).apply()
    }

    fun apiKey(): String? {
        val packed = prefs.getString(K_KEY, null) ?: return null
        return try {
            val (iv, enc) = packed.split(":")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(enc, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun hasApiKey(): Boolean = prefs.contains(K_KEY)

    var provider: String
        get() = prefs.getString(K_PROVIDER, "NONE") ?: "NONE"
        set(v) = prefs.edit().putString(K_PROVIDER, v).apply()

    var model: String
        get() = prefs.getString(K_MODEL, "") ?: ""
        set(v) = prefs.edit().putString(K_MODEL, v.trim()).apply()

    var baseUrl: String
        get() = prefs.getString(K_BASE, "") ?: ""
        set(v) = prefs.edit().putString(K_BASE, v.trim().trimEnd('/')).apply()

    /** Kullanıcı yaş/uygunluk uyarısını okuyup onayladı mı. */
    var eligibilityAcknowledged: Boolean
        get() = prefs.getBoolean(K_ELIG, false)
        set(v) = prefs.edit().putBoolean(K_ELIG, v).apply()

    /** Arka planda 6 saatte bir haber eşitleme. Varsayılan: açık. */
    var backgroundSync: Boolean
        get() = prefs.getBoolean(K_BG, true)
        set(v) = prefs.edit().putBoolean(K_BG, v).apply()

    fun wipe() = prefs.edit().clear().apply()

    private companion object {
        const val ALIAS = "atlas_ai_key"
        const val K_KEY = "key"
        const val K_PROVIDER = "provider"
        const val K_MODEL = "model"
        const val K_BASE = "base"
        const val K_ELIG = "elig"
        const val K_BG = "bgsync"
    }
}
