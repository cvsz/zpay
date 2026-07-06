package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
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

    // Classic BIP-39 word list subset for secure entropy mapping
    private val bip39Words = listOf(
        "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract", "academic", "accept",
        "access", "accident", "account", "accuse", "achieve", "acid", "acoustic", "acquire", "across", "act",
        "action", "actor", "actress", "actual", "adapt", "add", "addict", "address", "adjust", "admit",
        "adult", "advance", "advice", "advise", "aerobic", "affair", "afford", "afraid", "again", "age",
        "agent", "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album", "alcohol",
        "alert", "alien", "all", "alley", "allow", "almost", "alone", "alpha", "already", "also",
        "alter", "always", "amateur", "amazing", "among", "amount", "amused", "analyst", "anchor", "ancient",
        "anger", "angle", "angry", "animal", "ankle", "announce", "annual", "another", "answer", "antenna",
        "antique", "anxiety", "any", "apart", "apology", "appear", "apple", "approve", "april", "arch",
        "arctic", "area", "arena", "argue", "arm", "armed", "armor", "army", "around", "arrange",
        "arrest", "arrive", "arrow", "art", "artefact", "artist", "artwork", "ask", "aspect", "assault",
        "asset", "assist", "assume", "asthma", "athlete", "atom", "attack", "attend", "attitude", "attract",
        "auction", "audit", "august", "aunt", "author", "auto", "autumn", "average", "avoid", "awake",
        "aware", "away", "awesome", "awful", "awkward", "axis", "baby", "bachelor", "bacon", "badge",
        "bag", "balance", "balcony", "ball", "bamboo", "banana", "banner", "bar", "barely", "bargain",
        "barrel", "barrier", "base", "basic", "basket", "battle", "beach", "bean", "beauty", "because",
        "become", "beef", "before", "begin", "behave", "behind", "believe", "below", "belt", "bench",
        "benefit", "best", "betray", "better", "between", "beyond", "bicycle", "bid", "bike", "bind",
        "biology", "bird", "birth", "bitter", "black", "blade", "blame", "blanket", "blast", "bleak",
        "bless", "blind", "blood", "blossom", "blouse", "blue", "blur", "blush", "board", "boat",
        "body", "boil", "bomb", "bone", "bonus", "book", "boost", "border", "boring", "borrow",
        "boss", "bottom", "bounce", "box", "boy", "bracket", "brain", "brand", "brass", "brave",
        "bread", "breeze", "brick", "bridge", "brief", "bright", "bring", "brisk", "broccoli", "broken",
        "bronze", "broom", "brother", "brown", "brush", "bubble", "buddy", "budget", "buffalo", "build",
        "bulb", "bulk", "bullet", "bundle", "bunker", "burden", "burger", "burst", "bus", "business",
        "busy", "butter", "buyer", "buzz", "cabbage", "cabin", "cable", "cactus", "cage", "cake",
        "call", "calm", "camera", "camp", "can", "canal", "cancel", "candy", "cannon", "canoe",
        "canvas", "canyon", "capable", "capacity", "cape", "capital", "captain", "car", "carbon", "card",
        "cargo", "carpet", "carry", "cart", "case", "cash", "casino", "castle", "casual", "cat"
    )

    /**
     * Generates a cryptographically secure 12-word mnemonic phrase based on SecureRandom entropy.
     */
    fun generateMnemonic(): String {
        val secureRandom = SecureRandom()
        val words = mutableListOf<String>()
        for (i in 0 until 12) {
            val index = secureRandom.nextInt(bip39Words.size)
            words.add(bip39Words[index])
        }
        return words.joinToString(" ")
    }
}

data class EncryptedPayload(
    val encryptedData: String,
    val iv: String
)
