package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TradeGreen,
    onPrimary = Color(0xFF03251E),
    primaryContainer = Color(0xFF083832),
    onPrimaryContainer = Color(0xFF90F7DC),
    secondary = TradeBlue,
    onSecondary = Color(0xFF0F1A3A),
    background = TradeNavyBg,
    onBackground = TradeText,
    surface = TradePanel,
    onSurface = TradeText,
    surfaceVariant = TradePanel2,
    onSurfaceVariant = TradeMuted,
    outline = TradeLine,
    error = TradeRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
