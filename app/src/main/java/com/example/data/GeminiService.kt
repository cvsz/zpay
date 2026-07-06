package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    /**
     * Executes a raw text generation request to the Gemini API.
     */
    suspend fun generateText(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured in Secrets panel.")
            return@withContext "API_KEY_MISSING"
        }

        try {
            // Construct the standard JSON payload using Android's native JSONObject
            val requestJson = JSONObject()
            
            // Contents
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // System Instruction (if provided)
            if (systemInstruction != null) {
                val systemInstructionObj = JSONObject()
                val systemPartsArray = JSONArray()
                val systemPartObj = JSONObject()
                systemPartObj.put("text", systemInstruction)
                systemPartsArray.put(systemPartObj)
                systemInstructionObj.put("parts", systemPartsArray)
                requestJson.put("systemInstruction", systemInstructionObj)
            }

            // Generation Config
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.3) // Lower temperature for more factual financial insights
            requestJson.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini API request failed: Code ${response.code}, Body: $errBody")
                    return@withContext "ERROR_API_FAIL"
                }

                val responseBody = response.body?.string() ?: return@withContext "ERROR_EMPTY_RESPONSE"
                val responseJson = JSONObject(responseBody)
                
                // Parse the response candidates
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No result returned.")
                        }
                    }
                }
                "No analytical content generated."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
            "ERROR_EXCEPTION: ${e.localizedMessage}"
        }
    }

    /**
     * Conducts real-time cryptocurrency news sentiment analysis.
     */
    suspend fun analyzeNewsSentiment(newsHeadlinesList: List<String>): String {
        val headlines = newsHeadlinesList.joinToString("\n") { "- $it" }
        val systemInstruction = "You are an expert Web3 Cryptographic Financial Analyst. Analyze the sentiment of the provided cryptocurrency news headlines and output a structured report in bullet points."
        val prompt = """
            Analyze the market sentiment of the following news headlines. For each, give a direct impact analysis (Bullish, Neutral, or Bearish) and outline the core reasons. 
            Summarize the overall market fear & greed outlook.
            
            Headlines:
            $headlines
            
            Provide a concise, professional analysis under 200 words. Format with clean Material 3 headings.
        """.trimIndent()
        
        return generateText(prompt, systemInstruction)
    }

    /**
     * Rebalances a user portfolio using AI agent algorithmic execution advice.
     */
    suspend fun rebalancePortfolio(
        btcBalance: Double, btcValue: Double,
        ethBalance: Double, ethValue: Double,
        solBalance: Double, solValue: Double,
        bnbBalance: Double, bnbValue: Double,
        riskLevel: String // "Conservative", "Balanced", "Aggressive"
    ): String {
        val systemInstruction = "You are an elite AI algorithmic trading agent specialized in portfolio rebalancing and risk mitigation on-chain."
        val prompt = """
            Provide a professional algorithmic portfolio rebalancing strategy based on these assets and a '$riskLevel' investor profile:
            
            Current Holdings:
            - Bitcoin (BTC): $btcBalance BTC (~$${String.format("%.2f", btcValue)})
            - Ethereum (ETH): $ethBalance ETH (~$${String.format("%.2f", ethValue)})
            - Solana (SOL): $solBalance SOL (~$${String.format("%.2f", solValue)})
            - BNB: $bnbBalance BNB (~$${String.format("%.2f", bnbValue)})
            
            Deliver a direct rebalancing action plan:
            1. Recommend allocation target percentages for BTC, ETH, SOL, BNB under '$riskLevel' logic.
            2. Suggest specific asset swaps/reallocations (e.g., "Sell 5% SOL for BTC to lock gains").
            3. Highlight the technical routing benefit (e.g., lower gas fees, high liquidity path) to optimize cost execution.
            
            Keep the response action-oriented, precise, and under 250 words. Formatted in clear markdown.
        """.trimIndent()

        return generateText(prompt, systemInstruction)
    }
}
