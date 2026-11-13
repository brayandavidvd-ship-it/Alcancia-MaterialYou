package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.theme.LocalNumberFontFamily
import com.example.ui.viewmodel.AlcanciaViewModel

@Composable
fun AjustesPersonalizacionScreen(
    viewModel: AlcanciaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val palette = resolveInicioPalette(settings)
    val numberFont = LocalNumberFontFamily.current
    val isCompact = LocalIsCompactDensity.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg)
    ) {
        AjustesSubScreenTopBar(
            title = "Personalización",
            subtitle = "Vista previa, tema y accesibilidad",
            palette = palette,
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (isCompact) 14.dp else 20.dp,
                end = if (isCompact) 14.dp else 20.dp,
                top = 8.dp,
                bottom = 124.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. VISTA PREVIA EN VIVO
            item {
                Text(
                    text = "VISTA PREVIA EN VIVO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                AjustesPaletteCard(palette = palette) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alcancía Demo",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.t1
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.s3
                            ) {
                                Text(
                                    text = settings.currencyCode,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${settings.currencySymbol}14,500.00",
                            style = TextStyle(
                                fontFamily = numberFont,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.t1
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PreviewMetricPill(
                                icon = Icons.Default.ArrowUpward,
                                label = "+${settings.currencySymbol}22,000",
                                color = palette.ingreso,
                                surfaceColor = palette.s2,
                                numberFont = numberFont,
                                modifier = Modifier.weight(1f)
                            )
                            PreviewMetricPill(
                                icon = Icons.Default.ArrowDownward,
                                label = "-${settings.currencySymbol}7,500",
                                color = palette.gasto,
                                surfaceColor = palette.s2,
                                numberFont = numberFont,
                                modifier = Modifier.weight(1f)
                            )
                            PreviewMetricPill(
                                icon = Icons.Default.Savings,
                                label = "${settings.currencySymbol}5,000",
                                color = palette.ahorro,
                                surfaceColor = palette.s2,
                                numberFont = numberFont,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. MODO DE TEMA (Sistema / Claro / Oscuro)
            item {
                AjustesPaletteCard(palette = palette) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Modo de tema",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.t1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Elige la apariencia de toda la aplicación",
                            fontSize = 13.sp,
                            color = palette.t2
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val modes = listOf(
                                Triple("SYSTEM", "Sistema", Icons.Default.SettingsBrightness),
                                Triple("LIGHT", "Claro", Icons.Default.LightMode),
                                Triple("DARK", "Oscuro", Icons.Default.DarkMode)
                            )
                            for ((modeKey, modeTitle, modeIcon) in modes) {
                                val isSelected = settings.themeMode == modeKey
                                Surface(
                                    onClick = { viewModel.setThemeMode(modeKey) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) palette.s3 else palette.s2,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) palette.accent else palette.s3
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 56.dp)
                                        .testTag("theme_mode_$modeKey")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = modeIcon,
                                            contentDescription = null,
                                            tint = if (isSelected) palette.accent else palette.t2,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = modeTitle,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) palette.t1 else palette.t2,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. MODO DALTÓNICO ACCESIBLE
            item {
                AjustesPaletteCard(palette = palette) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AjustesIconBadge(
                                    icon = Icons.Default.Visibility,
                                    containerColor = palette.s2,
                                    iconTint = palette.ahorro,
                                    size = 42.dp,
                                    iconSize = 20.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Modo daltónico accesible",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.t1
                                    )
                                    Text(
                                        text = "Usa Azul para Ingresos y Naranja para Gastos",
                                        fontSize = 13.sp,
                                        color = palette.t2
                                    )
                                }
                            }
                            Switch(
                                checked = settings.isColorblindPreset,
                                onCheckedChange = { viewModel.setColorblindPreset(it) },
                                colors = ajustesSwitchColors(palette),
                                modifier = Modifier.testTag("switch_colorblind")
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.s2,
                            border = BorderStroke(1.dp, palette.s3),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                FinancialColorDot("Ingreso", palette.ingreso, palette.t2)
                                FinancialColorDot("Gasto", palette.gasto, palette.t2)
                                FinancialColorDot("Ahorro", palette.ahorro, palette.t2)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewMetricPill(
    icon: ImageVector,
    label: String,
    color: Color,
    surfaceColor: Color,
    numberFont: androidx.compose.ui.text.font.FontFamily,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = numberFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FinancialColorDot(
    label: String,
    color: Color,
    labelColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor
        )
    }
}
