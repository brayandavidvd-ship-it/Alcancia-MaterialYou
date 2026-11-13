package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.BackupSummary
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.viewmodel.AlcanciaViewModel
import kotlinx.coroutines.launch

@Composable
fun AjustesDatosScreen(
    viewModel: AlcanciaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val palette = resolveInicioPalette(settings)
    val monthlyHistory by viewModel.monthlyHistoryState.collectAsStateWithLifecycle()
    val lastBackupDate by viewModel.lastBackupDate.collectAsStateWithLifecycle()

    val isCompact = LocalIsCompactDensity.current

    var pendingRestoreSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var restoreMode by remember { mutableStateOf("COMBINE") }
    var validationErrorMessage by remember { mutableStateOf<String?>(null) }
    var cacheSizeBytes by remember { mutableStateOf(viewModel.getAppCacheSizeBytes(context)) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.createBackupJson()
                viewModel.saveBackupToUri(uri, json) { success ->
                    scope.launch {
                        if (success) snackbarHostState.showSnackbar("Respaldo guardado exitosamente")
                        else snackbarHostState.showSnackbar("Error al guardar el respaldo")
                    }
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.validateBackup(uri) { result ->
                result.onSuccess { bSummary ->
                    pendingRestoreSummary = bSummary
                    restoreMode = "COMBINE"
                }.onFailure { err ->
                    validationErrorMessage = err.message ?: "El archivo seleccionado no es un respaldo válido de Alcancía."
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg),
        containerColor = palette.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 92.dp)
            )
        },
        topBar = {
            AjustesSubScreenTopBar(
                title = "Copias de seguridad y datos",
                subtitle = "Moneda, respaldos y caché",
                palette = palette,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = if (isCompact) 14.dp else 20.dp,
                end = if (isCompact) 14.dp else 20.dp,
                top = 8.dp,
                bottom = 124.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Respaldo de la app
            item {
                AjustesPaletteCard(palette = palette) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AjustesIconBadge(
                                icon = Icons.Default.SettingsBackupRestore,
                                containerColor = palette.s2,
                                iconTint = palette.accent,
                                size = 44.dp,
                                iconSize = 22.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Respaldo de la app",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.t1
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Guarda todo en un archivo dentro de tu celular y restáuralo cuando quieras.",
                            fontSize = 13.sp,
                            color = palette.t2,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                                    createDocumentLauncher.launch("alcancia-respaldo-$dateStr.json")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.brand,
                                    contentColor = palette.onBrand
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar respaldo", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = palette.s2,
                                    contentColor = palette.t1
                                ),
                                border = BorderStroke(1.dp, palette.s3),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restaurar respaldo", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val backupDateLabel = if (lastBackupDate != null) {
                            "Último respaldo: $lastBackupDate"
                        } else {
                            "Aún no has hecho un respaldo"
                        }

                        Text(
                            text = backupDateLabel,
                            fontSize = 12.sp,
                            color = palette.t2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Moneda principal
            item {
                AjustesPaletteCard(palette = palette) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AjustesIconBadge(
                                icon = Icons.Default.MonetizationOn,
                                containerColor = palette.s2,
                                iconTint = palette.ahorro,
                                size = 44.dp,
                                iconSize = 22.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Moneda principal",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.t1
                                )
                                Text(
                                    text = "Símbolo actual: ${settings.currencySymbol} (${settings.currencyCode})",
                                    fontSize = 13.sp,
                                    color = palette.t2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val chipColors = ajustesChipColors(palette)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val currencies = listOf(
                                Triple("MXN", "$", "Peso (MXN)"),
                                Triple("USD", "$", "Dólar (USD)"),
                                Triple("EUR", "€", "Euro (EUR)"),
                                Triple("COP", "$", "Peso (COP)"),
                                Triple("ARS", "$", "Peso (ARS)"),
                                Triple("CLP", "$", "Peso (CLP)"),
                                Triple("PEN", "S/", "Sol (PEN)")
                            )
                            for ((code, sym, label) in currencies) {
                                val isSelected = settings.currencyCode == code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setCurrency(code, sym) },
                                    label = { Text(label, maxLines = 1, softWrap = false) },
                                    colors = chipColors,
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = palette.s3,
                                        selectedBorderColor = palette.accent
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Limpiar caché de la app
            item {
                AjustesPaletteCard(palette = palette) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AjustesIconBadge(
                                icon = Icons.Default.CleaningServices,
                                containerColor = palette.s2,
                                iconTint = palette.accent,
                                size = 44.dp,
                                iconSize = 22.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Limpiar caché de la app",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.t1
                                )
                                Text(
                                    text = "Almacenamiento temporal usado: ${formatBytes(cacheSizeBytes)}",
                                    fontSize = 13.sp,
                                    color = palette.t2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Libera espacio en tu dispositivo eliminando archivos temporales. Tus movimientos y metas de ahorro no se borrarán.",
                            fontSize = 13.sp,
                            color = palette.t2,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.clearAppCache(context) { freedBytes ->
                                    cacheSizeBytes = viewModel.getAppCacheSizeBytes(context)
                                    scope.launch {
                                        val freedStr = formatBytes(freedBytes)
                                        snackbarHostState.showSnackbar("Caché limpiada: se liberaron $freedStr")
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = palette.s2,
                                contentColor = palette.t1
                            ),
                            border = BorderStroke(1.dp, palette.s3),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Limpiar memoria caché", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Resumen y Estadísticas
            item {
                Text(
                    text = "RESUMEN Y ESTADÍSTICAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                )
            }

            item {
                AjustesPaletteCard(palette = palette) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Comparativa mensual (Últimos 6 meses)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.t1
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val maxVal = remember(monthlyHistory) {
                            monthlyHistory.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(100.0) ?: 100.0
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            for (monthData in monthlyHistory) {
                                val incH = ((monthData.income / maxVal) * 90).coerceIn(4.0, 90.0).dp
                                val expH = ((monthData.expense / maxVal) * 90).coerceIn(4.0, 90.0).dp

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(10.dp)
                                                .height(incH)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(palette.ingreso)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(10.dp)
                                                .height(expH)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(palette.gasto)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = monthData.monthLabel,
                                        fontSize = 11.sp,
                                        color = palette.t2,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(palette.ingreso))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ingresos", fontSize = 12.sp, color = palette.t2)

                            Spacer(modifier = Modifier.width(18.dp))

                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(palette.gasto))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gastos", fontSize = 12.sp, color = palette.t2)
                        }
                    }
                }
            }
        }
    }

    if (validationErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { validationErrorMessage = null },
            containerColor = palette.s1,
            titleContentColor = palette.t1,
            textContentColor = palette.t2,
            icon = { Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = palette.gasto) },
            title = { Text("Respaldo no válido", fontWeight = FontWeight.Bold, color = palette.t1) },
            text = { Text(validationErrorMessage!!, color = palette.t2) },
            confirmButton = {
                Button(
                    onClick = { validationErrorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.brand, contentColor = palette.onBrand)
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (pendingRestoreSummary != null) {
        val bSummary = pendingRestoreSummary!!
        val movStr = if (bSummary.movementCount == 1) "1 movimiento" else "${bSummary.movementCount} movimientos"
        val listStr = if (bSummary.listCount == 1) "1 lista de compras" else "${bSummary.listCount} listas de compras"
        val goalStr = if (bSummary.goalCount == 1) "1 meta" else "${bSummary.goalCount} metas"

        AlertDialog(
            onDismissRequest = { pendingRestoreSummary = null },
            containerColor = palette.s1,
            titleContentColor = palette.t1,
            textContentColor = palette.t2,
            title = { Text("Restaurar respaldo de Alcancía", fontWeight = FontWeight.Bold, color = palette.t1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "El archivo contiene: $movStr, $listStr y $goalStr.",
                        fontSize = 14.sp,
                        color = palette.t2
                    )

                    Text(
                        text = "Selecciona el método de restauración:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )

                    Surface(
                        onClick = { restoreMode = "COMBINE" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (restoreMode == "COMBINE") palette.s2 else Color.Transparent,
                        border = if (restoreMode == "COMBINE") BorderStroke(1.dp, palette.accent.copy(alpha = 0.45f)) else null
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = restoreMode == "COMBINE",
                                onClick = { restoreMode = "COMBINE" },
                                colors = ajustesRadioColors(palette)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Combinar (recomendado)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.t1)
                                Text("Agrega los datos faltantes sin borrar los actuales.", fontSize = 12.sp, color = palette.t2)
                            }
                        }
                    }

                    Surface(
                        onClick = { restoreMode = "REPLACE" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (restoreMode == "REPLACE") palette.gasto.copy(alpha = 0.14f) else Color.Transparent,
                        border = if (restoreMode == "REPLACE") BorderStroke(1.dp, palette.gasto.copy(alpha = 0.45f)) else null
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = restoreMode == "REPLACE",
                                onClick = { restoreMode = "REPLACE" },
                                colors = ajustesRadioColors(palette)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Reemplazar todo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.gasto)
                                Text("Borra los datos actuales y aplica únicamente el respaldo.", fontSize = 12.sp, color = palette.t2)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val data = bSummary.backupData
                        val isReplace = restoreMode == "REPLACE"
                        viewModel.applyRestore(data, isReplace) { success ->
                            scope.launch {
                                pendingRestoreSummary = null
                                if (success) snackbarHostState.showSnackbar("Datos restaurados correctamente")
                                else snackbarHostState.showSnackbar("Error al restaurar los datos")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (restoreMode == "REPLACE") palette.gasto else palette.brand,
                        contentColor = palette.onBrand
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (restoreMode == "REPLACE") "Reemplazar y restaurar" else "Combinar y restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreSummary = null }) {
                    Text("Cancelar", color = palette.t2)
                }
            }
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
    } else {
        String.format(java.util.Locale.getDefault(), "%.0f KB", kb)
    }
}
