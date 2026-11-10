package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val emoji: String = "🎯",
    val targetAmount: Double,
    val deadlineDateMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
