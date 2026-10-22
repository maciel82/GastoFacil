package com.example.gastofacil.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        NotificationEntity::class,
        GoalEntity::class,
        RecurringBillEntity::class,
        SuggestedTransactionEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun notificationDao(): NotificationDao
    abstract fun goalDao(): GoalDao
    abstract fun recurringBillDao(): RecurringBillDao
    abstract fun suggestedTransactionDao(): SuggestedTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gasto_facil_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Inserir categorias padrão ao criar o banco de dados pela primeira vez
                CoroutineScope(Dispatchers.IO).launch {
                    val defaultCategories = listOf(
                        "Lazer", "Mercado", "Alimentação", "Compras Online",
                        "Transporte", "Educação", "Contas de casa",
                        "Trabalho", "Saúde Capilar", "Contas de outros"
                    ).map { CategoryEntity(name = it, isDefault = true) }

                    getDatabase(context).categoryDao().insertCategories(defaultCategories)
                }
            }
        }
    }
}