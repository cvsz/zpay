package com.example.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BitcoinTxTest {

    @Test
    fun buildUnsignedP2wpkh_returnsSighash() {
        val input = BitcoinTx.Input(
            txid = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            vout = 0,
            valueSatoshis = 50000,
            compressedPubKey = ByteArray(33) { 0x02.toByte() } // dummy compressed pubkey
        )
        val outputs = listOf(
            BitcoinTx.Output(valueSatoshis = 49000, scriptPubKey = ByteArray(25) { 0x00.toByte() })
        )
        val unsigned = BitcoinTx.buildUnsignedP2wpkh(input, outputs)
        assertNotNull(unsigned.sighash)
        assertEquals(32, unsigned.sighash.size)
    }

    @Test
    fun p2wpkhScript_returns22Bytes() {
        val hash160 = ByteArray(20) { 0x42.toByte() }
        val script = BitcoinTx.p2wpkhScript(hash160)
        assertEquals(22, script.size)
        assertEquals(0x00.toByte(), script[0])
        assertEquals(0x14.toByte(), script[1])
    }
}
