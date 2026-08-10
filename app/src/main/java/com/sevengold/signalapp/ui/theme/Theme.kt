package com.sevengold.signalapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GoldPrimary = Color(0xFFC99B3F)
private val DarkBackground = Color(0xFF0B1220)

private val DarkColors = darkColorScheme(
    primary = GoldPrimary,
    secondary = Color(0xFF4CAF93),
    background = DarkBackground,
    surface = Color(0xFF141C2E)
)

private val LightColors = lightColorScheme(
    primary = GoldPrimary,
    secondary = Color(0xFF4CAF93)
)

@Composable
fun SignalAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
