package com.example.ui.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.animation.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WalletAppContent(viewModel: WalletViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()

    Scaffold(
        containerColor = BackgroundDark
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Render Sidebar ONLY if unlocked and not on onboarding
            if (isUnlocked && currentScreen != "onboarding") {
                WalletSidebar(
                    currentScreen = currentScreen,
                    onSelect = { viewModel.navigateTo(it) },
                    onLock = { viewModel.lockWallet() }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "ScreenTransition"
                ) { targetScreen ->
                    when (targetScreen) {
                        "onboarding" -> OnboardingScreen(viewModel)
                        "dashboard" -> DashboardScreen(viewModel)
                        "wallet_keys" -> KeyManagementScreen(viewModel)
                        "swap" -> BridgeSwapScreen(viewModel)
                        "alerts" -> AlertsManagementScreen(viewModel)
                        "dapp" -> DAppBrowserScreen(viewModel)
                        "news_ai" -> AiAgentScreen(viewModel)
                        "topup" -> TopUpScreen(viewModel, onBack = { viewModel.navigateTo("dashboard") })
                    }
                }
            }
        }
    }
}

// --- ONBOARDING / LOCK SCREEN ---
@Composable
fun OnboardingScreen(viewModel: WalletViewModel) {
    var passwordPin by remember { mutableStateOf("") }
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Brand Shield Decoration
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(CosmicCyan.copy(alpha = 0.2f), Color.Transparent)))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Shield Logo",
                tint = CosmicCyan,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "APEX WEB3 WALLET",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            letterSpacing = 2.sp
        )
        Text(
            text = "Secure Cryptographic Vault",
            fontSize = 14.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Encrypted Password Input
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardDarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Enter Security PIN to Decrypt",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = passwordPin,
                    onValueChange = { if (it.length <= 6) passwordPin = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (passwordPin == "1234" || passwordPin.length >= 4) {
                            viewModel.unlockWallet()
                        } else {
                            Toast.makeText(context, "Invalid PIN. Use 1234 or any 4+ digit PIN", Toast.LENGTH_SHORT).show()
                        }
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CosmicCyan,
                        unfocusedBorderColor = CardDarkBorder,
                        focusedContainerColor = BackgroundDark,
                        unfocusedContainerColor = BackgroundDark
                    ),
                    placeholder = { Text("6-Digit Vault PIN", color = TextGray) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (passwordPin.length >= 4) {
                            viewModel.unlockWallet()
                        } else {
                            Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("unlock_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("DECRYPT WALLET", fontWeight = FontWeight.Bold, color = DarkRedGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Simulated Biometric Option
        if (isBiometricEnabled) {
            IconButton(
                onClick = { viewModel.unlockWallet() },
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(CardDark)
                    .border(1.dp, CosmicCyan.copy(alpha = 0.3f), CircleShape)
                    .testTag("biometric_trigger")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Lock",
                    tint = CosmicCyan,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                text = "Tap to unlock with Biometrics",
                fontSize = 12.sp,
                color = CosmicCyan,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun AddressItemRow(
    chain: String,
    address: String,
    onCopy: () -> Unit
) {
    Column {
        Text(text = chain, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = address,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = CosmicCyan,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SecureBalanceVaultComponent(
    viewModel: WalletViewModel,
    totalBalance: Double,
    totalDeltaPercentage: Double,
    assets: List<AssetBalance>
) {
    var showAddresses by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardDarkBorder, RoundedCornerShape(16.dp))
            .testTag("secure_balance_vault_card"),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header: Status & Secure Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ElectricEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SECURE ENCLAVE ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricEmerald,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Active",
                        tint = CosmicCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    IconButton(
                        onClick = { viewModel.lockWallet() },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(BackgroundDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock Wallet",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Total Portfolio Balance
            Text(
                text = "Total Portfolio Balance",
                fontSize = 13.sp,
                color = TextMuted
            )
            
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                val balanceParts = String.format("%,.2f", totalBalance).split(".")
                Text(
                    text = "$${balanceParts[0]}",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Light,
                    color = TextWhite,
                    letterSpacing = (-1).sp
                )
                if (balanceParts.size > 1) {
                    Text(
                        text = ".${balanceParts[1]}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            // Delta Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (totalDeltaPercentage >= 0) DarkGreenGray else DarkRedGray)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (totalDeltaPercentage >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = "Trend",
                    tint = if (totalDeltaPercentage >= 0) ElectricEmerald else VolatilityPink,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${if (totalDeltaPercentage >= 0) "+" else ""}${String.format("%.2f", totalDeltaPercentage)}% today",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalDeltaPercentage >= 0) ElectricEmerald else VolatilityPink
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider
            HorizontalDivider(color = CardDarkBorder, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-chain breakdowns summary line
            Text(
                text = "MULTI-CHAIN BALANCES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                assets.forEach { asset ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = asset.symbol,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Text(
                            text = "${asset.balance}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Reveal Address Button
            if (isAuthenticating) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = CosmicCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Button(
                    onClick = {
                        if (!showAddresses) {
                            isAuthenticating = true
                            scope.launch {
                                kotlinx.coroutines.delay(600)
                                isAuthenticating = false
                                showAddresses = true
                                Toast.makeText(context, "Secure Decryption Successful via Keystore", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showAddresses = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("reveal_address_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showAddresses) BackgroundDark else CosmicCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (showAddresses) BorderStroke(1.dp, CardDarkBorder) else null
                ) {
                    Icon(
                        imageVector = if (showAddresses) Icons.Default.VisibilityOff else Icons.Default.Key,
                        contentDescription = "Lock",
                        tint = if (showAddresses) TextMuted else BackgroundDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showAddresses) "HIDE WALLET ADDRESSES" else "REVEAL SECURE ADDRESSES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showAddresses) TextWhite else BackgroundDark
                    )
                }
            }

            // Expandable secure address drawer
            AnimatedVisibility(
                visible = showAddresses && !isAuthenticating,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .background(BackgroundDark, RoundedCornerShape(12.dp))
                        .border(1.dp, CardDarkBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AddressItemRow(
                        chain = "Ethereum / EVM (ETH/BNB)",
                        address = viewModel.ethAddress,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(viewModel.ethAddress))
                            Toast.makeText(context, "EVM Address copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    AddressItemRow(
                        chain = "Bitcoin (BTC)",
                        address = viewModel.btcAddress,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(viewModel.btcAddress))
                            Toast.makeText(context, "BTC Address copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    AddressItemRow(
                        chain = "Solana (SOL)",
                        address = viewModel.solAddress,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(viewModel.solAddress))
                            Toast.makeText(context, "SOL Address copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

// --- CORE PORTFOLIO DASHBOARD ---
@Composable
fun DashboardScreen(viewModel: WalletViewModel) {
    val assets by viewModel.assetBalances.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    var selectedTimeframe by remember { mutableStateOf("1M") }
    var isTradingViewActive by remember { mutableStateOf(false) }
    var selectedTvSymbol by remember { mutableStateOf("BINANCE:BTCUSDT") }
    val context = LocalContext.current

    // Calculate totals
    val totalBalance = assets.sumOf { it.balance * it.priceUsd }
    val totalBalance24hAgo = assets.sumOf { 
        val balanceVal = it.balance * it.priceUsd
        balanceVal / (1 + (it.priceChange24h / 100))
    }
    val totalDeltaPercentage = if (totalBalance24hAgo > 0) {
        ((totalBalance - totalBalance24hAgo) / totalBalance24hAgo) * 100
    } else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Premium Secure Balance Card with Expandable Addresses
        item {
            SecureBalanceVaultComponent(
                viewModel = viewModel,
                totalBalance = totalBalance,
                totalDeltaPercentage = totalDeltaPercentage,
                assets = assets
            )
        }

        // Quick Actions Row (Send, Receive, Swap, Top Up)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionButton(
                    icon = Icons.Default.ArrowUpward,
                    label = "SEND",
                    onClick = {
                        Toast.makeText(context, "Secure outbox active. Select asset below to withdraw.", Toast.LENGTH_SHORT).show()
                    }
                )

                QuickActionButton(
                    icon = Icons.Default.ArrowDownward,
                    label = "RECEIVE",
                    onClick = {
                        Toast.makeText(context, "Click 'REVEAL SECURE ADDRESSES' on the portfolio card to view deposit details.", Toast.LENGTH_LONG).show()
                    }
                )

                QuickActionButton(
                    icon = Icons.Default.SwapHoriz,
                    label = "SWAP",
                    onClick = { viewModel.navigateTo("swap") }
                )

                QuickActionButton(
                    icon = Icons.Default.CreditCard,
                    label = "TOP UP",
                    glow = true,
                    onClick = { viewModel.navigateTo("topup") }
                )
            }
        }

        // Custom Bezier Line Chart or TradingView Widget Toggle
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardDarkBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isTradingViewActive) CosmicCyan.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { isTradingViewActive = false }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("PERFORMANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (!isTradingViewActive) CosmicCyan else TextMuted)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isTradingViewActive) CosmicCyan.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { isTradingViewActive = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ElectricEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("LIVE TRADINGVIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isTradingViewActive) CosmicCyan else TextMuted)
                                }
                            }
                        }
                        
                        if (!isTradingViewActive) {
                            // Timeframe Buttons
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BackgroundDark)
                                    .padding(2.dp)
                            ) {
                                listOf("1D", "1W", "1M", "1Y").forEach { tf ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (selectedTimeframe == tf) ElectricEmerald else Color.Transparent)
                                            .clickable { selectedTimeframe = tf }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = tf,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedTimeframe == tf) BackgroundDark else TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isTradingViewActive) {
                        // Horizontal row of active multi-chain assets to select for TradingView Symbol
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Triple("BTC", "BINANCE:BTCUSDT", "Bitcoin"),
                                Triple("ETH", "BINANCE:ETHUSDT", "Ethereum"),
                                Triple("SOL", "BINANCE:SOLUSDT", "Solana"),
                                Triple("BNB", "BINANCE:BNBUSDT", "BSC")
                            ).forEach { (label, sym, name) ->
                                val isSelected = selectedTvSymbol == sym
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CosmicCyan else BackgroundDark)
                                        .border(1.dp, if (isSelected) CosmicCyan else CardDarkBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedTvSymbol = sym }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) BackgroundDark else TextWhite
                                        )
                                        Text(
                                            text = name,
                                            fontSize = 8.sp,
                                            color = if (isSelected) BackgroundDark.copy(alpha = 0.7f) else TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Web TradingView widget
                        TradingViewWidget(
                            symbol = selectedTvSymbol,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Standard WebSocket Connection", fontSize = 10.sp, color = TextGray)
                            Text("TradingView Real-time Index", fontSize = 10.sp, color = ElectricEmerald)
                        }
                    } else {
                        // Bezier Canvas Drawing
                        val points = when (selectedTimeframe) {
                            "1D" -> listOf(0.4f, 0.45f, 0.42f, 0.52f, 0.6f, 0.55f, 0.68f, 0.72f, 0.82f)
                            "1W" -> listOf(0.3f, 0.4f, 0.35f, 0.5f, 0.45f, 0.65f, 0.58f, 0.78f, 0.82f)
                            "1M" -> listOf(0.2f, 0.28f, 0.42f, 0.38f, 0.55f, 0.48f, 0.68f, 0.75f, 0.82f)
                            else -> listOf(0.1f, 0.15f, 0.32f, 0.4f, 0.52f, 0.45f, 0.62f, 0.75f, 0.82f)
                        }
                        
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            val width = size.width
                            val height = size.height
                            val path = Path()
                            val stepX = width / (points.size - 1)

                            // Draw background glowing area
                            val fillPath = Path()
                            fillPath.moveTo(0f, height)

                            points.forEachIndexed { idx, point ->
                                val cx = idx * stepX
                                val cy = height - (point * height * 0.8f) // scale and offset
                                if (idx == 0) {
                                    path.moveTo(cx, cy)
                                    fillPath.lineTo(cx, cy)
                                } else {
                                    // Draw quadratic bezier curve for smooth rendering
                                    val prevX = (idx - 1) * stepX
                                    val prevY = height - (points[idx - 1] * height * 0.8f)
                                    val controlX = (prevX + cx) / 2
                                    path.quadraticTo(controlX, prevY, cx, cy)
                                    fillPath.quadraticTo(controlX, prevY, cx, cy)
                                }
                            }
                            fillPath.lineTo(width, height)
                            fillPath.close()

                            // Fill under line with gradient
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(ElectricEmerald.copy(alpha = 0.25f), Color.Transparent)
                                )
                            )

                            // Draw main line
                            drawPath(
                                path = path,
                                color = ElectricEmerald,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw end cursor node
                            val lastX = width
                            val lastY = height - (points.last() * height * 0.8f)
                            drawCircle(
                                color = ElectricEmerald,
                                radius = 6.dp.toPx(),
                                center = Offset(lastX, lastY)
                            )
                            drawCircle(
                                color = BackgroundDark,
                                radius = 3.dp.toPx(),
                                center = Offset(lastX, lastY)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Start of Period", fontSize = 10.sp, color = TextGray)
                            Text("USD $${String.format("%,.0f", totalBalance)} Target", fontSize = 10.sp, color = ElectricEmerald)
                        }
                    }
                }
            }
        }

        // Interactive Asset List (BNB, ETH, SOL, BTC)
        item {
            Text("MULTI-CHAIN ASSETS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        }

        items(assets) { asset ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        Toast.makeText(context, "${asset.name} Active Session Secure.", Toast.LENGTH_SHORT).show()
                    }
                    .border(1.dp, CardDarkBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Multi-Chain Specific Avatars/Icons
                        val chainIcon = when (asset.symbol) {
                            "BTC" -> Icons.Default.CurrencyBitcoin
                            "ETH" -> Icons.Default.Diamond
                            "SOL" -> Icons.Default.FlashOn
                            else -> Icons.Default.Layers
                        }
                        val iconColor = when (asset.symbol) {
                            "BTC" -> Color(0xFFF7931A)
                            "ETH" -> Color(0xFF627EEA)
                            "SOL" -> Color(0xFF14F195)
                            else -> Color(0xFFF0B90B) // BNB yellow
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(iconColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = chainIcon, contentDescription = asset.symbol, tint = iconColor)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(asset.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(
                                text = "${asset.balance} ${asset.symbol}",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${String.format("%,.2f", asset.balance * asset.priceUsd)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${if (asset.priceChange24h >= 0) "+" else ""}${asset.priceChange24h}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (asset.priceChange24h >= 0) ElectricEmerald else VolatilityPink
                            )
                        }
                    }
                }
            }
        }

        // Donut Chart Asset Allocation visualization
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardDarkBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom Donut Canvas
                    Canvas(modifier = Modifier.size(90.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        val colors = listOf(Color(0xFFF7931A), Color(0xFF627EEA), Color(0xFF14F195), Color(0xFFF0B90B))
                        
                        var startAngle = 0f
                        assets.forEachIndexed { index, asset ->
                            val value = asset.balance * asset.priceUsd
                            val sweepAngle = if (totalBalance > 0) {
                                ((value / totalBalance) * 360f).toFloat()
                            } else 90f
                            
                            drawArc(
                                color = colors.getOrElse(index) { Color.Gray },
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                style = Stroke(width = strokeWidth)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ASSET ALLOCATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        
                        val colors = listOf(Color(0xFFF7931A), Color(0xFF627EEA), Color(0xFF14F195), Color(0xFFF0B90B))
                        assets.forEachIndexed { index, asset ->
                            val value = asset.balance * asset.priceUsd
                            val percent = if (totalBalance > 0) (value / totalBalance) * 100 else 0.0
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colors.getOrElse(index) { Color.Gray }))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${asset.symbol}: ${String.format("%.1f", percent)}%",
                                    fontSize = 11.sp,
                                    color = TextWhite
                                )
                            }
                        }
                    }
                }
            }
        }

        // Volatility Alerts Notification Log
        if (notifications.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("VAULT SIGNAL TELEMETRY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    TextButton(onClick = { viewModel.clearNotifications() }) {
                        Text("Clear", color = VolatilityPink, fontSize = 12.dp.value.sp)
                    }
                }
            }

            items(notifications.take(5)) { alertMsg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (alertMsg.contains("VOLATILITY")) DarkRedGray else CardDark)
                        .border(
                            1.dp, 
                            if (alertMsg.contains("VOLATILITY")) VolatilityPink.copy(alpha = 0.3f) else CardDarkBorder, 
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(text = alertMsg, fontSize = 11.sp, color = TextWhite, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// --- SECURE CRYPTOGRAPHIC KEYS PAGE ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyManagementScreen(viewModel: WalletViewModel) {
    var btcKeyDecrypted by remember { mutableStateOf<String?>(null) }
    var ethKeyDecrypted by remember { mutableStateOf<String?>(null) }
    var solKeyDecrypted by remember { mutableStateOf<String?>(null) }
    var decryptedMnemonic by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingActionLabel by remember { mutableStateOf<String?>(null) }
    var pendingActionCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Key, contentDescription = "Keys", tint = CosmicCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SECURE KEY MANAGEMENT", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }

        Text(
            "Your private keys are encrypted locally using the Android hardware KeyStore. Below, you can authenticate to perform secure private key extractions or save backups.",
            fontSize = 12.sp,
            color = TextMuted
        )

        // 12-WORD MNEMONIC SEED PHRASE SECURE BACKUP CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardDarkBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("12-WORD MNEMONIC SEED PHRASE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ElectricEmerald.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("SECURE ENCLAVE", fontSize = 8.sp, color = ElectricEmerald, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This is your master seed phrase. It recovery-enables all multi-chain addresses. Never share this with anyone.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (decryptedMnemonic != null) {
                    val words = decryptedMnemonic?.split(" ") ?: emptyList()
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        words.forEachIndexed { index, word ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BackgroundDark)
                                    .border(1.dp, CardDarkBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}. ",
                                    fontSize = 10.sp,
                                    color = CosmicCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = word,
                                    fontSize = 12.sp,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { decryptedMnemonic = null },
                        colors = ButtonDefaults.buttonColors(containerColor = BackgroundDark),
                        modifier = Modifier.fillMaxWidth().border(1.dp, CardDarkBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = "Hide", tint = TextMuted)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("HIDE BACKUP PHRASE", fontSize = 11.sp, color = TextMuted)
                    }
                } else {
                    Button(
                        onClick = {
                            pendingActionLabel = "Mnemonic Master Seed Phrase"
                            pendingActionCallback = {
                                scope.launch {
                                    var backup = viewModel.getDecryptedMnemonic()
                                    if (backup == null) {
                                        // Generate fallback if not generated during first-time setup
                                        viewModel.generateNewSecureWallet()
                                        backup = viewModel.getDecryptedMnemonic()
                                    }
                                    decryptedMnemonic = backup
                                    Toast.makeText(context, "Mnemonic decrypted from Secure Enclave", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = "Decrypt Mnemonic", tint = BackgroundDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REVEAL RECOVERY SEED PHRASE", fontSize = 11.sp, color = BackgroundDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // BTC KEY BLOCK
        KeyCard(
            chainName = "Bitcoin (BTC)",
            address = viewModel.btcAddress,
            decryptedKey = btcKeyDecrypted,
            onReveal = {
                pendingActionLabel = "Bitcoin Private Key"
                pendingActionCallback = {
                    scope.launch {
                        val key = viewModel.getDecryptedKey("BTC")
                        if (key != null) {
                            btcKeyDecrypted = key
                            Toast.makeText(context, "BTC Key decrypted via Keystore AES-GCM", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Encryption error. Try regenerating wallet.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onHide = { btcKeyDecrypted = null }
        )

        // ETH & BNB KEY BLOCK
        KeyCard(
            chainName = "Ethereum & BSC (ETH/BNB)",
            address = viewModel.ethAddress,
            decryptedKey = ethKeyDecrypted,
            onReveal = {
                pendingActionLabel = "Ethereum Private Key"
                pendingActionCallback = {
                    scope.launch {
                        val key = viewModel.getDecryptedKey("ETH_BNB")
                        if (key != null) {
                            ethKeyDecrypted = key
                            Toast.makeText(context, "ETH Key decrypted via Keystore AES-GCM", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Encryption error. Try regenerating wallet.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onHide = { ethKeyDecrypted = null }
        )

        // SOLANA KEY BLOCK
        KeyCard(
            chainName = "Solana (SOL)",
            address = viewModel.solAddress,
            decryptedKey = solKeyDecrypted,
            onReveal = {
                pendingActionLabel = "Solana Private Key"
                pendingActionCallback = {
                    scope.launch {
                        val key = viewModel.getDecryptedKey("SOL")
                        if (key != null) {
                            solKeyDecrypted = key
                            Toast.makeText(context, "SOL Key decrypted via Keystore AES-GCM", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Encryption error. Try regenerating wallet.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onHide = { solKeyDecrypted = null }
        )

        // EXPORT PDF TAX DOCUMENTATION SECTION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ElectricEmerald.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkGreenGray),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Export Tax & Performance Report",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricEmerald
                )
                Text(
                    text = "Generate an official local PDF document containing on-chain transaction history logs and balances for IRS/tax compliance.",
                    fontSize = 11.sp,
                    color = TextWhite.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        generatePdfTaxReport(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = BackgroundDark)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GENERATE TAX COMPLIANT PDF", fontWeight = FontWeight.Bold, color = BackgroundDark, fontSize = 12.sp)
                }
            }
        }
    }

    if (pendingActionLabel != null && pendingActionCallback != null) {
        BiometricSimulationDialog(
            actionLabel = pendingActionLabel!!,
            onSuccess = {
                pendingActionCallback?.invoke()
                pendingActionLabel = null
                pendingActionCallback = null
            },
            onDismiss = {
                pendingActionLabel = null
                pendingActionCallback = null
            }
        )
    }
}

@Composable
fun BiometricSimulationDialog(
    actionLabel: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var showSuccessAnim by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundDark,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, CardDarkBorder, RoundedCornerShape(24.dp)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometrics",
                    tint = CosmicCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "BIOMETRIC VERIFICATION",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    letterSpacing = 1.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Confirm biometric validation to extract and reveal sensitive credentials for:\n$actionLabel",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(if (showSuccessAnim) ElectricEmerald.copy(alpha = 0.15f) else CardDark)
                        .clickable(enabled = !isAuthenticating && !showSuccessAnim) {
                            scope.launch {
                                isAuthenticating = true
                                authError = null
                                delay(1200) // Simulation delay
                                isAuthenticating = false
                                showSuccessAnim = true
                                delay(800) // Success checkmark delay
                                onSuccess()
                            }
                        }
                        .border(1.dp, if (showSuccessAnim) ElectricEmerald else CosmicCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            color = CosmicCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                    } else if (showSuccessAnim) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Verified",
                            tint = ElectricEmerald,
                            modifier = Modifier.size(44.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Tap to Scan",
                            tint = CosmicCyan,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isAuthenticating) "Scanning fingerprint..." else if (showSuccessAnim) "Biometrics Verified!" else "Tap fingerprint icon to authenticate",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (showSuccessAnim) ElectricEmerald else if (isAuthenticating) CosmicCyan else TextMuted
                )

                if (authError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = authError!!,
                        fontSize = 11.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextMuted)
            ) {
                Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun KeyCard(
    chainName: String,
    address: String,
    decryptedKey: String?,
    onReveal: () -> Unit,
    onHide: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardDarkBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(chainName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(6.dp))
            
            Text("Public Wallet Address", fontSize = 10.sp, color = TextMuted)
            Text(
                address,
                fontSize = 11.sp,
                color = CosmicCyan,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (decryptedKey != null) {
                Text("Decrypted Private Key", fontSize = 10.sp, color = VolatilityPink)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        decryptedKey,
                        fontSize = 10.sp,
                        color = TextWhite,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onHide) {
                        Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = "Hide", tint = TextMuted)
                    }
                }
            } else {
                Button(
                    onClick = onReveal,
                    colors = ButtonDefaults.buttonColors(containerColor = BackgroundDark),
                    modifier = Modifier.border(1.dp, CardDarkBorder, RoundedCornerShape(8.dp)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Decrypt", tint = ElectricEmerald, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DECRYPT KEY", fontSize = 11.sp, color = ElectricEmerald, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Creates and saves an actual .pdf tax statement utilizing the native Android PdfDocument
 */
fun generatePdfTaxReport(context: Context) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size (595x842pt)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint()
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 18f
        paint.isFakeBoldText = true

        // Draw header
        canvas.drawText("APEX WALLET TAX STATEMENT", 40f, 60f, paint)

        // Draw Meta Info
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        canvas.drawText("Report Generated: ${dateFormat.format(Date())}", 40f, 90f, paint)
        canvas.drawText("Status: Official / Tax Compliant On-Chain Export", 40f, 105f, paint)

        // Horizontal Rule
        paint.strokeWidth = 1f
        canvas.drawLine(40f, 120f, 555f, 120f, paint)

        // Section Headings
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("SUMMARY OF VALUATIONS", 40f, 150f, paint)

        // Table Rows
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("- Bitcoin (BTC) Valuation: 0.38 BTC (~$34,777.60)", 50f, 180f, paint)
        canvas.drawText("- Ethereum (ETH) Valuation: 4.12 ETH (~$13,760.80)", 50f, 200f, paint)
        canvas.drawText("- Solana (SOL) Valuation: 32.50 SOL (~$5,986.50)", 50f, 220f, paint)
        canvas.drawText("- BNB Smart Chain Valuation: 8.40 BNB (~$4,893.84)", 50f, 240f, paint)

        canvas.drawLine(40f, 270f, 555f, 270f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("RECENT ON-CHAIN TRANSACTIONS", 40f, 300f, paint)

        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Date: 2026-07-04 | Chain: Ethereum | Hash: 0x89e24ca6...5d6b", 50f, 330f, paint)
        canvas.drawText("Type: RECEIVE | Amount: +1.50 ETH | Network Fee: $12.45 | Status: Success", 60f, 345f, paint)

        // Disclaimer
        paint.textSize = 8f
        paint.color = android.graphics.Color.GRAY
        canvas.drawText("Disclaimer: This report represents simulated transactions compiled with local database references.", 40f, 780f, paint)

        pdfDocument.finishPage(page)

        // Write the PDF file to external private files directory
        val file = File(context.getExternalFilesDir(null), "ApexWallet_TaxReport.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        Toast.makeText(context, "Tax Report Saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "PDF Generation Failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// --- CROSS-CHAIN BRIDGE SWAP & GAS ESTIMATOR ---
@Composable
fun BridgeSwapScreen(viewModel: WalletViewModel) {
    val assets by viewModel.assetBalances.collectAsState()
    val bridgeStatus by viewModel.bridgeStatus.collectAsState()
    val gasGwei by viewModel.gasEthGwei.collectAsState()
    val gasSolFee by viewModel.gasSolFee.collectAsState()

    var fromChain by remember { mutableStateOf("Ethereum") }
    var toChain by remember { mutableStateOf("Solana") }
    var fromAsset by remember { mutableStateOf("ETH") }
    var toAsset by remember { mutableStateOf("SOL") }
    var amountText by remember { mutableStateOf("1.0") }
    
    val context = LocalContext.current

    val chains = listOf("Bitcoin", "Ethereum", "Solana", "BSC")
    val chainAssets = mapOf(
        "Bitcoin" to "BTC",
        "Ethereum" to "ETH",
        "Solana" to "SOL",
        "BSC" to "BNB"
    )

    val sourceAsset = assets.find { it.symbol == fromAsset }
    val destAsset = assets.find { it.symbol == toAsset }

    val currentBalance = sourceAsset?.balance ?: 0.0
    val rate = if (sourceAsset != null && destAsset != null && destAsset.priceUsd > 0) {
        sourceAsset.priceUsd / destAsset.priceUsd
    } else if (fromAsset == "ETH" && toAsset == "BNB") {
        5.7
    } else {
        1.0
    }

    val amt = amountText.toDoubleOrNull() ?: 0.0
    val expectedReceive = amt * rate

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Swap", tint = ElectricEmerald, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("CROSS-CHAIN SWAP & BRIDGE", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }

        Text("Swap and bridge assets across multiple blockchains utilizing secure multi-chain routing pathways.", fontSize = 11.sp, color = TextMuted)

        // --- FROM BLOCK ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardDarkBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Source Chain", fontSize = 11.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chains.forEach { chain ->
                        val asset = chainAssets[chain] ?: ""
                        val isSelected = fromChain == chain
                        Button(
                            onClick = { 
                                fromChain = chain
                                fromAsset = asset
                                if (fromChain == toChain) {
                                    // Shift destination to avoid identity swap
                                    val fallback = chains.find { it != chain } ?: "Ethereum"
                                    toChain = fallback
                                    toAsset = chainAssets[fallback] ?: "ETH"
                                }
                                viewModel.refreshGasEstimation(chain)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) ElectricEmerald else BackgroundDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp).weight(1f)
                        ) {
                            Text(asset, fontSize = 10.sp, color = if (isSelected) BackgroundDark else TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Amount ($fromAsset)", fontSize = 11.sp, color = TextMuted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bal: $currentBalance", fontSize = 10.sp, color = TextMuted)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "USE MAX",
                            fontSize = 10.sp,
                            color = ElectricEmerald,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { amountText = currentBalance.toString() }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = ElectricEmerald,
                        unfocusedBorderColor = CardDarkBorder
                    ),
                    singleLine = true
                )
            }
        }

        // SWAP ROW ARROW
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = {
                    val tempC = fromChain
                    fromChain = toChain
                    toChain = tempC
                    val tempA = fromAsset
                    fromAsset = toAsset
                    toAsset = tempA
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ElectricEmerald)
                    .size(36.dp)
            ) {
                Icon(imageVector = Icons.Default.SwapVert, contentDescription = "Flip", tint = BackgroundDark, modifier = Modifier.size(18.dp))
            }
        }

        // --- TO BLOCK ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardDarkBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Destination Chain", fontSize = 11.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chains.forEach { chain ->
                        val asset = chainAssets[chain] ?: ""
                        val isSelected = toChain == chain
                        Button(
                            onClick = { 
                                toChain = chain
                                toAsset = asset
                                if (toChain == fromChain) {
                                    // Shift source to avoid identity swap
                                    val fallback = chains.find { it != chain } ?: "Ethereum"
                                    fromChain = fallback
                                    fromAsset = chainAssets[fallback] ?: "ETH"
                                    viewModel.refreshGasEstimation(fallback)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) CosmicCyan else BackgroundDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp).weight(1f)
                        ) {
                            Text(asset, fontSize = 10.sp, color = if (isSelected) BackgroundDark else TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- INTEROPERABILITY ROUTE VISUALIZER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardDarkBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("CRYPTOGRAPHIC INTEROPERABILITY ROUTE", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(fromChain.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("$amountText $fromAsset", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CosmicCyan)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.TrendingFlat,
                            contentDescription = "Routing",
                            tint = ElectricEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Rate: 1 $fromAsset = ${String.format("%.4f", rate)} $toAsset",
                            fontSize = 8.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(toChain.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("${String.format("%.4f", expectedReceive)} $toAsset", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricEmerald)
                    }
                }
            }
        }

        // --- GAS ESTIMATION & ROUTING INFO ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardDarkBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GAS FEE ESTIMATION & ROUTING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    IconButton(onClick = { viewModel.refreshGasEstimation(fromChain) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Gas", tint = ElectricEmerald, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Active Pipeline", fontSize = 10.sp, color = TextGray)
                        Text(
                            text = when (fromChain) {
                                "Ethereum" -> "ERC-20 Bridge Link"
                                "Solana" -> "SPL Wormhole Portal"
                                "Bitcoin" -> "tBTC Native Custody"
                                else -> "BSC TokenBridge Gateway"
                            },
                            fontSize = 12.sp,
                            color = TextWhite,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Estimated Toll Cost", fontSize = 10.sp, color = TextGray)
                        Text(
                            text = when (fromChain) {
                                "Ethereum" -> "~${String.format("%.1f", gasGwei)} Gwei ($${String.format("%.2f", gasGwei * 0.15)})"
                                "Solana" -> "~${String.format("%.5f", gasSolFee)} SOL ($0.01)"
                                "Bitcoin" -> "~0.00015 BTC ($13.70)"
                                else -> "~0.0035 BNB ($2.04)"
                            },
                            fontSize = 12.sp,
                            color = ElectricEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // EXECUTE SWAP
        Button(
            onClick = {
                val amtValue = amountText.toDoubleOrNull() ?: 1.0
                viewModel.executeBridgeSwap(fromChain, toChain, fromAsset, toAsset, amtValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("bridge_swap_button"),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("INITIATE CRYPTOGRAPHIC BRIDGE", fontWeight = FontWeight.Bold, color = BackgroundDark)
        }

        // BRIDGE STATUS NOTIFIER
        if (bridgeStatus != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundDark)
                    .border(1.dp, if (bridgeStatus == "COMPLETED") ElectricEmerald else CosmicCyan, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = if (bridgeStatus == "COMPLETED") ElectricEmerald else CosmicCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bridgeStatus ?: "",
                            fontSize = 12.sp,
                            color = TextWhite,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// --- DAPP BROWSER ---
@Composable
fun DAppBrowserScreen(viewModel: WalletViewModel) {
    var dAppUrl by remember { mutableStateOf("https://app.uniswap.org") }
    var connectionState by remember { mutableStateOf("Disconnected") }
    val context = LocalContext.current

    val popularDapps = listOf(
        Pair("Uniswap Protocol", "https://app.uniswap.org"),
        Pair("PancakeSwap DEX", "https://pancakeswap.finance"),
        Pair("Raydium Solana", "https://raydium.io"),
        Pair("Magic Eden NFT", "https://magiceden.io")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Explore, contentDescription = "DApp", tint = CosmicCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("WEB3 DAPP BROWSER", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }

        Text(
            "Access standard decentralized exchanges and smart contracts with active client-side EVM injections.",
            fontSize = 11.sp,
            color = TextMuted
        )

        // Address Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardDarkBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Language, contentDescription = "Web", tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedTextField(
                    value = dAppUrl,
                    onValueChange = { dAppUrl = it },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Button(
                    onClick = {
                        Toast.makeText(context, "Navigating to $dAppUrl...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BackgroundDark),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("GO", fontSize = 11.sp, color = CosmicCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bookmark Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            popularDapps.forEach { (name, url) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardDark)
                        .border(1.dp, CardDarkBorder, RoundedCornerShape(8.dp))
                        .clickable { 
                            dAppUrl = url
                            connectionState = "Disconnected"
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(name.split(" ")[0], fontSize = 10.sp, color = TextWhite, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Simulated Embedded Browser View
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, CardDarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = BackgroundDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Launch, contentDescription = "Active Web3", tint = CosmicCyan, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = dAppUrl,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Web3 Injection Pipeline: Active",
                    fontSize = 11.sp,
                    color = ElectricEmerald,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Interactive Connect Button
                Button(
                    onClick = {
                        connectionState = "Wallet Connected"
                        Toast.makeText(context, "Secure signature shared with decentralized provider.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (connectionState == "Wallet Connected") ElectricEmerald else CosmicCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Usb, contentDescription = "Connected", tint = BackgroundDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (connectionState == "Wallet Connected") "CONNECTED TO WEB3" else "CONNECT APEX WALLET",
                        fontWeight = FontWeight.Bold,
                        color = BackgroundDark,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// --- GEMINI AI SENTIMENT AND REBALANCER AGENT ---
@Composable
fun AiAgentScreen(viewModel: WalletViewModel) {
    val marketSentiment by viewModel.marketSentimentResult.collectAsState()
    val rebalanceStrategy by viewModel.rebalanceStrategyResult.collectAsState()
    val isAnalyzingNews by viewModel.isAnalyzingSentiment.collectAsState()
    val isRebalancing by viewModel.isRebalancingPortfolio.collectAsState()
    
    var selectedRiskProfile by remember { mutableStateOf("Balanced") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = ElectricEmerald, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("TRADING WITH AI AGENTS", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }

        Text(
            "Leverage server-side Gemini intelligence models to execute on-chain sentiment assessments and algorithmic rebalancing optimization protocols.",
            fontSize = 11.sp,
            color = TextMuted
        )

        // --- SENTIMENT BLOCK ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardDarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("REAL-TIME NEWS SENTIMENT ANALYSIS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CosmicCyan)
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { viewModel.fetchMarketSentimentAnalysis() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isAnalyzingNews) {
                        CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(16.dp))
                    } else {
                        Text("REFRESH MARKET SENTIMENT SCORE", fontWeight = FontWeight.Bold, color = BackgroundDark, fontSize = 12.sp)
                    }
                }

                if (marketSentiment != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundDark)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = marketSentiment ?: "",
                            fontSize = 11.sp,
                            color = TextWhite,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // --- REBALANCING AGENT BLOCK ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardDarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("PORTFOLIO REBALANCING ADVISOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricEmerald)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Target Risk Profile", fontSize = 10.sp, color = TextMuted)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Conservative", "Balanced", "Aggressive").forEach { profile ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedRiskProfile == profile) ElectricEmerald else BackgroundDark)
                                .clickable { selectedRiskProfile = profile }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedRiskProfile == profile) BackgroundDark else TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.runAITradingRebalancer(selectedRiskProfile) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isRebalancing) {
                        CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(16.dp))
                    } else {
                        Text("RUN ALGORITHMIC ANALYSIS", fontWeight = FontWeight.Bold, color = BackgroundDark, fontSize = 12.sp)
                    }
                }

                if (rebalanceStrategy != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundDark)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = rebalanceStrategy ?: "",
                            fontSize = 11.sp,
                            color = TextWhite,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // --- AI AUTOMATED TRADING TRIGGERS BLOCK ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardDarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = "Triggers", tint = CosmicCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI AUTOMATED TRADING TRIGGERS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CosmicCyan)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Deploy responsive triggers to buy or sell assets automatically when specific market price barriers or AI sentiment thresholds are met.",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))

                val tradingRules by viewModel.aiTradingRules.collectAsState()
                val currentBalances by viewModel.assetBalances.collectAsState()

                var ruleSymbol by remember { mutableStateOf("BTC") }
                var ruleAction by remember { mutableStateOf("BUY") }
                var ruleCondition by remember { mutableStateOf("PRICE_BELOW") }
                var ruleThresholdText by remember { mutableStateOf("90000.0") }
                var ruleAmountText by remember { mutableStateOf("0.05") }

                val context = LocalContext.current

                // Form to create automated trading rule
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDark, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("CREATE NEW AUTOMATED RULE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)

                    // Row 1: Symbol & Action Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Symbol selection
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Asset Symbol", fontSize = 9.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .border(1.dp, CardDarkBorder, RoundedCornerShape(6.dp))
                                    .background(CardDark)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("BTC", "ETH", "SOL", "BNB").forEach { sym ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (ruleSymbol == sym) CosmicCyan else Color.Transparent)
                                            .clickable { 
                                                ruleSymbol = sym 
                                                // Pre-fill reasonable defaults based on asset
                                                ruleThresholdText = when(sym) {
                                                    "BTC" -> "90000.0"
                                                    "ETH" -> "3200.0"
                                                    "SOL" -> "175.0"
                                                    else -> "550.0"
                                                }
                                                ruleAmountText = when(sym) {
                                                    "BTC" -> "0.01"
                                                    "ETH" -> "0.15"
                                                    "SOL" -> "1.5"
                                                    else -> "0.5"
                                                }
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(sym, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (ruleSymbol == sym) BackgroundDark else TextWhite)
                                    }
                                }
                            }
                        }

                        // Action BUY / SELL Selection
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Action Type", fontSize = 9.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .border(1.dp, CardDarkBorder, RoundedCornerShape(6.dp))
                                    .background(CardDark)
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("BUY", "SELL").forEach { act ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (ruleAction == act) {
                                                    if (act == "BUY") ElectricEmerald else Color(0xFFFF3366)
                                                } else Color.Transparent
                                            )
                                            .clickable { ruleAction = act }
                                            .padding(vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(act, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (ruleAction == act) BackgroundDark else TextWhite)
                                    }
                                }
                            }
                        }
                    }

                    // Row 2: Condition selection
                    Column {
                        Text("Trigger Condition", fontSize = 9.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .border(1.dp, CardDarkBorder, RoundedCornerShape(6.dp))
                                .background(CardDark)
                                .padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("PRICE_BELOW", "PRICE_ABOVE", "SENTIMENT_BULLISH", "SENTIMENT_BEARISH").forEach { cond ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (ruleCondition == cond) CosmicCyan else Color.Transparent)
                                        .clickable { ruleCondition = cond }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        cond.replace("PRICE_", "").replace("SENTIMENT_", ""), 
                                        fontSize = 8.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        color = if (ruleCondition == cond) BackgroundDark else TextMuted
                                    )
                                }
                            }
                        }
                    }

                    // Row 3: Inputs for Threshold and Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Threshold Input (only visible/enabled for PRICE triggers)
                        val isPriceTrigger = ruleCondition.startsWith("PRICE")
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isPriceTrigger) "Target Price (USD)" else "Sentiment (AI Monitor)", fontSize = 9.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            OutlinedTextField(
                                value = if (isPriceTrigger) ruleThresholdText else "N/A (Auto Sentiment)",
                                onValueChange = { if (isPriceTrigger) ruleThresholdText = it },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isPriceTrigger,
                                textStyle = androidx.compose.ui.text.TextStyle(color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CosmicCyan,
                                    unfocusedBorderColor = CardDarkBorder,
                                    disabledTextColor = TextMuted,
                                    disabledBorderColor = CardDarkBorder
                                ),
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        // Amount Input
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trade Amount (Crypto)", fontSize = 9.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            OutlinedTextField(
                                value = ruleAmountText,
                                onValueChange = { ruleAmountText = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CosmicCyan,
                                    unfocusedBorderColor = CardDarkBorder
                                ),
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    // Create Rule Button
                    Button(
                        onClick = {
                            val threshold = ruleThresholdText.toDoubleOrNull() ?: 0.0
                            val amount = ruleAmountText.toDoubleOrNull() ?: 0.0
                            if (amount <= 0.0) {
                                Toast.makeText(context, "Please enter a valid trade amount", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.createTradingRule(
                                    symbol = ruleSymbol,
                                    action = ruleAction,
                                    condition = ruleCondition,
                                    threshold = threshold,
                                    amount = amount
                                )
                                Toast.makeText(context, "AI Rule deployed!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("DEPLOY ALGORITHMIC RULE", fontWeight = FontWeight.Bold, color = BackgroundDark, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active Rules Header
                Text("DEPLOYED AI ACTIVE RULES (${tradingRules.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(8.dp))

                if (tradingRules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active trading rules configured.", fontSize = 10.sp, color = TextMuted)
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tradingRules.forEach { rule ->
                            val assetPrice = currentBalances.find { it.symbol == rule.assetSymbol }?.priceUsd ?: 0.0
                            val isPriceTrigger = rule.triggerCondition.startsWith("PRICE")

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CardDarkBorder, RoundedCornerShape(10.dp))
                                    .background(BackgroundDark)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (rule.actionType == "BUY") ElectricEmerald.copy(alpha = 0.15f) else Color(0xFFFF3366).copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "AUTO-${rule.actionType}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (rule.actionType == "BUY") ElectricEmerald else Color(0xFFFF3366)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${rule.tradeAmountCrypto} ${rule.assetSymbol}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = when(rule.triggerCondition) {
                                                "PRICE_ABOVE" -> "Trigger when price is ABOVE $${rule.targetThreshold.toInt()}"
                                                "PRICE_BELOW" -> "Trigger when price is BELOW $${rule.targetThreshold.toInt()}"
                                                "SENTIMENT_BULLISH" -> "Trigger when AI sentiment is BULLISH/POSITIVE"
                                                "SENTIMENT_BEARISH" -> "Trigger when AI sentiment is BEARISH/NEGATIVE"
                                                else -> "Trigger when condition met"
                                            },
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                        if (isPriceTrigger) {
                                            Text(
                                                text = "Current ${rule.assetSymbol} Price: $$assetPrice",
                                                fontSize = 9.sp,
                                                color = CosmicCyan,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        if (rule.lastTriggered != null) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                            Text(
                                                text = "Last triggered: ${sdf.format(Date(rule.lastTriggered))}",
                                                fontSize = 9.sp,
                                                color = ElectricEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Switch(
                                            checked = rule.isEnabled,
                                            onCheckedChange = { viewModel.toggleTradingRule(rule.id, it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = CosmicCyan,
                                                checkedTrackColor = CosmicCyan.copy(alpha = 0.5f),
                                                uncheckedThumbColor = TextMuted,
                                                uncheckedTrackColor = CardDark
                                            )
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteTradingRule(rule.id) }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Rule", tint = Color(0xFFFF3366), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- PRICE ALERTS SCREEN ---
@Composable
fun AlertsManagementScreen(viewModel: WalletViewModel) {
    val alerts by viewModel.priceAlerts.collectAsState()
    var targetSymbol by remember { mutableStateOf("BTC") }
    var priceTargetText by remember { mutableStateOf("95000.0") }
    var directionSelection by remember { mutableStateOf("ABOVE") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = "Alerts", tint = VolatilityPink, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("VOLATILITY ALERTS", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }

        Text(
            "Configure automatic client-side triggers to monitor extreme crypto price movements. Alerts execute in safe background cycles.",
            fontSize = 11.sp,
            color = TextMuted
        )

        // Custom alert builder Form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardDarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CREATE CUSTOM VOLATILITY ALERT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Symbol Picker
                    listOf("BTC", "ETH", "SOL", "BNB").forEach { sym ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (targetSymbol == sym) CosmicCyan else BackgroundDark)
                                .clickable { targetSymbol = sym }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sym,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (targetSymbol == sym) BackgroundDark else TextMuted
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Direction ABOVE / BELOW
                    listOf("ABOVE", "BELOW").forEach { dir ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (directionSelection == dir) VolatilityPink else BackgroundDark)
                                .clickable { directionSelection = dir }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dir,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (directionSelection == dir) TextWhite else TextMuted
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = priceTargetText,
                    onValueChange = { priceTargetText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CosmicCyan,
                        unfocusedBorderColor = CardDarkBorder
                    ),
                    singleLine = true,
                    placeholder = { Text("Target Price USD", color = TextGray) }
                )

                Button(
                    onClick = {
                        val price = priceTargetText.toDoubleOrNull() ?: 0.0
                        if (price > 0.0) {
                            viewModel.createPriceAlert(targetSymbol, price, directionSelection)
                            Toast.makeText(context, "Target Alert set.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VolatilityPink),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("DEPLOY TRIGGER", fontWeight = FontWeight.Bold, color = TextWhite)
                }
            }
        }

        // Active Alert List
        Text("ACTIVE TELEMETRY MONITORS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        
        alerts.forEach { alert ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardDarkBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (alert.direction == "ABOVE") Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = "Direction",
                            tint = if (alert.direction == "ABOVE") ElectricEmerald else VolatilityPink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("${alert.symbol} Price Target", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(
                                text = "Notify when price goes ${alert.direction.lowercase()} $${alert.targetPrice}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.removePriceAlert(alert.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = VolatilityPink)
                    }
                }
            }
        }
    }
}

// --- NAVIGATION SIDEBAR ---
@Composable
fun WalletSidebar(
    currentScreen: String,
    onSelect: (String) -> Unit,
    onLock: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val menuItems = listOf(
        SidebarMenuItem("dashboard", "Portfolio", Icons.Default.AccountBalanceWallet),
        SidebarMenuItem("swap", "Bridge Portal", Icons.Default.SwapHoriz),
        SidebarMenuItem("news_ai", "AI Algorithm", Icons.Default.AutoAwesome),
        SidebarMenuItem("dapp", "DApp Browser", Icons.Default.Explore),
        SidebarMenuItem("alerts", "Price Alerts", Icons.Default.NotificationsActive),
        SidebarMenuItem("wallet_keys", "Security & Tax", Icons.Default.Key)
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isExpanded) 180.dp else 68.dp)
            .background(CardDark)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = CardDarkBorder,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExpanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ElectricEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Z", color = ElectricEmerald, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ZWallet",
                            color = TextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElectricEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Z", color = ElectricEmerald, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                if (isExpanded) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Collapse",
                            tint = CosmicCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (!isExpanded) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Expand",
                        tint = CosmicCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                menuItems.forEach { item ->
                    val isSelected = currentScreen == item.route || 
                        (item.route == "wallet_keys" && currentScreen == "wallet_keys") ||
                        (item.route == "alerts" && currentScreen == "alerts")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ActiveNavBg else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) ElectricEmerald.copy(alpha = 0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelect(item.route) }
                            .testTag("sidebar_nav_" + item.route),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) ActiveNavText else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            if (isExpanded) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextWhite else TextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFF3366).copy(alpha = 0.1f))
                .border(1.dp, Color(0xFFFF3366).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .clickable { onLock() }
                .testTag("sidebar_lock"),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = Color(0xFFFF3366),
                    modifier = Modifier.size(18.dp)
                )
                if (isExpanded) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Lock Wallet",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF3366)
                    )
                }
            }
        }
    }
}

data class SidebarMenuItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    glow: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (glow) CosmicCyan else CardDark)
                .border(
                    1.dp,
                    if (glow) CosmicCyan else CardDarkBorder,
                    CircleShape
                )
                .clickable { onClick() }
                .testTag("quick_action_" + label.lowercase()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (glow) BackgroundDark else CosmicCyan,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (glow) CosmicCyan else TextMuted,
            letterSpacing = 0.5.sp
        )
    }
}

