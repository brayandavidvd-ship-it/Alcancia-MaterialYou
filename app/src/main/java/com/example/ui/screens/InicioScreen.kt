package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.ui.components.AddTransactionBottomSheet
import com.example.ui.components.CurrencyUtils
import com.example.ui.theme.ColorblindExpenseColor
import com.example.ui.theme.ColorblindIncomeColor
import com.example.ui.viewmodel.AlcanciaViewModel
import com.example.ui.viewmodel.AppTab
import com.example.util.rememberVoiceDictationLauncher
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Paleta fija solo para Inicio (Regla 3).
 * No usa ningún Color(0x...) fuera de este objeto.
 */
object InicioPalette {
    data class Colors(
        val bg: Color,
        val s1: Color,
        val s2: Color,
        val s3: Color,
        val brand: Color,
        val onBrand: Color,
        val accent: Color,
        val ingreso: Color,
        val gasto: Color,
        val ahorro: Color,
        val t1: Color,
        val t2: Color
    )

    val Dark = Colors(
        bg = Color(0xFF05070B),
        s1 = Color(0xFF0B1118),
        s2 = Color(0xFF111A24),
        s3 = Color(0xFF172230),
        brand = Color(0xFF5D7396),
        onBrand = Color(0xFFFFFFFF),
        accent = Color(0xFFA9BFE0),
        ingreso = Color(0xFF5BD18A),
        gasto = Color(0xFFFF6B6B),
        ahorro = Color(0xFFB9A6FF),
        t1 = Color(0xFFE8EDF5),
        t2 = Color(0xFF93A1B5)
    )

    val Light = Colors(
        bg = Color(0xFFF6F8FC),
        s1 = Color(0xFFFFFFFF),
        s2 = Color(0xFFEEF2F8),
        s3 = Color(0xFFE3E9F2),
        brand = Color(0xFF3F5A82),
        onBrand = Color(0xFFFFFFFF),
        accent = Color(0xFF3F5A82),
        ingreso = Color(0xFF1E8E4E),
        gasto = Color(0xFFD64545),
        ahorro = Color(0xFF6B4FD1),
        t1 = Color(0xFF0B1118),
        t2 = Color(0xFF5B6778)
    )

    val AiGradient = listOf(
        Color(0xFF5D8BFF),
        Color(0xFFB9A6FF),
        Color(0xFF6FE3FF)
    )
}

private fun formatCurrencyNoDecimals(amount: Double, symbol: String): String {
    val formatter = NumberFormat.getNumberInstance(Locale("es", "MX")).apply {
        maximumFractionDigits = 0
    }
    return "$symbol${formatter.format(amount)}"
}

private data class SuggestionItem(
    val note: String,
    val amount: Double,
    val displayText: String
)

