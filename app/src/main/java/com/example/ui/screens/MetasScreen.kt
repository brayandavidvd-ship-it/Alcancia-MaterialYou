package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GoalEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.CurrencyUtils
import com.example.ui.theme.ColorblindExpenseColor
import com.example.ui.theme.ColorblindIncomeColor
import com.example.ui.theme.LocalNumberFontFamily
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
    val pinnedGoalId by viewModel.pinnedGoalId.collectAsStateWithLifecycle()

    // Palette resolution using InicioPalette (Rule E7 and Block C)
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDark
    }
    val rawPalette = if (isDark) InicioPalette.Dark else InicioPalette.Light
    val palette = if (settings.isColorblindPreset) {
        rawPalette.copy(
            ingreso = ColorblindIncomeColor,
            gasto = ColorblindExpenseColor
        )
    } else {
        rawPalette
    }

    val togglePin: (Long) -> Unit = { goalId ->
        viewModel.togglePinGoal(goalId)
    }

    // Clean up orphaned pinnedGoalId safely outside composition
    LaunchedEffect(goalsWithProgress, pinnedGoalId) {
        if (pinnedGoalId != -1L && goalsWithProgress.isNotEmpty() && goalsWithProgress.none { it.goal.id == pinnedGoalId }) {
            viewModel.clearPinnedGoalIfMatches(pinnedGoalId)
        }
    }

    // Resolve hero goal: pinned goal if exists, otherwise fallback to first active or first goal
    val pinnedGoal = remember(goalsWithProgress, pinnedGoalId) {
        if (goalsWithProgress.isEmpty()) null
        else {
            val found = if (pinnedGoalId != -1L) goalsWithProgress.find { it.goal.id == pinnedGoalId } else null
            found ?: goalsWithProgress.firstOrNull { !it.isCompleted } ?: goalsWithProgress.firstOrNull()
        }
    }

    var selectedGoalId by remember { mutableStateOf<Long?>(null) }
    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<GoalEntity?>(null) }

    // Quick Keypad Modal for Aportar / Retirar
    var activeKeypadGoal by remember { mutableStateOf<GoalWithProgress?>(null) }
    var activeKeypadIsWithdrawal by remember { mutableStateOf(false) }

    // Completed goals accordion state
    var isCompletedExpanded by remember { mutableStateOf(false) }

    // Secondary Screen: Goal Detail View
    if (selectedGoalId != null) {
        val selectedGoalProgress = goalsWithProgress.find { it.goal.id == selectedGoalId }
        if (selectedGoalProgress != null) {
            GoalDetailView(
                goalWithProgress = selectedGoalProgress,
                isPinned = (pinnedGoalId == selectedGoalProgress.goal.id),
                currencySymbol = settings.currencySymbol,
                palette = palette,
                onBack = { selectedGoalId = null },
                onTogglePin = { togglePin(selectedGoalProgress.goal.id) },
                onEditGoal = { goalToEdit = selectedGoalProgress.goal },
                onDeleteGoal = {
                    goalToDelete = selectedGoalProgress.goal
                },
                onOpenKeypad = { isWithdrawal ->
                    activeKeypadGoal = selectedGoalProgress
                    activeKeypadIsWithdrawal = isWithdrawal
                },
                onDeleteTransaction = { tx ->
                    viewModel.deleteTransaction(tx)
                },
                onUpdateTransaction = { tx, newAmount, newNote, newDate ->
                    viewModel.updateTransaction(
                        id = tx.id,
                        type = tx.type,
                        amount = newAmount,
                        category = null,
                        note = newNote,
                        dateMillis = newDate,
                        repeatInterval = tx.repeatInterval,
                        customRepeatDays = tx.customRepeatDays,
                        timeString = tx.timeString
                    )
                }
            )
        } else {
            selectedGoalId = null
        }
    } else {
        // Main Metas Screen
        val otherGoals = remember(goalsWithProgress, pinnedGoal) {
            goalsWithProgress.filter { it.goal.id != pinnedGoal?.goal?.id && !it.isCompleted }
        }
        val completedGoals = remember(goalsWithProgress) {
            goalsWithProgress.filter { it.isCompleted }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(palette.bg)
                .statusBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("metas_screen_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 124.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Circle Flag Icon (40dp, s3) + Title + Circle Grid Toggle (40dp, s2) - C5
                item(key = "metas_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(palette.s3),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = palette.t1,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Metas",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.t1
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleGoalsViewMode() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.s2)
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = "Cambiar vista",
                                tint = palette.t2,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // HERO CARD: Pinned / Featured Goal
                if (pinnedGoal != null) {
                    item(key = "hero_pinned_goal") {
                        HeroGoalCard(
                            goalWithProgress = pinnedGoal,
                            isExplicitlyPinned = (pinnedGoalId == pinnedGoal.goal.id),
                            currencySymbol = settings.currencySymbol,
                            palette = palette,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedGoalId = pinnedGoal.goal.id
                            },
                            onAportar = {
                                activeKeypadGoal = pinnedGoal
                                activeKeypadIsWithdrawal = false
                            },
                            onRetirar = {
                                activeKeypadGoal = pinnedGoal
                                activeKeypadIsWithdrawal = true
                            }
                        )
                    }
                }

                // Empty state if no goals at all
                if (goalsWithProgress.isEmpty()) {
                    item(key = "metas_empty_state") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = palette.s1,
                            border = BorderStroke(1.dp, palette.s3)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(palette.s2),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = palette.ahorro,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Sin metas activas",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.t1
                                )
                                Text(
                                    text = "Crea tu primera meta de ahorro con el botón de abajo.",
                                    fontSize = 13.sp,
                                    color = palette.t2,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // OTRAS METAS Section
                if (otherGoals.isNotEmpty()) {
                    item(key = "otras_metas_header") {
                        Text(
                            text = "OTRAS METAS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.t2,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                        )
                    }

                    if (isGridView) {
                        // 2-column grid layout
                        val pairs = otherGoals.chunked(2)
                        items(pairs, key = { it.first().goal.id }) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (item in rowItems) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        OtherGoalGridCard(
                                            goalWithProgress = item,
                                            currencySymbol = settings.currencySymbol,
                                            palette = palette,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedGoalId = item.goal.id
                                            },
                                            onPinClick = { togglePin(item.goal.id) }
                                        )
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        // 1-column list layout
                        items(otherGoals, key = { it.goal.id }) { item ->
                            OtherGoalListCard(
                                goalWithProgress = item,
                                currencySymbol = settings.currencySymbol,
                                palette = palette,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedGoalId = item.goal.id
                                },
                                onPinClick = { togglePin(item.goal.id) }
                            )
                        }
                    }
                }

                // COMPLETADAS Section
                if (completedGoals.isNotEmpty()) {
                    item(key = "completadas_accordion") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isCompletedExpanded = !isCompletedExpanded
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = palette.s1,
                            border = BorderStroke(1.dp, palette.s3)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Completadas · ${completedGoals.size}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.t1
                                    )
                                    Icon(
                                        imageVector = if (isCompletedExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
                                        contentDescription = "Expandir completadas",
                                        tint = palette.t2,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (isCompletedExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .padding(bottom = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        for (item in completedGoals) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        selectedGoalId = item.goal.id
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                color = palette.s2
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(CircleShape)
                                                                .background(palette.ingreso.copy(alpha = 0.2f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = palette.ingreso,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = item.goal.name,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = palette.t1
                                                        )
                                                    }

                                                    Text(
                                                        text = CurrencyUtils.formatCompact(item.goal.targetAmount, settings.currencySymbol),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = palette.t2
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Right "+ Nueva meta" Button (C1: fondo brand #5D7396, texto onBrand #FFFFFF)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 100.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = palette.brand,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .testTag("nueva_meta_button")
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showCreateGoalDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = palette.onBrand,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Nueva meta",
                            color = palette.onBrand,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet: Aportar / Retirar Keypad (C2 & C3)
    if (activeKeypadGoal != null) {
        GoalKeypadBottomSheet(
            goalWithProgress = activeKeypadGoal!!,
            initialIsWithdrawal = activeKeypadIsWithdrawal,
            currencySymbol = settings.currencySymbol,
            palette = palette,
            onDismiss = { activeKeypadGoal = null },
            onConfirm = { isWithdrawal, amount, note ->
                viewModel.addTransaction(
                    type = if (isWithdrawal) "SAVING_WITHDRAW" else "SAVING_DEPOSIT",
                    amount = amount,
                    category = null,
                    note = note,
                    dateMillis = System.currentTimeMillis(),
                    repeatInterval = "NONE",
                    goal = activeKeypadGoal!!.goal
                )
                activeKeypadGoal = null
            }
        )
    }

    // Dialog: Create or Edit Goal
    if (showCreateGoalDialog || goalToEdit != null) {
        GoalEditorDialog(
            goalToEdit = goalToEdit,
            currencySymbol = settings.currencySymbol,
            palette = palette,
            onDismiss = {
                showCreateGoalDialog = false
                goalToEdit = null
            },
            onSave = { name, emoji, target, deadline ->
                if (goalToEdit != null) {
                    viewModel.updateGoal(
                        goalToEdit!!.copy(
                            name = name,
                            emoji = emoji,
                            targetAmount = target,
                            deadlineDateMillis = deadline
                        )
                    )
                } else {
                    viewModel.createGoal(name, emoji, target, deadline)
                }
                showCreateGoalDialog = false
                goalToEdit = null
            }
        )
    }

    // Dialog: Delete Goal Confirmation
    if (goalToDelete != null) {
        val target = goalToDelete!!
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            containerColor = palette.s1,
            titleContentColor = palette.t1,
            textContentColor = palette.t2,
            title = { Text("¿Eliminar la meta \"${target.name}\"?") },
            text = { Text("Se eliminará la meta y los ahorros registrados volverán a reflejarse como saldo disponible.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(target)
                        if (selectedGoalId == target.id) {
                            selectedGoalId = null
                        }
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.gasto)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Cancelar", color = palette.t2)
                }
            }
        )
    }
}

// Donut Progress Ring Component (C3: color ahorro #B9A6FF)
@Composable
private fun GoalDonutRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    percentageFontSize: TextUnit = 16.sp,
    trackColor: Color,
    progressColor: Color,
    textColor: Color
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val percentage = (clampedProgress * 100).toInt()
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Background circle track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress arc
            if (clampedProgress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = clampedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = "$percentage%",
            color = textColor,
            fontSize = percentageFontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

// Hero Card (Pinned Goal)
@Composable
private fun HeroGoalCard(
    goalWithProgress: GoalWithProgress,
    isExplicitlyPinned: Boolean,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    onClick: () -> Unit,
    onAportar: () -> Unit,
    onRetirar: () -> Unit
) {
    val goal = goalWithProgress.goal
    val saved = goalWithProgress.savedAmount
    val target = goal.targetAmount
    val remaining = (target - saved).coerceAtLeast(0.0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = palette.s1,
        border = BorderStroke(1.dp, palette.s3)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Ring (C3: progreso en palette.ahorro)
                GoalDonutRing(
                    progress = goalWithProgress.progress,
                    size = 80.dp,
                    strokeWidth = 8.dp,
                    percentageFontSize = 16.sp,
                    trackColor = palette.s3,
                    progressColor = palette.ahorro,
                    textColor = palette.t1
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Pinned Chip (shown when explicitly pinned)
                    if (isExplicitlyPinned) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.s3,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = palette.ahorro,
                                    modifier = Modifier
                                        .size(11.dp)
                                        .rotate(45f)
                                )
                                Text(
                                    text = "Fijada",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.ahorro
                                )
                            }
                        }
                    }

                    // Goal Name
                    Text(
                        text = goal.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.t1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Target Amounts
                    Text(
                        text = "${CurrencyUtils.formatCompact(saved, currencySymbol)} de ${CurrencyUtils.formatCompact(target, currencySymbol)}",
                        fontSize = 13.sp,
                        color = palette.t2,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // Remaining
                    Text(
                        text = if (goalWithProgress.isCompleted) "Meta completada 🎉" else "Faltan ${CurrencyUtils.formatCompact(remaining, currencySymbol)}",
                        fontSize = 13.sp,
                        color = palette.t2,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: + Aportar / - Retirar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onAportar() },
                    shape = RoundedCornerShape(16.dp),
                    color = palette.s2
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Aportar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.t1
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onRetirar() },
                    shape = RoundedCornerShape(16.dp),
                    color = palette.s2
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "− Retirar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.t1
                        )
                    }
                }
            }
        }
    }
}

