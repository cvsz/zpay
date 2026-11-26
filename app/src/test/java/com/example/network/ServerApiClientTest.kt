package com.example.network

import com.example.network.ServerApiClient.AllocationItem
import com.example.network.ServerApiClient.PortfolioHistoryItem
import com.example.network.ServerApiClient.PortfolioSummary
import com.example.network.ServerApiClient.ServerPrice
import com.example.network.ServerApiClient.ServerTransaction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ServerApiClientTest {

    companion object {
        private const val TEST_BASE_URL = "https://test-server.example.com"
    }

    @Test
    fun portfolioHistoryItem_creation() {
        val item = PortfolioHistoryItem(
            timestamp = "2026-01-15T10:30:00Z",
            value = 125000.0,
            btcValue = 58000.0,
            ethValue = 47000.0,
            solValue = 20000.0
        )
        assertEquals("2026-01-15T10:30:00Z", item.timestamp)
        assertEquals(125000.0, item.value, 0.001)
        assertEquals(58000.0, item.btcValue, 0.001)
        assertEquals(47000.0, item.ethValue, 0.001)
        assertEquals(20000.0, item.solValue, 0.001)
    }

    @Test
    fun allocationItem_creation() {
        val item = AllocationItem(
            name = "Bitcoin Network",
            value = 61500.0,
            percentage = 46.78,
            color = "#F7931A",
            nativeAsset = "BTC"
        )
        assertEquals("Bitcoin Network", item.name)
        assertEquals(61500.0, item.value, 0.001)
        assertEquals(46.78, item.percentage, 0.01)
        assertEquals("#F7931A", item.color)
        assertEquals("BTC", item.nativeAsset)
    }

    @Test
    fun portfolioSummary_creation() {
        val summary = PortfolioSummary(
            totalValue = 131450.0,
            btcValue = 61500.0,
            ethValue = 48500.0,
            solValue = 21450.0,
            currency = "USD"
        )
        assertEquals(131450.0, summary.totalValue, 0.001)
        assertEquals(61500.0, summary.btcValue, 0.001)
        assertEquals(48500.0, summary.ethValue, 0.001)
        assertEquals(21450.0, summary.solValue, 0.001)
        assertEquals("USD", summary.currency)
    }

    @Test
    fun serverPrice_creation() {
        val price = ServerPrice(symbol = "BTC", priceUsd = 91520.0, change24hPercent = 2.5)
        assertEquals("BTC", price.symbol)
        assertEquals(91520.0, price.priceUsd, 0.001)
        assertEquals(2.5, price.change24hPercent, 0.01)
    }

    @Test
    fun serverTransaction_creation() {
        val tx = ServerTransaction(
            id = UUID.randomUUID().toString(),
            chain = "Ethereum",
            type = "Receive",
            fromAddress = "0x2e08demo21fa",
            toAddress = "0x71C7656EC7ab88b098defB751B7401B5f6d1476B",
            amountMinorUnit = 1500000000000000000L,
            feeUsdMinorUnit = 1245,
            status = "SUCCESS",
            timestamp = System.currentTimeMillis(),
            notes = "Inbound liquidity"
        )
        assertEquals("Ethereum", tx.chain)
        assertEquals("Receive", tx.type)
        assertEquals(1500000000000000000L, tx.amountMinorUnit)
        assertEquals(1245, tx.feeUsdMinorUnit)
        assertEquals("SUCCESS", tx.status)
        assertFalse(tx.notes.isEmpty())
    }

    @Test
    fun defaultBaseUrl_usesHttps() {
        assertTrue(ServerApiClient.DEFAULT_BASE_URL.startsWith("https://"))
    }

    @Test
    fun defaultHttpClient_createsNonNullClient() {
        val client = ServerApiClient.defaultHttpClient()
        assertNotNull(client)
        assertEquals(15, client.connectTimeoutMillis)
        assertEquals(30, client.readTimeoutMillis)
    }

    @Test
    fun init_rejectsNonHttpsUrl() {
        var threw = false
        try {
            ServerApiClient(baseUrl = "http://insecure.example.com")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun runBlocking_healthCheck_returnsFalseForUnreachableServer() = runBlocking {
        val client = ServerApiClient(baseUrl = "https://nonexistent.example.invalid:1")
        val result = client.healthCheck()
        assertFalse(result)
    }
}
