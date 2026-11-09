package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.crypto.WalletCrypto
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecureKeyVault {
    private const val KEY_ALIAS = "ZWalletSecureHardwareKeyAlias"
    private const val PROVIDER = "AndroidKeyStore"
    private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"

    init {
        setupKeystore()
    }

    fun setupKeystore() {
        try {
            val keyStore = KeyStore.getInstance(PROVIDER)
            keyStore.load(null)
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
                
                // Attempt to request StrongBox (Secure Enclave / Hardware backed HSM)
                val builder = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .setKeySize(256)

                // StrongBox is available on Android 9+ (API 28+) and selected hardware devices
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    try {
                        builder.setIsStrongBoxBacked(true)
                        keyGenerator.init(builder.build())
                        keyGenerator.generateKey()
                    } catch (e: Exception) {
                        // Fallback to standard TEE (Trusted Execution Environment) hardware-backed keystore if StrongBox HSM is unsupported
                        val fallbackBuilder = KeyGenParameterSpec.Builder(
                            KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                        )
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(true)
                            .setKeySize(256)
                        keyGenerator.init(fallbackBuilder.build())
                        keyGenerator.generateKey()
                    }
                } else {
                    keyGenerator.init(builder.build())
                    keyGenerator.generateKey()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER)
        keyStore.load(null)
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: throw IllegalStateException("Hardware-backed SecretKey not found in Android Keystore")
    }

    /**
     * Encrypts a plain text string (e.g. mnemonic phrase or raw private key) using the hardware-backed AES-GCM key.
     */
    fun encrypt(plainText: String): EncryptedPayload {
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        return EncryptedPayload(encryptedBase64, ivBase64)
    }

    /**
     * Decrypts an encrypted payload using the hardware-backed AES-GCM key.
     */
    fun decrypt(encryptedBase64: String, ivBase64: String): String {
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /** Generates a standards-compliant BIP-39 mnemonic (128-bit entropy, 12 words, valid checksum). */
    fun generateMnemonic(): String = WalletCrypto.generateMnemonic(128)
}

data class EncryptedPayload(
    val encryptedData: String,
    val iv: String
)
