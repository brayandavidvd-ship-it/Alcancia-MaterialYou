package com.example.data.repository

import com.example.data.db.ShoppingDao
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.ShoppingListEntity
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(
    private val shoppingDao: ShoppingDao
) {
    val allLists: Flow<List<ShoppingListEntity>> = shoppingDao.getAllShoppingLists()
    val allProductSuggestions: Flow<List<String>> = shoppingDao.getAllDistinctProductNames()

    fun getItemsForList(listId: Long): Flow<List<ShoppingItemEntity>> {
        return shoppingDao.getItemsForList(listId)
    }

    suspend fun getItemsForListSync(listId: Long): List<ShoppingItemEntity> {
        return shoppingDao.getItemsForListSync(listId)
    }

    suspend fun createShoppingList(name: String, budget: Double? = null): Long {
        return shoppingDao.insertShoppingList(ShoppingListEntity(name = name.ifBlank { "Mi lista" }, budget = budget))
    }

    suspend fun updateShoppingList(list: ShoppingListEntity) {
        shoppingDao.updateShoppingList(list)
    }

    suspend fun deleteShoppingList(list: ShoppingListEntity) {
        shoppingDao.deleteItemsForList(list.id)
        shoppingDao.deleteShoppingList(list)
    }

    suspend fun duplicateShoppingList(listId: Long): Long {
        val originalList = shoppingDao.getAllShoppingListsSync().find { it.id == listId }
        val newName = if (originalList != null) "${originalList.name} (Copia)" else "Nueva copia"
        val newListId = shoppingDao.insertShoppingList(
            ShoppingListEntity(
                name = newName,
                budget = originalList?.budget
            )
        )
        val items = shoppingDao.getItemsForListSync(listId)
        for (item in items) {
            shoppingDao.insertShoppingItem(
                item.copy(id = 0L, listId = newListId, isPurchased = false)
            )
        }
        return newListId
    }

    suspend fun insertItem(item: ShoppingItemEntity): Long {
        return shoppingDao.insertShoppingItem(item)
    }

    suspend fun updateItem(item: ShoppingItemEntity) {
        shoppingDao.updateShoppingItem(item)
    }

    suspend fun deleteItem(item: ShoppingItemEntity) {
        shoppingDao.deleteShoppingItem(item)
    }

    suspend fun getLastUnitPriceForProduct(name: String): Double? {
        return shoppingDao.getLastUnitPriceForProduct(name.trim())
    }

    suspend fun ensureDefaultList(): Long {
        if (shoppingDao.getListCount() == 0) {
            val listId = shoppingDao.insertShoppingList(ShoppingListEntity(name = "Lista de compras", budget = 500.0))
            shoppingDao.insertShoppingItem(
                ShoppingItemEntity(listId = listId, name = "Manzanas", quantity = 2.0, unitPrice = 35.0)
            )
            shoppingDao.insertShoppingItem(
                ShoppingItemEntity(listId = listId, name = "Leche", quantity = 3.0, unitPrice = 28.5)
            )
            return listId
        }
        return 1L
    }
}
