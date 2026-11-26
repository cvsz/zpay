package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * ZWallet Backend Server API Client.
 *
 * Connects to the ZWallet Enterprise Backend Server (web-dashboard/server.js) to fetch
 * portfolio data, market prices, and transaction history. All communication is over HTTPS.
 *
 * This client is used as a secondary data source alongside direct blockchain RPC clients.
 * The server provides aggregated portfolio views and cached market data.
 */
class ServerApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://localhost:3001"
        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false) // Server may not always be available
            .build()
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    init { require(baseUrl.startsWith("https://")) { "Server API base URL must use HTTPS" } }

    /** Portfolio history data point. */
    data class PortfolioHistoryItem(
        val timestamp: String,
        val value: Double,
        val btcValue: Double,
        val ethValue: Double,
        val solValue: Double
    )

    /** Blockchain allocation item. */
    data class AllocationItem(
        val name: String,
        val value: Double,
        val percentage: Double,
        val color: String,
        val nativeAsset: String
    )

    /** Portfolio summary. */
    data class PortfolioSummary(
        val totalValue: Double,
        val btcValue: Double,
        val ethValue: Double,
        val solValue: Double,
        val currency: String
    )

    /** Market price from server. */
    data class ServerPrice(
        val symbol: String,
        val priceUsd: Double,
        val change24hPercent: Double
    )

    /** Transaction from server. */
    data class ServerTransaction(
        val id: String,
        val chain: String,
        val type: String,
        val fromAddress: String,
        val toAddress: String,
        val amountMinorUnit: Long,
        val feeUsdMinorUnit: Long,
        val status: String,
        val timestamp: Long,
        val notes: String
    )

    /** Fetch portfolio history for a given timeframe. */
    suspend fun getPortfolioHistory(timeframe: String = "1W"): List<PortfolioHistoryItem> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/portfolio/history?timeframe=$timeframe"
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Portfolio history request failed with HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty response body")
            val json = JSONObject(body)
            val dataArray = json.getJSONArray("data")
            (0 until dataArray.length()).map { index ->
                val item = dataArray.getJSONObject(index)
                PortfolioHistoryItem(
                    timestamp = item.getString("timestamp"),
                    value = item.getDouble("value"),
                    btcValue = item.getDouble("btcValue"),
                    ethValue = item.getDouble("ethValue"),
                    solValue = item.getDouble("solValue")
                )
            }
        }
    }

    /** Fetch blockchain allocation. */
    suspend fun getPortfolioAllocation(): List<AllocationItem> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/portfolio/allocation"
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Portfolio allocation request failed with HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty response body")
            val json = JSONObject(body)
            val array = json.getJSONArray("allocation")
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                AllocationItem(
                    name = item.getString("name"),
                    value = item.getDouble("value"),
                    percentage = item.getDouble("percentage"),
                    color = item.getString("color"),
                    nativeAsset = item.getString("nativeAsset")
                )
            }
        }
    }

    /** Fetch portfolio summary. */
    suspend fun getPortfolioSummary(): PortfolioSummary = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/portfolio/summary"
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Portfolio summary request failed with HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty response body")
            val json = JSONObject(body)
            PortfolioSummary(
                totalValue = json.getDouble("totalValue"),
                btcValue = json.getDouble("btcValue"),
                ethValue = json.getDouble("ethValue"),
                solValue = json.getDouble("solValue"),
                currency = json.getString("currency")
            )
        }
    }

    /** Fetch market prices from server. */
    suspend fun getMarketPrices(): List<ServerPrice> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/market/prices"
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Market prices request failed with HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty response body")
            val json = JSONObject(body)
            val pricesObj = json.getJSONObject("prices")
            val keys = pricesObj.keys()
            val result = mutableListOf<ServerPrice>()
            while (keys.hasNext()) {
                val symbol = keys.next()
                val entry = pricesObj.getJSONObject(symbol)
                result.add(ServerPrice(
                    symbol = symbol,
                    priceUsd = entry.getDouble("usd"),
                    change24hPercent = entry.getDouble("change24h")
                ))
            }
            result
        }
    }

    /** Fetch transaction history from server. */
    suspend fun getTransactions(): List<ServerTransaction> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/transactions"
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Transactions request failed with HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty response body")
            val json = JSONObject(body)
            val array = json.getJSONArray("transactions")
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                ServerTransaction(
                    id = item.getString("id"),
                    chain = item.getString("chain"),
                    type = item.getString("type"),
                    fromAddress = item.getString("fromAddress"),
                    toAddress = item.getString("toAddress"),
                    amountMinorUnit = item.getLong("amountMinorUnit"),
                    feeUsdMinorUnit = item.getLong("feeUsdMinorUnit"),
                    status = item.getString("status"),
                    timestamp = item.getLong("timestamp"),
                    notes = item.getString("notes")
                )
            }
        }
    }

    /** Health check against the server. */
    suspend fun healthCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/health"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }
}
