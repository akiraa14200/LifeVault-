package com.lifevault.app

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.lifevault.app.data.*
import kotlinx.coroutines.launch
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Forest = Color(0xFF234C3D)
private val Mint = Color(0xFFE5F1E9)
private val Canvas = Color(0xFFF5F7F3)
private val Ink = Color(0xFF1E2B24)
private val Muted = Color(0xFF829087)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dao = VaultDatabase.get(this).vaultDao()
        val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Forest, onPrimary = Color.White, secondary = Color(0xFF789A82), background = Canvas, surface = Color.White, onSurface = Ink)) {
                LifeVaultApp(dao,
                    onAddReminder = { title, repeatDays ->
                        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        lifecycleScope.launch {
                            val dueAt = System.currentTimeMillis() + 86_400_000L
                            val id = dao.addReminder(ReminderEntity(title = title, dueAt = dueAt, repeatIntervalDays = repeatDays))
                            val request = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay((dueAt - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS).setInputData(workDataOf("title" to title, "reminder_id" to id, "repeat_days" to repeatDays)).build()
                            WorkManager.getInstance(this@MainActivity).enqueueUniqueWork("reminder_$id", androidx.work.ExistingWorkPolicy.REPLACE, request)
                        }
                    },
                    onCompleteReminder = { id -> lifecycleScope.launch { dao.completeReminder(id); WorkManager.getInstance(this@MainActivity).cancelUniqueWork("reminder_$id") } },
                    onAddTransaction = { title, amount, income, category -> lifecycleScope.launch { dao.addTransaction(TransactionEntity(title = title, amountPaise = (amount * 100).toLong(), isIncome = income, category = category)) } }
                )
            }
        }
    }
}

private enum class Destination(val label: String) { Home("Home"), Vault("Vault"), Reminders("Reminders"), Money("Money"), HomeCare("Home care"), Search("Search"), Settings("Settings"), About("About"), Guide("Guide"), Terms("Terms"), Privacy("Privacy") }

@Composable
private fun LifeVaultApp(dao: VaultDao, onAddReminder: (String, Int) -> Unit, onCompleteReminder: (Long) -> Unit, onAddTransaction: (String, Double, Boolean, String) -> Unit) {
    var selected by remember { mutableStateOf(Destination.Home) }
    val reminders by dao.observeReminders().collectAsStateWithLifecycle(initialValue = emptyList())
    val transactions by dao.observeTransactions().collectAsStateWithLifecycle(initialValue = emptyList())
    Scaffold(containerColor = Canvas, bottomBar = {
        NavigationBar(containerColor = Color.White) {
            val items = listOf(Destination.Home to Icons.Default.Home, Destination.Vault to Icons.Default.Description, Destination.Reminders to Icons.Default.Notifications, Destination.Money to Icons.Default.AccountBalanceWallet, Destination.HomeCare to Icons.Default.HomeRepairService)
            items.forEach { (dest, icon) -> NavigationBarItem(selected == dest, onClick = { selected = dest }, icon = { Icon(icon, dest.label) }, label = { Text(dest.label, maxLines = 1) }) }
        }
    }) { padding ->
        when (selected) {
            Destination.Home -> HomeScreen(padding, reminders, transactions, onCompleteReminder, onSearch = { selected = Destination.Search }, onSettings = { selected = Destination.Settings })
            Destination.Vault -> VaultScreen(padding, dao)
            Destination.Reminders -> RemindersScreen(padding, reminders, onAddReminder, onCompleteReminder)
            Destination.Money -> MoneyScreen(padding, transactions, dao, onAddTransaction)
            Destination.HomeCare -> FeatureScreen(padding, "Home care", "Appliances, service history and maintenance.", Icons.Default.HomeRepairService, "Appliance records and maintenance schedules are planned next.")
            Destination.Settings -> SettingsScreen(padding, dao, onNavigate = { selected = it })
            Destination.Search -> SearchScreen(padding, dao, reminders, transactions)
            Destination.About -> InfoScreen(padding, "About LifeVault", "A calm space for the important parts of everyday life.", listOf("LifeVault helps organise reminders, documents and simple money records.", "This native Android app stores its current records locally on your device.", "Some features are still being developed. The app does not provide financial, legal or medical advice."), onBack = { selected = Destination.Settings })
            Destination.Guide -> InfoScreen(padding, "Documentation", "A quick guide to using LifeVault.", listOf("Home: view a summary of reminders and recorded spending.", "Vault: import a file using the Android document picker. Open a saved file with a compatible app. Removing a vault entry does not delete the original file.", "Reminders: create a reminder and mark it complete. Notifications depend on Android notification permission and your notification setting.", "Money: record an income or expense manually. Entries are a personal log, not a bank connection.", "Home care: this section is a placeholder for future appliance and maintenance records."), onBack = { selected = Destination.Settings })
            Destination.Terms -> InfoScreen(padding, "Terms of use", "Please read these basic terms before using the app.", listOf("Use LifeVault lawfully and only for information you are permitted to store.", "You are responsible for keeping your device secure and maintaining your own backups of important files.", "LifeVault is provided as an organisational tool. It does not guarantee that reminders will always be delivered; Android settings, battery restrictions and permissions may affect them.", "The app is provided as-is while under development. Features may change, be unavailable or contain errors.", "Do not rely on LifeVault as the sole copy of critical documents or as professional financial, legal or medical advice."), onBack = { selected = Destination.Settings })
            Destination.Privacy -> InfoScreen(padding, "Privacy policy", "What the current native build does with your information.", listOf("The current native build stores reminders, money entries and document references in a local Room database on this device.", "Imported files remain in their original Android document provider. LifeVault stores a reference and display details; removing an entry removes the reference from the vault, not the original file.", "Reminder notifications are generated on-device. You can turn them off in Settings or in Android system settings.", "This build does not currently connect to a LifeVault cloud account or sync data to a server.", "Uninstalling the app or clearing its app data may remove locally stored records. Keep independent copies of important information."), onBack = { selected = Destination.Settings })
        }
    }
}

