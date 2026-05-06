package com.example.habittracker.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracker.data.local.entity.TodoEntity
import com.example.habittracker.data.local.entity.RoutineWithTasks
import com.example.habittracker.presentation.BriefingViewModel
import com.example.habittracker.presentation.DashboardViewModel
import com.example.habittracker.presentation.TodoViewModel
import com.example.habittracker.domain.model.TodoPriority
import com.example.habittracker.domain.model.TodoCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onStartRoutine: (String) -> Unit,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (String) -> Unit,
    onUseTemplate: () -> Unit = {},
    onAnalytics: () -> Unit = {},
    onNavigateToTodos: () -> Unit = {},
    onProfile: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
    todoViewModel: TodoViewModel = hiltViewModel()
) {
    val routines by viewModel.allRoutines.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val todoUiState by todoViewModel.uiState.collectAsState()

    // Delete confirmation dialog
    var routineToDelete by remember { mutableStateOf<RoutineWithTasks?>(null) }

    // Create routine dialog
    var showCreateDialog by remember { mutableStateOf(false) }

    // Sign out confirmation dialog
    var showSignOutDialog by remember { mutableStateOf(false) }

    if (routineToDelete != null) {
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("Delete Routine") },
            text = { Text("Are you sure you want to delete \"${routineToDelete?.routine?.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        routineToDelete?.let { viewModel.deleteRoutine(it) }
                        routineToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Routine") },
            text = { Text("How would you like to create your routine?") },
            confirmButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    onCreateRoutine()
                }) {
                    Text("Create From Scratch")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    onUseTemplate()
                }) {
                    Text("Use a Template")
                }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    viewModel.signOut()
                }) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = buildString {
                            val displayName = uiState.userProfile?.displayName?.ifBlank { null }
                            if (displayName != null) {
                                append("$displayName's Routines")
                            } else {
                                append("My Routines")
                            }
                        }
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToTodos) {
                        Icon(
                            Icons.Default.ListAlt,
                            contentDescription = "To-Do List",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onAnalytics) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = "Full Analytics",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onProfile) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Routine")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Daily Briefing Card — Hardware/Sensors + Cloud/Networking rubric
            item {
                Spacer(modifier = Modifier.height(4.dp))
                DailyBriefingCard()
            }

            // Stats Summary Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            TextButton(onClick = onAnalytics) {
                                Text(
                                    "Details",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "${uiState.totalRoutinesCompleted}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(text = "Completed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = uiState.totalTimeSpentFormatted, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(text = "Total Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "${uiState.currentStreak}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(text = "Day Streak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Whatshot, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "${uiState.longestStreak}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(text = "Best Streak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            // Today's To-Do Summary Card
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTodos() },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Today's To-Do",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            val todayCount = todoUiState.todayTodos.size
                            val overdueCount = todoUiState.overdueTodos.size
                            if (todayCount > 0 || overdueCount > 0) {
                                Text(
                                    text = buildString {
                                        append("$todayCount for today")
                                        if (overdueCount > 0) {
                                            append(" · $overdueCount overdue")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (overdueCount > 0) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            } else {
                                Text(
                                    text = "No tasks for today. Tap to add!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Quick preview of today's todos (max 3)
            val previewTodos = todoUiState.todayTodos.take(3)
            if (previewTodos.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Quick Tasks",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(previewTodos, key = { it.id }) { todo ->
                    QuickTodoCard(
                        todo = todo,
                        onToggle = { todoViewModel.toggleTodoCompleted(todo) }
                    )
                }
            }

            // Routines Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "My Routines",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(routines, key = { it.routine.id }) { routineWithTasks ->
                val totalSeconds = routineWithTasks.tasks.sumOf { it.seconds }
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val durationText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = routineWithTasks.routine.title,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = "${routineWithTasks.tasks.size} Tasks \u2022 $durationText total",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { onEditRoutine(routineWithTasks.routine.id) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Routine", tint = MaterialTheme.colorScheme.secondary)
                                }
                                IconButton(onClick = { routineToDelete = routineWithTasks }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Routine", tint = MaterialTheme.colorScheme.error)
                                }
                                FilledIconButton(onClick = { onStartRoutine(routineWithTasks.routine.id) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Routine")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun QuickTodoCard(
    todo: TodoEntity,
    onToggle: () -> Unit
) {
    val priority = try { TodoPriority.valueOf(todo.priority) } catch (e: Exception) { TodoPriority.MEDIUM }
    val priorityColor = when (priority) {
        TodoPriority.LOW -> Color(0xFF4CAF50)
        TodoPriority.MEDIUM -> Color(0xFFFF9800)
        TodoPriority.HIGH -> Color(0xFFF44336)
        TodoPriority.URGENT -> Color(0xFFD32F2F)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = priority.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    if (todo.dueDate != null) {
                        Text(
                            text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(todo.dueDate!!)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}