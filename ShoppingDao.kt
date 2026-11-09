package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.ShoppingListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_lists ORDER BY createdAt DESC")
    fun getAllShoppingLists(): Flow<List<ShoppingListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(list: ShoppingListEntity): Long

    @Update
    suspend fun updateShoppingList(list: ShoppingListEntity)

    @Delete
    suspend fun deleteShoppingList(list: ShoppingListEntity)

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY isPurchased ASC, id ASC")
    fun getItemsForList(listId: Long): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY id ASC")
    suspend fun getItemsForListSync(listId: Long): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItemEntity): Long

    @Update
    suspend fun updateShoppingItem(item: ShoppingItemEntity)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun deleteItemsForList(listId: Long)

    @Query("SELECT * FROM shopping_lists")
    suspend fun getAllShoppingListsSync(): List<ShoppingListEntity>

    @Query("DELETE FROM shopping_lists")
    suspend fun clearAllShoppingLists()

    @Query("DELETE FROM shopping_items")
    suspend fun clearAllShoppingItems()

    @Query("SELECT COUNT(*) FROM shopping_lists")
    suspend fun getListCount(): Int

    @Query("SELECT DISTINCT name FROM shopping_items ORDER BY id DESC LIMIT 50")
    fun getAllDistinctProductNames(): Flow<List<String>>

    @Query("SELECT DISTINCT name FROM shopping_items ORDER BY id DESC LIMIT 50")
    suspend fun getAllDistinctProductNamesSync(): List<String>

    @Query("SELECT unitPrice FROM shopping_items WHERE LOWER(name) = LOWER(:name) ORDER BY id DESC LIMIT 1")
    suspend fun getLastUnitPriceForProduct(name: String): Double?
}
