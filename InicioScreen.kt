package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.AddTransactionBottomSheet
import com.example.ui.components.AlcanciaCard
import com.example.ui.components.CategoryProgressItem
import com.example.ui.components.CurrencyUtils
import com.example.ui.theme.LocalExpenseColor
import com.example.ui.theme.LocalIncomeColor
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.theme.LocalNumberFontFamily
import com.example.ui.theme.LocalSavingsColor
import com.example.ui.theme.NeutralUncategorizedColor
import com.example.ui.theme.getCategoryColorById
import com.example.ui.viewmodel.AlcanciaViewModel
import com.example.ui.viewmodel.AppTab
import java.text.SimpleDateFormat
import java.util.Locale

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun InicioScreen(
    viewModel: AlcanciaViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val summary by viewModel.financialSummaryState.collectAsStateWithLifecycle()
    val categories by viewModel.categoriesState.collectAsStateWithLifecycle()
    val goals by viewModel.goalsState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val selectedCalendar by viewModel.selectedCalendar.collectAsStateWithLifecycle()
    val isGridView by viewModel.isMovementsGridView.collectAsStateWithLifecycle()

    val incomeColor = LocalIncomeColor.current
    val expenseColor = LocalExpenseColor.current
    val savingsColor = LocalSavingsColor.current
    val numberFont = LocalNumberFontFamily.current
    val isCompact = LocalIsCompactDensity.current

    // Dialog & sheet states
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var isEditFutureMode by remember { mutableStateOf(false) }

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var contextMenuTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var repeatedEditChoiceTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var repeatedDeleteChoiceTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val monthYearText = remember(selectedCalendar) {
        val format = SimpleDateFormat("MMMM yyyy", Locale("es", "MX"))
        format.format(selectedCalendar.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale("es", "MX")) else it.toString()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTransaction = null
                    isEditFutureMode = false
                    showAddSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("add_movement_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar movimiento",
                    modifier = Modifier.size(28.dp)
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
                top = 16.dp,
                bottom = 96.dp // Generous bottom space so the FAB never covers list items
            ),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
        ) {
            // Screen Title: "Alcancía" - solo texto, grande, sin icono
            item(key = "app_title_header") {
                Text(
                    text = "Alcancía",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp)
                )
            }

            // Month Navigation Selector
            item(key = "month_selector") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.previousMonth()
                            },
                            modifier = Modifier.testTag("prev_month_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Mes anterior",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = monthYearText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.setCurrentMonth()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.nextMonth()
                            },
                            modifier = Modifier.testTag("next_month_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Mes siguiente",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Big Balance Card with "Ahorrado este mes" label
            item(key = "balance_card") {
                AlcanciaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("balance_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SALDO DISPONIBLE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val balanceText = CurrencyUtils.formatAmount(summary.balance, settings.currencySymbol)
                        val balanceColor = if (summary.balance >= 0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            expenseColor
                        }

                        Text(
                            text = balanceText,
                            style = MaterialTheme.typography.displayLarge.copy(fontFamily = numberFont),
                            fontWeight = FontWeight.ExtraBold,
                            color = balanceColor,
                            textAlign = TextAlign.Center
                        )

                        if (summary.totalSavingsMonth > 0.0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = savingsColor.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "🐷", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Ahorrado este mes: ${CurrencyUtils.formatAmount(summary.totalSavingsMonth, settings.currencySymbol)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = savingsColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Income and Expense Dual Cards
            item(key = "income_expense_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 14.dp)
                ) {
                    // Ingresos Card
                    AlcanciaCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("income_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(incomeColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = incomeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ingresos",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+${CurrencyUtils.formatAmount(summary.totalIncome, settings.currencySymbol)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = numberFont),
                                fontWeight = FontWeight.Bold,
                                color = incomeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Gastos Card
                    AlcanciaCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("expense_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(expenseColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = expenseColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Gastos",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "-${CurrencyUtils.formatAmount(summary.totalExpense, settings.currencySymbol)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = numberFont),
                                fontWeight = FontWeight.Bold,
                                color = expenseColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Gastos por categoría (Resumen compacto)
            item(key = "category_breakdown_section") {
                AlcanciaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gastos por categoría",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val catCount = summary.categoryBreakdown.size
                            Text(
                                text = if (catCount == 1) "1 categoría" else "$catCount categorías",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (summary.categoryBreakdown.isEmpty()) {
                            Text(
                                text = "Sin gastos registrados en este mes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
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

            // Movimientos Header with List / 3-Column Grid Switcher
            item(key = "recent_movements_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Movimientos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { viewModel.toggleMovementsViewMode() },
                        modifier = Modifier.testTag("toggle_view_mode_btn")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Ver como lista" else "Ver como cuadrícula",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Empty state
            if (summary.groupedTransactions.isEmpty()) {
                item(key = "empty_movements") {
                    AlcanciaCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Tu alcancía está vacía este mes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Toca el botón (+) para registrar tu primer gasto, ingreso o ahorro.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                for (group in summary.groupedTransactions) {
                    item(key = "group_header_${group.dateMillis}") {
                        Text(
                            text = group.dayLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }

                    if (isGridView) {
                        items(
                            items = group.transactions.chunked(3),
                            key = { chunk -> "grid_${chunk.first().id}" }
                        ) { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (tx in chunk) {
                                    GridTransactionCard(
                                        transaction = tx,
                                        currencySymbol = settings.currencySymbol,
                                        categories = categories,
                                        modifier = Modifier.weight(1f),
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            contextMenuTransaction = tx
                                        }
                                    )
                                }
                                for (i in 0 until (3 - chunk.size)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(
                            items = group.transactions,
                            key = { it.id }
                        ) { tx ->
                            ListTransactionCard(
                                transaction = tx,
                                currencySymbol = settings.currencySymbol,
                                categories = categories,
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    contextMenuTransaction = tx
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Context Menu (Edit / Delete) on Long Press
    if (contextMenuTransaction != null) {
        val tx = contextMenuTransaction!!
        val isIncome = tx.type == "INCOME"
        val isSavings = tx.type.startsWith("SAVING")
        val isRepeated = tx.repeatInterval != "NONE" || !tx.repeatGroupId.isNullOrBlank()

        val neutralTitle = when {
            isIncome -> "Ingreso"
            isSavings -> if (tx.type == "SAVING_WITHDRAW") "Retiro de ahorro" else "Aporte a meta"
            else -> "Gasto"
        }
        val displayTitle = if (tx.categoryName.isNotBlank()) tx.categoryName else (if (isSavings) "🐷 ${tx.goalName}" else neutralTitle)

        AlertDialog(
            onDismissRequest = { contextMenuTransaction = null },
            title = {
                Text(
                    text = "$displayTitle • ${CurrencyUtils.formatAmount(tx.amount, settings.currencySymbol)}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val timeInfo = if (!tx.timeString.isNullOrBlank()) "Hora: ${tx.timeString}" else ""
                val repeatInfo = when (tx.repeatInterval) {
                    "DAILY" -> "Repetición: Diario"
                    "WEEKLY" -> "Repetición: Semanal"
                    "MONTHLY" -> "Repetición: Mensual"
                    "CUSTOM" -> "Repetición: Días ${tx.customRepeatDays}"
                    else -> ""
                }
                val details = listOfNotNull(
                    tx.note.ifBlank { null },
                    timeInfo.ifBlank { null },
                    repeatInfo.ifBlank { null }
                ).joinToString("\n")

                Text(
                    text = details.ifBlank { "Selecciona una acción para este movimiento." },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val target = tx
                            contextMenuTransaction = null
                            if (isRepeated) {
                                repeatedEditChoiceTransaction = target
                            } else {
                                editingTransaction = target
                                isEditFutureMode = false
                                showAddSheet = true
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar")
                    }

                    Button(
                        onClick = {
                            val target = tx
                            contextMenuTransaction = null
                            if (isRepeated) {
                                repeatedDeleteChoiceTransaction = target
                            } else {
                                transactionToDelete = target
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = expenseColor)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Borrar", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { contextMenuTransaction = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Repeated Movement Edit Choice: "Solo este" o "Este y los siguientes"
    if (repeatedEditChoiceTransaction != null) {
        val target = repeatedEditChoiceTransaction!!
        AlertDialog(
            onDismissRequest = { repeatedEditChoiceTransaction = null },
            title = { Text("¿Editar movimiento repetido?") },
            text = {
                Text("¿Deseas aplicar los cambios solo a este movimiento o también a las repeticiones futuras?")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            editingTransaction = target
                            isEditFutureMode = false
                            showAddSheet = true
                            repeatedEditChoiceTransaction = null
                        }
                    ) {
                        Text("Solo este")
                    }
                    Button(
                        onClick = {
                            editingTransaction = target
                            isEditFutureMode = true
                            showAddSheet = true
                            repeatedEditChoiceTransaction = null
                        }
                    ) {
                        Text("Este y los siguientes")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { repeatedEditChoiceTransaction = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Repeated Movement Delete Choice: "Solo este" o "Este y los siguientes"
    if (repeatedDeleteChoiceTransaction != null) {
        val target = repeatedDeleteChoiceTransaction!!
        AlertDialog(
            onDismissRequest = { repeatedDeleteChoiceTransaction = null },
            title = { Text("¿Borrar movimiento repetido?") },
            text = {
                Text("Este movimiento forma parte de una repetición. ¿Deseas borrar solo esta ocurrencia o también todas las siguientes?")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.deleteTransaction(target)
                            repeatedDeleteChoiceTransaction = null
                        }
                    ) {
                        Text("Solo este")
                    }
                    Button(
                        onClick = {
                            viewModel.deleteTransactionAndFuture(target)
                            repeatedDeleteChoiceTransaction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = expenseColor)
                    ) {
                        Text("Este y los siguientes", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { repeatedDeleteChoiceTransaction = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Confirmation Dialog for Non-Repeated Delete
    if (transactionToDelete != null) {
        val tx = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("¿Borrar este movimiento?") },
            text = {
                val amountStr = CurrencyUtils.formatAmount(tx.amount, settings.currencySymbol)
                val catStr = if (tx.categoryName.isNotBlank()) tx.categoryName else (if (tx.type == "INCOME") "Ingreso" else "Gasto")
                Text("Se eliminará permanentemente $catStr por $amountStr.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = expenseColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Borrar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Add or Edit Bottom Sheet
    if (showAddSheet) {
        AddTransactionBottomSheet(
            categories = categories,
            goals = goals,
            currencySymbol = settings.currencySymbol,
            initialTransaction = editingTransaction,
            onDismiss = {
                showAddSheet = false
                editingTransaction = null
                isEditFutureMode = false
            },
            onCreateGoal = {
                showAddSheet = false
                viewModel.selectTab(AppTab.METAS)
            },
            onSave = { type, amount, category, note, dateMillis, repeat, goal, timeString, customRepeatDays ->
                val editing = editingTransaction
                if (editing != null) {
                    if (isEditFutureMode) {
                        viewModel.updateTransactionAndFuture(
                            transaction = editing,
                            amount = amount,
                            category = category,
                            note = note,
                            timeString = timeString
                        )
                    } else {
                        viewModel.updateTransaction(
                            id = editing.id,
                            type = type,
                            amount = amount,
                            category = category,
                            note = note,
                            dateMillis = dateMillis,
                            repeatInterval = repeat,
                            goal = goal,
                            timeString = timeString,
                            customRepeatDays = customRepeatDays,
                            repeatGroupId = editing.repeatGroupId
                        )
                    }
                } else {
                    viewModel.addTransaction(
                        type = type,
                        amount = amount,
                        category = category,
                        note = note,
                        dateMillis = dateMillis,
                        repeatInterval = repeat,
                        goal = goal,
                        timeString = timeString,
                        customRepeatDays = customRepeatDays
                    )
                }
                showAddSheet = false
                editingTransaction = null
                isEditFutureMode = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListTransactionCard(
    transaction: TransactionEntity,
    currencySymbol: String,
    categories: List<CategoryEntity>,
    modifier: Modifier = Modifier,
    onLongPress: () -> Unit
) {
    val incomeColor = LocalIncomeColor.current
    val expenseColor = LocalExpenseColor.current
    val savingsColor = LocalSavingsColor.current
    val numberFont = LocalNumberFontFamily.current

    val isIncome = transaction.type == "INCOME"
    val isSaving = transaction.type.startsWith("SAVING")
    val isWithdraw = transaction.type == "SAVING_WITHDRAW"

    val displayEmoji = when {
        isSaving -> "🐷"
        transaction.categoryEmoji.isNotBlank() -> transaction.categoryEmoji
        isIncome -> "💰"
        else -> "💸"
    }

    val displayTitle = when {
        isSaving -> if (transaction.goalName.isNotBlank()) transaction.goalName else (if (isWithdraw) "Retiro de meta" else "Aporte a meta")
        transaction.categoryName.isNotBlank() -> transaction.categoryName
        isIncome -> "Ingreso"
        else -> "Gasto"
    }

    val amountColor = when {
        isIncome -> incomeColor
        isSaving -> savingsColor
        else -> expenseColor
    }

    val avatarBgColor = when {
        isSaving -> savingsColor
        transaction.categoryId.isNotBlank() -> getCategoryColorById(transaction.categoryId, categories)
        else -> NeutralUncategorizedColor
    }

    val sign = when {
        isIncome || isWithdraw -> "+"
        else -> "-"
    }

    AlcanciaCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onLongPress,
                onLongClick = onLongPress
            )
            .testTag("tx_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(avatarBgColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayEmoji,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Subtitle: Note and Time (if present)
                    val note = transaction.note.trim()
                    val time = transaction.timeString?.trim() ?: ""
                    val subtitleText = when {
                        note.isNotBlank() && time.isNotBlank() -> "$note • $time"
                        note.isNotBlank() -> note
                        time.isNotBlank() -> time
                        else -> ""
                    }

                    if (subtitleText.isNotBlank()) {
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (transaction.repeatInterval != "NONE") {
                        val repeatLabel = when (transaction.repeatInterval) {
                            "DAILY" -> "Diario"
                            "WEEKLY" -> "Semanal"
                            "MONTHLY" -> "Mensual"
                            "CUSTOM" -> if (transaction.customRepeatDays.isNotBlank()) "Días ${transaction.customRepeatDays}" else "Personalizado"
                            else -> transaction.repeatInterval
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = repeatLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            val amountText = "$sign${CurrencyUtils.formatAmount(transaction.amount, currencySymbol)}"
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = numberFont),
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridTransactionCard(
    transaction: TransactionEntity,
    currencySymbol: String,
    categories: List<CategoryEntity>,
    modifier: Modifier = Modifier,
    onLongPress: () -> Unit
) {
    val incomeColor = LocalIncomeColor.current
    val expenseColor = LocalExpenseColor.current
    val savingsColor = LocalSavingsColor.current
    val numberFont = LocalNumberFontFamily.current

    val isIncome = transaction.type == "INCOME"
    val isSaving = transaction.type.startsWith("SAVING")
    val isWithdraw = transaction.type == "SAVING_WITHDRAW"

    val displayEmoji = when {
        isSaving -> "🐷"
        transaction.categoryEmoji.isNotBlank() -> transaction.categoryEmoji
        isIncome -> "💰"
        else -> "💸"
    }

    val amountColor = when {
        isIncome -> incomeColor
        isSaving -> savingsColor
        else -> expenseColor
    }

    val avatarBgColor = when {
        isSaving -> savingsColor
        transaction.categoryId.isNotBlank() -> getCategoryColorById(transaction.categoryId, categories)
        else -> NeutralUncategorizedColor
    }

    val sign = when {
        isIncome || isWithdraw -> "+"
        else -> "-"
    }

    val amountText = "$sign${CurrencyUtils.formatCompact(transaction.amount, currencySymbol)}"
    val shortNote = when {
        transaction.note.isNotBlank() -> transaction.note
        !transaction.timeString.isNullOrBlank() -> transaction.timeString
        isSaving -> transaction.goalName.ifBlank { "Meta" }
        transaction.categoryName.isNotBlank() -> transaction.categoryName
        isIncome -> "Ingreso"
        else -> "Gasto"
    }

    AlcanciaCard(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onLongPress,
                onLongClick = onLongPress
            )
            .testTag("grid_tx_${transaction.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(avatarBgColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = displayEmoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = amountText,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = numberFont),
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = shortNote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
