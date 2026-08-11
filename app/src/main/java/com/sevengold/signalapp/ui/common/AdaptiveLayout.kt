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
 * Responsive content frame for phones, foldables and tablets.
 *
 * Breakpoints use available window width instead of device names, so the UI
 * also adapts correctly to split-screen and landscape mode.
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
            maxWidth >= 840.dp -> 32.dp
            maxWidth >= 600.dp -> 24.dp
            else -> 16.dp
        }

        val contentMaxWidth = when {
            maxWidth >= 840.dp -> 1200.dp
            maxWidth >= 600.dp -> 960.dp
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
