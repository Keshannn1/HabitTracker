package com.example.habittracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.dao.RoutineDao
import com.example.habittracker.data.local.entity.HistoryEntity
import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.data.remote.FirebaseRoutineRepository
import com.example.habittracker.domain.use_case.CompleteTaskAndAdvanceUseCase
import com.example.habittracker.domain.use_case.ResetAndStartRoutineUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerUiState(
    val currentTask: TaskEntity? = null,
    val secondsRemaining: Long = 0L,
    val isPaused: Boolean = true,
    val isRoutineComplete: Boolean = false
)

@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val resetAndStartRoutineUseCase: ResetAndStartRoutineUseCase,
    private val completeTaskAndAdvanceUseCase: CompleteTaskAndAdvanceUseCase,
    private val routineDao: RoutineDao,
    private val firebaseRoutineRepository: FirebaseRoutineRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var currentRoutineId: String? = null
    private var timeSpentOnCurrentTask: Long = 0L
    private var totalTimeSpent: Long = 0L

    // Guard to prevent concurrent advanceToNextTask calls (race condition fix)
    private var isAdvancing = false

    fun startRoutine(routineId: String) {
        timerJob?.cancel()
        currentRoutineId = routineId
        totalTimeSpent = 0L
        timeSpentOnCurrentTask = 0L
        isAdvancing = false
        viewModelScope.launch {
            try {
                val firstTask = resetAndStartRoutineUseCase(routineId)
                if (firstTask != null) {
                    _uiState.update {
                        it.copy(
                            currentTask = firstTask,
                            secondsRemaining = firstTask.seconds,
                            isPaused = true,
                            isRoutineComplete = false
                        )
                    }
                    timeSpentOnCurrentTask = 0L
                } else {
                    _uiState.update { it.copy(isRoutineComplete = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRoutineComplete = true) }
            }
        }
    }

    fun toggleTimer() {
        val currentState = _uiState.value
        if (currentState.isRoutineComplete || currentState.currentTask == null) return

        if (currentState.isPaused) {
            _uiState.update { it.copy(isPaused = false) }
            startTimerCoroutine()
        } else {
            _uiState.update { it.copy(isPaused = true) }
            timerJob?.cancel()
        }
    }

    private fun startTimerCoroutine() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.secondsRemaining > 0 && !_uiState.value.isPaused && !isAdvancing) {
                delay(1000L)
                timeSpentOnCurrentTask++
                _uiState.update { it.copy(secondsRemaining = it.secondsRemaining - 1) }

                if (_uiState.value.secondsRemaining <= 0L) {
                    advanceToNextTask()
                    break
                }
            }
        }
    }

    fun skipTask() {
        if (isAdvancing) return
        viewModelScope.launch {
            timerJob?.cancel()
            advanceToNextTask()
        }
    }

    private suspend fun advanceToNextTask() {
        // Guard: prevent concurrent execution (timer can fire while already advancing)
        if (isAdvancing) return
        isAdvancing = true
        try {
            val task = _uiState.value.currentTask
            val rId = currentRoutineId

            if (task != null && rId != null) {
                try {
                    totalTimeSpent += timeSpentOnCurrentTask
                    val nextTask = completeTaskAndAdvanceUseCase(
                        taskId = task.id,
                        routineId = rId,
                        timeSpent = timeSpentOnCurrentTask
                    )

                    if (nextTask != null) {
                        _uiState.update {
                            it.copy(
                                currentTask = nextTask,
                                secondsRemaining = nextTask.seconds,
                                isPaused = false,
                                isRoutineComplete = false
                            )
                        }
                        timeSpentOnCurrentTask = 0L
                        isAdvancing = false
                        startTimerCoroutine()
                        return
                    } else {
                        // Routine fully complete
                        val routineName = routineDao.getRoutineName(rId) ?: "Unknown Routine"
                        val history = HistoryEntity(
                            routineId = rId,
                            routineName = routineName,
                            completionDate = System.currentTimeMillis(),
                            totalTimeSpent = totalTimeSpent
                        )
                        routineDao.insertHistory(history)
                        firebaseAuth.currentUser?.uid?.let { userId ->
                            firebaseRoutineRepository.addHistory(userId, history)
                        }

                        _uiState.update {
                            it.copy(
                                currentTask = null,
                                secondsRemaining = 0L,
                                isPaused = true,
                                isRoutineComplete = true
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            currentTask = null,
                            secondsRemaining = 0L,
                            isPaused = true,
                            isRoutineComplete = true
                        )
                    }
                }
            }
        } finally {
            isAdvancing = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
