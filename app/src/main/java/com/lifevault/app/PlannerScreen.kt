package com.lifevault.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifevault.app.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PlannerScreen(padding: PaddingValues, dao: VaultDao) {
    val scope = rememberCoroutineScope()
    val tasks by dao.observePlannerTasks().collectAsStateWithLifecycle(initialValue = emptyList())
    val habits by dao.observeHabits().collectAsStateWithLifecycle(initialValue = emptyList())
    var taskTitle by remember { mutableStateOf("") }
    var habitTitle by remember { mutableStateOf("") }
    var showTask by remember { mutableStateOf(false) }
    var showHabit by remember { mutableStateOf(false) }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val completed = tasks.count { it.isDone }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PlannerHeader() }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF234C3D)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("TASK PROGRESS", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall)
                    Text("$completed / ${tasks.size} completed", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), color = Color(0xFFB8E5C9), trackColor = Color.White.copy(alpha = .2f))
                }
            }
        }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Button(onClick = { showTask = true }) { Icon(Icons.Default.Add, null); Text("Add task") } } }
        if (tasks.isEmpty()) item { PlannerEmptyCard("No tasks yet", "Add a task to plan your day.") }
        items(tasks, key = { "task-${it.id}" }) { task ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(task.isDone, { scope.launch { dao.setPlannerTaskDone(task.id, it) } })
                    Column(Modifier.weight(1f)) { Text(task.title, fontWeight = FontWeight.SemiBold); Text("${task.priority} priority • ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(task.dueAt))}", color = Color.Gray, style = MaterialTheme.typography.labelSmall) }
                    IconButton(onClick = { scope.launch { dao.deletePlannerTask(task.id) } }) { Icon(Icons.Default.DeleteOutline, null) }
                }
            }
        }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Habits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); OutlinedButton(onClick = { showHabit = true }) { Icon(Icons.Default.Add, null); Text("Add habit") } } }
        if (habits.isEmpty()) item { PlannerEmptyCard("Build a steady routine", "Add a habit and check it off each day.") }
        items(habits, key = { "habit-${it.id}" }) { habit ->
            val dates = habit.completedDates.split(",").filter { it.isNotBlank() }.toSet()
            val done = today in dates
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(habit.title, fontWeight = FontWeight.SemiBold); Text("${dates.size} days completed", color = Color.Gray, style = MaterialTheme.typography.labelSmall) }
                    FilledTonalButton(onClick = { scope.launch { val updated = if (done) dates - today else dates + today; dao.updateHabitDates(habit.id, updated.sorted().joinToString(",")) } }) { Text(if (done) "Done today" else "Complete today") }
                    IconButton(onClick = { scope.launch { dao.deleteHabit(habit.id) } }) { Icon(Icons.Default.DeleteOutline, null) }
                }
            }
        }
    }
    if (showTask) AlertDialog(onDismissRequest = { showTask = false }, title = { Text("Add a task") }, text = { OutlinedTextField(taskTitle, { taskTitle = it }, label = { Text("Task name") }, singleLine = true) }, confirmButton = { TextButton(onClick = { val title = taskTitle.trim(); if (title.isNotEmpty()) scope.launch { dao.addPlannerTask(PlannerTaskEntity(title = title, dueAt = System.currentTimeMillis())) }; taskTitle = ""; showTask = false }) { Text("Save") } }, dismissButton = { TextButton(onClick = { showTask = false }) { Text("Cancel") } })
    if (showHabit) AlertDialog(onDismissRequest = { showHabit = false }, title = { Text("Add a habit") }, text = { OutlinedTextField(habitTitle, { habitTitle = it }, label = { Text("Habit name") }, singleLine = true) }, confirmButton = { TextButton(onClick = { val title = habitTitle.trim(); if (title.isNotEmpty()) scope.launch { dao.addHabit(HabitEntity(title = title)) }; habitTitle = ""; showHabit = false }) { Text("Save") } }, dismissButton = { TextButton(onClick = { showHabit = false }) { Text("Cancel") } })
}

@Composable
private fun PlannerHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("PLAN YOUR DAY", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text("Daily planner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text("Tasks and habits, stored privately on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlannerEmptyCard(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}
