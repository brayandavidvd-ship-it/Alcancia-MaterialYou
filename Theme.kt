package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettingsEntity

fun getShapesForRadius(cornerRadius: String): Shapes {
    val cornerDp = when (cornerRadius) {
        "STRAIGHT" -> 4.dp
        "MEDIUM" -> 16.dp
        else -> 28.dp // "ROUNDED"
    }
    return Shapes(
        extraSmall = RoundedCornerShape((cornerDp.value * 0.4f).coerceAtLeast(4f).dp),
        small = RoundedCornerShape((cornerDp.value * 0.6f).coerceAtLeast(6f).dp),
        medium = RoundedCornerShape(cornerDp),
        large = RoundedCornerShape((cornerDp.value * 1.15f).coerceAtLeast(12f).dp),
        extraLarge = RoundedCornerShape((cornerDp.value * 1.3f).coerceAtLeast(16f).dp)
    )
}

fun getNumberFont(fontKey: String): FontFamily {
    return when (fontKey) {
        "TABULAR", "MONOSPACE" -> FontFamily.Default // Same clean font, using tabular figures
        "ROUNDED" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }
}

fun getNumberFontFeatures(fontKey: String): String {
    return when (fontKey) {
        "TABULAR", "MONOSPACE" -> "tnum"
        else -> ""
    }
}

@Composable
fun AlcanciaTheme(
    settings: AppSettingsEntity = AppSettingsEntity(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDark
    }

    val context = LocalContext.current
    val chosenAccent = if (settings.accentColorIndex in PresetAccents.indices) {
        PresetAccents[settings.accentColorIndex].color
    } else {
        Color(settings.customAccentColor)
    }

    val colorScheme: ColorScheme = when {
        settings.useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            buildHarmoniousColorScheme(
                primaryColor = chosenAccent,
                isDark = isDark,
                bgStyle = settings.bgStyle,
                isAmoled = settings.isAmoledBlack
            )
        }
    }

    val shapes = getShapesForRadius(settings.cornerRadius)
    val numberFont = getNumberFont(settings.numberFont)
    val numberFeatures = getNumberFontFeatures(settings.numberFont)
    val incomeColor = if (settings.isColorblindPreset) ColorblindIncomeColor else Color(settings.incomeColor)
    val expenseColor = if (settings.isColorblindPreset) ColorblindExpenseColor else Color(settings.expenseColor)
    val savingsColor = if (settings.isColorblindPreset) ColorblindSavingsColor else Color(settings.savingsColor)
    val isCompact = settings.density == "COMPACT"

    CompositionLocalProvider(
        LocalIncomeColor provides incomeColor,
        LocalExpenseColor provides expenseColor,
        LocalSavingsColor provides savingsColor,
        LocalCardStyle provides settings.cardStyle,
        LocalNumberFontFamily provides numberFont,
        LocalNumberFontFeatures provides numberFeatures,
        LocalIsCompactDensity provides isCompact
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = shapes,
            content = content
        )
    }
}
