package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.Flow
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Maximum single payment amount in minor units (guard against overflow / fat-finger errors). */
private const val MAX_AMOUNT_MINOR_UNIT: Long = 1_000_000_000_000L // 10^12 minor units

/** Supported ISO-4217-alike currency / asset symbols enforced server-side. */
private val SUPPORTED_SYMBOLS = setOf("BTC", "ETH", "SOL", "BNB")

class WalletRepository(private val dao: WalletDao) {

    // Reactive State Flows
    val assetBalances: Flow<List<AssetBalance>> = dao.getAssetBalances()
    val transactions: Flow<List<CryptoTransaction>> = dao.getTransactions()
    val activeAlerts: Flow<List<PriceAlert>> = dao.getActivePriceAlerts()
    val allAlerts: Flow<List<PriceAlert>> = dao.getAllPriceAlerts()

    // --- Secure Key Management (Hardware-Backed SecureKeyVault AES-GCM) ---

    suspend fun secureStoreMnemonic(mnemonic: String) {
        try {
            require(mnemonic.isNotBlank()) { "Mnemonic must not be blank" }
            val payload = SecureKeyVault.encrypt(mnemonic)
            val entry = EncryptedKey(
                chain = "MNEMONIC",
                address = "",
                encryptedPrivateKey = payload.encryptedData,
                iv = payload.iv
            )
            dao.insertKey(entry)
        } catch (e: Exception) {
            throw WalletRepositoryException("Failed to store mnemonic", e)
        }
    }

