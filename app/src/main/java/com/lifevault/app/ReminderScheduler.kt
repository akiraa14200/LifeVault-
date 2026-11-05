package com.lifevault.app

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private val alertOffsetsHours = listOf(24, 5, 4, 3, 2, 1, 0)

    fun schedule(context: Context, reminderId: Long, title: String, dueAt: Long, repeatDays: Int) {
        scheduleInternal(context, reminderId, title, dueAt, repeatDays, cancelExisting = true)
    }

    fun scheduleNextCycle(context: Context, reminderId: Long, title: String, dueAt: Long, repeatDays: Int) {
        scheduleInternal(context, reminderId, title, dueAt, repeatDays, cancelExisting = false)
    }

    private fun scheduleInternal(context: Context, reminderId: Long, title: String, dueAt: Long, repeatDays: Int, cancelExisting: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (cancelExisting) cancel(context, reminderId)
        val now = System.currentTimeMillis()

        alertOffsetsHours.forEach { offsetHours ->
            val alertAt = dueAt - TimeUnit.HOURS.toMillis(offsetHours.toLong())
            if (alertAt <= now) return@forEach

            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(alertAt - now, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(
                    "title" to title,
                    "reminder_id" to reminderId,
                    "due_at" to dueAt,
                    "alert_offset_hours" to offsetHours,
                    "repeat_days" to repeatDays
                ))
                .addTag(tag(reminderId))
                .build()

            workManager.enqueueUniqueWork(
                workName(reminderId, dueAt, offsetHours),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag(reminderId))
        // Cancel the original single-alert work name used by earlier app builds.
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$reminderId")
    }

    private fun tag(id: Long) = "lifevault_reminder_$id"
    private fun workName(id: Long, dueAt: Long, offset: Int) = "lifevault_reminder_${id}_${dueAt}_${offset}h"
}
