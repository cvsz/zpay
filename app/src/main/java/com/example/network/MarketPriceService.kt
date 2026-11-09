package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Live market data from CoinGecko simple-price endpoint.
 * Prices converted to USD minor units (cents) immediately.
 */
class MarketPriceService(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.coingecko.com/api/v3"
        val COIN_IDS = mapOf("BTC" to "bitcoin", "ETH" to "ethereum", "SOL" to "solana", "BNB" to "binancecoin")
        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        fun usdToMinorUnits(usd: Double): Long {
            val scaled = java.math.BigDecimal(java.lang.Double.toString(usd)).multiply(java.math.BigDecimal(100)).setScale(0, java.math.RoundingMode.HALF_UP)
            return scaled.toLong()
        }
    }

    init { require(baseUrl.startsWith("https://")) { "Market data base URL must use HTTPS" } }

    /** Price observation for one asset. */
    data class Price(val symbol: String, val priceUsdMinorUnit: Long, val change24hPercent: Double)

    /** Fetch current USD price for all supported assets. */
    suspend fun fetchPrices(): List<Price> = withContext(Dispatchers.IO) {
        val ids = COIN_IDS.values.joinToString(",")
        val url = "$baseUrl/simple/price?ids=$ids&vs_currencies=usd&include_24hr_change=true"
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty response body from $baseUrl")
            if (!response.isSuccessful) throw IOException("Market data request failed with HTTP ${response.code}")
            val json = JSONObject(body)
            COIN_IDS.mapNotNull { (symbol, coinId) ->
                val entry = json.optJSONObject(coinId) ?: return@mapNotNull null
                val usd = entry.optDouble("usd", 0.0)
                if (usd <= 0.0) return@mapNotNull null
                Price(symbol = symbol, priceUsdMinorUnit = usdToMinorUnits(usd), change24hPercent = entry.optDouble("usd_24h_change", 0.0))
            }
        }
    }
}
