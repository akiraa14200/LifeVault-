package com.lifevault.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Versioned offline JSON backup. Vault file bytes stay in Android's document provider. */
object BackupRestore {
    private suspend fun buildJson(dao: VaultDao): JSONObject {
        val root = JSONObject().put("format", "lifevault-backup").put("version", 3).put("createdAt", System.currentTimeMillis())
        val reminders = JSONArray()
        dao.allReminders().forEach { reminders.put(JSONObject().put("id",it.id).put("title",it.title).put("dueAt",it.dueAt).put("isDone",it.isDone).put("repeatIntervalDays",it.repeatIntervalDays)) }
        val transactions = JSONArray()
        dao.allTransactions().forEach { transactions.put(JSONObject().put("id",it.id).put("title",it.title).put("amountPaise",it.amountPaise).put("isIncome",it.isIncome).put("createdAt",it.createdAt).put("category",it.category)) }
        val documents = JSONArray()
        dao.allDocuments().forEach { documents.put(JSONObject().put("id",it.id).put("name",it.name).put("uri",it.uri).put("mimeType",it.mimeType).put("addedAt",it.addedAt).put("folder",it.folder).put("tags",it.tags)) }
        val budgets = JSONArray()
        dao.allBudgets().forEach { budgets.put(JSONObject().put("category",it.category).put("monthlyLimitPaise",it.monthlyLimitPaise)) }
        val tasks = JSONArray()
        dao.allPlannerTasks().forEach { tasks.put(JSONObject().put("id",it.id).put("title",it.title).put("notes",it.notes).put("dueAt",it.dueAt).put("priority",it.priority).put("isDone",it.isDone).put("createdAt",it.createdAt)) }
        val habits = JSONArray()
        dao.allHabits().forEach { habits.put(JSONObject().put("id",it.id).put("title",it.title).put("completedDates",it.completedDates).put("createdAt",it.createdAt)) }
        return root.put("reminders",reminders).put("transactions",transactions).put("documents",documents).put("budgets",budgets).put("plannerTasks",tasks).put("habits",habits)
    }

    suspend fun export(context: Context, uri: Uri, dao: VaultDao) {
        val json = buildJson(dao)
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json.toString(2)) }
            ?: error("Could not open the selected backup destination")
    }

    suspend fun exportToFile(context: Context, file: File, dao: VaultDao) {
        file.parentFile?.mkdirs()
        file.writeText(buildJson(dao).toString(2))
    }

    suspend fun restore(context: Context, uri: Uri, dao: VaultDao) {
        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not read the selected backup file")
        restoreJson(raw, dao)
    }

    suspend fun restoreFromFile(file: File, dao: VaultDao) {
        require(file.isFile && file.length() <= 10_000_000L) { "Backup file is unavailable or too large" }
        restoreJson(file.readText(), dao)
    }

    private suspend fun restoreJson(raw: String, dao: VaultDao) {
        require(raw.length <= 10_000_000) { "Backup file is too large" }
        val root = JSONObject(raw)
        require(root.optString("format") == "lifevault-backup") { "This is not a LifeVault backup" }
        require(root.optInt("version", -1) in 1..3) { "This backup version is not supported" }
        val reminders = mutableListOf<ReminderEntity>()
        val ra = root.optJSONArray("reminders") ?: JSONArray()
        for (i in 0 until ra.length()) { val x=ra.getJSONObject(i); reminders += ReminderEntity(x.getLong("id"),x.getString("title"),x.getLong("dueAt"),x.optBoolean("isDone"),x.optInt("repeatIntervalDays",0)) }
        val transactions = mutableListOf<TransactionEntity>()
        val ta = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until ta.length()) { val x=ta.getJSONObject(i); transactions += TransactionEntity(x.getLong("id"),x.getString("title"),x.getLong("amountPaise"),x.getBoolean("isIncome"),x.optLong("createdAt",System.currentTimeMillis()),x.optString("category","Other")) }
        val documents = mutableListOf<DocumentEntity>()
        val da = root.optJSONArray("documents") ?: JSONArray()
        for (i in 0 until da.length()) { val x=da.getJSONObject(i); documents += DocumentEntity(x.getLong("id"),x.getString("name"),x.getString("uri"),x.optString("mimeType","application/octet-stream"),x.optLong("addedAt",System.currentTimeMillis()),x.optString("folder","General"),x.optString("tags","")) }
        val budgets = mutableListOf<BudgetEntity>()
        val ba = root.optJSONArray("budgets") ?: JSONArray()
        for (i in 0 until ba.length()) { val x=ba.getJSONObject(i); budgets += BudgetEntity(x.getString("category"),x.getLong("monthlyLimitPaise")) }
        val tasks = mutableListOf<PlannerTaskEntity>()
        val pa = root.optJSONArray("plannerTasks") ?: JSONArray()
        for (i in 0 until pa.length()) { val x=pa.getJSONObject(i); tasks += PlannerTaskEntity(x.getLong("id"),x.getString("title"),x.optString("notes",""),x.getLong("dueAt"),x.optString("priority","Normal"),x.optBoolean("isDone"),x.optLong("createdAt",System.currentTimeMillis())) }
        val habits = mutableListOf<HabitEntity>()
        val ha = root.optJSONArray("habits") ?: JSONArray()
        for (i in 0 until ha.length()) { val x=ha.getJSONObject(i); habits += HabitEntity(x.getLong("id"),x.getString("title"),x.optString("completedDates",""),x.optLong("createdAt",System.currentTimeMillis())) }
        require(reminders.size <= 100_000 && transactions.size <= 100_000 && documents.size <= 100_000 && budgets.size <= 1_000 && tasks.size <= 100_000 && habits.size <= 10_000) { "Backup contains too many records" }
        dao.replaceAll(reminders,transactions,documents,budgets,tasks,habits)
    }
}
