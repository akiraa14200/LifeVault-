package com.lifevault.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.lifevault.app.data.BudgetEntity
import com.lifevault.app.data.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val InsightGreen = Color(0xFF315F4B)
private val InsightMint = Color(0xFFE5F1E9)
private val InsightMuted = Color(0xFF829087)
private val InsightAmber = Color(0xFFB7791F)
private val InsightRed = Color(0xFFB45309)

@Composable
fun MoneyInsightsSection(
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>
) {
    val now = Calendar.getInstance()
    val currentStart = (now.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val currentEnd = (currentStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    val previousStart = (currentStart.clone() as Calendar).apply { add(Calendar.MONTH, -1) }

    fun expensesBetween(start: Calendar, end: Calendar): Long =
        transactions.asSequence()
            .filter { !it.isIncome && it.createdAt >= start.timeInMillis && it.createdAt < end.timeInMillis }
            .sumOf { it.amountPaise }

    val currentExpenses = expensesBetween(currentStart, currentEnd)
    val previousExpenses = expensesBetween(previousStart, currentStart)
    val currentItems = transactions.filter {
        !it.isIncome && it.createdAt >= currentStart.timeInMillis && it.createdAt < currentEnd.timeInMillis
    }
    val categoryTotals = currentItems.groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.amountPaise } }
        .toList()
        .sortedByDescending { it.second }
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    fun money(paise: Long): String = formatter.format(paise / 100.0).replace("INR", "₹")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Smart money insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Last 6 months", fontWeight = FontWeight.SemiBold)
                val monthly = (5 downTo 0).map { offset ->
                    val start = (currentStart.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
                    val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    val total = expensesBetween(start, end)
                    SimpleDateFormat("MMM", Locale.getDefault()).format(start.time) to total
                }
                val maxValue = (monthly.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
                Canvas(Modifier.fillMaxWidth().height(112.dp)) {
                    val gap = 12.dp.toPx()
                    val barWidth = ((size.width - gap * (monthly.size - 1)) / monthly.size).coerceAtLeast(2f)
                    monthly.forEachIndexed { index, item ->
                        val barHeight = (item.second.toFloat() / maxValue.toFloat()) * size.height
                        val left = index * (barWidth + gap)
                        drawRoundRect(
                            color = InsightGreen.copy(alpha = if (index == monthly.lastIndex) 1f else 0.48f),
                            topLeft = androidx.compose.ui.geometry.Offset(left, size.height - barHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight.coerceAtLeast(3.dp.toPx())),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx())
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    monthly.forEach { (label, _) ->
                        Text(label, style = MaterialTheme.typography.labelSmall, color = InsightMuted)
                    }
                }
                Text("Bars show recorded expenses by month. The latest month may still be in progress.", color = InsightMuted, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Category breakdown", fontWeight = FontWeight.SemiBold)
                if (categoryTotals.isEmpty()) {
                    Text("Add an expense to see where your money goes.", color = InsightMuted, style = MaterialTheme.typography.bodySmall)
                } else {
                    categoryTotals.take(6).forEach { (category, total) ->
                        val fraction = (total.toFloat() / currentExpenses.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(category, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                Text(money(total), color = InsightMuted, style = MaterialTheme.typography.labelMedium)
                            }
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(7.dp),
                                color = InsightGreen,
                                trackColor = InsightMint
                            )
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Month-to-month comparison", fontWeight = FontWeight.SemiBold)
                Text("This month: ${money(currentExpenses)}", fontWeight = FontWeight.Medium)
                Text("Last month: ${money(previousExpenses)}", color = InsightMuted, style = MaterialTheme.typography.bodySmall)
                val difference = currentExpenses - previousExpenses
                val comparison = if (previousExpenses > 0L) {
                    val percent = kotlin.math.abs(difference.toDouble() / previousExpenses.toDouble() * 100.0)
                    "${String.format(Locale.getDefault(), "%.1f", percent)}% ${if (difference > 0) "more" else if (difference < 0) "less" else "change"} than last month"
                } else if (currentExpenses > 0L) "No previous-month spending to compare" else "No expenses recorded yet"
                Text(comparison, color = if (difference > 0) InsightAmber else InsightGreen, style = MaterialTheme.typography.labelMedium)
            }
        }

        val exceeded = budgets.filter { budget ->
            val spent = categoryTotals.firstOrNull { it.first == budget.category }?.second ?: 0L
            spent > budget.monthlyLimitPaise
        }
        val nearing = budgets.filter { budget ->
            val spent = categoryTotals.firstOrNull { it.first == budget.category }?.second ?: 0L
            budget.monthlyLimitPaise > 0L && spent <= budget.monthlyLimitPaise &&
                spent.toDouble() / budget.monthlyLimitPaise.toDouble() >= 0.8
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Budget alerts", fontWeight = FontWeight.SemiBold)
                if (budgets.isEmpty()) {
                    Text("Set a monthly budget to receive spending alerts.", color = InsightMuted, style = MaterialTheme.typography.bodySmall)
                } else if (exceeded.isEmpty() && nearing.isEmpty()) {
                    Text("All set — no budgets are close to their limits.", color = InsightGreen, style = MaterialTheme.typography.bodySmall)
                } else {
                    exceeded.forEach { budget ->
                        val spent = categoryTotals.firstOrNull { it.first == budget.category }?.second ?: 0L
                        Text("${budget.category}: over budget by ${money(spent - budget.monthlyLimitPaise)}", color = InsightRed, style = MaterialTheme.typography.bodySmall)
                    }
                    nearing.forEach { budget ->
                        val spent = categoryTotals.firstOrNull { it.first == budget.category }?.second ?: 0L
                        Text("${budget.category}: ${(spent * 100 / budget.monthlyLimitPaise).coerceAtMost(100)}% of budget used", color = InsightAmber, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
