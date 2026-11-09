package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.BackupSummary
import com.example.ui.components.AlcanciaCard
import com.example.ui.components.CategoryProgressItem
import com.example.ui.components.CurrencyUtils
import com.example.ui.theme.LocalExpenseColor
import com.example.ui.theme.LocalIncomeColor
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.theme.LocalNumberFontFamily
import com.example.ui.theme.LocalSavingsColor
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
    val summary by viewModel.financialSummaryState.collectAsStateWithLifecycle()
    val monthlyHistory by viewModel.monthlyHistoryState.collectAsStateWithLifecycle()
    val lastBackupDate by viewModel.lastBackupDate.collectAsStateWithLifecycle()

    val incomeColor = LocalIncomeColor.current
    val expenseColor = LocalExpenseColor.current
    val savingsColor = LocalSavingsColor.current
    val numberFont = LocalNumberFontFamily.current
    val isCompact = LocalIsCompactDensity.current

    // Backup & Restore states
    var pendingRestoreSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var restoreMode by remember { mutableStateOf("COMBINE") } // "COMBINE" vs "REPLACE"
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
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Copias de seguridad y datos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // RECUADRO 1: "Exportar a Excel"
            item {
                AlcanciaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Exportar a Excel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Exporta tus movimientos en CSV para abrirlos en Excel o Sheets. Solo sirve para consultar, no para restaurar la app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.exportCsv(context) { shareIntent ->
                                    context.startActivity(shareIntent)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar movimientos (CSV)", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // RECUADRO 2: "Respaldo de la app"
            item {
                AlcanciaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SettingsBackupRestore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Respaldo de la app",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Guarda todo en un archivo dentro de tu celular y restáuralo cuando quieras.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
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
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar respaldo", fontWeight = FontWeight.SemiBold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // RECUADRO 3: "Moneda principal"
            item {
                AlcanciaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Moneda principal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Símbolo actual: ${settings.currencySymbol} (${settings.currencyCode})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

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
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // RECUADRO 4: "Limpiar caché de la app"
            item {
                AlcanciaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CleaningServices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Limpiar caché de la app",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Almacenamiento temporal usado: ${formatBytes(cacheSizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Libera espacio en tu dispositivo eliminando archivos temporales y reportes exportados antiguos. Tus movimientos, presupuestos y metas de ahorro no se borrarán.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
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
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Limpiar memoria caché", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // SECCIÓN: "Resumen y Estadísticas"
            item {
                Text(
                    text = "Resumen y Estadísticas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Gráfica de distribución por categoría
            item {
                AlcanciaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Distribución por categoría (Mes actual)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (summary.categoryBreakdown.isEmpty()) {
                            Text(
                                text = "Sin datos en el mes seleccionado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            for (catSpent in summary.categoryBreakdown) {
                                CategoryProgressItem(
                                    item = catSpent,
                                    currencySymbol = settings.currencySymbol
                                )
                            }
                        }
                    }
                }
            }

            // Comparativa de los últimos 6 meses
            item {
                AlcanciaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Comparativa mensual (Últimos 6 meses)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
                                                .background(incomeColor)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(10.dp)
                                                .height(expH)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(expenseColor)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = monthData.monthLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(incomeColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ingresos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.width(18.dp))

                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(expenseColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gastos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // Modal Error de validación de respaldo
    if (validationErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { validationErrorMessage = null },
            icon = { Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Respaldo no válido") },
            text = { Text(validationErrorMessage!!) },
            confirmButton = {
                Button(onClick = { validationErrorMessage = null }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Modal Confirmación de Restauración
    if (pendingRestoreSummary != null) {
        val bSummary = pendingRestoreSummary!!
        val movStr = if (bSummary.movementCount == 1) "1 movimiento" else "${bSummary.movementCount} movimientos"
        val listStr = if (bSummary.listCount == 1) "1 lista de compras" else "${bSummary.listCount} listas de compras"
        val catStr = if (bSummary.categoryCount == 1) "1 categoría" else "${bSummary.categoryCount} categorías"
        val goalStr = if (bSummary.goalCount == 1) "1 meta" else "${bSummary.goalCount} metas"

        AlertDialog(
            onDismissRequest = { pendingRestoreSummary = null },
            title = { Text("Restaurar respaldo de Alcancía") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "El archivo contiene: $movStr, $listStr, $catStr y $goalStr.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Selecciona el método de restauración:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Opción Combinar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (restoreMode == "COMBINE") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                            .padding(8.dp)
                    ) {
                        RadioButton(
                            selected = restoreMode == "COMBINE",
                            onClick = { restoreMode = "COMBINE" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Combinar (recomendado)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Agrega los datos faltantes sin borrar los actuales.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Opción Reemplazar todo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (restoreMode == "REPLACE") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else Color.Transparent)
                            .padding(8.dp)
                    ) {
                        RadioButton(
                            selected = restoreMode == "REPLACE",
                            onClick = { restoreMode = "REPLACE" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Reemplazar todo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            Text("Borra los datos actuales y aplica únicamente el respaldo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (restoreMode == "REPLACE") "Reemplazar y restaurar" else "Combinar y restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreSummary = null }) {
                    Text("Cancelar")
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
