package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val budget: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val listId: Long,
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val isPurchased: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val subtotal: Double
        get() = quantity * unitPrice
}
