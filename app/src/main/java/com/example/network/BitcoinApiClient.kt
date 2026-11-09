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
 * Bitcoin network client backed by mempool.space REST API.
 * Only HTTPS endpoints; refuses plain-HTTP hosts.
 */
class BitcoinApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://mempool.space/api"
        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        private val TEXT_MEDIA = "text/plain; charset=utf-8".toMediaType()
    }

    init { require(baseUrl.startsWith("https://")) { "Bitcoin API base URL must use HTTPS" } }

    /** Confirmation state of UTXO. */
    data class UtxoStatus(val confirmed: Boolean, val blockHeight: Long)

    /** Unspent transaction output. */
    data class Utxo(
        val txid: String,
        val vout: Int,
        val valueSatoshis: Long,
        val status: UtxoStatus
    )

    /** Confirmed + unconfirmed balance in satoshis. */
    suspend fun getBalance(address: String): Long = withContext(Dispatchers.IO) {
        val json = getJsonObject("/address/$address")
        val chain = json.getJSONObject("chain_stats")
        val mempool = json.getJSONObject("mempool_stats")
        chain.getLong("funded_txo_sum") - chain.getLong("spent_txo_sum") +
            mempool.getLong("funded_txo_sum") - mempool.getLong("spent_txo_sum")
    }

    /** UTXOs sorted by confirmation depth (deepest first). */
    suspend fun getUtxos(address: String): List<Utxo> = withContext(Dispatchers.IO) {
        val items = getJsonArray("/address/$address/utxo")
        (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            val status = item.getJSONObject("status")
            Utxo(
                txid = item.getString("txid"),
                vout = item.getInt("vout"),
                valueSatoshis = item.getLong("value"),
                status = UtxoStatus(confirmed = status.getBoolean("confirmed"), blockHeight = status.optLong("block_height", 0L))
            )
        }.sortedByDescending { it.status.blockHeight }
    }

    /** Recommended fee rate in satoshis per vbyte. */
    suspend fun getFeeRateSatPerVbyte(): Long = withContext(Dispatchers.IO) {
        getJsonObject("/v1/fees/recommended").optLong("halfHourFee", 10L).coerceAtLeast(1L)
    }

    /** Transaction IDs for address. */
    suspend fun getTransactionIds(address: String): List<String> = withContext(Dispatchers.IO) {
        val array = getJsonArray("/address/$address/txs")
        (0 until array.length()).map { array.getJSONObject(it).getString("txid") }
    }

    /** Transaction details with confirmation status. */
    suspend fun getTransaction(txid: String): JSONObject = withContext(Dispatchers.IO) {
        getJsonObject("/tx/$txid")
    }

    /** Broadcast raw transaction and return txid. */
    suspend fun broadcast(rawHex: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(baseUrl + "/tx").post(rawHex.toRequestBody(TEXT_MEDIA)).build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Bitcoin broadcast failed (HTTP ${response.code}): $body")
            body.trim()
        }
    }

    private fun getJsonObject(path: String): JSONObject = JSONObject(getRaw(path))
    private fun getJsonArray(path: String): JSONArray = JSONArray(getRaw(path))
    private fun getRaw(path: String): String {
        val request = Request.Builder().url(baseUrl + path).get().build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Bitcoin API request failed (HTTP ${response.code}): $body")
            return body
        }
    }
}
