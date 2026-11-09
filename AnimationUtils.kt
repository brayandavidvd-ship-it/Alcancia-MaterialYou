package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Standard text display without rolling counters or custom frame animations.
 * Provides instant response and zero overhead.
 */
@Composable
fun RollingCounterText(
    formattedText: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = formattedText,
        style = style,
        color = color,
        modifier = modifier
    )
}