@Composable private fun VaultScreen(padding: PaddingValues, dao: VaultDao) {
    val context = LocalContext.current
    val documents by dao.observeDocuments().collectAsStateWithLifecycle(initialValue = emptyList())
    var folder by remember { mutableStateOf("General") }
    var tags by remember { mutableStateOf("") }
    var folderFilter by remember { mutableStateOf("All") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "Imported file"
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            (context as? ComponentActivity)?.lifecycleScope?.launch { dao.addDocument(DocumentEntity(name = name, uri = uri.toString(), mimeType = mime, folder = folder.trim().ifBlank { "General" }, tags = tags.trim())) }
        }
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Header("Your private space", "Document vault", "Keep important files organised on this device.")
        OutlinedTextField(folder, { folder = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Folder for new files") }, singleLine = true)
        OutlinedTextField(tags, { tags = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Tags (comma separated)") }, singleLine = true)
        Button(onClick = { picker.launch(arrayOf("*/*")) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(8.dp)); Text("Import files")
        }
        Text("Files stay on this device. You can open them from LifeVault whenever you need them.", color = Muted, style = MaterialTheme.typography.bodySmall)
        val folders = listOf("All") + documents.map { it.folder }.distinct().sorted()
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            folders.forEach { name -> FilterChip(selected = folderFilter == name, onClick = { folderFilter = name }, label = { Text(name) }) }
        }
        val visibleDocuments = if (folderFilter == "All") documents else documents.filter { it.folder == folderFilter }
        if (documents.isEmpty()) EmptyCard("Your vault is empty", "Import PDFs, images and other documents to get started.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(visibleDocuments, key = { it.id }) { file ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.InsertDriveFile, null, tint = Forest, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(file.name, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text("${file.folder} • ${file.mimeType}", color = Muted, style = MaterialTheme.typography.labelSmall)
                            if (file.tags.isNotBlank()) Text(file.tags, color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(file.uri), file.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                context.startActivity(intent)
                            }
                        }) { Icon(Icons.Default.OpenInNew, "Open file", tint = Forest) }
                        IconButton(onClick = { (context as? ComponentActivity)?.lifecycleScope?.launch { dao.deleteDocument(file.id) } }) {
                            Icon(Icons.Default.DeleteOutline, "Remove from vault", tint = Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun Header(kicker: String, title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(kicker.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted, fontWeight = FontWeight.SemiBold)
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

@Composable private fun HomeScreen(padding: PaddingValues, reminders: List<ReminderEntity>, transactions: List<TransactionEntity>, onComplete: (Long) -> Unit, onSearch: () -> Unit, onSettings: () -> Unit) {
    val expenses = transactions.filter { !it.isIncome }.sumOf { it.amountPaise }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("lifevault.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Forest); Spacer(Modifier.height(16.dp)); Header("Your personal space", "A little more organised.", "Everything important, in one calm place.") }; Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onSearch, modifier = Modifier.size(44.dp).background(Mint, CircleShape)) { Icon(Icons.Default.Search, "Search", tint = Forest) }
                IconButton(onClick = onSettings, modifier = Modifier.size(44.dp).background(Mint, CircleShape)) { Text("LV", color = Forest, fontWeight = FontWeight.Bold) }
            } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Metric("Open reminders", reminders.count { !it.isDone }.toString(), Modifier.weight(1f)); Metric("Saved records", transactions.size.toString(), Modifier.weight(1f)) } }
        item { Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Forest)) { Column(Modifier.fillMaxWidth().padding(20.dp)) { Text("Your spending", color = Color.White.copy(alpha = .72f)); Text(formatRupees(expenses), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold); Text("Recorded on this device", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall) } } }
        item { Text("Upcoming reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if (reminders.isEmpty()) item { EmptyCard("No reminders yet", "Add a reminder to see it here.") }
        items(reminders.take(4), key = { it.id }) { ReminderRow(it, onComplete) }
    }
}

@Composable private fun Metric(label: String, value: String, modifier: Modifier = Modifier) { Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(15.dp)) { Text(label, color = Muted, style = MaterialTheme.typography.labelMedium); Text(value, color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) } } }
@Composable private fun EmptyCard(title: String, subtitle: String) { Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) } } }

