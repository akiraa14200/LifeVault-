package com.lifevault.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifevault.app.data.VaultDatabase
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong("reminder_id", -1L)
        val dueAt = inputData.getLong("due_at", -1L)
        val offsetHours = inputData.getInt("alert_offset_hours", -1)
        if (reminderId <= 0L || dueAt <= 0L || offsetHours !in listOf(24, 5, 4, 3, 2, 1, 0)) return Result.failure()

        val dao = VaultDatabase.get(applicationContext).vaultDao()
        val reminder = dao.getReminder(reminderId) ?: return Result.success()
        if (reminder.isDone || reminder.dueAt != dueAt) return Result.success()

        val preferences = applicationContext.getSharedPreferences("lifevault_settings", Context.MODE_PRIVATE)
        val notificationsEnabled = preferences.getBoolean("notifications_enabled", true)
        val title = reminder.title
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                "lifevault_reminders",
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Scheduled LifeVault reminder alerts" })
        }

        val message = when (offsetHours) {
            24 -> "Your reminder is tomorrow"
            0 -> "It's time for this reminder"
            else -> "Your reminder is in $offsetHours hour" + if (offsetHours == 1) "" else "s"
        }
        val notification = NotificationCompat.Builder(applicationContext, "lifevault_reminders")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val hasNotificationPermission = Build.VERSION.SDK_INT < 33 ||
            applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (notificationsEnabled && hasNotificationPermission) {
            try {
                val notificationId = ((reminderId % 100_000L) * 100L + offsetHours).toInt()
                NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
            } catch (_: SecurityException) {
                // Permission can be revoked between the check and notify call.
            }
        }

        // The due-time alert is the final alert in this cycle. Advance recurring reminders
        // and schedule their next complete set of alerts only after this point.
        val repeatDays = reminder.repeatIntervalDays
        if (offsetHours == 0 && repeatDays in 1..365) {
            val nextDueAt = dueAt + TimeUnit.DAYS.toMillis(repeatDays.toLong())
            dao.advanceReminder(reminderId, nextDueAt)
            ReminderScheduler.scheduleNextCycle(
                applicationContext,
                reminderId,
                title,
                nextDueAt,
                repeatDays
            )
        }
        return Result.success()
    }
}
