package com.lifevault.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.workDataOf
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val preferences = applicationContext.getSharedPreferences("lifevault_settings", Context.MODE_PRIVATE)
        if (!preferences.getBoolean("notifications_enabled", true)) return Result.success()

        val title = inputData.getString("title") ?: "LifeVault reminder"
        val reminderId = inputData.getLong("reminder_id", id.hashCode().toLong()).toInt()
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel("lifevault_reminders", "Reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = NotificationCompat.Builder(applicationContext, "lifevault_reminders")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("LifeVault reminder")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT < 33 || applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify(reminderId, notification)
        }
        val repeatDays = inputData.getInt("repeat_days", 0)
        if (repeatDays > 0 && repeatDays <= 365) {
            val nextRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(repeatDays.toLong(), TimeUnit.DAYS)
                .setInputData(workDataOf("title" to title, "reminder_id" to reminderId.toLong(), "repeat_days" to repeatDays))
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "reminder_${inputData.getLong("reminder_id", reminderId.toLong())}",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                nextRequest
            )
        }
        return Result.success()
    }
}