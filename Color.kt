package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.core.graphics.ColorUtils
import com.example.data.model.CategoryEntity

data class PresetAccent(
    val name: String,
    val color: Color
)

val PresetAccents = listOf(
    PresetAccent("Esmeralda", Color(0xFF107C41)),
    PresetAccent("Menta", Color(0xFF00897B)),
    PresetAccent("Océano", Color(0xFF0288D1)),
    PresetAccent("Índigo", Color(0xFF3F51B5)),
    PresetAccent("Púrpura", Color(0xFF7B1FA2)),
    PresetAccent("Sakura", Color(0xFFD81B60)),
    PresetAccent("Atardecer", Color(0xFFE64A19)),
    PresetAccent("Ámbar", Color(0xFFF57C00)),
    PresetAccent("Dorado", Color(0xFFFBC02D)),
    PresetAccent("Mocha", Color(0xFF6D4C41)),
    PresetAccent("Pizarra", Color(0xFF455A64)),
    PresetAccent("Grafito", Color(0xFF37474F))
)

data class QuickThemeOption(
    val id: String,
    val name: String,
    val emoji: String,
    val accentColor: Color,
    val cardStyle: String,
    val bgStyle: String,
    val cornerRadius: String
)

val QuickThemes = listOf(
    QuickThemeOption("MINT", "Menta", "🌿", Color(0xFF00897B), "TINT", "LIGHT_TINT", "ROUNDED"),
    QuickThemeOption("SUNSET", "Atardecer", "🌅", Color(0xFFE64A19), "SOLID", "DEFAULT", "ROUNDED"),
    QuickThemeOption("OCEAN", "Océano", "🌊", Color(0xFF0288D1), "TINT", "DEFAULT", "MEDIUM"),
    QuickThemeOption("SAKURA", "Sakura", "🌸", Color(0xFFD81B60), "TINT", "LIGHT_TINT", "ROUNDED"),
    QuickThemeOption("MIDNIGHT", "Medianoche", "🌌", Color(0xFF3F51B5), "OUTLINE", "AMOLED", "STRAIGHT"),
    QuickThemeOption("MOCHA", "Mocha", "☕", Color(0xFF6D4C41), "SOLID", "DEFAULT", "MEDIUM")
)

// Fixed Category Colors
val DefaultCategoryColors = mapOf(
    "comida" to Color(0xFFFF8A65),
    "transporte" to Color(0xFF4FC3F7),
    "casa" to Color(0xFF81C784),
    "salud" to Color(0xFFE57373),
    "ocio" to Color(0xFFBA68C8),
    "escuela" to Color(0xFFFFD54F),
    "compras" to Color(0xFFF06292),
    "otros" to Color(0xFF90A4AE)
)

val NeutralUncategorizedColor = Color(0xFF9E9E9E)

fun getCategoryColor(category: CategoryEntity?, isDark: Boolean = false): Color {
    if (category == null || category.id.isBlank() || category.id == "uncategorized") {
        return NeutralUncategorizedColor
    }
    // Check preset map first for standard categories
    val defaultMatch = DefaultCategoryColors[category.id.lowercase()]
    if (defaultMatch != null) return defaultMatch

    return try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (_: Exception) {
        NeutralUncategorizedColor
    }
}

fun getCategoryColorById(categoryId: String?, categories: List<CategoryEntity>, isDark: Boolean = false): Color {
    if (categoryId.isNullOrBlank() || categoryId == "uncategorized") {
        return NeutralUncategorizedColor
    }
    val cat = categories.find { it.id.equals(categoryId, ignoreCase = true) || it.name.equals(categoryId, ignoreCase = true) }
    return getCategoryColor(cat, isDark)
}

// Financial colors defaults & colorblind mode
val DefaultIncomeColor = Color(0xFF2E7D32)
val DefaultExpenseColor = Color(0xFFC62828)
val DefaultSavingsColor = Color(0xFF0288D1)

// Colorblind friendly: Blue for income, Orange for expense, Purple for savings
val ColorblindIncomeColor = Color(0xFF1976D2)
val ColorblindExpenseColor = Color(0xFFE65100)
val ColorblindSavingsColor = Color(0xFF7B1FA2)

val LocalIncomeColor = compositionLocalOf { DefaultIncomeColor }
val LocalExpenseColor = compositionLocalOf { DefaultExpenseColor }
val LocalSavingsColor = compositionLocalOf { DefaultSavingsColor }
val LocalCardStyle = compositionLocalOf { "SOLID" } // "SOLID", "TINT", "OUTLINE"
val LocalNumberFontFamily = compositionLocalOf<FontFamily> { FontFamily.Default }
val LocalNumberFontFeatures = compositionLocalOf { "" } // e.g. "tnum" for tabular numbers
val LocalIsCompactDensity = compositionLocalOf { false }

@Composable
fun TextStyle.withNumberFont(): TextStyle {
    val family = LocalNumberFontFamily.current
    val features = LocalNumberFontFeatures.current
    return this.copy(
        fontFamily = family,
        fontFeatureSettings = features.ifBlank { null }
    )
}

