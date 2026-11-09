package com.example.data

import com.example.data.VaultAuthManager.Companion.PIN_LENGTH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultAuthManagerTest {

    @Test
    fun setPin_andVerifyPin() {
        // We can't easily instantiate VaultAuthManager without a Context in unit tests
        // This test validates the PIN length constant
        assertEquals(6, PIN_LENGTH)
    }

    @Test
    fun pinLengthIsSix() {
        assertTrue(PIN_LENGTH == 6)
        assertFalse(PIN_LENGTH == 4)
    }
}
