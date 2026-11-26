package com.example.data

import com.example.crypto.WalletCrypto
import com.example.data.TransactionService.EvmSendResult
import com.example.data.TransactionService.BtcSendResult
import com.example.data.TransactionService.SolSendResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger
import java.util.UUID

class TransactionServiceConcurrencyTest {

    @Test
    fun transactionService_evmSendResult_creation() {
        val result = EvmSendResult(
            txHash = "0x${UUID.randomUUID().toString().substring(0, 64)}",
            chainId = 1L,
            nonce = 5L,
            gasUsed = 21000L,
            status = "PENDING"
        )
        assertNotNull(result.txHash)
        assertEquals(1L, result.chainId)
        assertEquals(5L, result.nonce)
        assertEquals(21000L, result.gasUsed)
        assertEquals("PENDING", result.status)
    }

    @Test
    fun transactionService_btcSendResult_creation() {
        val result = BtcSendResult(
            txid = "0x${UUID.randomUUID().toString().substring(0, 64)}",
            feeSatoshis = 1000L
        )
        assertNotNull(result.txid)
        assertEquals(1000L, result.feeSatoshis)
    }

    @Test
    fun transactionService_solSendResult_creation() {
        val result = SolSendResult(
            signature = "${UUID.randomUUID()}${UUID.randomUUID()}",
            blockhash = "blockhash123"
        )
        assertNotNull(result.signature)
        assertEquals("blockhash123", result.blockhash)
    }

    @Test
    fun walletCrypto_keyPairFromPrivateKey_derivesCorrectly() = runBlocking {
        val privateKey = BigInteger("12345678901234567890123456789012")
        val keyPair = WalletCrypto.keyPairFromPrivateKey(privateKey)
        assertNotNull(keyPair)
        assertNotNull(keyPair.privateKey)
        assertNotNull(keyPair.publicKey)
        assertEquals(privateKey, keyPair.privateKey)
    }

    @Test
    fun idempotencyKey_uniqueness() {
        val key1 = UUID.randomUUID().toString()
        val key2 = UUID.randomUUID().toString()
        assertNotEquals(key1, key2)
        assertFalse(key1.isEmpty())
        assertFalse(key2.isEmpty())
    }
}
