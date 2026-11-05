package com.lifevault.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.lifevault.app.data.VaultDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class LifeVaultWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = VaultDatabase.get(context).vaultDao()
                val reminders = dao.allReminders().filter { !it.isDone }.sortedBy { it.dueAt }
                val transactions = dao.allTransactions()
                val tasks = dao.allPlannerTasks().count { !it.isDone }
                val expenses = transactions.filter { !it.isIncome }.sumOf { it.amountPaise }
                val next = reminders.firstOrNull()?.title ?: "No upcoming reminders"
                val amount = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(expenses / 100.0)
                ids.forEach { id ->
                    val views = RemoteViews(context.packageName, R.layout.lifevault_widget)
                    views.setTextViewText(R.id.widget_reminder, next.take(48))
                    views.setTextViewText(R.id.widget_expenses, amount)
                    views.setTextViewText(R.id.widget_count, reminders.size.toString() + " open reminders • " + tasks + " tasks")
                    val launch = android.content.Intent(context, MainActivity::class.java)
                    val pending = android.app.PendingIntent.getActivity(context, id, launch, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_root, pending)
                    manager.updateAppWidget(id, views)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(android.content.ComponentName(context, LifeVaultWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val intent = android.content.Intent(context, LifeVaultWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
            }
        }
    }
}
