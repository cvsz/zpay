package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asset_balances")
data class AssetBalance(
    @PrimaryKey val symbol: String, // BTC, ETH, SOL, BNB
    val name: String,
    val chain: String, // Bitcoin, Ethereum, Solana, BSC
    val balance: Double,
    val priceUsd: Double,
    val priceChange24h: Double,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "crypto_transactions")
data class CryptoTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chain: String,
    val hash: String,
    val type: String, // Send, Receive, Swap, Bridge
    val fromAddress: String,
    val toAddress: String,
    val amount: Double,
    val feeUsd: Double,
    val status: String, // Success, Pending, Failed
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val targetPrice: Double,
    val direction: String, // "ABOVE" or "BELOW"
    val isTriggered: Boolean = false,
    val isCustom: Boolean = true,
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "encrypted_keys")
data class EncryptedKey(
    @PrimaryKey val chain: String, // BTC, ETH_BNB, SOL
    val address: String,
    val encryptedPrivateKey: String, // AES GCM Base64 string
    val iv: String, // Initialization vector for decryption
    val dateGenerated: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_trading_rules")
data class AITradingRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetSymbol: String, // BTC, ETH, SOL, BNB
    val actionType: String, // BUY, SELL
    val triggerCondition: String, // PRICE_ABOVE, PRICE_BELOW, SENTIMENT_BULLISH, SENTIMENT_BEARISH
    val targetThreshold: Double, // target price or sentiment score
    val tradeAmountCrypto: Double, // amount in crypto
    val isEnabled: Boolean = true,
    val dateCreated: Long = System.currentTimeMillis(),
    val lastTriggered: Long? = null
)
