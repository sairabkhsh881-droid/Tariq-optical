package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = OpticalNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E4F7),
    onPrimaryContainer = OpticalNavyDark,
    secondary = PrecisionTeal,
    onSecondary = Color.White,
    secondaryContainer = PrecisionCyanContainer,
    onSecondaryContainer = OnPrecisionCyanContainer,
    tertiary = BrushedGold,
    onTertiary = Color.White,
    tertiaryContainer = BrushedGoldLight,
    onTertiaryContainer = OnBrushedGoldContainer,
    error = CriticalStockRed,
    onError = Color.White,
    errorContainer = CriticalStockContainer,
    onErrorContainer = Color(0xFF7F1D1D),
    background = AlabasterBackground,
    onBackground = TextPrimaryDark,
    surface = PureSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextSecondarySlate,
    outline = Color(0xFFCBD5E1)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrecisionCyanBright,
    onPrimary = OpticalNavyDark,
    primaryContainer = OpticalNavyLight,
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF38BDF8),
    onSecondary = OpticalNavyDark,
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3E2E04),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = BrushedGoldLight,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = DarkBackground,
    onBackground = TextPrimaryLight,
    surface = DarkSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFF334155)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