@Composable
internal fun AiVoiceMicButton(
    palette: InicioPalette.Colors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = palette.s2,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                InicioPalette.AiGradient.map {
                    it.copy(alpha = if (enabled) 0.72f else 0.30f)
                }
            )
        ),
        modifier = modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.MicNone,
                contentDescription = "Dictar por voz",
                tint = if (enabled) palette.accent else palette.t2,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    viewModel: AlcanciaViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val summary by viewModel.financialSummaryState.collectAsStateWithLifecycle()
    val categories by viewModel.categoriesState.collectAsStateWithLifecycle()
    val goals by viewModel.goalsState.collectAsStateWithLifecycle()
    val goalsWithProgress by viewModel.goalsWithProgressState.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactionsState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val selectedCalendar by viewModel.selectedCalendar.collectAsStateWithLifecycle()
    val isGridView by viewModel.isMovementsGridView.collectAsStateWithLifecycle()
    val pinnedGoalId by viewModel.pinnedGoalId.collectAsStateWithLifecycle()

    // Resuelve modo oscuro respetando Sistema, Claro y Oscuro de Personalización
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

    // Sheet y diálogos
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var isEditFutureMode by remember { mutableStateOf(false) }
    var preselectedType by remember { mutableStateOf("EXPENSE") }
    var suggestedAmount by remember { mutableStateOf<Double?>(null) }
    var suggestedNote by remember { mutableStateOf<String?>(null) }

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var contextMenuTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var repeatedEditChoiceTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var repeatedDeleteChoiceTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    // Mes en formato "Nov 2026" con abreviaturas fijas
    val monthText = remember(selectedCalendar) {
        val monthNames = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        val m = selectedCalendar.get(Calendar.MONTH)
        val y = selectedCalendar.get(Calendar.YEAR)
        "${monthNames.getOrElse(m) { "" }} $y"
    }

    // Regla de meta mostrada (Bloque B): meta FIJADA si existe (aunque esté completada 100%).
    // Si no hay o ya no existe: meta activa con aporte más reciente; si ninguna tiene, la primera; si no hay, null.
    val activeGoals = remember(goalsWithProgress) {
        goalsWithProgress.filter { it.savedAmount < it.goal.targetAmount }
    }
    val chosenGoal = remember(goalsWithProgress, activeGoals, allTransactions, pinnedGoalId) {
        val pinned = if (pinnedGoalId != -1L) goalsWithProgress.find { it.goal.id == pinnedGoalId } else null
        if (pinned != null) {
            pinned
        } else if (activeGoals.isNotEmpty()) {
            activeGoals.maxByOrNull { g ->
                allTransactions
                    .filter { it.goalId == g.goal.id && it.type == "SAVING_DEPOSIT" }
                    .maxOfOrNull { it.dateMillis } ?: Long.MIN_VALUE
            } ?: activeGoals.first()
        } else {
            null
        }
    }

    // Sugeridos (6.2): últimos 90 días, Gasto con nota no vacía, >= 2 ocurrencias, top 3
    val suggestedItems = remember(allTransactions) {
        val cutoff = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        val eligible = allTransactions.filter { tx ->
            tx.type == "EXPENSE" &&
            tx.note.isNotBlank() &&
            tx.dateMillis >= cutoff
        }
        val grouped = eligible.groupBy { Pair(it.note.trim().lowercase(Locale.ROOT), it.amount) }
        val repeated = grouped.filter { it.value.size >= 2 }
        val sorted = repeated.entries.sortedWith(
            compareByDescending<Map.Entry<Pair<String, Double>, List<TransactionEntity>>> { it.value.size }
                .thenByDescending { entry -> entry.value.maxOf { it.dateMillis } }
        ).take(3)

        sorted.map { entry ->
            val rawNote = entry.value.first().note.trim()
            val capNote = rawNote.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            val amount = entry.key.second
            val amountStr = if (amount % 1.0 == 0.0) {
                amount.toLong().toString()
            } else {
                String.format(Locale("es", "MX"), "%.2f", amount)
            }
            val text = "$capNote $$amountStr"
            SuggestionItem(note = capNote, amount = amount, displayText = text)
        }
    }

    val launchVoiceInput = rememberVoiceDictationLauncher(
        prompt = "Dime qué registrar…",
        onResult = { transcript -> viewModel.openAssistantChatWithVoice(transcript) },
        onUnavailable = { viewModel.openAssistantChatWithVoice(null) }
    )

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusBarTop + 8.dp,
                bottom = 124.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 4.1 Encabezado (una sola fila, 48.dp)
            item(key = "header_row") {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    val showBadge = maxWidth >= 340.dp
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showBadge) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(palette.s3),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = palette.accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "Alcancía",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.t1,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Cápsula de mes (altura 40.dp, s2, forma de cápsula)
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(CircleShape)
                                .background(palette.s2),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.previousMonth()
                                    }
                                    .testTag("prev_month_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Mes anterior",
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = monthText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.accent,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.nextMonth()
                                    }
                                    .testTag("next_month_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Mes siguiente",
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4.2 Saldo (SIN tarjeta de fondo)
            item(key = "saldo_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "SALDO DISPONIBLE",
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = palette.t2
                    )

                    val balanceFormatted = CurrencyUtils.formatAmount(summary.balance, settings.currencySymbol)
                    Text(
                        text = balanceFormatted,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.t1,
                        textAlign = TextAlign.Center
                    )

                    if (summary.totalSavingsMonth > 0.0) {
                        Surface(
                            shape = CircleShape,
                            color = palette.s3
                        ) {
                            Text(
                                text = "Ahorrado este mes: ${CurrencyUtils.formatAmount(summary.totalSavingsMonth, settings.currencySymbol)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.ahorro,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 4.3 Fila de mosaicos (Row con altura intrínseca y mínimo 156.dp, separación 8.dp)
            item(key = "tiles_row") {
                val density = LocalDensity.current
                val strokeWidthPx = remember(density) { with(density) { 9.dp.toPx() } }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .heightIn(min = 156.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Izquierda (weight 1f): mosaico de meta, forma 28.dp, fondo s1, centrado
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(28.dp))
                            .background(palette.s1)
                            .clickable { viewModel.selectTab(AppTab.METAS) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (chosenGoal != null) {
                            val targetAmt = chosenGoal.goal.targetAmount
                            val progress = if (targetAmt > 0.0) {
                                (chosenGoal.savedAmount / targetAmt).toFloat().coerceIn(0f, 1f)
                            } else 0f

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Canvas(modifier = Modifier.size(88.dp)) {
                                    val diameter = size.minDimension - strokeWidthPx
                                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                                    val arcSize = Size(diameter, diameter)

                                    // Pista completa en s3
                                    drawArc(
                                        color = palette.s3,
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                    )
                                    // Progreso en color ahorro, inicio -90f
                                    if (progress > 0f) {
                                        drawArc(
                                            color = palette.ahorro,
                                            startAngle = -90f,
                                            sweepAngle = 360f * progress,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.ahorro
                                )
                                Text(
                                    text = "Meta ${chosenGoal.goal.name}",
                                    fontSize = 11.sp,
                                    color = palette.t2,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val savedNoDec = formatCurrencyNoDecimals(chosenGoal.savedAmount, settings.currencySymbol)
                                val targetNoDec = formatCurrencyNoDecimals(chosenGoal.goal.targetAmount, settings.currencySymbol)
                                Text(
                                    text = "$savedNoDec de $targetNoDec",
                                    fontSize = 11.sp,
                                    color = palette.t2,
                                    maxLines = 1
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Crea tu primera meta",
                                    fontSize = 12.sp,
                                    color = palette.t2,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Derecha (weight 1f): Column con separación 8.dp y dos mosaicos de igual altura, forma 24.dp
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Mosaico Ingresos
                        val incomeText = CurrencyUtils.formatAmount(summary.totalIncome, settings.currencySymbol)
                        val incomeFontSize = when {
                            incomeText.length <= 10 -> 20.sp
                            incomeText.length <= 13 -> 17.sp
                            else -> 14.sp
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(palette.ingreso.copy(alpha = 0.10f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column(verticalArrangement = Arrangement.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = palette.ingreso,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ingresos",
                                        fontSize = 12.sp,
                                        color = palette.ingreso
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = incomeText,
                                    fontSize = incomeFontSize,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.ingreso,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Mosaico Gastos
                        val expenseText = CurrencyUtils.formatAmount(summary.totalExpense, settings.currencySymbol)
                        val expenseFontSize = when {
                            expenseText.length <= 10 -> 20.sp
                            expenseText.length <= 13 -> 17.sp
                            else -> 14.sp
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(palette.gasto.copy(alpha = 0.10f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column(verticalArrangement = Arrangement.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = palette.gasto,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Gastos",
                                        fontSize = 12.sp,
                                        color = palette.gasto
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = expenseText,
                                    fontSize = expenseFontSize,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.gasto,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // 4.4 Barra de IA (altura 56.dp, forma de cápsula)
            item(key = "ai_bar") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("ai_bar_inicio"),
                    shape = CircleShape,
                    color = palette.s1,
                    border = BorderStroke(1.5.dp, Brush.horizontalGradient(InicioPalette.AiGradient)),
                    onClick = { viewModel.openAssistantChat() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Asistente de IA",
                            tint = palette.ahorro,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Dime qué registrar…",
                            fontSize = 14.sp,
                            color = palette.t2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AiVoiceMicButton(
                            palette = palette,
                            onClick = { launchVoiceInput() },
                            modifier = Modifier.testTag("mic_bar_inicio_btn")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.brand)
                                .clickable {
                                    editingTransaction = null
                                    isEditFutureMode = false
                                    preselectedType = "EXPENSE"
                                    suggestedAmount = null
                                    suggestedNote = null
                                    showAddSheet = true
                                }
                                .testTag("add_movement_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Nuevo movimiento",
                                tint = palette.onBrand,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 4.5 Sugeridos (6.2)
            if (suggestedItems.isNotEmpty()) {
                item(key = "sugeridos_section") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Sugeridos",
                            fontSize = 11.sp,
                            color = palette.t2
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (sug in suggestedItems) {
                                Surface(
                                    shape = CircleShape,
                                    color = palette.s2,
                                    onClick = {
                                        editingTransaction = null
                                        isEditFutureMode = false
                                        preselectedType = "EXPENSE"
                                        suggestedAmount = sug.amount
                                        suggestedNote = sug.note
                                        showAddSheet = true
                                    }
                                ) {
                                    Text(
                                        text = sug.displayText,
                                        fontSize = 12.sp,
                                        color = palette.t1,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4.6 Movimientos: título y selector
            item(key = "movements_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Movimientos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.t1
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        onClick = { viewModel.toggleMovementsViewMode() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = if (isGridView) "Ver como lista" else "Ver como cuadrícula",
                                tint = palette.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Estado vacío o lista/cuadrícula por día
            if (summary.groupedTransactions.isEmpty()) {
                item(key = "empty_movements") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(palette.s1)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(palette.s3),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Tu alcancía está vacía este mes",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.t1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toca el botón (+) en la barra de IA para registrar tu primer gasto, ingreso o ahorro.",
                                fontSize = 12.sp,
                                color = palette.t2,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                for (group in summary.groupedTransactions) {
                    if (isGridView) {
                        item(key = "day_group_grid_${group.dateMillis}") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = group.dayLabel,
                                    fontSize = 14.sp,
                                    color = palette.accent,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                val chunks = group.transactions.chunked(3)
                                for (chunk in chunks) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        for (tx in chunk) {
                                            InicioGridTransactionTile(
                                                transaction = tx,
                                                currencySymbol = settings.currencySymbol,
                                                palette = palette,
                                                modifier = Modifier.weight(1f),
                                                onClick = { contextMenuTransaction = tx },
                                                onLongClick = {
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
                            }
                        }
                    } else {
                        item(key = "day_group_list_${group.dateMillis}") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = group.dayLabel,
                                    fontSize = 14.sp,
                                    color = palette.accent,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                val transactions = group.transactions
                                transactions.forEachIndexed { index, tx ->
                                    val shape = when {
                                        transactions.size == 1 -> RoundedCornerShape(20.dp)
                                        index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                                        index == transactions.size - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                                        else -> RoundedCornerShape(6.dp)
                                    }
                                    InicioListTransactionRow(
                                        transaction = tx,
                                        currencySymbol = settings.currencySymbol,
                                        palette = palette,
                                        shape = shape,
                                        onClick = { contextMenuTransaction = tx },
                                        onLongClick = {
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
        }
    }

    // Context Menu (Edit / Delete) on Long Press or Tap
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
        val displayTitle = if (tx.note.isNotBlank()) tx.note else neutralTitle

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
                                preselectedType = target.type
                                suggestedAmount = null
                                suggestedNote = null
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
                        colors = ButtonDefaults.buttonColors(containerColor = palette.gasto)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = palette.onBrand, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Borrar", color = palette.onBrand)
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

    // Diálogo de edición para movimientos repetidos
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
                            preselectedType = target.type
                            suggestedAmount = null
                            suggestedNote = null
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
                            preselectedType = target.type
                            suggestedAmount = null
                            suggestedNote = null
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

    // Diálogo de borrado para movimientos repetidos
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
                        colors = ButtonDefaults.buttonColors(containerColor = palette.gasto)
                    ) {
                        Text("Este y los siguientes", color = palette.onBrand)
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

    // Diálogo de confirmación para borrado simple
    if (transactionToDelete != null) {
        val tx = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("¿Borrar este movimiento?") },
            text = {
                val amountStr = CurrencyUtils.formatAmount(tx.amount, settings.currencySymbol)
                val descStr = if (tx.note.isNotBlank()) tx.note else (if (tx.type == "INCOME") "Ingreso" else if (tx.type.startsWith("SAVING")) "Ahorro" else "Gasto")
                Text("Se eliminará permanentemente $descStr por $amountStr.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.gasto),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Borrar", color = palette.onBrand, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Hoja "Nuevo movimiento" (AddTransactionBottomSheet)
    if (showAddSheet) {
        AddTransactionBottomSheet(
            categories = categories,
            goals = goals,
            currencySymbol = settings.currencySymbol,
            initialTransaction = editingTransaction,
            initialType = preselectedType,
            initialAmount = suggestedAmount,
            initialNote = suggestedNote,
            onDismiss = {
                showAddSheet = false
                editingTransaction = null
                isEditFutureMode = false
                suggestedAmount = null
                suggestedNote = null
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
                suggestedAmount = null
                suggestedNote = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InicioListTransactionRow(
    transaction: TransactionEntity,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isIncome = transaction.type == "INCOME"
    val isSaving = transaction.type.startsWith("SAVING")
    val isWithdraw = transaction.type == "SAVING_WITHDRAW"

    val typeColor = when {
        isIncome -> palette.ingreso
        isSaving -> palette.ahorro
        else -> palette.gasto
    }

    val typeIcon = when {
        isIncome -> Icons.Default.ArrowUpward
        isSaving -> Icons.Default.Savings
        else -> Icons.Default.ArrowDownward
    }

    val rawNote = transaction.note.trim()
    val defaultTitle = when {
        isIncome -> "Ingreso"
        isSaving -> if (isWithdraw) "Retiro de meta" else "Ahorro"
        else -> "Gasto"
    }
    val title = if (rawNote.isNotBlank()) rawNote else defaultTitle

    val sign = when {
        isIncome || isWithdraw -> "+"
        else -> "-"
    }
    val amountText = "$sign${CurrencyUtils.formatAmount(transaction.amount, currencySymbol)}"
    val subtitle = transaction.timeString?.trim() ?: ""

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.s1)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp)
            .testTag("tx_item_${transaction.id}")
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
                // Círculo de 36.dp con el ícono del tipo
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.t1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = palette.t2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = amountText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = typeColor
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InicioGridTransactionTile(
    transaction: TransactionEntity,
    currencySymbol: String,
    palette: InicioPalette.Colors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isIncome = transaction.type == "INCOME"
    val isSaving = transaction.type.startsWith("SAVING")
    val isWithdraw = transaction.type == "SAVING_WITHDRAW"

    val typeColor = when {
        isIncome -> palette.ingreso
        isSaving -> palette.ahorro
        else -> palette.gasto
    }

    val typeIcon = when {
        isIncome -> Icons.Default.ArrowUpward
        isSaving -> Icons.Default.Savings
        else -> Icons.Default.ArrowDownward
    }

    val sign = when {
        isIncome || isWithdraw -> "+"
        else -> "-"
    }
    val amountText = "$sign${CurrencyUtils.formatCompact(transaction.amount, currencySymbol)}"

    val rawNote = transaction.note.trim()
    val defaultTitle = when {
        isIncome -> "Ingreso"
        isSaving -> if (isWithdraw) "Retiro de meta" else "Ahorro"
        else -> "Gasto"
    }
    val title = if (rawNote.isNotBlank()) rawNote else defaultTitle

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(palette.s1)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag("grid_tx_${transaction.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Círculo de 36.dp con el ícono del tipo
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = amountText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = typeColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                color = palette.t2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
