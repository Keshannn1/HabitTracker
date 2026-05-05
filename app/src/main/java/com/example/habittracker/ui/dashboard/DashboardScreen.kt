package com.example.habittracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracker.presentation.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onStartRoutine: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val routines by viewModel.allRoutines.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Routines") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(routines) { routineWithTasks ->
                val totalSeconds = routineWithTasks.tasks.sumOf { it.seconds }
                val minutes = totalSeconds / 60
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = routineWithTasks.routine.title,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "${routineWithTasks.tasks.size} Tasks • ${minutes}m total",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        
                        FilledIconButton(
                            onClick = { onStartRoutine(routineWithTasks.routine.id) }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Routine")
                        }
                    }
                }
            }
        }
    }
}
