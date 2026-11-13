package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.viewmodel.AlcanciaViewModel

@Composable
fun AjustesInterfazScreen(
    viewModel: AlcanciaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val palette = resolveInicioPalette(settings)
    val isCompact = LocalIsCompactDensity.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg)
    ) {
        AjustesSubScreenTopBar(
            title = "Preferencias de interfaz",
            subtitle = "Respuesta táctil y vibración háptica",
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
            item {
                AjustesPaletteCard(palette = palette) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AjustesIconBadge(
                            icon = Icons.Default.TouchApp,
                            containerColor = palette.s2,
                            iconTint = palette.accent,
                            size = 46.dp,
                            iconSize = 24.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Respuesta táctil y háptica",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.t1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Sensación física y vibración al pulsar botones, guardar movimientos y completar metas.",
                                fontSize = 13.sp,
                                color = palette.t2,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            item {
                AjustesPaletteCard(palette = palette) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AjustesIconBadge(
                                icon = Icons.Default.Vibration,
                                containerColor = palette.s2,
                                iconTint = palette.ahorro,
                                size = 42.dp,
                                iconSize = 22.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Intensidad de vibración",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.t1
                                )
                                Text(
                                    text = "Elige la fuerza del toque táctil",
                                    fontSize = 13.sp,
                                    color = palette.t2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val levels = listOf(
                            Triple("OFF", "Desactivado", "Sin vibración al interactuar"),
                            Triple("LOW", "Bajo", "Pulsación muy suave y casi imperceptible"),
                            Triple("MEDIUM", "Medio", "Vibración nítida y agradable (estándar)"),
                            Triple("HIGH", "Alto", "Confirmación táctil firme y pronunciada")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for ((levelKey, title, desc) in levels) {
                                val isSelected = settings.hapticLevel == levelKey
                                Surface(
                                    onClick = { viewModel.setHapticLevel(levelKey) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) palette.s2 else Color.Transparent,
                                    border = if (isSelected) BorderStroke(1.dp, palette.accent.copy(alpha = 0.45f)) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 52.dp)
                                        .testTag("haptic_level_$levelKey")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.setHapticLevel(levelKey) },
                                            colors = ajustesRadioColors(palette)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = title,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = palette.t1
                                            )
                                            Text(
                                                text = desc,
                                                fontSize = 12.sp,
                                                color = palette.t2
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (settings.hapticLevel != "OFF") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.vibrateDevice() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.s2,
                                    contentColor = palette.t1
                                ),
                                border = BorderStroke(1.dp, palette.s3),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Probar vibración actual", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
