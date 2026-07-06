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
}
