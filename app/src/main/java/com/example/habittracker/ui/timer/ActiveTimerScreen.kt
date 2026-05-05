package com.example.habittracker.ui.timer

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracker.presentation.RoutineViewModel

@Composable
fun ActiveTimerScreen(
    routineId: String,
    onBack: () -> Unit,
    onRoutineCompleted: (() -> Unit)? = null,
    viewModel: RoutineViewModel = hiltViewModel()
) {
    // 1. Keep Screen On logic
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(routineId) {
        viewModel.startRoutine(routineId)
    }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isRoutineComplete) {
        if (uiState.isRoutineComplete) {
            // Trigger cloud profile update before navigating back
            onRoutineCompleted?.invoke()
            onBack()
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // 2. The Sequential Slide: Animated Task Name
                AnimatedContent(
                    targetState = uiState.currentTask?.name ?: "Preparing...",
                    transitionSpec = {
                        (slideInHorizontally(animationSpec = tween(400)) { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally(animationSpec = tween(400)) { width -> -width } + fadeOut()
                        )
                    },
                    label = "Task Name Animation"
                ) { taskName ->
                    Text(
                        text = taskName,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 3. The Depletion Ring & Clock
                Box(contentAlignment = Alignment.Center) {
                    val totalTaskSeconds = uiState.currentTask?.seconds?.toFloat() ?: 1f
                    val remainingSeconds = uiState.secondsRemaining.toFloat()
                    val progress = if (totalTaskSeconds > 0) remainingSeconds / totalTaskSeconds else 0f

                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(280.dp),
                        strokeWidth = 12.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    val mins = uiState.secondsRemaining / 60
                    val secs = uiState.secondsRemaining % 60
                    val timeString = String.format("%02d:%02d", mins, secs)

                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(64.dp))

                // 4. Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = { viewModel.toggleTimer() },
                        modifier = Modifier.size(72.dp)
                    ) {
                        if (uiState.isPaused) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(36.dp))
                        } else {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(36.dp))
                        }
                    }

                    TextButton(onClick = { viewModel.skipTask() }) {
                        Text("Skip")
                    }
                }
            }
        }
    }
}