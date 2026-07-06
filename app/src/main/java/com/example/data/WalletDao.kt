package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    // --- Asset Balances ---
    @Query("SELECT * FROM asset_balances ORDER BY balance * priceUsd DESC")
    fun getAssetBalances(): Flow<List<AssetBalance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssetBalance(balance: AssetBalance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssetBalances(balances: List<AssetBalance>)

    // --- Transactions ---
    @Query("SELECT * FROM crypto_transactions ORDER BY timestamp DESC")
    fun getTransactions(): Flow<List<CryptoTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: CryptoTransaction)

    // --- Price Alerts ---
    @Query("SELECT * FROM price_alerts WHERE isTriggered = 0 ORDER BY dateCreated DESC")
    fun getActivePriceAlerts(): Flow<List<PriceAlert>>

    @Query("SELECT * FROM price_alerts ORDER BY dateCreated DESC")
    fun getAllPriceAlerts(): Flow<List<PriceAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceAlert(alert: PriceAlert)

    @Query("UPDATE price_alerts SET isTriggered = 1 WHERE id = :id")
    suspend fun triggerPriceAlert(id: Long)

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun deletePriceAlert(id: Long)

    // --- Encrypted Keys ---
    @Query("SELECT * FROM encrypted_keys")
    suspend fun getAllKeys(): List<EncryptedKey>

    @Query("SELECT * FROM encrypted_keys WHERE chain = :chain LIMIT 1")
    suspend fun getKeyForChain(chain: String): EncryptedKey?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: EncryptedKey)

    @Query("DELETE FROM encrypted_keys")
    suspend fun clearAllKeys()

    // --- AI Trading Rules ---
    @Query("SELECT * FROM ai_trading_rules ORDER BY dateCreated DESC")
    fun getAllTradingRules(): Flow<List<AITradingRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTradingRule(rule: AITradingRule)

    @Query("UPDATE ai_trading_rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun toggleTradingRule(id: Long, enabled: Boolean)

    @Query("UPDATE ai_trading_rules SET lastTriggered = :timestamp WHERE id = :id")
    suspend fun updateRuleTriggered(id: Long, timestamp: Long)

    @Query("DELETE FROM ai_trading_rules WHERE id = :id")
    suspend fun deleteTradingRule(id: Long)
}
