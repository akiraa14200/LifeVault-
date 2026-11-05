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
@Entity(tableName = "planner_tasks")
data class PlannerTaskEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val notes: String = "", val dueAt: Long, val priority: String = "Normal", val isDone: Boolean = false, val createdAt: Long = System.currentTimeMillis())
@Entity(tableName = "habits")
data class HabitEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val completedDates: String = "", val createdAt: Long = System.currentTimeMillis())

@Dao
interface VaultDao {
    @Query("SELECT * FROM reminders ORDER BY isDone ASC, dueAt ASC") fun observeReminders(): Flow<List<ReminderEntity>>
    @Query("SELECT * FROM reminders ORDER BY id ASC") suspend fun allReminders(): List<ReminderEntity>
    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1") suspend fun getReminder(id: Long): ReminderEntity?
    @Query("UPDATE reminders SET dueAt = :newDueAt WHERE id = :id AND isDone = 0") suspend fun advanceReminder(id: Long, newDueAt: Long)
    @Query("SELECT * FROM budgets ORDER BY category ASC") fun observeBudgets(): Flow<List<BudgetEntity>>
    @Query("SELECT * FROM budgets ORDER BY category ASC") suspend fun allBudgets(): List<BudgetEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveBudget(budget: BudgetEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreBudgets(items: List<BudgetEntity>)
    @Query("DELETE FROM budgets WHERE category = :category") suspend fun deleteBudget(category: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreReminders(items: List<ReminderEntity>)
    @Insert suspend fun addReminder(reminder: ReminderEntity): Long
    @Query("UPDATE reminders SET isDone = 1 WHERE id = :id") suspend fun completeReminder(id: Long)
    @Query("SELECT * FROM documents ORDER BY addedAt DESC") fun observeDocuments(): Flow<List<DocumentEntity>>
    @Query("SELECT * FROM documents ORDER BY id ASC") suspend fun allDocuments(): List<DocumentEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreDocuments(items: List<DocumentEntity>)
    @Insert suspend fun addDocument(document: DocumentEntity)
    @Query("DELETE FROM documents WHERE id = :id") suspend fun deleteDocument(id: Long)
    @Query("UPDATE documents SET folder = :folder WHERE id = :id") suspend fun updateDocumentFolder(id: Long, folder: String)
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC") fun observeTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions ORDER BY id ASC") suspend fun allTransactions(): List<TransactionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreTransactions(items: List<TransactionEntity>)
    @Insert suspend fun addTransaction(transaction: TransactionEntity)
    @Query("SELECT * FROM planner_tasks ORDER BY isDone ASC, dueAt ASC") fun observePlannerTasks(): Flow<List<PlannerTaskEntity>>
    @Query("SELECT * FROM planner_tasks ORDER BY id ASC") suspend fun allPlannerTasks(): List<PlannerTaskEntity>
    @Insert suspend fun addPlannerTask(item: PlannerTaskEntity): Long
    @Query("UPDATE planner_tasks SET isDone = :done WHERE id = :id") suspend fun setPlannerTaskDone(id: Long, done: Boolean)
    @Query("DELETE FROM planner_tasks WHERE id = :id") suspend fun deletePlannerTask(id: Long)
    @Query("SELECT * FROM habits ORDER BY createdAt DESC") fun observeHabits(): Flow<List<HabitEntity>>
    @Query("SELECT * FROM habits ORDER BY id ASC") suspend fun allHabits(): List<HabitEntity>
    @Insert suspend fun addHabit(item: HabitEntity): Long
    @Query("UPDATE habits SET completedDates = :dates WHERE id = :id") suspend fun updateHabitDates(id: Long, dates: String)
    @Query("DELETE FROM habits WHERE id = :id") suspend fun deleteHabit(id: Long)
    @Query("DELETE FROM reminders") suspend fun clearReminders()
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM documents") suspend fun clearDocuments()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM planner_tasks") suspend fun clearPlannerTasks()
    @Query("DELETE FROM habits") suspend fun clearHabits()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePlannerTasks(items: List<PlannerTaskEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreHabits(items: List<HabitEntity>)
    @Transaction
    suspend fun replaceAll(reminders: List<ReminderEntity>, transactions: List<TransactionEntity>, documents: List<DocumentEntity>, budgets: List<BudgetEntity> = emptyList(), tasks: List<PlannerTaskEntity> = emptyList(), habits: List<HabitEntity> = emptyList()) {
        clearReminders(); clearTransactions(); clearDocuments(); clearBudgets(); clearPlannerTasks(); clearHabits()
        if (reminders.isNotEmpty()) restoreReminders(reminders)
        if (transactions.isNotEmpty()) restoreTransactions(transactions)
        if (documents.isNotEmpty()) restoreDocuments(documents)
        if (budgets.isNotEmpty()) restoreBudgets(budgets)
        if (tasks.isNotEmpty()) restorePlannerTasks(tasks)
        if (habits.isNotEmpty()) restoreHabits(habits)
    }
}

@Database(entities = [ReminderEntity::class, TransactionEntity::class, DocumentEntity::class, BudgetEntity::class, PlannerTaskEntity::class, HabitEntity::class], version = 4, exportSchema = false)
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
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS planner_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, notes TEXT NOT NULL, dueAt INTEGER NOT NULL, priority TEXT NOT NULL, isDone INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS habits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, completedDates TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }
        @Volatile private var INSTANCE: VaultDatabase? = null
        fun get(context: Context): VaultDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, VaultDatabase::class.java, "lifevault.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
        }
    }
}
