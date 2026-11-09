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
 * Solana JSON-RPC client using standard JSON-RPC 2.0 envelope.
 */
class SolanaRpcClient(
    private val rpcUrl: String = DEFAULT_RPC_URL,
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    companion object {
        const val DEFAULT_RPC_URL = "https://api.mainnet-beta.solana.com"
        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    init { require(rpcUrl.startsWith("https://")) { "Solana RPC URL must use HTTPS" } }

    /** Account balance in lamports. */
    suspend fun getBalance(address: String): Long = withContext(Dispatchers.IO) {
        val result = rpc("getBalance", jsonArrayOf(address, JSONObject().put("commitment", "confirmed")))
        result.getJSONObject("value").getLong("lamports")
    }

    /** Latest blockhash (base58). */
    suspend fun getLatestBlockhash(): String = withContext(Dispatchers.IO) {
        val result = rpc("getLatestBlockhash", jsonArrayOf(JSONObject().put("commitment", "confirmed")))
        result.getJSONObject("value").getString("blockhash")
    }

    /** Signature status for submitted transaction. */
    suspend fun getSignatureStatus(signature: String): JSONObject = withContext(Dispatchers.IO) {
        val result = rpc("getSignatureStatuses", jsonArrayOf(jsonArrayOf(signature), JSONObject().put("searchTransactionHistory", true)))
        val values = result.getJSONArray("value")
        if (values.length() == 0) {
            JSONObject().put("confirmationStatus", "notFound")
        } else {
            values.getJSONObject(0)
        }
    }

    /** Submit signed transaction (base64). Returns signature. */
    suspend fun sendTransaction(encodedTransaction: String): String = withContext(Dispatchers.IO) {
        val result = rpc("sendTransaction", jsonArrayOf(encodedTransaction, JSONObject().put("encoding", "base64")))
        result.optString("result").ifEmpty { throw IOException("Solana sendTransaction returned no signature") }
    }

    /** SPL token balance scaled by decimals. */
    suspend fun getTokenBalance(mint: String, owner: String): Double = withContext(Dispatchers.IO) {
        val result = rpc("getTokenAccountsByOwner", jsonArrayOf(owner, JSONObject().put("mint", mint), JSONObject().put("encoding", "jsonParsed")))
        val accounts = result.getJSONArray("value")
        if (accounts.length() == 0) {
            0.0
        } else {
            accounts.getJSONObject(0).getJSONObject("account").getJSONObject("data").getJSONObject("parsed").getJSONObject("info").getJSONObject("tokenAmount").getDouble("uiAmount")
        }
    }

    private suspend fun rpc(method: String, params: JSONArray): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString()
        val request = Request.Builder().url(rpcUrl).post(payload.toRequestBody(JSON_MEDIA)).header("Content-Type", "application/json").build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty response body from $rpcUrl")
            if (!response.isSuccessful) throw IOException("Solana RPC $method failed with HTTP ${response.code}")
            val json = JSONObject(body)
            if (json.has("error")) throw IOException("Solana RPC $method error: ${json.getJSONObject("error")}")
            json.getJSONObject("result")
        }
    }
}
