package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GoalEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.AlcanciaCard
import com.example.ui.components.CurrencyUtils
import com.example.ui.theme.LocalExpenseColor
import com.example.ui.theme.LocalIncomeColor
import com.example.ui.theme.LocalIsCompactDensity
import com.example.ui.theme.LocalNumberFontFamily
import com.example.ui.theme.LocalSavingsColor
import com.example.ui.viewmodel.AlcanciaViewModel
import com.example.ui.viewmodel.GoalWithProgress
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasScreen(
    viewModel: AlcanciaViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val goalsWithProgress by viewModel.goalsWithProgressState.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGoalsGridView.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val savingsColor = LocalSavingsColor.current
    val expenseColor = LocalExpenseColor.current
    val isCompact = LocalIsCompactDensity.current

    var selectedGoalId by remember { mutableStateOf<Long?>(null) }
    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<GoalEntity?>(null) }
    var goalContextMenu by remember { mutableStateOf<GoalEntity?>(null) }

    // If viewing a goal's detail
    if (selectedGoalId != null) {
        val selectedGoalProgress = goalsWithProgress.find { it.goal.id == selectedGoalId }
        if (selectedGoalProgress != null) {
            GoalDetailView(
                goalWithProgress = selectedGoalProgress,
                currencySymbol = settings.currencySymbol,
                onBack = { selectedGoalId = null },
                onAddContribution = { isWithdrawal, amount, note, dateMillis ->
                    viewModel.addTransaction(
                        type = if (isWithdrawal) "SAVING_WITHDRAW" else "SAVING_DEPOSIT",
                        amount = amount,
                        category = null,
                        note = note,
                        dateMillis = dateMillis,
                        repeatInterval = "NONE",
                        goal = selectedGoalProgress.goal
                    )
                },
                onUpdateTransaction = { tx, newAmount, newNote, newDate ->
                    viewModel.updateTransaction(
                        id = tx.id,
                        type = tx.type,
                        amount = newAmount,
                        category = null,
                        note = newNote,
                        dateMillis = newDate,
                        repeatInterval = "NONE",
                        goal = selectedGoalProgress.goal
                    )
                },
                onDeleteTransaction = { tx ->
                    viewModel.deleteTransaction(tx)
                },
                viewModel = viewModel
            )
        } else {
            selectedGoalId = null
        }
    } else {

    val hasApiKey by viewModel.hasGeminiApiKey.collectAsStateWithLifecycle()
    val showAiFab = settings.isCloudAiEnabled && settings.cloudAiConsentAccepted && hasApiKey

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 86.dp)
            ) {
                if (showAiFab) {
                    SmallFloatingActionButton(
                        onClick = { viewModel.openAssistantChat() },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("ai_assistant_fab_metas")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Asistente de IA",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showCreateGoalDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.testTag("create_goal_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nueva meta", modifier = Modifier.size(28.dp))
                }
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
                bottom = 124.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
        ) {
            // Header
            item(key = "metas_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Metas de Ahorro",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ahorra para lo que te importa",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { viewModel.toggleGoalsViewMode() }) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Ver como lista" else "Ver como cuadrícula",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (goalsWithProgress.isEmpty()) {
                item(key = "empty_goals") {
                    AlcanciaCard(modifier = Modifier.fillMaxWidth()) {
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
                                    .background(savingsColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🎯", fontSize = 32.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Sin metas de ahorro todavía",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Crea tu primera meta con un monto objetivo y fecha límite para calcular cuánto ahorrar cada mes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateGoalDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text("Crear mi primera meta", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (isGridView) {
                // 3-Column Grid
                items(
                    items = goalsWithProgress.chunked(3),
                    key = { chunk -> "grid_goals_${chunk.first().goal.id}" }
                ) { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (gp in chunk) {
                            GridGoalCard(
                                goalWithProgress = gp,
                                currencySymbol = settings.currencySymbol,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedGoalId = gp.goal.id },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    goalContextMenu = gp.goal
                                }
                            )
                        }
                        for (i in 0 until (3 - chunk.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                // List View
                items(
                    items = goalsWithProgress,
                    key = { it.goal.id }
                ) { gp ->
                    ListGoalCard(
                        goalWithProgress = gp,
                        currencySymbol = settings.currencySymbol,
                        onClick = { selectedGoalId = gp.goal.id },
                        onEdit = { goalToEdit = gp.goal },
                        onDelete = { goalToDelete = gp.goal },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            goalContextMenu = gp.goal
                        }
                    )
                }
            }
        }
    }
}

    // Context Menu Dialog on Long Press (Main Menu)
    if (goalContextMenu != null) {
        val target = goalContextMenu!!
        AlertDialog(
            onDismissRequest = { goalContextMenu = null },
            title = { Text("${target.emoji} ${target.name}", fontWeight = FontWeight.Bold) },
            text = { Text("Selecciona una acción para esta meta de ahorro.") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val g = target
                            goalContextMenu = null
                            goalToEdit = g
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar")
                    }

                    Button(
                        onClick = {
                            val g = target
                            goalContextMenu = null
                            goalToDelete = g
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = expenseColor)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { goalContextMenu = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Create Goal Dialog / Sheet
    if (showCreateGoalDialog) {
        GoalEditorDialog(
            goal = null,
            currencySymbol = settings.currencySymbol,
            onDismiss = { showCreateGoalDialog = false },
            onSave = { name, emoji, target, deadline ->
                viewModel.createGoal(name, emoji, target, deadline)
                showCreateGoalDialog = false
            }
        )
    }

    // Edit Goal Dialog
    if (goalToEdit != null) {
        GoalEditorDialog(
            goal = goalToEdit,
            currencySymbol = settings.currencySymbol,
            onDismiss = { goalToEdit = null },
            onSave = { name, emoji, target, deadline ->
                viewModel.updateGoal(
                    goalToEdit!!.copy(
                        name = name,
                        emoji = emoji,
                        targetAmount = target,
                        deadlineDateMillis = deadline
                    )
                )
                goalToEdit = null
            }
        )
    }

    // Delete Goal Confirmation
    if (goalToDelete != null) {
        val target = goalToDelete!!
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("¿Eliminar meta?") },
            text = { Text("Se eliminará la meta \"${target.name}\" y su historial de aportes.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(target)
                        goalToDelete = null
                        if (selectedGoalId == target.id) {
                            selectedGoalId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = expenseColor)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListGoalCard(
    goalWithProgress: GoalWithProgress,
    currencySymbol: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goal = goalWithProgress.goal
    val savingsColor = LocalSavingsColor.current
    val expenseColor = LocalExpenseColor.current
    val numberFont = LocalNumberFontFamily.current

    AlcanciaCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .background(savingsColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = goal.emoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (goalWithProgress.isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = savingsColor.copy(alpha = 0.18f),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "¡Meta cumplida! 🎉",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = savingsColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (goalWithProgress.recommendedMonthlySaving != null) {
                            Text(
                                text = "Ahorra aprox. ${CurrencyUtils.formatAmount(goalWithProgress.recommendedMonthlySaving, currencySymbol)}/mes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${(goalWithProgress.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = numberFont),
                        fontWeight = FontWeight.Bold,
                        color = savingsColor
                    )
                    Text(
                        text = "${CurrencyUtils.formatCompact(goalWithProgress.savedAmount, currencySymbol)} / ${CurrencyUtils.formatCompact(goal.targetAmount, currencySymbol)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = numberFont),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Static Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(goalWithProgress.progress.coerceAtLeast(0.02f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (goalWithProgress.isCompleted) MaterialTheme.colorScheme.primary else savingsColor)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row with Edit and Delete buttons in the main menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Toca para ver historial",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar meta",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar meta",
                            tint = expenseColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridGoalCard(
    goalWithProgress: GoalWithProgress,
    currencySymbol: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goal = goalWithProgress.goal
    val savingsColor = LocalSavingsColor.current
    val numberFont = LocalNumberFontFamily.current

    AlcanciaCard(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(savingsColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = goal.emoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = goal.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${(goalWithProgress.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = numberFont),
                fontWeight = FontWeight.Bold,
                color = savingsColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = CurrencyUtils.formatCompact(goalWithProgress.savedAmount, currencySymbol),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = numberFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (goalWithProgress.isCompleted) {
                Text(
                    text = "¡Cumplida!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun GoalDetailView(
    goalWithProgress: GoalWithProgress,
    currencySymbol: String,
    onBack: () -> Unit,
    onAddContribution: (isWithdrawal: Boolean, amount: Double, note: String, dateMillis: Long) -> Unit,
    onUpdateTransaction: (tx: TransactionEntity, newAmount: Double, newNote: String, newDate: Long) -> Unit,
    onDeleteTransaction: (tx: TransactionEntity) -> Unit,
    viewModel: AlcanciaViewModel
) {
    BackHandler { onBack() }

    val haptic = LocalHapticFeedback.current
    val goal = goalWithProgress.goal
    val savingsColor = LocalSavingsColor.current
    val incomeColor = LocalIncomeColor.current
    val expenseColor = LocalExpenseColor.current
    val numberFont = LocalNumberFontFamily.current

    val transactions by viewModel.getTransactionsForGoal(goal.id).collectAsStateWithLifecycle(emptyList())

    var showContributionSheet by remember { mutableStateOf(false) }
    var contributionIsWithdrawal by remember { mutableStateOf(false) }
    var contextMenuTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(goal.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            item(key = "goal_summary_card") {
                AlcanciaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(savingsColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = goal.emoji, fontSize = 32.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = CurrencyUtils.formatAmount(goalWithProgress.savedAmount, currencySymbol),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = numberFont
                            ),
                            color = savingsColor
                        )

                        Text(
                            text = "Meta objetivo: ${CurrencyUtils.formatAmount(goal.targetAmount, currencySymbol)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (goal.deadlineDateMillis != null) {
                            val deadlineStr = SimpleDateFormat("d 'de' MMMM, yyyy", Locale("es", "MX")).format(Date(goal.deadlineDateMillis))
                            Text(
                                text = "Fecha límite: $deadlineStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(goalWithProgress.progress.coerceAtLeast(0.02f))
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (goalWithProgress.isCompleted) MaterialTheme.colorScheme.primary else savingsColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(goalWithProgress.progress * 100).toInt()}% completado",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = savingsColor
                            )
                            if (goalWithProgress.isCompleted) {
                                Text(
                                    text = "¡Meta cumplida! 🎉",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Big Action Buttons: Aportar & Retirar
            item(key = "goal_action_buttons") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            contributionIsWithdrawal = false
                            showContributionSheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = savingsColor)
                    ) {
                        Text("Aportar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            contributionIsWithdrawal = true
                            showContributionSheet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text("Retirar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // History Header
            item(key = "history_header") {
                Text(
                    text = "Historial de aportes y retiros",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (transactions.isEmpty()) {
                item(key = "empty_history") {
                    Text(
                        text = "Aún no hay movimientos en esta meta. Toca \"Aportar\" para comenzar a ahorrar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(items = transactions, key = { it.id }) { tx ->
                    val isWithdraw = tx.type == "SAVING_WITHDRAW"
                    val dateStr = SimpleDateFormat("d 'de' MMMM yyyy", Locale("es", "MX")).format(Date(tx.dateMillis))

                    AlcanciaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    contextMenuTx = tx
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    contextMenuTx = tx
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isWithdraw) "Retiro de ahorro" else "Aporte a meta",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWithdraw) expenseColor else savingsColor
                                )
                                if (tx.note.isNotBlank()) {
                                    Text(
                                        text = tx.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "${if (isWithdraw) "-" else "+"}${CurrencyUtils.formatAmount(tx.amount, currencySymbol)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = numberFont),
                                fontWeight = FontWeight.Bold,
                                color = if (isWithdraw) expenseColor else savingsColor
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal to contribute or withdraw
    if (showContributionSheet) {
        ContributionBottomSheet(
            isWithdrawal = contributionIsWithdrawal,
            goalName = goal.name,
            currencySymbol = currencySymbol,
            onDismiss = { showContributionSheet = false },
            onSave = { amount, note, dateMillis ->
                onAddContribution(contributionIsWithdrawal, amount, note, dateMillis)
                showContributionSheet = false
            }
        )
    }

    // Transaction Context Menu (Edit / Delete)
    if (contextMenuTx != null) {
        val tx = contextMenuTx!!
        val isWithdraw = tx.type == "SAVING_WITHDRAW"
        AlertDialog(
            onDismissRequest = { contextMenuTx = null },
            title = {
                Text(
                    text = "${if (isWithdraw) "Retiro" else "Aporte"} • ${CurrencyUtils.formatAmount(tx.amount, currencySymbol)}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("Selecciona qué deseas hacer con este movimiento.") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val target = tx
                        contextMenuTx = null
                        txToEdit = target
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar")
                    }

                    Button(
                        onClick = {
                            val target = tx
                            contextMenuTx = null
                            txToDelete = target
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = expenseColor)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Borrar", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { contextMenuTx = null }) { Text("Cancelar") }
            }
        )
    }

    // Delete confirmation
    if (txToDelete != null) {
        val target = txToDelete!!
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("¿Borrar este movimiento?") },
            text = { Text("Se eliminará del historial y el saldo disponible se ajustará correspondientemente.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(target)
                        txToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = expenseColor)
                ) {
                    Text("Borrar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { txToDelete = null }) { Text("Cancelar") } }
        )
    }

    // Edit transaction dialog
    if (txToEdit != null) {
        val target = txToEdit!!
        EditContributionDialog(
            tx = target,
            currencySymbol = currencySymbol,
            onDismiss = { txToEdit = null },
            onSave = { newAmount, newNote, newDate ->
                onUpdateTransaction(target, newAmount, newNote, newDate)
                txToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContributionBottomSheet(
    isWithdrawal: Boolean,
    goalName: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, note: String, dateMillis: Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val savingsColor = LocalSavingsColor.current
    val expenseColor = LocalExpenseColor.current
    val numberFont = LocalNumberFontFamily.current

    var amountString by remember { mutableStateOf("0") }
    var noteText by remember { mutableStateOf(if (isWithdrawal) "Retiro de $goalName" else "Aporte a $goalName") }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val parsedAmount = amountString.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (isWithdrawal) "Retirar de la meta" else "Aportar a la meta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isWithdrawal) "El dinero volverá a tu saldo disponible del mes" else "Se descontará de tu saldo disponible del mes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "${if (isWithdrawal) "-" else "+"}$currencySymbol$amountString",
                style = MaterialTheme.typography.displayLarge.copy(fontFamily = numberFont),
                fontWeight = FontWeight.Bold,
                color = if (isWithdrawal) expenseColor else savingsColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Keypad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(".", "0", "DEL")
                )

                for (row in keys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (key in row) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        when (key) {
                                            "DEL" -> amountString = if (amountString.length > 1) amountString.dropLast(1) else "0"
                                            "." -> if (!amountString.contains(".")) amountString += "."
                                            else -> {
                                                if (amountString == "0") amountString = key
                                                else if (amountString.length < 9) amountString += key
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = key, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Nota (opcional)") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (parsedAmount > 0.0) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(parsedAmount, noteText, selectedDateMillis)
                    }
                },
                enabled = parsedAmount > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isWithdrawal) expenseColor else savingsColor)
            ) {
                Text(
                    text = if (isWithdrawal) "Confirmar retiro" else "Confirmar aporte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun GoalEditorDialog(
    goal: GoalEntity?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, target: Double, deadline: Long?) -> Unit
) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var emoji by remember { mutableStateOf(goal?.emoji ?: "🎯") }
    var targetStr by remember {
        val t = goal?.targetAmount
        mutableStateOf(if (t != null && t > 0) t.toInt().toString() else "")
    }
    var deadlineDateMillis by remember { mutableStateOf(goal?.deadlineDateMillis) }

    val emojis = listOf("🎯", "🚗", "🏠", "✈️", "💻", "💍", "🎓", "🏖️", "📱", "🎁", "🛡️", "🐷")
    val context = LocalContext.current

    val targetVal = targetStr.toDoubleOrNull() ?: 0.0
    val now = System.currentTimeMillis()

    // Automatic calculation of monthly recommended savings
    val recMonthlyText: String? = remember(targetVal, deadlineDateMillis) {
        if (deadlineDateMillis != null && deadlineDateMillis!! > now && targetVal > 0.0) {
            val diff = deadlineDateMillis!! - now
            val months = (diff / (1000L * 60 * 60 * 24 * 30.416)).coerceAtLeast(1.0)
            val monthly = targetVal / months
            "Ahorra aprox. ${CurrencyUtils.formatAmount(monthly, currencySymbol)} al mes para llegar a tiempo"
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal != null) "Editar meta" else "Nueva meta", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la meta") },
                    placeholder = { Text("Ej. Viaje a Japón, Auto nuevo...") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icono", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (e in emojis) {
                        val isSelected = emoji == e
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = e, fontSize = 20.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Monto objetivo ($currencySymbol)") },
                    placeholder = { Text("Ej. 25000") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Fecha límite opcional
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fecha límite (opcional)", style = MaterialTheme.typography.labelMedium)
                    if (deadlineDateMillis != null) {
                        Text(
                            text = "Quitar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { deadlineDateMillis = null }
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            if (deadlineDateMillis != null) timeInMillis = deadlineDateMillis!!
                            else add(Calendar.MONTH, 6)
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val newCal = Calendar.getInstance().apply { set(year, month, day, 12, 0) }
                                deadlineDateMillis = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    val label = if (deadlineDateMillis != null) {
                        SimpleDateFormat("d 'de' MMMM, yyyy", Locale("es", "MX")).format(Date(deadlineDateMillis!!))
                    } else {
                        "Seleccionar fecha límite"
                    }
                    Text(label)
                }

                if (recMonthlyText != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 $recMonthlyText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && targetVal > 0.0) {
                        onSave(name.trim(), emoji, targetVal, deadlineDateMillis)
                    }
                },
                enabled = name.isNotBlank() && targetVal > 0.0,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Guardar meta", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EditContributionDialog(
    tx: TransactionEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, note: String, dateMillis: Long) -> Unit
) {
    var amountStr by remember { mutableStateOf(tx.amount.toString()) }
    var note by remember { mutableStateOf(tx.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar movimiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Monto ($currencySymbol)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: tx.amount
                    onSave(amt, note, tx.dateMillis)
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