// Other Goal Card (2-column Grid, C6: pin con zona táctil 40dp y color t2)
@Composable
private fun OtherGoalGridCard(
    goalWithProgress: GoalWithProgress,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    onClick: () -> Unit,
    onPinClick: () -> Unit
) {
    val goal = goalWithProgress.goal
    val percentage = (goalWithProgress.progress.coerceIn(0f, 1f) * 100).toInt()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = palette.s1,
        border = BorderStroke(1.dp, palette.s3)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Flag circle + Pin button (min 40dp touch target, color t2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(palette.s2),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = palette.ahorro,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onPinClick,
                    modifier = Modifier.size(40.dp) // C6: min 40dp
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Fijar meta",
                        tint = palette.t2, // C6: color t2
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(45f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Goal Name
            Text(
                text = goal.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.t1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Amounts
            Text(
                text = "${CurrencyUtils.formatCompact(goalWithProgress.savedAmount, currencySymbol)} de ${CurrencyUtils.formatCompact(goal.targetAmount, currencySymbol)}",
                fontSize = 12.sp,
                color = palette.t2,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Linear Progress Bar (C3: color ahorro)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.s3)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = goalWithProgress.progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.ahorro)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Percentage (C3: color ahorro)
            Text(
                text = "$percentage%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.ahorro
            )
        }
    }
}

