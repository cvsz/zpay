package com.example.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletCryptoTest {

    @Test
    fun generateMnemonic_returns12Words() {
        val mnemonic = WalletCrypto.generateMnemonic(128)
        val words = mnemonic.split(" ")
        assertEquals(12, words.size)
    }

    @Test
    fun generateMnemonic_256Bit_returns24Words() {
        val mnemonic = WalletCrypto.generateMnemonic(256)
        val words = mnemonic.split(" ")
        assertEquals(24, words.size)
    }

    @Test
    fun isValidMnemonic_acceptsValidMnemonic() {
        val mnemonic = WalletCrypto.generateMnemonic(128)
        assertTrue(WalletCrypto.isValidMnemonic(mnemonic))
    }

    @Test
    fun isValidMnemonic_rejectsInvalidMnemonic() {
        assertFalse(WalletCrypto.isValidMnemonic("abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon invalid"))
        assertFalse(WalletCrypto.isValidMnemonic("not a valid mnemonic"))
        assertFalse(WalletCrypto.isValidMnemonic(""))
    }

    @Test
    fun mnemonicToSeed_produces64Bytes() {
        val mnemonic = WalletCrypto.generateMnemonic(128)
        val seed = WalletCrypto.mnemonicToSeed(mnemonic)
        assertEquals(64, seed.size)
    }

    @Test
    fun deriveSecp256k1_returnsKeyPair() {
        val mnemonic = WalletCrypto.generateMnemonic(128)
        val seed = WalletCrypto.mnemonicToSeed(mnemonic)
        val keyPair = WalletCrypto.deriveSecp256k1(seed, WalletCrypto.EVM_DERIVATION_PATH)
        assertNotNull(keyPair)
        assertNotNull(keyPair.privateKey)
        assertNotNull(keyPair.publicKey)
    }

    @Test
    fun evmAddress_startsWith0x() {
        val mnemonic = WalletCrypto.generateMnemonic(128)
        val seed = WalletCrypto.mnemonicToSeed(mnemonic)
        val keyPair = WalletCrypto.deriveSecp256k1(seed, WalletCrypto.EVM_DERIVATION_PATH)
        val address = WalletCrypto.evmAddress(keyPair)
        assertTrue(address.startsWith("0x"))
        assertEquals(42, address.length) // 0x + 40 hex chars
    }

    @Test
    fun bitcoinAddress_returnsBech32() {
        val mnemonic = WalletCrypto.generateMnemonic(128)
        val seed = WalletCrypto.mnemonicToSeed(mnemonic)
        val keyPair = WalletCrypto.deriveSecp256k1(seed, WalletCrypto.BTC_DERIVATION_PATH)
        val compressedPub = WalletCrypto.compressedPublicKey(keyPair.privateKey)
        val address = WalletCrypto.bitcoinAddress(compressedPub)
        assertTrue(address.startsWith("bc1"))
    }

    @Test
    fun solanaAddress_returnsBase58() {
        val mnemonic = WalletCrypto.generateMnemonic(128)
        val seed = WalletCrypto.mnemonicToSeed(mnemonic)
        val keyPair = WalletCrypto.deriveEd25519(seed, WalletCrypto.SOL_DERIVATION_PATH)
        val address = WalletCrypto.solanaAddress(keyPair.publicKey)
        assertNotNull(address)
        assertTrue(address.length > 32)
    }

    @Test
    fun signPersonalMessage_returns65Bytes() {
        val mnemonic = WalletCrypto.generateMnemonic(128)
        val seed = WalletCrypto.mnemonicToSeed(mnemonic)
        val keyPair = WalletCrypto.deriveSecp256k1(seed, WalletCrypto.EVM_DERIVATION_PATH)
        val message = "Hello ZWallet".toByteArray()
        val sig = WalletCrypto.signPersonalMessage(message, keyPair)
        assertEquals(65, sig.size)
    }

    @Test
    fun signHashLowS_returnsDerSignature() {
        val mnemonic = WalletCrypto.generateMnemonic(128)
        val seed = WalletCrypto.mnemonicToSeed(mnemonic)
        val keyPair = WalletCrypto.deriveSecp256k1(seed, WalletCrypto.BTC_DERIVATION_PATH)
        val hash = WalletCrypto.keccak256("test".toByteArray())
        val sig = WalletCrypto.signHashLowS(hash, keyPair.privateKey)
        assertTrue(sig.size > 0)
        assertEquals(0x01.toByte(), sig.last()) // sighash type
    }

    @Test
    fun hexAndUnhex_areInverses() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val hex = WalletCrypto.hex(data)
        assertTrue(hex.startsWith("0x"))
        assertTrue(data.contentEquals(WalletCrypto.unhex(hex)))
    }
}
