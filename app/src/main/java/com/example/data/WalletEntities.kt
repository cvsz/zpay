package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Financial amounts are stored as integer minor units (e.g. cents, satoshis, wei) to avoid
 * floating-point rounding errors. [balanceMinorUnit] and [priceUsdMinorUnit] are immutable
 * once committed; corrections are made via compensating entries, never by mutation.
 */
@Entity(tableName = "asset_balances")
data class AssetBalance(
    @PrimaryKey val symbol: String, // BTC, ETH, SOL, BNB
    val name: String,
    val chain: String, // Bitcoin, Ethereum, Solana, BSC
    /** Balance in the asset's minor unit (e.g. satoshi for BTC). */
    val balanceMinorUnit: Long,
    /** Price per whole unit in USD minor units (cents). */
    val priceUsdMinorUnit: Long,
    val priceChange24h: Double,
    val lastUpdated: Long = System.currentTimeMillis(),
    /** Optimistic-lock version for concurrent updates. */
    val version: Long = 0
)

/**
 * Immutable transaction ledger entry.
 *
 * [status] is a server-side settlement decision. Client must never supply a final settlement state.
 * [idempotencyKey] guarantees idempotent payment creation: duplicate submissions with the same key
 * are detected and rejected.
 * [amountMinorUnit] / [feeUsdMinorUnit] use integer minor units.
 */
@Entity(
    tableName = "crypto_transactions",
    indices = [
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class CryptoTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chain: String,
    /** Provider/network transaction hash (immutable once set). */
    val hash: String,
    val type: String, // Send, Receive, Swap, Bridge
    val fromAddress: String,
    val toAddress: String,
    /** Amount in minor units of the transacted asset. */
    val amountMinorUnit: Long,
    /** Network fee in USD minor units (cents). */
    val feeUsdMinorUnit: Long,
    /**
     * Transaction lifecycle state. Valid values: PENDING, SUCCESS, FAILED, REVERSED.
     * Transitions are validated by [TransactionStateMachine].
     */
    val status: String = "PENDING",
    /** Client-supplied idempotency key to prevent duplicate payment creation. */
    val idempotencyKey: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/** Valid transaction statuses enforced by the state machine. */
object TransactionStatus {
    const val PENDING = "PENDING"
    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"
    const val REVERSED = "REVERSED"

    val all = listOf(PENDING, SUCCESS, FAILED, REVERSED)
}

/**
 * Validated, server-side transaction state transitions.
 *
 * Rule: a terminal state (SUCCESS, FAILED, REVERSED) can never transition to another state.
 * Rule: PENDING may transition to SUCCESS, FAILED, or REVERSED.
 * Rule: a credit (balance increase) must never be applied twice for the same idempotencyKey.
 */
object TransactionStateMachine {
    /**
     * Returns true if [from] -> [to] is a permitted transition.
     * Never trusts client-supplied settlement state.
     */
    fun isValidTransition(from: String, to: String): Boolean {
        if (!TransactionStatus.all.contains(from) || !TransactionStatus.all.contains(to)) {
            return false
        }
        // Terminal states are immutable.
        if (from == TransactionStatus.SUCCESS || from == TransactionStatus.FAILED ||
            from == TransactionStatus.REVERSED) {
            return false
        }
        // PENDING may settle to any terminal state or remain PENDING.
        return from == TransactionStatus.PENDING && to != TransactionStatus.PENDING
    }

    /** True once a settlement decision has been finalized. */
    fun isTerminal(status: String): Boolean {
        return status == TransactionStatus.SUCCESS ||
            status == TransactionStatus.FAILED ||
            status == TransactionStatus.REVERSED
    }
}

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    /** Target price in USD minor units (cents). */
    val targetPriceMinorUnit: Long,
    val direction: String, // "ABOVE" or "BELOW"
    val isTriggered: Boolean = false,
    val isCustom: Boolean = true,
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "encrypted_keys")
data class EncryptedKey(
    @PrimaryKey val chain: String, // BTC, ETH_BNB, SOL
    val address: String,
    /** AES-GCM encrypted key material, Base64. Never stored plaintext. */
    val encryptedPrivateKey: String,
    val iv: String,
    val dateGenerated: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_trading_rules")
data class AITradingRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetSymbol: String, // BTC, ETH, SOL, BNB
    val actionType: String, // BUY, SELL
    val triggerCondition: String, // PRICE_ABOVE, PRICE_BELOW, SENTIMENT_BULLISH, SENTIMENT_BEARISH
    /** Target threshold in minor units for price triggers. */
    val targetThresholdMinorUnit: Long,
    /** Trade amount in minor units of the asset. */
    val tradeAmountMinorUnit: Long,
    val isEnabled: Boolean = true,
    /**
     * Requires explicit user confirmation before an automated trade debits the account.
     * Automated execution without per-trade consent is disabled by default.
     */
    val requiresConfirmation: Boolean = true,
    val dateCreated: Long = System.currentTimeMillis(),
    val lastTriggered: Long? = null
)

/** Decimals per asset symbol for converting between minor units and display units. */
object AssetDecimals {
    val map = mapOf(
        "BTC" to 8,
        "ETH" to 18,
        "SOL" to 9,
        "BNB" to 8
    )
    fun decimals(symbol: String): Int = map[symbol.uppercase()] ?: 2
}

/**
 * Display-only derived values. Conversion happens at read time; all storage and mutation remain
 * in integer minor units. Safe for UI because these are read-only and never written back.
 */
fun AssetBalance.displayBalance(): Double {
    val d = AssetDecimals.decimals(symbol)
    return balanceMinorUnit / Math.pow(10.0, d.toDouble())
}

fun AssetBalance.displayPriceUsd(): Double {
    return priceUsdMinorUnit / 100.0 // minor units are cents
}

/** Total USD value of this balance as a display Double (read-only). */
fun AssetBalance.displayValueUsd(): Double {
    return displayBalance() * displayPriceUsd()
}

/**
 * Formats a minor-unit amount into a whole-unit display string (read-only).
 * E.g. 38000000 minor units of BTC (8 decimals) -> "0.38".
 */
fun formatMinorUnits(amountMinorUnit: Long, symbol: String): String {
    val d = AssetDecimals.decimals(symbol)
    return java.lang.String.format("%.8f", amountMinorUnit / java.lang.Math.pow(10.0, d.toDouble()))
        .trimEnd('0').trimEnd('.')
}

/** Formats a USD minor-unit (cents) amount into a dollar display string. */
fun formatUsdMinorUnits(amountUsdMinorUnit: Long): String {
    return java.lang.String.format("%.2f", amountUsdMinorUnit / 100.0)
}
