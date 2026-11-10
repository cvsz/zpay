package com.example.data

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local vault authentication.
 *
 * - PIN is stored as PBKDF2-HMAC-SHA256 hash (120000 iterations, 16-byte salt) in app-private
 *   preferences excluded from backup. Verification is constant-time.
 * - Repeated failures trigger escalating lockout.
 * - OS biometrics via real BiometricPrompt; authenticator set selected per API level.
 *
 * Client-side access control for a non-custodial local vault.
 */
class VaultAuthManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasPin(): Boolean = prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    fun setPin(pin: String) {
        require(pin.length == PIN_LENGTH && pin.all { it.isDigit() }) {
            "PIN must be exactly $PIN_LENGTH digits"
        }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt(KEY_ITER, ITERATIONS)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (isLockedOut()) return false
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_HASH, null) ?: return false
        val iter = prefs.getInt(KEY_ITER, ITERATIONS)
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)
        val actual = pbkdf2(pin, salt, iter)
        val matches = MessageDigest.isEqual(expected, actual)
        if (matches) {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCKOUT_UNTIL, 0L).apply()
        } else {
            registerFailedAttempt()
        }
        return matches
    }

    fun failedAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun lockoutRemainingMillis(): Long {
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun isLockedOut(): Boolean = lockoutRemainingMillis() > 0

    fun clearPin() {
        prefs.edit().remove(KEY_SALT).remove(KEY_HASH).remove(KEY_ITER)
            .remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKOUT_UNTIL).apply()
    }

    fun canUseBiometric(context: Context): Int =
        BiometricManager.from(context).canAuthenticate(authenticators())

    fun authenticateBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onError(errString.toString())
                }
            }
            override fun onAuthenticationFailed() { /* allow retry */ }
        })
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators())
        if (!usesDeviceCredential()) {
            builder.setNegativeButtonText("Use PIN")
        }
        prompt.authenticate(builder.build())
    }

    fun usesDeviceCredential(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            (authenticators() and BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0

    fun authenticators(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        }

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun registerFailedAttempt() {
        val attempts = failedAttempts() + 1
        val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
        if (attempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
            val exponent = (attempts - MAX_ATTEMPTS_BEFORE_LOCKOUT).coerceAtMost(4)
            val lockMillis = BASE_LOCKOUT_MILLIS shl exponent
            editor.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + lockMillis)
        }
        editor.apply()
    }

    companion object {
        private const val PREFS = "device.xml"
        private const val KEY_SALT = "vault_salt"
        private const val KEY_HASH = "vault_hash"
        private const val KEY_ITER = "vault_iter"
        private const val KEY_FAILED_ATTEMPTS = "vault_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "vault_lockout_until"
        const val PIN_LENGTH = 6
        private const val ITERATIONS = 120_000
        private const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 5
        private const val BASE_LOCKOUT_MILLIS = 30_000L
    }
}
