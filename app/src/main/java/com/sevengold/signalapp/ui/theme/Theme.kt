package com.sevengold.signalapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Palet premium: emas (gold) di atas navy gelap yang dalam, terinspirasi kartu member VIP ---
val GoldPrimary = Color(0xFFD4AF62)
val GoldLight = Color(0xFFF1D999)
val GoldDeep = Color(0xFFA9762F)
val EmeraldAccent = Color(0xFF3FBF8F)
val DangerRed = Color(0xFFE5657A)

val NavyBackground = Color(0xFF070C16)
val NavySurface = Color(0xFF121A2C)
val NavySurfaceElevated = Color(0xFF19233A)
val NavyOutline = Color(0xFF2B3752)

private val DarkColors = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF241A02),
    primaryContainer = Color(0xFF3A2E10),
    onPrimaryContainer = GoldLight,
    secondary = EmeraldAccent,
    onSecondary = Color(0xFF033223),
    background = NavyBackground,
    onBackground = Color(0xFFEDEFF5),
    surface = NavySurface,
    onSurface = Color(0xFFEDEFF5),
    surfaceVariant = NavySurfaceElevated,
    onSurfaceVariant = Color(0xFFB6BECF),
    outline = NavyOutline,
    error = DangerRed,
    onError = Color(0xFF3A0714)
)

private val LightColors = lightColorScheme(
    primary = GoldDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6E7C7),
    onPrimaryContainer = Color(0xFF3A2E10),
    secondary = Color(0xFF1F9C74),
    background = Color(0xFFFAF8F4),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1ECE0),
    error = Color(0xFFB3273D)
)

val SignalAppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val SignalAppTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 0.2.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, letterSpacing = 0.1.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.6.sp)
)

@Composable
fun SignalAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        shapes = SignalAppShapes,
        typography = SignalAppTypography,
        content = content
    )
}
