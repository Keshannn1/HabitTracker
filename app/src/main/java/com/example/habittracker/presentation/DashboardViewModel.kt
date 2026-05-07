package com.example.habittracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.dao.RoutineDao
import com.example.habittracker.data.local.entity.HistoryEntity
import com.example.habittracker.data.local.entity.RoutineWithTasks
import com.example.habittracker.data.remote.FirebaseRoutineRepository
import com.example.habittracker.domain.model.AuthState
import com.example.habittracker.domain.model.UserProfile
import com.example.habittracker.domain.repository.AuthRepository
import com.example.habittracker.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val userProfile: UserProfile? = null,
    val isProfileLoading: Boolean = false,
    // Inline analytics
    val totalRoutinesCompleted: Int = 0,
    val totalTimeSpentFormatted: String = "0m",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val routineDao: RoutineDao,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val firebaseRoutineRepository: FirebaseRoutineRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    val allRoutines: StateFlow<List<RoutineWithTasks>> = routineDao.getAllRoutinesWithTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var profileObserver: AutoCloseable? = null

    init {
        // Observe auth and load profile when authenticated
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    loadProfile(state.userId)
                } else {
                    _uiState.value = _uiState.value.copy(userProfile = null)
                    profileObserver?.close()
                    profileObserver = null
                }
            }
        }

        // Observe history for inline analytics
        viewModelScope.launch {
            routineDao.getAllHistory().collect { historyList ->
                computeInlineAnalytics(historyList)
            }
        }
    }

    private fun computeInlineAnalytics(historyList: List<HistoryEntity>) {
        if (historyList.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                totalRoutinesCompleted = 0,
                totalTimeSpentFormatted = "0m",
                currentStreak = 0,
                longestStreak = 0
            )
            return
        }

        val totalRoutinesCompleted = historyList.size
        val totalSeconds = historyList.sumOf { it.totalTimeSpent }
        val totalTimeSpentFormatted = formatDuration(totalSeconds)

        // Calculate streaks from history
        val groupedByDay = historyList
            .map { entry ->
                Calendar.getInstance().apply { timeInMillis = entry.completionDate }
            }
            .distinctBy { cal ->
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
            }
            .sortedByDescending { it.timeInMillis }

        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        if (groupedByDay.isNotEmpty()) {
            val today = Calendar.getInstance()
            val mostRecent = groupedByDay.first()

            // Calculate current streak
            if (isTodayOrYesterday(mostRecent, today)) {
                currentStreak = 1
                for (i in 1 until groupedByDay.size) {
                    val expected = Calendar.getInstance().apply {
                        timeInMillis = groupedByDay[i - 1].timeInMillis
                        add(Calendar.DAY_OF_YEAR, -1)
                    }
                    if (isSameDay(expected, groupedByDay[i])) {
                        currentStreak++
                    } else {
                        break
                    }
                }
            }

            // Calculate longest streak
            tempStreak = 1
            longestStreak = 1
            for (i in 1 until groupedByDay.size) {
                val expected = Calendar.getInstance().apply {
                    timeInMillis = groupedByDay[i - 1].timeInMillis
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                if (isSameDay(expected, groupedByDay[i])) {
                    tempStreak++
                } else {
                    longestStreak = maxOf(longestStreak, tempStreak)
                    tempStreak = 1
                }
            }
            longestStreak = maxOf(longestStreak, tempStreak)
        }

        _uiState.value = _uiState.value.copy(
            totalRoutinesCompleted = totalRoutinesCompleted,
            totalTimeSpentFormatted = totalTimeSpentFormatted,
            currentStreak = currentStreak,
            longestStreak = longestStreak
        )
    }

    private fun isTodayOrYesterday(date: Calendar, today: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(date, today) || isSameDay(date, yesterday)
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    private fun loadProfile(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProfileLoading = true)

            // Get initial profile
            profileRepository.getProfile(userId).onSuccess { profile ->
                _uiState.value = _uiState.value.copy(
                    userProfile = profile,
                    isProfileLoading = false
                )
            }

            // Observe real-time profile updates
            profileObserver?.close()
            profileObserver = profileRepository.observeProfile(userId) { updatedProfile ->
                _uiState.value = _uiState.value.copy(
                    userProfile = updatedProfile,
                    isProfileLoading = false
                )
            }
        }
    }

    /** Called when a routine is completed - increments cloud stats */
    fun onRoutineCompleted() {
        val currentAuth = authState.value
        if (currentAuth is AuthState.Authenticated) {
            viewModelScope.launch {
                profileRepository.incrementRoutinesCompleted(currentAuth.userId)
                profileRepository.updateStreak(currentAuth.userId)
            }
        }
    }

    fun deleteRoutine(routineWithTasks: RoutineWithTasks) {
        viewModelScope.launch {
            routineDao.deleteRoutine(routineWithTasks.routine)
            val currentAuth = authState.value
            if (currentAuth is AuthState.Authenticated) {
                firebaseRoutineRepository.deleteRoutine(currentAuth.userId, routineWithTasks.routine.id)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileObserver?.close()
    }
}