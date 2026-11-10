package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.GoalEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.ShoppingListEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupSummary(
    val movementCount: Int,
    val listCount: Int,
    val categoryCount: Int,
    val goalCount: Int,
    val backupDateFormatted: String,
    val backupData: BackupData
)

data class BackupData(
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val goals: List<GoalEntity>,
    val shoppingListsWithItems: List<Pair<ShoppingListEntity, List<ShoppingItemEntity>>>,
    val settings: AppSettingsEntity?,
    val movementsGridView: Boolean,
    val categoriesGridView: Boolean,
    val goalsGridView: Boolean
)

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val prefs = context.getSharedPreferences("alcancia_ui_prefs", Context.MODE_PRIVATE)

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "Alcancia")
        root.put("version", 2)
        val now = System.currentTimeMillis()
        root.put("backupDate", now)
        root.put("backupDateFormatted", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(now)))

        // 1. Settings & UI prefs
        val settings = database.settingsDao().getSettingsSync() ?: AppSettingsEntity()
        val settingsObj = JSONObject().apply {
            put("themeMode", settings.themeMode)
            put("isAmoledBlack", settings.isAmoledBlack)
            put("useDynamicColors", settings.useDynamicColors)
            put("accentColorIndex", settings.accentColorIndex)
            put("customAccentColor", settings.customAccentColor)
            put("quickTheme", settings.quickTheme)
            put("cardStyle", settings.cardStyle)
            put("bgStyle", settings.bgStyle)
            put("cornerRadius", settings.cornerRadius)
            put("density", settings.density)
            put("numberFont", settings.numberFont)
            put("incomeColor", settings.incomeColor)
            put("expenseColor", settings.expenseColor)
            put("savingsColor", settings.savingsColor)
            put("isColorblindPreset", settings.isColorblindPreset)
            put("currencyCode", settings.currencyCode)
            put("currencySymbol", settings.currencySymbol)
            put("isLocalAiEnabled", settings.isLocalAiEnabled)
            put("localAiServerUrl", settings.localAiServerUrl)
            put("localAiModel", settings.localAiModel)
            put("isCloudAiEnabled", settings.isCloudAiEnabled)
            put("cloudAiConsentAccepted", settings.cloudAiConsentAccepted)
            put("cloudAiModel", settings.cloudAiModel)
            put("movementsGridView", prefs.getBoolean("movements_grid_view", false))
            put("categoriesGridView", prefs.getBoolean("categories_grid_view", false))
            put("goalsGridView", prefs.getBoolean("goals_grid_view", false))
        }
        root.put("settings", settingsObj)

        // 2. Categories
        val categories = database.categoryDao().getAllCategoriesList()
        val categoriesArray = JSONArray()
        for (cat in categories) {
            val catObj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("emoji", cat.emoji)
                put("colorHex", cat.colorHex)
                if (cat.monthlyBudgetLimit != null) {
                    put("monthlyBudgetLimit", cat.monthlyBudgetLimit)
                }
                put("isDefault", cat.isDefault)
                put("sortOrder", cat.sortOrder)
            }
            categoriesArray.put(catObj)
        }
        root.put("categories", categoriesArray)

        // 3. Goals
        val goals = database.goalDao().getAllGoalsSync()
        val goalsArray = JSONArray()
        for (g in goals) {
            val gObj = JSONObject().apply {
                put("id", g.id)
                put("name", g.name)
                put("emoji", g.emoji)
                put("targetAmount", g.targetAmount)
                if (g.deadlineDateMillis != null) {
                    put("deadlineDateMillis", g.deadlineDateMillis)
                }
                put("createdAt", g.createdAt)
            }
            goalsArray.put(gObj)
        }
        root.put("goals", goalsArray)

        // 4. Transactions
        val transactions = database.transactionDao().getAllTransactionsList()
        val transactionsArray = JSONArray()
        for (tx in transactions) {
            val txObj = JSONObject().apply {
                put("type", tx.type)
                put("amount", tx.amount)
                put("categoryId", tx.categoryId)
                put("categoryName", tx.categoryName)
                put("categoryEmoji", tx.categoryEmoji)
                put("note", tx.note)
                put("dateMillis", tx.dateMillis)
                put("repeatInterval", tx.repeatInterval)
                put("customRepeatDays", tx.customRepeatDays)
                if (tx.repeatGroupId != null) {
                    put("repeatGroupId", tx.repeatGroupId)
                }
                if (tx.timeString != null) {
                    put("timeString", tx.timeString)
                }
                if (tx.goalId != null) {
                    put("goalId", tx.goalId)
                }
                put("goalName", tx.goalName)
                put("createdAt", tx.createdAt)
            }
            transactionsArray.put(txObj)
        }
        root.put("transactions", transactionsArray)

        // 5. Shopping Lists & Items
        val lists = database.shoppingDao().getAllShoppingListsSync()
        val listsArray = JSONArray()
        for (list in lists) {
            val listObj = JSONObject().apply {
                put("name", list.name)
                if (list.budget != null) {
                    put("budget", list.budget)
                }
                put("createdAt", list.createdAt)
                val items = database.shoppingDao().getItemsForListSync(list.id)
                val itemsArray = JSONArray()
                for (item in items) {
                    val itemObj = JSONObject().apply {
                        put("name", item.name)
                        put("quantity", item.quantity)
                        put("unitPrice", item.unitPrice)
                        put("isPurchased", item.isPurchased)
                        put("createdAt", item.createdAt)
                    }
                    itemsArray.put(itemObj)
                }
                put("items", itemsArray)
            }
            listsArray.put(listObj)
        }
        root.put("shoppingLists", listsArray)

        root.toString(2)
    }

    suspend fun saveBackupToUri(uri: Uri, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(jsonString.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            recordBackupTimestamp()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun parseAndValidateBackup(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        sb.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo seleccionado."))

            val content = sb.toString()
            if (content.isBlank()) {
                return@withContext Result.failure(Exception("El archivo está vacío."))
            }

            val root = JSONObject(content)
            val app = root.optString("app", "")
            if (app != "Alcancia") {
                return@withContext Result.failure(Exception("El archivo seleccionado no es un respaldo válido de Alcancía."))
            }

            val backupDateFormatted = root.optString("backupDateFormatted", "Fecha desconocida")

            // Parse settings
            var settings: AppSettingsEntity? = null
            var movementsGrid = false
            var categoriesGrid = false
            var goalsGrid = false
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                settings = AppSettingsEntity(
                    themeMode = sObj.optString("themeMode", "SYSTEM"),
                    isAmoledBlack = sObj.optBoolean("isAmoledBlack", false),
                    useDynamicColors = sObj.optBoolean("useDynamicColors", false),
                    accentColorIndex = sObj.optInt("accentColorIndex", 0),
                    customAccentColor = sObj.optLong("customAccentColor", 0xFF2E7D32),
                    quickTheme = sObj.optString("quickTheme", "CUSTOM"),
                    cardStyle = sObj.optString("cardStyle", "TINT"),
                    bgStyle = sObj.optString("bgStyle", "DEFAULT"),
                    cornerRadius = sObj.optString("cornerRadius", "ROUNDED"),
                    density = sObj.optString("density", "COMFORTABLE"),
                    numberFont = sObj.optString("numberFont", "ROUNDED"),
                    incomeColor = sObj.optLong("incomeColor", 0xFF2E7D32),
                    expenseColor = sObj.optLong("expenseColor", 0xFFC62828),
                    savingsColor = sObj.optLong("savingsColor", 0xFF0288D1),
                    isColorblindPreset = sObj.optBoolean("isColorblindPreset", false),
                    currencyCode = sObj.optString("currencyCode", "MXN"),
                    currencySymbol = sObj.optString("currencySymbol", "$"),
                    isLocalAiEnabled = sObj.optBoolean("isLocalAiEnabled", false),
                    localAiServerUrl = sObj.optString("localAiServerUrl", "http://localhost:11434"),
                    localAiModel = sObj.optString("localAiModel", "llama3.2:1b"),
                    isCloudAiEnabled = sObj.optBoolean("isCloudAiEnabled", false),
                    cloudAiConsentAccepted = sObj.optBoolean("cloudAiConsentAccepted", false),
                    cloudAiModel = sObj.optString("cloudAiModel", "gemini-3.5-flash-lite")
                )
                movementsGrid = sObj.optBoolean("movementsGridView", false)
                categoriesGrid = sObj.optBoolean("categoriesGridView", false)
                goalsGrid = sObj.optBoolean("goalsGridView", false)
            }

            // Parse categories
            val categoriesList = mutableListOf<CategoryEntity>()
            if (root.has("categories")) {
                val arr = root.getJSONArray("categories")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val budget = if (o.has("monthlyBudgetLimit") && !o.isNull("monthlyBudgetLimit")) {
                        o.getDouble("monthlyBudgetLimit")
                    } else null
                    categoriesList.add(
                        CategoryEntity(
                            id = o.optString("id", "cat_${System.currentTimeMillis()}_$i"),
                            name = o.optString("name", "Categoría"),
                            emoji = o.optString("emoji", "🏷️"),
                            colorHex = o.optString("colorHex", "#FF8A65"),
                            monthlyBudgetLimit = budget,
                            isDefault = o.optBoolean("isDefault", false),
                            sortOrder = o.optInt("sortOrder", i)
                        )
                    )
                }
            }

            // Parse goals
            val goalsList = mutableListOf<GoalEntity>()
            if (root.has("goals")) {
                val arr = root.getJSONArray("goals")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val deadline = if (o.has("deadlineDateMillis") && !o.isNull("deadlineDateMillis")) {
                        o.getLong("deadlineDateMillis")
                    } else null
                    goalsList.add(
                        GoalEntity(
                            id = o.optLong("id", 0L),
                            name = o.optString("name", "Meta"),
                            emoji = o.optString("emoji", "🎯"),
                            targetAmount = o.optDouble("targetAmount", 1000.0),
                            deadlineDateMillis = deadline,
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse transactions
            val transactionsList = mutableListOf<TransactionEntity>()
            if (root.has("transactions")) {
                val arr = root.getJSONArray("transactions")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val goalId = if (o.has("goalId") && !o.isNull("goalId")) o.getLong("goalId") else null
                    val repeatGroupId = if (o.has("repeatGroupId") && !o.isNull("repeatGroupId")) o.getString("repeatGroupId") else null
                    val timeString = if (o.has("timeString") && !o.isNull("timeString")) o.getString("timeString") else null
                    transactionsList.add(
                        TransactionEntity(
                            type = o.optString("type", "EXPENSE"),
                            amount = o.optDouble("amount", 0.0),
                            categoryId = o.optString("categoryId", ""),
                            categoryName = o.optString("categoryName", ""),
                            categoryEmoji = o.optString("categoryEmoji", ""),
                            note = o.optString("note", ""),
                            dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                            repeatInterval = o.optString("repeatInterval", "NONE"),
                            customRepeatDays = o.optString("customRepeatDays", ""),
                            repeatGroupId = repeatGroupId,
                            timeString = timeString,
                            goalId = goalId,
                            goalName = o.optString("goalName", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse shopping lists & items
            val shoppingData = mutableListOf<Pair<ShoppingListEntity, List<ShoppingItemEntity>>>()
            if (root.has("shoppingLists")) {
                val arr = root.getJSONArray("shoppingLists")
                for (i in 0 until arr.length()) {
                    val lObj = arr.getJSONObject(i)
                    val budget = if (lObj.has("budget") && !lObj.isNull("budget")) lObj.getDouble("budget") else null
                    val list = ShoppingListEntity(
                        name = lObj.optString("name", "Lista"),
                        budget = budget,
                        createdAt = lObj.optLong("createdAt", System.currentTimeMillis())
                    )
                    val itemsList = mutableListOf<ShoppingItemEntity>()
                    if (lObj.has("items")) {
                        val iArr = lObj.getJSONArray("items")
                        for (j in 0 until iArr.length()) {
                            val itObj = iArr.getJSONObject(j)
                            itemsList.add(
                                ShoppingItemEntity(
                                    listId = 0L,
                                    name = itObj.optString("name", "Producto"),
                                    quantity = itObj.optDouble("quantity", 1.0),
                                    unitPrice = itObj.optDouble("unitPrice", 0.0),
                                    isPurchased = itObj.optBoolean("isPurchased", false),
                                    createdAt = itObj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                    shoppingData.add(Pair(list, itemsList))
                }
            }

            val data = BackupData(
                transactions = transactionsList,
                categories = categoriesList,
                goals = goalsList,
                shoppingListsWithItems = shoppingData,
                settings = settings,
                movementsGridView = movementsGrid,
                categoriesGridView = categoriesGrid,
                goalsGridView = goalsGrid
            )

            Result.success(
                BackupSummary(
                    movementCount = transactionsList.size,
                    listCount = shoppingData.size,
                    categoryCount = categoriesList.size,
                    goalCount = goalsList.size,
                    backupDateFormatted = backupDateFormatted,
                    backupData = data
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("El archivo no es un respaldo válido de Alcancía (${e.message ?: "error"})."))
        }
    }

    suspend fun applyRestore(data: BackupData, replaceAll: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            if (replaceAll) {
                database.transactionDao().clearAllTransactions()
                database.categoryDao().clearAllCategories()
                database.goalDao().clearAllGoals()
                database.shoppingDao().clearAllShoppingLists()
                database.shoppingDao().clearAllShoppingItems()

                database.categoryDao().insertCategories(data.categories)

                val goalOldToNewId = mutableMapOf<Long, Long>()
                for (g in data.goals) {
                    val oldId = g.id
                    val newId = database.goalDao().insertGoal(g.copy(id = 0L))
                    if (oldId > 0L) {
                        goalOldToNewId[oldId] = newId
                    }
                }

                for (tx in data.transactions) {
                    val mappedGoalId = if (tx.goalId != null && goalOldToNewId.containsKey(tx.goalId)) {
                        goalOldToNewId[tx.goalId]
                    } else {
                        tx.goalId
                    }
                    database.transactionDao().insertTransaction(tx.copy(id = 0L, goalId = mappedGoalId))
                }

                for ((list, items) in data.shoppingListsWithItems) {
                    val newListId = database.shoppingDao().insertShoppingList(list.copy(id = 0L))
                    for (item in items) {
                        database.shoppingDao().insertShoppingItem(item.copy(id = 0L, listId = newListId))
                    }
                }
            } else {
                // Combinar
                val existingCategories = database.categoryDao().getAllCategoriesList()
                val existingCatIds = existingCategories.map { it.id }.toSet()
                for (cat in data.categories) {
                    if (!existingCatIds.contains(cat.id)) {
                        database.categoryDao().insertCategory(cat)
                    }
                }

                val existingGoals = database.goalDao().getAllGoalsSync()
                val existingGoalNames = existingGoals.map { it.name.trim().lowercase() }.toSet()
                val goalOldToNewId = mutableMapOf<Long, Long>()
                for (g in data.goals) {
                    if (!existingGoalNames.contains(g.name.trim().lowercase())) {
                        val newId = database.goalDao().insertGoal(g.copy(id = 0L))
                        if (g.id > 0L) {
                            goalOldToNewId[g.id] = newId
                        }
                    } else {
                        val matched = existingGoals.find { it.name.trim().equals(g.name.trim(), ignoreCase = true) }
                        if (matched != null && g.id > 0L) {
                            goalOldToNewId[g.id] = matched.id
                        }
                    }
                }

                val existingTx = database.transactionDao().getAllTransactionsList()
                val existingSignatures = existingTx.map { "${it.type}_${it.amount}_${it.dateMillis}_${it.note}" }.toSet()
                for (tx in data.transactions) {
                    val sig = "${tx.type}_${tx.amount}_${tx.dateMillis}_${tx.note}"
                    if (!existingSignatures.contains(sig)) {
                        val mappedGoalId = if (tx.goalId != null && goalOldToNewId.containsKey(tx.goalId)) {
                            goalOldToNewId[tx.goalId]
                        } else {
                            tx.goalId
                        }
                        database.transactionDao().insertTransaction(tx.copy(id = 0L, goalId = mappedGoalId))
                    }
                }

                val existingLists = database.shoppingDao().getAllShoppingListsSync()
                val existingListNames = existingLists.map { it.name.trim().lowercase() }.toSet()
                for ((list, items) in data.shoppingListsWithItems) {
                    val listName = if (existingListNames.contains(list.name.trim().lowercase())) {
                        "${list.name} (Respaldo)"
                    } else {
                        list.name
                    }
                    val newListId = database.shoppingDao().insertShoppingList(list.copy(id = 0L, name = listName))
                    for (item in items) {
                        database.shoppingDao().insertShoppingItem(item.copy(id = 0L, listId = newListId))
                    }
                }
            }

            if (data.settings != null) {
                database.settingsDao().setSettings(data.settings)
                prefs.edit()
                    .putBoolean("movements_grid_view", data.movementsGridView)
                    .putBoolean("categories_grid_view", data.categoriesGridView)
                    .putBoolean("goals_grid_view", data.goalsGridView)
                    .apply()
            }

            recordBackupTimestamp()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun recordBackupTimestamp() {
        prefs.edit().putLong("last_backup_timestamp", System.currentTimeMillis()).apply()
    }

    fun getLastBackupFormatted(): String? {
        val ts = prefs.getLong("last_backup_timestamp", 0L)
        if (ts <= 0L) return null
        val format = SimpleDateFormat("d 'de' MMMM, HH:mm", Locale("es", "MX"))
        return format.format(Date(ts))
    }
}