@Composable private fun ReminderRow(reminder: ReminderEntity, onComplete: (Long) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(reminder.title, fontWeight = FontWeight.SemiBold, color = if (reminder.isDone) Muted else Ink); Text(SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(reminder.dueAt)) + if (reminder.repeatIntervalDays > 0) " • Repeats every ${reminder.repeatIntervalDays} day(s)" else "", color = Muted, style = MaterialTheme.typography.labelSmall) }
            if (!reminder.isDone) IconButton(onClick = { onComplete(reminder.id) }) { Icon(Icons.Default.CheckCircle, "Complete", tint = Forest) } else Text("Done", color = Forest, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun RemindersScreen(padding: PaddingValues, reminders: List<ReminderEntity>, onAdd: (String, Int) -> Unit, onComplete: (Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    var repeatDays by remember { mutableIntStateOf(0) }
    var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Header("Stay on track", "Reminders", "Small things, remembered.")
        Button(onClick = { show = true }, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add reminder") }
        if (reminders.isEmpty()) EmptyCard("Nothing scheduled", "Create your first reminder.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) { items(reminders, key = { it.id }) { ReminderRow(it, onComplete) } }
    }
    if (show) AlertDialog(
        onDismissRequest = { show = false },
        title = { Text("New reminder") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Reminder title") }, singleLine = true)
            Text("Repeat", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "Never", 1 to "Daily", 7 to "Weekly", 30 to "Every 30d").forEach { (days, label) ->
                    FilterChip(selected = repeatDays == days, onClick = { repeatDays = days }, label = { Text(label) })
                }
            }
            Text("First alert is scheduled for 24 hours from now. Repeats are scheduled after each alert.", color = Muted, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) { onAdd(title.trim(), repeatDays); title = ""; repeatDays = 0; show = false } }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } }
    )
}

