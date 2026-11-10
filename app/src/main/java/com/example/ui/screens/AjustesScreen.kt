package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.AlcanciaCard
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.viewmodel.AlcanciaViewModel

enum class AjustesSection {
    PERSONALIZACION,
    INTERFAZ,
    SEGURIDAD,
    RECORDATORIOS,
    IA_LOCAL,
    COPIAS_Y_DATOS,
    INFO_APP
}

@Composable
fun AjustesScreen(
    viewModel: AlcanciaViewModel,
    modifier: Modifier = Modifier
) {
    var currentSection by remember { mutableStateOf<AjustesSection?>(null) }
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
        AjustesSection.IA_LOCAL -> {
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
                onBack = { currentSection = null }
            )
        }
        null -> {
            // Main Ajustes screen showing 6 clean rounded cards with rows
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = if (isCompact) 14.dp else 20.dp,
                    end = if (isCompact) 14.dp else 20.dp,
                    top = 16.dp,
                    bottom = 124.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp)
                    )
                }

                // Row Card 1: Personalización
                item {
                    AjustesNavigationCard(
                        icon = Icons.Default.Palette,
                        title = "Personalización",
                        subtitle = "Temas, colores, esquemas, tarjetas, fuentes y categorías",
                        onClick = { currentSection = AjustesSection.PERSONALIZACION }
                    )
                }

                // Row Card 2: Preferencias de interfaz
                item {
                    AjustesNavigationCard(
                        icon = Icons.Default.Vibration,
                        title = "Preferencias de interfaz",
                        subtitle = "Respuesta táctil y vibración háptica",
                        onClick = { currentSection = AjustesSection.INTERFAZ }
                    )
                }

                // Row Card 3: Seguridad
                item {
                    AjustesNavigationCard(
                        icon = Icons.Default.Security,
                        title = "Seguridad",
                        subtitle = "Bloqueo por huella digital y PIN de acceso",
                        onClick = { currentSection = AjustesSection.SEGURIDAD }
                    )
                }

                // Row Card 4: Recordatorios
                item {
                    AjustesNavigationCard(
                        icon = Icons.Default.Notifications,
                        title = "Recordatorios",
                        subtitle = "Notificaciones locales para registrar gastos y metas",
                        onClick = { currentSection = AjustesSection.RECORDATORIOS }
                    )
                }

                // Row Card 5: IA de ahorros
                item {
                    AjustesNavigationCard(
                        icon = Icons.Default.AutoAwesome,
                        title = "IA de ahorros",
                        subtitle = "Asistente en la nube e IA local",
                        onClick = { currentSection = AjustesSection.IA_LOCAL }
                    )
                }

                // Row Card 6: Copias de seguridad y datos
                item {
                    AjustesNavigationCard(
                        icon = Icons.Default.SettingsBackupRestore,
                        title = "Copias de seguridad y datos",
                        subtitle = "Moneda, exportar CSV, respaldos y limpiar caché",
                        onClick = { currentSection = AjustesSection.COPIAS_Y_DATOS }
                    )
                }

                // Row Card 7: Info de la app
                item {
                    AjustesNavigationCard(
                        icon = Icons.Default.Info,
                        title = "Info de la app",
                        subtitle = "Privacidad, versión y créditos",
                        onClick = { currentSection = AjustesSection.INFO_APP }
                    )
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlcanciaCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
