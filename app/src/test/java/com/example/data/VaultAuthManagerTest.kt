package com.example.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaultAuthManagerTest {

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun pinLengthIsSix() {
        assertEquals(6, VaultAuthManager.Companion.PIN_LENGTH)
    }

    @Test
    fun setPin_andVerifyPin_succeeds() {
        val vault = VaultAuthManager(ctx)
        assertFalse(vault.hasPin())
        vault.setPin("123456")
        assertTrue(vault.hasPin())
        assertTrue(vault.verifyPin("123456"))
        assertEquals(0, vault.failedAttempts())
        assertFalse(vault.isLockedOut())
    }

    @Test
    fun verifyPin_rejectsWrongPin() {
        val vault = VaultAuthManager(ctx)
        vault.setPin("123456")
        assertFalse(vault.verifyPin("000000"))
        assertEquals(1, vault.failedAttempts())
    }

    @Test
    fun setPin_rejectsNonSixDigit() {
        val vault = VaultAuthManager(ctx)
        var threw = false
        try { vault.setPin("1234") } catch (e: IllegalArgumentException) { threw = true }
        assertTrue(threw)
        threw = false
        try { vault.setPin("1234567") } catch (e: IllegalArgumentException) { threw = true }
        assertTrue(threw)
        threw = false
        try { vault.setPin("abcdef") } catch (e: IllegalArgumentException) { threw = true }
        assertTrue(threw)
        assertFalse(vault.hasPin())
    }

    @Test
    fun escalatingLockout_afterFiveFailures() {
        val vault = VaultAuthManager(ctx)
        vault.setPin("123456")
        // 5 failed attempts -> lockout
        repeat(5) { vault.verifyPin("000000") }
        assertTrue(vault.isLockedOut())
        assertTrue(vault.lockoutRemainingMillis() > 0)
        // Wrong pin while locked out still counts / does not reset
        assertFalse(vault.verifyPin("123456"))
    }

    @Test
    fun successfulVerify_resetsFailedAttempts() {
        val vault = VaultAuthManager(ctx)
        vault.setPin("123456")
        vault.verifyPin("000000") // 1 failure
        assertEquals(1, vault.failedAttempts())
        vault.verifyPin("123456") // success resets
        assertEquals(0, vault.failedAttempts())
        assertFalse(vault.isLockedOut())
    }

    @Test
    fun clearPin_removesState() {
        val vault = VaultAuthManager(ctx)
        vault.setPin("123456")
        assertTrue(vault.hasPin())
        vault.clearPin()
        assertFalse(vault.hasPin())
    }
}
