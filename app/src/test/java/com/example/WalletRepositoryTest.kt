package com.example

import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Regression tests for financial-integrity guards in WalletRepository. */
class WalletRepositoryTest {

    private val dao = object : WalletDao {
        private val transactions = mutableListOf<CryptoTransaction>()
        override fun getAssetBalances(): Flow<List<AssetBalance>> = flow { emit(emptyList()) }
        override suspend fun insertAssetBalance(balance: AssetBalance) = Unit
        override suspend fun insertAssetBalances(balances: List<AssetBalance>) = Unit
        override fun getTransactions(): Flow<List<CryptoTransaction>> = flow { emit(transactions.toList()) }
        override suspend fun findTransactionByIdempotencyKey(key: String): CryptoTransaction? =
            transactions.find { it.idempotencyKey == key }
        override suspend fun insertTransaction(tx: CryptoTransaction): Long {
            transactions.add(tx)
            return tx.id
        }
        override fun getActivePriceAlerts(): Flow<List<PriceAlert>> = flow { emit(emptyList()) }
        override fun getAllPriceAlerts(): Flow<List<PriceAlert>> = flow { emit(emptyList()) }
        override suspend fun insertPriceAlert(alert: PriceAlert) = Unit
        override suspend fun triggerPriceAlert(id: Long) = Unit
        override suspend fun deletePriceAlert(id: Long) = Unit
        override fun getAllKeys(): List<EncryptedKey> = emptyList()
        override suspend fun getKeyForChain(chain: String): EncryptedKey? = null
        override suspend fun insertKey(key: EncryptedKey) = Unit
        override suspend fun clearAllKeys() = Unit
        override fun getAllTradingRules(): Flow<List<AITradingRule>> = flow { emit(emptyList()) }
        override suspend fun insertTradingRule(rule: AITradingRule) = Unit
        override suspend fun toggleTradingRule(id: Long, enabled: Boolean) = Unit
        override suspend fun updateRuleTriggered(id: Long, timestamp: Long) = Unit
        override suspend fun deleteTradingRule(id: Long) = Unit
    }

    private val repo = WalletRepository(dao)

    @Test
    fun `validatePayment rejects unsupported symbol`() {
        assertFailsWith<IllegalArgumentException> {
            repo.validatePayment("XYZ", 1000)
        }
    }

    @Test
    fun `validatePayment rejects non-positive amount`() {
        assertFailsWith<IllegalArgumentException> {
            repo.validatePayment("BTC", 0)
        }
        assertFailsWith<IllegalArgumentException> {
            repo.validatePayment("BTC", -500)
        }
    }

    @Test
    fun `validatePayment accepts valid amount`() {
        repo.validatePayment("BTC", 1000)
        repo.validatePayment("ETH", 500_000_000_000_000_000L)
    }

    @Test
    fun `createPaymentIdempotent creates pending transaction`() = runBlocking {
        val key = UUID.randomUUID().toString()
        val tx = repo.createPaymentIdempotent(
            chain = "BTC",
            hash = "0xabc",
            type = "Receive",
            fromAddress = "0xfrom",
            toAddress = "0xto",
            amountMinorUnit = 1000,
            feeUsdMinorUnit = 100,
            idempotencyKey = key
        )
        assertEquals(TransactionStatus.PENDING, tx.status)
        assertEquals(key, tx.idempotencyKey)
        assertEquals(1000L, tx.amountMinorUnit)
    }

    @Test
    fun `createPaymentIdempotent rejects duplicate key without double credit`() = runBlocking {
        val key = UUID.randomUUID().toString()
        val first = repo.createPaymentIdempotent(
            chain = "BTC", hash = "0xabc", type = "Receive",
            fromAddress = "0xfrom", toAddress = "0xto",
            amountMinorUnit = 1000, feeUsdMinorUnit = 100,
            idempotencyKey = key
        )
        val second = repo.createPaymentIdempotent(
            chain = "BTC", hash = "0xdef", type = "Receive",
            fromAddress = "0xfrom", toAddress = "0xto",
            amountMinorUnit = 999999, feeUsdMinorUnit = 0,
            idempotencyKey = key // same key -> must return the original
        )
        assertEquals(first, second) // replay returns the original transaction (content-equal)
        assertEquals(1000L, second.amountMinorUnit) // not overwritten with 999999
    }

    @Test
    fun `createPaymentIdempotent rejects blank idempotency key`() = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            repo.createPaymentIdempotent(
                chain = "BTC", hash = "0xabc", type = "Receive",
                fromAddress = "0xfrom", toAddress = "0xto",
                amountMinorUnit = 1000, feeUsdMinorUnit = 100,
                idempotencyKey = ""
            )
        }
    }

    @Test
    fun `createPaymentIdempotent validates amount`() = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            repo.createPaymentIdempotent(
                chain = "BTC", hash = "0xabc", type = "Receive",
                fromAddress = "0xfrom", toAddress = "0xto",
                amountMinorUnit = -100, feeUsdMinorUnit = 100,
                idempotencyKey = UUID.randomUUID().toString()
            )
        }
    }

    @Test
    fun `settleTransaction rejects non-terminal target`() = runBlocking {
        val tx = repo.createPaymentIdempotent(
            chain = "BTC", hash = "0xabc", type = "Receive",
            fromAddress = "0xfrom", toAddress = "0xto",
            amountMinorUnit = 1000, feeUsdMinorUnit = 100,
            idempotencyKey = UUID.randomUUID().toString()
        )
        assertFailsWith<IllegalArgumentException> {
            repo.settleTransaction(tx.id, TransactionStatus.PENDING)
        }
    }

    private fun storedTx(id: Long): CryptoTransaction = runBlocking {
        dao.getTransactions().first().firstOrNull { it.id == id }
            ?: error("Transaction not found: $id")
    }

    @Test
    fun `settleTransaction rejects transition from terminal state`() = runBlocking {
        val tx = repo.createPaymentIdempotent(
            chain = "BTC", hash = "0xabc", type = "Receive",
            fromAddress = "0xfrom", toAddress = "0xto",
            amountMinorUnit = 1000, feeUsdMinorUnit = 100,
            idempotencyKey = UUID.randomUUID().toString()
        )
        repo.settleTransaction(tx.id, TransactionStatus.SUCCESS)
        assertEquals(TransactionStatus.SUCCESS, storedTx(tx.id).status)
        assertFailsWith<IllegalStateException> {
            repo.settleTransaction(tx.id, TransactionStatus.FAILED)
        }
        assertEquals(TransactionStatus.SUCCESS, storedTx(tx.id).status) // still SUCCESS
    }

    @Test
    fun `settleTransaction permits pending to success then is immutable`() = runBlocking {
        val tx = repo.createPaymentIdempotent(
            chain = "BTC", hash = "0xabc", type = "Receive",
            fromAddress = "0xfrom", toAddress = "0xto",
            amountMinorUnit = 1000, feeUsdMinorUnit = 100,
            idempotencyKey = UUID.randomUUID().toString()
        )
        val settled = repo.settleTransaction(tx.id, TransactionStatus.SUCCESS)
        assertEquals(TransactionStatus.SUCCESS, settled.status)
        assertEquals(TransactionStatus.SUCCESS, storedTx(tx.id).status)
        assertTrue(TransactionStateMachine.isTerminal(storedTx(tx.id).status))
    }
}
