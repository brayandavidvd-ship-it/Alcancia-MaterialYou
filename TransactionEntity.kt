package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String, // "EXPENSE", "INCOME", "SAVING_DEPOSIT", "SAVING_WITHDRAW"
    val amount: Double,
    val categoryId: String = "",
    val categoryName: String = "",
    val categoryEmoji: String = "",
    val note: String = "",
    val dateMillis: Long,
    val repeatInterval: String = "NONE", // "NONE", "DAILY", "WEEKLY", "MONTHLY", "CUSTOM"
    val customRepeatDays: String = "", // e.g. "5,15,30" for monthly recurrence on specific days
    val repeatGroupId: String? = null, // Shared UUID across instances of a repeating series
    val timeString: String? = null, // e.g. "14:30"
    val goalId: Long? = null,
    val goalName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
