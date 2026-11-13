package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.ui.theme.LocalIsCompactDensity

@Composable
fun AjustesInfoScreen(
    settings: AppSettingsEntity,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val palette = resolveInicioPalette(settings)
    val isCompact = LocalIsCompactDensity.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg)
    ) {
        AjustesSubScreenTopBar(
            title = "Info de la app",
            subtitle = "Privacidad, versión y créditos",
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
                InfoTrustCard(
                    icon = Icons.Default.Info,
                    title = "Almacenamiento de datos",
                    description = "Tus datos se guardan en tu celular. Si activas la IA en la nube, se envían a Google únicamente los datos necesarios para responder.",
                    palette = palette
                )
            }

            item {
                InfoTrustCard(
                    icon = Icons.Default.Block,
                    title = "Sin anuncios ni rastreadores",
                    description = "Esta aplicación está 100% libre de publicidad, rastreadores y servicios de analítica en segundo plano. Cero consumo innecesario de batería o datos móviles.",
                    palette = palette
                )
            }

            item {
                InfoTrustCard(
                    icon = Icons.Default.WifiOff,
                    title = "Tus datos son tuyos: respáldalos cuando quieras",
                    description = "Puedes crear respaldos completos de tus cuentas en formato JSON en cualquier momento.",
                    palette = palette
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Creado por PixelX_O y Google Studio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.t1.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Alcancía / Versión 1.3.0",
                        fontSize = 12.sp,
                        color = palette.t2.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoTrustCard(
    icon: ImageVector,
    title: String,
    description: String,
    palette: InicioPalette.Colors,
    modifier: Modifier = Modifier
) {
    AjustesPaletteCard(palette = palette, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AjustesIconBadge(
                    icon = icon,
                    containerColor = palette.s2,
                    iconTint = palette.accent,
                    size = 44.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.t1,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = palette.t2,
                lineHeight = 19.sp
            )
        }
    }
}
