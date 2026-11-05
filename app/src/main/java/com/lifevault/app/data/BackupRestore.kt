package com.lifevault.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned, offline JSON backup for LifeVault's Room records.
 * Document content itself remains in Android's document provider; only its URI metadata is backed up.
 */
object BackupRestore {
    suspend fun export(context: Context, uri: Uri, dao: VaultDao) {
        val root = JSONObject()
            .put("format", "lifevault-backup")
            .put("version", 2)
            .put("createdAt", System.currentTimeMillis())
        val reminders = JSONArray()
        dao.allReminders().forEach { item ->
            reminders.put(JSONObject().put("id", item.id).put("title", item.title).put("dueAt", item.dueAt).put("isDone", item.isDone).put("repeatIntervalDays", item.repeatIntervalDays))
        }
        val transactions = JSONArray()
        dao.allTransactions().forEach { item ->
            transactions.put(JSONObject().put("id", item.id).put("title", item.title).put("amountPaise", item.amountPaise).put("isIncome", item.isIncome).put("createdAt", item.createdAt).put("category", item.category))
        }
        val documents = JSONArray()
        dao.allDocuments().forEach { item ->
            documents.put(JSONObject().put("id", item.id).put("name", item.name).put("uri", item.uri).put("mimeType", item.mimeType).put("addedAt", item.addedAt).put("folder", item.folder).put("tags", item.tags))
        }
        val budgets = JSONArray()
        dao.allBudgets().forEach { item -> budgets.put(JSONObject().put("category", item.category).put("monthlyLimitPaise", item.monthlyLimitPaise)) }
        root.put("reminders", reminders).put("transactions", transactions).put("documents", documents).put("budgets", budgets)
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(root.toString(2)) }
            ?: error("Could not open the selected backup destination")
    }

    suspend fun restore(context: Context, uri: Uri, dao: VaultDao) {
        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not read the selected backup file")
        require(raw.length <= 10_000_000) { "Backup file is too large" }
        val root = JSONObject(raw)
        require(root.optString("format") == "lifevault-backup") { "This is not a LifeVault backup" }
        require(root.optInt("version", -1) in 1..2) { "This backup version is not supported" }

        val reminders = mutableListOf<ReminderEntity>()
        val reminderArray = root.optJSONArray("reminders") ?: JSONArray()
        for (i in 0 until reminderArray.length()) {
            val item = reminderArray.getJSONObject(i)
            reminders += ReminderEntity(item.getLong("id"), item.getString("title"), item.getLong("dueAt"), item.optBoolean("isDone"), item.optInt("repeatIntervalDays", 0))
        }

        val transactions = mutableListOf<TransactionEntity>()
        val transactionArray = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until transactionArray.length()) {
            val item = transactionArray.getJSONObject(i)
            transactions += TransactionEntity(item.getLong("id"), item.getString("title"), item.getLong("amountPaise"), item.getBoolean("isIncome"), item.optLong("createdAt", System.currentTimeMillis()), item.optString("category", "Other"))
        }

        val documents = mutableListOf<DocumentEntity>()
        val documentArray = root.optJSONArray("documents") ?: JSONArray()
        for (i in 0 until documentArray.length()) {
            val item = documentArray.getJSONObject(i)
            documents += DocumentEntity(item.getLong("id"), item.getString("name"), item.getString("uri"), item.optString("mimeType", "application/octet-stream"), item.optLong("addedAt", System.currentTimeMillis()), item.optString("folder", "General"), item.optString("tags", ""))
        }

        val budgets = mutableListOf<BudgetEntity>()
        val budgetArray = root.optJSONArray("budgets") ?: JSONArray()
        for (i in 0 until budgetArray.length()) {
            val item = budgetArray.getJSONObject(i)
            budgets += BudgetEntity(item.getString("category"), item.getLong("monthlyLimitPaise"))
        }

        // Validate the entire file before replacing local records.
        require(reminders.size <= 100_000 && transactions.size <= 100_000 && documents.size <= 100_000 && budgets.size <= 1_000) {
            "Backup contains too many records"
        }
        dao.replaceAll(reminders, transactions, documents, budgets)
    }
}
