package com.sevengold.signalapp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Kumpulan gradient dipakai berulang di seluruh app supaya tampilannya konsisten
 * "premium" (bukan flat/polos) — dipakai di background layar, header card, tombol utama,
 * dan avatar.
 */
object SignalGradients {
    val screenBackground = Brush.verticalGradient(
        colors = listOf(NavyBackground, Color(0xFF0C1526), NavyBackground)
    )

    val goldButton = Brush.horizontalGradient(
        colors = listOf(GoldDeep, GoldPrimary, GoldLight)
    )

    val heroCard = Brush.linearGradient(
        colors = listOf(Color(0xFF1B2540), Color(0xFF0F1626))
    )

    val goldAccentBar = Brush.verticalGradient(
        colors = listOf(GoldLight, GoldPrimary, GoldDeep)
    )

    val avatarRing = Brush.linearGradient(
        colors = listOf(GoldLight, GoldPrimary, EmeraldAccent)
    )

    val premiumBadge = Brush.horizontalGradient(
        colors = listOf(GoldDeep, GoldPrimary)
    )
}
