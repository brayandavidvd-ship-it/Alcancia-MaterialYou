package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalCardStyle
import com.example.ui.theme.ensureMinContrast

@Composable
fun getCardColorsForStyle(style: String = LocalCardStyle.current): CardColors {
    val surface = MaterialTheme.colorScheme.surface
    val container = when (style) {
        "OUTLINE" -> surface.copy(alpha = 0.2f)
        "TINT" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val content = ensureMinContrast(
        textColor = MaterialTheme.colorScheme.onSurface,
        backgroundColor = container,
        minContrast = 4.5,
        baseSurface = surface
    )
    return CardDefaults.cardColors(
        containerColor = container,
        contentColor = content
    )
}

@Composable
fun getCardBorderForStyle(style: String = LocalCardStyle.current): BorderStroke? {
    return if (style == "OUTLINE") {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
    } else {
        null
    }
}

@Composable
fun AlcanciaCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    onClick: (() -> Unit)? = null,
    colors: CardColors = getCardColorsForStyle(),
    border: BorderStroke? = getCardBorderForStyle(),
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            content = content
        )
    }
}
