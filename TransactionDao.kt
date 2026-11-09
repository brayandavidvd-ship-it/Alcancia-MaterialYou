package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateMillis >= :startMillis AND dateMillis <= :endMillis ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE goalId = :goalId ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsForGoal(goalId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE goalId = :goalId ORDER BY dateMillis DESC, id DESC")
    suspend fun getTransactionsForGoalSync(goalId: Long): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE goalId = :goalId")
    suspend fun deleteTransactionsByGoalId(goalId: Long)

    // Series and Recurring Transactions support
    @Query("DELETE FROM transactions WHERE repeatGroupId = :repeatGroupId AND dateMillis >= :dateMillis")
    suspend fun deleteSeriesFromDate(repeatGroupId: String, dateMillis: Long)

    @Query("SELECT * FROM transactions WHERE repeatGroupId = :repeatGroupId ORDER BY dateMillis ASC")
    suspend fun getTransactionsByRepeatGroupId(repeatGroupId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE repeatInterval != 'NONE' AND repeatGroupId IS NOT NULL")
    suspend fun getAllRepeatingTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET amount = :amount, note = :note, categoryId = :categoryId, categoryName = :categoryName, categoryEmoji = :categoryEmoji, timeString = :timeString WHERE repeatGroupId = :repeatGroupId AND dateMillis >= :dateMillis")
    suspend fun updateSeriesFromDate(
        repeatGroupId: String,
        dateMillis: Long,
        amount: Double,
        note: String,
        categoryId: String,
        categoryName: String,
        categoryEmoji: String,
        timeString: String?
    )
}
