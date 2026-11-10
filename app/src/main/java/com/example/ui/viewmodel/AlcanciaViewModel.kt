package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.GoalEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.ShoppingListEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.BackupData
import com.example.data.repository.BackupManager
import com.example.data.repository.BackupSummary
import com.example.data.repository.FinanceRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.ShoppingRepository
import com.example.ui.theme.QuickThemeOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONObject
import org.json.JSONArray
import com.example.data.security.SecureStorage
import com.example.data.service.GeminiService
import com.example.data.service.GeminiMessage
import com.example.data.service.GeminiTurnResult
import com.example.data.service.GeminiFunctionCallItem
import com.example.data.service.GeminiHistorySanitizer
import com.example.ui.model.ChatMessage
import com.example.ui.model.ChatActionProposal
import com.example.ui.model.MessageSender
import com.example.ui.model.ActionStatus

enum class AppTab {
    INICIO,
    METAS,
    COMPRAS,
    AJUSTES
}

data class CategorySpent(
    val category: CategoryEntity,
    val spent: Double,
    val percentageOfTotal: Float,
    val budgetLimit: Double?,
    val isLimitExceeded: Boolean
)

data class DayGroupedTransactions(
    val dayLabel: String,
    val dateMillis: Long,
    val transactions: List<TransactionEntity>
)

data class GoalWithProgress(
    val goal: GoalEntity,
    val savedAmount: Double,
    val progress: Float,
    val isCompleted: Boolean,
    val recommendedMonthlySaving: Double? = null
)

data class MonthlyFinanceComparison(
    val monthLabel: String,
    val income: Double,
    val expense: Double,
    val savings: Double
)

class AlcanciaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val financeRepository = FinanceRepository(database.transactionDao(), database.categoryDao(), database.goalDao())
    private val shoppingRepository = ShoppingRepository(database.shoppingDao())
    private val settingsRepository = SettingsRepository(database.settingsDao(), application.applicationContext)
    private val backupManager = BackupManager(application, database)
    private val prefs = application.getSharedPreferences("alcancia_ui_prefs", Context.MODE_PRIVATE)

    private val _lastBackupDate = MutableStateFlow(backupManager.getLastBackupFormatted())
    val lastBackupDate: StateFlow<String?> = _lastBackupDate.asStateFlow()

    fun refreshLastBackupDate() {
        _lastBackupDate.value = backupManager.getLastBackupFormatted()
    }

    // 4 Bottom navigation tabs
    private val _currentTab = MutableStateFlow(AppTab.INICIO)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // Grid vs List view preferences
    private val _isMovementsGridView = MutableStateFlow(prefs.getBoolean("movements_grid_view", false))
    val isMovementsGridView: StateFlow<Boolean> = _isMovementsGridView.asStateFlow()

    fun toggleMovementsViewMode() {
        val next = !_isMovementsGridView.value
        _isMovementsGridView.value = next
        prefs.edit().putBoolean("movements_grid_view", next).apply()
        vibrateDevice()
    }

    private val _isCategoriesGridView = MutableStateFlow(prefs.getBoolean("categories_grid_view", false))
    val isCategoriesGridView: StateFlow<Boolean> = _isCategoriesGridView.asStateFlow()

    fun toggleCategoriesViewMode() {
        val next = !_isCategoriesGridView.value
        _isCategoriesGridView.value = next
        prefs.edit().putBoolean("categories_grid_view", next).apply()
        vibrateDevice()
    }

    private val _isGoalsGridView = MutableStateFlow(prefs.getBoolean("goals_grid_view", false))
    val isGoalsGridView: StateFlow<Boolean> = _isGoalsGridView.asStateFlow()

    fun toggleGoalsViewMode() {
        val next = !_isGoalsGridView.value
        _isGoalsGridView.value = next
        prefs.edit().putBoolean("goals_grid_view", next).apply()
        vibrateDevice()
    }

    // Selected Month & Year for INICIO
    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    val selectedCalendar: StateFlow<Calendar> = _selectedCalendar.asStateFlow()

    fun previousMonth() {
        val cal = (_selectedCalendar.value.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        _selectedCalendar.value = cal
    }

    fun nextMonth() {
        val cal = (_selectedCalendar.value.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        _selectedCalendar.value = cal
    }

    fun setCurrentMonth() {
        _selectedCalendar.value = Calendar.getInstance()
    }

    // Settings
    val settingsState: StateFlow<AppSettingsEntity> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsRepository.getCachedSettings(application.applicationContext)
        )

    // All categories
    val categoriesState: StateFlow<List<CategoryEntity>> = financeRepository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All goals
    val goalsState: StateFlow<List<GoalEntity>> = financeRepository.allGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All transactions (overall, for goal progress computation)
    val allTransactionsState: StateFlow<List<TransactionEntity>> = financeRepository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Goals with calculated progress
    val goalsWithProgressState: StateFlow<List<GoalWithProgress>> = combine(goalsState, allTransactionsState) { goals, txList ->
        val goalSavingsMap = mutableMapOf<Long, Double>()
        for (tx in txList) {
            val gid = tx.goalId ?: continue
            val current = goalSavingsMap[gid] ?: 0.0
            if (tx.type == "SAVING_DEPOSIT") {
                goalSavingsMap[gid] = current + tx.amount
            } else if (tx.type == "SAVING_WITHDRAW") {
                goalSavingsMap[gid] = (current - tx.amount).coerceAtLeast(0.0)
            }
        }

        val now = System.currentTimeMillis()
        goals.map { g ->
            val saved = goalSavingsMap[g.id] ?: 0.0
            val target = if (g.targetAmount > 0.0) g.targetAmount else 1.0
            val progress = (saved / target).toFloat().coerceIn(0f, 1f)
            val isCompleted = saved >= g.targetAmount

            // Recommended monthly savings if deadline exists
            var recMonthly: Double? = null
            if (g.deadlineDateMillis != null && g.deadlineDateMillis > now && !isCompleted) {
                val diffMillis = g.deadlineDateMillis - now
                val monthsRemaining = (diffMillis / (1000L * 60 * 60 * 24 * 30.416)).coerceAtLeast(1.0)
                val remainingAmount = (g.targetAmount - saved).coerceAtLeast(0.0)
                recMonthly = remainingAmount / monthsRemaining
            }

            GoalWithProgress(
                goal = g,
                savedAmount = saved,
                progress = progress,
                isCompleted = isCompleted,
                recommendedMonthlySaving = recMonthly
            )
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Transactions for selected month
    @OptIn(ExperimentalCoroutinesApi::class)
    val monthTransactionsState: StateFlow<List<TransactionEntity>> = _selectedCalendar.flatMapLatest { cal ->
        val startCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        financeRepository.getTransactionsBetween(startCal.timeInMillis, endCal.timeInMillis)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Derived financial summary for Inicio: Income, Expense, Savings, and Balance
    val financialSummaryState = combine(monthTransactionsState, categoriesState) { txList, cats ->
        var income = 0.0
        var expense = 0.0
        var savingsDeposits = 0.0
        var savingsWithdraws = 0.0
        val categorySpentMap = mutableMapOf<String, Double>()
        var uncategorizedSpent = 0.0

        for (tx in txList) {
            when (tx.type) {
                "INCOME" -> income += tx.amount
                "EXPENSE" -> {
                    expense += tx.amount
                    if (tx.categoryId.isBlank()) {
                        uncategorizedSpent += tx.amount
                    } else {
                        val current = categorySpentMap[tx.categoryId] ?: 0.0
                        categorySpentMap[tx.categoryId] = current + tx.amount
                    }
                }
                "SAVING_DEPOSIT" -> savingsDeposits += tx.amount
                "SAVING_WITHDRAW" -> savingsWithdraws += tx.amount
            }
        }

        // Net savings this month (deposits - withdrawals)
        val netSavingsMonth = (savingsDeposits - savingsWithdraws).coerceAtLeast(0.0)

        // Balance = Income - Expense - SavingsDeposits + SavingsWithdraws
        val balance = income - expense - (savingsDeposits - savingsWithdraws)

        // Group by day for movements list
        val dayGroups = groupTransactionsByDay(txList)

        // Category breakdown: only EXPENSE
        val breakdown = cats.mapNotNull { cat ->
            val spent = categorySpentMap[cat.id] ?: 0.0
            val limit = cat.monthlyBudgetLimit
            val isExceeded = limit != null && limit > 0 && spent > limit
            if (spent > 0 || (limit != null && limit > 0)) {
                val percentage = if (expense > 0) (spent / expense).toFloat().coerceIn(0f, 1f) else 0f
                CategorySpent(
                    category = cat,
                    spent = spent,
                    percentageOfTotal = percentage,
                    budgetLimit = limit,
                    isLimitExceeded = isExceeded
                )
            } else null
        }.toMutableList()

        if (uncategorizedSpent > 0.0) {
            val percentage = if (expense > 0) (uncategorizedSpent / expense).toFloat().coerceIn(0f, 1f) else 0f
            breakdown.add(
                CategorySpent(
                    category = CategoryEntity(
                        id = "uncategorized",
                        name = "Sin categoría",
                        emoji = "🏷️",
                        colorHex = "#9E9E9E",
                        monthlyBudgetLimit = null,
                        isDefault = false,
                        sortOrder = 9999
                    ),
                    spent = uncategorizedSpent,
                    percentageOfTotal = percentage,
                    budgetLimit = null,
                    isLimitExceeded = false
                )
            )
        }

        breakdown.sortByDescending { it.spent }

        FinanceSummary(
            totalIncome = income,
            totalExpense = expense,
            totalSavingsMonth = netSavingsMonth,
            balance = balance,
            groupedTransactions = dayGroups,
            categoryBreakdown = breakdown
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceSummary()
    )

    // Monthly comparisons for last 6 months (used in in-app analytics charts)
    val monthlyHistoryState: StateFlow<List<MonthlyFinanceComparison>> = allTransactionsState.combine(_selectedCalendar) { txList, _ ->
        val result = mutableListOf<MonthlyFinanceComparison>()
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMM", Locale("es", "MX"))

        for (i in 5 downTo 0) {
            val c = (cal.clone() as Calendar).apply {
                add(Calendar.MONTH, -i)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = c.timeInMillis
            val end = (c.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            var inc = 0.0
            var exp = 0.0
            var sav = 0.0
            for (tx in txList) {
                if (tx.dateMillis in start..end) {
                    when (tx.type) {
                        "INCOME" -> inc += tx.amount
                        "EXPENSE" -> exp += tx.amount
                        "SAVING_DEPOSIT" -> sav += tx.amount
                        "SAVING_WITHDRAW" -> sav = (sav - tx.amount).coerceAtLeast(0.0)
                    }
                }
            }
            val label = monthFormat.format(c.time).replaceFirstChar { it.uppercase() }
            result.add(MonthlyFinanceComparison(label, inc, exp, sav))
        }
        result
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addTransaction(
        type: String,
        amount: Double,
        category: CategoryEntity?,
        note: String,
        dateMillis: Long,
        repeatInterval: String,
        goal: GoalEntity? = null,
        timeString: String? = null,
        customRepeatDays: String = "",
        repeatGroupId: String? = null
    ) {
        viewModelScope.launch {
            val assignedGroupId = if (repeatInterval != "NONE") {
                repeatGroupId ?: java.util.UUID.randomUUID().toString()
            } else null

            val entity = TransactionEntity(
                type = type,
                amount = amount,
                categoryId = category?.id ?: "",
                categoryName = category?.name ?: "",
                categoryEmoji = category?.emoji ?: "",
                note = note.trim(),
                dateMillis = dateMillis,
                repeatInterval = repeatInterval,
                customRepeatDays = customRepeatDays,
                repeatGroupId = assignedGroupId,
                timeString = timeString,
                goalId = goal?.id,
                goalName = goal?.name ?: ""
            )
            financeRepository.insertTransaction(entity)
            vibrateDevice()
        }
    }

    fun updateTransaction(
        id: Long,
        type: String,
        amount: Double,
        category: CategoryEntity?,
        note: String,
        dateMillis: Long,
        repeatInterval: String,
        goal: GoalEntity? = null,
        timeString: String? = null,
        customRepeatDays: String = "",
        repeatGroupId: String? = null
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                id = id,
                type = type,
                amount = amount,
                categoryId = category?.id ?: "",
                categoryName = category?.name ?: "",
                categoryEmoji = category?.emoji ?: "",
                note = note.trim(),
                dateMillis = dateMillis,
                repeatInterval = repeatInterval,
                customRepeatDays = customRepeatDays,
                repeatGroupId = repeatGroupId,
                timeString = timeString,
                goalId = goal?.id,
                goalName = goal?.name ?: ""
            )
            financeRepository.updateTransaction(entity)
            vibrateDevice()
        }
    }

    fun updateTransactionAndFuture(
        transaction: TransactionEntity,
        amount: Double,
        category: CategoryEntity?,
        note: String,
        timeString: String?
    ) {
        viewModelScope.launch {
            if (!transaction.repeatGroupId.isNullOrBlank()) {
                financeRepository.updateSeriesFromDate(
                    repeatGroupId = transaction.repeatGroupId,
                    dateMillis = transaction.dateMillis,
                    amount = amount,
                    note = note.trim(),
                    categoryId = category?.id ?: "",
                    categoryName = category?.name ?: "",
                    categoryEmoji = category?.emoji ?: "",
                    timeString = timeString
                )
            } else {
                financeRepository.updateTransaction(
                    transaction.copy(
                        amount = amount,
                        categoryId = category?.id ?: "",
                        categoryName = category?.name ?: "",
                        categoryEmoji = category?.emoji ?: "",
                        note = note.trim(),
                        timeString = timeString
                    )
                )
            }
            vibrateDevice()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            financeRepository.deleteTransaction(transaction)
            vibrateDevice()
        }
    }

    fun deleteTransactionAndFuture(transaction: TransactionEntity) {
        viewModelScope.launch {
            if (!transaction.repeatGroupId.isNullOrBlank()) {
                financeRepository.deleteSeriesFromDate(transaction.repeatGroupId, transaction.dateMillis)
            } else {
                financeRepository.deleteTransaction(transaction)
            }
            vibrateDevice()
        }
    }

    // Goals operations
    fun createGoal(name: String, emoji: String, targetAmount: Double, deadlineDateMillis: Long?) {
        viewModelScope.launch {
            val entity = GoalEntity(
                name = name.trim().ifBlank { "Mi meta" },
                emoji = emoji.ifBlank { "🎯" },
                targetAmount = if (targetAmount > 0.0) targetAmount else 1000.0,
                deadlineDateMillis = deadlineDateMillis
            )
            financeRepository.insertGoal(entity)
            vibrateDevice()
        }
    }

    fun updateGoal(goal: GoalEntity) {
        viewModelScope.launch {
            financeRepository.updateGoal(goal)
            vibrateDevice()
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            financeRepository.deleteGoal(goal)
            vibrateDevice()
        }
    }

    fun getTransactionsForGoal(goalId: Long) = financeRepository.getTransactionsForGoal(goalId)

    // Shopping state
    val shoppingListsState: StateFlow<List<ShoppingListEntity>> = shoppingRepository.allLists
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val productSuggestions: StateFlow<List<String>> = shoppingRepository.allProductSuggestions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedListId = MutableStateFlow<Long?>(null)
    val selectedListId: StateFlow<Long?> = _selectedListId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentShoppingItemsState: StateFlow<List<ShoppingItemEntity>> = _selectedListId.flatMapLatest { listId ->
        if (listId != null) {
            shoppingRepository.getItemsForList(listId)
        } else {
            MutableStateFlow(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectShoppingList(listId: Long) {
        _selectedListId.value = listId
    }

    fun createShoppingList(name: String, budget: Double? = null) {
        viewModelScope.launch {
            val id = shoppingRepository.createShoppingList(name, budget)
            _selectedListId.value = id
            vibrateDevice()
        }
    }

    fun updateShoppingList(list: ShoppingListEntity) {
        viewModelScope.launch {
            shoppingRepository.updateShoppingList(list)
            vibrateDevice()
        }
    }

    fun renameShoppingList(list: ShoppingListEntity, newName: String) {
        viewModelScope.launch {
            shoppingRepository.updateShoppingList(list.copy(name = newName.ifBlank { list.name }))
            vibrateDevice()
        }
    }

    fun duplicateShoppingList(listId: Long) {
        viewModelScope.launch {
            val newId = shoppingRepository.duplicateShoppingList(listId)
            _selectedListId.value = newId
            vibrateDevice()
        }
    }

    fun deleteShoppingList(list: ShoppingListEntity) {
        viewModelScope.launch {
            shoppingRepository.deleteShoppingList(list)
            if (_selectedListId.value == list.id) {
                _selectedListId.value = null
            }
            vibrateDevice()
        }
    }

    fun addShoppingItem(name: String, quantity: Double, unitPrice: Double) {
        val listId = _selectedListId.value ?: return
        viewModelScope.launch {
            val item = ShoppingItemEntity(
                listId = listId,
                name = name.trim().ifBlank { "Producto" },
                quantity = if (quantity > 0) quantity else 1.0,
                unitPrice = if (unitPrice >= 0) unitPrice else 0.0
            )
            shoppingRepository.insertItem(item)
            vibrateDevice()
        }
    }

    fun updateShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            shoppingRepository.updateItem(item)
            vibrateDevice()
        }
    }

    fun toggleShoppingItemPurchased(item: ShoppingItemEntity) {
        viewModelScope.launch {
            shoppingRepository.updateItem(item.copy(isPurchased = !item.isPurchased))
            vibrateDevice()
        }
    }

    fun duplicateShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            shoppingRepository.insertItem(item.copy(id = 0L, isPurchased = false))
            vibrateDevice()
        }
    }

    fun deleteShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            shoppingRepository.deleteItem(item)
            vibrateDevice()
        }
    }

    suspend fun getLastUnitPriceForProduct(name: String): Double? {
        return shoppingRepository.getLastUnitPriceForProduct(name)
    }

    fun registerShoppingListAsExpense(
        category: CategoryEntity?,
        totalAmount: Double,
        note: String,
        dateMillis: Long,
        repeatOption: String,
        timeString: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val assignedGroupId = if (repeatOption != "NONE") java.util.UUID.randomUUID().toString() else null
            val tx = TransactionEntity(
                type = "EXPENSE",
                amount = totalAmount,
                categoryId = category?.id ?: "",
                categoryName = category?.name ?: "",
                categoryEmoji = category?.emoji ?: "",
                note = note.ifBlank { "Compras" },
                dateMillis = dateMillis,
                repeatInterval = repeatOption,
                repeatGroupId = assignedGroupId,
                timeString = timeString
            )
            financeRepository.insertTransaction(tx)
            vibrateDevice()
            onSuccess()
        }
    }

    // Category settings
    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            financeRepository.updateCategory(category)
            vibrateDevice()
        }
    }

    fun addCategory(name: String, emoji: String, budgetLimit: Double?) {
        viewModelScope.launch {
            val id = "cat_" + System.currentTimeMillis()
            val newCat = CategoryEntity(
                id = id,
                name = name.trim().ifBlank { "Nueva categoría" },
                emoji = emoji.ifBlank { "🏷️" },
                colorHex = "#FF8A65",
                monthlyBudgetLimit = budgetLimit,
                isDefault = false,
                sortOrder = 99
            )
            financeRepository.insertCategory(newCat)
            vibrateDevice()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            financeRepository.deleteCategory(category)
            vibrateDevice()
        }
    }

    // Appearance & Settings Modifiers (Section B)
    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColors(enabled) }
    }

    fun setAccentPreset(index: Int) {
        viewModelScope.launch {
            settingsRepository.setAccentColorIndex(index)
            vibrateDevice()
        }
    }

    fun setCustomAccentColor(colorLong: Long) {
        viewModelScope.launch {
            settingsRepository.setAccentColorIndex(-1, colorLong)
            vibrateDevice()
        }
    }

    fun applyQuickTheme(option: QuickThemeOption) {
        viewModelScope.launch {
            settingsRepository.setQuickTheme(option)
            vibrateDevice()
        }
    }

    fun setCardStyle(style: String) {
        viewModelScope.launch {
            settingsRepository.setCardStyle(style)
            vibrateDevice()
        }
    }

    fun setBgStyle(style: String) {
        viewModelScope.launch {
            settingsRepository.setBgStyle(style)
            vibrateDevice()
        }
    }

    fun setCornerRadius(radius: String) {
        viewModelScope.launch {
            settingsRepository.setCornerRadius(radius)
            vibrateDevice()
        }
    }

    fun setDensity(density: String) {
        viewModelScope.launch {
            settingsRepository.setDensity(density)
            vibrateDevice()
        }
    }

    fun setNumberFont(font: String) {
        viewModelScope.launch {
            settingsRepository.setNumberFont(font)
            vibrateDevice()
        }
    }

    fun setFinancialColors(income: Long, expense: Long, savings: Long, isColorblind: Boolean = false) {
        viewModelScope.launch {
            settingsRepository.setFinancialColors(income, expense, savings, isColorblind)
            vibrateDevice()
        }
    }

    fun setColorblindPreset(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                settingsRepository.setFinancialColors(
                    income = 0xFF1976D2,
                    expense = 0xFFE65100,
                    savings = 0xFF7B1FA2,
                    isColorblind = true
                )
            } else {
                settingsRepository.setFinancialColors(
                    income = 0xFF2E7D32,
                    expense = 0xFFC62828,
                    savings = 0xFF0288D1,
                    isColorblind = false
                )
            }
            vibrateDevice()
        }
    }

    fun resetDefaultFinancialColors() {
        viewModelScope.launch {
            settingsRepository.setFinancialColors(0xFF2E7D32, 0xFFC62828, 0xFF0288D1, false)
            vibrateDevice()
        }
    }

    fun setCurrency(code: String, symbol: String) {
        viewModelScope.launch {
            settingsRepository.setCurrency(code, symbol)
            vibrateDevice()
        }
    }

    // Security (Fingerprint & PIN)
    fun setFingerprintEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFingerprintEnabled(enabled)
            vibrateDevice()
        }
    }

    fun setPinSecurity(enabled: Boolean, pin: String) {
        viewModelScope.launch {
            settingsRepository.setPinSecurity(enabled, pin)
            vibrateDevice()
        }
    }

    // Reminders
    fun setRemindersConfig(enabled: Boolean, time: String, frequency: String) {
        viewModelScope.launch {
            settingsRepository.setRemindersConfig(enabled, time, frequency)
            com.example.util.ReminderHelper.scheduleSavingsReminder(
                context = getApplication<Application>(),
                enabled = enabled,
                timeStr = time,
                frequency = frequency
            )
            vibrateDevice()
        }
    }

    // Haptic level
    fun setHapticLevel(level: String) {
        viewModelScope.launch {
            settingsRepository.setHapticLevel(level)
            vibrateDevice(level)
        }
    }

    // Local AI (Ollama)
    fun setLocalAiConfig(enabled: Boolean, serverUrl: String, model: String) {
        viewModelScope.launch {
            settingsRepository.setLocalAiConfig(enabled, serverUrl, model)
            vibrateDevice()
        }
    }

    fun testOllamaConnection(serverUrl: String, onResult: (Result<List<String>>) -> Unit) {
        viewModelScope.launch {
            val result = com.example.data.service.OllamaService.testConnection(serverUrl)
            onResult(result)
        }
    }

    fun suggestCategoryWithOllama(
        description: String,
        categories: List<CategoryEntity>,
        onResult: (CategoryEntity?) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val settings = settingsState.value
            val result = com.example.data.service.OllamaService.suggestCategory(
                serverUrl = settings.localAiServerUrl,
                model = settings.localAiModel,
                description = description,
                categories = categories
            )
            result.onSuccess { category ->
                onResult(category)
            }.onFailure { err ->
                onError(err.message ?: "No se pudo conectar con la IA local")
            }
        }
    }

    // Cloud AI (Gemini) configuration
    private val _hasGeminiApiKey = MutableStateFlow(SecureStorage.hasGeminiApiKey(application))
    val hasGeminiApiKey: StateFlow<Boolean> = _hasGeminiApiKey.asStateFlow()

    fun getGeminiApiKey(): String = SecureStorage.getGeminiApiKey(getApplication())

    fun saveGeminiApiKey(apiKey: String) {
        SecureStorage.saveGeminiApiKey(getApplication(), apiKey)
        _hasGeminiApiKey.value = apiKey.trim().isNotEmpty()
    }

    fun setCloudAiConfig(enabled: Boolean, consentAccepted: Boolean, model: String) {
        viewModelScope.launch {
            val oldModel = settingsState.value.cloudAiModel
            settingsRepository.setCloudAiConfig(enabled, consentAccepted, model)
            if (oldModel != model.trim()) {
                resetChatConversation()
            }
            vibrateDevice()
        }
    }

    fun setCloudAiEnabled(enabled: Boolean, consentAccepted: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCloudAiEnabled(enabled, consentAccepted)
            vibrateDevice()
        }
    }

    fun testGeminiConnection(apiKey: String, model: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = GeminiService.testConnection(apiKey, model)
            onResult(result)
        }
    }

    // Assistant Chat (In-Memory only, lives while app is open)
    private val _isAssistantChatOpen = MutableStateFlow(false)
    val isAssistantChatOpen: StateFlow<Boolean> = _isAssistantChatOpen.asStateFlow()

    fun openAssistantChat() {
        _isAssistantChatOpen.value = true
        vibrateDevice()
    }

    fun closeAssistantChat() {
        val pendingMsg = _chatMessages.value.firstOrNull { it.actionProposal?.status == ActionStatus.PENDING }
        if (pendingMsg != null && pendingMsg.actionProposal != null) {
            updateProposalStatus(pendingMsg.actionProposal.id, ActionStatus.CANCELLED, "Cancelado")
            val pendingCalls = pendingMsg.actionProposal.allCalls.ifEmpty {
                listOf(GeminiFunctionCallItem(pendingMsg.actionProposal.toolName, pendingMsg.actionProposal.arguments, false))
            }
            val cancelParts = pendingCalls.map {
                GeminiHistorySanitizer.createCancelledResponse(it.name, "cancelado_por_el_usuario")
            }
            apiHistory.add(GeminiMessage(role = "user", parts = cancelParts))
            apiHistory.add(GeminiMessage(role = "model", parts = listOf(JSONObject().put("text", "Entendido, acción cancelada."))))
        }
        val sanitized = GeminiHistorySanitizer.sanitize(apiHistory)
        apiHistory.clear()
        apiHistory.addAll(sanitized)
        _isAssistantChatOpen.value = false
    }

    // UI-only chat messages (list A)
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatTyping = MutableStateFlow(false)
    val isChatTyping: StateFlow<Boolean> = _isChatTyping.asStateFlow()

    private val _chatErrorMessage = MutableStateFlow<String?>(null)
    val chatErrorMessage: StateFlow<String?> = _chatErrorMessage.asStateFlow()

    private val _chatErrorDetail = MutableStateFlow<String?>(null)
    val chatErrorDetail: StateFlow<String?> = _chatErrorDetail.asStateFlow()

    // API-only Content history (list B) - NEVER reconstructed from UI messages
    private val apiHistory = mutableListOf<GeminiMessage>()
    private var chatJob: Job? = null

    fun stopGeneration() {
        chatJob?.cancel()
        chatJob = null
        _isChatTyping.value = false
        val sanitized = GeminiHistorySanitizer.sanitize(apiHistory)
        apiHistory.clear()
        apiHistory.addAll(sanitized)
        vibrateDevice()
    }

    fun resetChatConversation() {
        chatJob?.cancel()
        chatJob = null
        _chatMessages.value = emptyList()
        apiHistory.clear()
        _chatErrorMessage.value = null
        _chatErrorDetail.value = null
        _isChatTyping.value = false
        vibrateDevice()
    }

    fun toggleCloudAiModel() {
        val currentModel = settingsState.value.cloudAiModel
        val newModel = if (currentModel == GeminiService.FLASH_MODEL) {
            GeminiService.DEFAULT_MODEL
        } else {
            GeminiService.FLASH_MODEL
        }
        selectCloudAiModel(newModel)
    }

    fun selectCloudAiModel(model: String) {
        val clean = GeminiService.normalizeModel(model)
        setCloudAiConfig(
            enabled = settingsState.value.isCloudAiEnabled,
            consentAccepted = settingsState.value.cloudAiConsentAccepted,
            model = clean
        )
        resetChatConversation()
    }

    fun retryLastTurn() {
        _chatErrorMessage.value = null
        _chatErrorDetail.value = null
        runGeminiChatTurn(isRetry = false)
    }

    fun regenerateLastResponse() {
        val msgs = _chatMessages.value
        val lastUserIdx = msgs.indexOfLast { it.sender == MessageSender.USER }
        if (lastUserIdx == -1) return

        // Prune UI messages to the last user message
        _chatMessages.value = msgs.take(lastUserIdx + 1)
        _chatErrorMessage.value = null
        _chatErrorDetail.value = null

        // Prune API history: keep up to the matching user message
        val lastUserText = msgs[lastUserIdx].text
        var cutIdx = -1
        for (i in apiHistory.indices.reversed()) {
            if (apiHistory[i].isUser()) {
                val txt = apiHistory[i].parts.firstOrNull { it.has("text") }?.optString("text")
                if (txt == lastUserText) {
                    cutIdx = i
                    break
                }
            }
        }
        if (cutIdx != -1) {
            val pruned = apiHistory.take(cutIdx + 1)
            apiHistory.clear()
            apiHistory.addAll(pruned)
        }

        runGeminiChatTurn(isRetry = false)
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isChatTyping.value) return

        // Rule 4: If a confirmation card was pending, resolve it as cancelled first
        val pendingMsg = _chatMessages.value.firstOrNull { it.actionProposal?.status == ActionStatus.PENDING }
        if (pendingMsg != null && pendingMsg.actionProposal != null) {
            updateProposalStatus(pendingMsg.actionProposal.id, ActionStatus.CANCELLED, "Cancelado")
            val pendingCalls = pendingMsg.actionProposal.allCalls.ifEmpty {
                listOf(GeminiFunctionCallItem(pendingMsg.actionProposal.toolName, pendingMsg.actionProposal.arguments, false))
            }
            val cancelParts = pendingCalls.map {
                GeminiHistorySanitizer.createCancelledResponse(it.name, "cancelado_por_el_usuario")
            }
            apiHistory.add(GeminiMessage(role = "user", parts = cancelParts))
            apiHistory.add(GeminiMessage(role = "model", parts = listOf(JSONObject().put("text", "Entendido, acción cancelada."))))
        }

        val userMsg = ChatMessage(sender = MessageSender.USER, text = trimmed)
        _chatMessages.value = _chatMessages.value + userMsg
        _chatErrorMessage.value = null
        _chatErrorDetail.value = null

        val geminiUserMsg = GeminiMessage(
            role = "user",
            parts = listOf(JSONObject().put("text", trimmed))
        )
        apiHistory.add(geminiUserMsg)

        runGeminiChatTurn(isRetry = false)
    }

    private fun runGeminiChatTurn(maxSteps: Int = 6, isRetry: Boolean = false) {
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            _isChatTyping.value = true
            var steps = 0

            while (steps < maxSteps) {
                steps++
                val apiKey = getGeminiApiKey()
                val currentSettings = settingsState.value
                val currency = currentSettings.currencySymbol

                // Rule 5: Sanitize history before each API call
                val sanitized = GeminiHistorySanitizer.sanitize(apiHistory)
                apiHistory.clear()
                apiHistory.addAll(sanitized)

                val result = GeminiService.generateContent(
                    apiKey = apiKey,
                    model = currentSettings.cloudAiModel,
                    currencySymbol = currency,
                    history = apiHistory
                )

                when (result) {
                    is GeminiTurnResult.TextResponse -> {
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = result.text
                        )
                        apiHistory.add(
                            GeminiMessage(
                                role = "model",
                                parts = result.rawModelParts.ifEmpty {
                                    listOf(JSONObject().put("text", result.text))
                                }
                            )
                        )
                        _isChatTyping.value = false
                        return@launch
                    }
                    is GeminiTurnResult.QueryFunctionCall -> {
                        // Keep raw model parts intact with thoughtSignature
                        apiHistory.add(
                            GeminiMessage(
                                role = "model",
                                parts = result.rawModelParts
                            )
                        )

                        // Rule 6: Read-only query tools execute automatically without card
                        val responseParts = mutableListOf<JSONObject>()
                        val callsToExecute = result.allCalls.ifEmpty {
                            listOf(GeminiFunctionCallItem(result.functionName, result.arguments, true))
                        }

                        for (call in callsToExecute) {
                            val queryResultJson = executeQueryTool(call.name, call.arguments)
                            responseParts.add(
                                JSONObject().apply {
                                    put("functionResponse", JSONObject().apply {
                                        put("name", call.name)
                                        put("response", JSONObject().apply {
                                            put("resultado", queryResultJson)
                                        })
                                    })
                                }
                            )
                        }

                        // Rule 2: User turn with matching functionResponses for all calls in previous turn
                        apiHistory.add(
                            GeminiMessage(
                                role = "user",
                                parts = responseParts
                            )
                        )
                    }
                    is GeminiTurnResult.ActionProposalCall -> {
                        val proposal = ChatActionProposal(
                            id = result.proposalId,
                            toolName = result.toolName,
                            summary = result.summary,
                            isDestructive = result.isDestructive,
                            arguments = result.arguments,
                            status = ActionStatus.PENDING,
                            allCalls = result.allCalls
                        )
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "",
                            actionProposal = proposal
                        )

                        // Keep raw model parts intact with thoughtSignature
                        apiHistory.add(
                            GeminiMessage(
                                role = "model",
                                parts = result.rawModelParts
                            )
                        )

                        _isChatTyping.value = false
                        return@launch
                    }
                    is GeminiTurnResult.Error -> {
                        // Rule 7: If API returns 400 for turn order, sanitize and retry ONCE transparently
                        if (result.isTurnOrderError && !isRetry) {
                            val fixed = GeminiHistorySanitizer.sanitize(apiHistory)
                            apiHistory.clear()
                            apiHistory.addAll(fixed)
                            runGeminiChatTurn(maxSteps = maxSteps, isRetry = true)
                            return@launch
                        }

                        _chatErrorMessage.value = result.message
                        _chatErrorDetail.value = result.technicalDetail ?: result.message
                        _isChatTyping.value = false
                        return@launch
                    }
                }
            }
            _isChatTyping.value = false
        }
    }

    private fun updateProposalStatus(proposalId: String, status: ActionStatus, resultText: String?) {
        _chatMessages.value = _chatMessages.value.map { msg ->
            if (msg.actionProposal?.id == proposalId) {
                msg.copy(actionProposal = msg.actionProposal.copy(status = status, resultText = resultText))
            } else {
                msg
            }
        }
    }

    fun confirmActionProposal(proposal: ChatActionProposal) {
        viewModelScope.launch {
            updateProposalStatus(proposal.id, ActionStatus.EXECUTING, "Ejecutando acción...")
            vibrateDevice()

            val result = executeActionTool(proposal.toolName, proposal.arguments)
            val resultStatus = if (result.success) "ejecutado" else "error"
            val resultMsg = if (result.success) "Acción ejecutada con éxito" else (result.errorMessage ?: "Ocurrió un error al ejecutar la acción")

            updateProposalStatus(proposal.id, if (result.success) ActionStatus.CONFIRMED else ActionStatus.CANCELLED, resultMsg)

            // Prepare functionResponses for all calls in that turn (Rule 2 & Rule 3)
            val calls = proposal.allCalls.ifEmpty {
                listOf(GeminiFunctionCallItem(proposal.toolName, proposal.arguments, false))
            }
            val responseParts = mutableListOf<JSONObject>()
            for (call in calls) {
                if (call.name == proposal.toolName) {
                    val respObj = JSONObject().apply {
                        put("status", resultStatus)
                        put("mensaje", resultMsg)
                        for ((k, v) in result.details) {
                            put(k, v)
                        }
                    }
                    responseParts.add(
                        JSONObject().apply {
                            put("functionResponse", JSONObject().apply {
                                put("name", call.name)
                                put("response", respObj)
                            })
                        }
                    )
                } else if (call.isQuery) {
                    val qResult = executeQueryTool(call.name, call.arguments)
                    responseParts.add(
                        JSONObject().apply {
                            put("functionResponse", JSONObject().apply {
                                put("name", call.name)
                                put("response", JSONObject().apply {
                                    put("resultado", qResult)
                                })
                            })
                        }
                    )
                } else {
                    responseParts.add(GeminiHistorySanitizer.createCancelledResponse(call.name, "cancelado_por_el_usuario"))
                }
            }

            apiHistory.add(
                GeminiMessage(
                    role = "user",
                    parts = responseParts
                )
            )

            runGeminiChatTurn(isRetry = false)
        }
    }

    fun cancelActionProposal(proposal: ChatActionProposal) {
        viewModelScope.launch {
            updateProposalStatus(proposal.id, ActionStatus.CANCELLED, "Cancelado")
            vibrateDevice()

            val calls = proposal.allCalls.ifEmpty {
                listOf(GeminiFunctionCallItem(proposal.toolName, proposal.arguments, false))
            }
            val responseParts = calls.map {
                GeminiHistorySanitizer.createCancelledResponse(it.name, "cancelado_por_el_usuario")
            }

            apiHistory.add(
                GeminiMessage(
                    role = "user",
                    parts = responseParts
                )
            )

            runGeminiChatTurn(isRetry = false)
        }
    }

    private suspend fun executeQueryTool(name: String, args: Map<String, Any?>): String = withContext(Dispatchers.IO) {
        when (name) {
            "consultar_movimientos" -> {
                val mes = args["mes"]?.toString()?.toIntOrNull()
                val anio = args["anio"]?.toString()?.toIntOrNull()
                val categoria = args["categoria"]?.toString()?.takeIf { it.isNotBlank() }
                val tipo = args["tipo"]?.toString()?.takeIf { it.isNotBlank() }
                val limite = args["limite"]?.toString()?.toIntOrNull() ?: 20

                val allList = database.transactionDao().getAllTransactionsList()
                val cal = Calendar.getInstance()
                val filtered = allList.filter { tx ->
                    cal.timeInMillis = tx.dateMillis
                    val txMes = cal.get(Calendar.MONTH) + 1
                    val txAnio = cal.get(Calendar.YEAR)
                    val matchMes = mes == null || txMes == mes
                    val matchAnio = anio == null || txAnio == anio
                    val matchCat = categoria == null || tx.categoryName.equals(categoria, ignoreCase = true)
                    val matchTipo = tipo == null || tx.type.equals(tipo, ignoreCase = true)
                    matchMes && matchAnio && matchCat && matchTipo
                }.take(limite)

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val arr = JSONArray()
                for (tx in filtered) {
                    arr.put(JSONObject().apply {
                        put("id", tx.id)
                        put("fecha", sdf.format(Date(tx.dateMillis)))
                        put("tipo", tx.type)
                        put("monto", tx.amount)
                        put("categoria", tx.categoryName)
                        put("nota", tx.note)
                    })
                }
                arr.toString()
            }
            "consultar_resumen" -> {
                val cal = Calendar.getInstance()
                val mes = args["mes"]?.toString()?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)
                val anio = args["anio"]?.toString()?.toIntOrNull() ?: cal.get(Calendar.YEAR)

                val allList = database.transactionDao().getAllTransactionsList()
                var inc = 0.0
                var exp = 0.0
                for (tx in allList) {
                    cal.timeInMillis = tx.dateMillis
                    if ((cal.get(Calendar.MONTH) + 1) == mes && cal.get(Calendar.YEAR) == anio) {
                        when (tx.type) {
                            "INCOME" -> inc += tx.amount
                            "EXPENSE" -> exp += tx.amount
                        }
                    }
                }
                JSONObject().apply {
                    put("mes", mes)
                    put("anio", anio)
                    put("totalIngresos", inc)
                    put("totalGastos", exp)
                    put("balance", inc - exp)
                }.toString()
            }
            "listar_categorias" -> {
                val cats = database.categoryDao().getAllCategoriesList()
                val arr = JSONArray()
                for (c in cats) {
                    arr.put(JSONObject().apply {
                        put("id", c.id)
                        put("nombre", c.name)
                        put("emoji", c.emoji)
                        put("limiteMensual", c.monthlyBudgetLimit)
                    })
                }
                arr.toString()
            }
            "listar_metas" -> {
                val goals = database.goalDao().getAllGoalsSync()
                val arr = JSONArray()
                for (g in goals) {
                    val txs = database.transactionDao().getTransactionsForGoalSync(g.id)
                    var saved = 0.0
                    for (t in txs) {
                        if (t.type == "SAVING_DEPOSIT") saved += t.amount
                        else if (t.type == "SAVING_WITHDRAW") saved -= t.amount
                    }
                    arr.put(JSONObject().apply {
                        put("id", g.id)
                        put("nombre", g.name)
                        put("emoji", g.emoji)
                        put("montoObjetivo", g.targetAmount)
                        put("ahorrado", saved)
                    })
                }
                arr.toString()
            }
            "listar_compras" -> {
                val lists = database.shoppingDao().getAllShoppingListsSync()
                val arr = JSONArray()
                for (l in lists) {
                    val items = database.shoppingDao().getItemsForListSync(l.id)
                    val itemsArr = JSONArray()
                    for (it in items) {
                        itemsArr.put(JSONObject().apply {
                            put("id", it.id)
                            put("nombre", it.name)
                            put("cantidad", it.quantity)
                            put("precioEstimado", it.unitPrice)
                            put("comprado", it.isPurchased)
                        })
                    }
                    arr.put(JSONObject().apply {
                        put("id", l.id)
                        put("nombre", l.name)
                        put("productos", itemsArr)
                    })
                }
                arr.toString()
            }
            else -> "{}"
        }
    }

    data class ToolExecutionResult(
        val success: Boolean,
        val details: Map<String, Any?> = emptyMap(),
        val errorMessage: String? = null
    )

    private suspend fun executeActionTool(name: String, args: Map<String, Any?>): ToolExecutionResult = withContext(Dispatchers.IO) {
        try {
            when (name) {
                "agregar_movimiento" -> {
                    val tipo = args["tipo"]?.toString() ?: "EXPENSE"
                    val monto = args["monto"]?.toString()?.toDoubleOrNull()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Monto no válido")
                    val nota = args["nota"]?.toString() ?: ""
                    val catName = args["categoria"]?.toString()?.trim() ?: "General"
                    val fechaStr = args["fecha"]?.toString()?.trim()

                    val cats = database.categoryDao().getAllCategoriesList()
                    val matchedCat = cats.firstOrNull { it.name.equals(catName, ignoreCase = true) }
                        ?: cats.firstOrNull()
                        ?: CategoryEntity("general", "General", "🏷️", "#FF8A65", null, true, 0)

                    val dateMillis = if (!fechaStr.isNullOrBlank()) {
                        try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fechaStr)?.time ?: System.currentTimeMillis()
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }
                    } else {
                        System.currentTimeMillis()
                    }

                    val tx = TransactionEntity(
                        type = tipo,
                        amount = monto,
                        categoryId = matchedCat.id,
                        categoryName = matchedCat.name,
                        categoryEmoji = matchedCat.emoji,
                        note = nota,
                        dateMillis = dateMillis
                    )
                    val insertedId = database.transactionDao().insertTransaction(tx)
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    ToolExecutionResult(
                        success = true,
                        details = mapOf(
                            "id" to insertedId,
                            "tipo" to tipo,
                            "monto" to monto,
                            "categoria" to matchedCat.name,
                            "nota" to nota,
                            "fecha" to sdf.format(dateMillis)
                        )
                    )
                }
                "editar_movimiento" -> {
                    val id = args["id"]?.toString()?.toLongOrNull()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "ID de movimiento no proporcionado")
                    val tx = database.transactionDao().getById(id)
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Movimiento no encontrado")
                    val monto = args["monto"]?.toString()?.toDoubleOrNull() ?: tx.amount
                    val nota = args["nota"]?.toString() ?: tx.note
                    val catName = args["categoria"]?.toString()?.trim()

                    var catId = tx.categoryId
                    var catEmoji = tx.categoryEmoji
                    var finalCatName = tx.categoryName
                    if (!catName.isNullOrBlank()) {
                        val cats = database.categoryDao().getAllCategoriesList()
                        val matched = cats.firstOrNull { it.name.equals(catName, ignoreCase = true) }
                        if (matched != null) {
                            catId = matched.id
                            finalCatName = matched.name
                            catEmoji = matched.emoji
                        }
                    }

                    val updated = tx.copy(amount = monto, note = nota, categoryId = catId, categoryName = finalCatName, categoryEmoji = catEmoji)
                    database.transactionDao().updateTransaction(updated)
                    ToolExecutionResult(
                        success = true,
                        details = mapOf(
                            "id" to id,
                            "monto" to monto,
                            "categoria" to finalCatName,
                            "nota" to nota
                        )
                    )
                }
                "eliminar_movimiento" -> {
                    val id = args["id"]?.toString()?.toLongOrNull()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "ID no válido")
                    database.transactionDao().deleteById(id)
                    ToolExecutionResult(success = true, details = mapOf("id" to id, "eliminado" to true))
                }
                "crear_categoria" -> {
                    val nombre = args["nombre"]?.toString()?.trim()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Nombre requerido")
                    val emoji = args["icono"]?.toString()?.trim() ?: "🏷️"
                    val id = "cat_" + System.currentTimeMillis()
                    val cat = CategoryEntity(
                        id = id,
                        name = nombre,
                        emoji = emoji,
                        colorHex = "#FF8A65",
                        monthlyBudgetLimit = null,
                        isDefault = false,
                        sortOrder = 99
                    )
                    database.categoryDao().insertCategory(cat)
                    ToolExecutionResult(success = true, details = mapOf("id" to id, "nombre" to nombre, "icono" to emoji))
                }
                "editar_categoria" -> {
                    val id = args["id"]?.toString()?.trim()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "ID no válido")
                    val cats = database.categoryDao().getAllCategoriesList()
                    val cat = cats.firstOrNull { it.id == id || it.name.equals(id, ignoreCase = true) }
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Categoría no encontrada")
                    val nuevoNombre = args["nuevoNombre"]?.toString()?.trim() ?: cat.name
                    database.categoryDao().updateCategory(cat.copy(name = nuevoNombre))
                    ToolExecutionResult(success = true, details = mapOf("id" to cat.id, "nombre" to nuevoNombre))
                }
                "eliminar_categoria" -> {
                    val id = args["id"]?.toString()?.trim()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "ID no válido")
                    val cats = database.categoryDao().getAllCategoriesList()
                    val cat = cats.firstOrNull { it.id == id || it.name.equals(id, ignoreCase = true) }
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Categoría no encontrada")
                    database.categoryDao().deleteCategory(cat)
                    ToolExecutionResult(success = true, details = mapOf("id" to cat.id, "nombre" to cat.name, "eliminado" to true))
                }
                "crear_lista_compras" -> {
                    val nombre = args["nombre"]?.toString()?.trim() ?: "Lista de compras"
                    val list = ShoppingListEntity(name = nombre)
                    val listId = database.shoppingDao().insertShoppingList(list)
                    ToolExecutionResult(success = true, details = mapOf("id" to listId, "nombre" to nombre))
                }
                "agregar_producto_compra" -> {
                    val prodName = args["nombreProducto"]?.toString()?.trim()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Nombre de producto requerido")
                    val cant = args["cantidad"]?.toString()?.toDoubleOrNull() ?: 1.0
                    val precio = args["precioEstimado"]?.toString()?.toDoubleOrNull() ?: 0.0
                    val listIdArg = args["listaId"]?.toString()?.toLongOrNull()

                    val targetListId = if (listIdArg != null && listIdArg > 0) {
                        listIdArg
                    } else {
                        val lists = database.shoppingDao().getAllShoppingListsSync()
                        lists.firstOrNull()?.id ?: database.shoppingDao().insertShoppingList(ShoppingListEntity(name = "Despensa"))
                    }

                    val item = ShoppingItemEntity(
                        listId = targetListId,
                        name = prodName,
                        quantity = cant,
                        unitPrice = precio
                    )
                    val itemId = database.shoppingDao().insertShoppingItem(item)
                    ToolExecutionResult(success = true, details = mapOf("id" to itemId, "nombre" to prodName, "cantidad" to cant, "precio" to precio))
                }
                "crear_meta" -> {
                    val nombre = args["nombre"]?.toString()?.trim()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Nombre de meta requerido")
                    val monto = args["montoObjetivo"]?.toString()?.toDoubleOrNull()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Monto objetivo requerido")
                    val emoji = args["emoji"]?.toString()?.trim()?.ifBlank { "🎯" } ?: "🎯"
                    val fechaLimiteStr = args["fechaLimite"]?.toString()?.trim()
                    val deadline = if (!fechaLimiteStr.isNullOrBlank()) {
                        try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fechaLimiteStr)?.time
                        } catch (_: Exception) { null }
                    } else null
                    val goal = GoalEntity(
                        name = nombre,
                        emoji = emoji,
                        targetAmount = monto,
                        deadlineDateMillis = deadline
                    )
                    val metaId = database.goalDao().insertGoal(goal)
                    ToolExecutionResult(success = true, details = mapOf("id" to metaId, "nombre" to nombre, "montoObjetivo" to monto, "emoji" to emoji))
                }
                "editar_meta" -> {
                    val metaId = args["id"]?.toString()?.toLongOrNull()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "ID de meta requerido")
                    val goal = database.goalDao().getGoalById(metaId)
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Meta no encontrada")
                    val nuevoNombre = args["nuevoNombre"]?.toString()?.trim() ?: goal.name
                    val nuevoMonto = args["nuevoMontoObjetivo"]?.toString()?.toDoubleOrNull() ?: goal.targetAmount
                    val nuevoEmoji = args["nuevoEmoji"]?.toString()?.trim()?.ifBlank { goal.emoji } ?: goal.emoji
                    val nuevaFechaStr = args["nuevaFechaLimite"]?.toString()?.trim()
                    val deadline = if (!nuevaFechaStr.isNullOrBlank()) {
                        try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(nuevaFechaStr)?.time
                        } catch (_: Exception) { goal.deadlineDateMillis }
                    } else goal.deadlineDateMillis

                    val updated = goal.copy(
                        name = nuevoNombre,
                        emoji = nuevoEmoji,
                        targetAmount = nuevoMonto,
                        deadlineDateMillis = deadline
                    )
                    database.goalDao().updateGoal(updated)
                    ToolExecutionResult(success = true, details = mapOf("id" to metaId, "nombre" to nuevoNombre, "montoObjetivo" to nuevoMonto))
                }
                "eliminar_meta" -> {
                    val metaId = args["id"]?.toString()?.toLongOrNull()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "ID de meta requerido")
                    val goal = database.goalDao().getGoalById(metaId)
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Meta no encontrada")
                    database.transactionDao().deleteTransactionsByGoalId(metaId)
                    database.goalDao().deleteGoal(goal)
                    ToolExecutionResult(success = true, details = mapOf("id" to metaId, "nombre" to goal.name, "eliminado" to true))
                }
                "aportar_a_meta" -> {
                    val metaId = args["metaId"]?.toString()?.toLongOrNull()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "ID de meta requerido")
                    val monto = args["monto"]?.toString()?.toDoubleOrNull()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Monto no válido")
                    val goals = database.goalDao().getAllGoalsSync()
                    val goal = goals.firstOrNull { it.id == metaId }
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Meta no encontrada")
                    val nota = args["nota"]?.toString()?.trim()?.ifBlank { "Aporte a meta: ${goal.name}" } ?: "Aporte a meta: ${goal.name}"

                    val tx = TransactionEntity(
                        type = "SAVING_DEPOSIT",
                        amount = monto,
                        categoryId = "ahorro",
                        categoryName = "Ahorro",
                        categoryEmoji = "💰",
                        note = nota,
                        dateMillis = System.currentTimeMillis(),
                        goalId = goal.id,
                        goalName = goal.name
                    )
                    val txId = database.transactionDao().insertTransaction(tx)
                    ToolExecutionResult(success = true, details = mapOf("id" to txId, "metaId" to goal.id, "metaNombre" to goal.name, "montoAportado" to monto))
                }
                "retirar_de_meta" -> {
                    val metaId = args["metaId"]?.toString()?.toLongOrNull()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "ID de meta requerido")
                    val monto = args["monto"]?.toString()?.toDoubleOrNull()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Monto no válido")
                    val goals = database.goalDao().getAllGoalsSync()
                    val goal = goals.firstOrNull { it.id == metaId }
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Meta no encontrada")
                    val nota = args["nota"]?.toString()?.trim()?.ifBlank { "Retiro de meta: ${goal.name}" } ?: "Retiro de meta: ${goal.name}"

                    val tx = TransactionEntity(
                        type = "SAVING_WITHDRAW",
                        amount = monto,
                        categoryId = "ahorro",
                        categoryName = "Ahorro",
                        categoryEmoji = "💸",
                        note = nota,
                        dateMillis = System.currentTimeMillis(),
                        goalId = goal.id,
                        goalName = goal.name
                    )
                    val txId = database.transactionDao().insertTransaction(tx)
                    ToolExecutionResult(success = true, details = mapOf("id" to txId, "metaId" to goal.id, "metaNombre" to goal.name, "montoRetirado" to monto))
                }
                "editar_producto_compra" -> {
                    val id = args["id"]?.toString()?.toLongOrNull()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "ID de producto requerido")
                    val item = database.shoppingDao().getItemById(id)
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Producto no encontrado")
                    val nuevoNombre = args["nuevoNombre"]?.toString()?.trim() ?: item.name
                    val nuevaCant = args["nuevaCantidad"]?.toString()?.toDoubleOrNull() ?: item.quantity
                    val nuevoPrecio = args["nuevoPrecio"]?.toString()?.toDoubleOrNull() ?: item.unitPrice
                    val comprado = args["comprado"]?.toString()?.toBooleanStrictOrNull() ?: item.isPurchased

                    database.shoppingDao().updateShoppingItem(
                        item.copy(
                            name = nuevoNombre,
                            quantity = nuevaCant,
                            unitPrice = nuevoPrecio,
                            isPurchased = comprado
                        )
                    )
                    ToolExecutionResult(success = true, details = mapOf("id" to id, "nombre" to nuevoNombre, "cantidad" to nuevaCant, "precio" to nuevoPrecio, "comprado" to comprado))
                }
                "eliminar_producto_compra" -> {
                    val id = args["id"]?.toString()?.toLongOrNull()
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "ID de producto requerido")
                    val item = database.shoppingDao().getItemById(id)
                        ?: return@withContext ToolExecutionResult(false, emptyMap(), "Producto no encontrado")
                    database.shoppingDao().deleteShoppingItem(item)
                    ToolExecutionResult(success = true, details = mapOf("id" to id, "eliminado" to true))
                }
                "borrar_todos_los_movimientos" -> {
                    val respaldar = args["crearRespaldoPrevio"]?.toString()?.toBooleanStrictOrNull() ?: true
                    if (respaldar) {
                        try {
                            backupManager.createBackupJson()
                        } catch (_: Exception) { }
                    }
                    database.transactionDao().clearAllTransactions()
                    ToolExecutionResult(success = true, details = mapOf("eliminados" to true))
                }
                "cambiar_moneda" -> {
                    val sym = args["nuevoSimbolo"]?.toString()?.trim() ?: "$"
                    val code = when (sym) {
                        "€" -> "EUR"
                        "$" -> "MXN"
                        "US$" -> "USD"
                        else -> "MXN"
                    }
                    settingsRepository.setCurrency(code, sym)
                    ToolExecutionResult(success = true, details = mapOf("nuevoSimbolo" to sym, "codigo" to code))
                }
                else -> ToolExecutionResult(false, emptyMap(), "Herramienta no reconocida")
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, emptyMap(), e.message ?: "Error al ejecutar la acción")
        }
    }

    // Cache management
    fun getAppCacheSizeBytes(context: Context): Long {
        return try {
            fun dirSize(dir: File?): Long {
                if (dir == null || !dir.exists()) return 0L
                var size = 0L
                dir.listFiles()?.forEach { file ->
                    size += if (file.isDirectory) dirSize(file) else file.length()
                }
                return size
            }
            dirSize(context.cacheDir) + dirSize(context.externalCacheDir)
        } catch (_: Exception) {
            0L
        }
    }

    fun clearAppCache(context: Context, onResult: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val initialSize = getAppCacheSizeBytes(context)
            try {
                fun deleteDir(dir: File?) {
                    if (dir == null || !dir.exists()) return
                    dir.listFiles()?.forEach { file ->
                        if (file.isDirectory) deleteDir(file)
                        file.delete()
                    }
                }
                deleteDir(context.cacheDir)
                deleteDir(context.externalCacheDir)
            } catch (_: Exception) {
                // ignore
            }
            val freed = initialSize.coerceAtLeast(0L)
            withContext(Dispatchers.Main) {
                vibrateDevice()
                onResult(freed)
            }
        }
    }

    // Export CSV with BOM (Section G)
    fun exportCsv(context: Context, onShareIntentReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val currentSettings = settingsState.value
            val csvText = financeRepository.generateCsvData(currentSettings.currencyCode)
            try {
                val cacheDir = context.cacheDir
                val file = File(cacheDir, "alcancia_movimientos.csv")
                file.writeText(csvText, Charsets.UTF_8)

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Reporte de movimientos - Alcancía")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Exportar movimientos CSV")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                onShareIntentReady(chooser)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Reporte de movimientos - Alcancía")
                    putExtra(Intent.EXTRA_TEXT, csvText)
                }
                val chooser = Intent.createChooser(intent, "Exportar movimientos CSV")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                onShareIntentReady(chooser)
            }
        }
    }

    suspend fun createBackupJson(): String = backupManager.createBackupJson()

    fun saveBackupToUri(uri: Uri, json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = backupManager.saveBackupToUri(uri, json)
            if (success) {
                refreshLastBackupDate()
                vibrateDevice()
            }
            onResult(success)
        }
    }

    fun validateBackup(uri: Uri, onResult: (Result<BackupSummary>) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.parseAndValidateBackup(uri)
            onResult(result)
        }
    }

    fun applyRestore(data: BackupData, replaceAll: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = backupManager.applyRestore(data, replaceAll)
            if (success) {
                refreshLastBackupDate()
                _isMovementsGridView.value = prefs.getBoolean("movements_grid_view", false)
                _isCategoriesGridView.value = prefs.getBoolean("categories_grid_view", false)
                _isGoalsGridView.value = prefs.getBoolean("goals_grid_view", false)
                vibrateDevice()
            }
            onResult(success)
        }
    }

    fun vibrateDevice(customIntensity: String? = null) {
        val level = customIntensity ?: settingsState.value.hapticLevel
        if (level == "OFF") return

        try {
            val context = getApplication<Application>()
            val durationMs: Long = when (level) {
                "LOW" -> 15L
                "HIGH" -> 50L
                else -> 25L // MEDIUM
            }
            val amplitude: Int = when (level) {
                "LOW" -> 60
                "HIGH" -> 255
                else -> 150 // MEDIUM
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                when (level) {
                    "LOW" -> vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    )
                    "HIGH" -> vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    )
                    else -> vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {
            // ignore
        }
    }

    suspend fun generatePendingRecurringTransactions() {
        withContext(Dispatchers.IO) {
            val allRepeating = financeRepository.getAllRepeatingTransactions()
            if (allRepeating.isEmpty()) return@withContext

            val groups = allRepeating.groupBy { it.repeatGroupId }
            val nowCal = Calendar.getInstance()
            val todayYear = nowCal.get(Calendar.YEAR)
            val todayMonth = nowCal.get(Calendar.MONTH)

            for ((groupId, txs) in groups) {
                if (groupId.isNullOrBlank()) continue
                val template = txs.minByOrNull { it.dateMillis } ?: continue
                val interval = template.repeatInterval
                if (interval == "NONE") continue

                val existingDatesSet = txs.map {
                    val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                    "${c.get(Calendar.YEAR)}_${c.get(Calendar.MONTH)}_${c.get(Calendar.DAY_OF_MONTH)}"
                }.toSet()

                val newTransactions = mutableListOf<TransactionEntity>()

                when (interval) {
                    "DAILY" -> {
                        val curCal = Calendar.getInstance().apply { timeInMillis = template.dateMillis }
                        curCal.add(Calendar.DAY_OF_YEAR, 1)
                        while (curCal.timeInMillis <= nowCal.timeInMillis) {
                            val key = "${curCal.get(Calendar.YEAR)}_${curCal.get(Calendar.MONTH)}_${curCal.get(Calendar.DAY_OF_MONTH)}"
                            if (!existingDatesSet.contains(key)) {
                                newTransactions.add(
                                    template.copy(
                                        id = 0L,
                                        dateMillis = curCal.timeInMillis,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                            }
                            curCal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                    "WEEKLY" -> {
                        val curCal = Calendar.getInstance().apply { timeInMillis = template.dateMillis }
                        curCal.add(Calendar.WEEK_OF_YEAR, 1)
                        while (curCal.timeInMillis <= nowCal.timeInMillis) {
                            val key = "${curCal.get(Calendar.YEAR)}_${curCal.get(Calendar.MONTH)}_${curCal.get(Calendar.DAY_OF_MONTH)}"
                            if (!existingDatesSet.contains(key)) {
                                newTransactions.add(
                                    template.copy(
                                        id = 0L,
                                        dateMillis = curCal.timeInMillis,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                            }
                            curCal.add(Calendar.WEEK_OF_YEAR, 1)
                        }
                    }
                    "MONTHLY" -> {
                        val startCal = Calendar.getInstance().apply { timeInMillis = template.dateMillis }
                        val targetDay = startCal.get(Calendar.DAY_OF_MONTH)
                        val curCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }

                        while (curCal.get(Calendar.YEAR) < todayYear || (curCal.get(Calendar.YEAR) == todayYear && curCal.get(Calendar.MONTH) <= todayMonth)) {
                            val maxDays = curCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            val actualDay = minOf(targetDay, maxDays)
                            curCal.set(Calendar.DAY_OF_MONTH, actualDay)

                            if (curCal.timeInMillis <= nowCal.timeInMillis) {
                                val key = "${curCal.get(Calendar.YEAR)}_${curCal.get(Calendar.MONTH)}_${curCal.get(Calendar.DAY_OF_MONTH)}"
                                if (!existingDatesSet.contains(key)) {
                                    newTransactions.add(
                                        template.copy(
                                            id = 0L,
                                            dateMillis = curCal.timeInMillis,
                                            createdAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                            curCal.add(Calendar.MONTH, 1)
                        }
                    }
                    "CUSTOM" -> {
                        val daysList = template.customRepeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                        if (daysList.isNotEmpty()) {
                            val startCal = Calendar.getInstance().apply { timeInMillis = template.dateMillis }
                            val curCal = (startCal.clone() as Calendar)

                            while (curCal.get(Calendar.YEAR) < todayYear || (curCal.get(Calendar.YEAR) == todayYear && curCal.get(Calendar.MONTH) <= todayMonth)) {
                                val maxDays = curCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                for (day in daysList) {
                                    val actualDay = minOf(day, maxDays)
                                    val dayCal = (curCal.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, actualDay)
                                    }
                                    if (dayCal.timeInMillis > template.dateMillis && dayCal.timeInMillis <= nowCal.timeInMillis) {
                                        val key = "${dayCal.get(Calendar.YEAR)}_${dayCal.get(Calendar.MONTH)}_${dayCal.get(Calendar.DAY_OF_MONTH)}"
                                        if (!existingDatesSet.contains(key)) {
                                            newTransactions.add(
                                                template.copy(
                                                    id = 0L,
                                                    dateMillis = dayCal.timeInMillis,
                                                    createdAt = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                    }
                                }
                                curCal.add(Calendar.MONTH, 1)
                            }
                        }
                    }
                }

                if (newTransactions.isNotEmpty()) {
                    financeRepository.insertTransactions(newTransactions)
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            financeRepository.ensureDefaultCategories()
            val listId = shoppingRepository.ensureDefaultList()
            if (_selectedListId.value == null) {
                _selectedListId.value = listId
            }
            generatePendingRecurringTransactions()
            val currentSettings = settingsState.value
            if (currentSettings.remindersEnabled) {
                com.example.util.ReminderHelper.scheduleSavingsReminder(
                    context = application.applicationContext,
                    enabled = true,
                    timeStr = currentSettings.reminderTime,
                    frequency = currentSettings.reminderFrequency
                )
            }
        }
    }
}

data class FinanceSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalSavingsMonth: Double = 0.0,
    val balance: Double = 0.0,
    val groupedTransactions: List<DayGroupedTransactions> = emptyList(),
    val categoryBreakdown: List<CategorySpent> = emptyList()
)

private fun groupTransactionsByDay(transactions: List<TransactionEntity>): List<DayGroupedTransactions> {
    val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val labelFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "MX"))
    val todayCal = Calendar.getInstance()
    val todayKey = dayFormat.format(todayCal.time)
    val yesterdayCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val yesterdayKey = dayFormat.format(yesterdayCal.time)

    val map = linkedMapOf<String, MutableList<TransactionEntity>>()
    val dateMap = mutableMapOf<String, Long>()

    for (tx in transactions) {
        val key = dayFormat.format(Date(tx.dateMillis))
        if (!map.containsKey(key)) {
            map[key] = mutableListOf()
            dateMap[key] = tx.dateMillis
        }
        map[key]?.add(tx)
    }

    return map.map { (key, list) ->
        val dateMillis = dateMap[key] ?: list.first().dateMillis
        val label = when (key) {
            todayKey -> "Hoy, " + SimpleDateFormat("d 'de' MMMM", Locale("es", "MX")).format(Date(dateMillis))
            yesterdayKey -> "Ayer, " + SimpleDateFormat("d 'de' MMMM", Locale("es", "MX")).format(Date(dateMillis))
            else -> {
                val formatted = labelFormat.format(Date(dateMillis))
                formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "MX")) else it.toString() }
            }
        }
        DayGroupedTransactions(
            dayLabel = label,
            dateMillis = dateMillis,
            transactions = list
        )
    }
}
