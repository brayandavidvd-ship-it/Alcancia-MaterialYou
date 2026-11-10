package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val monthlyBudgetLimit: Double? = null,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)