// Other Goal Card (1-column List, C6: pin con zona táctil 40dp y color t2)
@Composable
private fun OtherGoalListCard(
    goalWithProgress: GoalWithProgress,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    onClick: () -> Unit,
    onPinClick: () -> Unit
) {
    val goal = goalWithProgress.goal
    val percentage = (goalWithProgress.progress.coerceIn(0f, 1f) * 100).toInt()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = palette.s1,
        border = BorderStroke(1.dp, palette.s3)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.s2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = palette.ahorro,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = goal.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.t1
                        )
                        Text(
                            text = "${CurrencyUtils.formatCompact(goalWithProgress.savedAmount, currencySymbol)} de ${CurrencyUtils.formatCompact(goal.targetAmount, currencySymbol)}",
                            fontSize = 12.sp,
                            color = palette.t2
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$percentage%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.ahorro,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(
                        onClick = onPinClick,
                        modifier = Modifier.size(40.dp) // C6: min 40dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Fijar meta",
                            tint = palette.t2, // C6: color t2
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(45f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Linear Progress Bar (C3: color ahorro)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.s3)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = goalWithProgress.progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.ahorro)
                )
            }
        }
    }
}

// Goal Detail View (C3 & C4: contentPadding inferior 124dp, colores de InicioPalette)
@Composable
private fun GoalDetailView(
    goalWithProgress: GoalWithProgress,
    isPinned: Boolean,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    onBack: () -> Unit,
    onTogglePin: () -> Unit,
    onEditGoal: () -> Unit,
    onDeleteGoal: () -> Unit,
    onOpenKeypad: (isWithdrawal: Boolean) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onUpdateTransaction: (TransactionEntity, Double, String, Long) -> Unit
) {
    BackHandler { onBack() }

    val haptic = LocalHapticFeedback.current
    val goal = goalWithProgress.goal
    val numberFont = LocalNumberFontFamily.current
    val saved = goalWithProgress.savedAmount
    val target = goal.targetAmount
    val remaining = (target - saved).coerceAtLeast(0.0)

    val viewModel: AlcanciaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val transactions by viewModel.getTransactionsForGoal(goal.id).collectAsStateWithLifecycle(emptyList())

    var txToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = palette.t1
                    )
                }

                Text(
                    text = goal.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.t1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Pin Button
                IconButton(onClick = onTogglePin, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Fijar",
                        tint = if (isPinned) palette.ahorro else palette.t2,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(45f)
                    )
                }

                // Edit Button
                IconButton(onClick = onEditGoal, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = palette.t2,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete Button (soft red)
                IconButton(onClick = onDeleteGoal, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = palette.gasto,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Scrollable Content (C4: contentPadding inferior 124dp)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 124.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Donut Hero Section
                item(key = "detail_hero") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large Donut Ring (110dp, stroke 10dp, C3: color ahorro)
                        GoalDonutRing(
                            progress = goalWithProgress.progress,
                            size = 110.dp,
                            strokeWidth = 10.dp,
                            percentageFontSize = 22.sp,
                            trackColor = palette.s3,
                            progressColor = palette.ahorro,
                            textColor = palette.t1
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Amount (e.g. $2,040)
                        Text(
                            text = CurrencyUtils.formatCompact(saved, currencySymbol),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = numberFont,
                            color = palette.t1
                        )

                        // Subtitle
                        Text(
                            text = "de ${CurrencyUtils.formatCompact(target, currencySymbol)} objetivo",
                            fontSize = 14.sp,
                            color = palette.t2,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pill (Faltan $2,960)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.s2,
                            border = BorderStroke(1.dp, palette.s3)
                        ) {
                            Text(
                                text = if (goalWithProgress.isCompleted) "Meta cumplida 🎉" else "Faltan ${CurrencyUtils.formatCompact(remaining, currencySymbol)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.ahorro,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // + Aportar and - Retirar Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onOpenKeypad(false)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = palette.s2
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+ Aportar",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.t1
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onOpenKeypad(true)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = palette.s2
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "− Retirar",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.t1
                                    )
                                }
                            }
                        }
                    }
                }

                // Section Header: HISTORIAL
                item(key = "historial_header") {
                    Text(
                        text = "HISTORIAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.t2,
                        letterSpacing = 1.4.sp,
                        modifier = Modifier.padding(top = 8.dp, start = 2.dp)
                    )
                }

                // Transactions List
                if (transactions.isEmpty()) {
                    item(key = "historial_empty") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = palette.s1,
                            border = BorderStroke(1.dp, palette.s3)
                        ) {
                            Text(
                                text = "Sin aportes registrados aún.",
                                fontSize = 13.sp,
                                color = palette.t2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                } else {
                    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("es", "MX"))
                    items(transactions, key = { it.id }) { tx ->
                        val isWithdraw = tx.type == "SAVING_WITHDRAW"
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    txToEdit = tx
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = palette.s1,
                            border = BorderStroke(1.dp, palette.s3)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Icon Circle
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (isWithdraw) palette.gasto.copy(alpha = 0.2f) else palette.ahorro.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isWithdraw) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (isWithdraw) palette.gasto else palette.ahorro,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column {
                                        val displayTitle = if (tx.note.isNotBlank() && !tx.note.startsWith("Aporte a") && !tx.note.startsWith("Retiro de")) {
                                            tx.note
                                        } else {
                                            if (isWithdraw) "Retiro" else "Aporte"
                                        }

                                        Text(
                                            text = displayTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.t1
                                        )

                                        Text(
                                            text = dateFormat.format(Date(tx.dateMillis)),
                                            fontSize = 12.sp,
                                            color = palette.t2
                                        )
                                    }
                                }

                                Text(
                                    text = "${if (isWithdraw) "-" else "+"}${CurrencyUtils.formatAmount(tx.amount, currencySymbol)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = numberFont,
                                    color = if (isWithdraw) palette.gasto else palette.t1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Transaction Dialog
    if (txToEdit != null) {
        val target = txToEdit!!
        val isWithdraw = target.type == "SAVING_WITHDRAW"
        AlertDialog(
            onDismissRequest = { txToEdit = null },
            containerColor = palette.s1,
            titleContentColor = palette.t1,
            textContentColor = palette.t2,
            title = {
                Text(
                    text = "${if (isWithdraw) "Retiro" else "Aporte"} • ${CurrencyUtils.formatAmount(target.amount, currencySymbol)}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("¿Deseas eliminar este registro del historial?") },
            confirmButton = {
                Button(
                    onClick = {
                        txToDelete = target
                        txToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.gasto)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Borrar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToEdit = null }) {
                    Text("Cerrar", color = palette.t2)
                }
            }
        )
    }

    // Delete Transaction Dialog
    if (txToDelete != null) {
        val target = txToDelete!!
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            containerColor = palette.s1,
            titleContentColor = palette.t1,
            textContentColor = palette.t2,
            title = { Text("¿Borrar este movimiento?") },
            text = { Text("Se eliminará del historial y el saldo disponible se ajustará correspondientemente.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(target)
                        txToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.gasto)
                ) {
                    Text("Borrar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("Cancelar", color = palette.t2)
                }
            }
        )
    }
}

