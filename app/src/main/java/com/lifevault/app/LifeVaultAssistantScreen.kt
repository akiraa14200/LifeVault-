package com.lifevault.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifevault.app.data.VaultDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

private val AssistantForest = androidx.compose.ui.graphics.Color(0xFF234C3D)
private val AssistantMuted = androidx.compose.ui.graphics.Color(0xFF829087)

@Composable
fun LifeVaultAssistantScreen(padding: PaddingValues, dao: VaultDao) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reminders by dao.observeReminders().collectAsStateWithLifecycle(initialValue = emptyList())
    val transactions by dao.observeTransactions().collectAsStateWithLifecycle(initialValue = emptyList())
    val documents by dao.observeDocuments().collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by dao.observePlannerTasks().collectAsStateWithLifecycle(initialValue = emptyList())
    val habits by dao.observeHabits().collectAsStateWithLifecycle(initialValue = emptyList())

    val prefs = remember { context.getSharedPreferences("lifevault_ai_settings", android.content.Context.MODE_PRIVATE) }
    var endpoint by remember { mutableStateOf(prefs.getString("endpoint","https://api.openai.com/v1") ?: "https://api.openai.com/v1") }
    var model by remember { mutableStateOf(prefs.getString("model","gpt-4o-mini") ?: "gpt-4o-mini") }
    var apiKey by remember { mutableStateOf(SecureApiKeyStore.load(context)) }
    var showKey by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var includeContext by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val localSummary = remember(reminders, transactions, documents, tasks, habits) {
        val expenses = transactions.filter { !it.isIncome }.sumOf { it.amountPaise }
        val income = transactions.filter { it.isIncome }.sumOf { it.amountPaise }
        val money = NumberFormat.getCurrencyInstance(Locale("en","IN")).format(expenses / 100.0)
        buildString {
            append("Open reminders: ").append(reminders.count { !it.isDone }).append('\n')
            append("Unfinished planner tasks: ").append(tasks.count { !it.isDone }).append('\n')
            append("Habits tracked: ").append(habits.size).append('\n')
            append("Vault file references: ").append(documents.size).append('\n')
            append("Recorded income in all time: ").append(NumberFormat.getCurrencyInstance(Locale("en","IN")).format(income / 100.0)).append('\n')
            append("Recorded expenses in all time: ").append(money)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).overscroll(rememberOverscrollEffect()),
        contentPadding = PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Your AI, your API key", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AssistantForest)
            Text("Connect an OpenAI-compatible provider. LifeVault does not supply or pay for an AI service.", color = AssistantMuted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("API connection", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(endpoint, { endpoint = it }, modifier = Modifier.fillMaxWidth(), label = { Text("API base URL") }, singleLine = true)
                    OutlinedTextField(model, { model = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Model") }, singleLine = true)
                    OutlinedTextField(
                        value = apiKey, onValueChange = { apiKey = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Your API key") }, singleLine = true,
                        visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { showKey = !showKey }) { Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle key visibility") } }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            runCatching {
                                require(endpoint.startsWith("https://")) { "Use an HTTPS endpoint." }
                                require(model.isNotBlank() && apiKey.isNotBlank()) { "Enter a model and API key." }
                                prefs.edit().putString("endpoint",endpoint.trim()).putString("model",model.trim()).apply()
                                SecureApiKeyStore.save(context,apiKey.trim())
                                status = "Connection settings saved securely on this device."
                            }.onFailure { status = it.message ?: "Could not save settings." }
                        }) { Icon(Icons.Default.Save,null); Spacer(Modifier.width(6.dp)); Text("Save") }
                        OutlinedButton(onClick = {
                            SecureApiKeyStore.clear(context); apiKey = ""
                            status = "Saved API key cleared."
                        }) { Text("Clear key") }
                    }
                    Text("The API key is encrypted with Android Keystore. Use only a provider you trust. Requests are sent only when you tap Ask.", color = AssistantMuted, style = MaterialTheme.typography.labelSmall)
                    if (status.isNotBlank()) Text(status, color = AssistantForest, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ask LifeVault", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(question, { question = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("What would you like help with?") })
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = includeContext, onCheckedChange = { includeContext = it })
                        Column(Modifier.weight(1f)) {
                            Text("Include a summary of my LifeVault data")
                            Text("Off by default. Sends counts and money totals, not document contents or titles.", color = AssistantMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Button(onClick = {
                        if (loading) return@Button
                        loading = true; answer = ""; status = ""
                        val savedEndpoint = endpoint.trim()
                        val savedModel = model.trim()
                        val savedKey = apiKey.trim()
                        val prompt = question.trim()
                        val contextSummary = if (includeContext) localSummary else ""
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) { LifeVaultAiClient.ask(savedEndpoint,savedModel,savedKey,prompt,contextSummary) }
                            }.onSuccess { answer = it }
                                .onFailure { status = it.message ?: "AI request failed." }
                            loading = false
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !loading && question.isNotBlank() && apiKey.isNotBlank()) {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.AutoAwesome,null)
                        Spacer(Modifier.width(8.dp)); Text(if (loading) "Asking…" else "Ask")
                    }
                    if (answer.isNotBlank()) Text(answer, style = MaterialTheme.typography.bodyMedium)
                    if (status.isNotBlank() && answer.isBlank()) Text(status, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
