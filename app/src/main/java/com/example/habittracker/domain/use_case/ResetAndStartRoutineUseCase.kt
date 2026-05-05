package com.example.habittracker.domain.use_case

import com.example.habittracker.data.local.dao.RoutineDao
import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.data.local.entity.TaskStatus
import javax.inject.Inject

class ResetAndStartRoutineUseCase @Inject constructor(
    private val routineDao: RoutineDao
) {
    suspend operator fun invoke(routineId: String): TaskEntity? {
        // Set all tasks for a routineId to PENDING
        routineDao.resetTasksToPending(routineId)

        // Fetch the first task (orderIndex = 0), set it to ACTIVE, and return it
        val firstTask = routineDao.getNextPendingTask(routineId)
        if (firstTask != null) {
            val activeTask = firstTask.copy(status = TaskStatus.ACTIVE)
            routineDao.updateTask(activeTask)
            return activeTask
        }
        
        return null
    }
}
