package com.lifevault.app

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.lifevault.app.data.VaultDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Phase3Export {
    private fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    suspend fun exportTransactionsCsv(context: Context, uri: Uri, dao: VaultDao) {
        val rows = dao.allTransactions()
        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { out ->
            out.appendLine("Date,Type,Title,Category,Amount (INR)")
            rows.forEach { item ->
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(item.createdAt))
                val amount = String.format(Locale.US, "%.2f", item.amountPaise / 100.0)
                out.appendLine(listOf(csv(date), csv(if (item.isIncome) "Income" else "Expense"), csv(item.title), csv(item.category), amount).joinToString(","))
            }
        } ?: error("Could not open the selected file")
    }

    suspend fun exportVaultIndexCsv(context: Context, uri: Uri, dao: VaultDao) {
        val rows = dao.allDocuments()
        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { out ->
            out.appendLine("Name,Folder,Tags,MIME type,Added")
            rows.forEach { item ->
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(item.addedAt))
                out.appendLine(listOf(csv(item.name), csv(item.folder), csv(item.tags), csv(item.mimeType), csv(date)).joinToString(","))
            }
        } ?: error("Could not open the selected file")
    }

    suspend fun exportMoneyPdf(context: Context, uri: Uri, dao: VaultDao) {
        val rows = dao.allTransactions()
        val income = rows.filter { it.isIncome }.sumOf { it.amountPaise }
        val expenses = rows.filter { !it.isIncome }.sumOf { it.amountPaise }
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f; isFakeBoldText = true }
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; isFakeBoldText = true }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; alpha = 150 }
        canvas.drawText("LifeVault • Money report", 40f, 52f, title)
        canvas.drawText("Generated " + SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()), 40f, 75f, muted)
        canvas.drawText("Income: ₹%.2f".format(Locale.US, income / 100.0), 40f, 112f, heading)
        canvas.drawText("Expenses: ₹%.2f".format(Locale.US, expenses / 100.0), 240f, 112f, heading)
        canvas.drawText("Date", 40f, 150f, heading)
        canvas.drawText("Type / Category", 145f, 150f, heading)
        canvas.drawText("Description", 310f, 150f, heading)
        canvas.drawText("Amount", 500f, 150f, heading)
        var y = 172f
        rows.sortedByDescending { it.createdAt }.take(38).forEach { item ->
            val date = SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(item.createdAt))
            canvas.drawText(date, 40f, y, body)
            canvas.drawText((if (item.isIncome) "Income" else "Expense") + " / " + item.category.take(18), 145f, y, body)
            canvas.drawText(item.title.take(27), 310f, y, body)
            canvas.drawText("₹%.2f".format(Locale.US, item.amountPaise / 100.0), 500f, y, body)
            y += 16f
        }
        if (rows.size > 38) canvas.drawText("Showing 38 of " + rows.size + " entries. Export CSV for the full list.", 40f, 810f, muted)
        document.finishPage(page)
        try {
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) } ?: error("Could not open the selected file")
        } finally {
            document.close()
        }
    }
}
