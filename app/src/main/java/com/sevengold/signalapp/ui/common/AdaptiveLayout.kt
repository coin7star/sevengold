package com.sevengold.signalapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Responsive content frame for phones, foldables and tablets.
 *
 * Uses the available window width so split-screen, landscape and tablets
 * receive an appropriate content density without relying on device models.
 */
@Composable
fun AdaptiveAppFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isTablet = maxWidth >= 600.dp
        val isWide = maxWidth >= 840.dp
        // Keep phone content wider while still respecting safe screen edges.
        // The screen itself may add small inner padding for cards/lists.
        val horizontalPadding = when {
            isWide -> 24.dp
            isTablet -> 16.dp
            else -> 8.dp
        }
        val maxContentWidth = when {
            isWide -> 1400.dp
            isTablet -> 1100.dp
            else -> 760.dp
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxContentWidth)
                .padding(horizontal = horizontalPadding)
        ) {
            content()
        }
    }
}

/**
 * A lightweight responsive column used by screens that need consistent
 * vertical rhythm while remaining comfortable on tablets.
 */
@Composable
fun AdaptiveContentColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val spacing = if (maxWidth >= 600.dp) 16.dp else 12.dp
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            content()
        }
    }
}
