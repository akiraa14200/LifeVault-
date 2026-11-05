package com.lifevault.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reminders")
data class ReminderEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val dueAt: Long, val isDone: Boolean = false)

@Entity(tableName = "transactions")
data class TransactionEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val amountPaise: Long, val isIncome: Boolean, val createdAt: Long = System.currentTimeMillis())

@Dao
interface VaultDao {
    @Query("SELECT * FROM reminders ORDER BY isDone ASC, dueAt ASC") fun observeReminders(): Flow<List<ReminderEntity>>
    @Insert suspend fun addReminder(reminder: ReminderEntity)
    @Query("UPDATE reminders SET isDone = 1 WHERE id = :id") suspend fun completeReminder(id: Long)
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC") fun observeTransactions(): Flow<List<TransactionEntity>>
    @Insert suspend fun addTransaction(transaction: TransactionEntity)
}

@Database(entities = [ReminderEntity::class, TransactionEntity::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    companion object {
        @Volatile private var INSTANCE: VaultDatabase? = null
        fun get(context: Context): VaultDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, VaultDatabase::class.java, "lifevault.db").build().also { INSTANCE = it }
        }
    }
}
