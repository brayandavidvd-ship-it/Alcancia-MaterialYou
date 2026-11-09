package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.GoalEntity
import com.example.data.model.ShoppingItemEntity
import com.example.data.model.ShoppingListEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        ShoppingListEntity::class,
        ShoppingItemEntity::class,
        AppSettingsEntity::class,
        GoalEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun settingsDao(): SettingsDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val INITIAL_CATEGORIES = listOf(
            CategoryEntity("comida", "Comida", "🍔", "#FF8A65", null, true, 0),
            CategoryEntity("transporte", "Transporte", "🚌", "#4FC3F7", null, true, 1),
            CategoryEntity("casa", "Casa", "🏠", "#81C784", null, true, 2),
            CategoryEntity("salud", "Salud", "💊", "#E57373", null, true, 3),
            CategoryEntity("ocio", "Ocio", "🎮", "#BA68C8", null, true, 4),
            CategoryEntity("escuela", "Escuela", "📚", "#FFD54F", null, true, 5),
            CategoryEntity("compras", "Compras", "🛍️", "#F06292", null, true, 6),
            CategoryEntity("otros", "Otros", "✨", "#90A4AE", null, true, 7)
        )

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE transactions ADD COLUMN customRepeatDays TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE transactions ADD COLUMN repeatGroupId TEXT")
                    database.execSQL("ALTER TABLE transactions ADD COLUMN timeString TEXT")
                } catch (_: Exception) {
                    // Columns might already exist if tables were created with v3
                }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE app_settings ADD COLUMN isFingerprintEnabled INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE app_settings ADD COLUMN isPinEnabled INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE app_settings ADD COLUMN pinCode TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE app_settings ADD COLUMN remindersEnabled INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE app_settings ADD COLUMN reminderTime TEXT NOT NULL DEFAULT '20:00'")
                    database.execSQL("ALTER TABLE app_settings ADD COLUMN reminderFrequency TEXT NOT NULL DEFAULT 'DAILY'")
                    database.execSQL("ALTER TABLE app_settings ADD COLUMN hapticLevel TEXT NOT NULL DEFAULT 'MEDIUM'")
                } catch (_: Exception) {
                    // Columns might already exist if created with v4
                }
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alcancia_database.db"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            database.categoryDao().insertCategories(INITIAL_CATEGORIES)
                            database.settingsDao().setSettings(AppSettingsEntity())
                            // Create a starter shopping list
                            val listId = database.shoppingDao().insertShoppingList(
                                ShoppingListEntity(name = "Despensa mensual")
                            )
                            database.shoppingDao().insertShoppingItem(
                                ShoppingItemEntity(listId = listId, name = "Manzanas", quantity = 2.0, unitPrice = 35.0)
                            )
                            database.shoppingDao().insertShoppingItem(
                                ShoppingItemEntity(listId = listId, name = "Leche entera", quantity = 3.0, unitPrice = 28.50)
                            )
                            database.shoppingDao().insertShoppingItem(
                                ShoppingItemEntity(listId = listId, name = "Pan de caja", quantity = 1.0, unitPrice = 45.0)
                            )
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