@Composable private fun MoneyScreen(padding: PaddingValues, transactions: List<TransactionEntity>, dao: VaultDao, onAdd: (String, Double, Boolean, String) -> Unit) {
    val budgets by dao.observeBudgets().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories = listOf("Food", "Transport", "Bills", "Shopping", "Health", "Education", "Entertainment", "Home", "Other")
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var income by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Food") }
    var show by remember { mutableStateOf(false) }
    var budgetCategory by remember { mutableStateOf("Food") }
    var budgetAmount by remember { mutableStateOf("") }
    var showBudget by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val monthStart = remember { java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis }
    val monthExpenses = transactions.filter { !it.isIncome && it.createdAt >= monthStart }.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amountPaise } }
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Header("Know where it goes", "Money", "A simple, private spending log.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { show = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add transaction") }
            OutlinedButton(onClick = { showBudget = true }, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Flag, null); Spacer(Modifier.width(5.dp)); Text("Budget") }
        }
        Text("Monthly budgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (budgets.isEmpty()) Text("Set a category budget to track monthly spending.", color = Muted, style = MaterialTheme.typography.bodySmall)
        budgets.forEach { budget ->
            val spent = monthExpenses[budget.category] ?: 0L
            val progress = (spent.toFloat() / budget.monthlyLimitPaise.coerceAtLeast(1)).coerceIn(0f, 1f)
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(budget.category, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text("${formatRupees(spent)} / ${formatRupees(budget.monthlyLimitPaise)}", style = MaterialTheme.typography.labelSmall)
                        IconButton(onClick = { scope.launch { dao.deleteBudget(budget.category) } }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Remove budget", tint = Muted) }
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    if (spent > budget.monthlyLimitPaise) Text("Budget exceeded", color = Color(0xFFB45309), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Text("Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (transactions.isEmpty()) EmptyCard("No transactions yet", "Add income or an expense to get started.") else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(transactions, key = { it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(item.title, fontWeight = FontWeight.SemiBold); Text("${if (item.isIncome) "Income" else "Expense"} • ${item.category}", color = Muted, style = MaterialTheme.typography.labelSmall) }
                        Text((if (item.isIncome) "+" else "−") + formatRupees(item.amountPaise), color = if (item.isIncome) Forest else Ink, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
    if (show) AlertDialog(onDismissRequest = { show = false }, title = { Text("New transaction") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("Description") }, singleLine = true)
        OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount in ₹") }, singleLine = true)
        Row { FilterChip(selected = !income, onClick = { income = false }, label = { Text("Expense") }); Spacer(Modifier.width(8.dp)); FilterChip(selected = income, onClick = { income = true }, label = { Text("Income") }) }
        Text("Category", style = MaterialTheme.typography.labelMedium)
        Column { categories.chunked(3).forEach { group -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { group.forEach { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) }) } } } }
    } }, confirmButton = { TextButton(onClick = { val value = amount.toDoubleOrNull(); if (title.isNotBlank() && value != null && value > 0) { onAdd(title.trim(), value, income, category); title = ""; amount = ""; show = false } }) { Text("Save") } }, dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } })
    if (showBudget) AlertDialog(onDismissRequest = { showBudget = false }, title = { Text("Set monthly budget") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Choose a category and spending limit in ₹.")
        categories.chunked(3).forEach { group -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { group.forEach { item -> FilterChip(selected = budgetCategory == item, onClick = { budgetCategory = item }, label = { Text(item) }) } } }
        OutlinedTextField(budgetAmount, { budgetAmount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Monthly limit in ₹") }, singleLine = true)
    } }, confirmButton = { TextButton(onClick = { val value = budgetAmount.toDoubleOrNull(); if (value != null && value > 0) { scope.launch { dao.saveBudget(BudgetEntity(budgetCategory, (value * 100).toLong())) }; budgetAmount = ""; showBudget = false } }) { Text("Save budget") } }, dismissButton = { TextButton(onClick = { showBudget = false }) { Text("Cancel") } })
}

@Composable private fun FeatureScreen(padding: PaddingValues, title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, note: String) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { Header("LifeVault", title, subtitle); Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) { Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = Forest, modifier = Modifier.size(42.dp)); Spacer(Modifier.height(12.dp)); Text(note, color = Muted, style = MaterialTheme.typography.bodyMedium) } } }
}

private fun formatRupees(paise: Long): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(paise / 100.0)