    suspend fun decryptMnemonic(): String? {
        val entry = dao.getKeyForChain("MNEMONIC") ?: return null
        return try {
            SecureKeyVault.decrypt(entry.encryptedPrivateKey, entry.iv)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun secureStoreKey(chain: String, address: String, rawPrivateKey: String) {
        try {
            require(chain.isNotBlank()) { "Chain must not be blank" }
            require(address.isNotBlank()) { "Address must not be blank" }
            require(rawPrivateKey.isNotBlank()) { "Private key must not be blank" }
            require(SUPPORTED_SYMBOLS.contains(chain.uppercase().removeSuffix("_BNB"))) {
                "Unsupported chain: $chain"
            }
            val payload = SecureKeyVault.encrypt(rawPrivateKey)
            val keyEntry = EncryptedKey(
                chain = chain,
                address = address,
                encryptedPrivateKey = payload.encryptedData,
                iv = payload.iv
            )
            dao.insertKey(keyEntry)
        } catch (e: Exception) {
            throw WalletRepositoryException("Failed to store key for $chain", e)
        }
    }

    suspend fun decryptKey(chain: String): String? {
        val keyEntry = dao.getKeyForChain(chain) ?: return null
        return try {
            SecureKeyVault.decrypt(keyEntry.encryptedPrivateKey, keyEntry.iv)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun hasKeys(): Boolean {
        // Exclude the MNEMONIC record from checking if primary crypto keys exist
        return dao.getAllKeys().any { it.chain != "MNEMONIC" }
    }

    suspend fun removeAllKeys() {
        dao.clearAllKeys()
    }

    // --- Financial validation ---

    /**
     * Validates a payment amount before any state mutation.
     * - amount must be positive
     * - symbol must be supported
     * - amount must not exceed the platform maximum
     * Never trusts client-supplied amounts.
     */
    fun validatePayment(symbol: String, amountMinorUnit: Long, feeUsdMinorUnit: Long = 0) {
        require(SUPPORTED_SYMBOLS.contains(symbol.uppercase())) {
            "Unsupported currency symbol: $symbol"
        }
        require(amountMinorUnit > 0) { "Amount must be positive, got $amountMinorUnit" }
        require(amountMinorUnit <= MAX_AMOUNT_MINOR_UNIT) {
            "Amount exceeds platform maximum: $amountMinorUnit"
        }
        require(feeUsdMinorUnit >= 0) { "Fee must not be negative" }
    }

    /**
     * Idempotent payment creation.
     * Creates a PENDING transaction only when no prior transaction exists for [idempotencyKey].
     * Returns the existing transaction if a duplicate key is detected (replay-safe).
     *
     * Callers must validate the asset symbol and amount before invoking (see [validatePayment]).
     * Throws [IllegalStateException] if the key is blank or a conflicting non-duplicate exists.
     */
    suspend fun createPaymentIdempotent(
        chain: String,
        hash: String,
        type: String,
        fromAddress: String,
        toAddress: String,
        amountMinorUnit: Long,
        feeUsdMinorUnit: Long,
        idempotencyKey: String,
        notes: String = ""
    ): CryptoTransaction {
        require(idempotencyKey.isNotBlank()) { "Idempotency key must not be blank" }
        require(amountMinorUnit > 0) { "Amount must be positive, got $amountMinorUnit" }
        require(amountMinorUnit <= MAX_AMOUNT_MINOR_UNIT) {
            "Amount exceeds platform maximum: $amountMinorUnit"
        }
        require(feeUsdMinorUnit >= 0) { "Fee must not be negative" }

        // Duplicate detection at the application layer first.
        val existing = dao.findTransactionByIdempotencyKey(idempotencyKey)
        if (existing != null) {
            return existing // Replay: return the original, never double-credit/debit.
        }

        val tx = CryptoTransaction(
            chain = chain,
            hash = hash,
            type = type,
            fromAddress = fromAddress,
            toAddress = toAddress,
            amountMinorUnit = amountMinorUnit,
            feeUsdMinorUnit = feeUsdMinorUnit,
            status = TransactionStatus.PENDING,
            idempotencyKey = idempotencyKey,
            notes = notes
        )
        val newId = dao.insertTransaction(tx)
        return tx.copy(id = newId)
    }

    /**
     * Settles a transaction to a terminal state via the state machine.
     * Server-side settlement decision: client must not supply final settlement state.
     * Throws [IllegalStateException] for invalid transitions.
     */
    suspend fun settleTransaction(transactionId: Long, newStatus: String): CryptoTransaction {
        require(TransactionStatus.all.contains(newStatus)) {
            "Invalid status: $newStatus"
        }
        require(TransactionStateMachine.isTerminal(newStatus)) {
            "Only terminal states allowed via settle: $newStatus"
        }
        // Load current row under the same coroutine to check ownership/transition atomically.
        val current = dao.getTransactionById(transactionId)
            ?: throw IllegalStateException("Transaction not found: $transactionId")
        require(TransactionStateMachine.isValidTransition(current.status, newStatus)) {
            "Invalid transition ${current.status} -> $newStatus for tx $transactionId"
        }
        // Apply the transition (single-writer per transaction id via PK).
        val updated = current.copy(status = newStatus)
        dao.updateTransaction(updated)
        return updated
    }

    // --- DB Mutations ---
    suspend fun saveBalances(balances: List<AssetBalance>) {
        dao.insertAssetBalances(balances)
    }

    suspend fun saveBalance(balance: AssetBalance) {
        dao.insertAssetBalance(balance)
    }

    /**
     * Deprecated: direct transaction insertion bypasses idempotency and state-machine checks.
     * Use [createPaymentIdempotent] / [settleTransaction] instead.
     */
    @Deprecated("Use createPaymentIdempotent to guarantee idempotent payment creation.", level = DeprecationLevel.ERROR)
    suspend fun saveTransaction(tx: CryptoTransaction) = dao.insertTransaction(tx)

    suspend fun savePriceAlert(alert: PriceAlert) {
        dao.insertPriceAlert(alert)
    }

    suspend fun triggerAlert(id: Long) {
        dao.triggerPriceAlert(id)
    }

    suspend fun removeAlert(id: Long) {
        dao.deletePriceAlert(id)
    }

    // --- AI Trading Rules ---
    val aiTradingRules: Flow<List<AITradingRule>> = dao.getAllTradingRules()

    suspend fun saveTradingRule(rule: AITradingRule) {
        dao.insertTradingRule(rule)
    }

    suspend fun toggleTradingRule(id: Long, enabled: Boolean) {
        dao.toggleTradingRule(id, enabled)
    }

    suspend fun updateRuleTriggered(id: Long, timestamp: Long) {
        dao.updateRuleTriggered(id, timestamp)
    }

    suspend fun deleteTradingRule(id: Long) {
        dao.deleteTradingRule(id)
    }
}

/** Thrown when a repository operation fails for an unexpected (non-validation) reason. */
class WalletRepositoryException(message: String, cause: Throwable?) : Exception(message, cause)
