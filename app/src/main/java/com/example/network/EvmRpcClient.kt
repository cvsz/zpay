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
import java.math.BigInteger
import java.util.concurrent.TimeUnit

/**
 * JSON-RPC 2.0 client for EVM chains (Ethereum, BNB Smart Chain).
 *
 * All calls dispatched on [Dispatchers.IO]. Public endpoints need no API key;
 * production should use authenticated RPC providers.
 */
class EvmRpcClient(
    private val rpcUrl: String,
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    companion object {
        const val ETHEREUM_RPC = "https://ethereum-rpc.publicnode.com"
        const val BSC_RPC = "https://bsc-rpc.publicnode.com"

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    init { require(rpcUrl.startsWith("https://")) { "EVM RPC URL must use HTTPS" } }

    /** Native balance in wei. */
    suspend fun getBalance(address: String): BigInteger {
        val result = rpc("eth_getBalance", jsonArrayOf(address, "latest"))
        return result.optString("result", "0x0").hexToBigInteger()
    }

    /** Transaction count (nonce). */
    suspend fun getTransactionCount(address: String): Long {
        val result = rpc("eth_getTransactionCount", jsonArrayOf(address, "pending"))
        return result.optString("result", "0x0").hexToBigInteger().toLong()
    }

    /** Current gas price in wei. */
    suspend fun getGasPrice(): BigInteger {
        val result = rpc("eth_gasPrice", jsonArrayOf())
        return result.optString("result", "0x0").hexToBigInteger()
    }

    /** Chain ID. */
    suspend fun getChainId(): Long {
        val result = rpc("eth_chainId", jsonArrayOf())
        return result.optString("result", "0x0").hexToBigInteger().toLong()
    }

    /** Latest block number. */
    suspend fun getBlockNumber(): Long {
        val result = rpc("eth_blockNumber", jsonArrayOf())
        return result.optString("result", "0x0").hexToBigInteger().toLong()
    }

    /** Gas estimate for native transfer. */
    suspend fun estimateGas(from: String, to: String, valueWei: BigInteger, gasPriceWei: BigInteger? = null): Long {
        val call = JSONObject().put("from", from).put("to", to).put("value", valueWei.toString(16))
        gasPriceWei?.let { call.put("gasPrice", it.toString(16)) }
        val result = rpc("eth_estimateGas", jsonArrayOf(call))
        return result.optString("result", "0x5208").hexToBigInteger().toLong()
    }

    /** Broadcast signed raw transaction. Returns tx hash. */
    suspend fun sendRawTransaction(signedHex: String): String {
        val result = rpc("eth_sendRawTransaction", jsonArrayOf(signedHex))
        if (result.has("error")) throw IOException("sendRawTransaction failed: ${result.getJSONObject("error")}")
        return result.getString("result")
    }

    /** Transaction receipt or null if not mined. */
    suspend fun getTransactionReceipt(txHash: String): JSONObject? {
        val result = rpc("eth_getTransactionReceipt", jsonArrayOf(txHash))
        val receipt = result.optJSONObject("result")
        return if (receipt == null || receipt.length() == 0) null else receipt
    }

    /** Number of confirmations (0 while pending). */
    suspend fun getTransactionConfirmations(txHash: String): Long {
        val receipt = getTransactionReceipt(txHash) ?: return 0
        val blockNumber = receipt.optString("blockNumber", "0x0").hexToBigInteger().toLong()
        if (blockNumber == 0L) return 0
        return (getBlockNumber() - blockNumber).coerceAtLeast(0L)
    }

    private suspend fun rpc(method: String, params: JSONArray): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString()
        val request = Request.Builder().url(rpcUrl).post(payload.toRequestBody(JSON_MEDIA)).header("Content-Type", "application/json").build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty response body from $rpcUrl")
            if (!response.isSuccessful) throw IOException("RPC $method failed with HTTP ${response.code}")
            val json = JSONObject(body)
            if (json.has("error")) throw IOException("RPC $method error: ${json.getJSONObject("error")}")
            json
        }
    }
}

/** Parse 0x-prefixed hex quantity into BigInteger. */
fun String.hexToBigInteger(): BigInteger = BigInteger(removePrefix("0x").ifEmpty { "0" }, 16)

/** Build JSON array from values (uses index-based put for Kotlin compatibility). */
internal fun jsonArrayOf(vararg values: Any): JSONArray {
    val array = JSONArray()
    values.forEachIndexed { index, value -> array.put(index, value) }
    return array
}
