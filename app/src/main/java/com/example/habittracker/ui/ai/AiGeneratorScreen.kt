package com.example.habittracker.ui.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.presentation.AiArchitectViewModel
import com.example.habittracker.presentation.AiGenerationState

/**
 * AI Routine Architect screen.
 *
 * Allows the user to type a goal, generates a routine (via remote AI
 * or local fallback templates), previews the tasks, and saves to Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGeneratorScreen(
    onNavigateUp: () -> Unit,
    viewModel: AiArchitectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var goalText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Navigate back once after a successful save (not on every emission)
    val hasNavigated = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (uiState is AiGenerationState.Saved && !hasNavigated.value) {
            hasNavigated.value = true
            onNavigateUp()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("AI Routine Architect")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetState()
                        onNavigateUp()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate up"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ---------- Goal input ----------
            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                label = { Text("Describe your goal") },
                placeholder = { Text("e.g. ADHD Morning Focus, Evening Wind-Down\u2026") },
                isError = uiState is AiGenerationState.Error,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (goalText.isNotBlank()) {
                        focusManager.clearFocus()
                        viewModel.generateRoutine(goalText)
                    }
                }),
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            )

            // ---------- Generate button ----------
            val isGenerating = uiState is AiGenerationState.Generating
            Button(
                onClick = {
                    if (goalText.isNotBlank()) {
                        focusManager.clearFocus()
                        viewModel.generateRoutine(goalText)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = goalText.isNotBlank() && !isGenerating,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Generating\u2026")
                } else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Routine")
                }
            }

            // ---------- State-driven content ----------
            when (val state = uiState) {
                is AiGenerationState.Generating -> {
                    ThinkingAnimation()
                }
                is AiGenerationState.Success -> {
                    AiGeneratedRoutinePreview(
                        routine = state.routine,
                        tasks = state.tasks,
                        onSave = { viewModel.saveGeneratedRoutine(state.routine, state.tasks) },
                        onRegenerate = {
                            viewModel.generateRoutine(goalText)
                        }
                    )
                }
                is AiGenerationState.Error -> {
                    ErrorCard(
                        message = state.message,
                        onRetry = { viewModel.generateRoutine(goalText) }
                    )
                }
                is AiGenerationState.Saved -> {
                    // Handled by LaunchedEffect above
                }
                else -> {
                    // Idle — helpful hint
                    IdleHintCard()
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Sub-composables
// ──────────────────────────────────────────────────────────────

/** Pulsing dots animation shown while the AI is "thinking". */
@Composable
private fun ThinkingAnimation() {
    val transition = rememberInfiniteTransition(label = "thinking")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .graphicsLayer { this.alpha = if (i == 0) alpha else if (i == 1) 1f - alpha + 0.3f else alpha }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Crafting your routine\u2026",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Idle-state hint card shown before the user generates anything. */
@Composable
private fun IdleHintCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "How it works",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "1. Type a goal like \"ADHD Morning Focus\" or \"Evening Wind-Down\".\n" +
                "2. The AI generates a tailored sequence of timed tasks.\n" +
                "3. Preview the routine, then save it to your dashboard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

/** Preview card listing all generated tasks with a Save button. */
@Composable
private fun AiGeneratedRoutinePreview(
    routine: com.example.habittracker.data.local.entity.RoutineEntity,
    tasks: List<TaskEntity>,
    onSave: () -> Unit,
    onRegenerate: () -> Unit
) {
    val totalSeconds = tasks.fold(0L) { acc, task -> acc + task.seconds }
    val mins = totalSeconds / 60

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = routine.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${tasks.size} tasks \u00B7 $mins min total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRegenerate) {
                Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
            }
        }

        HorizontalDivider()

        // Task list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(tasks) { index, task ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    TaskCard(index = index, task = task)
                }
            }
        }

        // Save button
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = tasks.isNotEmpty()
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Save to My Routines", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))
    }
}

/** Single task row inside the preview list. */
@Composable
private fun TaskCard(index: Int, task: TaskEntity) {
    val mins = task.seconds / 60
    val secs = task.seconds % 60
    val durationText = if (mins > 0 && secs > 0) "${mins}m ${secs}s"
        else if (mins > 0) "${mins}m"
        else "${secs}s"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step number badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "${index + 1}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Error card with retry button. */
@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Retry")
            }
        }
    }
}