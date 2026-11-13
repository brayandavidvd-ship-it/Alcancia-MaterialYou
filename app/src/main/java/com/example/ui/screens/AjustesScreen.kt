package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppSettingsEntity
import com.example.ui.theme.ColorblindExpenseColor
import com.example.ui.theme.ColorblindIncomeColor
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.viewmodel.AlcanciaViewModel

enum class AjustesSection {
    PERSONALIZACION,
    INTERFAZ,
    SEGURIDAD,
    RECORDATORIOS,
    IA_AHORROS,
    COPIAS_Y_DATOS,
    INFO_APP
}

@Composable
fun resolveInicioPalette(settings: AppSettingsEntity): InicioPalette.Colors {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDark
    }
    val rawPalette = if (isDark) InicioPalette.Dark else InicioPalette.Light
    return if (settings.isColorblindPreset) {
        rawPalette.copy(
            ingreso = ColorblindIncomeColor,
            gasto = ColorblindExpenseColor
        )
    } else {
        rawPalette
    }
}

@Composable
internal fun AjustesSubScreenTopBar(
    title: String,
    palette: InicioPalette.Colors,
    onBack: () -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = palette.s2,
            border = BorderStroke(1.dp, palette.s3),
            modifier = Modifier
                .size(44.dp)
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .testTag("ajustes_back_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    tint = palette.t1,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = palette.t1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = palette.t2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun AjustesPaletteCard(
    palette: InicioPalette.Colors,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    containerColor: Color = palette.s1,
    borderColor: Color = palette.s3,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(content = content)
        }
    }
}

@Composable
internal fun AjustesIconBadge(
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
internal fun ajustesSwitchColors(palette: InicioPalette.Colors): SwitchColors {
    return SwitchDefaults.colors(
        checkedThumbColor = palette.onBrand,
        checkedTrackColor = palette.brand,
        checkedBorderColor = palette.brand,
        uncheckedThumbColor = palette.t2,
        uncheckedTrackColor = palette.s2,
        uncheckedBorderColor = palette.s3
    )
}

@Composable
internal fun ajustesRadioColors(palette: InicioPalette.Colors) = RadioButtonDefaults.colors(
    selectedColor = palette.accent,
    unselectedColor = palette.t2
)

@Composable
internal fun ajustesTextFieldColors(palette: InicioPalette.Colors): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = palette.t1,
        unfocusedTextColor = palette.t1,
        focusedContainerColor = palette.s2,
        unfocusedContainerColor = palette.s2,
        focusedBorderColor = palette.accent,
        unfocusedBorderColor = palette.s3,
        focusedLabelColor = palette.accent,
        unfocusedLabelColor = palette.t2,
        focusedPlaceholderColor = palette.t2.copy(alpha = 0.7f),
        unfocusedPlaceholderColor = palette.t2.copy(alpha = 0.7f),
        cursorColor = palette.accent,
        focusedTrailingIconColor = palette.t1,
        unfocusedTrailingIconColor = palette.t2
    )
}

@Composable
internal fun ajustesChipColors(palette: InicioPalette.Colors): SelectableChipColors {
    return FilterChipDefaults.filterChipColors(
        containerColor = palette.s2,
        labelColor = palette.t2,
        selectedContainerColor = palette.s3,
        selectedLabelColor = palette.t1,
        selectedLeadingIconColor = palette.accent
    )
}

