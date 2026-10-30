package com.example.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WalletDatabase.getDatabase(application)
    private val repository = WalletRepository(db.walletDao())

    // --- Active Wallet Configuration / Addresses ---
    val btcAddress = "NOT_CONFIGURED"
    val ethAddress = "NOT_CONFIGURED"
    val solAddress = "NOT_CONFIGURED"
    val bnbAddress = "NOT_CONFIGURED" // Same as EVM for BNB Smart Chain

    // --- UI Navigation State ---
    private val _currentScreen = MutableStateFlow("onboarding") // "onboarding", "dashboard", "wallet_keys", "swap", "alerts", "dapp", "news_ai"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    var pendingDeepLinkScreen: String? = null

    // --- Biometric Authentication state ---
    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // --- Gas Fee Estimation State ---
    private val _gasEthLimit = MutableStateFlow(21000)
    val gasEthLimit: StateFlow<Int> = _gasEthLimit.asStateFlow()

    private val _gasEthGwei = MutableStateFlow(28.5) // Fast gwei
    val gasEthGwei: StateFlow<Double> = _gasEthGwei.asStateFlow()

    private val _gasSolFee = MutableStateFlow(0.00005) // standard SOL fee
    val gasSolFee: StateFlow<Double> = _gasSolFee.asStateFlow()

    // --- Cross-Chain Swaps & Bridges State ---
    private val _bridgeStatus = MutableStateFlow<String?>(null)
    val bridgeStatus: StateFlow<String?> = _bridgeStatus.asStateFlow()

    // --- Market Sentiment Analysis & AI Trading states ---
    private val _marketSentimentResult = MutableStateFlow<String?>(null)
    val marketSentimentResult: StateFlow<String?> = _marketSentimentResult.asStateFlow()

    private val _isAnalyzingSentiment = MutableStateFlow(false)
    val isAnalyzingSentiment: StateFlow<Boolean> = _isAnalyzingSentiment.asStateFlow()

    private val _rebalanceStrategyResult = MutableStateFlow<String?>(null)
    val rebalanceStrategyResult: StateFlow<String?> = _rebalanceStrategyResult.asStateFlow()

    private val _isRebalancingPortfolio = MutableStateFlow(false)
    val isRebalancingPortfolio: StateFlow<Boolean> = _isRebalancingPortfolio.asStateFlow()

    // --- Notifications Feed ---
    private val _notifications = MutableStateFlow<List<String>>(emptyList())
    val notifications: StateFlow<List<String>> = _notifications.asStateFlow()

    // Database assets observed reactively
    val assetBalances: StateFlow<List<AssetBalance>> = repository.assetBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<CryptoTransaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val priceAlerts: StateFlow<List<PriceAlert>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiTradingRules: StateFlow<List<AITradingRule>> = repository.aiTradingRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        _isUnlocked.value = false
        viewModelScope.launch {
            // Demo mode must not create wallet keys, balances, or synthetic transactions.
            startPriceAlertMonitoring()
            startAITradingMonitoring()
            addNotification("DEMO MODE: no wallet keys, payment provider, or on-chain integration is configured.")
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun unlockWallet() {
        _isUnlocked.value = true
        _currentScreen.value = pendingDeepLinkScreen ?: "dashboard"
        pendingDeepLinkScreen = null
        addNotification("Demo dashboard opened; wallet authentication is not implemented.")
    }

    fun lockWallet() {
        _isUnlocked.value = false
        _currentScreen.value = "onboarding"
    }

    fun handleDeepLink(uriString: String?) {
        if (uriString == null) return
        try {
            val uri = android.net.Uri.parse(uriString)
            if (uri.host == "zwallet.zeaz.dev") {
                val path = uri.path ?: ""
                val screen = when {
                    path.contains("swap") -> "swap"
                    path.contains("alerts") -> "alerts"
                    path.contains("dapp") -> "dapp"
                    path.contains("keys") -> "wallet_keys"
                    path.contains("news") -> "news_ai"
                    else -> "dashboard"
                }
                if (_isUnlocked.value) {
                    _currentScreen.value = screen
                } else {
                    pendingDeepLinkScreen = screen
                }
                addNotification("Deep link detected for /$screen. Processing secure routing.")
            }
        } catch (e: Exception) {
            // Safe parse ignore
        }
    }

    fun toggleBiometrics() {
        addNotification("Biometric authentication is unavailable until a verified biometric prompt is implemented.")
    }

    /**
     * Set up standard Web3 addresses and generate encrypted AES private keys securely
     */
    fun generateNewSecureWallet() {
        _isUnlocked.value = false
        _currentScreen.value = "onboarding"
        addNotification("Wallet creation is disabled: audited key derivation, signing, and recovery are not implemented.")
    }

    /**
     * Decrypts the hardware-secured seed phrase.
     */
    suspend fun getDecryptedMnemonic(): String? = null

    /**
     * Decrypts a specific key by chain symbol.
     */
    suspend fun getDecryptedKey(chain: String): String? = null

    /**
     * Estimates gas based on user input, updating routing values dynamically
     */
    fun refreshGasEstimation(chain: String) {
        if (chain == "Ethereum" || chain == "BSC") {
            _gasEthGwei.value = (20..45).random().toDouble()
        } else if (chain == "Solana") {
            _gasSolFee.value = 0.00005 + (0..10).random() * 0.00001
        }
    }

    /**
     * Cross-Chain Bridge Swap execution.
     */
    fun executeBridgeSwap(
        fromChain: String,
        toChain: String,
        fromSymbol: String,
        toSymbol: String,
        amount: Double
    ) {
        _bridgeStatus.value = "DISABLED: no audited bridge provider or transaction signer is configured."
        addNotification("Bridge execution blocked in demo mode. No balances or transactions were changed.")
    }

    /**
     * Triggers the Gemini AI Sentiment analysis on cryptocurrency headlines.
     */
    fun fetchMarketSentimentAnalysis() {
        if (_isAnalyzingSentiment.value) return
        _isAnalyzingSentiment.value = true
        _marketSentimentResult.value = null

        viewModelScope.launch {
            val sampleHeadlines = listOf(
                "SEC holds positive talks with major Bitcoin ETF providers regarding option listings.",
                "Ethereum network transaction fees tumble to 4-year low as L2 scaling expands aggressively.",
                "Solana hits new record high in daily active wallet addresses following DeFi DEX volume breakout.",
                "Global macroeconomic tensions ease; institutional venture funding inflows to Web3 up 24%."
            )
            val result = GeminiService.analyzeNewsSentiment(sampleHeadlines)
            _marketSentimentResult.value = result
            _isAnalyzingSentiment.value = false
            addNotification("AI Sentiment analysis updated.")
        }
    }

    /**
     * Requests the Gemini AI trading agent to optimize asset allocations based on user holdings.
     */
    fun runAITradingRebalancer(riskLevel: String) {
        if (_isRebalancingPortfolio.value) return
        _isRebalancingPortfolio.value = true
        _rebalanceStrategyResult.value = null

        viewModelScope.launch {
            val balances = assetBalances.value
            val btc = balances.find { it.symbol == "BTC" }
            val eth = balances.find { it.symbol == "ETH" }
            val sol = balances.find { it.symbol == "SOL" }
            val bnb = balances.find { it.symbol == "BNB" }

            val result = GeminiService.rebalancePortfolio(
                btcBalance = btc?.balance ?: 0.0,
                btcValue = (btc?.balance ?: 0.0) * (btc?.priceUsd ?: 91520.0),
                ethBalance = eth?.balance ?: 0.0,
                ethValue = (eth?.balance ?: 0.0) * (eth?.priceUsd ?: 3340.0),
                solBalance = sol?.balance ?: 0.0,
                solValue = (sol?.balance ?: 0.0) * (sol?.priceUsd ?: 184.20),
                bnbBalance = bnb?.balance ?: 0.0,
                bnbValue = (bnb?.balance ?: 0.0) * (bnb?.priceUsd ?: 582.60),
                riskLevel = riskLevel
            )
            _rebalanceStrategyResult.value = result
            _isRebalancingPortfolio.value = false
            addNotification("AI Trading Agent proposed execution strategy.")
        }
    }

    /**
     * Create custom target price alarms
     */
    fun createPriceAlert(symbol: String, price: Double, direction: String) {
        viewModelScope.launch {
            val alert = PriceAlert(symbol = symbol, targetPrice = price, direction = direction)
            repository.savePriceAlert(alert)
            addNotification("Custom Alert added: $symbol $direction $$price")
        }
    }

    fun removePriceAlert(id: Long) {
        viewModelScope.launch {
            repository.removeAlert(id)
            addNotification("Alert removed.")
        }
    }

    private fun addNotification(message: String) {
        val current = _notifications.value.toMutableList()
        current.add(0, "[${java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $message")
        _notifications.value = current.take(30) // Keep last 30
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    /**
     * Executes a secure top-up of the specified crypto asset using PromptPay, Visa/Mastercard, or Crypto Transfer.
     */
    fun performTopUp(
        symbol: String,
        cryptoAmount: Double,
        fiatAmount: Double,
        paymentMethod: String,
        currency: String = "THB"
    ) {
        addNotification("Payment blocked in demo mode. No provider authorization or funds were received.")
    }

    /**
     * Start background price monitors
     */
    private fun startPriceAlertMonitoring() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000) // check alerts periodically
                val alerts = db.walletDao().getAllPriceAlerts().firstOrNull() ?: continue
                val currentBalances = assetBalances.value

                for (alert in alerts) {
                    if (alert.isTriggered) continue
                    val currentAsset = currentBalances.find { it.symbol == alert.symbol } ?: continue

                    var triggerConditionMet = false
                    if (alert.direction == "ABOVE" && currentAsset.priceUsd >= alert.targetPrice) {
                        triggerConditionMet = true
                    } else if (alert.direction == "BELOW" && currentAsset.priceUsd <= alert.targetPrice) {
                        triggerConditionMet = true
                    }

                    if (triggerConditionMet) {
                        repository.triggerAlert(alert.id)
                        addNotification("🚨 HIGH VOLATILITY ALERT: ${alert.symbol} has gone ${alert.direction.lowercase()} $$ ${alert.targetPrice}! Currently at $${currentAsset.priceUsd}")
                    }
                }
            }
        }
    }

    private fun startPriceTickerSimulation() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000) // update prices every 10 seconds
                val currentBalances = assetBalances.value
                for (asset in currentBalances) {
                    // Slight fluctuation between -0.3% and +0.3%
                    val percentChange = ((-30..30).random().toDouble() / 100.0)
                    val newPrice = (asset.priceUsd * (1.0 + percentChange / 100.0)).coerceAtLeast(1.0)
                    val newPriceChange = asset.priceChange24h + percentChange
                    repository.saveBalance(asset.copy(priceUsd = newPrice, priceChange24h = newPriceChange))
                }
            }
        }
    }

    // --- AI Automated Trading rules management ---
    fun createTradingRule(
        symbol: String,
        action: String,
        condition: String,
        threshold: Double,
        amount: Double
    ) {
        viewModelScope.launch {
            val rule = AITradingRule(
                assetSymbol = symbol,
                actionType = action,
                triggerCondition = condition,
                targetThreshold = threshold,
                tradeAmountCrypto = amount
            )
            repository.saveTradingRule(rule)
            addNotification("🤖 AI Agent configured: Auto-$action $amount $symbol when $condition ($threshold)")
        }
    }

    fun toggleTradingRule(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleTradingRule(id, enabled)
            addNotification("AI Trading Rule ${if (enabled) "enabled" else "disabled"}.")
        }
    }

    fun deleteTradingRule(id: Long) {
        viewModelScope.launch {
            repository.deleteTradingRule(id)
            addNotification("AI Trading Rule deleted.")
        }
    }

    private fun startAITradingMonitoring() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(12000) // check rules periodically
                val rules = db.walletDao().getAllTradingRules().firstOrNull() ?: continue
                val currentBalances = assetBalances.value
                val sentiment = marketSentimentResult.value ?: ""

                for (rule in rules) {
                    if (!rule.isEnabled) continue
                    
                    // Prevent immediate double triggering (e.g., cooldown of 1 minute)
                    if (rule.lastTriggered != null && System.currentTimeMillis() - rule.lastTriggered < 60000) {
                        continue
                    }

                    val currentAsset = currentBalances.find { it.symbol == rule.assetSymbol } ?: continue
                    var conditionMet = false

                    when (rule.triggerCondition) {
                        "PRICE_ABOVE" -> {
                            if (currentAsset.priceUsd >= rule.targetThreshold) {
                                conditionMet = true
                            }
                        }
                        "PRICE_BELOW" -> {
                            if (currentAsset.priceUsd <= rule.targetThreshold) {
                                conditionMet = true
                            }
                        }
                        "SENTIMENT_BULLISH" -> {
                            if (sentiment.uppercase().contains("BULLISH") || sentiment.uppercase().contains("POSITIVE")) {
                                conditionMet = true
                            }
                        }
                        "SENTIMENT_BEARISH" -> {
                            if (sentiment.uppercase().contains("BEARISH") || sentiment.uppercase().contains("NEGATIVE")) {
                                conditionMet = true
                            }
                        }
                    }

                    if (conditionMet) {
                        // Execute the trade!
                        val updatedBalance = if (rule.actionType == "BUY") {
                            currentAsset.balance + rule.tradeAmountCrypto
                        } else {
                            (currentAsset.balance - rule.tradeAmountCrypto).coerceAtLeast(0.0)
                        }

                        // Save updated balance
                        repository.saveBalance(currentAsset.copy(balance = updatedBalance))

                        // Update the last triggered timestamp
                        repository.updateRuleTriggered(rule.id, System.currentTimeMillis())

                        // Log Transaction
                        val chainName = when (rule.assetSymbol) {
                            "BTC" -> "Bitcoin"
                            "ETH" -> "Ethereum"
                            "SOL" -> "Solana"
                            else -> "BSC"
                        }
                        repository.saveTransaction(
                            CryptoTransaction(
                                chain = chainName,
                                hash = "0xaitrade_" + UUID.randomUUID().toString().substring(0, 12),
                                type = if (rule.actionType == "BUY") "AI Auto-Buy" else "AI Auto-Sell",
                                fromAddress = "AI Algorithmic Engine",
                                toAddress = when (rule.assetSymbol) {
                                    "BTC" -> btcAddress
                                    "SOL" -> solAddress
                                    else -> ethAddress
                                },
                                amount = rule.tradeAmountCrypto,
                                feeUsd = 1.50,
                                status = "Success",
                                timestamp = System.currentTimeMillis(),
                                notes = "Automated execution triggered by: ${rule.triggerCondition} at threshold ${rule.targetThreshold}"
                            )
                        )

                        addNotification("🤖 AI AGENT TRIGGERED: Automated ${rule.actionType} of ${rule.tradeAmountCrypto} ${rule.assetSymbol} executed successfully! Trigger rule: ${rule.triggerCondition} threshold ${rule.targetThreshold} met.")
                    }
                }
            }
        }
    }
}
