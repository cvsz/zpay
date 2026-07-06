package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WalletViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TopUpScreen(viewModel: WalletViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    // State parameters
    var selectedTab by remember { mutableStateOf("PROMPTPAY") } // "PROMPTPAY", "CARD", "CRYPTO"
    var selectedAsset by remember { mutableStateOf("BTC") } // "BTC", "ETH", "SOL", "BNB"
    var fiatAmountInput by remember { mutableStateOf("1000") } // Default 1000 THB
    
    // Card inputs
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var cardType by remember { mutableStateOf("unknown") } // "visa", "mastercard", "unknown"

    // Custom Transaction States
    var isProcessing by remember { mutableStateOf(false) }
    var processingStage by remember { mutableStateOf("") }
    var topupSuccess by remember { mutableStateOf(false) }
    var successCryptoAmount by remember { mutableStateOf(0.0) }
    var successFiatAmount by remember { mutableStateOf(0.0) }

    // Constants for conversion (1 USD = 35 THB approximately)
    val thbToUsdRate = 1.0 / 35.0
    val assetPrices = mapOf(
        "BTC" to 91520.0,
        "ETH" to 3340.0,
        "SOL" to 184.20,
        "BNB" to 582.60
    )

    // Derived values
    val currentFiatAmount = fiatAmountInput.toDoubleOrNull() ?: 0.0
    val currentUsdAmount = currentFiatAmount * thbToUsdRate
    val assetPrice = assetPrices[selectedAsset] ?: 1.0
    val calculatedCryptoAmount = if (assetPrice > 0) currentUsdAmount / assetPrice else 0.0

    // Card detector logic
    LaunchedEffect(cardNumber) {
        val clean = cardNumber.replace(" ", "")
        cardType = when {
            clean.startsWith("4") -> "visa"
            clean.startsWith("5") && clean.length >= 2 && clean.substring(0, 2).toIntOrNull() in 51..55 -> "mastercard"
            else -> "unknown"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // High-fidelity Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardDark)
                        .border(1.dp, CardDarkBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "FIAT ON-RAMP PORTAL",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ElectricEmerald.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ElectricEmerald)
                        )
                        Text(
                            text = "PCI-COMPLIANT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricEmerald
                        )
                    }
                }
            }

            if (!topupSuccess) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECURE ON-RAMP CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardDarkBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "ASSET & AMOUNT CONFIGURATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicCyan,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Asset Selectors Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("BTC", "ETH", "SOL", "BNB").forEach { asset ->
                                    val isAssetSelected = selectedAsset == asset
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isAssetSelected) CosmicCyan else BackgroundDark)
                                            .border(
                                                1.dp,
                                                if (isAssetSelected) CosmicCyan else CardDarkBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedAsset = asset }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = asset,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAssetSelected) BackgroundDark else TextWhite
                                            )
                                            Text(
                                                text = "$${assetPrices[asset]}",
                                                fontSize = 8.sp,
                                                color = if (isAssetSelected) BackgroundDark.copy(alpha = 0.8f) else TextMuted
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Input Field
                            Text(
                                text = "Amount to deposit (THB)",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = fiatAmountInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 7) {
                                        fiatAmountInput = input
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("topup_fiat_input"),
                                textStyle = LocalTextStyle.current.copy(
                                    color = TextWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                placeholder = { Text("0", color = TextGray) },
                                leadingIcon = {
                                    Text(
                                        "฿",
                                        fontSize = 18.sp,
                                        color = CosmicCyan,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                    )
                                },
                                trailingIcon = {
                                    Text(
                                        "THB",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CosmicCyan,
                                    unfocusedBorderColor = CardDarkBorder,
                                    focusedContainerColor = BackgroundDark,
                                    unfocusedContainerColor = BackgroundDark
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Quick Presets Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("500", "1000", "5000", "10000").forEach { preset ->
                                    val isSelected = fiatAmountInput == preset
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) ElectricEmerald.copy(alpha = 0.15f) else BackgroundDark)
                                            .border(
                                                1.dp,
                                                if (isSelected) ElectricEmerald else CardDarkBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                fiatAmountInput = preset
                                                focusManager.clearFocus()
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "฿$preset",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) ElectricEmerald else TextMuted
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Live Rate calculations
                            HorizontalDivider(color = CardDarkBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ESTIMATED CRYPTO LOAD", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = "${String.format("%.6f", calculatedCryptoAmount)} $selectedAsset",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricEmerald,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("USD EQUIVALENT", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = "$${String.format("%,.2f", currentUsdAmount)} USD",
                                    fontSize = 11.sp,
                                    color = TextWhite,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // PAYMENT METHOD TABS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardDark)
                            .border(1.dp, CardDarkBorder, RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        listOf(
                            Triple("PROMPTPAY", Icons.Default.QrCode, "PromptPay"),
                            Triple("CARD", Icons.Default.CreditCard, "Card"),
                            Triple("CRYPTO", Icons.Default.CurrencyExchange, "Crypto")
                        ).forEach { (tabId, icon, label) ->
                            val isTabSelected = selectedTab == tabId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isTabSelected) BackgroundDark else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isTabSelected) CardDarkBorder else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedTab = tabId }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isTabSelected) CosmicCyan else TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTabSelected) CosmicCyan else TextMuted
                                    )
                                }
                            }
                        }
                    }

                    // TAB RENDERING
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                        },
                        label = "TabContent"
                    ) { activeTab ->
                        when (activeTab) {
                            "PROMPTPAY" -> {
                                PromptPayOnRampPanel(
                                    fiatAmount = currentFiatAmount,
                                    selectedAsset = selectedAsset,
                                    calculatedCryptoAmount = calculatedCryptoAmount,
                                    onConfirm = {
                                        scope.launch {
                                            isProcessing = true
                                            processingStage = "Establishing Secure PromptPay Link..."
                                            delay(1000)
                                            processingStage = "Verifying Bank Gateway QR Status..."
                                            delay(1200)
                                            processingStage = "Finalizing Crypto Minting..."
                                            delay(1000)
                                            isProcessing = false
                                            successCryptoAmount = calculatedCryptoAmount
                                            successFiatAmount = currentFiatAmount
                                            viewModel.performTopUp(
                                                symbol = selectedAsset,
                                                cryptoAmount = calculatedCryptoAmount,
                                                fiatAmount = currentFiatAmount,
                                                paymentMethod = "PromptPay"
                                            )
                                            topupSuccess = true
                                        }
                                    }
                                )
                            }
                            "CARD" -> {
                                CardOnRampPanel(
                                    cardNumber = cardNumber,
                                    cardExpiry = cardExpiry,
                                    cardCvv = cardCvv,
                                    cardHolder = cardHolder,
                                    cardType = cardType,
                                    onNumberChange = { if (it.length <= 19) cardNumber = it },
                                    onExpiryChange = { if (it.length <= 5) cardExpiry = it },
                                    onCvvChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) cardCvv = it },
                                    onHolderChange = { if (it.length <= 30) cardHolder = it },
                                    onConfirm = {
                                        if (cardNumber.length < 16 || cardExpiry.length < 5 || cardCvv.length < 3 || cardHolder.isEmpty()) {
                                            Toast.makeText(context, "Please complete card credentials", Toast.LENGTH_SHORT).show()
                                            return@CardOnRampPanel
                                        }
                                        scope.launch {
                                            isProcessing = true
                                            processingStage = "Inbound Authorization with Bank Gateway..."
                                            delay(1200)
                                            processingStage = "Securing 3D Secure Verification..."
                                            delay(1500)
                                            processingStage = "Encrypting Settlement..."
                                            delay(1000)
                                            isProcessing = false
                                            successCryptoAmount = calculatedCryptoAmount
                                            successFiatAmount = currentFiatAmount
                                            viewModel.performTopUp(
                                                symbol = selectedAsset,
                                                cryptoAmount = calculatedCryptoAmount,
                                                fiatAmount = currentFiatAmount,
                                                paymentMethod = "Card"
                                            )
                                            topupSuccess = true
                                        }
                                    }
                                )
                            }
                            "CRYPTO" -> {
                                CryptoTransferPanel(
                                    selectedAsset = selectedAsset,
                                    viewModel = viewModel,
                                    calculatedCryptoAmount = calculatedCryptoAmount,
                                    onConfirm = {
                                        scope.launch {
                                            isProcessing = true
                                            processingStage = "Scanning blockchain node mempools..."
                                            delay(1500)
                                            processingStage = "Checking gas threshold requirements..."
                                            delay(1000)
                                            processingStage = "Completing balance transfer..."
                                            delay(1200)
                                            isProcessing = false
                                            successCryptoAmount = calculatedCryptoAmount
                                            successFiatAmount = currentFiatAmount
                                            viewModel.performTopUp(
                                                symbol = selectedAsset,
                                                cryptoAmount = calculatedCryptoAmount,
                                                fiatAmount = currentFiatAmount,
                                                paymentMethod = "Crypto"
                                            )
                                            topupSuccess = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // SUCCESS SCREEN
                TopUpSuccessComponent(
                    cryptoAmount = successCryptoAmount,
                    fiatAmount = successFiatAmount,
                    symbol = selectedAsset,
                    paymentMethod = when (selectedTab) {
                        "PROMPTPAY" -> "PromptPay QR"
                        "CARD" -> "Visa/Mastercard"
                        else -> "Crypto Network"
                    },
                    onBack = {
                        topupSuccess = false
                        cardNumber = ""
                        cardExpiry = ""
                        cardCvv = ""
                        cardHolder = ""
                        onBack()
                    }
                )
            }
        }

        // FULL SCREEN SEMI-TRANSPARENT LOADING MODAL FOR CRYPTO PROCESSING
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark.copy(alpha = 0.85f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .border(1.dp, CosmicCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = CosmicCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "SECURE PROTOCOL RUNNING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicCyan,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = processingStage,
                            fontSize = 13.sp,
                            color = TextWhite,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Do not close or leave this screen. Transaction is secured by AES-256 HSM architecture.",
                            fontSize = 9.sp,
                            color = TextMuted,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PromptPayOnRampPanel(
    fiatAmount: Double,
    selectedAsset: String,
    calculatedCryptoAmount: Double,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardDarkBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // PromptPay Header Design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark, RoundedCornerShape(8.dp))
                    .border(1.dp, CardDarkBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // PromptPay Stylized Text Logo
                Text(
                    text = "Prompt",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF003764)
                )
                Text(
                    text = "Pay",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0072B1)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0072B1).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("DYNAMIC QR", fontSize = 8.sp, color = Color(0xFF0072B1), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Simulated dynamic QR Code using custom high-fidelity Grid drawing
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(2.dp, Color(0xFF003764), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // High-fidelity Stylized QR Pattern
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cols = 9
                    val rows = 9
                    val squareWidth = size.width / cols
                    val squareHeight = size.height / rows

                    // Stylized mock QR array
                    for (c in 0 until cols) {
                        for (r in 0 until rows) {
                            // Draw corner finding patterns (classic QR anchors)
                            val isAnchor = (c < 3 && r < 3) || (c >= cols - 3 && r < 3) || (c < 3 && r >= rows - 3)
                            val isCenterDot = (c == 4 && r == 4) || (c == 5 && r == 3) || (c == 3 && r == 5)
                            val isRandomBit = ((c * 7 + r * 13) % 5 == 0 || (c * r) % 3 == 1)

                            if (isAnchor) {
                                // Draw solid anchor ring
                                if (c == 0 || c == 2 || r == 0 || r == 2) {
                                    drawRect(
                                        color = Color(0xFF003764),
                                        topLeft = androidx.compose.ui.geometry.Offset(c * squareWidth, r * squareHeight),
                                        size = androidx.compose.ui.geometry.Size(squareWidth, squareHeight)
                                    )
                                } else if (c == 1 && r == 1) {
                                    // center block of anchor
                                    drawRect(
                                        color = Color(0xFF003764),
                                        topLeft = androidx.compose.ui.geometry.Offset(c * squareWidth, r * squareHeight),
                                        size = androidx.compose.ui.geometry.Size(squareWidth, squareHeight)
                                    )
                                }
                            } else if (isCenterDot || isRandomBit) {
                                drawRect(
                                    color = Color(0xFF0b0c10),
                                    topLeft = androidx.compose.ui.geometry.Offset(c * squareWidth, r * squareHeight),
                                    size = androidx.compose.ui.geometry.Size(squareWidth, squareHeight)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "THB ${String.format("%,.2f", fiatAmount)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Equivalent to ~${String.format("%.6f", calculatedCryptoAmount)} $selectedAsset",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gateway Provider", fontSize = 10.sp, color = TextMuted)
                    Text("ZWallet Bank Settlement", fontSize = 10.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Scan Limit", fontSize = 10.sp, color = TextMuted)
                    Text("Expires in 15:00 min", fontSize = 10.sp, color = VolatilityPink, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Confirm",
                    tint = BackgroundDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("SIMULATE SCAN & APPROVE", fontSize = 11.sp, color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CardOnRampPanel(
    cardNumber: String,
    cardExpiry: String,
    cardCvv: String,
    cardHolder: String,
    cardType: String,
    onNumberChange: (String) -> Unit,
    onExpiryChange: (String) -> Unit,
    onCvvChange: (String) -> Unit,
    onHolderChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardDarkBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "SECURE CARD DETAILS (PCI-DSS ENCRYPTED)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CosmicCyan,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Visual Card Representation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1f2833), Color(0xFF0b0c10))
                        )
                    )
                    .border(1.dp, CardDarkBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PREMIUM DEBIT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicCyan,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = when (cardType) {
                                "visa" -> Icons.Default.CreditCard
                                "mastercard" -> Icons.Default.CreditCard
                                else -> Icons.Default.CreditCard
                            },
                            contentDescription = "Card Issuer Logo",
                            tint = if (cardType != "unknown") ElectricEmerald else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = cardNumber.chunked(4).joinToString(" ").ifEmpty { "•••• •••• •••• ••••" },
                            fontSize = 16.sp,
                            color = TextWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("CARDHOLDER", fontSize = 7.sp, color = TextMuted)
                                Text(
                                    text = cardHolder.uppercase().ifEmpty { "YOUR NAME" },
                                    fontSize = 10.sp,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("EXPIRES", fontSize = 7.sp, color = TextMuted)
                                Text(
                                    text = cardExpiry.ifEmpty { "MM/YY" },
                                    fontSize = 10.sp,
                                    color = TextWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card inputs form
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Card Number Input
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        onNumberChange(digits)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Card Number", fontSize = 11.sp, color = TextMuted) },
                    textStyle = LocalTextStyle.current.copy(color = TextWhite, fontFamily = FontFamily.Monospace),
                    placeholder = { Text("4000 1234 5678 9010", color = TextGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicCyan,
                        unfocusedBorderColor = CardDarkBorder,
                        focusedContainerColor = BackgroundDark,
                        unfocusedContainerColor = BackgroundDark
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Expiry Input
                    OutlinedTextField(
                        value = cardExpiry,
                        onValueChange = { input ->
                            // Format automatically MM/YY
                            val clean = input.filter { it.isDigit() }
                            val formatted = if (clean.length >= 3) {
                                "${clean.substring(0, 2)}/${clean.substring(2)}"
                            } else {
                                clean
                            }
                            onExpiryChange(formatted)
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Expires", fontSize = 11.sp, color = TextMuted) },
                        textStyle = LocalTextStyle.current.copy(color = TextWhite, fontFamily = FontFamily.Monospace),
                        placeholder = { Text("MM/YY", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicCyan,
                            unfocusedBorderColor = CardDarkBorder,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark
                        )
                    )

                    // CVV Input
                    OutlinedTextField(
                        value = cardCvv,
                        onValueChange = onCvvChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("CVV", fontSize = 11.sp, color = TextMuted) },
                        textStyle = LocalTextStyle.current.copy(color = TextWhite, fontFamily = FontFamily.Monospace),
                        placeholder = { Text("•••", color = TextGray) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicCyan,
                            unfocusedBorderColor = CardDarkBorder,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark
                        )
                    )
                }

                // Holder Name Input
                OutlinedTextField(
                    value = cardHolder,
                    onValueChange = onHolderChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cardholder Name", fontSize = 11.sp, color = TextMuted) },
                    textStyle = LocalTextStyle.current.copy(color = TextWhite),
                    placeholder = { Text("John Doe", color = TextGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicCyan,
                        unfocusedBorderColor = CardDarkBorder,
                        focusedContainerColor = BackgroundDark,
                        unfocusedContainerColor = BackgroundDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = BackgroundDark,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("PROCESS SECURE PAYMENT", fontSize = 11.sp, color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CryptoTransferPanel(
    selectedAsset: String,
    viewModel: WalletViewModel,
    calculatedCryptoAmount: Double,
    onConfirm: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val depositAddress = when (selectedAsset) {
        "BTC" -> viewModel.btcAddress
        "SOL" -> viewModel.solAddress
        else -> viewModel.ethAddress
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardDarkBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "DIRECT CRYPTO DEPOSIT MEMPOOL LISTENERS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CosmicCyan,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Stylized deposit QR
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(2.dp, CardDarkBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cols = 8
                    val rows = 8
                    val squareWidth = size.width / cols
                    val squareHeight = size.height / rows

                    for (c in 0 until cols) {
                        for (r in 0 until rows) {
                            val isAnchor = (c < 2 && r < 2) || (c >= cols - 2 && r < 2) || (c < 2 && r >= rows - 2)
                            val isCenterDot = (c == 3 && r == 3) || (c == 4 && r == 2) || (c == 2 && r == 4)
                            val isRandomBit = ((c * 9 + r * 11) % 4 == 0)

                            if (isAnchor) {
                                if (c == 0 || c == 1 || r == 0 || r == 1) {
                                    drawRect(
                                        color = Color(0xFF0b0c10),
                                        topLeft = androidx.compose.ui.geometry.Offset(c * squareWidth, r * squareHeight),
                                        size = androidx.compose.ui.geometry.Size(squareWidth, squareHeight)
                                    )
                                }
                            } else if (isCenterDot || isRandomBit) {
                                drawRect(
                                    color = Color(0xFF0b0c10),
                                    topLeft = androidx.compose.ui.geometry.Offset(c * squareWidth, r * squareHeight),
                                    size = androidx.compose.ui.geometry.Size(squareWidth, squareHeight)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Address display card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark, RoundedCornerShape(10.dp))
                    .border(1.dp, CardDarkBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "YOUR SECURE RECEIVING ADDRESS ($selectedAsset)",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = depositAddress,
                        fontSize = 11.sp,
                        color = TextWhite,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(depositAddress))
                            Toast.makeText(context, "$selectedAsset Address Copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CardDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Address",
                            tint = CosmicCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElectricEmerald.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield",
                    tint = ElectricEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Deposit to this address only. Sending non-$selectedAsset assets to this address will result in permanent loss of funds.",
                    fontSize = 9.sp,
                    color = ElectricEmerald,
                    lineHeight = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Check Blockchain",
                    tint = BackgroundDark,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("SIMULATE BLOCKCHAIN CREDIT", fontSize = 11.sp, color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TopUpSuccessComponent(
    cryptoAmount: Double,
    fiatAmount: Double,
    symbol: String,
    paymentMethod: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High-fidelity success vector & feedback loops
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ElectricEmerald.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(ElectricEmerald.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = ElectricEmerald,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "LOAD SUCCESSFUL",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricEmerald,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "+${String.format("%.6f", cryptoAmount)} $symbol",
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            color = TextWhite,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Settled via $paymentMethod: ฿${String.format("%,.2f", fiatAmount)} THB",
            fontSize = 13.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Receipt details card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardDarkBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "TRANSACTION RECEIPT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CosmicCyan,
                    letterSpacing = 0.5.sp
                )
                
                HorizontalDivider(color = CardDarkBorder, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Status", fontSize = 11.sp, color = TextMuted)
                    Text("SUCCESS / CONFIRMED", fontSize = 11.sp, color = ElectricEmerald, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Transaction ID", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = "tx_" + java.util.UUID.randomUUID().toString().substring(0, 16),
                        fontSize = 11.sp,
                        color = TextWhite,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Security Protocol", fontSize = 11.sp, color = TextMuted)
                    Text("AES-256-GCM / HSM Secured", fontSize = 11.sp, color = CosmicCyan, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Network Fee", fontSize = 11.sp, color = TextMuted)
                    Text("FREE", fontSize = 11.sp, color = ElectricEmerald, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("BACK TO PORTFOLIO", fontSize = 12.sp, color = BackgroundDark, fontWeight = FontWeight.Bold)
        }
    }
}