@Composable
fun AjustesScreen(
    viewModel: AlcanciaViewModel,
    modifier: Modifier = Modifier
) {
    var currentSection by remember { mutableStateOf<AjustesSection?>(null) }
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val palette = resolveInicioPalette(settings)
    val isCompact = LocalIsCompactDensity.current

    when (currentSection) {
        AjustesSection.PERSONALIZACION -> {
            AjustesPersonalizacionScreen(
                viewModel = viewModel,
                onBack = { currentSection = null }
            )
        }
        AjustesSection.INTERFAZ -> {
            AjustesInterfazScreen(
                viewModel = viewModel,
                onBack = { currentSection = null }
            )
        }
        AjustesSection.SEGURIDAD -> {
            AjustesSeguridadScreen(
                viewModel = viewModel,
                onBack = { currentSection = null }
            )
        }
        AjustesSection.RECORDATORIOS -> {
            AjustesRecordatoriosScreen(
                viewModel = viewModel,
                onBack = { currentSection = null }
            )
        }
        AjustesSection.IA_AHORROS -> {
            AjustesIaAhorrosScreen(
                viewModel = viewModel,
                onBack = { currentSection = null }
            )
        }
        AjustesSection.COPIAS_Y_DATOS -> {
            AjustesDatosScreen(
                viewModel = viewModel,
                onBack = { currentSection = null }
            )
        }
        AjustesSection.INFO_APP -> {
            AjustesInfoScreen(
                settings = settings,
                onBack = { currentSection = null }
            )
        }
        null -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(palette.bg)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(
                        start = if (isCompact) 14.dp else 20.dp,
                        end = if (isCompact) 14.dp else 20.dp,
                        top = 16.dp,
                        bottom = 124.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AjustesIconBadge(
                                icon = Icons.Default.Settings,
                                containerColor = palette.s2,
                                iconTint = palette.accent,
                                size = 40.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Ajustes",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.t1
                            )
                        }
                    }

                    // 1. Personalización
                    item {
                        AjustesNavigationCard(
                            icon = Icons.Default.Palette,
                            title = "Personalización",
                            subtitle = "Vista previa, tema y modo daltónico",
                            palette = palette,
                            badgeTint = palette.ahorro,
                            testTag = "ajustes_item_personalizacion",
                            onClick = { currentSection = AjustesSection.PERSONALIZACION }
                        )
                    }

                    // 2. Preferencias de interfaz
                    item {
                        AjustesNavigationCard(
                            icon = Icons.Default.Vibration,
                            title = "Preferencias de interfaz",
                            subtitle = "Respuesta táctil y vibración háptica",
                            palette = palette,
                            testTag = "ajustes_item_interfaz",
                            onClick = { currentSection = AjustesSection.INTERFAZ }
                        )
                    }

                    // 3. Seguridad
                    item {
                        AjustesNavigationCard(
                            icon = Icons.Default.Security,
                            title = "Seguridad",
                            subtitle = "Bloqueo por huella digital y PIN de acceso",
                            palette = palette,
                            testTag = "ajustes_item_seguridad",
                            onClick = { currentSection = AjustesSection.SEGURIDAD }
                        )
                    }

                    // 4. Recordatorios
                    item {
                        AjustesNavigationCard(
                            icon = Icons.Default.Notifications,
                            title = "Recordatorios",
                            subtitle = "Notificaciones locales para registrar gastos y metas",
                            palette = palette,
                            testTag = "ajustes_item_recordatorios",
                            onClick = { currentSection = AjustesSection.RECORDATORIOS }
                        )
                    }

                    // 5. IA de ahorros
                    item {
                        AjustesNavigationCard(
                            icon = Icons.Default.AutoAwesome,
                            title = "IA de ahorros",
                            subtitle = "Asistente en la nube (Gemini)",
                            palette = palette,
                            badgeTint = palette.ahorro,
                            testTag = "ajustes_item_ia",
                            onClick = { currentSection = AjustesSection.IA_AHORROS }
                        )
                    }

                    // 6. Copias de seguridad y datos
                    item {
                        AjustesNavigationCard(
                            icon = Icons.Default.SettingsBackupRestore,
                            title = "Copias de seguridad y datos",
                            subtitle = "Moneda, respaldos y limpiar caché",
                            palette = palette,
                            testTag = "ajustes_item_datos",
                            onClick = { currentSection = AjustesSection.COPIAS_Y_DATOS }
                        )
                    }

                    // 7. Info de la app
                    item {
                        AjustesNavigationCard(
                            icon = Icons.Default.Info,
                            title = "Info de la app",
                            subtitle = "Privacidad, versión y créditos",
                            palette = palette,
                            testTag = "ajustes_item_info",
                            onClick = { currentSection = AjustesSection.INFO_APP }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AjustesNavigationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    palette: InicioPalette.Colors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeTint: Color = palette.accent,
    testTag: String? = null
) {
    AjustesPaletteCard(
        palette = palette,
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 76.dp)
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                AjustesIconBadge(
                    icon = icon,
                    containerColor = palette.s2,
                    iconTint = badgeTint,
                    size = 46.dp,
                    iconSize = 22.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.t1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = palette.t2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = palette.t2,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
