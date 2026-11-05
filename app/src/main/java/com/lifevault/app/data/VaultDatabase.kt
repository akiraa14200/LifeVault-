package com.lifevault.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reminders")
data class ReminderEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val dueAt: Long, val isDone: Boolean = false)

@Entity(tableName = "documents")
data class DocumentEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val uri: String, val mimeType: String, val addedAt: Long = System.currentTimeMillis())

@Entity(tableName = "transactions")
data class TransactionEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val amountPaise: Long, val isIncome: Boolean, val createdAt: Long = System.currentTimeMillis())

@Dao
interface VaultDao {
    @Query("SELECT * FROM reminders ORDER BY isDone ASC, dueAt ASC") fun observeReminders(): Flow<List<ReminderEntity>>
    @Insert suspend fun addReminder(reminder: ReminderEntity): Long
    @Query("UPDATE reminders SET isDone = 1 WHERE id = :id") suspend fun completeReminder(id: Long)
    @Query("SELECT * FROM documents ORDER BY addedAt DESC") fun observeDocuments(): Flow<List<DocumentEntity>>
    @Insert suspend fun addDocument(document: DocumentEntity)
    @Query("DELETE FROM documents WHERE id = :id") suspend fun deleteDocument(id: Long)
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC") fun observeTransactions(): Flow<List<TransactionEntity>>
    @Insert suspend fun addTransaction(transaction: TransactionEntity)
}

@Database(entities = [ReminderEntity::class, TransactionEntity::class, DocumentEntity::class], version = 2, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS documents (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, uri TEXT NOT NULL, mimeType TEXT NOT NULL, addedAt INTEGER NOT NULL)")
            }
        }
        @Volatile private var INSTANCE: VaultDatabase? = null
        fun get(context: Context): VaultDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, VaultDatabase::class.java, "lifevault.db").addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