// Modal Sheet: Aportar / Retirar Keypad (C2: Botón brand #5D7396 con onBrand, sin estado deshabilitado)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalKeypadBottomSheet(
    goalWithProgress: GoalWithProgress,
    initialIsWithdrawal: Boolean,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    onDismiss: () -> Unit,
    onConfirm: (isWithdrawal: Boolean, amount: Double, note: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val goal = goalWithProgress.goal
    val numberFont = LocalNumberFontFamily.current
    val saved = goalWithProgress.savedAmount
    val target = goal.targetAmount
    val percentage = (goalWithProgress.progress.coerceIn(0f, 1f) * 100).toInt()

    var isWithdrawal by remember { mutableStateOf(initialIsWithdrawal) }
    var amountString by remember { mutableStateOf("0") }
    var noteText by remember { mutableStateOf("") }

    val parsedAmount = amountString.toDoubleOrNull() ?: 0.0
    val isWithdrawalExceeded = isWithdrawal && parsedAmount > saved

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = palette.s1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.s3)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title: Goal Name
            Text(
                text = goal.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.t1
            )

            // Subtitle: % · $saved de $target
            Text(
                text = "$percentage% · ${CurrencyUtils.formatCompact(saved, currencySymbol)} de ${CurrencyUtils.formatCompact(target, currencySymbol)}",
                fontSize = 13.sp,
                color = palette.t2,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Control Toggle: Aportar / Retirar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.s2,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Aportar Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isWithdrawal = false
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isWithdrawal) palette.s3 else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aportar",
                                fontSize = 14.sp,
                                fontWeight = if (!isWithdrawal) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isWithdrawal) palette.t1 else palette.t2
                            )
                        }
                    }

                    // Retirar Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isWithdrawal = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isWithdrawal) palette.s3 else Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Retirar",
                                fontSize = 14.sp,
                                fontWeight = if (isWithdrawal) FontWeight.Bold else FontWeight.Medium,
                                color = if (isWithdrawal) palette.t1 else palette.t2
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Helper text
            Text(
                text = if (isWithdrawal) "Se devolverá a tu saldo del mes" else "Se descontará de tu saldo del mes",
                fontSize = 13.sp,
                color = palette.t2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Big Amount Display (+$0 / -$0, C3: color ahorro #B9A6FF)
            val displaySign = if (isWithdrawal) "−" else "+"
            Text(
                text = "$displaySign$currencySymbol$amountString",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = numberFont,
                color = if (isWithdrawalExceeded) palette.gasto else palette.ahorro,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (isWithdrawalExceeded) {
                Text(
                    text = "El retiro supera el saldo ahorrado (${CurrencyUtils.formatCompact(saved, currencySymbol)})",
                    fontSize = 12.sp,
                    color = palette.gasto,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3x4 Custom Numeric Keypad
            val keyRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "DEL")
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in keyRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (key in row) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        when (key) {
                                            "DEL" -> {
                                                if (amountString.length > 1) {
                                                    amountString = amountString.dropLast(1)
                                                } else {
                                                    amountString = "0"
                                                }
                                            }
                                            "." -> {
                                                if (!amountString.contains(".")) {
                                                    amountString += "."
                                                }
                                            }
                                            else -> {
                                                if (amountString == "0") {
                                                    amountString = key
                                                } else {
                                                    val dotIdx = amountString.indexOf('.')
                                                    if (dotIdx == -1 || amountString.length - dotIdx <= 2) {
                                                        if (amountString.length < 9) {
                                                            amountString += key
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = palette.s2
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "DEL") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Borrar",
                                            tint = palette.t1,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.t1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Note Field (Optional)
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = {
                    Text("Nota (opcional)", color = palette.t2, fontSize = 14.sp)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.t1,
                    unfocusedTextColor = palette.t1,
                    focusedContainerColor = palette.s2,
                    unfocusedContainerColor = palette.s2,
                    focusedBorderColor = palette.brand,
                    unfocusedBorderColor = palette.s3
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Confirm Button (C2: fondo brand #5D7396, texto onBrand #FFFFFF; si parsedAmount == 0 no hace nada, sin deshabilitar)
            Button(
                onClick = {
                    if (parsedAmount <= 0.0 || isWithdrawalExceeded) return@Button
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val note = if (noteText.isBlank()) {
                        if (isWithdrawal) "Retiro de ${goal.name}" else "Aporte a ${goal.name}"
                    } else {
                        noteText.trim()
                    }
                    onConfirm(isWithdrawal, parsedAmount, note)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.brand,
                    contentColor = palette.onBrand
                )
            ) {
                Text(
                    text = if (isWithdrawal) "Confirmar retiro" else "Confirmar aporte",
                    color = palette.onBrand,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// Goal Editor Dialog (Create / Edit)
@Composable
private fun GoalEditorDialog(
    goalToEdit: GoalEntity?,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, targetAmount: Double, deadlineDateMillis: Long?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(goalToEdit?.name ?: "") }
    var emoji by remember { mutableStateOf(goalToEdit?.emoji ?: "🎯") }
    var targetStr by remember { mutableStateOf(if (goalToEdit != null) goalToEdit.targetAmount.toString() else "") }
    var deadlineDateMillis by remember { mutableStateOf(goalToEdit?.deadlineDateMillis) }

    val emojis = listOf("🎯", "🏖️", "💻", "🚗", "🏠", "💍", "🎓", "📱", "✈️", "💰", "🎮", "🚴")
    val targetVal = targetStr.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.s1,
        titleContentColor = palette.t1,
        textContentColor = palette.t2,
        title = {
            Text(
                text = if (goalToEdit != null) "Editar meta" else "Nueva meta",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la meta", color = palette.t2) },
                    placeholder = { Text("Ej. Viaje, Laptop...", color = palette.t2.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.t1,
                        unfocusedTextColor = palette.t1,
                        focusedBorderColor = palette.brand,
                        unfocusedBorderColor = palette.s3
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Emoji Row
                Text(
                    text = "Icono",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.t2
                )
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) palette.s3 else palette.s2)
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = e, fontSize = 20.sp)
                        }
                    }
                }

                // Target Amount Field
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Monto objetivo ($currencySymbol)", color = palette.t2) },
                    placeholder = { Text("Ej. 5000", color = palette.t2.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.t1,
                        unfocusedTextColor = palette.t1,
                        focusedBorderColor = palette.brand,
                        unfocusedBorderColor = palette.s3
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Deadline Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fecha límite (opcional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.t2
                    )
                    if (deadlineDateMillis != null) {
                        Text(
                            text = "Quitar",
                            fontSize = 12.sp,
                            color = palette.brand,
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = palette.brand,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val label = if (deadlineDateMillis != null) {
                        SimpleDateFormat("d 'de' MMMM, yyyy", Locale("es", "MX")).format(Date(deadlineDateMillis!!))
                    } else {
                        "Seleccionar fecha límite"
                    }
                    Text(label, color = palette.t1)
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
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.brand,
                    disabledContainerColor = palette.s3
                )
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold, color = palette.onBrand)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = palette.t2)
            }
        }
    )
}
