package com.example.data

object GeminiService {
    private const val DISABLED_MESSAGE =
        "AI_SERVICE_DISABLED: a trusted server-side AI gateway is not configured."

    @Suppress("UNUSED_PARAMETER")
    suspend fun generateText(prompt: String, systemInstruction: String? = null): String =
        DISABLED_MESSAGE

    @Suppress("UNUSED_PARAMETER")
    suspend fun analyzeNewsSentiment(newsHeadlinesList: List<String>): String =
        DISABLED_MESSAGE

    @Suppress("UNUSED_PARAMETER")
    suspend fun rebalancePortfolio(
        btcBalance: Double,
        btcValue: Double,
        ethBalance: Double,
        ethValue: Double,
        solBalance: Double,
        solValue: Double,
        bnbBalance: Double,
        bnbValue: Double,
        riskLevel: String
    ): String = DISABLED_MESSAGE
}
