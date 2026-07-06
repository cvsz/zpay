package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardDarkBorder
import com.example.ui.theme.CosmicCyan

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradingViewWidget(
    symbol: String, // e.g. "BINANCE:BTCUSDT"
    modifier: Modifier = Modifier
) {
    var isLoading by remember(symbol) { mutableStateOf(true) }

    // Custom dark TradingView Widget HTML code
    val htmlPayload = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                body, html {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background-color: #0b0c10; /* Match ZWallet Obsidian Black theme */
                    overflow: hidden;
                }
                #tradingview_container {
                    width: 100%;
                    height: 100%;
                }
            </style>
        </head>
        <body>
            <div id="tradingview_container"></div>
            <script type="text/javascript" src="https://s3.tradingview.com/tv.js"></script>
            <script type="text/javascript">
                new TradingView.widget({
                    "width": "100%",
                    "height": "100%",
                    "symbol": "$symbol",
                    "interval": "D",
                    "timezone": "Etc/UTC",
                    "theme": "dark",
                    "style": "1",
                    "locale": "en",
                    "toolbar_bg": "#0b0c10",
                    "enable_publishing": false,
                    "hide_top_toolbar": false,
                    "hide_legend": false,
                    "save_image": false,
                    "container_id": "tradingview_container",
                    "studies": [
                        "RSI@tv-basicstudies",
                        "MASimple@tv-basicstudies"
                    ]
                });
            </script>
        </body>
        </html>
    """.trimIndent()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundDark)
            .border(1.dp, CardDarkBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.setSupportZoom(false)
                    setBackgroundColor(android.graphics.Color.parseColor("#0b0c10"))
                    
                    loadDataWithBaseURL(
                        "https://s.tradingview.com",
                        htmlPayload,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            update = { webView ->
                // When symbol changes, reload the WebView with the new payload
                webView.loadDataWithBaseURL(
                    "https://s.tradingview.com",
                    htmlPayload,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        )

        if (isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = CosmicCyan,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LACING SECURE TRADINGVIEW FEED...",
                    fontSize = 10.sp,
                    color = CosmicCyan
                )
            }
        }
    }
}
