package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.Flow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class WalletRepository(private val dao: WalletDao) {

    // Reactive State Flows
    val assetBalances: Flow<List<AssetBalance>> = dao.getAssetBalances()
    val transactions: Flow<List<CryptoTransaction>> = dao.getTransactions()
    val activeAlerts: Flow<List<PriceAlert>> = dao.getActivePriceAlerts()
    val allAlerts: Flow<List<PriceAlert>> = dao.getAllPriceAlerts()

    // --- Secure Key Management (Hardware-Backed SecureKeyVault AES-GCM) ---

    suspend fun secureStoreMnemonic(mnemonic: String) {
        try {
            val payload = SecureKeyVault.encrypt(mnemonic)
            val entry = EncryptedKey(
                chain = "MNEMONIC",
                address = "",
                encryptedPrivateKey = payload.encryptedData,
                iv = payload.iv
            )
            dao.insertKey(entry)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun decryptMnemonic(): String? {
        val entry = dao.getKeyForChain("MNEMONIC") ?: return null
        return try {
            SecureKeyVault.decrypt(entry.encryptedPrivateKey, entry.iv)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun secureStoreKey(chain: String, address: String, rawPrivateKey: String) {
        try {
            val payload = SecureKeyVault.encrypt(rawPrivateKey)
            val keyEntry = EncryptedKey(
                chain = chain,
                address = address,
                encryptedPrivateKey = payload.encryptedData,
                iv = payload.iv
            )
            dao.insertKey(keyEntry)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun decryptKey(chain: String): String? {
        val keyEntry = dao.getKeyForChain(chain) ?: return null
        return try {
            SecureKeyVault.decrypt(keyEntry.encryptedPrivateKey, keyEntry.iv)
        } catch (e: Exception) {
            e.printStackTrace()
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
