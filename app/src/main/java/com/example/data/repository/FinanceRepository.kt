package com.example.data.repository

import com.example.data.db.CategoryDao
import com.example.data.db.GoalDao
import com.example.data.db.TransactionDao
import com.example.data.model.CategoryEntity
import com.example.data.model.GoalEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val goalDao: GoalDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()

    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(startMillis, endMillis)
    }

    fun getTransactionsForGoal(goalId: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForGoal(goalId)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>) {
        transactionDao.insertTransactions(transactions)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteSeriesFromDate(repeatGroupId: String, dateMillis: Long) {
        transactionDao.deleteSeriesFromDate(repeatGroupId, dateMillis)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun updateSeriesFromDate(
        repeatGroupId: String,
        dateMillis: Long,
        amount: Double,
        note: String,
        categoryId: String,
        categoryName: String,
        categoryEmoji: String,
        timeString: String?
    ) {
        transactionDao.updateSeriesFromDate(
            repeatGroupId = repeatGroupId,
            dateMillis = dateMillis,
            amount = amount,
            note = note,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryEmoji = categoryEmoji,
            timeString = timeString
        )
    }

    suspend fun getAllRepeatingTransactions(): List<TransactionEntity> {
        return transactionDao.getAllRepeatingTransactions()
    }

    suspend fun getTransactionsByRepeatGroupId(repeatGroupId: String): List<TransactionEntity> {
        return transactionDao.getTransactionsByRepeatGroupId(repeatGroupId)
    }

    suspend fun insertCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun insertGoal(goal: GoalEntity): Long {
        return goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: GoalEntity) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.deleteGoal(goal)
        transactionDao.deleteTransactionsByGoalId(goal.id)
    }

    suspend fun ensureDefaultCategories() {
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertCategories(com.example.data.db.AppDatabase.INITIAL_CATEGORIES)
        }
    }

    /**
     * Generates a clean UTF-8 CSV with BOM (\uFEFF) for direct Microsoft Excel & Google Sheets compatibility.
     * Columns: Fecha, Hora, Tipo, Categoría, Meta, Nota, Monto.
     * Signed amount: positive for income/withdraw, negative for expense/deposit.
     * No internal IDs are exposed.
     */
    suspend fun generateCsvData(currencyCode: String): String {
        val transactions = transactionDao.getAllTransactionsList()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sb = StringBuilder()
        // Prepend UTF-8 BOM
        sb.append("\uFEFF")
        sb.append("Fecha,Hora,Tipo,Categoría,Meta,Nota,Monto\n")

        for (tx in transactions) {
            val dateStr = dateFormat.format(Date(tx.dateMillis))
            val timeStr = tx.timeString ?: ""
            val tipoStr = when (tx.type) {
                "INCOME" -> "Ingreso"
                "EXPENSE" -> "Gasto"
                "SAVING_DEPOSIT" -> "Aporte a meta"
                "SAVING_WITHDRAW" -> "Retiro de meta"
                else -> tx.type
            }
            val catStr = if (tx.categoryName.isNotBlank()) {
                "${tx.categoryEmoji} ${tx.categoryName}".trim()
            } else {
                "Sin categoría"
            }
            val metaStr = tx.goalName.ifBlank { "-" }
            val cleanNote = tx.note.replace("\"", "\"\"")
            val cleanCat = catStr.replace("\"", "\"\"")
            val cleanMeta = metaStr.replace("\"", "\"\"")

            // Signed amount: positive for income and saving withdraw; negative for expense and saving deposit
            val signedAmount = when (tx.type) {
                "INCOME", "SAVING_WITHDRAW" -> tx.amount
                else -> -tx.amount
            }

            sb.append("\"$dateStr\",\"$timeStr\",\"$tipoStr\",\"$cleanCat\",\"$cleanMeta\",\"$cleanNote\",$signedAmount\n")
        }
        return sb.toString()
    }
}
