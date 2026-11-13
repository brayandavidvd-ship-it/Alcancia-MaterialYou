package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import android.widget.Toast
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.GoalEntity
import com.example.data.model.TransactionEntity
import com.example.ui.theme.LocalExpenseColor
import com.example.ui.theme.LocalIncomeColor
import com.example.ui.theme.LocalNumberFontFamily
import com.example.ui.theme.LocalSavingsColor
import com.example.ui.theme.getCategoryColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    categories: List<CategoryEntity>,
    goals: List<GoalEntity> = emptyList(),
    currencySymbol: String,
    initialTransaction: TransactionEntity? = null,
    initialType: String = "EXPENSE",
    initialAmount: Double? = null,
    initialNote: String? = null,
    isLocalAiEnabled: Boolean = false,
    onSuggestCategory: ((description: String, onResult: (CategoryEntity?) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit,
    onCreateGoal: (() -> Unit)? = null,
    onSave: (
        type: String, // "EXPENSE", "INCOME", "SAVING_DEPOSIT", "SAVING_WITHDRAW"
        amount: Double,
        category: CategoryEntity?,
        note: String,
        dateMillis: Long,
        repeat: String,
        goal: GoalEntity?,
        timeString: String?,
        customRepeatDays: String
    ) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val incomeColor = LocalIncomeColor.current
    val expenseColor = LocalExpenseColor.current
    val savingsColor = LocalSavingsColor.current
    val numberFont = LocalNumberFontFamily.current

    // Initial mode detection
    val initialMode = when {
        initialTransaction != null -> when (initialTransaction.type) {
            "INCOME" -> "INCOME"
            "SAVING_DEPOSIT", "SAVING_WITHDRAW" -> "SAVINGS"
            else -> "EXPENSE"
        }
        initialType == "INCOME" -> "INCOME"
        initialType == "SAVINGS" || initialType == "SAVING_DEPOSIT" -> "SAVINGS"
        else -> "EXPENSE"
    }

    var mainTab by remember { mutableStateOf(initialMode) } // "EXPENSE", "INCOME", "SAVINGS"
    var savingSubtype by remember {
        mutableStateOf(if (initialTransaction?.type == "SAVING_WITHDRAW") "WITHDRAW" else "DEPOSIT")
    }

    var selectedGoal by remember {
        mutableStateOf(goals.find { it.id == initialTransaction?.goalId } ?: goals.firstOrNull())
    }

    var amountString by remember {
        val initAmt = initialTransaction?.amount ?: initialAmount
        mutableStateOf(
            if (initAmt != null) {
                if (initAmt % 1.0 == 0.0) initAmt.toInt().toString() else initAmt.toString()
            } else "0"
        )
    }

    // Retain existing category silently if editing an old transaction, never show it, null for new
    val existingCategory = remember {
        if (initialTransaction != null && initialTransaction.categoryId.isNotBlank()) {
            categories.firstOrNull { it.id == initialTransaction.categoryId }
                ?: CategoryEntity(
                    id = initialTransaction.categoryId,
                    name = initialTransaction.categoryName,
                    emoji = initialTransaction.categoryEmoji,
                    colorHex = "#9E9E9E",
                    monthlyBudgetLimit = null,
                    isDefault = false,
                    sortOrder = 0
                )
        } else null
    }

    var noteText by remember {
        mutableStateOf(initialTransaction?.note ?: initialNote ?: "")
    }
    var selectedDateMillis by remember {
        mutableLongStateOf(initialTransaction?.dateMillis ?: System.currentTimeMillis())
    }
    var selectedTimeString by remember {
        mutableStateOf(initialTransaction?.timeString)
    }
    var repeatOption by remember {
        mutableStateOf(initialTransaction?.repeatInterval ?: "NONE") // "NONE", "DAILY", "WEEKLY", "MONTHLY", "CUSTOM"
    }
    var customRepeatDaysSet by remember {
        val initDays = initialTransaction?.customRepeatDays?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
        mutableStateOf(initDays)
    }

    val parsedAmount = amountString.toDoubleOrNull() ?: 0.0

    val activeColor = when (mainTab) {
        "INCOME" -> incomeColor
        "SAVINGS" -> savingsColor
        else -> expenseColor
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header with title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTransaction != null) "Editar movimiento" else "Nuevo movimiento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3-way Connected Segmented Tab Selector: Gasto | Ingreso | Ahorro
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Gasto
                    val isExpense = mainTab == "EXPENSE"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (isExpense) expenseColor else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                mainTab = "EXPENSE"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gasto",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                                color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Ingreso
                    val isIncome = mainTab == "INCOME"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (isIncome) incomeColor else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                mainTab = "INCOME"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ingreso",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                                color = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Ahorro
                    val isSavings = mainTab == "SAVINGS"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSavings) savingsColor else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                mainTab = "SAVINGS"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = if (isSavings) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ahorro",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSavings) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSavings) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // If Ahorro: Sub-toggle (Aporte vs Retiro) and Goal Picker
            if (mainTab == "SAVINGS") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = savingSubtype == "DEPOSIT",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            savingSubtype = "DEPOSIT"
                        },
                        label = { Text("Aporte (guardar)", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = savingsColor.copy(alpha = 0.2f),
                            selectedLabelColor = savingsColor
                        )
                    )
                    FilterChip(
                        selected = savingSubtype == "WITHDRAW",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            savingSubtype = "WITHDRAW"
                        },
                        label = { Text("Retiro (sacar)", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = savingsColor.copy(alpha = 0.2f),
                            selectedLabelColor = savingsColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (goals.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Aún no tienes metas creadas",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (onCreateGoal != null) {
                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onCreateGoal()
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Crear primera meta", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Selecciona la meta:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (g in goals) {
                            val isGoalSelected = selectedGoal?.id == g.id
                            FilterChip(
                                selected = isGoalSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedGoal = g
                                },
                                label = {
                                    Text("${g.emoji} ${g.name}", fontWeight = if (isGoalSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, softWrap = false)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = savingsColor.copy(alpha = 0.2f),
                                    selectedLabelColor = savingsColor
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Amount Display
            val prefixSign = when (mainTab) {
                "INCOME" -> "+"
                "SAVINGS" -> if (savingSubtype == "WITHDRAW") "+" else "-"
                else -> "-"
            }
            Text(
                text = "$prefixSign$currencySymbol$amountString",
                style = MaterialTheme.typography.displayLarge.copy(fontFamily = numberFont),
                fontWeight = FontWeight.Bold,
                color = activeColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("keypad_amount_display")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Numeric Keypad
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val keyRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf(".", "0", "DEL")
                    )

                    for (row in keyRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (key in row) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            when (key) {
                                                "DEL" -> {
                                                    amountString = if (amountString.length > 1) {
                                                        amountString.dropLast(1)
                                                    } else {
                                                        "0"
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
                                                        val parts = amountString.split(".")
                                                        if (parts.size <= 1 || parts[1].length < 2) {
                                                            amountString += key
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("keypad_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "DEL") {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Borrar",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = numberFont),
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Nota (opcional)
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Nota (opcional)") },
                placeholder = {
                    Text(
                        if (mainTab == "SAVINGS") "Ej. Ahorro quincenal" else "Ej. Despensa, salario, etc."
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de fecha y Hora
            Text(
                text = "Fecha y hora",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            val todayMillis = remember {
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis
            }
            val yesterdayMillis = remember {
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis
            }

            val dayFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
            val isToday = dayFormat.format(Date(selectedDateMillis)) == dayFormat.format(Date(todayMillis))
            val isYesterday = dayFormat.format(Date(selectedDateMillis)) == dayFormat.format(Date(yesterdayMillis))
            val isCustomDate = !isToday && !isYesterday

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isToday,
                    onClick = {
                        val currentCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                        val todayCal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                        }
                        selectedDateMillis = todayCal.timeInMillis
                    },
                    label = { Text("Hoy", maxLines = 1, softWrap = false) },
                    shape = RoundedCornerShape(16.dp)
                )

                FilterChip(
                    selected = isYesterday,
                    onClick = {
                        val currentCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                        val yestCal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -1)
                            set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                        }
                        selectedDateMillis = yestCal.timeInMillis
                    },
                    label = { Text("Ayer", maxLines = 1, softWrap = false) },
                    shape = RoundedCornerShape(16.dp)
                )

                FilterChip(
                    selected = isCustomDate,
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                                selectedDateMillis = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        val labelText = if (isCustomDate) {
                            SimpleDateFormat("d MMM yyyy", Locale("es", "MX")).format(Date(selectedDateMillis))
                        } else {
                            "Otra fecha"
                        }
                        Text(labelText, maxLines = 1, softWrap = false)
                    },
                    shape = RoundedCornerShape(16.dp)
                )

                // Chip: Hora (opcional)
                FilterChip(
                    selected = selectedTimeString != null,
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                        val currentHour = if (selectedTimeString != null) {
                            selectedTimeString!!.split(":").firstOrNull()?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
                        } else cal.get(Calendar.HOUR_OF_DAY)
                        val currentMinute = if (selectedTimeString != null) {
                            selectedTimeString!!.split(":").getOrNull(1)?.toIntOrNull() ?: cal.get(Calendar.MINUTE)
                        } else cal.get(Calendar.MINUTE)

                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val timeStr = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                                selectedTimeString = timeStr
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis
                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    set(Calendar.MINUTE, minute)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                selectedDateMillis = newCal.timeInMillis
                            },
                            currentHour,
                            currentMinute,
                            true
                        ).show()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = if (selectedTimeString != null) {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quitar hora",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { selectedTimeString = null }
                            )
                        }
                    } else null,
                    label = {
                        Text(
                            text = if (selectedTimeString != null) selectedTimeString!! else "Hora (opcional)",
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            if (mainTab != "SAVINGS") {
                Spacer(modifier = Modifier.height(16.dp))

                // Repetir: No repetir, Diario, Semanal, Mensual, Personalizado
                Text(
                    text = "Repetir",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val repeatOptions = listOf(
                    "NONE" to "No repetir",
                    "DAILY" to "Diario",
                    "WEEKLY" to "Semanal",
                    "MONTHLY" to "Mensual",
                    "CUSTOM" to "Personalizado"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((key, title) in repeatOptions) {
                        val isSelected = repeatOption == key
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                repeatOption = key
                            },
                            label = { Text(title, maxLines = 1, softWrap = false) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // If Personalizado is selected: Month day selection grid (1 to 31)
                if (repeatOption == "CUSTOM") {
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomRepeatMonthGrid(
                        selectedDays = customRepeatDaysSet,
                        onToggleDay = { day ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            customRepeatDaysSet = if (customRepeatDaysSet.contains(day)) {
                                customRepeatDaysSet - day
                            } else {
                                customRepeatDaysSet + day
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Save Button
            val isSavingsValid = mainTab != "SAVINGS" || selectedGoal != null
            val isRepeatValid = repeatOption != "CUSTOM" || customRepeatDaysSet.isNotEmpty()
            val isSaveEnabled = parsedAmount > 0.0 && isSavingsValid && isRepeatValid

            Button(
                onClick = {
                    if (isSaveEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val finalType = when (mainTab) {
                            "INCOME" -> "INCOME"
                            "SAVINGS" -> if (savingSubtype == "WITHDRAW") "SAVING_WITHDRAW" else "SAVING_DEPOSIT"
                            else -> "EXPENSE"
                        }
                        val sortedDays = customRepeatDaysSet.sorted().joinToString(",")
                        onSave(
                            finalType,
                            parsedAmount,
                            if (mainTab == "SAVINGS") null else existingCategory,
                            noteText,
                            selectedDateMillis,
                            if (mainTab == "SAVINGS") "NONE" else repeatOption,
                            if (mainTab == "SAVINGS") selectedGoal else null,
                            selectedTimeString,
                            if (repeatOption == "CUSTOM") sortedDays else ""
                        )
                    }
                },
                enabled = isSaveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeColor
                )
            ) {
                val buttonLabel = when {
                    initialTransaction != null -> "Guardar cambios"
                    mainTab == "INCOME" -> "Guardar ingreso"
                    mainTab == "SAVINGS" -> if (savingSubtype == "WITHDRAW") "Retirar del ahorro" else "Aportar al ahorro"
                    else -> "Guardar gasto"
                }
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Month grid with weekdays (L, M, M, J, V, S, D) to select multiple days for custom monthly recurrence
 */
@Composable
fun CustomRepeatMonthGrid(
    selectedDays: Set<Int>,
    onToggleDay: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp)
    ) {
        val headers = listOf("L", "M", "M", "J", "V", "S", "D")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            for (h in headers) {
                Text(
                    text = h,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val days = (1..31).toList()
        val chunks = days.chunked(7)
        for (chunk in chunks) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (day in chunk) {
                    val isSelected = selectedDays.contains(day)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { onToggleDay(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                for (empty in 0 until (7 - chunk.size)) {
                    Spacer(modifier = Modifier.size(36.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val summaryText = if (selectedDays.isEmpty()) {
            "Selecciona al menos un día del mes"
        } else {
            "Se repetirá los días ${selectedDays.sorted().joinToString(", ")}"
        }
        Text(
            text = summaryText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Section C & 2: Modal for registering shopping as expense
 * Two equal-sized halves: "Solo lo comprado" and "Toda la lista",
 * with name on top and amount below in small text, without breaking lines.
 * If no products purchased, selects "Toda la lista" automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterShoppingExpenseSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    categories: List<CategoryEntity>,
    currencySymbol: String,
    listName: String,
    purchasedTotal: Double,
    fullTotal: Double,
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: CategoryEntity?, note: String, dateMillis: Long, repeat: String, timeString: String?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val expenseColor = LocalExpenseColor.current
    val numberFont = LocalNumberFontFamily.current

    val hasPurchased = purchasedTotal > 0.0
    var registerMode by remember {
        mutableStateOf(if (hasPurchased) "PURCHASED" else "ALL")
    }

    var noteText by remember { mutableStateOf("Compras: $listName") }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedTimeString by remember { mutableStateOf<String?>(null) }
    var repeatOption by remember { mutableStateOf("NONE") }

    val amountToRegister = if (registerMode == "PURCHASED") purchasedTotal else fullTotal

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Registrar como gasto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selector de dos mitades iguales: nombre arriba y monto abajo en texto pequeño
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Solo lo comprado
                val isPurchasedSelected = registerMode == "PURCHASED"
                val purchasedEnabled = hasPurchased
                Surface(
                    onClick = {
                        if (purchasedEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            registerMode = "PURCHASED"
                        }
                    },
                    enabled = purchasedEnabled,
                    shape = RoundedCornerShape(18.dp),
                    color = if (isPurchasedSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (isPurchasedSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Solo lo comprado",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isPurchasedSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (purchasedEnabled) {
                                if (isPurchasedSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currencySymbol${CurrencyUtils.formatAmount(purchasedTotal, "").trim()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (purchasedEnabled) {
                                if (isPurchasedSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Toda la lista
                val isAllSelected = registerMode == "ALL"
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        registerMode = "ALL"
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (isAllSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Toda la lista",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currencySymbol${CurrencyUtils.formatAmount(fullTotal, "").trim()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Amount
            Text(
                text = "-$currencySymbol${CurrencyUtils.formatAmount(amountToRegister, "").trim()}",
                style = MaterialTheme.typography.displayLarge.copy(fontFamily = numberFont),
                fontWeight = FontWeight.Bold,
                color = expenseColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))



            // Nota
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Nota") },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Fecha y hora
            Text(
                text = "Fecha y hora",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val todayMillis = remember {
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis
            }
            val yesterdayMillis = remember {
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis
            }
            val dayFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
            val isToday = dayFormat.format(Date(selectedDateMillis)) == dayFormat.format(Date(todayMillis))
            val isYesterday = dayFormat.format(Date(selectedDateMillis)) == dayFormat.format(Date(yesterdayMillis))
            val isCustomDate = !isToday && !isYesterday

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isToday,
                    onClick = {
                        val currentCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                        val todayCal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                        }
                        selectedDateMillis = todayCal.timeInMillis
                    },
                    label = { Text("Hoy", maxLines = 1, softWrap = false) },
                    shape = RoundedCornerShape(16.dp)
                )

                FilterChip(
                    selected = isYesterday,
                    onClick = {
                        val currentCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                        val yestCal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -1)
                            set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                        }
                        selectedDateMillis = yestCal.timeInMillis
                    },
                    label = { Text("Ayer", maxLines = 1, softWrap = false) },
                    shape = RoundedCornerShape(16.dp)
                )

                FilterChip(
                    selected = isCustomDate,
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                                selectedDateMillis = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = {
                        val labelText = if (isCustomDate) {
                            SimpleDateFormat("d MMM yyyy", Locale("es", "MX")).format(Date(selectedDateMillis))
                        } else {
                            "Otra fecha"
                        }
                        Text(labelText, maxLines = 1, softWrap = false)
                    },
                    shape = RoundedCornerShape(16.dp)
                )

                // Chip: Hora (opcional)
                FilterChip(
                    selected = selectedTimeString != null,
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                        val currentHour = if (selectedTimeString != null) {
                            selectedTimeString!!.split(":").firstOrNull()?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
                        } else cal.get(Calendar.HOUR_OF_DAY)
                        val currentMinute = if (selectedTimeString != null) {
                            selectedTimeString!!.split(":").getOrNull(1)?.toIntOrNull() ?: cal.get(Calendar.MINUTE)
                        } else cal.get(Calendar.MINUTE)

                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val timeStr = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                                selectedTimeString = timeStr
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis
                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    set(Calendar.MINUTE, minute)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                selectedDateMillis = newCal.timeInMillis
                            },
                            currentHour,
                            currentMinute,
                            true
                        ).show()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = if (selectedTimeString != null) {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quitar hora",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { selectedTimeString = null }
                            )
                        }
                    } else null,
                    label = {
                        Text(
                            text = if (selectedTimeString != null) selectedTimeString!! else "Hora (opcional)",
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (amountToRegister > 0.0) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(amountToRegister, null, noteText, selectedDateMillis, repeatOption, selectedTimeString)
                    }
                },
                enabled = amountToRegister > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = expenseColor)
            ) {
                Text(
                    text = "Registrar como gasto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