/**
 * Ensures minimum contrast ratio (e.g. 4.5:1 for WCAG AA compliance)
 * between a foreground text color and background color by adjusting lightness.
 */
fun ensureMinContrast(
    textColor: Color,
    backgroundColor: Color,
    minContrast: Double = 4.5,
    baseSurface: Color = Color.Unspecified
): Color {
    // ColorUtils.calculateContrast requires the background color to be completely opaque (alpha == 255).
    // When backgroundColor is translucent, composite it over an opaque base surface.
    val opaqueBgArgb = run {
        val bg = if (backgroundColor.alpha < 1f) {
            val baseArgb = if (baseSurface != Color.Unspecified && baseSurface.alpha >= 0.99f) {
                baseSurface.toArgb()
            } else {
                val isDark = (backgroundColor.red * 0.299 + backgroundColor.green * 0.587 + backgroundColor.blue * 0.114) < 0.5
                if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
            ColorUtils.compositeColors(backgroundColor.toArgb(), baseArgb)
        } else {
            backgroundColor.toArgb()
        }
        // Strictly guarantee alpha == 255 so ColorUtils.calculateContrast never throws IllegalArgumentException
        bg or 0xFF000000.toInt()
    }

    var textArgb = textColor.toArgb()
    var currentContrast = ColorUtils.calculateContrast(textArgb, opaqueBgArgb)
    if (currentContrast >= minContrast) return textColor

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(textArgb, hsl)
    val bgLuminance = ColorUtils.calculateLuminance(opaqueBgArgb)

    // If background is light, make text darker (decrease lightness)
    // If background is dark, make text lighter (increase lightness)
    if (bgLuminance > 0.45) {
        var l = hsl[2]
        while (l > 0f && currentContrast < minContrast) {
            l -= 0.02f
            hsl[2] = l.coerceIn(0f, 1f)
            textArgb = ColorUtils.HSLToColor(hsl)
            currentContrast = ColorUtils.calculateContrast(textArgb, opaqueBgArgb)
        }
    } else {
        var l = hsl[2]
        while (l < 1f && currentContrast < minContrast) {
            l += 0.02f
            hsl[2] = l.coerceIn(0f, 1f)
            textArgb = ColorUtils.HSLToColor(hsl)
            currentContrast = ColorUtils.calculateContrast(textArgb, opaqueBgArgb)
        }
    }
    return Color(textArgb)
}

/**
 * Builds a harmonious Material 3 ColorScheme from a single primary accent color,
 * ensuring all text on surfaces and tinted backgrounds has at least 4.5:1 contrast.
 */
fun buildHarmoniousColorScheme(
    primaryColor: Color,
    isDark: Boolean,
    bgStyle: String,
    isAmoled: Boolean = false
): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(primaryColor.toArgb(), hsl)
    val hue = hsl[0]
    val sat = hsl[1].coerceIn(0.2f, 0.95f)

    fun colorFromHsl(h: Float, s: Float, l: Float): Color {
        val rgb = ColorUtils.HSLToColor(floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f)))
        return Color(rgb)
    }

    val primary = if (isDark) colorFromHsl(hue, sat, 0.75f) else colorFromHsl(hue, sat, 0.38f)
    val onPrimary = if (isDark) colorFromHsl(hue, sat, 0.10f) else Color.White
    val primaryContainer = if (isDark) colorFromHsl(hue, sat * 0.7f, 0.22f) else colorFromHsl(hue, sat * 0.45f, 0.90f)
    val onPrimaryContainer = if (isDark) colorFromHsl(hue, sat * 0.5f, 0.92f) else colorFromHsl(hue, sat, 0.12f)

    val secondary = if (isDark) colorFromHsl((hue + 25f) % 360f, sat * 0.4f, 0.70f) else colorFromHsl((hue + 25f) % 360f, sat * 0.35f, 0.42f)
    val onSecondary = if (isDark) colorFromHsl(hue, 0.2f, 0.10f) else Color.White
    val secondaryContainer = if (isDark) colorFromHsl((hue + 25f) % 360f, sat * 0.3f, 0.20f) else colorFromHsl((hue + 25f) % 360f, sat * 0.25f, 0.92f)
    val onSecondaryContainer = if (isDark) colorFromHsl((hue + 25f) % 360f, sat * 0.3f, 0.90f) else colorFromHsl((hue + 25f) % 360f, sat * 0.5f, 0.15f)

    val tertiary = if (isDark) colorFromHsl((hue + 60f) % 360f, sat * 0.5f, 0.72f) else colorFromHsl((hue + 60f) % 360f, sat * 0.45f, 0.40f)

    // Background & Surfaces based on bgStyle
    val background: Color
    val surface: Color
    val surfaceVariant: Color
    val surfaceContainer: Color
    val surfaceContainerHigh: Color
    val surfaceContainerHighest: Color

    if (isDark) {
        when {
            isAmoled || bgStyle == "AMOLED" -> {
                background = Color.Black
                surface = Color.Black
                surfaceVariant = Color(0xFF1E1E1E)
                surfaceContainer = Color(0xFF101010)
                surfaceContainerHigh = Color(0xFF1A1A1A)
                surfaceContainerHighest = Color(0xFF262626)
            }
            bgStyle == "DARK_GRAY" -> {
                background = Color(0xFF141416)
                surface = Color(0xFF141416)
                surfaceVariant = Color(0xFF25262B)
                surfaceContainer = Color(0xFF1C1D21)
                surfaceContainerHigh = Color(0xFF26282E)
                surfaceContainerHighest = Color(0xFF31333A)
            }
            bgStyle == "DARK_TINT" -> {
                background = colorFromHsl(hue, 0.20f, 0.08f)
                surface = colorFromHsl(hue, 0.20f, 0.08f)
                surfaceVariant = colorFromHsl(hue, 0.18f, 0.18f)
                surfaceContainer = colorFromHsl(hue, 0.18f, 0.13f)
                surfaceContainerHigh = colorFromHsl(hue, 0.20f, 0.17f)
                surfaceContainerHighest = colorFromHsl(hue, 0.22f, 0.22f)
            }
            else -> {
                // Default dark
                background = Color(0xFF121316)
                surface = Color(0xFF121316)
                surfaceVariant = Color(0xFF22242A)
                surfaceContainer = Color(0xFF1A1C20)
                surfaceContainerHigh = Color(0xFF24262C)
                surfaceContainerHighest = Color(0xFF2E3138)
            }
        }
    } else {
        when (bgStyle) {
            "PURE_WHITE" -> {
                background = Color.White
                surface = Color.White
                surfaceVariant = Color(0xFFF0F0F2)
                surfaceContainer = Color(0xFFF8F9FA)
                surfaceContainerHigh = Color(0xFFEDEDF0)
                surfaceContainerHighest = Color(0xFFE2E3E6)
            }
            "LIGHT_GRAY" -> {
                background = Color(0xFFF3F4F6)
                surface = Color(0xFFF3F4F6)
                surfaceVariant = Color(0xFFE5E7EB)
                surfaceContainer = Color(0xFFFFFFFF)
                surfaceContainerHigh = Color(0xFFECEEF2)
                surfaceContainerHighest = Color(0xFFDEE1E7)
            }
            "LIGHT_TINT" -> {
                background = colorFromHsl(hue, 0.18f, 0.97f)
                surface = colorFromHsl(hue, 0.18f, 0.97f)
                surfaceVariant = colorFromHsl(hue, 0.22f, 0.90f)
                surfaceContainer = colorFromHsl(hue, 0.15f, 0.94f)
                surfaceContainerHigh = colorFromHsl(hue, 0.20f, 0.91f)
                surfaceContainerHighest = colorFromHsl(hue, 0.22f, 0.87f)
            }
            else -> {
                // Default light
                background = Color(0xFFFAF9FD)
                surface = Color(0xFFFAF9FD)
                surfaceVariant = Color(0xFFE7E7ED)
                surfaceContainer = Color(0xFFF3F2F8)
                surfaceContainerHigh = Color(0xFFECEBF2)
                surfaceContainerHighest = Color(0xFFE4E3EC)
            }
        }
    }

    var baseOnSurface = if (isDark) Color(0xFFEDEBF0) else Color(0xFF1A1A1E)
    var baseOnSurfaceVariant = if (isDark) Color(0xFFC7C5D0) else Color(0xFF46464F)

    // Enforce >= 4.5:1 contrast against both surface and background
    baseOnSurface = ensureMinContrast(baseOnSurface, surface, 4.5)
    baseOnSurface = ensureMinContrast(baseOnSurface, background, 4.5)
    baseOnSurfaceVariant = ensureMinContrast(baseOnSurfaceVariant, surface, 4.5)
    baseOnSurfaceVariant = ensureMinContrast(baseOnSurfaceVariant, background, 4.5)

    val finalOnPrimaryContainer = ensureMinContrast(onPrimaryContainer, primaryContainer, 4.5)
    val finalOnSecondaryContainer = ensureMinContrast(onSecondaryContainer, secondaryContainer, 4.5)

    val outline = if (isDark) Color(0xFF908F9A) else Color(0xFF767680)
    val outlineVariant = if (isDark) Color(0xFF46464F) else Color(0xFFC7C5D0)

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = finalOnPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = finalOnSecondaryContainer,
            tertiary = tertiary,
            background = background,
            onBackground = baseOnSurface,
            surface = surface,
            onSurface = baseOnSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = baseOnSurfaceVariant,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            outline = outline,
            outlineVariant = outlineVariant,
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = finalOnPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = finalOnSecondaryContainer,
            tertiary = tertiary,
            background = background,
            onBackground = baseOnSurface,
            surface = surface,
            onSurface = baseOnSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = baseOnSurfaceVariant,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            outline = outline,
            outlineVariant = outlineVariant,
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }
}
