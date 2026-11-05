package com.lifevault.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reminders")
data class ReminderEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val dueAt: Long, val isDone: Boolean = false, val repeatIntervalDays: Int = 0)

@Entity(tableName = "documents")
data class DocumentEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val uri: String, val mimeType: String, val addedAt: Long = System.currentTimeMillis(), val folder: String = "General", val tags: String = "")

@Entity(tableName = "transactions")
data class TransactionEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val amountPaise: Long, val isIncome: Boolean, val createdAt: Long = System.currentTimeMillis(), val category: String = "Other")

@Entity(tableName = "budgets")
data class BudgetEntity(@PrimaryKey val category: String, val monthlyLimitPaise: Long)

@Dao
interface VaultDao {
    @Query("SELECT * FROM reminders ORDER BY isDone ASC, dueAt ASC") fun observeReminders(): Flow<List<ReminderEntity>>
    @Query("SELECT * FROM reminders ORDER BY id ASC") suspend fun allReminders(): List<ReminderEntity>
    @Query("SELECT * FROM budgets ORDER BY category ASC") fun observeBudgets(): Flow<List<BudgetEntity>>
    @Query("SELECT * FROM budgets ORDER BY category ASC") suspend fun allBudgets(): List<BudgetEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveBudget(budget: BudgetEntity)
    @Query("DELETE FROM budgets WHERE category = :category") suspend fun deleteBudget(category: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreReminders(items: List<ReminderEntity>)
    @Insert suspend fun addReminder(reminder: ReminderEntity): Long
    @Query("UPDATE reminders SET isDone = 1 WHERE id = :id") suspend fun completeReminder(id: Long)
    @Query("SELECT * FROM documents ORDER BY addedAt DESC") fun observeDocuments(): Flow<List<DocumentEntity>>
    @Query("SELECT * FROM documents ORDER BY id ASC") suspend fun allDocuments(): List<DocumentEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreDocuments(items: List<DocumentEntity>)
    @Insert suspend fun addDocument(document: DocumentEntity)
    @Query("DELETE FROM documents WHERE id = :id") suspend fun deleteDocument(id: Long)
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC") fun observeTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions ORDER BY id ASC") suspend fun allTransactions(): List<TransactionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreTransactions(items: List<TransactionEntity>)
    @Insert suspend fun addTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM reminders") suspend fun clearReminders()
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM documents") suspend fun clearDocuments()

    @Transaction
    @Query("DELETE FROM budgets") suspend fun clearBudgets()

    @Transaction
    suspend fun replaceAll(reminders: List<ReminderEntity>, transactions: List<TransactionEntity>, documents: List<DocumentEntity>, budgets: List<BudgetEntity> = emptyList()) {
        clearReminders()
        clearTransactions()
        clearDocuments()
        clearBudgets()
        if (reminders.isNotEmpty()) restoreReminders(reminders)
        if (transactions.isNotEmpty()) restoreTransactions(transactions)
        if (documents.isNotEmpty()) restoreDocuments(documents)
        if (budgets.isNotEmpty()) budgets.forEach { saveBudget(it) }
    }
}

@Database(entities = [ReminderEntity::class, TransactionEntity::class, DocumentEntity::class, BudgetEntity::class], version = 3, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS documents (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, uri TEXT NOT NULL, mimeType TEXT NOT NULL, addedAt INTEGER NOT NULL)")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN repeatIntervalDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'")
                db.execSQL("ALTER TABLE documents ADD COLUMN folder TEXT NOT NULL DEFAULT 'General'")
                db.execSQL("ALTER TABLE documents ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS budgets (category TEXT NOT NULL PRIMARY KEY, monthlyLimitPaise INTEGER NOT NULL)")
            }
        }
        @Volatile private var INSTANCE: VaultDatabase? = null
        fun get(context: Context): VaultDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, VaultDatabase::class.java, "lifevault.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
        }
    }
}
