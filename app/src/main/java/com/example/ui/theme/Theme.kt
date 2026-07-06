package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricEmerald,
    onPrimary = BackgroundDark,
    secondary = CosmicCyan,
    onSecondary = BackgroundDark,
    tertiary = VolatilityPink,
    background = BackgroundDark,
    onBackground = TextWhite,
    surface = CardDark,
    onSurface = TextWhite,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextMuted,
    outline = CardDarkBorder,
    error = VolatilityPink,
    onError = Color.White
)

@Composable
fun ZWalletTheme(
    content: @Composable () -> Unit
) {
    // Force dark theme as requested for dark mode accessibility / low-light cyberpunk environment
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
