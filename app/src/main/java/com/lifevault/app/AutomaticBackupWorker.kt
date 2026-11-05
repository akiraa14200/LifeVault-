package com.lifevault.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifevault.app.data.BackupRestore
import com.lifevault.app.data.VaultDatabase
import java.io.File
import java.util.concurrent.TimeUnit

class AutomaticBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val dao = VaultDatabase.get(applicationContext).vaultDao()
        val folder = File(applicationContext.filesDir, "automatic_backups")
        folder.mkdirs()
        val output = File(folder, "LifeVault-auto-${System.currentTimeMillis()}.json")
        BackupRestore.exportToFile(applicationContext, output, dao)
        folder.listFiles()?.filter { it.isFile && it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }?.drop(7)?.forEach { it.delete() }
        LifeVaultWidgetProvider.refresh(applicationContext)
    }.fold({ Result.success() }, { android.util.Log.e("LifeVault", "Automatic backup failed", it); Result.retry() })

    companion object {
        private const val UNIQUE_NAME = "lifevault_daily_automatic_backup"
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutomaticBackupWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
        fun cancel(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME) }
        fun runNow(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<AutomaticBackupWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