@Composable
private fun SearchScreen(
    padding: PaddingValues,
    dao: VaultDao,
    reminders: List<ReminderEntity>,
    transactions: List<TransactionEntity>
) {
    var query by remember { mutableStateOf("") }
    val documents by dao.observeDocuments().collectAsStateWithLifecycle(initialValue = emptyList())
    val term = query.trim()
    val matchingReminders = reminders.filter { term.isNotEmpty() && it.title.contains(term, ignoreCase = true) }
    val matchingTransactions = transactions.filter { term.isNotEmpty() && (it.title.contains(term, ignoreCase = true) || it.category.contains(term, ignoreCase = true)) }
    val matchingDocuments = documents.filter { term.isNotEmpty() && (it.name.contains(term, ignoreCase = true) || it.mimeType.contains(term, ignoreCase = true) || it.folder.contains(term, ignoreCase = true) || it.tags.contains(term, ignoreCase = true)) }

    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Header("Find anything", "Search", "Search reminders, money records and saved files.")
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear search") } },
            placeholder = { Text("Search LifeVault") }
        )
        Spacer(Modifier.height(12.dp))
        if (term.isEmpty()) {
            EmptyCard("Start typing to search", "Results update as you type.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (matchingReminders.isNotEmpty()) {
                    item { Text("REMINDERS", color = Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    items(matchingReminders, key = { "r${it.id}" }) { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, null, tint = Forest)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) { Text(item.title, fontWeight = FontWeight.SemiBold); Text(if (item.isDone) "Completed" else "Reminder", color = Muted, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
                if (matchingTransactions.isNotEmpty()) {
                    item { Text("MONEY", color = Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    items(matchingTransactions, key = { "t${it.id}" }) { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, null, tint = Forest)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) { Text(item.title, fontWeight = FontWeight.SemiBold); Text(if (item.isIncome) "Income" else "Expense", color = Muted, style = MaterialTheme.typography.labelSmall) }
                                Text(formatRupees(item.amountPaise), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                if (matchingDocuments.isNotEmpty()) {
                    item { Text("VAULT FILES", color = Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    items(matchingDocuments, key = { "d${it.id}" }) { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InsertDriveFile, null, tint = Forest)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) { Text(item.name, fontWeight = FontWeight.SemiBold); Text(item.mimeType, color = Muted, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
                if (matchingReminders.isEmpty() && matchingTransactions.isEmpty() && matchingDocuments.isEmpty()) {
                    item { EmptyCard("No matches found", "Try a different title or filename.") }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(padding: PaddingValues, dao: VaultDao, onNavigate: (Destination) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dataMessage by remember { mutableStateOf("") }
    var confirmRestore by remember { mutableStateOf(false) }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            dataMessage = runCatching { BackupRestore.export(context, uri, dao); "Backup saved successfully." }.getOrElse { "Backup failed: ${it.message ?: "unknown error"}" }
            android.widget.Toast.makeText(context, dataMessage, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            dataMessage = runCatching { BackupRestore.restore(context, uri, dao); "Backup restored. Existing local records were replaced." }.getOrElse { "Restore failed: ${it.message ?: "unknown error"}" }
            android.widget.Toast.makeText(context, dataMessage, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val prefs = remember { context.getSharedPreferences("lifevault_settings", android.content.Context.MODE_PRIVATE) }
    var notifications by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    var cacheMessage by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Header("Personalise your app", "Settings", "Manage LifeVault on this device.")
                }
                IconButton(
                    onClick = { onNavigate(Destination.Home) },
                    modifier = Modifier.background(Mint, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close settings", tint = Forest)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("App preferences", fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Reminder notifications", fontWeight = FontWeight.Medium)
                            Text("Allow LifeVault to post reminder alerts", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = notifications,
                            onCheckedChange = { enabled ->
                                notifications = enabled
                                prefs.edit().putBoolean("notifications_enabled", enabled).apply()
                            }
                        )
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Appearance")
                            Text("Light theme", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Default", color = Forest, style = MaterialTheme.typography.labelMedium)
                    }
                    Text("Language follows your device language where supported.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backup & restore", fontWeight = FontWeight.SemiBold)
                    Text("Export your reminders, money records and vault index to a JSON file, or restore a previous backup.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { exportPicker.launch("LifeVault-backup-${System.currentTimeMillis()}.json") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Create backup")
                    }
                    OutlinedButton(onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Restore, null); Spacer(Modifier.width(8.dp)); Text("Restore backup")
                    }
                    if (dataMessage.isNotBlank()) Text(dataMessage, color = Forest, style = MaterialTheme.typography.labelSmall)
                    Text("Note: imported document files are not copied into the backup; only their saved references are included. References may not work after moving to another device.", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("App storage", fontWeight = FontWeight.SemiBold)
                    Text("Clear temporary cache files. Your vault entries and money records are not deleted.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        val cleared = runCatching {
                            val files = context.cacheDir.listFiles()
                            if (files != null) files.forEach { it.deleteRecursively() }
                            true
                        }.getOrDefault(false)
                        cacheMessage = if (cleared) "Temporary cache cleared." else "Could not clear cache."
                        android.widget.Toast.makeText(context, cacheMessage, android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.DeleteSweep, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Clear cache")
                    }
                    if (cacheMessage.isNotBlank()) {
                        Text(cacheMessage, color = Forest, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item { Text("Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
        item { SettingsLink("Documentation", "How to use the app", Icons.Default.MenuBook) { onNavigate(Destination.Guide) } }
        item { SettingsLink("Terms of use", "Rules for using LifeVault", Icons.Default.Gavel) { onNavigate(Destination.Terms) } }
        item { SettingsLink("Privacy policy", "How your data is handled", Icons.Default.PrivacyTip) { onNavigate(Destination.Privacy) } }
        item { SettingsLink("About LifeVault", "App information and version", Icons.Default.Info) { onNavigate(Destination.About) } }
        item { Text("LifeVault • Native Android • Local-first preview", color = Muted, style = MaterialTheme.typography.labelSmall) }
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Replace local data?") },
            text = { Text("Restoring a backup replaces the reminders, money entries and vault index currently stored on this device. Export a backup of the current data first if you may need it.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    importPicker.launch(arrayOf("application/json", "text/*"))
                }) { Text("Choose backup") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsLink(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Forest)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
private fun InfoScreen(padding: PaddingValues, title: String, subtitle: String, paragraphs: List<String>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Settings") }
        Header("LifeVault", title, subtitle)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(paragraphs) { paragraph ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Text(paragraph, Modifier.fillMaxWidth().padding(16.dp), color = Ink, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
