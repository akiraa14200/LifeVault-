package com.lifevault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.lifevault.app.data.*
import kotlinx.coroutines.launch
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
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Forest, onPrimary = Color.White, secondary = Color(0xFF789A82), background = Canvas, surface = Color.White, onSurface = Ink)) {
                LifeVaultApp(dao,
                    onAddReminder = { title -> lifecycleScope.launch { dao.addReminder(ReminderEntity(title = title, dueAt = System.currentTimeMillis() + 86_400_000L)) } },
                    onCompleteReminder = { id -> lifecycleScope.launch { dao.completeReminder(id) } },
                    onAddTransaction = { title, amount, income -> lifecycleScope.launch { dao.addTransaction(TransactionEntity(title = title, amountPaise = (amount * 100).toLong(), isIncome = income)) } }
                )
            }
        }
    }
}

private enum class Destination(val label: String) { Home("Home"), Vault("Vault"), Reminders("Reminders"), Money("Money"), HomeCare("Home care") }

@Composable
private fun LifeVaultApp(dao: VaultDao, onAddReminder: (String) -> Unit, onCompleteReminder: (Long) -> Unit, onAddTransaction: (String, Double, Boolean) -> Unit) {
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
            Destination.Home -> HomeScreen(padding, reminders, transactions, onCompleteReminder)
            Destination.Vault -> FeatureScreen(padding, "Document vault", "Keep important files organised in one private place.", Icons.Default.Description, "Document import and previews are planned for the next milestone.")
            Destination.Reminders -> RemindersScreen(padding, reminders, onAddReminder, onCompleteReminder)
            Destination.Money -> MoneyScreen(padding, transactions, onAddTransaction)
            Destination.HomeCare -> FeatureScreen(padding, "Home care", "Appliances, service history and maintenance.", Icons.Default.HomeRepairService, "Appliance records and maintenance schedules are planned next.")
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

@Composable private fun HomeScreen(padding: PaddingValues, reminders: List<ReminderEntity>, transactions: List<TransactionEntity>, onComplete: (Long) -> Unit) {
    val expenses = transactions.filter { !it.isIncome }.sumOf { it.amountPaise }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("lifevault.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Forest); Spacer(Modifier.height(16.dp)); Header("Your personal space", "A little more organised.", "Everything important, in one calm place.") }; Box(Modifier.size(44.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) { Text("LV", color = Forest, fontWeight = FontWeight.Bold) } } }
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
            Column(Modifier.weight(1f)) { Text(reminder.title, fontWeight = FontWeight.SemiBold, color = if (reminder.isDone) Muted else Ink); Text(SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(reminder.dueAt)), color = Muted, style = MaterialTheme.typography.labelSmall) }
            if (!reminder.isDone) IconButton(onClick = { onComplete(reminder.id) }) { Icon(Icons.Default.CheckCircle, "Complete", tint = Forest) } else Text("Done", color = Forest, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun RemindersScreen(padding: PaddingValues, reminders: List<ReminderEntity>, onAdd: (String) -> Unit, onComplete: (Long) -> Unit) {
    var title by remember { mutableStateOf("") }; var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Header("Stay on track", "Reminders", "Small things, remembered.")
        Button(onClick = { show = true }, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add reminder") }
        if (reminders.isEmpty()) EmptyCard("Nothing scheduled", "Create your first reminder.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) { items(reminders, key = { it.id }) { ReminderRow(it, onComplete) } }
    }
    if (show) AlertDialog(onDismissRequest = { show = false }, title = { Text("New reminder") }, text = { OutlinedTextField(title, { title = it }, label = { Text("Reminder title") }, singleLine = true) }, confirmButton = { TextButton(onClick = { if (title.isNotBlank()) { onAdd(title.trim()); title = ""; show = false } }) { Text("Save") } }, dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } })
}

@Composable private fun MoneyScreen(padding: PaddingValues, transactions: List<TransactionEntity>, onAdd: (String, Double, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var income by remember { mutableStateOf(false) }; var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Header("Know where it goes", "Money", "A simple, private spending log.")
        Button(onClick = { show = true }, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add transaction") }
        if (transactions.isEmpty()) EmptyCard("No transactions yet", "Add income or an expense to get started.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) { items(transactions, key = { it.id }) { item -> Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.title, fontWeight = FontWeight.SemiBold); Text(if (item.isIncome) "Income" else "Expense", color = Muted, style = MaterialTheme.typography.labelSmall) }; Text((if (item.isIncome) "+" else "−") + formatRupees(item.amountPaise), color = if (item.isIncome) Forest else Ink, fontWeight = FontWeight.SemiBold) } } } }
    }
    if (show) AlertDialog(onDismissRequest = { show = false }, title = { Text("New transaction") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(title, { title = it }, label = { Text("Description") }, singleLine = true); OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount in ₹") }, singleLine = true); Row { FilterChip(selected = !income, onClick = { income = false }, label = { Text("Expense") }); Spacer(Modifier.width(8.dp)); FilterChip(selected = income, onClick = { income = true }, label = { Text("Income") }) } } }, confirmButton = { TextButton(onClick = { val value = amount.toDoubleOrNull(); if (title.isNotBlank() && value != null && value > 0) { onAdd(title.trim(), value, income); title = ""; amount = ""; show = false } }) { Text("Save") } }, dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } })
}

@Composable private fun FeatureScreen(padding: PaddingValues, title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, note: String) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { Header("LifeVault", title, subtitle); Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) { Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = Forest, modifier = Modifier.size(42.dp)); Spacer(Modifier.height(12.dp)); Text(note, color = Muted, style = MaterialTheme.typography.bodyMedium) } } }
}

private fun formatRupees(paise: Long): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(paise / 100.0)
