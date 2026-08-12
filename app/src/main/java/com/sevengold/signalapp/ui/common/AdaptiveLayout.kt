package com.sevengold.signalapp.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared responsive frame.
 *
 * Uses available width instead of device names, so phones, tablets,
 * foldables and split-screen all get sensible spacing automatically.
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
        val horizontalPadding = when {
            maxWidth >= 1200.dp -> 40.dp
            maxWidth >= 840.dp -> 28.dp
            maxWidth >= 600.dp -> 20.dp
            maxWidth >= 420.dp -> 16.dp
            else -> 12.dp
        }

        val contentMaxWidth = when {
            maxWidth >= 1200.dp -> 1120.dp
            maxWidth >= 840.dp -> 1040.dp
            maxWidth >= 600.dp -> 920.dp
            else -> maxWidth
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = contentMaxWidth)
                .padding(horizontal = horizontalPadding)
        ) {
            content()
        }
    }
}
