package com.example.data

import kotlinx.coroutines.flow.Flow

class WalletRepository(private val dao: WalletDao) {

    // Reactive State Flows
    val assetBalances: Flow<List<AssetBalance>> = dao.getAssetBalances()
    val transactions: Flow<List<CryptoTransaction>> = dao.getTransactions()
    val activeAlerts: Flow<List<PriceAlert>> = dao.getActivePriceAlerts()
    val allAlerts: Flow<List<PriceAlert>> = dao.getAllPriceAlerts()

    // Wallet key custody is intentionally unavailable in this demo build.
    @Suppress("UNUSED_PARAMETER")
    suspend fun secureStoreMnemonic(mnemonic: String) {
        throw UnsupportedOperationException("Wallet key storage is disabled until audited key derivation and recovery are implemented.")
    }

    suspend fun decryptMnemonic(): String? = null

    @Suppress("UNUSED_PARAMETER")
    suspend fun secureStoreKey(chain: String, address: String, rawPrivateKey: String) {
        throw UnsupportedOperationException("Wallet key storage is disabled until audited key derivation and signing are implemented.")
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun decryptKey(chain: String): String? = null

    suspend fun hasKeys(): Boolean = false

    suspend fun removeAllKeys() {
        dao.clearAllKeys()
    }

    // --- DB Mutations ---
    suspend fun saveBalances(balances: List<AssetBalance>) {
        dao.insertAssetBalances(balances)
    }

    suspend fun saveBalance(balance: AssetBalance) {
        dao.insertAssetBalance(balance)
    }

    suspend fun saveTransaction(tx: CryptoTransaction) {
        dao.insertTransaction(tx)
    }

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
